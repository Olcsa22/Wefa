package hu.lanoga.toolbox.export.xlsx;

import hu.lanoga.toolbox.export.ExporterException;

public class XlsxExporterException extends ExporterException {

	public XlsxExporterException() {
		super();
	}

	public XlsxExporterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public XlsxExporterException(String message, Throwable cause) {
		super(message, cause);
	}

	public XlsxExporterException(String message) {
		super(message);
	}

	public XlsxExporterException(Throwable cause) {
		super(cause);
	}
	
}
