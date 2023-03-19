package hu.lanoga.toolbox.user;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;

@ConditionalOnMissingBean(name = "userPasswordJdbcRepositoryOverrideBean")
@Repository
public class UserPasswordJdbcRepository extends DefaultJdbcRepository<UserPassword> {

	// --------------------------------------------------------------------------------------------------------------------------
	// "kézi" tenant paraméteres metódusok:

	public UserPassword findUserPassword(int tenantId, int userId) {

		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("tenant_id", tenantId).addValue("user_id", userId);

		// try { // itt kívételesen az a jobb, ha exception-t dob
		return namedParameterJdbcTemplate.queryForObject("SELECT * FROM auth_user_password WHERE tenant_id = :tenant_id AND user_id = :user_id", namedParameters, newRowMapperInstance());
		// } catch (final IncorrectResultSizeDataAccessException e) {
		// return null;
		// }

	}

	public void updateUserPasswordByUserId(final int userId, final String password) {

		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("user_id", userId).addValue("password", password);

		namedParameterJdbcTemplate.update("UPDATE auth_user_password SET  password =:password WHERE user_id = :user_id", namedParameters);
	}

}
