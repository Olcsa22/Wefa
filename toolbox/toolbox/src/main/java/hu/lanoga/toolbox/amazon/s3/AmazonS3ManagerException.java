package hu.lanoga.toolbox.amazon.s3;

import hu.lanoga.toolbox.exception.ToolboxException;

public class AmazonS3ManagerException extends RuntimeException implements ToolboxException {

	public AmazonS3ManagerException() {
		super();
	}

	public AmazonS3ManagerException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public AmazonS3ManagerException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public AmazonS3ManagerException(final String message) {
		super(message);
	}

	public AmazonS3ManagerException(final Throwable cause) {
		super(cause);
	}

}
