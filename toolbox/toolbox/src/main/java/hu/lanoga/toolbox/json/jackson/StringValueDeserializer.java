package hu.lanoga.toolbox.json.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class StringValueDeserializer extends JsonDeserializer<String> {

	@Override
	public String deserialize(final JsonParser jsonParser, final DeserializationContext ctxt) throws IOException, JsonParseException {
				
		final TreeNode tree = jsonParser.getCodec().readTree(jsonParser);
		return tree.toString();

	}

}