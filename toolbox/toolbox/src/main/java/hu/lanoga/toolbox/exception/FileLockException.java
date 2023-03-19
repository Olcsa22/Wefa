package hu.lanoga.toolbox.exception;

import lombok.Getter;

@Getter
public class FileLockException extends ManualValidationException {

	public FileLockException() {
		super();
	}

	public FileLockException(String message, String uiMessage) {
		super(message, uiMessage);
	}

	public FileLockException(String message, Throwable cause) {
		super(message, cause);
	}

	public FileLockException(String message) {
		super(message);
	}

}
