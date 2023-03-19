package hu.lanoga.toolbox.vaadin.component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.WizardStep;

import com.google.common.collect.Lists;
import com.teamunify.i18n.I;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.twofactorcredential.TwoFactorCredentialJdbcRepository;
import hu.lanoga.toolbox.user.User;
import hu.lanoga.toolbox.user.UserKeyValueSettings;
import hu.lanoga.toolbox.user.UserKeyValueSettingsService;
import hu.lanoga.toolbox.user.UserPassword;
import hu.lanoga.toolbox.user.UserPasswordJdbcRepository;
import hu.lanoga.toolbox.user.UserService;
import hu.lanoga.toolbox.vaadin.util.UiHelper;

/**
 * a felhasználó adatainak módosítására ad lehetőséget (például: vezetéknév, keresztnév, jelszó stb.)
 */
public class MyProfileComponent extends VerticalLayout {

	private final UserKeyValueSettingsService userKeyValueSettingsService;

	private UserKeyValueSettings userCurrency;
	private UserKeyValueSettings userLocale;
	private UserKeyValueSettings userTimeZone;
	private UserKeyValueSettings userTheme;
	private UserKeyValueSettings userMeasurementSystem;

	/**
	 * true esetén hozzáadás, false érték esetén eltávolítás
	 */
	private Boolean isAddTwoFactor;
	private Boolean isVerified;

	private final User user;

	public MyProfileComponent() {

		SecurityUtil.limitAccessNotAnonymous();

		this.userKeyValueSettingsService = ApplicationContextHelper.getBean(UserKeyValueSettingsService.class);
		this.user = ApplicationContextHelper.getBean(UserService.class).findOne(SecurityUtil.getLoggedInUser().getId()); // itt direkt van így SecurityUtil.getLoggedInUser() helyett

		// ---

		this.userCurrency = this.userKeyValueSettingsService.findOneByKey(this.user.getId(), ToolboxSysKeys.UserKeyValueSettings.PREFERRED_CURRENCY);
		this.userLocale = this.userKeyValueSettingsService.findOneByKey(this.user.getId(), ToolboxSysKeys.UserKeyValueSettings.PREFERRED_LOCALE);
		this.userTimeZone = this.userKeyValueSettingsService.findOneByKey(this.user.getId(), ToolboxSysKeys.UserKeyValueSettings.PREFERRED_TIME_ZONE);
		this.userMeasurementSystem = this.userKeyValueSettingsService.findOneByKey(this.user.getId(), ToolboxSysKeys.UserKeyValueSettings.PREFERRED_MEASUREMENT_SYSTEM);
		this.userTheme = this.userKeyValueSettingsService.findOneByKey(this.user.getId(), ToolboxSysKeys.UserKeyValueSettings.PREFERRED_THEME);

		// ---

		final TabSheet tsMyProfile = new TabSheet();
		this.addComponent(tsMyProfile);

		tsMyProfile.addTab(this.buildFirstTab(), I.trc("Caption", "Basic data"));
		tsMyProfile.addTab(this.buildPasswordTab(), I.trc("Caption", "Change password"));

		tsMyProfile.addSelectedTabChangeListener(x -> {
			if (x.isUserOriginated()) {
				UiHelper.centerParentWindow(this);
			}
		});

		final VerticalLayout vlKeyValueTab = this.buildKeyValueTab();
		if (vlKeyValueTab != null) {
			tsMyProfile.addTab(vlKeyValueTab, I.trc("Caption", "Preferences"));
		}

		if (ApplicationContextHelper.getConfigProperty("tools.login.two-factor-auth", Boolean.class)) {
			tsMyProfile.addTab(this.buildTwoFactorAuthTab(), I.trc("Caption", "Two factor authentication"));
		}

	}

