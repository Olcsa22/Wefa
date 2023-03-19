package hu.lanoga.toolbox.codestore;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.service.AdminOnlyCrudService;

@ConditionalOnMissingBean(name = "codeStoreTypeServiceOverrideBean")
@Service
public class CodeStoreTypeService extends AdminOnlyCrudService<CodeStoreType, CodeStoreTypeJdbcRepository> {

	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	public List<CodeStoreType> findAllExpandable() {
		return this.repository.findAllBy("expandable", true);
	}

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR)
	@Transactional
	public void delete(final int id) {
		
		throw new UnsupportedOperationException("CodeStoreTypeService delete is not allowed!");
		
		// super.delete(id);
	}

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR)
	@Transactional
	public CodeStoreType save(final CodeStoreType t) {
		
		throw new UnsupportedOperationException("CodeStoreTypeService save is not allowed!");
		
		// return super.save(t);
		
	}

}
