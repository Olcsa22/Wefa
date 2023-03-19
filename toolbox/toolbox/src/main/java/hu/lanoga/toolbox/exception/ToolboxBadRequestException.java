package hu.lanoga.toolbox.exception;

public final class ToolboxBadRequestException extends RuntimeException implements ToolboxException {

	public ToolboxBadRequestException() {
		super();
	}

	public ToolboxBadRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ToolboxBadRequestException(String message, Throwable cause) {
		super(message, cause);
	}

	public ToolboxBadRequestException(String message) {
		super(message);
	}

	public ToolboxBadRequestException(Throwable cause) {
		super(cause);
	}
	
}