	private VerticalLayout buildFirstTab() {

		final Binder<User> binder = new Binder<>();

		final VerticalLayout vlContent = new VerticalLayout();

		final TextField username = new TextField(I.trc("Caption", "Username (cannot be modified)"));
		username.setEnabled(false);
		username.setWidth("100%");

		binder.bind(username, User::getUsername, User::setUsername);

		final TextField title = new TextField(I.trc("Caption", "Title"));
		title.setWidth("100%");
		binder.bind(title, User::getTitle, User::setTitle);

		final TextField familyName = new TextField(I.trc("Caption", "Family name"));
		familyName.setWidth("100%");
		binder.bind(familyName, User::getFamilyName, User::setFamilyName);

		final TextField givenName = new TextField(I.trc("Caption", "First name"));
		givenName.setWidth("100%");
		binder.bind(givenName, User::getGivenName, User::setGivenName);

		final TextField phoneNumber = new TextField(I.trc("Caption", "Phone"));
		phoneNumber.setWidth("100%");
		binder.bind(phoneNumber, User::getPhoneNumber, User::setPhoneNumber);

		final TextField email = new TextField(I.trc("Caption", "Email"));
		email.setWidth("100%");
		binder.bind(email, User::getEmail, User::setEmail);

		final Button btnModify = new Button(I.trc("Button", "Save profile data"));
		btnModify.addStyleName(ValoTheme.BUTTON_PRIMARY);
		btnModify.setIcon(FontAwesome.SAVE);
		btnModify.addClickListener(action -> {

			try {

				binder.writeBean(this.user);

				this.user.setUserRoles(ApplicationContextHelper.getBean(UserService.class).addAdditionalRoles2(this.user));

				ApplicationContextHelper.getBean(UserService.class).save(this.user);

				SecurityUtil.setUser(this.user); // ezzel/így magában a security context-ben is frissíti...

				Notification.show(I.trc("Notification", "The changes has been successfully saved!"));

				if (MyProfileComponent.this.getParent() instanceof Window) {
					((Window) MyProfileComponent.this.getParent()).close();
				}

			} catch (final ValidationException e) {
				Notification.show(I.trc("Notification", "Validation failed! Please check and correct the fields!"), Notification.Type.WARNING_MESSAGE);
			}
		});
		btnModify.setWidth(null);

		vlContent.addComponents(username);

		final UserRoleField userRoleField = new UserRoleField(I.trc("Caption", "User roles (only admin can modify in admin settings)"));
		userRoleField.setUserAsIsNow(user);
		userRoleField.setEnabled(false);

		userRoleField.setDescription(userRoleField.getValue()); // ha sok van és nem férne ki, akkor így biztosan látszik
		userRoleField.setValue(this.user.getUserRoles());
		// userRoleField.setDescription(); // TODO: jó lenne tooltipben is mutatni (ha sok van nem fér ki)
		vlContent.addComponent(userRoleField);

		vlContent.addComponents(title, givenName, familyName, email, phoneNumber);

		final HorizontalLayout hlButtons = new HorizontalLayout();
		hlButtons.setWidth("100%");
		// hlButtons.setMargin(new MarginInfo(true, false, false, false));
		hlButtons.addComponent(btnModify);
		hlButtons.setComponentAlignment(btnModify, Alignment.MIDDLE_LEFT);

		vlContent.addComponent(hlButtons);

		binder.readBean(this.user); // ennek a mezők után kell lennie

		vlContent.setMargin(new MarginInfo(true, false, false, false));

		return vlContent;

	}

