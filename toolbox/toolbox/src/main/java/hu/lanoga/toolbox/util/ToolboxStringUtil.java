package hu.lanoga.toolbox.util;

import java.text.Normalizer;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.CaseFormat;

public final class ToolboxStringUtil {

	private ToolboxStringUtil() {
		//
	}

	/**
	 * camelCase -> kisbetűs aláhúzásos név (Spring metódus majdnem változatlan másolata)
	 */
	public static String camelCaseToUnderscore(final String str) {

		// com.google.common.base.CaseFormat is tudja ezt, nem kell megírni így

		if (StringUtils.isBlank(str)) {
			return str;
		}

		final StringBuilder result = new StringBuilder();
		result.append(str.substring(0, 1).toLowerCase());
		for (int i = 1; i < str.length(); i++) {
			final String s = str.substring(i, i + 1);
			final String slc = s.toLowerCase();
			if (!s.equals(slc)) {
				result.append("_").append(slc);
			} else {
				result.append(s);
			}
		}
		return result.toString();
	}
	
	/**
	 * camelCase -> nagybetűs aláhúzásos név
	 * 
	 * @see #camelCaseToUnderscore
	 */
	public static String camelCaseToUnderscoreBig(final String str) {
	
		if (StringUtils.isBlank(str)) {
			return str;
		}
		
		return camelCaseToUnderscore(str).toUpperCase();
	}
	
	/**
	 * aláhúzásos név (nagybetűs vagy kisbetűs) -> camelCase (lower camel)
	 */
	public static String underscoreToCamelCase(final String str) {
		
		if (StringUtils.isBlank(str)) {
			return str;
		}
		
		return CaseFormat.LOWER_UNDERSCORE.converterTo(CaseFormat.LOWER_CAMEL).convert(str.toLowerCase());
	}
	
	/**
	 * aláhúzásos név (nagybetűs vagy kisbetűs) -> CamelCase (nagybetű kezdőbetűként)
	 */
	public static String underscoreToCamelCaseBig(final String str) {
		
		if (StringUtils.isBlank(str)) {
			return str;
		}
		
		return CaseFormat.LOWER_UNDERSCORE.converterTo(CaseFormat.UPPER_CAMEL).convert(str.toLowerCase());
	}
	
	/**
	 * camelCase -> kisbetűs kötőjeles (pl.: "valami-nev")
	 */
	public static String toHyphen(final String str) {
		
		if (StringUtils.isBlank(str)) {
			return str;
		}
		
		final String result = camelCaseToUnderscore(str).toUpperCase();
		return CaseFormat.UPPER_UNDERSCORE.converterTo(CaseFormat.LOWER_HYPHEN).convert(result);
	}

	/**
	 * többféle átalakítás: ékezetek le, kisbetűsé tétel, szóköz kiszedése...
	 * 
	 * @param s
	 * @param whiteSpaceReplace
	 * @return
	 */
	public static String convertToUltraSafe(String s, String whiteSpaceReplace) {

		if (whiteSpaceReplace == null) {
			whiteSpaceReplace = "-";
		}
		
		s = s.trim();

		s = Normalizer.normalize(s, Normalizer.Form.NFD);
		s = s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
		
		s = s.toLowerCase();
		
		s = s.replaceAll("[^a-z0-9]", " ");

		s = StringUtils.normalizeSpace(s);
		
		s = s.replaceAll("\\s+", whiteSpaceReplace);
		
		return s;

	}

}
