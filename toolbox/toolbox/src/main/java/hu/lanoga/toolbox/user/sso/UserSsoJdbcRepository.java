package hu.lanoga.toolbox.user.sso;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;

@ConditionalOnMissingBean(name = "userSsoJdbcRepositoryOverrideBean")
@Repository
public class UserSsoJdbcRepository extends DefaultJdbcRepository<UserSso> {

	//
	
}