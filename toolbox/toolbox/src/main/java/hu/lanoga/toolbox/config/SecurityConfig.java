package hu.lanoga.toolbox.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.google.common.collect.Lists;
import com.teamunify.i18n.I;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxSpringException;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.spring.ToolboxUserDetailsService;
import hu.lanoga.toolbox.user.ToolboxPersistentTokenRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Security config (CORS is)
 *
 * @see SecurityUtil
 * @see ToolboxPersistentTokenRepository
 * @see AuthenticationSuccessHandler
 * @see AuthenticationFailureHandler
 * @see SsoFilterConfig
 * @see WebMvcCorsConfig
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true) // fontos !!!
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private ToolboxPersistentTokenRepository toolboxPersistentTokenRepository;

	@Autowired
	private ToolboxUserDetailsService toolboxUserDetailsService;

	// outdated, kiszedve, ha kell valahol, akkor le kell porolni
//	@Autowired(required = false)
//	private SsoFilterConfig ssoFilter;

	@Autowired
	private hu.lanoga.toolbox.config.AuthenticationSuccessHandler authenticationSuccessHandler;

	@Autowired
	private hu.lanoga.toolbox.config.AuthenticationFailureHandler authenticationFailureHandler;

	@Value("${tools.security.web-security-ignore-url-patterns-additional}")
	private String webSecurityIgnorePatternsAdditional;

	@Value("${tools.security.http-security-permit-all-url-patterns-additional}")
	private String httpSecurityPermitAllPatternsAdditional;

	@Value("${tools.security.cors-config.allowed-origins}")
	private List<String> corsConfigAllowedOrigins;

	@Value("${tools.security.cors-config.allow-credentials}")
	private boolean corsConfigAllowCredentials;

	@Value("${tools.security.http-basic.enabled}")
	private boolean httpBasicEnabled;

//	@Value("${tools.security.sso.enabled}")
//	private boolean ssoEnabled;

	@Value("${tools.security.remember-me.cookie-name}")
	private String rememberMeCookieName;

	@Value("${tools.security.remember-me.validity}")
	private Integer rememberMeValidity;

	@Value("${tools.security.remember-me.always-remember}")
	private boolean rememberMeAlways;
	
	@Value("${tools.security.maximum-sessions}")
	private Integer maxSessions;

	@Override
	public void configure(final WebSecurity web) {
		final String strWebSecIgnoreMatcher = "/robots.txt,/VAADIN/vaadinBootstrap.js,/VAADIN/vaadinPush.debug.js,/VAADIN/vaadinPush.js,/VAADIN/widgetsets/**,/VAADIN/themes/**,/VAADIN/addons/**,/VAADIN/frontend/**,/cdn/**,/assets/**,/public/qc/**,/default/assets/**,/b/assets/**,/" + "b-*/assets/**,/admin/assets/**,/webjars/**,/sw.js" + this.webSecurityIgnorePatternsAdditional;
		log.debug("SEC strWebSecIgnoreMatcher: " + strWebSecIgnoreMatcher);
		web.ignoring().antMatchers(strWebSecIgnoreMatcher.split(",")); // ezen fájloknál semmilyen security nem lesz (Spring Sec. filterek élből nem játszanak), anoymous user stb. sem lesz itt
	}

	@Override
	protected void configure(final HttpSecurity http) throws Exception {

		SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_THREADLOCAL); // SecurityContextHolder.MODE_INHERITABLETHREADLOCAL semmiképp ne, veszélyes

		// ---

		final ToolboxUserDetails publicUser = ApplicationContextHelper.getApplicationContext().getBean(ToolboxUserDetailsService.class)
				.loadUserByUsername(ToolboxSysKeys.UserAuth.COMMON_TENANT_ID + ":" + ToolboxSysKeys.UserAuth.ANONYMOUS_USERNAME);

		if (!publicUser.isEnabled()) {
			throw new ToolboxSpringException("Anonymous user is not enabled!"); // should never happen
		}

		http.anonymous().principal(publicUser).authorities(Lists.newArrayList(new SimpleGrantedAuthority(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR)));

		// ---

		http.cors();

		// ---

