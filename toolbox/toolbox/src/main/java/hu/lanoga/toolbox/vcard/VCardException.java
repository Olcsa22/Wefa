package hu.lanoga.toolbox.vcard;

import hu.lanoga.toolbox.exception.ToolboxException;

public class VCardException extends RuntimeException implements ToolboxException {

	public VCardException() {
	}

	public VCardException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public VCardException(String message, Throwable cause) {
		super(message, cause);
	}

	public VCardException(String message) {
		super(message);
	}

	public VCardException(Throwable cause) {
		super(cause);
	}
}
