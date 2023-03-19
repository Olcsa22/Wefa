package hu.lanoga.toolbox.user;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.domain.Page;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.UserAuth;
import hu.lanoga.toolbox.auth.token.AuthToken;
import hu.lanoga.toolbox.auth.token.AuthTokenService;
import hu.lanoga.toolbox.email.EmailTemplateService;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.json.orgjson.OrgJsonUtil;
import hu.lanoga.toolbox.service.LazyEnhanceCrudService;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.tenant.Tenant;
import hu.lanoga.toolbox.tenant.TenantService;
import hu.lanoga.toolbox.util.BrandUtil;
import hu.lanoga.toolbox.util.ToolboxAssert;
import net.coobird.thumbnailator.Thumbnailator;

@ConditionalOnMissingBean(name = "userServiceOverrideBean")
@Service
public class UserService implements LazyEnhanceCrudService<User> {

	@Autowired
	private UserJdbcRepository userJdbcRepository;

	@Autowired
	private UserPasswordJdbcRepository userPasswordJdbcRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private EmailTemplateService emailTemplateService;

	@Autowired
	private FileStoreService fileService;

	@Autowired
	private TenantService tenantService;

	@Autowired
	private AuthTokenService authTokenService;

	@Autowired
	private UserInfoJdbcRepository userInfoJdbcRepository;

	@Value("${tools.misc.application-name}")
	private String appName;

	// /**
	// * regisztrációkor ellenőrzi, hogy a felhasználónév szabad-e (belépett user tenant-jában)
	// *
	// * @param username
	// * @return true, ha még nincs iyen
	// */
	// public boolean checkUsernameAvailability(final String username) {
	// return !userJdbcRepository.existsBy("username", username);
	// }

