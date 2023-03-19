package hu.lanoga.toolbox.repository.jdbc;

import java.util.LinkedHashMap;
import java.util.Set;

import org.springframework.jdbc.core.RowMapper;

/**
 * {@link RowMapper} ihlette, fordított irány...
 * 
 * @param <T>
 */
public interface RowUnmapper<T> {

	/**
	 * @param t
	 * @param leaveOutFields
	 *            Java osztály mezőnevét kell megadni (camelCase)... (null esetén nincs alkalmazva (értsd nincs kihagyva e miatt semmi))
	 * @param isInsert
	 * @return
	 */
	LinkedHashMap<String, Object> mapColumns(T t, Set<String> leaveOutFields, boolean isInsert); // fontos, hogy sorrend tartó legyen a visszaadott map (ezért nem sima map van az interface-ben sem)

}