	private VerticalLayout buildPasswordTab() {

		final Label lblInfo = new Label(VaadinIcons.INFO_CIRCLE.getHtml() + " " + I.trc("Caption", "Required: min. 8 character, at least one lowercase character, one uppercase character and one number."));
		lblInfo.setContentMode(ContentMode.HTML);

		final PasswordField oldPassword = new PasswordField(I.trc("Caption", "Old password"));
		oldPassword.setRequiredIndicatorVisible(true);
		oldPassword.setWidth("100%");

		final PasswordField password = new PasswordField(I.trc("Caption", "New password"));
		password.setRequiredIndicatorVisible(true);
		password.setWidth("100%");

		final PasswordField passwordAgain = new PasswordField(I.trc("Caption", "New password again"));
		passwordAgain.setWidth("100%");
		passwordAgain.setRequiredIndicatorVisible(true);

		final Button btnPasswordChangeConfirm = new Button(I.trc("Button", "Save password"));
		btnPasswordChangeConfirm.addStyleName(ValoTheme.BUTTON_PRIMARY);
		btnPasswordChangeConfirm.setIcon(FontAwesome.SAVE);
		btnPasswordChangeConfirm.setWidth("200px");
		btnPasswordChangeConfirm.addClickListener(clickEvent -> {

			if (StringUtils.isNotEmpty(oldPassword.getValue()) && StringUtils.isNotEmpty(password.getValue()) && StringUtils.isNotEmpty(passwordAgain.getValue())) {

				final PasswordEncoder passwordEncoder = ApplicationContextHelper.getBean(PasswordEncoder.class);

				if (!passwordEncoder.matches(oldPassword.getValue(), this.user.getPassword())) {
					Notification.show(I.trc("Notification", "Old password is invalid!"), Notification.Type.WARNING_MESSAGE);
					return;
				}

				if (password.getValue().equals(passwordAgain.getValue())) {

					if (SecurityUtil.checkPasswordStrengthSimple(password.getValue())) {

						// TODO: itt kivételesen közvetlenül a repository-t használjuk, szódával elmegy, de majd tisztázni kell még

						final UserPasswordJdbcRepository userPasswordJdbcRepository = ApplicationContextHelper.getBean(UserPasswordJdbcRepository.class);

						final UserPassword userPassword = userPasswordJdbcRepository.findUserPassword(this.user.getTenantId(), this.user.getId());
						userPassword.setPassword(passwordEncoder.encode(password.getValue()));
						userPasswordJdbcRepository.save(userPassword);

						Notification.show(I.trc("Notification", "The password has been modified."));

						if (MyProfileComponent.this.getParent() instanceof Window) {
							((Window) MyProfileComponent.this.getParent()).close();
						}

					} else {
						Notification.show(I.trc("Notification", "The password is too weak! Required: min. 8 character, at least one lowercase character, one uppercase character and one number"), Notification.Type.WARNING_MESSAGE);
					}

				} else {
					Notification.show(I.trc("Notification", "The two password values do not match!"), Notification.Type.WARNING_MESSAGE);
				}

			} else {
				Notification.show(I.trc("Notification", "Please fill out the missing fields!"), Notification.Type.WARNING_MESSAGE);
			}

		});

		final VerticalLayout vlContent = new VerticalLayout();
		vlContent.addComponents(lblInfo, oldPassword, password, passwordAgain);

		final HorizontalLayout hlButtons = new HorizontalLayout();
		hlButtons.setWidth("100%");
		// hlButtons.setMargin(new MarginInfo(true, false, false, false));
		hlButtons.addComponent(btnPasswordChangeConfirm);
		hlButtons.setComponentAlignment(btnPasswordChangeConfirm, Alignment.MIDDLE_LEFT);

		vlContent.addComponent(hlButtons);

		vlContent.setMargin(new MarginInfo(true, false, false, false));

		return vlContent;
	}

