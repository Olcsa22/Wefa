package hu.lanoga.toolbox.json.orgjson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;

import hu.lanoga.toolbox.json.jackson.JacksonHelper;

/**
 * JSON library helper (org.json)
 * 
 * @see hu.lanoga.toolbox.json.jackson.JacksonHelper
 * @see hu.lanoga.toolbox.json.gson.GsonHelper
 */
public final class OrgJsonUtil {
	
	
	public OrgJsonUtil() {
		//
	}

	public static JSONArray jsonArrayIntegerOrder(JSONArray ja, final boolean desc) {
		
		final List<Integer> collection = new ArrayList<>(JacksonHelper.jsonArrayIntegerToCollection(ja.toString()));
		Collections.sort(collection);
		
		if (desc) {
			Collections.reverse(collection);
		}
		
		return new JSONArray(collection);
		
	}
}
