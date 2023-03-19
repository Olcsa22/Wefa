package hu.lanoga.toolbox.file;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.file.remote.RemoteFile;
import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;

@ConditionalOnMissingBean(name = "fileDescriptorJdbcRepositoryOverrideBean")
@Repository
public class FileDescriptorJdbcRepository extends DefaultJdbcRepository<FileDescriptor> {
	
	private static ThreadLocal<Boolean> tlAllowCommontTenantFallbackLookup = new ThreadLocal<>();

	/**
	 * Security szempontból veszélyes, csak akkor használd, ha érted, hogy mi történi 
	 * (és a finally blokkodban ott a clear párja...)
	 * 
	 * @param allowCommontTenantLookup
	 */
	public static void setTlAllowCommontTenantFallbackLookup(final boolean allowCommontTenantLookup) {
		
		// SecurityUtil.limitAccessSystem();
		
		tlAllowCommontTenantFallbackLookup.set(allowCommontTenantLookup);
	}

	public static void clearTlAllowCommontTenantFallbackLookup() {
		tlAllowCommontTenantFallbackLookup.remove();
	}
 
	public static Boolean getTlAllowCommontTenantFallbackLookup() {
		return tlAllowCommontTenantFallbackLookup.get();
	}
	
	@Override
	public FileDescriptor findOne(final Integer id) {
		
		FileDescriptor fileDescriptor = super.findOne(id);
		
		if (fileDescriptor == null && Boolean.TRUE.equals(getTlAllowCommontTenantFallbackLookup())) {
			
			final Integer oldTlTenantId = JdbcRepositoryManager.getTlTenantId();
			try {
				JdbcRepositoryManager.setTlTenantId(1);
				fileDescriptor = super.findOne(id);
			} finally {
				if (oldTlTenantId == null) {
					JdbcRepositoryManager.clearTlTenantId();
				} else {
					JdbcRepositoryManager.setTlTenantId(oldTlTenantId);
				}
				
			}
		}
		
		return fileDescriptor;
	
	}

	/**
	 * {@code ToolboxSysKeys.FileDescriptorStatus.TO_BE_DELETED}, vagy {@code ToolboxSysKeys.FileDescriptorStatus.TEMPORARY};
	 * és tools.temp.file.preservation.time propertyből jövő napnál régebbi az utolsó módosítás dátuma;
	 * és nincs hozzá {@link RemoteFile}
	 * 
	 * (tenant független query)
	 *
	 * @return
	 */
	public List<FileDescriptor> findToBeDeleted(int daysSinceMod) {
				
		final SqlParameterSource namedParameters = new MapSqlParameterSource()
				.addValue("sttbd", ToolboxSysKeys.FileDescriptorStatus.TO_BE_DELETED)
				.addValue("stt", ToolboxSysKeys.FileDescriptorStatus.TEMPORARY);
		
		return this.namedParameterJdbcTemplate.query(this.fillVariables(
				"SELECT fd.* FROM file_descriptor fd LEFT JOIN remote_file rf ON fd.id = rf.file_id "
				+ "WHERE (fd.tenant_id = #tenantParam) AND ((fd.status = :sttbd) OR (fd.status = :stt)) AND (fd.modified_on < NOW() - INTERVAL '" + daysSinceMod + " DAY')"), // nincs SQL injection veszély itt, mert int
				namedParameters, this.newRowMapperInstance());
	}
	
	/**
	 * {@code ToolboxSysKeys.FileDescriptorLocationType.PROTECTED_FOLDER}, vagy {@code ToolboxSysKeys.FileDescriptorStatus.NORMAL} fájlok,
	 * olyanok, amiknek van {@link RemoteFile} bejegyzésük (már feltöltött)...
	 * 100-100 darab minden hívásnál
	 * 
	 * (tenant független query)
	 *
	 * @return
	 */
	public List<FileDescriptor> findForManageArchive() {
		
		final SqlParameterSource namedParameters = new MapSqlParameterSource()
				.addValue("locationType", ToolboxSysKeys.FileDescriptorLocationType.PROTECTED_FOLDER)
				.addValue("status1", ToolboxSysKeys.FileDescriptorStatus.NORMAL)
				.addValue("status2", ToolboxSysKeys.RemoteProviderStatus.UPLOADED);
		
		return this.namedParameterJdbcTemplate.query(("SELECT fd.* FROM file_descriptor fd INNER JOIN remote_file rf ON rf.file_id = fd.id "
				+ "WHERE (fd.remote_only IS NULL OR fd.remote_only = FALSE) AND (fd.location_type = :locationType) AND (fd.status = :status1) AND (rf.status = :status2) "
				+ "ORDER BY modified_on ASC LIMIT 100"), namedParameters, this.newRowMapperInstance());
	}

