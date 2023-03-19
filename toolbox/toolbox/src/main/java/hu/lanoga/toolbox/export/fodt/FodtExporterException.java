package hu.lanoga.toolbox.export.fodt;

import hu.lanoga.toolbox.export.ExporterException;

public class FodtExporterException extends ExporterException {

	public FodtExporterException() {
		super();
	}

	public FodtExporterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public FodtExporterException(String message, Throwable cause) {
		super(message, cause);
	}

	public FodtExporterException(String message) {
		super(message);
	}

	public FodtExporterException(Throwable cause) {
		super(cause);
	}
	
}
