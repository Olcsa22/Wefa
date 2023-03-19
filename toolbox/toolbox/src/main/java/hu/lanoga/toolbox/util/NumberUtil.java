package hu.lanoga.toolbox.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import hu.lanoga.toolbox.i18n.I18nUtil;

public class NumberUtil {

	public static final char THIN_SPACE = 0x2009; // see https://en.wikipedia.org/wiki/Thin_space
	public static final char NON_BREAKING_SPACE = 0x00A0; // see https://en.wikipedia.org/wiki/Non-breaking_space

	private NumberUtil() {
		//
	}

	/**
	 * replaces some special space chars (0x2009 and 0x00A0) with "normal" space char
	 * 
	 * @param value
	 * @return
	 */
	public static String replaceThinAndNonBreakingSpace(String value) {

		if (StringUtils.isNotBlank(value)) {
			return value.replace(THIN_SPACE, ' ').replace(NON_BREAKING_SPACE, ' ');
		}

		return null;

	}

	public static String formatBigDecimal(final BigDecimal value, final Locale locale, final int decimalScale, final RoundingMode roundingMode) {

		if (value == null) {
			return null;
		}

		final DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(locale); // teszthez Locale.GERMAN
		formatter.setGroupingSize(3); // értsd: 123,456.000 azaz százhuszonháromezer-négyszázötvenhat
		formatter.setMinimumFractionDigits(decimalScale);
		formatter.setMaximumFractionDigits(decimalScale);
		formatter.setRoundingMode(roundingMode);
		return replaceThinAndNonBreakingSpace(formatter.format(value));
	}
	
	public static String formatBigDecimal(final BigDecimal value, final Locale locale, final int decimalScale) {
		return formatBigDecimal(value, locale, decimalScale, RoundingMode.HALF_UP);
	}

	public static String formatBigDecimalLoggedInUserLocale(final BigDecimal value, final int decimalScale, final RoundingMode roundingMode) {
		return formatBigDecimal(value, I18nUtil.getLoggedInUserLocale(), decimalScale, roundingMode);
	}
	
	public static String formatBigDecimalLoggedInUserLocale(final BigDecimal value, final int decimalScale) {
		return formatBigDecimal(value, I18nUtil.getLoggedInUserLocale(), decimalScale, RoundingMode.HALF_UP);
	}

}
