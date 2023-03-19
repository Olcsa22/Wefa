package hu.lanoga.toolbox.cache;

import hu.lanoga.toolbox.exception.ToolboxException;

public class ToolboxCacheException extends RuntimeException implements ToolboxException {

	public ToolboxCacheException() {
		super();
	}

	public ToolboxCacheException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ToolboxCacheException(String message, Throwable cause) {
		super(message, cause);
	}

	public ToolboxCacheException(String message) {
		super(message);
	}

	public ToolboxCacheException(Throwable cause) {
		super(cause);
	}
	
}
