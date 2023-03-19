package hu.lanoga.toolbox.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import hu.lanoga.toolbox.exception.ErrorResult;
import hu.lanoga.toolbox.exception.ExceptionManager;

/**
 * általános exception handler az API-hoz
 */
@ConditionalOnMissingBean(name = "controllerExceptionHandlerOverrideBean")
@ControllerAdvice
public class ControllerExceptionHandler {

	@Autowired
	private ExceptionManager exceptionManager;

	@ExceptionHandler(value = Throwable.class)
	public ResponseEntity<ErrorResult> handleException(final Throwable e, @SuppressWarnings("unused") final WebRequest request) {
		
		ErrorResult errorResult = exceptionManager.exceptionToErrorResult(e);
		exceptionManager.logErrorResult(errorResult, e);
		
		HttpHeaders responseHeaders = new HttpHeaders();
	    responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		
		return new ResponseEntity<>(errorResult, responseHeaders, errorResult.getHttpStatus());
		
	}

}