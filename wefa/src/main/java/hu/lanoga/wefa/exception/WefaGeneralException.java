package hu.lanoga.wefa.exception;

public class WefaGeneralException extends RuntimeException implements WefaException {

	public WefaGeneralException() {
		super();
	}

	public WefaGeneralException(final String message) {
		super(message);
	}

	public WefaGeneralException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public WefaGeneralException(final Throwable cause) {
		super(cause);
	}

	protected WefaGeneralException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
