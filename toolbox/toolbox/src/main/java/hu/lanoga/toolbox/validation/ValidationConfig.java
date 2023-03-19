package hu.lanoga.toolbox.validation;

import javax.validation.Validation;
import javax.validation.Validator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import hu.lanoga.toolbox.vaadin.binder.AutoBinder;
import lombok.extern.slf4j.Slf4j;

/**
 * @see AutoBinder (itt külön van állítva a nyelv)
 */
@Slf4j
@Configuration
public class ValidationConfig {

	// @Autowired
	// private UserSessionAwareMessageInterpolator userSessionAwareMessageInterpolator;

//	@Value("${tools.validation.fallback-locale:en_US}")
//	private String validationFallbackLocaleStr;

	@Bean // ez defaultban singleton lesz (@Scope("singleton")), a Validator thread-safe
	public Validator validator() {

		// FIXME: userSessionAwareMessageInterpolator nem érvényesül
		// (akkor, sem, ha itt messageInterpolator()-ba beteszem)

		log.debug("ValidationConfig (Hibernate validator) happened");

		// return Validation.byDefaultProvider().configure().buildValidatorFactory().getValidator();
		// return Validation.byDefaultProvider().configure().messageInterpolator(userSessionAwareMessageInterpolator).buildValidatorFactory().getValidator();

		// ---

		// TODO: talán így megy valamennyire... teszt...
		// nem megy így sem

		javax.validation.Configuration<?> configuration = Validation.byDefaultProvider().configure();

//		if (configuration instanceof HibernateValidatorConfiguration) {
//
//			Locale validationFallbackLocale;
//
//			try {
//				validationFallbackLocale = LocaleUtils.toLocale(validationFallbackLocaleStr);
//			} catch (Exception e) {
//				validationFallbackLocale = Locale.getDefault();
//			}
//
//			((HibernateValidatorConfiguration) configuration).defaultLocale(validationFallbackLocale);
//		}
		
		return configuration.buildValidatorFactory().getValidator();

	}

}
