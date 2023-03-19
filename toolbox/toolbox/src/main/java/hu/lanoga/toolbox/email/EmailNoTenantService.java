package hu.lanoga.toolbox.email;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.domain.Page;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import hu.lanoga.toolbox.service.AdminOnlyCrudService;

/**
 * {@link ToolboxSysKeys.UserAuth#ROLE_SUPER_ADMIN_STR} számára diagnosztika
 */
@ConditionalOnMissingBean(name = "emailNoTenantServiceOverrideBean")
@Service
public class EmailNoTenantService extends AdminOnlyCrudService<Email, EmailNoTenantJdbcRepository> {

	@Secured(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR)
	@Override
	public EmailNoTenantJdbcRepository getRepository() {
		return super.getRepository();
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR)
	@Override
	public Email findOne(int id) {
		return super.findOne(id);
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR)
	@Override
	public List<Email> findAll() {
		return super.findAll();
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR)
	@Override
	public Page<Email> findAll(BasePageRequest<Email> pageRequest) {
		return super.findAll(pageRequest);
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR)
	@Override
	public long count() {
		return super.count();
	}

	@Override
	public Email save(Email t) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(final int id) {
		throw new UnsupportedOperationException();
	}

}
