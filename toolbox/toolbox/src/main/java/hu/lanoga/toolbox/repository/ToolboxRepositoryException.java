package hu.lanoga.toolbox.repository;

import hu.lanoga.toolbox.exception.ToolboxException;

public class ToolboxRepositoryException extends RuntimeException implements ToolboxException {

	public ToolboxRepositoryException() {
		super();
	}

	public ToolboxRepositoryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ToolboxRepositoryException(String message, Throwable cause) {
		super(message, cause);
	}

	public ToolboxRepositoryException(String message) {
		super(message);
	}

	public ToolboxRepositoryException(Throwable cause) {
		super(cause);
	}
	
}
