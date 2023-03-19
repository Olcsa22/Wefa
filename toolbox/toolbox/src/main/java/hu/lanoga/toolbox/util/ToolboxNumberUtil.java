package hu.lanoga.toolbox.util;

import org.apache.commons.lang3.StringUtils;

public final class ToolboxNumberUtil {

	private ToolboxNumberUtil() {
		//
	}

	public static String stringToBigDecimalSafeString(String value, String thousandSep, String decimalMark) {
		
		value = StringUtils.trimToNull(value);
		
		if (value == null) {
			return null;
		}
		
		value = StringUtils.replace(value, thousandSep, "");
		value = StringUtils.replace(value, " ", "");
		value = StringUtils.replace(value, " ", ""); // ez egy másik fajta space
		// value = value.replaceAll("\\s+", ""); // nem jó
		value = StringUtils.replace(value, decimalMark, ".");

		return value;
		
	}

}
