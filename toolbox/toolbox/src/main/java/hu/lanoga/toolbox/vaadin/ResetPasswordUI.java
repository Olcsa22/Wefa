package hu.lanoga.toolbox.vaadin;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.teamunify.i18n.I;
import com.vaadin.annotations.Theme;
import com.vaadin.event.ShortcutAction;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.auth.token.AuthToken;
import hu.lanoga.toolbox.auth.token.AuthTokenService;
import hu.lanoga.toolbox.exception.ManualValidationException;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.tenant.Tenant;
import hu.lanoga.toolbox.tenant.TenantService;
import hu.lanoga.toolbox.user.User;
import hu.lanoga.toolbox.user.UserService;
import hu.lanoga.toolbox.util.ToolboxAssert;

/**
 * folyamat vége, 
 * párja a {@link ForgottenPasswordUI}, ami a folyamat elején van (azzal lehet kezdeményezni token-es email küldését)
 */
@Theme("login-ui")
@SpringUI(path = "/public/v/reset-password") // Vaadin 1X kompatibilitás miatt már nem használjuk toolbox alap UI-ok esetén ezt az annotációt, lásd még kapcsolodó Servlet class és hu.lanoga.toolbox.config.VaadinServletRegistrationConfig
public class ResetPasswordUI extends AbstractToolboxUI {

	private static boolean isSameAndNotEmptyPassword(final String tfNewPassword1, final String tfNewPassword2) {
		return StringUtils.isNotBlank(tfNewPassword1) && StringUtils.isNotBlank(tfNewPassword2) && tfNewPassword1.equals(tfNewPassword2);
	}

	private AuthTokenService authTokenService;
	private UserService userService;
	private TenantService tenantService;
	
	private VerticalLayout vlContent;
		
	public ResetPasswordUI() {
		super();

		// this.applicationName = ApplicationContextHelper.getConfigProperty("tools.misc.application-name");
		this.authTokenService = ApplicationContextHelper.getBean(AuthTokenService.class);
		this.userService = ApplicationContextHelper.getBean(UserService.class);
		this.tenantService = ApplicationContextHelper.getBean(TenantService.class);
	}

	@Override
	protected void init(final VaadinRequest vaadinRequest) {

		SecurityUtil.setAnonymous();

		// ---

		super.init(vaadinRequest);

		this.vlContent = new VerticalLayout();
		this.vlContent.setMargin(new MarginInfo(false, true));
		this.vlContent.setHeight(null);

		final PasswordField tfNewPassword1 = new PasswordField(I.trc("Button", "New password"));
		final PasswordField tfNewPassword2 = new PasswordField(I.trc("Button", "New password again"));

		final Button btnSetNewPassword = new Button(I.trc("Button", "Save"));
		btnSetNewPassword.setClickShortcut(ShortcutAction.KeyCode.ENTER);

		final HorizontalLayout hlUserInfo = new HorizontalLayout();
		hlUserInfo.setMargin(false);

		final Label lblUserIcon = new Label(VaadinIcons.USERS.getHtml(), ContentMode.HTML);

		final String tokenFromUrl = vaadinRequest.getParameter("token");
		ToolboxAssert.notNull(tokenFromUrl);

		// log.debug("Forgotten password token: " + tokenFromUrl);

		try {

			SecurityUtil.setSystemUser();

			final AuthToken authToken = this.authTokenService.findAuthTokenByToken(tokenFromUrl, ToolboxSysKeys.AuthTokenType.FORGOTTEN_PASSWORD);

			JdbcRepositoryManager.setTlTenantId(authToken.getTenantId());

			final User user = this.userService.findOne(authToken.getResourceId1());
			ToolboxAssert.notNull(user);
			
			Tenant tenant = tenantService.findOne(user.getTenantId());
			ToolboxAssert.notNull(tenant);

			final Label lblUsername = new Label(tenant.getName() + "/" + user.getUsername());

			btnSetNewPassword.addClickListener(e -> {

				if (!authToken.getValidUntil().after(new Date())) {
					throw new ManualValidationException("The token has expired!", I.trc("Notification", "The token has expired!"));
				}

				if (!ResetPasswordUI.isSameAndNotEmptyPassword(tfNewPassword1.getValue(), tfNewPassword2.getValue())) {
					throw new ManualValidationException("The passwords do not match!", I.trc("Notification", "The passwords do not match!"));
				}

				if (!SecurityUtil.checkPasswordStrengthSimple(tfNewPassword1.getValue())) {
					throw new ManualValidationException("The password is too weak!", I.trc("Notification", "The password is too weak!"));
				}

				try {

					SecurityUtil.setSystemUser();
					JdbcRepositoryManager.setTlTenantId(authToken.getTenantId());

					this.userService.updateUserPasswordByUserId(user.getId(), tfNewPassword1.getValue(), authToken);

				} finally {
					JdbcRepositoryManager.clearTlTenantId();
					SecurityUtil.clearAuthentication();
				}
				
				UI.getCurrent().showNotification(I.trc("Notification", "Password was changed!"));
				UI.getCurrent().getPage().getJavaScript().execute("setTimeout(function(){ window.parent.location = '../login' }, 3000);");


			});

			tfNewPassword1.setWidth("100%");
			tfNewPassword2.setWidth("100%");
			btnSetNewPassword.setWidth("100%");

			hlUserInfo.addComponents(lblUserIcon, lblUsername);

			this.vlContent.addComponents(hlUserInfo, tfNewPassword1, tfNewPassword2, btnSetNewPassword);
			this.vlContent.setComponentAlignment(hlUserInfo, Alignment.MIDDLE_CENTER);
			this.vlContent.setComponentAlignment(tfNewPassword1, Alignment.MIDDLE_CENTER);
			this.vlContent.setComponentAlignment(tfNewPassword2, Alignment.MIDDLE_CENTER);
			this.vlContent.setComponentAlignment(btnSetNewPassword, Alignment.MIDDLE_CENTER);

			this.setContent(this.vlContent);

		} finally {
			JdbcRepositoryManager.clearTlTenantId();
			SecurityUtil.setAnonymous();
		}
	}
}