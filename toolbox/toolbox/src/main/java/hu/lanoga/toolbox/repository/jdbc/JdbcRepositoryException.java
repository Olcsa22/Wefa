package hu.lanoga.toolbox.repository.jdbc;

import hu.lanoga.toolbox.repository.ToolboxRepositoryException;

public class JdbcRepositoryException extends ToolboxRepositoryException {

	public JdbcRepositoryException() {
		super();
	}

	public JdbcRepositoryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public JdbcRepositoryException(String message, Throwable cause) {
		super(message, cause);
	}

	public JdbcRepositoryException(String message) {
		super(message);
	}

	public JdbcRepositoryException(Throwable cause) {
		super(cause);
	}
	
}
