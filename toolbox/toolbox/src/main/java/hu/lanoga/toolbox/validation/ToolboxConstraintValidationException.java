package hu.lanoga.toolbox.validation;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;

import hu.lanoga.toolbox.exception.ToolboxException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * hibás, hiányzó paraméterek stb.
 * fontos, hogy a message olyan legyen, amit a kliensnek is le lehet küldeni (security szempontból szenzitív/fontos adat ne legyen benne!)
 */
@Getter
@Setter
@ToString
public final class ToolboxConstraintValidationException extends ValidationException implements ToolboxException {

	private final transient Set<ConstraintViolation<Object>> constraintViolationSet;

	public ToolboxConstraintValidationException(Set<ConstraintViolation<Object>> constraintViolationSet) {
		super(constraintViolationSet.toString());
		this.constraintViolationSet = constraintViolationSet;
	}

}
