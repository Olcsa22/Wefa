package hu.lanoga.toolbox.config.vaadin;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import hu.lanoga.toolbox.vaadin.BasicVaadinServlet;
import hu.lanoga.toolbox.vaadin.ForgottenPasswordUIVaadinServlet;
import hu.lanoga.toolbox.vaadin.ResetPasswordUIVaadinServlet;
import hu.lanoga.toolbox.vaadin.superadmin.SuperAdminUIVaadinServlet;

// Vaadin 14-hez kell (toolbox-v10x-experimental)

@SuppressWarnings("all")

// @ServletComponentScan("hu.lanoga") // első próba, nem volt jó, mert @ConditionalOnProperty-t nem tiszteli

// @Configuration // TODO: második próba, még tisztázni kell, átmenetileg visszakerültek még a @SpringUI annotációk (amik nem Vaadin 1X projekt barátak (ez volt az eredeti motiváció))

public class VaadinServletRegistrationConfig {

	@ConditionalOnProperty(name = "tools.super-admin.ui.enabled", matchIfMissing = true)
	@Bean
	public ServletRegistrationBean superAdminUIVaadinServletBean() {
		final ServletRegistrationBean bean = new ServletRegistrationBean(new SuperAdminUIVaadinServlet());
		bean.setAsyncSupported(true);
		bean.addUrlMappings("/super-admin/v/main/*");
		bean.setLoadOnStartup(1);
		return bean;
	}
	
	@Bean
	public ServletRegistrationBean resetPasswordUIVaadinServletBean() {	
		final ServletRegistrationBean bean = new ServletRegistrationBean(new ResetPasswordUIVaadinServlet());
		bean.setAsyncSupported(true);
		bean.addUrlMappings("/public/v/reset-password/*");
		bean.setLoadOnStartup(1);
		return bean;
	}
	
	@Bean
	public ServletRegistrationBean forgottenPasswordUIVaadinServletBean() {	
		final ServletRegistrationBean bean = new ServletRegistrationBean(new ForgottenPasswordUIVaadinServlet());
		bean.setAsyncSupported(true);
		bean.addUrlMappings("/public/v/forgotten-password/*");
		bean.setLoadOnStartup(1);
		return bean;
	}
	
	@Bean
	public ServletRegistrationBean basicVaadinServletBean() {	
		final ServletRegistrationBean bean = new ServletRegistrationBean(new BasicVaadinServlet());
		bean.setAsyncSupported(true);
		bean.addUrlMappings("/VAADIN/*", "/vaadinServlet/*");
		bean.setLoadOnStartup(1);
		return bean;
	}


}
