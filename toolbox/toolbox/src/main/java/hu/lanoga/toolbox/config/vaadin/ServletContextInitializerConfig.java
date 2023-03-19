package hu.lanoga.toolbox.config.vaadin;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.boot.web.servlet.ServletContextInitializer;

// Próba

// @Configuration
public class ServletContextInitializerConfig { // unused

	// @Bean
	public ServletContextInitializer initializer() {

		return new ServletContextInitializer() {

			@Override
			public void onStartup(final ServletContext servletContext) throws ServletException {

				// servletContext.setInitParameter("disable-xsrf-protection", "true"); // Vaadin

			}
		};

	}

}
