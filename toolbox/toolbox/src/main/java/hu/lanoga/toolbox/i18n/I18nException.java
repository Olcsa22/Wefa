package hu.lanoga.toolbox.i18n;

import hu.lanoga.toolbox.exception.ToolboxException;

public class I18nException extends RuntimeException implements ToolboxException {

	public I18nException() {
		super();
	}

	public I18nException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public I18nException(String message, Throwable cause) {
		super(message, cause);
	}

	public I18nException(String message) {
		super(message);
	}

	public I18nException(Throwable cause) {
		super(cause);
	}
	
}