	private VerticalLayout buildKeyValueTab() {

		final VerticalLayout vlContent = new VerticalLayout();

		// final ComboBox<String> cmbCurrency;
		final ComboBox<String> cmbLocale;
		final ComboBox<TimeZone> cmbTimeZone; // TODO: ezt is átírni (ComboBox<String> legyen) és általánosítani a cmbLangCode-hoz hasonlóan
		final ComboBox<String> cmbTheme;
		final ComboBox<String> cmbMeasurementSystems;

		// megjegyzések:
		// property fájlból töltjük be a lehetséges opciókat (pl. a nyelvkódokat) (feliratokat itt tesszük hozzá)

		// Egyenlőre ki lett kapcsolva

		// {
		//
		// final String propStr = ApplicationContextHelper.getConfigProperty("tools.preferences.available-currencies"); // property fájlból betöltjük a
		//
		// if (StringUtils.isNotBlank(propStr)) {
		//
		// final String[] currencies = propStr.split(",");
		//
		// cmbCurrency = UiHelper.buildCurrencyCombo(I.trc("Caption", "Currency (empty = default)"), Lists.newArrayList(currencies));
		// cmbCurrency.setEmptySelectionAllowed(true);
		//
		// if (this.userCurrency != null && StringUtils.isNotBlank(this.userCurrency.getKvValue())) {
		// cmbCurrency.setValue(this.userCurrency.getKvValue());
		// }
		//
		// vlContent.addComponent(cmbCurrency);
		//
		// } else {
		// cmbCurrency = null;
		// }
		//
		// }

		// ---

		{

			final String propStr = ApplicationContextHelper.getConfigProperty("tools.preferences.available-locales"); // property fájlból betöltjük a

			if (StringUtils.isNotBlank(propStr)) {

				final String[] languages = propStr.split(",");

				cmbLocale = UiHelper.buildLangCodeCombo(I.trc("Caption", "Locale (empty = automatic detection)"), Lists.newArrayList(languages));
				cmbLocale.setEmptySelectionAllowed(true);

				if (this.userLocale != null && StringUtils.isNotBlank(this.userLocale.getKvValue())) {
					cmbLocale.setValue(this.userLocale.getKvValue());
				}

				vlContent.addComponent(cmbLocale);

			} else {
				cmbLocale = null;
			}

		}

		// ---

		// TODO: egyelőre a Vaadin Page (= a böngésző) TimeZone van használva (ha ez nem elérhető akkor a tenant és még "lentebbi" fallback értékek...)

		// {
		//
		// final String propStr = ApplicationContextHelper.getConfigProperty("tools.preferences.available-timezones"); // property fájlból
		//
		// if (StringUtils.isNotBlank(propStr)) {
		//
		// final List<TimeZone> timeZoneList = new ArrayList<>();
		//
		// if (StringUtils.isNotBlank(propStr)) {
		//
		// final String[] timeZones = propStr.split(",");
		//
		// for (final String timeZoneStr : timeZones) {
		// final TimeZone timeZone = TimeZone.getTimeZone(timeZoneStr);
		// timeZoneList.add(timeZone);
		// }
		//
		// }
		//
		// cmbTimeZone = new ComboBox<>();
		// cmbTimeZone.setItems(timeZoneList);
		// cmbTimeZone.setEmptySelectionAllowed(true);
		// cmbTimeZone.setWidth("100%");
		//
		// cmbTimeZone.setItemCaptionGenerator(timeZone -> {
		//
		// final long offSetHour = TimeUnit.HOURS.convert(timeZone.getRawOffset(), TimeUnit.MILLISECONDS);
		//
		// String offSetString = "UTC+" + offSetHour;
		//
		// if (offSetHour < 0) {
		// offSetString = "UTC" + offSetHour;
		// }
		//
		// return timeZone.getID() + " - " + timeZone.getDisplayName(I18nUtil.getLoggedInUserLocale()) + " (" + offSetString + ")";
		// });
		//
		// cmbTimeZone.setCaption(I.trc("Caption", "Time zone (empty = default)"));
		//
		// if (this.userTimeZone != null && StringUtils.isNotBlank(this.userTimeZone.getKvValue())) {
		// final TimeZone timeZone = TimeZone.getTimeZone(this.userTimeZone.getKvValue());
		// cmbTimeZone.setValue(timeZone);
		// }
		//
		// vlContent.addComponent(cmbTimeZone);
		//
		// } else {

		cmbTimeZone = null;

		// }
		//
		// }

		// ---

		{
			
			// measurement-systems: értsd metrikus, us, imperial stb.

			final String propStr = ApplicationContextHelper.getConfigProperty("tools.preferences.available-measurement-systems"); // property fájlból

			if (StringUtils.isNotBlank(propStr)) {

				final List<String> systemList = new ArrayList<>();

				if (StringUtils.isNotBlank(propStr)) {
					final String[] systems = propStr.split(",");
					systemList.addAll(Arrays.asList(systems)); // TODO: nyelvesítés az elnevezéshez...
				}

				cmbMeasurementSystems = new ComboBox<>();
				cmbMeasurementSystems.setItems(systemList);
				cmbMeasurementSystems.setEmptySelectionAllowed(true);
				cmbMeasurementSystems.setWidth("100%");

				cmbMeasurementSystems.setCaption(I.trc("Caption", "Measurement system (empty = use default)"));

				if (this.userMeasurementSystem != null && StringUtils.isNotBlank(this.userMeasurementSystem.getKvValue())) {
					cmbMeasurementSystems.setValue(this.userMeasurementSystem.getKvValue());
				}

				vlContent.addComponent(cmbMeasurementSystems);

			} else {
				cmbMeasurementSystems = null;
			}

		}

		// ---

		{

			final String propStr = ApplicationContextHelper.getConfigProperty("tools.preferences.available-themes"); // property fájlból

			if (StringUtils.isNotBlank(propStr)) {

				final List<String> themeList = new ArrayList<>();

				if (StringUtils.isNotBlank(propStr)) {
					final String[] themes = propStr.split(",");
					themeList.addAll(Arrays.asList(themes));
				}

				cmbTheme = new ComboBox<>();
				cmbTheme.setItems(themeList);
				cmbTheme.setEmptySelectionAllowed(true);
				cmbTheme.setWidth("100%");

				cmbTheme.setCaption(I.trc("Caption", "Theme (empty = default)"));

				if (this.userTheme != null && StringUtils.isNotBlank(this.userTheme.getKvValue())) {
					cmbTheme.setValue(this.userTheme.getKvValue());
				}

				vlContent.addComponent(cmbTheme);

			} else {
				cmbTheme = null;
			}

		}

		// ---

		if (vlContent.getComponentCount() == 0) {
			return null;
		}

		// ---

		{

			final Button btnSave = new Button(I.trc("Button", "Save preferences"));
			btnSave.addStyleName(ValoTheme.BUTTON_PRIMARY);
			btnSave.setIcon(FontAwesome.SAVE);
			btnSave.setWidth("200px");

			btnSave.addClickListener(x -> {

				// final String userCurrencyValue = (cmbCurrency != null) ? cmbCurrency.getValue() : null;
				final String userLocaleValue = (cmbLocale != null) ? cmbLocale.getValue() : null;
				final String userTimeZoneValue = (cmbTimeZone != null && cmbTimeZone.getValue() != null) ? cmbTimeZone.getValue().getID() : null;
				final String userThemeValue = (cmbTheme != null) ? cmbTheme.getValue() : null;
				final String userMeasurementSystemValue = (cmbMeasurementSystems != null) ? cmbMeasurementSystems.getValue() : null;

				if (
				// Objects.equals(userCurrencyValue, this.userCurrency == null ? null : this.userCurrency.getKvValue()) &&
				Objects.equals(userLocaleValue, this.userLocale == null ? null : this.userLocale.getKvValue()) &&
						Objects.equals(userTimeZoneValue, this.userTimeZone == null ? null : this.userTimeZone.getKvValue()) &&
						Objects.equals(userThemeValue, this.userTheme == null ? null : this.userTheme.getKvValue()) &&
						Objects.equals(userMeasurementSystemValue, this.userMeasurementSystem == null ? null : this.userMeasurementSystem.getKvValue())) {

					// ha semmi sem változott, akkor bezárjuk simán

					if (this.getParent() instanceof Window) {
						((Window) this.getParent()).close();
					}

					return;

				}

				this.userCurrency = new UserKeyValueSettings();
				this.userCurrency.setId(null);
				this.userCurrency.setUserId(this.user.getId());
				this.userCurrency.setKvKey(ToolboxSysKeys.UserKeyValueSettings.PREFERRED_CURRENCY);
				// this.userCurrency.setKvValue(userCurrencyValue);
				this.userCurrency.setManualEditAllowed(true);

				this.userLocale = new UserKeyValueSettings();
				this.userLocale.setId(null);
				this.userLocale.setUserId(this.user.getId());
				this.userLocale.setKvKey(ToolboxSysKeys.UserKeyValueSettings.PREFERRED_LOCALE);
				this.userLocale.setKvValue(userLocaleValue);
				this.userLocale.setManualEditAllowed(true);

				this.userTimeZone = new UserKeyValueSettings();
				this.userTimeZone.setId(null);
				this.userTimeZone.setUserId(this.user.getId());
				this.userTimeZone.setKvKey(ToolboxSysKeys.UserKeyValueSettings.PREFERRED_TIME_ZONE);
				this.userTimeZone.setKvValue(userTimeZoneValue);
				this.userTimeZone.setManualEditAllowed(true);

				this.userMeasurementSystem = new UserKeyValueSettings();
				this.userMeasurementSystem.setId(null);
				this.userMeasurementSystem.setUserId(this.user.getId());
				this.userMeasurementSystem.setKvKey(ToolboxSysKeys.UserKeyValueSettings.PREFERRED_MEASUREMENT_SYSTEM);
				this.userMeasurementSystem.setKvValue(userMeasurementSystemValue);
				this.userMeasurementSystem.setManualEditAllowed(true);

				this.userTheme = new UserKeyValueSettings();
				this.userTheme.setId(null);
				this.userTheme.setUserId(this.user.getId());
				this.userTheme.setKvKey(ToolboxSysKeys.UserKeyValueSettings.PREFERRED_THEME);
				this.userTheme.setKvValue(userThemeValue);
				this.userTheme.setManualEditAllowed(true);

				this.userKeyValueSettingsService.saveAll(this.userCurrency, this.userLocale, this.userTimeZone, this.userTheme, this.userMeasurementSystem); // a tranzakció kezelés miatt egyben kell lennie

				// TODO: ki kellene írni, hogy megváltoztak a beállítások és csak utána pár másodperccel újratölteni

				// ---

				if (this.getParent() instanceof Window) {
					((Window) this.getParent()).close(); // ez akkor fontos, ha @PreserveOnRefresh van a UI-on (ekkor a UI.getCurrent().getPage().reload() után újra felkönne az ablak)
				}

				UI.getCurrent().getPage().reload(); // mentés után újratöltünk, hogy a téma, nyelv stb. érvénysüljön // TODO: nem lehet valahogy csak a Vaadin UI-t újratölteni (úgy, hogy a böngészőben ne legyen full újratöltés)

			});

			final HorizontalLayout hlButtons = new HorizontalLayout();
			hlButtons.setWidth("100%");
			// hlButtons.setMargin(new MarginInfo(true, false, false, false));
			hlButtons.addComponent(btnSave);
			hlButtons.setComponentAlignment(btnSave, Alignment.MIDDLE_LEFT);

			vlContent.addComponent(hlButtons);

		}

		vlContent.setMargin(new MarginInfo(true, false, false, false));

		return vlContent;
	}

