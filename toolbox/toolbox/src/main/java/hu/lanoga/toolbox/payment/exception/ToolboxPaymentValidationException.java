package hu.lanoga.toolbox.payment.exception;

public class ToolboxPaymentValidationException extends javax.validation.ValidationException implements ToolboxPaymentException {

	public ToolboxPaymentValidationException(String message) {
		super(message);
	}

	public ToolboxPaymentValidationException() {
	}

	public ToolboxPaymentValidationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ToolboxPaymentValidationException(Throwable cause) {
		super(cause);
	}
}
