package hu.lanoga.toolbox.user;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;

@ConditionalOnMissingBean(name = "userKeyValueSettingsJdbcRepositoryOverrideBean")
@Repository
public class UserKeyValueSettingsJdbcRepository extends DefaultJdbcRepository<UserKeyValueSettings> {

	//
	
}
