package hu.lanoga.toolbox.vaadin.component;

import com.vaadin.data.provider.GridSortOrderBuilder;
import com.vaadin.ui.VerticalLayout;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.email.Email;
import hu.lanoga.toolbox.email.EmailNoTenantService;
import hu.lanoga.toolbox.email.EmailSenderScheduler;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponent;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponentBuilder;
import hu.lanoga.toolbox.vaadin.component.crud.FormLayoutCrudFormComponent;
import lombok.Getter;
import lombok.Setter;

/**
 * {@link ToolboxSysKeys.UserAuth#ROLE_SUPER_ADMIN_STR} számára kiküldött email-ek (model/tábla) 
 * követése debug stb. célra
 * 
 * @see Email
 * @see EmailSenderScheduler
 */
@Getter
@Setter
public class EmailSenderDiagnosticsComponent extends VerticalLayout {

	private CrudGridComponent<Email> emailCrud;

	public void initLayout() {

		this.removeAllComponents();

		// ---

		SecurityUtil.limitAccessSuperAdmin();

		// ---

		final CrudGridComponentBuilder<Email> emailCrudBuilder = new CrudGridComponentBuilder<Email>()
				.setModelType(Email.class)
				.setCrudFormComponentSupplier(() -> new FormLayoutCrudFormComponent<>(() -> new Email.VaadinForm()))
				.setCrudService(ApplicationContextHelper.getBean(EmailNoTenantService.class))
				.setLeftRightPaging(true);
		
		this.emailCrud = emailCrudBuilder.createCrudGridComponent();
		this.emailCrud.toggleButtonVisibility(true, false, false, false, false, true, true, false);

		this.emailCrud.getGrid().setSortOrder(new GridSortOrderBuilder<Email>().thenDesc(this.emailCrud.getGrid().getColumn("createdOn")));
		
		this.emailCrud.setFormDialogWidthOverride("800px");
		
		this.addComponent(this.emailCrud);

	}

	public CrudGridComponent<Email> getEmailCrud() {
		return this.emailCrud;
	}

}
