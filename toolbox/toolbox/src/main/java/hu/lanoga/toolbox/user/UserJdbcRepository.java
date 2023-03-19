package hu.lanoga.toolbox.user;

import java.util.List;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.teamunify.i18n.I;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.exception.ManualValidationException;
import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.util.ToolboxAssert;

/**
 * a "system" user nincs benne a normál findOne/findAll hívások eredményében...
 * lásd a külön {@link #findSystemUser()} hivást
 */
@ConditionalOnMissingBean(name = "userJdbcRepositoryOverrideBean")
@Repository
public class UserJdbcRepository extends DefaultJdbcRepository<User> {

	@Override
	public String getInnerSelect() {
		return getInnerSelectNoExclude() + " WHERE au.username <> '" + ToolboxSysKeys.UserAuth.SYSTEM_USERNAME + "' AND au.username <> '" + ToolboxSysKeys.UserAuth.LCU_SYSTEM_USERNAME + "' AND au.username <> '" + ToolboxSysKeys.UserAuth.ANONYMOUS_USERNAME + "'";
	}
	
	public String getInnerSelectNoExclude() {

		// az anonymous user miatt kell a LEFT JOIN az INNER JOIN helyett

		return "SELECT au.*, aui.title, aui.job_title, aui.date_of_birth, aui.given_name, aui.family_name, aui.email, aui.phone_number FROM auth_user au "
				+ "LEFT JOIN auth_user_info aui ON au.id = aui.user_id";
	}

	/**
	 * admin user-ek listáját adja vissza, csak enabled userek
	 * 
	 * @return
	 */
	public List<User> findAdminUsers() {
		final SqlParameterSource namedParameters = new MapSqlParameterSource();	
		return namedParameterJdbcTemplate.query(fillVariables("SELECT * FROM (" + getInnerSelect() + ") insel WHERE #tenantCondition AND enabled = true AND user_roles @> '" + ToolboxSysKeys.UserAuth.ROLE_ADMIN_CS_ID + "'"), namedParameters, newRowMapperInstance());
	}

	/**
	 * admin user id-k listáját adja vissza, csak enabled userek
	 * 
	 * @return
	 */
	public List<Integer> findAdminUserIds() {

		final SqlParameterSource namedParameters = new MapSqlParameterSource();

		try {
			return namedParameterJdbcTemplate.queryForList(fillVariables("SELECT id FROM (" + getInnerSelect() + ") insel WHERE #tenantCondition AND enabled = true AND user_roles @> '" + ToolboxSysKeys.UserAuth.ROLE_ADMIN_CS_ID + "'"), namedParameters, Integer.class);
		} catch (final IncorrectResultSizeDataAccessException e) {
			return null;
		}
	}

	/**
	 * csak enabled userek
	 * 
	 * @return
	 */
	public List<User> findUsersByRole(int userRoleId) {
		final SqlParameterSource namedParameters = new MapSqlParameterSource();
		return namedParameterJdbcTemplate.query(fillVariables("SELECT * FROM (" + getInnerSelect() + ") insel WHERE tenant_id = #tenantParam AND enabled = true AND user_roles @> '" + userRoleId + "'"), namedParameters, newRowMapperInstance());
	}

	@Override
	public <S extends User> S save(S entity, Set<String> leaveOutFields, boolean findAfter) {

		String t = entity.getUsername().toLowerCase();

		if (entity.isNew() && t.startsWith("jump.")) {
			SecurityUtil.limitAccessSystem();
		}

		if (entity.isNew()) {
			
			if (t.equals(ToolboxSysKeys.UserAuth.ANONYMOUS_USERNAME) ||
					t.equals(ToolboxSysKeys.UserAuth.SYSTEM_USERNAME) ||
					t.equals(ToolboxSysKeys.UserAuth.LCU_SYSTEM_USERNAME)
					) {
				throw new ManualValidationException("Reserved username!", I.trc("Error", "Reserved username!"));
			}
						
		}

		return super.save(entity, leaveOutFields, findAfter);
	}

	// --------------------------------------------------------------------------------------------------------------------------
	// "kézi" tenant paraméteres metódusok és tenant param nélküli metódusok (ami "mindenben" keres):

	public User findUser(final int tenantId, final String username) {

		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("username", username);

		try {
			return namedParameterJdbcTemplate.queryForObject("SELECT * FROM (" + getInnerSelect() + ") insel WHERE insel.tenant_id = :tenantId AND insel.username = :username", namedParameters, newRowMapperInstance());
		} catch (final IncorrectResultSizeDataAccessException e) {
			return null;
		}
	}

	public User findUser(final int tenantId, final int userId) {

		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("userId", userId);

		try {
			return namedParameterJdbcTemplate.queryForObject("SELECT * FROM (" + getInnerSelect() + ") insel WHERE insel.tenant_id = :tenantId AND insel.id = :userId", namedParameters, newRowMapperInstance());
		} catch (final IncorrectResultSizeDataAccessException e) {
			return null;
		}
	}

	public User findUser(final String tenantName, final String username) {

		ToolboxAssert.notNull(tenantName);

		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("name", tenantName).addValue("username", username);

		try {
			return namedParameterJdbcTemplate.queryForObject("SELECT insel.* FROM (" + getInnerSelect() + ") insel INNER JOIN tenant ON insel.tenant_id = tenant.id WHERE tenant_id = (SELECT id FROM tenant WHERE name = :name) AND username = :username", namedParameters, newRowMapperInstance());
		} catch (final IncorrectResultSizeDataAccessException e) {
			return null;
		}
	}

