package hu.lanoga.toolbox.vaadin.component;

import com.vaadin.ui.VerticalLayout;
import hu.lanoga.toolbox.email.EmailTemplate;
import hu.lanoga.toolbox.email.EmailTemplateService;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponent;
import hu.lanoga.toolbox.vaadin.component.crud.FormLayoutCrudFormComponent;

/**
 * super admin célokra szolgál
 */
public class EmailTemplateComponent extends VerticalLayout {

	public void initLayout() {

		this.removeAllComponents();

		// ---

		final EmailTemplateService emailTemplateService = ApplicationContextHelper.getBean(EmailTemplateService.class);

		final CrudGridComponent<EmailTemplate> codeStoreTypeCrud = new CrudGridComponent<>(
				EmailTemplate.class,
				emailTemplateService,
				() -> new FormLayoutCrudFormComponent<>(() -> new EmailTemplate.VaadinForm()),
				true);

		codeStoreTypeCrud.toggleButtonVisibility(true, true, true, true, true, true, true, true);
		
		this.addComponent(codeStoreTypeCrud);

	}

}
