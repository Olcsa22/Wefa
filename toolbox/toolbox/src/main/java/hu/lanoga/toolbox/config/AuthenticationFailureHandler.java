package hu.lanoga.toolbox.config;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import hu.lanoga.toolbox.spring.SecurityUtil;

/**
 * @see SecurityConfig
 */
@ConditionalOnMissingBean(name = "authenticationFailureHandlerOverrideBean")
@Component
public class AuthenticationFailureHandler implements org.springframework.security.web.authentication.AuthenticationFailureHandler {

	private final SimpleUrlAuthenticationFailureHandler simpleUrlAuthenticationFailureHandler = new SimpleUrlAuthenticationFailureHandler("/public/login?error=true");

	@Override
	public void onAuthenticationFailure(final HttpServletRequest request, final HttpServletResponse response, final AuthenticationException exception) throws IOException, ServletException {

		final String fromHeader = request.getHeader("From");

		if (SecurityUtil.checkForNoRedirectClient(fromHeader)) {
			response.setStatus(401);
		} else {
			this.simpleUrlAuthenticationFailureHandler.onAuthenticationFailure(request, response, exception);
		}

	}

}
