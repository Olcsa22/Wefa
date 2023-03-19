package hu.lanoga.toolbox.config;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class ErrorPageConfig implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

	@Override
	public void customize(ConfigurableServletWebServerFactory factory) {
		factory.addErrorPages(
				new ErrorPage(HttpStatus.BAD_REQUEST, "/public/error/400"),
				new ErrorPage(HttpStatus.NOT_FOUND, "/public/error/404"),
				new ErrorPage(HttpStatus.UNAUTHORIZED, "/public/error/401"),
				new ErrorPage(HttpStatus.FORBIDDEN, "/public/error/403"),
				new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/public/error/500"));
	}

}