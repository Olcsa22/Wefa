package hu.lanoga.toolbox.validation;

import java.util.Locale;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.LocaleUtils;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.spring.SecurityUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Hibernate Validator nyelv...
 * 
 * @see SecurityUtil#getLoggedInUser()
 */
@Slf4j
@ConditionalOnMissingBean(name = "userSessionAwareMessageInterpolatorOverrideBean")
@Component
public class UserSessionAwareMessageInterpolator extends ResourceBundleMessageInterpolator {
	
	@Value("${tools.validation.fallback-locale:en_US}")
	private String validationFallbackLocaleStr;

	private Locale validationFallbackLocale;
	
	@PostConstruct
	public void init() {
				
		try {
			this.validationFallbackLocale = LocaleUtils.toLocale(validationFallbackLocaleStr);
		} catch (Exception e) {
			this.validationFallbackLocale = Locale.getDefault();
		}
				
	}

	@Override
	public String interpolate(final String messageTemplate, final Context context) {
		
		Locale locale = validationFallbackLocale;
				
		try {
			
			if (SecurityUtil.hasLoggedInUser() && !SecurityUtil.isAnonymous() && !SecurityUtil.isSystem()) {
				locale = I18nUtil.getLoggedInUserLocale();
			}
			
		} catch (Exception e) {
			log.warn("Locale error", e);
		}

		return interpolate(messageTemplate, context, locale);
	}

}