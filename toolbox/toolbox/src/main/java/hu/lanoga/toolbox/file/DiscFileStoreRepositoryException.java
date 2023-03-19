package hu.lanoga.toolbox.file;

import hu.lanoga.toolbox.exception.ToolboxException;

public class DiscFileStoreRepositoryException extends RuntimeException implements ToolboxException {

	public DiscFileStoreRepositoryException() {
		super();
	}

	public DiscFileStoreRepositoryException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DiscFileStoreRepositoryException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public DiscFileStoreRepositoryException(final String message) {
		super(message);
	}

	public DiscFileStoreRepositoryException(final Throwable cause) {
		super(cause);
	}

}
