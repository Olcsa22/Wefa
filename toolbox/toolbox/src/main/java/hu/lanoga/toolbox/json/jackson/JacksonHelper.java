package hu.lanoga.toolbox.json.jackson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;

import com.fasterxml.jackson.databind.ObjectMapper;

import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;

/**
 * JSON "oda-vissza" (Spring context kell hozzá!)
 * 
 * @see hu.lanoga.toolbox.json.orgjson.OrgJsonUtil
 * @see hu.lanoga.toolbox.json.gson.GsonHelper
 */
public final class JacksonHelper {

	private JacksonHelper() {
		//
	}

	private static ObjectMapper getObjectMapper() {
		
		return ApplicationContextHelper.getBean(ObjectMapper.class);

		// ObjectMapper objectMapper = new ObjectMapper()
		// .configure(org.codehaus.jackson.map.DeserializationConfig.Feature.USE_ANNOTATIONS, false)
		// .configure(org.codehaus.jackson.map.SerializationConfig.Feature.USE_ANNOTATIONS, false);
		// return objectMapper;
	}

	public static <T> T fromJson(final String jsonStr, final Class<T> targetType) {

		if (StringUtils.isBlank(jsonStr)) {
			return null;
		}

		try {
			return getObjectMapper().readValue(jsonStr, targetType);
		} catch (final IOException e) {
			throw new ToolboxGeneralException(e);
		}

	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> fromJsonToMap(final String jsonStr) {

		// try {
		// final ObjectMapper mapper = ApplicationContextHelper.getBean(ObjectMapper.class);
		// final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
		// };
		// return mapper.readValue(jsonStr, typeRef);
		// } catch (final Exception e) {
		// throw new ToolboxGeneralException("jsonObjectToMap failed!", e);
		// }

		return fromJson(jsonStr, Map.class);

	}

	public static String toJson(final Object o) {

		if (o == null) {
			return null;
		}

		try {
			return getObjectMapper().writeValueAsString(o);
		} catch (final IOException e) {
			throw new ToolboxGeneralException(e);
		}

	}

	public static Object deepCopy(final Object o, final Class<?> clazz) {
		return fromJson(toJson(o), clazz);
	}

	/**
	 * (nem Jackson, hanem org.json)
	 * 
	 * @param jsonArrayString
	 * @return
	 */
	public static Collection<String> jsonArrayStringToList(final String jsonArrayString) {
		final Collection<String> collection = new ArrayList<>();

		if (StringUtils.isNotBlank(jsonArrayString)) {

			final JSONArray jsonArray = new JSONArray(jsonArrayString);

			for (int i = 0; i < jsonArray.length(); i++) {
				final String value = jsonArray.getString(i);
				collection.add(value);
			}

			return collection;

		} else {
			return new ArrayList<>();
		}
	}

	/**
	 * (nem Jackson, hanem org.json)
	 * 
	 * @param jsonArrayString
	 * @return
	 * 		soha nem null (üres lehet)
	 */
	public static Collection<Integer> jsonArrayIntegerToCollection(final String jsonArrayString) {

		final Collection<Integer> collection = new ArrayList<>();

		if (StringUtils.isNotBlank(jsonArrayString)) {

			final JSONArray jsonArray = new JSONArray(jsonArrayString);

			for (int i = 0; i < jsonArray.length(); i++) {
				final Integer value = jsonArray.getInt(i);
				collection.add(value);
			}

			return collection;

		} else {
			return new ArrayList<>();
		}
	}

	/**
	 * @param jsonArrayString
	 * 		int elemek tartalmazó JSON array
	 * @param desc
	 * 		true esetén csökkenő, false esetén növekvő
	 * @return
	 * 		ugyanaz a JSON array, csak sorrendbe rendezve a számok
	 */
	public static String jsonArrayIntegerOrder(final String jsonArrayString, final boolean desc) {

		if (StringUtils.isBlank(jsonArrayString)) {
			return jsonArrayString;
		}

		final List<Integer> collection = new ArrayList<>(jsonArrayIntegerToCollection(jsonArrayString));
		Collections.sort(collection);
		
		if (desc) {
			Collections.reverse(collection);
		}

		return toJson(collection);

	}

}
