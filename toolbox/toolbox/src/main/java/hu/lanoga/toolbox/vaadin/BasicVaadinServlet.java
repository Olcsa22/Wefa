package hu.lanoga.toolbox.vaadin;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinServlet;

import hu.lanoga.toolbox.config.vaadin.VaadinServletRegistrationConfig;

/**
 * @see VaadinServletRegistrationConfig
 */
@VaadinServletConfiguration(productionMode = false, ui = ForgottenPasswordUI.class, widgetset = "AppWidgetset")
public class BasicVaadinServlet extends VaadinServlet {

	//
		
}