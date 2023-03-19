package hu.lanoga.toolbox.email;

import hu.lanoga.toolbox.exception.ToolboxException;

public class EmailException  extends RuntimeException implements ToolboxException {

	public EmailException() {
		super();
	}

	public EmailException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public EmailException(String message, Throwable cause) {
		super(message, cause);
	}

	public EmailException(String message) {
		super(message);
	}

	public EmailException(Throwable cause) {
		super(cause);
	}
	
}
