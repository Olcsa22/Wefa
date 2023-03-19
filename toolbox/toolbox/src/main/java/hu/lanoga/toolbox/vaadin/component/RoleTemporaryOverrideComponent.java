package hu.lanoga.toolbox.vaadin.component;

import org.apache.commons.lang3.StringUtils;

import com.teamunify.i18n.I;
import com.vaadin.server.Page;
import com.vaadin.ui.Button;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.exception.ManualValidationException;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.user.User;
import hu.lanoga.toolbox.user.UserService;
import hu.lanoga.toolbox.vaadin.util.UiHelper;

/**
 * átmeneti extra role/authority adás... (belépett user-re, csak session-re)... 
 * bugos lehet... ha újra játékba kerül valahol, akkor át kell nézni alaposan
 */
@Deprecated
public class RoleTemporaryOverrideComponent extends VerticalLayout {

	public RoleTemporaryOverrideComponent() {

		SecurityUtil.limitAccessNotAnonymous();

		// ---

		final User loggedInUser = (User) SecurityUtil.getLoggedInUser();
		final User loggedInUserFromDb = ApplicationContextHelper.getBean(UserService.class).findOne(loggedInUser.getId());

		final UserRoleField userRoleField = new UserRoleField(I.trc("Caption", "User roles"));
		userRoleField.setUserAsIsNow(loggedInUser);
		userRoleField.setWidth("100%");
		userRoleField.setValue(loggedInUser.getUserRoles());

		this.addComponent(userRoleField);

		if (loggedInUser.getAuthorities().equals(loggedInUserFromDb.getAuthorities())) {

			final TextField txtUsername = new TextField(I.trc("Caption", "Username (the allower / admin)"));
			txtUsername.setRequiredIndicatorVisible(true);
			txtUsername.setWidth("100%");

			final PasswordField pfPassword = new PasswordField(I.trc("Caption", "Password (of the allower)"));
			pfPassword.setRequiredIndicatorVisible(true);
			pfPassword.setWidth("100%");

			final Button btnOk = new Button(I.trc("Button", "OK (temporarly grant/add roles to current user)"));
			btnOk.setWidth("");
			btnOk.addStyleName("min-width-150px");
			btnOk.addStyleName("max-width-400px");
			btnOk.addStyleName(ValoTheme.BUTTON_FRIENDLY);

			btnOk.addClickListener(x -> {

				if (StringUtils.isAnyBlank(txtUsername.getValue(), pfPassword.getValue())) {
					final ManualValidationException manualValidationException = new ManualValidationException("Missing or invalid values!");
					manualValidationException.setUiMessage(I.trc("Error", "Missing or invalid values!"));
					throw manualValidationException;
				}

				// ---

				if (!StringUtils.contains(txtUsername.getValue(), ":") &&  !StringUtils.contains(txtUsername.getValue(), "/")) {
					txtUsername.setValue(SecurityUtil.getLoggedInUserTenantId() + ":" + txtUsername.getValue());
				}

				SecurityUtil.checkUserPasswordManually(txtUsername.getValue(), pfPassword.getValue());

				if (SecurityUtil.userHasRole(txtUsername.getValue(), ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)) {

					// ---

					loggedInUser.setUserRoles(userRoleField.getValue());
					loggedInUser.setUserRoles(ApplicationContextHelper.getBean(UserService.class).addAdditionalRoles2(loggedInUser));
					SecurityUtil.setUser(loggedInUser); // így frissül GrantedAuth... listája a userRoles String alapján

					UiHelper.closeParentWindow(this);
					Page.getCurrent().reload();
					
					SecurityUtil.makeNewHttpSession();
				} else {
					throw new ManualValidationException("User does not exist or not admin!", I.trc("Error", "User does not exist or not admin!"));
				}
			});

			this.addComponents(txtUsername, pfPassword, btnOk);

		} else {

			userRoleField.setCaption(I.trc("Caption", "User roles (current temp. state)"));
			userRoleField.setEnabled(false);

			final Button btnRevoke = new Button(I.trc("Button", "Revoke (every temporarly granted role)"));
			btnRevoke.setWidth("");
			btnRevoke.addStyleName("min-width-150px");
			btnRevoke.addStyleName("max-width-400px");
			btnRevoke.addStyleName(ValoTheme.BUTTON_FRIENDLY);

			btnRevoke.addClickListener(x -> {

				loggedInUser.setUserRoles(loggedInUserFromDb.getUserRoles());
				SecurityUtil.setUser(loggedInUser); // így frissül GrantedAuth... listája a userRoles String alapján

				UiHelper.closeParentWindow(this);
				
				Page.getCurrent().reload();

			});

			this.addComponent(btnRevoke);

		}

	}

}