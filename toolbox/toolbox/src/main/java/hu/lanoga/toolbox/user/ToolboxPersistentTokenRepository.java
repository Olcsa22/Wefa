package hu.lanoga.toolbox.user;

import java.util.Date;

import org.simpleflatmapper.jdbc.spring.JdbcTemplateMapperFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Repository;

import hu.lanoga.toolbox.spring.SecurityUtil;

/**
 * Spring Security persistent_login ("maradjak bejelentkezve" funkció, a szokásos Spring "módszer")...
 */
@ConditionalOnMissingBean(name = "toolboxPersistentTokenRepositoryOverrideBean")
@Repository
public class ToolboxPersistentTokenRepository implements PersistentTokenRepository {

	@Autowired
	protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	/**
	 * beszúrja a persistent_login táblába...
	 */
	@Override
	public void createNewToken(PersistentRememberMeToken persistentRememberMeToken) {
			
		final String usernameWithTenantId = SecurityUtil.getLoggedInUser().getTenantId() + ":" + persistentRememberMeToken.getUsername();
		
		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("series", persistentRememberMeToken.getSeries()).addValue("tokenValue", persistentRememberMeToken.getTokenValue()).addValue("date", persistentRememberMeToken.getDate()).addValue("username", usernameWithTenantId);
		namedParameterJdbcTemplate.update("INSERT INTO persistent_login (series, token_value, date, username) VALUES (:series, :tokenValue, :date, :username)", namedParameters);
	}

	@Override
	public void updateToken(String series, String tokenValue, Date date) {		
		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("series", series).addValue("tokenValue", tokenValue).addValue("date", date);
		namedParameterJdbcTemplate.update("UPDATE persistent_login SET token_value = :tokenValue, date = :date WHERE series = :series", namedParameters);
	}

	@Override
	public PersistentRememberMeToken getTokenForSeries(String series) {
		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("series", series);
		try {
			return namedParameterJdbcTemplate.queryForObject("SELECT * FROM persistent_login WHERE series = :series", namedParameters, JdbcTemplateMapperFactory.newInstance().newRowMapper(ToolboxPersistentRememberMeToken.class));
		} catch (final IncorrectResultSizeDataAccessException e) {
			return null;
		}
	}

	@Override
	public void removeUserTokens(String username) {
		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("username", username);
		namedParameterJdbcTemplate.update("DELETE FROM persistent_login WHERE username = :username", namedParameters);
	}

}