	@Override
	public User enhance(final User user) {

		if (user != null) {

			if (user.getCreatedBy() != null) {
				user.setCreatedByCaption(I18nUtil.buildFullName(this.userJdbcRepository.findOne(user.getCreatedBy()), true, false));
			}

			if (user.getModifiedBy() != null) {
				user.setModifiedByCaption(I18nUtil.buildFullName(this.userJdbcRepository.findOne(user.getModifiedBy()), true, false));
			}

		}

		return user;
	}

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	@Transactional
	public void delete(final int id) {
		throw new UnsupportedOperationException();
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	@Transactional
	public void disable(final int id) {
		this.userJdbcRepository.disable(id);
	}

	@Override
	@Secured(UserAuth.ROLE_USER_STR) // fontos: ez nem lazítható LCU-ra, mert az összes vásárlót láthatná így bármelyik vásárlót...
	public List<User> findAll() {
		return this.userJdbcRepository.findAll();
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public List<User> findAllEnabledUser() {
		return this.userJdbcRepository.findAllBy("enabled", true);
	}

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR) // "rendes" usernek direkt engedtük a listázást/lekérést (pl. EGO névjegy lista view-hoz)
	public Page<User> findAll(final BasePageRequest<User> pageRequest) {
		final Page<User> users = this.userJdbcRepository.findAll(pageRequest);
		for (final User user : users) {
			this.enhance(user);
		}
		return users;
	}

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public User findOne(final int id) {

		// "rendes" usernek direkt engedtük a listázást/lekérést (pl. EGO névjegy lista view-hoz)
		// LCU user viszont csak saját magát "kérheti" le

		SecurityUtil.limitAccess(ToolboxSysKeys.UserAuth.ROLE_USER_STR, ToolboxSysKeys.UserAuth.ROLE_LCU_STR, id);

		return this.userJdbcRepository.findOne(id);
	}

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	public long count() {
		return this.userJdbcRepository.count();
	}

	/**
	 * user admin általi létrehozásá/módosítása, user saját adatainak módosítása
	 * (jelszó nem adható meg itt, új user admin általi felvitelénél generált van,
	 * meglévő user admin/maga általi módosításnál a password itt transient-ként viselkedik)
	 *
	 * @param user
	 * @return
	 */
	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	@Transactional
	public User save(final User user) {

		if (user.isNew()) {
			return this.saveInnerNew(user);
		} else {
			return this.saveInnerUpdate(user);
		}

	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	@Transactional
	public User save(final User user, final boolean skipPasswordEmail) {

		if (user.isNew()) {
			return this.saveInnerNew(user, skipPasswordEmail);
		} else {
			return this.saveInnerUpdate(user);
		}

	}

	protected User saveInnerNew(final User user) {
		return this.saveInnerNew(user, false);
	}

	protected User saveInnerNew(final User user, final boolean skipPasswordEmail) {

		SecurityUtil.limitAccessAdmin(); // új usert jelenleg csak az admin vihet fel

		ToolboxAssert.isTrue(user.getUserRoles().startsWith("["));
		ToolboxAssert.isTrue(user.getUserRoles().endsWith("]"));
		// ToolboxAssert.isTrue(!user.getAuthorities().contains(new SimpleGrantedAuthority(UserAuth.ROLE_SUPER_ADMIN_STR)));

		ToolboxAssert.isTrue(!UserAuth.SYSTEM_USERNAME.equalsIgnoreCase(user.getUsername()));

		// ---

		{
			JSONArray jaUserRoles = this.addAdditionalRoles(user);
			jaUserRoles = OrgJsonUtil.jsonArrayIntegerOrder(jaUserRoles, false);

			user.setUserRoles(jaUserRoles.toString());

			user.setAnonymizedDeleted(false);

		}

		// ---

		final User savedUser = this.userJdbcRepository.save(user);

		// ---

		{

			final UserInfo userInfo = new UserInfo();

			userInfo.setUserId(savedUser.getId());
			userInfo.setDateOfBirth(user.getDateOfBirth());
			userInfo.setEmail(user.getEmail());
			userInfo.setTitle(user.getTitle());
			userInfo.setFamilyName(user.getFamilyName());
			userInfo.setGivenName(user.getGivenName());
			userInfo.setJobTitle(user.getJobTitle());

			if (StringUtils.isNotBlank(user.getPhoneNumber())) {
				userInfo.setPhoneNumber(user.getPhoneNumber());
			} else {
				userInfo.setPhoneNumber(null);
			}

			this.userInfoJdbcRepository.save(userInfo);
		}

		// ---

		final String genPassword = SecurityUtil.generateRandomPassword();

		{

			final UserPassword userPassword = new UserPassword();
			userPassword.setPassword(this.passwordEncoder.encode(genPassword));
			userPassword.setUserId(savedUser.getId());
			this.userPasswordJdbcRepository.save(userPassword);

		}

		// ---

		if (!skipPasswordEmail) {

			// a user megadott email címére elküldi a jelszót...

			final Map<String, Object> valueMapForMailTemplate = new HashMap<>();
			valueMapForMailTemplate.put("data", genPassword);

			final String redirectUriHostFrontend = BrandUtil.getRedirectUriHostFrontend();
			valueMapForMailTemplate.put("url", "<a href=\"" + redirectUriHostFrontend + "\">" + redirectUriHostFrontend + "</a>");
			valueMapForMailTemplate.put("recipient", I18nUtil.buildFullName(savedUser, false) + " (" + user.getUsername() + ")");

			final Tenant tenant = this.tenantService.findOne(savedUser.getTenantId());
			valueMapForMailTemplate.put("tenant", tenant.getName());

			String applicationName = this.appName;
			if (ApplicationContextHelper.hasDevProfile()) {
				applicationName = this.appName + " (dev)";
			}

			valueMapForMailTemplate.put("appname", applicationName);

			this.emailTemplateService.addMail(SecurityUtil.getLoggedInUser(), user.getEmail(), ToolboxSysKeys.EmailTemplateType.GENERATED_PASSWORD, valueMapForMailTemplate, I18nUtil.getLoggedInUserLocale()); // ilyenkor a küldött email a létrehozó user nyelvén lesz (tehát az adminén)

		}

		return savedUser;
	}

	protected User saveInnerUpdate(final User user) {

		SecurityUtil.limitAccessAdminOrOwner(user.getId()); // admin mindent módosíthat vagy "saját magát" módosíthatja a belépett user

		ToolboxAssert.isTrue(user.getUserRoles().startsWith("["));
		ToolboxAssert.isTrue(user.getUserRoles().endsWith("]"));
		// ToolboxAssert.isTrue(!user.getAuthorities().contains(new SimpleGrantedAuthority(UserAuth.ROLE_SUPER_ADMIN_STR))); // újabban már nincs ilyen limitáció, lehet több ROLE_SUPER_ADMIN user is már
		ToolboxAssert.isTrue(!UserAuth.SYSTEM_USERNAME.equalsIgnoreCase(user.getUsername()));

		JSONArray jaUserRoles = this.addAdditionalRoles(user);
		jaUserRoles = OrgJsonUtil.jsonArrayIntegerOrder(jaUserRoles, false);
		user.setUserRoles(jaUserRoles.toString());

		// TODO: ez a leaveOutFields nem volt használva már, miért is?
		// final Set<String> leaveOutFields = new HashSet<>();
		//
		// leaveOutFields.add("username");
		// leaveOutFields.add("password");
		//
		// if (!SecurityUtil.hasAdminRole()) {
		// leaveOutFields.add("enabled");
		// leaveOutFields.add("userRoles");
		// }

		if (!user.isEnabled()) {
			SecurityUtil.expireUserSessions(user.getId());
		}

		final User savedUser = this.userJdbcRepository.save(user);

		final UserInfo userInfo = this.userInfoJdbcRepository.findOneBy("userId", savedUser.getId());

		userInfo.setDateOfBirth(user.getDateOfBirth());
		userInfo.setEmail(user.getEmail());
		userInfo.setTitle(user.getTitle());
		userInfo.setFamilyName(user.getFamilyName());
		userInfo.setGivenName(user.getGivenName());
		userInfo.setJobTitle(user.getJobTitle());

		if (StringUtils.isNotBlank(user.getPhoneNumber())) {
			userInfo.setPhoneNumber(user.getPhoneNumber());
		} else {
			userInfo.setPhoneNumber(null);
		}

		this.userInfoJdbcRepository.save(userInfo);

		return savedUser;
	}

	@Transactional
	public void updateUserPasswordByUserId(final int id, final String password) {
		SecurityUtil.limitAccessSystem();
		this.userPasswordJdbcRepository.updateUserPasswordByUserId(id, new BCryptPasswordEncoder(12).encode(password));
	}

	@Transactional
	public void updateUserPasswordByUserId(final int id, final String password, final AuthToken authTokenToInvalidate) {
		SecurityUtil.limitAccessSystem();
		this.updateUserPasswordByUserId(id, password);
		this.authTokenService.invalidateToken(authTokenToInvalidate);
	}

	@Transactional
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	public void updateUserPasswordByUserIdManual(final int id, final String password) {
		this.userPasswordJdbcRepository.updateUserPasswordByUserId(id, new BCryptPasswordEncoder(12).encode(password));
	}

	/**
	 * amennyiben a user role-ok között nem szerepelnek ezek az alapértelmezett role-ok,  
	 * akkor hozzáadódnak a JSON array-hez... 
	 * 
	 * plusz némi validáció is van már itt... 
	 *
	 * @param user
	 * @return
	 */
	protected JSONArray addAdditionalRoles(final User user) {

		// TODO: rossz név, ez nem addol semmit, a user object nem is változik!!!

		// lásd még hu.lanoga.toolbox.vaadin.component.UserRoleField
		
		final JSONArray jaUserRoles = new JSONArray(user.getUserRoles());
		
		// ---

		if (!hasRoleInJSONArray(jaUserRoles, UserAuth.ROLE_USER_CS_ID) && SecurityUtil.getLoggedInUserTenantId() != 2) {
			jaUserRoles.put(UserAuth.ROLE_USER_CS_ID);
		}

		if (!hasRoleInJSONArray(jaUserRoles, UserAuth.ROLE_LCU_CS_ID)) {
			jaUserRoles.put(UserAuth.ROLE_LCU_CS_ID);
		}

		if (!hasRoleInJSONArray(jaUserRoles, UserAuth.ROLE_ANONYMOUS_CS_ID)) {
			jaUserRoles.put(UserAuth.ROLE_ANONYMOUS_CS_ID);
		}

		// ---

		final JSONArray jaUserRolesFromDb;

		if (user.isNew()) {
			jaUserRolesFromDb = new JSONArray();
		} else {
			jaUserRolesFromDb = new JSONArray(findOne(user.getId()).getUserRoles());
		}

		// ---

		if (hasRoleInJSONArray(jaUserRoles, UserAuth.ROLE_SUPER_ADMIN_CS_ID) && !hasRoleInJSONArray(jaUserRolesFromDb, UserAuth.ROLE_SUPER_ADMIN_CS_ID)) {
			SecurityUtil.limitAccessSystemOrSuperAdmin();
		}
		if (hasRoleInJSONArray(jaUserRoles, UserAuth.ROLE_SUPER_USER_CS_ID) && !hasRoleInJSONArray(jaUserRolesFromDb, UserAuth.ROLE_SUPER_USER_CS_ID)) {
			SecurityUtil.limitAccessSystemOrSuperAdmin();
		}
		if (hasRoleInJSONArray(jaUserRoles, UserAuth.ROLE_SUPER_REMOTE_CS_ID) && !hasRoleInJSONArray(jaUserRolesFromDb, UserAuth.ROLE_SUPER_REMOTE_CS_ID)) {
			SecurityUtil.limitAccessSystemOrSuperAdmin();
		}
		if (hasRoleInJSONArray(jaUserRoles, UserAuth.ROLE_SUPER_TENANT_OVERSEER_CS_ID) && !hasRoleInJSONArray(jaUserRolesFromDb, UserAuth.ROLE_SUPER_TENANT_OVERSEER_CS_ID)) {
			SecurityUtil.limitAccessSystemOrSuperAdmin();
		}
		if (hasRoleInJSONArray(jaUserRoles, UserAuth.ROLE_TENANT_OVERSEER_CS_ID) && !hasRoleInJSONArray(jaUserRolesFromDb, UserAuth.ROLE_TENANT_OVERSEER_CS_ID)) {
			SecurityUtil.limitAccessSystemOrSuperAdmin();
		}

		// ---

		if (user.getUsername().startsWith("jump.")) {
			
			// itt visszateszünk dolgokat, mert a UserRoleField leszedte

			if (!hasRoleInJSONArray(jaUserRoles, UserAuth.ROLE_SUPER_ADMIN_CS_ID) && hasRoleInJSONArray(jaUserRolesFromDb, UserAuth.ROLE_SUPER_ADMIN_CS_ID)) {
				jaUserRoles.put(UserAuth.ROLE_SUPER_ADMIN_CS_ID);
			}
			if (!hasRoleInJSONArray(jaUserRoles, UserAuth.ROLE_SUPER_USER_CS_ID) && hasRoleInJSONArray(jaUserRolesFromDb, UserAuth.ROLE_SUPER_USER_CS_ID)) {
				jaUserRoles.put(UserAuth.ROLE_SUPER_USER_CS_ID);
			}
			if (!hasRoleInJSONArray(jaUserRoles, UserAuth.ROLE_SUPER_REMOTE_CS_ID) && hasRoleInJSONArray(jaUserRolesFromDb, UserAuth.ROLE_SUPER_REMOTE_CS_ID)) {
				jaUserRoles.put(UserAuth.ROLE_SUPER_REMOTE_CS_ID);
			}
			if (!hasRoleInJSONArray(jaUserRoles, UserAuth.ROLE_SUPER_TENANT_OVERSEER_CS_ID) && hasRoleInJSONArray(jaUserRolesFromDb, UserAuth.ROLE_SUPER_TENANT_OVERSEER_CS_ID)) {
				jaUserRoles.put(UserAuth.ROLE_SUPER_TENANT_OVERSEER_CS_ID);
			}
			if (!hasRoleInJSONArray(jaUserRoles, UserAuth.ROLE_TENANT_OVERSEER_CS_ID) && hasRoleInJSONArray(jaUserRolesFromDb, UserAuth.ROLE_TENANT_OVERSEER_CS_ID)) {
				jaUserRoles.put(UserAuth.ROLE_TENANT_OVERSEER_CS_ID);
			}

		}

		// ---

		return jaUserRoles;
	}

	public String addAdditionalRoles2(final User user) {
		return this.addAdditionalRoles(user).toString();
	}

	/**
	 * megvizsgálja, hogy az adott JSON array-ben szerepel-e a megadott role id-ja 
	 * (amely a {@link hu.lanoga.toolbox.ToolboxSysKeys.UserAuth}-ban található)
	 * 
	 * @param jsonArray
	 * @param roleCSId
	 * 
	 * @return true értékkel tér vissza, ha megtalálja a role-t a JSON array-ben
	 */
	protected static boolean hasRoleInJSONArray(final JSONArray jsonArray, final int roleCSId) {
		boolean foundRole = false;
		for (int i = 0; i < jsonArray.length(); ++i) {
			if (roleCSId == jsonArray.getInt(i)) {
				foundRole = true;
			}
		}
		return foundRole;
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	@Transactional
	public User savePasswordForLoggedInUser(final SavePassword1 savePassword) {

		final ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();

		final UserPassword userPassword = this.userPasswordJdbcRepository.findUserPassword(loggedInUser.getTenantId(), loggedInUser.getId());
		userPassword.setPassword(this.passwordEncoder.encode(savePassword.getNewPassword()));

		this.userPasswordJdbcRepository.save(userPassword);

		return (User) loggedInUser;
	}

	/**
	 * a belépett user-hez állítja be
	 * 
	 * @param fileDescriptor
	 *            ezt képet átméretezi 500*500 px felbontásra; az új kép {@link FileDescriptor}-át visszaadja; a régi/orig (nagy) kép törlendőnek lesz jelölve...
	 * @return
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	@Transactional
	public FileDescriptor saveProfileImgForLoggedInUser(final FileDescriptor fileDescriptor) {

		try {

			FileDescriptor tmpFile = null;
			FileDescriptor tmpFile2 = null;

			try {

				tmpFile = this.fileService.getFile2(fileDescriptor.getId());
				tmpFile2 = this.fileService.createTmpFile2("user-img-thumb.jpg", ToolboxSysKeys.FileDescriptorLocationType.PROTECTED_FOLDER, ToolboxSysKeys.FileDescriptorSecurityType.AUTHENTICATED_USER);

				try (InputStream is = new BufferedInputStream(new FileInputStream(tmpFile.getFile()), 128 * 1024); OutputStream os = new BufferedOutputStream(new FileOutputStream(tmpFile2.getFile()), 128 * 1024)) {
					Thumbnailator.createThumbnail(is, os, "JPEG", 500, 500);
				}

			} catch (final Exception e) {

				if (tmpFile2 != null) {
					this.fileService.setToBeDeleted(tmpFile2.getId());
				}

				throw e;

			} finally {

				if (tmpFile != null) {
					this.fileService.setToBeDeleted(tmpFile.getId());
				}

			}

			// ---

			this.fileService.setToNormal(tmpFile2.getId());

			final User loggedInUser = (User) SecurityUtil.getLoggedInUser();
			final Integer oldProfileImgId = loggedInUser.getProfileImg();

			loggedInUser.setProfileImg(tmpFile2.getId());

			this.userJdbcRepository.save(loggedInUser);

			if (oldProfileImgId != null) {

				// amennyiben volt már prof. képe, akkor azt töröltetjük...
				this.fileService.setToBeDeleted(oldProfileImgId);

			}

			return tmpFile2;

		} catch (final Exception e) {

			throw new ToolboxGeneralException(e);
		}
	}

	/**
	 * elfelejtett jelszó feature-höz email cím -> user objektum keresés...
	 * 
	 * @param emailAddress
	 * @return
	 */
	protected User findByEmailAddressForForgottenPassword(final String emailAddress) {

		// fontos, hogy itt legyen protected-ként, egyes projektekben felülírjuk

		return this.userJdbcRepository.findOneBy("email", emailAddress);
	}

	/**
	 * milyen email címen értesítsük az admin usereket új felhasználó regisztrációjáról/regisztrációs kérelméről...
	 * 
	 * @param adminUser
	 * @return
	 */
	protected List<String> findEmailAddressesForAdminNotification(final User adminUser) {

		// fontos, hogy itt legyen protected-ként, egyes projektekben felülírjuk

		return Lists.newArrayList(adminUser.getEmail());
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public boolean userHasRole(final String username, final String role) {
		final User user = this.userJdbcRepository.findOneBy("username", username);

		if (user != null) {
			return SecurityUtil.hasRole(user, role);
		} else {
			return false;
		}
	}

	/**
	 * csak enabled user-ek
	 * 
	 * @param userRoleId
	 * @return
	 */
	public List<User> findAllUserByRole(final int userRoleId) {
		return this.userJdbcRepository.findUsersByRole(userRoleId);
	}

	/**
	 * csak enabled user-ek
	 * 
	 * @return
	 */
	public List<User> findAdminUsers() {
		return this.userJdbcRepository.findAdminUsers();
	}

	/**
	 * csak enabled user-ek
	 * 
	 * @return
	 */
	public List<Integer> findAdminUserIds() {
		return this.userJdbcRepository.findAdminUserIds();
	}

}
