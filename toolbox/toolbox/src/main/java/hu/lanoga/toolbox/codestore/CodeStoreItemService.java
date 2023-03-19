package hu.lanoga.toolbox.codestore;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.domain.Page;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import hu.lanoga.toolbox.service.DefaultCrudService;

@ConditionalOnMissingBean(name = "codeStoreItemServiceOverrideBean")
@Service
public class CodeStoreItemService extends DefaultCrudService<CodeStoreItem, CodeStoreItemJdbcRepository> {

	@Autowired
	private CodeStoreTypeService codeStoreTypeService;

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR)
	@Transactional
	public CodeStoreItem save(final CodeStoreItem codeStoreItem) {
		final CodeStoreType codeStoreType = this.codeStoreTypeService.findOne(codeStoreItem.getCodeStoreTypeId());

		if (codeStoreItem.isNew() && !codeStoreType.getExpandable()) {
			throw new ToolboxGeneralException("can't create code store item on a not expandable codeStoreType");
		}

		return this.repository.save(codeStoreItem);
	}

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR)
	@Transactional
	public void delete(final int id) {
		throw new UnsupportedOperationException("CodeStoreItemService delete is not allowed!");
	}

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR)
	public CodeStoreItem findOne(final int id) {
		return super.findOne(id);
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR)
	public List<CodeStoreItem> findAllByType(final int codeStoreTypeId) {
		return this.repository.findAllBy("codeStoreTypeId", codeStoreTypeId);
	}

	/**
	 * veszélyes mert nincs unique constraint rajta!
	 * 
	 * @param command
	 * @return
	 * 
	 * @see #findOneByTypeAndCommand(int, String)
	 */
	@Deprecated
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR)
	public CodeStoreItem findOneByCommand(final String command) {
		return this.repository.findOneBy("command", command);
	}
	
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR)
	public CodeStoreItem findOneByTypeAndCommand(final int codeStoreTypeId, final String command) {
		return this.repository.findOneBy("codeStoreTypeId", codeStoreTypeId, "command", command);
	}

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	public Page<CodeStoreItem> findAll(final BasePageRequest<CodeStoreItem> pageRequest) {
		return super.findAll(pageRequest);
	}

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	public List<CodeStoreItem> findAll() {
		return super.findAll();
	}

}
