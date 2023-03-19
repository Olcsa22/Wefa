package hu.lanoga.toolbox.file;

import hu.lanoga.toolbox.exception.ToolboxException;

public final class FileStoreFileCountException extends RuntimeException implements ToolboxException {

	public FileStoreFileCountException() {
		super();
	}

	public FileStoreFileCountException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public FileStoreFileCountException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public FileStoreFileCountException(final String message) {
		super(message);
	}

	public FileStoreFileCountException(final Throwable cause) {
		super(cause);
	}

}
