package hu.lanoga.toolbox.file.remote;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;

@ConditionalOnMissingBean(name = "remoteFileJdbcRepositoryOverrideBean")
@Repository
public class RemoteFileJdbcRepository extends DefaultJdbcRepository<RemoteFile> {

	public List<RemoteFile> findForScheduler() {

		final SqlParameterSource namedParameters = new MapSqlParameterSource()
				.addValue("status1", ToolboxSysKeys.RemoteProviderStatus.PENDING)
				.addValue("status2", ToolboxSysKeys.RemoteProviderStatus.FAILED)
				.addValue("status3", ToolboxSysKeys.RemoteProviderStatus.UPLOADED)
				.addValue("attempt", 4);

		return this.namedParameterJdbcTemplate.query(this.fillVariables("SELECT * FROM remote_file rf WHERE #tenantCondition AND ((rf.status = :status1) OR (rf.status = :status2 AND rf.attempt < :attempt) OR (rf.status = :status3 AND rf.anyone_with_link_requested = true)) ORDER BY rf.priority DESC, rf.created_on ASC LIMIT 10"), namedParameters, this.newRowMapperInstance());
	}

	/**
	 * ha a státusza {@link ToolboxSysKeys.RemoteProviderStatus#PENDING} és alacsonyan van a prioritása, de már régóta vár a feltöltésre...
	 * 
	 * @return
	 */
	public List<RemoteFile> findAllForPriorityIncrease() {

		final SqlParameterSource namedParameters = new MapSqlParameterSource()
				.addValue("status", ToolboxSysKeys.RemoteProviderStatus.PENDING)
				.addValue("priority", ToolboxSysKeys.RemoteProviderPriority.VERY_HIGH);

		return this.namedParameterJdbcTemplate.query(this.fillVariables("SELECT * FROM remote_file rf WHERE #tenantCondition AND rf.status = :status AND rf.priority < :priority AND rf.created_on < NOW() - INTERVAL '1 DAY'"), namedParameters, this.newRowMapperInstance());

	}
}