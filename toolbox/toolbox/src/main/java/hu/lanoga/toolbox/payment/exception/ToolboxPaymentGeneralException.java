package hu.lanoga.toolbox.payment.exception;

public class ToolboxPaymentGeneralException extends RuntimeException implements ToolboxPaymentException {

	public ToolboxPaymentGeneralException() {
		super();
	}

	public ToolboxPaymentGeneralException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ToolboxPaymentGeneralException(String message, Throwable cause) {
		super(message, cause);
	}

	public ToolboxPaymentGeneralException(String message) {
		super(message);
	}

	public ToolboxPaymentGeneralException(Throwable cause) {
		super(cause);
	}

}
