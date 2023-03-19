package hu.lanoga.toolbox.repository.jdbc;

import org.apache.commons.lang3.StringUtils;

import hu.lanoga.toolbox.ToolboxSysKeys.RepositoryTenantMode;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
public class JdbcRepositoryTenantManager {


	/**
	 * @see JdbcRepositoryManager#JdbcRepositoryManager(Class, RepositoryTenantMode)
	 */
	static final String DEFAULT_TENANT_ID_COLUMN_NAME = "tenant_id";

	// ========================================================================

	private static ThreadLocal<Integer> tlTenantId = new ThreadLocal<>();

	/**
	 * {@link ThreadLocal} alapú érték beállítása, pl. job futtásnál kell... 
	 * 
	 * (fontos, hogy rögtön az összes repository-ra vontakozik...) 
	 * 
	 * @param tenantId
	 */
	public static void setTlTenantId(final int tenantId) {
		tlTenantId.set(tenantId);
	}

	/**
	 * {@link ThreadLocal} alapú (bővebb leírásért lásd a setter párját)
	 * 
	 * @see #setTlTenantId(int)
	 */
	public static void clearTlTenantId() {
		tlTenantId.remove();
	}

	/**
	 * {@link ThreadLocal} alapú (bővebb leírásért lásd a setter párját)
	 * 
	 * @return 
	 * 		null esetén nincs beállítva {@link ThreadLocal} érték
	 * 
	 * @see #setTlTenantId(int)
	 */
	public static Integer getTlTenantId() {
		return tlTenantId.get();
	}

	// ========================================================================

	private final RepositoryTenantMode tenantMode;
	private final String tenantIdColumnName;

	JdbcRepositoryTenantManager(final RepositoryTenantMode tenantMode, final String tenantIdColumnName) {

		this.tenantMode = tenantMode;
		this.tenantIdColumnName = tenantIdColumnName;

	}

	String fillTenantId(final String sql) { // TODO: lehet, hogy kellene itt is egy intent (írás vs olvasás)

		if (RepositoryTenantMode.NO_TENANT.equals(this.tenantMode)) {

			return this.fillTenantIdNoTenantMode(sql);

		} else if (RepositoryTenantMode.DEFAULT.equals(this.tenantMode)) {

			return this.fillTenantIdDefaultMode(sql);

		} else {
			throw new JdbcRepositoryException("Unimplemented tenant mode (should never go into this branch)!");
		}

	}

	private String fillTenantIdNoTenantMode(final String sql) {
		if (sql.contains("#tenantParam")) {
			throw new JdbcRepositoryException("#tenantParam is not allowed in NO_TENANT mode");
		}

		return StringUtils.replace(sql, "#tenantCondition", " (1 = 1) ");
	}

	private String fillTenantIdDefaultMode(final String sql) {

		final boolean b1 = sql.contains("#tenantParam");
		final boolean b2 = sql.contains("#tenantCondition");

		if (!((b1 && !b2) || (!b1 && b2))) {
			throw new JdbcRepositoryException("Missing tenant placeholder (#tenantParam xor #tenantCondition is required)!"); // tényleg xor (vagy az egyik vagy a másik)
		}

		final int tenantIdForTheSql;
		final Integer tl = getTlTenantId(); // lehetséges override érték, például jobokhoz

		if (tl != null) {

			tenantIdForTheSql = tl;

		} else {

			final ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();
			tenantIdForTheSql = loggedInUser.getTenantId();

		}

		String retSql = sql;

		if (b1) {
			retSql = StringUtils.replace(retSql, "#tenantParam",
					Integer.toString(tenantIdForTheSql)); // insertekhez kell
		}

		if (b2) {
			retSql = StringUtils.replace(retSql, "#tenantCondition",
					" (" + this.tenantIdColumnName + " = " + tenantIdForTheSql + ") "); // select, update, delete where részébe kell
		}

		return retSql;

	}

}
