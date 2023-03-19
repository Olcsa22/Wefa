package hu.lanoga.toolbox.export.docx;

import hu.lanoga.toolbox.export.ExporterException;

public class DocxExporterException extends ExporterException {

	public DocxExporterException() {
		super();
	}

	public DocxExporterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DocxExporterException(String message, Throwable cause) {
		super(message, cause);
	}

	public DocxExporterException(String message) {
		super(message);
	}

	public DocxExporterException(Throwable cause) {
		super(cause);
	}
	
}
