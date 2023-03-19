package hu.lanoga.toolbox.vaadin.component;

import com.google.common.collect.Sets;
import com.teamunify.i18n.I;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.SearchCriteriaOperation;
import hu.lanoga.toolbox.exception.ManualValidationException;
import hu.lanoga.toolbox.filter.internal.SearchCriteria;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.user.User;
import hu.lanoga.toolbox.user.UserService;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponent;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponentBuilder;
import hu.lanoga.toolbox.vaadin.component.crud.FormLayoutCrudFormComponent;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserComponent extends VerticalLayout {  // TODO: átnevezni, UserListComponent, UserGridComponent stb.

	private final boolean onlyReadAllowed;
	protected boolean exportVisible = true;
	protected Integer leftRightPagingPageSize;

	/**
	 * @see ToolboxSysKeys.UserAuth#ROLE_TENANT_OVERSEER_STR
	 * @see ToolboxSysKeys.UserAuth#ROLE_SUPER_TENANT_OVERSEER_STR
	 */
	protected boolean showJumpUsersToo = true;

	private User selectedUser;

	private CrudGridComponent<User> userCrud;

	// ---

	// TODO: tisztázni ezeket a constructorokat, legyen egy fő és a több azt hívja!

	public UserComponent() {
		this.onlyReadAllowed = !SecurityUtil.hasAdminRole();
	}

	public UserComponent(final boolean onlyReadAllowed) {
		this.onlyReadAllowed = onlyReadAllowed;
	}

	public UserComponent(final boolean onlyReadAllowed, final boolean exportVisible) {
		this.onlyReadAllowed = onlyReadAllowed;
		this.exportVisible = exportVisible;
	}

	public UserComponent(final boolean onlyReadAllowed, final boolean exportVisible, final Integer leftRightPagingPageSize) {
		this.onlyReadAllowed = onlyReadAllowed;
		this.exportVisible = exportVisible;
		this.leftRightPagingPageSize = leftRightPagingPageSize;
	}

	public UserComponent(final boolean onlyReadAllowed, final boolean exportVisible, final Integer leftRightPagingPageSize, boolean showJumpUsersToo) {
		this.onlyReadAllowed = onlyReadAllowed;
		this.exportVisible = exportVisible;
		this.leftRightPagingPageSize = leftRightPagingPageSize;
		this.showJumpUsersToo = showJumpUsersToo;
	}

	// ---

	protected void initLayout() {

		this.removeAllComponents();

		// ---

		final UserService userService = ApplicationContextHelper.getBean(UserService.class);

		final CrudGridComponentBuilder<User> userCrudBuilder = new CrudGridComponentBuilder<User>()
				.setModelType(User.class)
				.setCrudFormComponentSupplier(() -> new FormLayoutCrudFormComponent<>(() -> new User.VaadinForm()))
				.setCrudService(userService)
				.setLeftRightPaging(true);

		if (this.leftRightPagingPageSize != null) {
			userCrudBuilder.setLeftRightPagingPageSize(this.leftRightPagingPageSize);
		}

		if (!showJumpUsersToo) {
			
			// TODO: tisztázni, mapfre2 miatt tettem be, de lehet, hogy máshol nem így kell
			
			userCrudBuilder.setInitialFixedSearchCriteriaSet(Sets.newHashSet(SearchCriteria.builder()
					.fieldName("username")
					.operation(SearchCriteriaOperation.NOT_LIKE_END)
					.criteriaType(String.class)
					.value("jump.")
					.build())); 
		}

		this.userCrud = userCrudBuilder.createCrudGridComponent();

		// userCrud.enableColumnOrderPersistence(this.getClass().getCanonicalName()); // experimental
		
		this.userCrud.setAsyncExport(true);
		
		this.userCrud.toggleButtonVisibility(true, !onlyReadAllowed, !onlyReadAllowed, false, false, true, true, this.exportVisible);

		final HorizontalLayout hlFooter = new HorizontalLayout();

		final Button btnChangePassword = new Button(I.trc("Button", "Change password"));
		btnChangePassword.setIcon(VaadinIcons.LOCK);
		btnChangePassword.setEnabled(false);
		btnChangePassword.setVisible(!onlyReadAllowed);
		btnChangePassword.addClickListener(x -> {

			final Window dialog = new Window(I.trc("Caption", "Change password for selected user") + ": " + I18nUtil.buildFullName(this.selectedUser, false));
			dialog.setModal(true);
			dialog.setWidth("");

			final VerticalLayout vlContent = new VerticalLayout();
			vlContent.setWidth("100%");

			// ---

			final Label lblInfo = new Label(VaadinIcons.INFO_CIRCLE.getHtml() + " " + I.trc("Caption", "Required: min. 8 character, at least one lowercase character, one uppercase character and one number."));
			lblInfo.setContentMode(ContentMode.HTML);
			vlContent.addComponent(lblInfo);

			final HorizontalLayout hlPassword = new HorizontalLayout();

			final Label lblNewPassword = new Label(I.trc("Caption", "New password") + ": ");
			hlPassword.addComponent(lblNewPassword);

			final TextField txtNewPassword = new TextField();
			hlPassword.addComponent(txtNewPassword);

			final Button btnGeneratePassword = new Button(I.trc("Button", "Generate"));
			btnGeneratePassword.setIcon(VaadinIcons.REFRESH);
			btnGeneratePassword.addClickListener(y -> {
				txtNewPassword.setValue(SecurityUtil.generateRandomPassword());
			});

			hlPassword.addComponent(btnGeneratePassword);

			hlPassword.setComponentAlignment(lblNewPassword, Alignment.MIDDLE_CENTER);
			hlPassword.setComponentAlignment(txtNewPassword, Alignment.MIDDLE_CENTER);
			hlPassword.setComponentAlignment(btnGeneratePassword, Alignment.MIDDLE_CENTER);

			vlContent.addComponent(hlPassword);

			hlPassword.setMargin(true);

			// ---

			final HorizontalLayout hlButtons = new HorizontalLayout();
			vlContent.addComponent(hlButtons);

			{
				final Button btnSave = new Button(I.trc("Button", "Save"));
				btnSave.addStyleName(ValoTheme.BUTTON_PRIMARY);
				btnSave.setIcon(FontAwesome.SAVE);
				btnSave.setWidth("150px");

				btnSave.addClickListener(y -> {

					if (!SecurityUtil.checkPasswordStrengthSimple(txtNewPassword.getValue())) {
						throw new ManualValidationException("Password is too weak", I.trc("Error", "Password is too weak"));
					}

					userService.updateUserPasswordByUserIdManual(this.selectedUser.getId(), txtNewPassword.getValue());

					UiHelper.closeParentWindow(btnSave);
				});

				hlButtons.addComponent(btnSave);
			}

			{

				final Button btnSaveAndCopyToClipb = new Button(I.trc("Button", "Save and copy to clipboard"));
				btnSaveAndCopyToClipb.addStyleName(ValoTheme.BUTTON_PRIMARY);
				btnSaveAndCopyToClipb.setDescription(I.trc("Button", "Save and copy to clipboard"));
				btnSaveAndCopyToClipb.setIcon(FontAwesome.SAVE);
				btnSaveAndCopyToClipb.setWidth("250px");

				btnSaveAndCopyToClipb.addClickListener(y -> {
					if (!SecurityUtil.checkPasswordStrengthSimple(txtNewPassword.getValue())) {
						throw new ManualValidationException("Password is too weak", I.trc("Error", "Password is too weak"));
					}

					userService.updateUserPasswordByUserIdManual(this.selectedUser.getId(), txtNewPassword.getValue());

					JavaScript.eval("navigator.clipboard.writeText('" + txtNewPassword.getValue() + "');");
					Notification.show(I.trc("Button", "Password copied to clipboard!"));

					UiHelper.closeParentWindow(btnSaveAndCopyToClipb);
				});

				hlButtons.addComponent(btnSaveAndCopyToClipb);

			}

			dialog.setContent(vlContent);
			UI.getCurrent().addWindow(dialog);
		});

		hlFooter.addComponent(btnChangePassword);

		userCrud.addAdditionalFooterToolbar(hlFooter);

		userCrud.setSelectionConsumer(x -> {
			if (x != null) {
				this.selectedUser = x;

				btnChangePassword.setEnabled(true);
			} else {
				btnChangePassword.setEnabled(false);
			}
		});

		this.addComponent(userCrud);

	}

	public CrudGridComponent<User> getUserCrud() {
		return this.userCrud;
	}

}
