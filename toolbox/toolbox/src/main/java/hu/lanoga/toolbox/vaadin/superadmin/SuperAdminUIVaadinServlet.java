package hu.lanoga.toolbox.vaadin.superadmin;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinServlet;

import hu.lanoga.toolbox.config.vaadin.VaadinServletRegistrationConfig;

/**
 * @see VaadinServletRegistrationConfig
 */
@VaadinServletConfiguration(productionMode = false, ui = SuperAdminUI.class, widgetset = "AppWidgetset")
public class SuperAdminUIVaadinServlet extends VaadinServlet {

	//
		
}