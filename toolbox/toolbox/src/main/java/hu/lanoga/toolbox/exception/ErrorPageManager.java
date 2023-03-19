package hu.lanoga.toolbox.exception;

import java.io.StringWriter;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.teamunify.i18n.I;

import hu.lanoga.toolbox.util.BrandUtil;

@Component
public class ErrorPageManager {

	@Autowired
	private ExceptionManager exceptionManager;

	public String generateErrorPage(final int errorCode) {

		final VelocityContext context = new VelocityContext();
		context.put("lblTitle", I.trc("ErrorMessage", "Error"));
		
		// ---
		
		context.put("lblErrorCode", errorCode);

		if (errorCode == 404) {

			context.put("lblErrorMessage", I.trc("ErrorMessage", "Page not found!"));

		} else if ((errorCode == 401) || (errorCode == 403)) {
			
			context.put("lblErrorMessage", I.trc("ErrorMessage", I.trc("Caption", "Access denied!")));
			
		} else if ((errorCode >= 500) && (errorCode < 600)) {

			context.put("lblErrorMessage", I.trc("ErrorMessage", "Server error!"));

			ToolboxGeneralException exception = new ToolboxGeneralException("ErrorPageManager 500!");
			ErrorResult errorResult = exceptionManager.exceptionToErrorResult(exception);
			exceptionManager.logErrorResult(errorResult, exception);

		} else {
			context.put("lblErrorMessage", I.trc("ErrorMessage", "Other error!"));
		}
		
		// ---
		
		context.put("lblBackLinkMessage", I.trc("BackLinkMessage", "Go back"));
		context.put("redirectPage", BrandUtil.getRedirectUriHostFrontend());

		// ---
		
		final StringWriter writer = new StringWriter();
		
		final VelocityEngine velocity = new VelocityEngine();
		velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		velocity.init();

		velocity.getTemplate("html_templates/error-code.vm", "UTF-8").merge(context, writer);
		return writer.toString();
		
	}

}
