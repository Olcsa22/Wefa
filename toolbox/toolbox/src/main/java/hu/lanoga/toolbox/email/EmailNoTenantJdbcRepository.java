package hu.lanoga.toolbox.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;

@ConditionalOnMissingBean(name = "emailNoTenantJdbcRepositoryOverrideBean")
@Repository
public class EmailNoTenantJdbcRepository extends DefaultJdbcRepository<Email> {

	public EmailNoTenantJdbcRepository() {
		super(ToolboxSysKeys.RepositoryTenantMode.NO_TENANT);
	}
	
}
