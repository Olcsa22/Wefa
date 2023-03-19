package hu.lanoga.toolbox.email;

import hu.lanoga.toolbox.ToolboxSysKeys;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;

@ConditionalOnMissingBean(name = "emailTemplateJdbcRepositoryOverrideBean")
@Repository
public class EmailTemplateJdbcRepository extends DefaultJdbcRepository<EmailTemplate> {

	public EmailTemplateJdbcRepository() {
		super(ToolboxSysKeys.RepositoryTenantMode.NO_TENANT);
	}
	
	@Override
	public String getInnerSelect() {
		return "SELECT e.*, extr_from_lang(csi1.caption, '#lang1', '#lang2') AS templ_code_caption FROM email_template e INNER JOIN code_store_item csi1 ON e.templ_code = csi1.id";
	}

}
