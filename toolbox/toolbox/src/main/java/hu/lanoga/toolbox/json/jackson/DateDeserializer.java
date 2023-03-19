package hu.lanoga.toolbox.json.jackson;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * JavaScript yyyy-MM-dd'T'HH:mm:ss.SSS'Z' formátumot helyesen dolgoz fel, akkor is, ha szerver időzónája nem UTC (értsd: nem UTC+0). 
 * Jelenleg nem kell sehol, a szerver fixen UTC-re van állítva általában.
 * 
 *  @deprecated
 */
@Deprecated
public class DateDeserializer extends JsonDeserializer<java.sql.Date> {

	@Override
	public java.sql.Date deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException, JsonParseException {

		try {
			final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			final java.util.Date parsed = format.parse(jp.getValueAsString());
			return new java.sql.Date(parsed.getTime());
		} catch (final ParseException e) {
			throw new JsonParseException(jp, "Date parsing error!", e);
		}

	}
}