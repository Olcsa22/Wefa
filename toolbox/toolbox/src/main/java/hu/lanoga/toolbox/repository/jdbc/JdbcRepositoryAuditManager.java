package hu.lanoga.toolbox.repository.jdbc;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import hu.lanoga.toolbox.session.LcuHelperSessionBean;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.user.User;
import hu.lanoga.toolbox.user.UserJdbcRepository;
import hu.lanoga.toolbox.util.ToolboxAssert;

public class JdbcRepositoryAuditManager {

	// ========================================================================

	private static ThreadLocal<AuditModel> tlAuditModel = new ThreadLocal<>();

	/**
	 * {@link #setAuditFields(Map, boolean, boolean)}-hez opcionális override lehetőség... 
	 * (created_by/modified_by/created_on/modified_on column-okhoz) 
	 * 
	 * (amennyiben {@link JdbcRepositoryTenantManager#setTlTenantId(int)} is van használatban, 
	 * akkor a kettő közül az kell előbb meghívni!) 
	 * 
	 * @param auditModel
	 * 		az override... 
	 * 
	 * 		csak ugyanazon tenant-ban lévő user-t lehet beállítani (a belépett userhez képest nézve) 
	 * 		(vagy a {@link JdbcRepositoryTenantManager#setTlTenantId(int)} tenant alá kell tartoznia})... 
	 * 
	 * 		lehet csak egy dolgot felülírni (ami null a {@link AuditModel} objektumon belül 
	 * 		az nem lesz felülírva, a szokásos marad (belépett user, mostani aktuális dátum)
	 * 
	 * @see ThreadLocal
	 */
	public static void setTlAuditModel(final AuditModel auditModel) {

		if (auditModel.getAuditUserId() != null) {
			final User auditUser = ApplicationContextHelper.getBean(UserJdbcRepository.class).findOne(auditModel.getAuditUserId());
			SecurityUtil.limitAccessSameTenant(auditUser.getTenantId());
		}
		// ---

		tlAuditModel.set(auditModel);
	}

	/**
	 * {@link ThreadLocal} alapú (bővebb leírásért lásd a setter párját)
	 * 
	 * @return 
	 * 		null esetén nincs beállítva ThreadLocal alapú forced, override érték
	 * 
	 * @see #setTlAuditModel(int)
	 */
	public static void clearTlAuditModel() {
		tlAuditModel.remove();
	}

	/**
	 * {@link ThreadLocal} alapú (bővebb leírásért lásd a setter párját)
	 * 
	 * @see #setTlAuditModel(int)
	 */
	public static AuditModel getTlAuditModel() {
		return tlAuditModel.get();
	}

	// ========================================================================

	static void setAuditFields(final Map<String, Object> columns, final boolean setCreate, final boolean setModify) {

		ToolboxAssert.isTrue(!columns.isEmpty());
		ToolboxAssert.isTrue(setCreate || setModify);

		// ---

		AuditModel auditModel = tlAuditModel.get();

		if (auditModel == null) {
			auditModel = new AuditModel();
		}

		if (auditModel.getAuditUserId() == null) {
			auditModel.setAuditUserId(SecurityUtil.getLoggedInUser().getId());
		}

		if (auditModel.getAuditTs() == null) {
			auditModel.setAuditTs(JdbcRepositoryManager.getNowTs());
		}

		// ---

		if (setCreate) {

			if (columns.containsKey("created_by")) {
				columns.put("created_by", auditModel.getAuditUserId());
			}

			if (columns.containsKey("lcu_gid") && SecurityUtil.isLcuLevelUser()) {

				final String lcuGid = LcuHelperSessionBean.specialLcuGidRetrieve();
				
				ToolboxAssert.isTrue(StringUtils.isNotBlank(lcuGid));
				
				columns.put("lcu_gid", lcuGid);

			}

			if (columns.containsKey("created_on")) {
				columns.put("created_on", auditModel.getAuditTs());
			}

		}

		if (setModify) {

			if (columns.containsKey("modified_by")) {
				columns.put("modified_by", auditModel.getAuditUserId());
			}

			if (columns.containsKey("modified_on")) {
				columns.put("modified_on", auditModel.getAuditTs());
			}

		}

	}

	JdbcRepositoryAuditManager() {
		//
	}

}