	/**
	 * tenant nélkül kikeresi a user-t; 
	 * nem enabled tenant-okban és user-ekben is keres 
	 * (a "ha több mint 1 találat volt" így értendő)
	 * 
	 * @param username
	 * @return
	 * 		ha több mint 1 találat volt a username-re, akkor null-t ad vissza (csak egyedi user esetén működik)
	 */
	public User findUserWithoutTenantId(final String username) {

		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("username", username);

		try {
			return namedParameterJdbcTemplate.queryForObject("SELECT * FROM (" + getInnerSelect() + ") insel WHERE insel.username = :username", namedParameters, newRowMapperInstance());
		} catch (final IncorrectResultSizeDataAccessException e) {
			return null;
		}
	}
	
	/**
	 * {tools.tenant.allow-login-without-tenant-id} property esetén felhasznált query, 
	 * ami tenant nélkül kikeresi a user-t; 
	 * csak enabled tenant-okban és user-ekben keres 
	 * (a "ha több mint 1 találat volt" így értendő)
	 * 
	 * @param username
	 * @return
	 * 		ha több mint 1 találat volt a username-re, akkor null-t ad vissza (csak egyedi user esetén működik)
	 */
	public User findUserWithoutTenantId2(final String username) {
				
		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("username", username);
		
		try {
			return namedParameterJdbcTemplate.queryForObject("SELECT insel.* FROM (" + getInnerSelect() + ") insel INNER JOIN tenant ta ON ta.id = insel.tenant_id WHERE insel.username = :username AND insel.enabled = TRUE AND ta.enabled = TRUE", namedParameters, newRowMapperInstance());
		} catch (final IncorrectResultSizeDataAccessException e) {
			return null;
		}
	}

	public User findUserWithoutTenantId(final int userId) {
		
		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("id", userId);

		try {
			return namedParameterJdbcTemplate.queryForObject("SELECT * FROM (" + getInnerSelect() + ") insel WHERE insel.id = :id", namedParameters, newRowMapperInstance());
		} catch (final IncorrectResultSizeDataAccessException e) {
			return null;
		}
	}
	
	/**
	 * az "anonymous" user csak ezen a metóduson keresztül érhető el
	 * 
	 * @return
	 */
	public User findAnonymousUser() {
		try {
			return namedParameterJdbcTemplate.queryForObject("SELECT * FROM (" + getInnerSelectNoExclude() + ") insel WHERE tenant_id = " + ToolboxSysKeys.UserAuth.COMMON_TENANT_ID + " AND username = '" + ToolboxSysKeys.UserAuth.ANONYMOUS_USERNAME + "'", new MapSqlParameterSource(), newRowMapperInstance());
		} catch (final IncorrectResultSizeDataAccessException e) {
			return null;
		}
	}

	/**
	 * a "system" user csak ezen a metóduson keresztül érhető el
	 * 
	 * @return
	 */
	public User findSystemUser() {
		try {
			return namedParameterJdbcTemplate.queryForObject("SELECT * FROM (" + getInnerSelectNoExclude() + ") insel WHERE tenant_id = " + ToolboxSysKeys.UserAuth.COMMON_TENANT_ID + " AND username = '" + ToolboxSysKeys.UserAuth.SYSTEM_USERNAME + "'", new MapSqlParameterSource(), newRowMapperInstance());
		} catch (final IncorrectResultSizeDataAccessException e) {
			return null;
		}
	}
	
	/**
	 * a "lcu-system" user csak ezen a metóduson keresztül érhető el
	 * 
	 * @return
	 */
	public User findLcuSystemUser() {
		try {
			return namedParameterJdbcTemplate.queryForObject("SELECT * FROM (" + getInnerSelectNoExclude() + ") insel WHERE tenant_id = " + ToolboxSysKeys.UserAuth.LCU_TENANT_ID + " AND username = '" + ToolboxSysKeys.UserAuth.LCU_SYSTEM_USERNAME + "'", new MapSqlParameterSource(), newRowMapperInstance());
		} catch (final IncorrectResultSizeDataAccessException e) {
			return null;
		}
	}

	/**
	 * {@link User} keresése email alapján
	 * (tenant nélkül/függtelenül keres (tehát minden tenant játszik))
	 *
	 * @param email
	 * @return
	 */
	public List<User> findUsersByEmail(final String email) {
		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("email", email);
		return namedParameterJdbcTemplate.query(fillVariables("SELECT * FROM (" + getInnerSelect() + ") insel WHERE email = :email", false), namedParameters, newRowMapperInstance());
	}

	/**
	 * óvatosan, mert itt tenant mentesen keres (mert minden tenant-ban meg kell találnia a testvér/avatar felhasználóit)... 
	 * maga a parent user is bekerül a listába!
	 *
	 * @return
	 * 
	 * @see ToolboxSysKeys.UserAuth#ROLE_TENANT_OVERSEER_STR
	 */
	public List<User> findAllByParentId(final Integer parentId) {
		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("parentId", parentId);
		return namedParameterJdbcTemplate.query(fillVariables("SELECT * FROM (" + getInnerSelect() + ") inse WHERE id = :parentId OR parent_id = :parentId", false), namedParameters, newRowMapperInstance());
	}

}