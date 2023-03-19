package hu.lanoga.toolbox.export;

import hu.lanoga.toolbox.exception.ToolboxException;

public class ExporterException extends RuntimeException implements ToolboxException {

	public ExporterException() {
		super();
	}

	public ExporterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ExporterException(String message, Throwable cause) {
		super(message, cause);
	}

	public ExporterException(String message) {
		super(message);
	}

	public ExporterException(Throwable cause) {
		super(cause);
	}
	
}
