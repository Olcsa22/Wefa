package hu.lanoga.toolbox.json.gson;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import hu.lanoga.toolbox.exception.ToolboxGeneralException;

/**
 * JSON library helper (Gson)
 * 
 * @see hu.lanoga.toolbox.json.jackson.JacksonHelper
 * @see hu.lanoga.toolbox.json.orgjson.OrgJsonUtil
 */
public final class GsonHelper {
	
	// Gson másfajta annotációkkal dolgozik, mint a Jackson
	// tehát az a fajta @JsonIgnore stb. nem hat rá (ami egyes esetekben előny is lehet)
	// https://www.baeldung.com/gson-exclude-fields-serialization

	private static Gson gson = new GsonBuilder().create();

	private GsonHelper() {
		//
	}

	public static <T> T fromJson(final String jsonStr, final Class<T> targetType) {
		
		if (StringUtils.isBlank(jsonStr)) {
			return null;
		}

		try {
			return gson.fromJson(jsonStr, targetType);
		} catch (final Exception e) {
			throw new ToolboxGeneralException(e);
		}

	}

	public static String toJson(final Object o) {

		if (o == null) {
			return null;
		}

		try {
			return gson.toJson(o);
		} catch (final Exception e) {
			throw new ToolboxGeneralException(e);
		}

	}

	public static Object deepCopy(final Object o, final Class<?> clazz) {
		return fromJson(toJson(o), clazz);
	}

}