//		if (this.ssoFilter != null) {
//			http.antMatcher("/**").addFilterBefore(this.ssoFilter.getFilter(), AnonymousAuthenticationFilter.class);
//		}

		// ---

		final String strPermitAllMatcher = "/vaadinServlet/**,/public/**,/api/public/**" + this.httpSecurityPermitAllPatternsAdditional;
		log.debug("SEC strPermitAllMatcher: " + strPermitAllMatcher);

		final String[] strPermitAllMatcherSplit = strPermitAllMatcher.split(",");

		http.authorizeRequests()
				.antMatchers("/super-admin/**").hasRole(StringUtils.removeStart(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR, "ROLE_"))
				.antMatchers("/super-user/**").hasRole(StringUtils.removeStart(ToolboxSysKeys.UserAuth.ROLE_SUPER_USER_STR, "ROLE_"))
				.antMatchers("/actuator/**").hasRole(StringUtils.removeStart(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR, "ROLE_"))
				.antMatchers("/admin/**").hasRole(StringUtils.removeStart(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR, "ROLE_"))
				.antMatchers("/staff/**").hasRole(StringUtils.removeStart(ToolboxSysKeys.UserAuth.ROLE_USER_STR, "ROLE_"))
				.antMatchers("/samples/**").hasRole(ApplicationContextHelper.hasDevProfile() ? StringUtils.removeStart(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR, "ROLE_") : StringUtils.removeStart(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR, "ROLE_"))
				.antMatchers(strPermitAllMatcherSplit).permitAll() // ezeknél anonymous user lesz beléptetve, tehát a Spring Sec. filterek működnek, csak mindenkit engednek eképpen
				.anyRequest().hasAnyRole(
						StringUtils.removeStart(ToolboxSysKeys.UserAuth.ROLE_USER_STR, "ROLE_"),
						StringUtils.removeStart(ToolboxSysKeys.UserAuth.ROLE_LCU_STR, "ROLE_"),
						StringUtils.removeStart(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR, "ROLE_"));

		if (this.httpBasicEnabled) {
			http.httpBasic(); // formLogin melett tud menni ez is
		}

		http.formLogin().loginPage("/public/login").loginProcessingUrl("/api/login").permitAll()
				.successHandler(authenticationSuccessHandler)
				.failureHandler(authenticationFailureHandler);

		// ---

		if (StringUtils.isNotBlank(this.rememberMeCookieName)) {
			http.rememberMe()
					.tokenRepository(this.toolboxPersistentTokenRepository)
					.userDetailsService(this.toolboxUserDetailsService)
					.rememberMeCookieName(this.rememberMeCookieName)
					.rememberMeParameter("rmp")
					.tokenValiditySeconds(this.rememberMeValidity)
					.alwaysRemember(this.rememberMeAlways);
		}

		// ---

		http.logout()
				.logoutRequestMatcher(new AntPathRequestMatcher("/api/logout")) // figyelem hu.lanoga.toolbox.vaadin.util.UiHelper.logout(UI)-ban is módosítani kell, ha változik
				.logoutSuccessHandler(new LogoutSuccessHandler() {

					@Override
					public void onLogoutSuccess(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) throws IOException, ServletException {

						I.clearLocale();

						// ---

						final String fromHeader = request.getHeader("From");

						if (SecurityUtil.checkForNoRedirectClient(fromHeader)) {
							response.setStatus(200);
						} else {
							response.sendRedirect(request.getContextPath() + "/");
						}

					}
				})
				.and().logout().permitAll();

		// ---

		http.exceptionHandling()
				.accessDeniedHandler(new AccessDeniedHandler() {

					@Override
					public void handle(final HttpServletRequest request, final HttpServletResponse response, final AccessDeniedException accessDeniedException) throws IOException, ServletException {

						final String fromHeader = request.getHeader("From");

						if (SecurityUtil.checkForNoRedirectClient(fromHeader)) {
							response.setStatus(401);
						} else {

							if (("/admin".equals(request.getServletPath()) || "/admin/".equals(request.getServletPath())) && SecurityUtil.isLcuLevelUser()) {
								SecurityUtil.setAnonymous();
								response.sendRedirect(request.getContextPath() + "/public/login");
							} else if (("/staff".equals(request.getServletPath()) || "/staff/".equals(request.getServletPath())) && SecurityUtil.isLcuLevelUser()) {
								SecurityUtil.setAnonymous();
								response.sendRedirect(request.getContextPath() + "/public/login");
							} else if ("/".equals(request.getServletPath())) {

								if (!ArrayUtils.contains(strPermitAllMatcherSplit, "/")) {
									response.sendRedirect(request.getContextPath() + "/public/login");
								}

							} else {
								response.sendRedirect(request.getContextPath() + "/public/no-permission");
							}

						}

					}
				})
				.authenticationEntryPoint(new AuthenticationEntryPoint() {

					private final AuthenticationEntryPoint aep1 = new AuthenticationEntryPoint() {

						@Override
						public void commence(final HttpServletRequest request, final HttpServletResponse response, final AuthenticationException authException) throws IOException, ServletException {

							response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
									authException.getMessage());

						}

					};

					private final LoginUrlAuthenticationEntryPoint aep2 = new LoginUrlAuthenticationEntryPoint("/public/login");

					@Override
					public void commence(final HttpServletRequest request, final HttpServletResponse response, final AuthenticationException authException) throws IOException, ServletException {

						final String fromHeader = request.getHeader("From");

						if (SecurityUtil.checkForNoRedirectClient(fromHeader)) {
							this.aep1.commence(request, response, authException);
						} else {
							this.aep2.commence(request, response, authException);

						}

					}
				});

		// ---

		http.csrf().disable(); // FIXME: berakni, elvileg a frontend mindenhol tudja már... (megnézni azért), Vaadin esetén nem feltétlenül kell, annak van saját ilyen megoldása (megnézni, hogy be van-e kapcsolva)

		// csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).requireCsrfProtectionMatcher(new RequestMatcher() {
		//
		// @Override
		// public boolean matches(HttpServletRequest request) {
		//
		// String method = request.getMethod();
		// String requestUri = request.getRequestURI();
		//
		// if ((requestUri.startsWith("/api"))
		// && !(requestUri.startsWith("/api/public") || requestUri.endsWith("/pagedexp") || requestUri.endsWith("/paged"))
		// && !(method.equals("GET") || method.equals("HEAD") || method.equals("TRACE") || method.equals("OPTIONS"))) {
		//
		// return true;
		// }
		//
		// return false;
		// }
		//
		// })
		// .and()

		// ---

		// sessionFixation:
		// The framework offers protection against typical Session Fixation attacks by configuring what happens to an existing session when the user tries to authenticate again.
		// "migrateSession" = on authentication a new HTTP Session is created, the old one is invalidated and the attributes from the old session are copied over
		// "none" = is set, the original session will not be invalidated
		// "newSession" = a clean session will be created without any of the attributes from the old session being copied over

		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).sessionFixation().newSession();

		// ---

		if (this.maxSessions != null && this.maxSessions >= 0) {
			http.sessionManagement().maximumSessions(this.maxSessions).maxSessionsPreventsLogin(false).sessionRegistry(this.sessionRegistry());
		}
		
		http.headers().frameOptions().sameOrigin();
		
	}

	/**
	 * WebMvcCorsConfig is játszik...
	 * az vonatkozik a static fájlokra is (értsd a WebSecurity hatálya alá tartozókra),
	 * ez a CorsConfigurationSource nem (ez csak azokra, amik mára Spring Sec hatáskörébe tartoznak, tehát a HttpSecurity hatálya alá)
	 * 
	 * @return
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {

		final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

		// ---

		if (this.corsConfigAllowedOrigins != null && !this.corsConfigAllowedOrigins.isEmpty()) {

			final CorsConfiguration configuration1 = new CorsConfiguration();

			configuration1.setAllowedOrigins(this.corsConfigAllowedOrigins);
			configuration1.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "CONNECT", "HEAD", "OPTIONS", "PATCH"));
			configuration1.setAllowCredentials(this.corsConfigAllowCredentials);

			log.debug("SEC corsConfigAllowedOrigins: " + this.corsConfigAllowedOrigins);

			configuration1.setMaxAge(3600L);
			configuration1.setAllowedHeaders(Arrays.asList("Content-Type", "From", "X-XSRF-TOKEN", "Two-Factor"));
			configuration1.setExposedHeaders(Arrays.asList("Content-Type", "From", "X-XSRF-TOKEN", "Two-Factor"));

			source.registerCorsConfiguration("/**", configuration1);

		}

		// ---

//		if (this.ssoEnabled) {
//
//			final CorsConfiguration configuration2 = new CorsConfiguration();
//
//			configuration2.setAllowedOrigins(Arrays.asList("https://www.facebook.com"));
//			configuration2.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "CONNECT", "HEAD", "OPTIONS", "PATCH"));
//			configuration2.setAllowCredentials(false);
//			configuration2.setMaxAge(3600L);
//			configuration2.setAllowedHeaders(Arrays.asList("Content-Type", "From", "X-XSRF-TOKEN", "Two-Factor"));
//			configuration2.setExposedHeaders(Arrays.asList("Content-Type", "From", "X-XSRF-TOKEN", "Two-Factor"));
//			source.registerCorsConfiguration("/api/public/connect/facebook", configuration2);
//
//			log.debug("SEC corsConfigAllowedOrigins: facebook login (https://www.facebook.com -> /api/public/connect/facebook)");
//
//		}

		return source;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	/**
	 * maximumSessions limitációhoz kell ez
	 * 
	 * @return
	 */
	@Bean
	public SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}

	/**
	 * maximumSessions miatt, illetve a {@link SecurityUtil}-nek is kellhet itt-ott
	 * 
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Bean
	public static ServletListenerRegistrationBean httpSessionEventPublisher() {
		return new ServletListenerRegistrationBean(new HttpSessionEventPublisher());
	}

	/**
	 * AuthenticationManager-t nem itt hozzuk létre, ez csak azért kell, hogy pl. a {@link SecurityUtil}-ban Spring Bean-ként is elérhető legyen (egyébként csak a Spring Security belsejében lenne elérhető)
	 */
	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

	@Bean
	public HttpFirewall defaultHttpFirewall() {
		return new DefaultHttpFirewall();
	}

}