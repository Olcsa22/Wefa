package hu.lanoga.toolbox.vaadin;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.teamunify.i18n.I;
import com.vaadin.annotations.Theme;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.auth.token.AuthToken;
import hu.lanoga.toolbox.auth.token.AuthTokenService;
import hu.lanoga.toolbox.exception.ManualValidationException;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.user.User;
import hu.lanoga.toolbox.util.BrandUtil;
import hu.lanoga.toolbox.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * folyamat eleje, 
 * párja a {@link ResetPasswordUI}, ami a folyamat végén van (amikor már az email linkkel "visszajön")
 */
@Slf4j
@Theme("login-ui")
@SpringUI(path = "/public/v/forgotten-password") // Vaadin 1X kompatibilitás miatt már nem használjuk toolbox alap UI-ok esetén ezt az annotációt, lásd még kapcsolodó Servlet class és hu.lanoga.toolbox.config.VaadinServletRegistrationConfig
public class ForgottenPasswordUI extends AbstractToolboxUI {

	private final String applicationName;
	private AuthTokenService authTokenService;

	private final VerticalLayout vlContent;

	public ForgottenPasswordUI() {
		super();

		this.applicationName = ApplicationContextHelper.getConfigProperty("tools.misc.application-name");
		this.authTokenService = ApplicationContextHelper.getBean(AuthTokenService.class);

		// ---

		this.getPage().setTitle(BrandUtil.getAppTitle(false) + " " + I.trc("Title", "Forgotten password"));

		this.vlContent = new VerticalLayout();
		this.vlContent.setMargin(false);
		this.vlContent.setHeight("220px");

		this.setContent(this.vlContent);
	}

	@Override
	protected void init(final VaadinRequest vaadinRequest) {

		SecurityUtil.setAnonymous();

		// ---

		super.init(vaadinRequest);

		final HorizontalLayout hlTop = new HorizontalLayout();
		final HorizontalLayout hlTopInner = new HorizontalLayout();

		final VerticalLayout vlMain = new VerticalLayout();
		vlMain.setMargin(true);

		final Panel panel = new Panel();
		panel.setContent(vlMain);
		this.vlContent.addComponent(panel);

		hlTop.setWidth("100%");

		hlTop.addComponent(hlTopInner);
		hlTop.setComponentAlignment(hlTopInner, Alignment.MIDDLE_CENTER);

		// ---

		final TextField tfExistingEmailAddress = new TextField(I.trc("Caption", "Email"));
		tfExistingEmailAddress.setWidth("100%");

		final Button btnResetPassword = new Button(I.trc("Caption", "Reset password"));
		btnResetPassword.setClickShortcut(ShortcutAction.KeyCode.ENTER);
		btnResetPassword.setWidth("100%");

		btnResetPassword.addClickListener(e -> {

			if (StringUtils.isBlank(tfExistingEmailAddress.getValue())) {
				throw new ManualValidationException("Wrong email!", I.trc("Notification", "Wrong email!"));
			}

			try {

				SecurityUtil.setSystemUser();

				final String browserApplication = Page.getCurrent().getWebBrowser().getBrowserApplication();

				final List<User> users = this.authTokenService.findUsersByEmail(tfExistingEmailAddress.getValue());

				if (CollectionUtils.isEmpty(users)) {
					throw new ManualValidationException("Wrong email!", I.trc("Notification", "Wrong email!"));
				}

				for (final User user : users) {

					try {

						JdbcRepositoryManager.setTlTenantId(user.getTenantId());

						final AuthToken authToken = this.authTokenService.createAuthToken(user.getId(), browserApplication, ToolboxSysKeys.AuthTokenType.FORGOTTEN_PASSWORD, DateTimeUtil.DAY_IN_SEC);
						this.authTokenService.sendForgottenPasswordEmail(this.applicationName, user, authToken);

						log.debug("Forgotten password email has been added to the email queue: " + user.getUsername() + ", " + user.getEmail());

					} finally {
						JdbcRepositoryManager.clearTlTenantId();
					}

				}

				UI.getCurrent().showNotification(I.trc("Notification", "Please check your inbox!"));
				UI.getCurrent().getPage().getJavaScript().execute("setTimeout(function(){ window.parent.location = '../login' }, 3000);");

			} finally {
				SecurityUtil.setAnonymous();
			}

		});

		vlMain.addComponents(tfExistingEmailAddress, btnResetPassword);
		vlMain.setComponentAlignment(tfExistingEmailAddress, Alignment.MIDDLE_CENTER);
		vlMain.setComponentAlignment(btnResetPassword, Alignment.MIDDLE_CENTER);
	}

}
