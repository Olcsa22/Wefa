package hu.lanoga.toolbox.tenant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import hu.lanoga.toolbox.ToolboxSysKeys.RepositoryTenantMode;
import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;

@ConditionalOnMissingBean(name = "tenantInfoJdbcRepositoryOverrideBean")
@Repository
public class TenantInfoJdbcRepository extends DefaultJdbcRepository<TenantInfo> {

	public TenantInfoJdbcRepository() {
		super(RepositoryTenantMode.NO_TENANT);
	}

}