	private VerticalLayout buildTwoFactorAuthTab() {

		final VerticalLayout vlContent = new VerticalLayout();
		vlContent.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);

		final Wizard wizard = new Wizard();
		wizard.getFinishButton().setVisible(false);
		wizard.getFinishButton().addStyleName(ValoTheme.BUTTON_PRIMARY);
		wizard.getCancelButton().setVisible(false);

		final WizardStep wizardStep = new WizardStep() {

			@Override
			public String getCaption() {
				return I.trc("Caption", "What would you like to do?");
			}

			@Override
			public Component getContent() {
				final String strAdd = I.trc("Caption", "Enable two factor authentication");
				final String strRemove = I.trc("Caption", "Disable two factor authentication");

				final VerticalLayout vlcon = new VerticalLayout();

				final RadioButtonGroup<String> rbgMethod = new RadioButtonGroup<>();
				rbgMethod.addStyleName(ValoTheme.OPTIONGROUP_LARGE);

				final List<String> methods = new ArrayList<>();
				methods.add(strAdd);
				methods.add(strRemove);

				rbgMethod.setItems(methods);

				final UserPassword userPassword = ApplicationContextHelper.getBean(UserPasswordJdbcRepository.class).findUserPassword(SecurityUtil.getLoggedInUser().getTenantId(), SecurityUtil.getLoggedInUser().getId());

				rbgMethod.setItemEnabledProvider(item -> {
					if (userPassword.getTwoFactorEnabled() != null && userPassword.getTwoFactorEnabled()) {
						return strRemove.equalsIgnoreCase(item);
					} else {
						return strAdd.equalsIgnoreCase(item);
					}
				});

				rbgMethod.addValueChangeListener(x -> {
					if (StringUtils.equalsIgnoreCase(x.getValue(), strAdd)) {
						MyProfileComponent.this.isAddTwoFactor = true;
					} else {
						MyProfileComponent.this.isAddTwoFactor = false;
					}

					wizard.getNextButton().setVisible(true);
				});

				if (rbgMethod.getValue() == null) {
					wizard.getNextButton().setVisible(false);
				}

				vlcon.addComponent(rbgMethod);

				return vlcon;
			}

			@Override
			public boolean onAdvance() {
				wizard.getNextButton().setVisible(false);
				wizard.getFinishButton().setVisible(true);

				if (MyProfileComponent.this.isAddTwoFactor == null) {
					return false;
				}

				if (MyProfileComponent.this.isAddTwoFactor) {

					UiHelper.centerParentWindow(vlContent);
					return true;

				} else {

					ApplicationContextHelper.getBean(TwoFactorCredentialJdbcRepository.class).removeTwoFactorAuth(SecurityUtil.getLoggedInUser().getUsername());

					UiHelper.closeParentWindow(MyProfileComponent.this);

					Notification n = new Notification(I.trc("Notification", "Two factor auth. has been disabled!"));
					n.setDelayMsec(3000);
					n.show(UI.getCurrent().getPage());

					return false;

				}

			}

			@Override
			public boolean onBack() {
				return false;
			}
		};

