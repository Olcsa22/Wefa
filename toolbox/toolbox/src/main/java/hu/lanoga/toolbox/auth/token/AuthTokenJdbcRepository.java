package hu.lanoga.toolbox.auth.token;

import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@ConditionalOnMissingBean(name = "authTokenJdbcRepositoryOverrideBean")
@Repository
public class AuthTokenJdbcRepository extends DefaultJdbcRepository<AuthToken> {

	// --------------------------------------------------------------------------------------------------------------------------
	// "kézi" tenant paraméteres metódusok:

	/**
	 * token alapján visszaadja az AuthToken-t (tenant nélkül keres)
	 *
	 * @param token
	 * @return
	 */
	public AuthToken findByToken(String token) {
		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("token", token);

		return namedParameterJdbcTemplate.queryForObject(fillVariables("SELECT t.* FROM auth_token t WHERE t.token = :token", false), namedParameters, newRowMapperInstance());

	}
}
