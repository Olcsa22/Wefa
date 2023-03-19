package hu.lanoga.toolbox.spring;

import hu.lanoga.toolbox.exception.ToolboxException;

public class ToolboxSpringException extends RuntimeException implements ToolboxException {

	public ToolboxSpringException() {
		super();
	}

	public ToolboxSpringException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ToolboxSpringException(String message, Throwable cause) {
		super(message, cause);
	}

	public ToolboxSpringException(String message) {
		super(message);
	}

	public ToolboxSpringException(Throwable cause) {
		super(cause);
	}
	
}
