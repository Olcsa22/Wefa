package hu.lanoga.toolbox.exception;

/**
 * általános Lanoga Toolbox exception...
 */
public final class ToolboxGeneralException extends RuntimeException implements ToolboxException {

	public ToolboxGeneralException() {
		super();
	}

	public ToolboxGeneralException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ToolboxGeneralException(String message, Throwable cause) {
		super(message, cause);
	}

	public ToolboxGeneralException(String message) {
		super(message);
	}

	public ToolboxGeneralException(Throwable cause) {
		super(cause);
	}
	
}
