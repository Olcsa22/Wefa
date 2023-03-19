package hu.lanoga.toolbox.auth;

import java.io.StringWriter;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.teamunify.i18n.I;

import hu.lanoga.toolbox.exception.ManualValidationException;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.user.PublicTokenHolder;
import hu.lanoga.toolbox.util.BrandUtil;
import hu.lanoga.toolbox.util.DiagnosticsHelper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ConditionalOnMissingBean(name = "loginControllerOverrideBean")
@ConditionalOnProperty(name = "tools.login.controller.enabled", matchIfMissing = true)
@RestController
public class LoginController { // TODO: rename, LoginFormController

	@Value("${tools.misc.application-name}")
	private String applicationName;

	@Value("${tools.security.remember-me.always-remember}")
	private boolean rememberMeAlways;
	
	@Value("${tools.login.two-factor-auth}")
	private boolean twoFactorAuthEnabled;

//	@Value("${tools.security.sso.enabled}")
//	private boolean ssoEnabled;
	
	@Autowired
	private PublicTokenHolder publicTokenHolder;

	/**
	 * desktop helper miatt kellett elsősorban
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "public/logoutLogin", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String logoutLogin(final HttpServletRequest request, final HttpServletResponse response) {

		publicTokenHolder.removeToken();

		request.getSession().invalidate();
		SecurityUtil.setAnonymous();

		return login(request, response);
	}

	@RequestMapping(value = "public/action/checkIfTwoFactorAuthIsNeeded", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	public String checkIfTwoFactorAuthIsNeeded(@RequestParam(value = "u") final String username) {

		if (StringUtils.isBlank(username)) {
			throw new ManualValidationException("Missing username param!");
		}
		
		try {
			
			if (SecurityUtil.checkIfTwoFactorAuthIsNeeded(new String(Base64Utils.decodeFromUrlSafeString(username), "UTF-8"))) {
				return "yes";
			}

			return "no";
			
		} catch (Exception e) {
			throw new ToolboxGeneralException("checkIfTwoFactorAuthIsNeeded error!", e);
		}

	}
	
	/**
	 * származtatott osztályban itt még lehet módosítani a login oldal {@link VelocityContext}-jén, 
	 * az alap feliratok többsége már benne van a {@link VelocityContext}-ben 
	 * (ezeket is lehet itt szükség esetén módostani, kivenni stb.) 
	 * 
	 * @param velocityContext
	 */
	protected void enhanceVelocityContext(VelocityContext velocityContext) {
		//
	}

	/**
	 * login UI (Velocity)...
	 *
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "public/login", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String login(final HttpServletRequest request, final HttpServletResponse response) {

		final VelocityContext velocityContext = new VelocityContext();

		velocityContext.put("contentType", "login-form");

		velocityContext.put("btnLogin", I.trc("Button", "Login"));
		velocityContext.put("btnLoginFacebook", I.trc("Button", "Login with Facebook"));
		velocityContext.put("btnForgottenPassword", I.trc("Button", "Forgotten password"));
		velocityContext.put("chkRememberMe", I.trc("Button", "Remember me (optional, uses cookie)"));
		velocityContext.put("rememberMeAlways", rememberMeAlways);
		velocityContext.put("twoFactorAuthEnabled", twoFactorAuthEnabled);
		// velocityContext.put("ssoEnabled", ssoEnabled);
		velocityContext.put("ssoEnabled", false);
		velocityContext.put("twoFactorCodePlaceholder", I.trc("Button", "Two factor code"));

		// ha van hiba
		final Map<String, String[]> paramMap = request.getParameterMap();

		if (paramMap.containsKey("error")) {
			velocityContext.put("notif", I.trc("Notification", "Wrong username or password!"));
		}
		
		enhanceVelocityContext(velocityContext);

		// ---

		if (ApplicationContextHelper.hasDevProfile()) {

			try {
				String t = request.getParameter("redirectAfterLogin");

				if (StringUtils.isNotBlank(t)) {

					t = new String(Base64Utils.decodeFromUrlSafeString(t));
					
					// mj.: redirectAfterLogin urlsafe base64 gen. js-ben:
				    // let str = window.btoa(window.location.href);
				    // str.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');

					final Cookie cookie = new Cookie("redirectAfterLogin", t);
					cookie.setMaxAge(-1);
					cookie.setHttpOnly(true);
					cookie.setPath(request.getContextPath() + "/");

					response.addCookie(cookie);

				}

			} catch (Exception e) {
				//
			}

		}

		final String pt = request.getParameter("pt");

		if (StringUtils.isNotBlank(pt)) {
			
			// TODO: ez a cookie-ba rakás itt neccess lehet, Security szempontból is, lasd meg PublicTokenHolder
			
			final Cookie cookie = new Cookie("pt", pt);
			cookie.setMaxAge(-1);
			cookie.setHttpOnly(true);
			cookie.setPath(request.getContextPath() + "/");
			response.addCookie(cookie);
		}

		return createVelocityPage(velocityContext, response);
	}

	@RequestMapping(value = "public/forgotten-password", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String forgottenPassword(final HttpServletResponse response) {

		final VelocityContext velocityContext = new VelocityContext();

		velocityContext.put("contentType", "forgotten-password");
		velocityContext.put("goBackMsg", I.trc("Caption", "Back to main page"));

		return createVelocityPage(velocityContext, response);
	}

	@RequestMapping(value = "public/reset-password", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String resetPassword(@RequestParam(value = "token") final String token, final HttpServletResponse response) {

		final VelocityContext velocityContext = new VelocityContext();

		velocityContext.put("contentType", "reset-password");
		velocityContext.put("token", token);
		velocityContext.put("goBackMsg", I.trc("Caption", "Back to main page"));

		return createVelocityPage(velocityContext, response);
	}

	private static String createVelocityPage(final VelocityContext velocityContext, final HttpServletResponse response) {

		try {

			if (!SecurityUtil.isAnonymous()) {
				response.setHeader("Location", "/");
				response.setStatus(302);
				return "";
			}

			final String appTitle = BrandUtil.getAppTitle(false);
			velocityContext.put("appTitle", appTitle);
			velocityContext.put("brand", BrandUtil.getBrand(false, true));

			velocityContext.put("copyright", "&copy; " + (new Date().getYear() + 1900) + " " + appTitle + (ApplicationContextHelper.hasDevProfile() ? " (dev)" : ""));
			velocityContext.put("appVersion", "v" + DiagnosticsHelper.getShortBuildAndGitCommitInfo());

			// ---

			try (final StringWriter writer = new StringWriter()) {

				final VelocityEngine velocity = new VelocityEngine();
				velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
				velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
				velocity.init();

				velocity.getTemplate("html_templates/login-and-auth-frame.vm", "UTF-8").merge(velocityContext, writer);
				return writer.toString();

			}

		} catch (final Exception e) {
			log.error("login error", e);
			throw new ToolboxGeneralException("login error", e);
		}
	}

}