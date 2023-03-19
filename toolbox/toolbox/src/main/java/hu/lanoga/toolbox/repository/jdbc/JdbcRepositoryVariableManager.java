package hu.lanoga.toolbox.repository.jdbc;

import org.apache.commons.lang3.StringUtils;

import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.user.UserKeyValueSettings;
import hu.lanoga.toolbox.util.ToolboxAssert;

public class JdbcRepositoryVariableManager<T extends ToolboxPersistable> {
	
	private final Class<T> entityType;
	private final JdbcRepositoryTenantManager jdbcRepositoryTenantManager;
	
	JdbcRepositoryVariableManager(final Class<T> entityType, final JdbcRepositoryTenantManager jdbcRepositoryTenantManager) {
		this.entityType = entityType;
		this.jdbcRepositoryTenantManager = jdbcRepositoryTenantManager;
	}
	
	/**
	 * #lang, #loggedInUserId stb.
	 * 
	 * @param sql
	 * @param doFillTenantVariables
	 * @return
	 */
	public String fillVariables(final String sql, final boolean doFillTenantVariables) {

		ToolboxAssert.isTrue(StringUtils.isNotBlank(sql));

		final String pattern1 = "(.*)(\\s+)drop(\\s+)(.*)";
		final String pattern2 = "^drop(\\s+)(.*)";

		if (sql.toLowerCase().matches(pattern1) || sql.toLowerCase().matches(pattern2)) {
			throw new JdbcRepositoryException("Invalid operation (drop)!");
		}

		String retSql = sql;

		if (!this.entityType.equals(UserKeyValueSettings.class)) { // egyébként itt stack overflow lenne (a I18nUtil is épít a UserKeyValueSettings-re...)

			if (retSql.contains("#lang")) {

				final String l1;
				final String l2 = I18nUtil.getServerLocale().getLanguage().substring(0, 2);

				if (SecurityUtil.hasLoggedInUser()) {
					l1 = I18nUtil.getLoggedInUserLocale().getLanguage().substring(0, 2);
				} else {
					l1 = l2;
				}

				retSql = StringUtils.replace(retSql, "#lang1", l1);
				retSql = StringUtils.replace(retSql, "#lang2", l2);
			}

		}

		if (retSql.contains("#loggedInUserId")) {
			retSql = StringUtils.replace(retSql, "#loggedInUserId", SecurityUtil.getLoggedInUser().getId().toString());
		}

		if (doFillTenantVariables) {
			retSql = this.jdbcRepositoryTenantManager.fillTenantId(retSql);
		}

		return retSql;
	}

}
