package hu.lanoga.toolbox.file;

import hu.lanoga.toolbox.exception.ToolboxException;

public final class FileStoreMimeCheckException extends RuntimeException implements ToolboxException {

	public FileStoreMimeCheckException() {
		super();
	}

	public FileStoreMimeCheckException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public FileStoreMimeCheckException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public FileStoreMimeCheckException(final String message) {
		super(message);
	}

	public FileStoreMimeCheckException(final Throwable cause) {
		super(cause);
	}

}
