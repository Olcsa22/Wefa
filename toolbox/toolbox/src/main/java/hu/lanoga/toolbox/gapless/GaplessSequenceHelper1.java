package hu.lanoga.toolbox.gapless;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * postgres table lock alapú megoldás... 
 * 
 * a) költséges 
 * b) megbízható, még több Java server esetén is (cluster stb.) 
 * c) csak akkor működik, ha {@link Transactional} közegben van 
 */
@Service
public class GaplessSequenceHelper1 {

	/**
	 * thread-safe...
	 */
	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@Autowired
	protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	/**
	 * @return
	 * 
	 * @see #incrementAndGetSequenceValue()
	 */
	public int incrementAndGetSequenceValueInt(final String lockTableName, final String rowName) {
		return Math.toIntExact(this.incrementAndGetSequenceValue(lockTableName, rowName));
	}

	/**
	 * @param lockTableName
	 * 		nem SQL inject védett (!), ne user inputból legyen
	 * @param rowName
	 * @return
	 */
	public long incrementAndGetSequenceValue(final String lockTableName, final String rowName) {

		this.jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS gapless");
		this.jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS gapless." + lockTableName + " (n varchar(50), v bigint);");

		this.jdbcTemplate.execute("LOCK TABLE gapless." + lockTableName + " IN ACCESS EXCLUSIVE MODE;");

		long currentValue = 0L;
		boolean doInsert = false;

		try {
			final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("rowName", rowName);
			currentValue = this.namedParameterJdbcTemplate.queryForObject("SELECT v from gapless." + lockTableName + " WHERE n = :rowName", namedParameters, Long.class);
		} catch (final IncorrectResultSizeDataAccessException e) {
			doInsert = true;
		}

		final long incrementedValue = currentValue + 1;

		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("rowName", rowName).addValue("incrementedValue", incrementedValue);
		if (doInsert) {
			this.namedParameterJdbcTemplate.update("INSERT INTO gapless." + lockTableName + " (n, v) VALUES (:rowName, :incrementedValue)", namedParameters);
		} else {
			this.namedParameterJdbcTemplate.update("UPDATE gapless." + lockTableName + " SET v = :incrementedValue WHERE n = :rowName", namedParameters);
		}

		return incrementedValue;

	}

}
