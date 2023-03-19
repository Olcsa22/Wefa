package hu.lanoga.toolbox.codestore;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import hu.lanoga.toolbox.ToolboxSysKeys.RepositoryTenantMode;
import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;

@ConditionalOnMissingBean(name = "codeStoreTypeJdbcRepositoryOverrideBean")
@Repository
public class CodeStoreTypeJdbcRepository extends DefaultJdbcRepository<CodeStoreType> {

	public CodeStoreTypeJdbcRepository() {
		super(RepositoryTenantMode.NO_TENANT);
	}

	@Override
	public String getInnerSelect() {
		return "SELECT c.*, extr_from_lang(c.caption, '#lang1', '#lang2') AS caption_caption FROM code_store_type c";
	}

}