		wizard.addStep(wizardStep);

		final WizardStep wizardStep2 = new WizardStep() {

			final GoogleAuthenticator gAuth = new GoogleAuthenticator();

			final GoogleAuthenticatorKey key = this.gAuth.createCredentials();

			@Override
			public String getCaption() {
				return I.trc("Caption", "Finish action");
			}

			@Override
			public Component getContent() {

				UiHelper.centerParentWindow(vlContent);

				final VerticalLayout vlcon = new VerticalLayout();

				String keyUri = "";
				try {

					String appName = ApplicationContextHelper.getConfigProperty("tools.misc.application-name");

					if (ApplicationContextHelper.hasDevProfile()) {
						appName += " (dev)";
					}

					keyUri = generateKeyUri(SecurityUtil.getLoggedInUser().getUsername(), appName, this.key.getKey());
				} catch (final URISyntaxException e) {
					Notification.show(I.trc("Notification", "Could not generate QR code (system error)!"), Notification.Type.ERROR_MESSAGE);
				}

				final HorizontalLayout hlColumns = new HorizontalLayout();
				hlColumns.setWidth("100%");
				hlColumns.setSpacing(true);
				vlcon.addComponent(hlColumns);

				final QrCodeSvgComponent qrCodeSvgComponent = new QrCodeSvgComponent(keyUri);
				qrCodeSvgComponent.setWidth("100%");
				qrCodeSvgComponent.setHeight("100%");
				hlColumns.addComponent(qrCodeSvgComponent);
				hlColumns.setExpandRatio(qrCodeSvgComponent, 0.3f);

				// ---

				VerticalLayout vlRightSide = new VerticalLayout();
				vlRightSide.setWidth("100%");
				vlRightSide.setMargin(false);
				hlColumns.addComponent(vlRightSide);
				hlColumns.setExpandRatio(vlRightSide, 0.7f);

				final Label lblText = new Label(I.trc("Caption", "Please scan the QR Code or Enter the key manually into your app") + ": "
						+ "<h2><a href=\"" + Jsoup.clean(keyUri, Safelist.none()) + "\">" + Jsoup.clean(this.key.getKey(), Safelist.none()) + "</a></h2>"
						+ I.trc("Caption", "To finish the process please type in the generated code first") + ":", ContentMode.HTML);

				vlRightSide.addComponent(lblText);

				final TextField txtCode = new TextField(I.trc("Caption", "Code"));
				txtCode.setMaxLength(6);
				txtCode.setWidth("100%");
				txtCode.focus();
				vlRightSide.addComponent(txtCode);

				final Button btnCheckAndVerifyTwoFactor = new Button(I.trc("Button", "Verify"));
				btnCheckAndVerifyTwoFactor.addStyleName(ValoTheme.BUTTON_FRIENDLY);
				btnCheckAndVerifyTwoFactor.setWidth("150px");
				btnCheckAndVerifyTwoFactor.addClickListener(event -> {

					final String value = txtCode.getValue();
					final Integer totp = Integer.valueOf((value.equals("") ? "-1" : value));

					// verify the password
					final boolean matches = this.gAuth.authorize(this.key.getKey(), totp);

					// ---

					Notification n;
					if (matches) {
						n = new Notification(I.trc("Notification", "Correct") + "!");
						n.setDelayMsec(3000);
						MyProfileComponent.this.isVerified = true;
					} else {
						n = new Notification(I.trc("Notification", "Incorrect") + "!", Notification.Type.WARNING_MESSAGE);
						MyProfileComponent.this.isVerified = false;
					}
					n.show(UI.getCurrent().getPage());

				});

				vlRightSide.addComponent(btnCheckAndVerifyTwoFactor);

				txtCode.addShortcutListener(new ShortcutListener("CheckAndVerifyTwoFactor", ShortcutAction.KeyCode.ENTER, null) {

					@Override
					public void handleAction(Object sender, Object target) {
						btnCheckAndVerifyTwoFactor.click();
					}
				});

				return vlcon;
			}

			@Override
			public boolean onAdvance() {

				if (!Boolean.TRUE.equals(MyProfileComponent.this.isVerified)) {
					UiHelper.centerParentWindow(vlContent);
					Notification.show(I.trc("Notification", "Please verify first!"), Notification.Type.WARNING_MESSAGE);
					return false;
				}

				ApplicationContextHelper.getBean(TwoFactorCredentialJdbcRepository.class).saveUserCredentials(SecurityUtil.getLoggedInUser().getUsername(), this.key.getKey(), this.key.getVerificationCode(), this.key.getScratchCodes());

				UiHelper.closeParentWindow(MyProfileComponent.this);

				Notification n = new Notification(I.trc("Notification", "Two factor auth. has been enabled!"));
				n.setDelayMsec(3000);
				n.show(UI.getCurrent().getPage());

				return true;

			}

			@Override
			public boolean onBack() {

				UiHelper.centerParentWindow(vlContent);

				wizard.getNextButton().setVisible(true);
				wizard.getFinishButton().setVisible(false);

				return true;
			}

		};

		wizard.addStep(wizardStep2);

		vlContent.addComponent(wizard);

		return vlContent;
	}

	private static String generateKeyUri(final String account, final String issuer, final String secret) throws URISyntaxException {
		final URI uri = new URI("otpauth", "totp", "/" + issuer + ":" + account, "secret=" + secret + "&issuer=" + issuer, null);

		return uri.toASCIIString();

	}

}