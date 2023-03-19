package hu.lanoga.toolbox.exception;

/**
 * hasonló, mint {@link InterruptedException}, de {@link RuntimeException} 
 */
public final class ToolboxInterruptedException extends RuntimeException implements ToolboxException {

	public ToolboxInterruptedException() {
		super();
	}

	public ToolboxInterruptedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ToolboxInterruptedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ToolboxInterruptedException(String message) {
		super(message);
	}

	public ToolboxInterruptedException(Throwable cause) {
		super(cause);
	}
	
}
