package hu.lanoga.toolbox.google.drive;

import hu.lanoga.toolbox.exception.ToolboxException;

public class GoogleDriveManagerException extends RuntimeException implements ToolboxException {

	public GoogleDriveManagerException() {
		super();
	}

	public GoogleDriveManagerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public GoogleDriveManagerException(String message, Throwable cause) {
		super(message, cause);
	}

	public GoogleDriveManagerException(String message) {
		super(message);
	}

	public GoogleDriveManagerException(Throwable cause) {
		super(cause);
	}
	
}
