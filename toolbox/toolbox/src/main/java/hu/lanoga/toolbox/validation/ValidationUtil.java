package hu.lanoga.toolbox.validation;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import hu.lanoga.toolbox.spring.ApplicationContextHelper;

/**
 * Validation
 *
 * @see ValidationConfig
 */
public class ValidationUtil {

	private ValidationUtil() {
		//
	}

	public static void validateObject(Object o) throws ToolboxConstraintValidationException {

		Set<ConstraintViolation<Object>> constraintViolationSet = ApplicationContextHelper.getBean(Validator.class).validate(o);

		if (!constraintViolationSet.isEmpty()) {
			throw new ToolboxConstraintValidationException(constraintViolationSet);
		}
	
	}

}
