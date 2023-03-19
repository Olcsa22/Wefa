package hu.lanoga.toolbox.cert;

import hu.lanoga.toolbox.exception.ToolboxException;

public class LetsEncryptException extends RuntimeException implements ToolboxException {

	public LetsEncryptException() {
		super();
	}

	public LetsEncryptException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public LetsEncryptException(String message, Throwable cause) {
		super(message, cause);
	}

	public LetsEncryptException(String message) {
		super(message);
	}

	public LetsEncryptException(Throwable cause) {
		super(cause);
	}

}