	/**
	 * {@code ToolboxSysKeys.FileDescriptorLocationType.PROTECTED_FOLDER}, vagy {@code ToolboxSysKeys.FileDescriptorStatus.NORMAL} fájlok
	 * 
	 * (tenant független query)
	 *
	 * @return
	 */
	public BigDecimal findTotalFileSize1() {
		final SqlParameterSource namedParameters = new MapSqlParameterSource("locationType", ToolboxSysKeys.FileDescriptorLocationType.PROTECTED_FOLDER).addValue("status", ToolboxSysKeys.FileDescriptorStatus.NORMAL);

		final Long value = this.namedParameterJdbcTemplate.queryForObject(("SELECT SUM(file_size) AS total_size FROM file_descriptor fd WHERE location_type = :locationType AND status = :status"), namedParameters, Long.class);

		return new BigDecimal((value == null) ? 0 : value.longValue());
	}

	/**
	 * {@code ToolboxSysKeys.FileDescriptorLocationType.PUBLIC_FOLDER_FOR_CDN}, vagy {@code ToolboxSysKeys.FileDescriptorStatus.NORMAL} fájlok
	 * 
	 * (tenant független query)
	 *
	 * @return
	 */
	public BigDecimal findTotalFileSize2() {
		final SqlParameterSource namedParameters = new MapSqlParameterSource("locationType", ToolboxSysKeys.FileDescriptorLocationType.PUBLIC_FOLDER_FOR_CDN).addValue("status", ToolboxSysKeys.FileDescriptorStatus.NORMAL);

		final Long value = this.namedParameterJdbcTemplate.queryForObject(("SELECT SUM(file_size) AS total_size FROM file_descriptor fd WHERE location_type = :locationType AND status = :status"), namedParameters, Long.class);
		return new BigDecimal((value == null) ? 0 : value.longValue());
	}

	public void saveMeta1(final int fileDescId, final String meta1) {
		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("fileDescId", fileDescId).addValue("meta1", meta1);
		this.namedParameterJdbcTemplate.update(this.fillVariables("UPDATE file_descriptor SET meta_1 = :meta1 WHERE #tenantCondition AND id = :fileDescId"), namedParameters);
	}

	public void saveMeta3(final int fileDescId, final int csTypeId, final Integer csItemId) {
		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("fileDescId", fileDescId).addValue("csTypeId", csTypeId).addValue("csItemId", csItemId);
		this.namedParameterJdbcTemplate.update(this.fillVariables("UPDATE file_descriptor SET meta_3 = :csItemId, meta_3_type = :csTypeId WHERE #tenantCondition AND id = :fileDescId"), namedParameters);
	}

	public void saveSecurityType(final int fileDescId, final int securityType) {
		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("fileDescId", fileDescId).addValue("securityType", securityType);
		this.namedParameterJdbcTemplate.update(this.fillVariables("UPDATE file_descriptor SET security_type = :securityType WHERE #tenantCondition AND id = :fileDescId"), namedParameters);
	}


	public void renameFile(final int fileDescId, final String filename) {
		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("fileDescId", fileDescId).addValue("filename", filename);
		this.namedParameterJdbcTemplate.update(this.fillVariables("UPDATE file_descriptor SET filename = :filename WHERE #tenantCondition AND id = :fileDescId"), namedParameters);
	}

	public void setLockedByAndOn(final int fileDescId, final Integer lockedBy, final Timestamp lockedOn) {
		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("fileDescId", fileDescId).addValue("lockedBy", lockedBy).addValue("lockedOn", lockedOn);
		this.namedParameterJdbcTemplate.update(this.fillVariables("UPDATE file_descriptor SET locked_by = :lockedBy, locked_on = :lockedOn WHERE #tenantCondition AND id = :fileDescId"), namedParameters);
	}

}