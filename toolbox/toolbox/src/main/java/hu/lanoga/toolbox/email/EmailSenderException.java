package hu.lanoga.toolbox.email;

public class EmailSenderException extends EmailException {

	public EmailSenderException() {
		super();
	}

	public EmailSenderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public EmailSenderException(String message, Throwable cause) {
		super(message, cause);
	}

	public EmailSenderException(String message) {
		super(message);
	}

	public EmailSenderException(Throwable cause) {
		super(cause);
	}
	
}
