package hu.lanoga.toolbox.tenant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;

@ConditionalOnMissingBean(name = "tenantKeyValueSettingsJdbcRepositoryOverrideBean")
@Repository
public class TenantKeyValueSettingsJdbcRepository extends DefaultJdbcRepository<TenantKeyValueSettings> {

	//

}
