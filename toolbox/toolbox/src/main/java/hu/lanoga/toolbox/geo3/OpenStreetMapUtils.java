package hu.lanoga.toolbox.geo3;

import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import hu.lanoga.toolbox.exception.ToolboxGeneralException;

public class OpenStreetMapUtils {

	private OpenStreetMapUtils() {
		//
	}

	/**
	 * Cím (pl.: "Budapest, Hungary") lat/long koordináta lekérdezése... 
	 * (HTTP request a nominatim.openstreetmap.org-ra)
	 * 
	 * @param address
	 * @return null esetén nincs találat, egyéb Lat (left) és Lon (right)
	 */
	public static Pair<Double, Double> getCoordinates(final String address) {

		final String[] split = address.split(" ");

		if (split.length == 0) {
			return null;
		}

		final StringBuilder sb = new StringBuilder();

		sb.append("https://nominatim.openstreetmap.org/search?q=");

		for (int i = 0; i < split.length; i++) {
			sb.append(split[i]);
			if (i < (split.length - 1)) {
				sb.append("+");
			}
		}
		sb.append("&format=json&addressdetails=1");

		HttpResponse<JsonNode> httpResponse = null;
		try {

			httpResponse = Unirest.get(sb.toString()).asJson();

		} catch (final Exception e) {
			throw new ToolboxGeneralException("Error when trying to get data with the following query " + sb.toString());
		}

		if ((httpResponse == null) || (httpResponse.getBody() == null) || !httpResponse.getBody().isArray()) {
			return null;
		}

		final JSONArray array = httpResponse.getBody().getArray();
		if (array.length() == 0) {
			return null;
		}
		final JSONObject jsonObject = (JSONObject) array.get(0);

		final String lat = (String) jsonObject.get("lat");
		final String lon = (String) jsonObject.get("lon");
		
		return Pair.of(Double.parseDouble(lat), Double.parseDouble(lon));
		
	}
}