package hu.lanoga.toolbox.exception;

public final class ToolboxNotFoundException extends RuntimeException implements ToolboxException {

	public ToolboxNotFoundException() {
		super();
	}

	public ToolboxNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ToolboxNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public ToolboxNotFoundException(String message) {
		super(message);
	}

	public ToolboxNotFoundException(Throwable cause) {
		super(cause);
	}
	
}
