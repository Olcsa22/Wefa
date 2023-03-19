package hu.lanoga.toolbox.config;

import java.io.IOException;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import com.google.common.collect.Sets;
import com.teamunify.i18n.I;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.controller.ToolboxHttpUtils;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.user.PublicTokenHolder;
import lombok.extern.slf4j.Slf4j;

/**
 * @see SecurityConfig
 */
@Slf4j
@ConditionalOnMissingBean(name = "authenticationSuccessHandlerOverrideBean")
@Component
public class AuthenticationSuccessHandler implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {

	@Autowired
	private PublicTokenHolder publicTokenHolder;

	@Value("${tools.security.ip-white-list.super-admin}")
	private String ipWhiteListSuperAdmin;

	@Value("${tools.security.ip-white-list.super-user}")
	private String ipWhiteListSuperUser;

	@Value("${tools.security.ip-white-list.admin}")
	private String ipWhiteListAdmin;

	@Value("${tools.security.ip-white-list.user}")
	private String ipWhiteListUser;

	private final SavedRequestAwareAuthenticationSuccessHandler savedRequestAwareAuthenticationSuccessHandler = new SavedRequestAwareAuthenticationSuccessHandler();

	@Override
	public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) throws IOException, ServletException {

		final String fromHeader = request.getHeader("From");

		// ---

		I.clearLocale();

		// ---

		// ip check

		// FIXME: ezeket a whitelist check-eket máshová jobb lenne tenni, ahol biztosabban lefutnak (Spring interceptor)... (egyelőre experimental a feature, ezért jó így is)
		
		// FONTOS: HTTP basic auth-ra ez eleve nem vonatkozik

		final boolean hasIpWhiteListSuperAdmin = StringUtils.isNotBlank(this.ipWhiteListSuperAdmin);
		final boolean hasIpWhiteListSuperUser = StringUtils.isNotBlank(this.ipWhiteListSuperUser);
		final boolean hasIpWhiteListAdmin = StringUtils.isNotBlank(this.ipWhiteListAdmin);
		final boolean hasIpWhiteListUser = StringUtils.isNotBlank(this.ipWhiteListUser);

		if (hasIpWhiteListSuperAdmin || hasIpWhiteListSuperUser || hasIpWhiteListAdmin || hasIpWhiteListUser) {

			final String ipAddress = ToolboxHttpUtils.determineIpAddress(request);
			boolean higherLevelIpCheckChecked = false;

			if (hasIpWhiteListSuperAdmin) {
				checkIpWhiteList(authentication, ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR, this.ipWhiteListSuperAdmin, ipAddress);
				higherLevelIpCheckChecked = true;
			}

			if (hasIpWhiteListSuperUser && !higherLevelIpCheckChecked) {
				checkIpWhiteList(authentication, ToolboxSysKeys.UserAuth.ROLE_SUPER_USER_STR, this.ipWhiteListSuperUser, ipAddress);
				higherLevelIpCheckChecked = true;
			}

			if (hasIpWhiteListAdmin && !higherLevelIpCheckChecked) {
				checkIpWhiteList(authentication, ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR, this.ipWhiteListAdmin, ipAddress);
				higherLevelIpCheckChecked = true;
			}

			if (hasIpWhiteListUser && !higherLevelIpCheckChecked) {
				checkIpWhiteList(authentication, ToolboxSysKeys.UserAuth.ROLE_USER_STR, this.ipWhiteListUser, ipAddress);
				higherLevelIpCheckChecked = true;
			}
			
		}

		// ---

		// two factor check

		final String twoFactorCode = request.getParameter("tfc");
		if (!SecurityUtil.handleTwoFactorLogin((ToolboxUserDetails) authentication.getPrincipal(), twoFactorCode)) {
			
			log.debug("two factor auth. fail");
			
			SecurityUtil.clearAuthentication();

			new DefaultRedirectStrategy().sendRedirect(request, response, "/public/login?error=true"); // ez már figyeli a contextPath-ot alapban elvileg
			return;
			
		}

		// ---

		if (SecurityUtil.checkForNoRedirectClient(fromHeader)) {

			response.setStatus(200);

		} else {

			handlePublicTokenCookieStoring(request, response);

			// ---
			
			// redirect (role stb. alapján)

			boolean wasRedirectedAlready = redirectAfterLoginDevTool(request, response, authentication);

			if (!wasRedirectedAlready) {
				wasRedirectedAlready = redirectAfterLoginSuper(request, response, authentication);
			}
			
			if (!wasRedirectedAlready) {
				wasRedirectedAlready = redirectAfterLogin(request, response, authentication);
			}
			
			if (!wasRedirectedAlready) {
				this.savedRequestAwareAuthenticationSuccessHandler.
				onAuthenticationSuccess(request, response, authentication); // oda viszi, ahova próbált menni login oldara irányítás előtt
			}

		}

	}
	
	/**
	 * @param authentication
	 * @param roleStr
	 * @param ipAddress
	 * @param whiteListStr
	 * 
	 * @throws AccessDeniedException
	 * 		ez dob (ez kell dobni), ha elbukik a check
	 */
	protected void checkIpWhiteList(final Authentication authentication, final String roleStr, final String ipAddress, final String whiteListStr) throws AccessDeniedException {

		final HashSet<String> ipWhitelistSet = Sets.newHashSet(whiteListStr.split(","));

		for (final GrantedAuthority authority : authentication.getAuthorities()) {
			if (authority.getAuthority().equalsIgnoreCase(roleStr)) {

				if (!ipWhitelistSet.contains(ipAddress)) {
					log.warn("ipWhiteList (for role: " + roleStr + ") check failed!");
					SecurityUtil.clearAuthentication();
					throw new AccessDeniedException("Access denied.");
				}

				break;
			}
		}

	}
	
	/**
	 * @param request
	 * @param response
	 * 
	 * @see PublicTokenHolder
	 * 		lásd javadoc ott, hogy mi is ez az egész
	 */
	protected void handlePublicTokenCookieStoring(final HttpServletRequest request, final HttpServletResponse response) {

		final Cookie c = WebUtils.getCookie(request, "pt");

		if (c != null) {

			final String t = c.getValue();

			if (StringUtils.isNotBlank(t)) {

				this.publicTokenHolder.storeToken(t);

				final Cookie cookie = new Cookie("pt", "");
				cookie.setHttpOnly(true);
				cookie.setMaxAge(-1);
				cookie.setPath(request.getContextPath() + "/"); // ez kell!

				response.addCookie(cookie); // már nem tudom miért, de itt rá kell frissíteni a cookie-ra

			}
		}

	}

	/**
	 * ez fejlesztői célra van, amikor Angular, Vue JS stb. developer futattó eszközzel van használva... 
	 * (a szokásos 4200 portos eszközre kell gondolni, "ng serve" stb.)... 
	 * lásd még CORS kapcsán config... 
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	@SuppressWarnings("unused")
	protected boolean redirectAfterLoginDevTool(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) {

		try {

			if (ApplicationContextHelper.hasDevProfile()) {

				final Cookie c = WebUtils.getCookie(request, "redirectAfterLogin");

				if (c != null) {
					final String t = c.getValue();

					if (StringUtils.isNotBlank(t)) {

						final Cookie cookie = new Cookie("redirectAfterLogin", "");
						cookie.setHttpOnly(true);
						cookie.setMaxAge(-1);
						cookie.setPath(request.getContextPath() + "/"); // ez kell!

						response.addCookie(cookie);

						new DefaultRedirectStrategy().sendRedirect(request, response, t);

						return true;
					}

				}

			}

		} catch (final Exception e) {
			log.warn("devToolRedirectAfterLogin failed", e);
		}
		
		return false;
	}

	protected boolean redirectAfterLoginSuper(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) throws IOException {

		// TODO: túl sok dolog van már erre a célra, mégegyszer összevetni a RedirectController-ben lévő dolgokkal is
		
		for (final GrantedAuthority authority : authentication.getAuthorities()) {
			if (authority.getAuthority().equalsIgnoreCase(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR)) {
				final String targetUrl = "/super-admin";
				new DefaultRedirectStrategy().sendRedirect(request, response, targetUrl); // ez már figyeli a contextPath-ot alapban elvileg
				return true;
			}
		}
		
		for (final GrantedAuthority authority : authentication.getAuthorities()) {
			if (authority.getAuthority().equalsIgnoreCase(ToolboxSysKeys.UserAuth.ROLE_SUPER_USER_STR)) {
				final String targetUrl = "/super-user";
				new DefaultRedirectStrategy().sendRedirect(request, response, targetUrl); // ez már figyeli a contextPath-ot alapban elvileg
				return true;
			}
		}
				
		return false;

	}
	
	protected boolean redirectAfterLogin(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) throws IOException {
				
		// TODO: túl sok dolog van, összevetni a RedirectController-ben lévő dolgokkal is
		
		for (final GrantedAuthority authority : authentication.getAuthorities()) {
			if (authority.getAuthority().equalsIgnoreCase(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)) {
				final String targetUrl = "/admin";
				new DefaultRedirectStrategy().sendRedirect(request, response, targetUrl); // ez már figyeli a contextPath-ot alapban elvileg
				return true;
			}
		}
		
		return false;
		
	}
	
}
