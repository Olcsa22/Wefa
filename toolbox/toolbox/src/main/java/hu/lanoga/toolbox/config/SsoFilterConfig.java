//package hu.lanoga.toolbox.config;
//
//import java.io.IOException;
//
//import javax.servlet.Filter;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
//import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.boot.web.servlet.FilterRegistrationBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.oauth2.client.OAuth2ClientContext;
//import org.springframework.security.oauth2.client.OAuth2RestTemplate;
//import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
//import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
//import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
//import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
//import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
//
//import hu.lanoga.toolbox.spring.SecurityUtil;
//import hu.lanoga.toolbox.user.sso.UserSsoService;
//
///**
// * ritkán használt, elavult, problémás lehet (átnézni, tesztelni, ha kell egy projektben)
// */
//@SuppressWarnings("all") // TODO: átmentileg
//@ConditionalOnProperty("tools.security.sso.enabled")
//@Configuration
//@EnableOAuth2Client
//public class SsoFilterConfig {
//
//	@Autowired
//	@Qualifier("oauth2ClientContext")
//	private OAuth2ClientContext oauth2ClientContext;
//	
//	@Value("${tools.security.sso.success-url}")
//	private String successUrl;
//
//	@Autowired
//	private UserSsoService userSsoService;
//
//	public Filter getFilter() {
//
//		// final List<Filter> filters = new ArrayList<>();
//
//		final OAuth2ClientAuthenticationProcessingFilter facebookFilter = new OAuth2ClientAuthenticationProcessingFilter("/api/public/connect/facebook");
//
//		final AuthorizationCodeResourceDetails facebook = facebook();
//		final ResourceServerProperties facebookResource = facebookResource();
//
//		final OAuth2RestTemplate facebookTemplate = new OAuth2RestTemplate(facebook, oauth2ClientContext);
//		facebookFilter.setRestTemplate(facebookTemplate);
//
//		final UserInfoTokenServices tokenServices = new UserInfoTokenServices(facebookResource.getUserInfoUri(), facebook.getClientId());
//		tokenServices.setRestTemplate(facebookTemplate);
//		tokenServices.setPrincipalExtractor(sourceMap -> {
//			SecurityUtil.setAnonymous();
//			return userSsoService.extractPrincipal(sourceMap);
//		});
//		facebookFilter.setTokenServices(tokenServices);
//
//		facebookFilter.setAuthenticationSuccessHandler(new SimpleUrlAuthenticationSuccessHandler() {
//		    @Override
//			public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
//		        this.setDefaultTargetUrl(successUrl);
//		        super.onAuthenticationSuccess(request, response, authentication);
//		    }
//		});
//		
//		return facebookFilter;
//
//		// filters.add(facebookFilter);
//
//		// final CompositeFilter compositeFilter = new CompositeFilter();
//		// compositeFilter.setFilters(filters);
//		// return compositeFilter;
//	}
//
//	@Bean
//	public FilterRegistrationBean oauth2ClientFilterRegistration(final OAuth2ClientContextFilter filter) {
//		final FilterRegistrationBean registration = new FilterRegistrationBean();
//		registration.setFilter(filter);
//		registration.setOrder(-100);
//		return registration;
//	}
//
//	@Bean
//	@ConfigurationProperties("facebook.client")
//	public AuthorizationCodeResourceDetails facebook() {
//		return new AuthorizationCodeResourceDetails();
//	}
//
//	@Bean
//	@ConfigurationProperties("facebook.resource")
//	public ResourceServerProperties facebookResource() {
//		return new ResourceServerProperties();
//	}
//
//	// @Bean
//	// @ConfigurationProperties("google.client")
//	// public AuthorizationCodeResourceDetails google() {
//	// return new AuthorizationCodeResourceDetails();
//	// }
//	//
//	// @Bean
//	// @ConfigurationProperties("google.resource")
//	// public ResourceServerProperties googleResource() {
//	// return new ResourceServerProperties();
//	// }
//	//
//	// @Bean
//	// @ConfigurationProperties("linkedIn.client")
//	// public AuthorizationCodeResourceDetails linkedIn() {
//	// return new AuthorizationCodeResourceDetails();
//	// }
//	//
//	// @Bean
//	// @ConfigurationProperties("linkedIn.resource")
//	// public ResourceServerProperties linkedInResource() {
//	// return new ResourceServerProperties();
//	// }
//	//
//	// @Bean
//	// @ConfigurationProperties("twitter.client")
//	// public AuthorizationCodeResourceDetails twitter() {
//	// return new AuthorizationCodeResourceDetails();
//	// }
//	//
//	// @Bean
//	// @ConfigurationProperties("twitter.resource")
//	// public ResourceServerProperties twitterResource() {
//	// return new ResourceServerProperties();
//	// }
//
//}
