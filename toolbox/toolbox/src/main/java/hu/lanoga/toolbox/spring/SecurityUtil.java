
package hu.lanoga.toolbox.spring;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.json.JSONArray;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Chars;
import com.vaadin.ui.UI;
import com.warrenstrange.googleauth.GoogleAuthenticator;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.UserAuth;
import hu.lanoga.toolbox.codestore.CodeStoreItem;
import hu.lanoga.toolbox.codestore.CodeStoreItemJdbcRepository;
import hu.lanoga.toolbox.config.SecurityConfig;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.service.RecordAccessCheckService;
import hu.lanoga.toolbox.session.LcuHelperSessionBean;
import hu.lanoga.toolbox.tenant.Tenant;
import hu.lanoga.toolbox.tenant.TenantJdbcRepository;
import hu.lanoga.toolbox.tenant.TenantService;
import hu.lanoga.toolbox.twofactorcredential.TwoFactorCredential;
import hu.lanoga.toolbox.twofactorcredential.TwoFactorCredentialJdbcRepository;
import hu.lanoga.toolbox.user.User;
import hu.lanoga.toolbox.user.UserJdbcRepository;
import hu.lanoga.toolbox.user.UserPassword;
import hu.lanoga.toolbox.user.UserPasswordJdbcRepository;
import hu.lanoga.toolbox.user.UserService;
import hu.lanoga.toolbox.util.ToolboxAssert;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Security-hoz segítség, aktuális belépett user 
 * (akihez az aktuális HTTP request/session tartozik és/vagy kézzel beléptetett) 
 * neve, role-jai...
 *
 * @see ToolboxUserDetails
 * @see ToolboxUserDetailsService
 * @see SecurityContext
 * @see Authentication
 */
@Slf4j
public final class SecurityUtil {

	/**
	 * @param fromHeader
	 * @return
	 * 		true esetén noRedirect jellegű a kliens (Angular, Postman stb.) 
	 */
	public static boolean checkForNoRedirectClient(final String fromHeader) {

		// TODO: itt a "prefer-redirects" vagy hasonló, később kitatlált header alapján kellene (de egyelőre jó így) (lényeg, hogy tisztáni a header név/érték-et, ami az elágazásban szerepel)

		return StringUtils.isNotBlank(fromHeader) && (fromHeader.contains("client-software="));
	}

	/**
	 * @return
	 * 		true ha szükséges
	 */
	/**
	 * @param username
	 * 		a login-nál megadott, "full" username (tehát a tenantId vagy tenantName is benne van (vagy default tenant)...)
	 * @return
	 */
	public static boolean checkIfTwoFactorAuthIsNeeded(final String username) {

		final boolean twoFactorAuthEnabled = ApplicationContextHelper.getConfigProperty("tools.login.two-factor-auth", Boolean.class); // eleve a projectben be van-e kapcsolva

		// ha nincs bekapcsolva a two factor auth, vagy nincs használva az advanced login screen, akkor nem ellenőrzünk
		if (!twoFactorAuthEnabled) {
			return false;
		}
		ToolboxUserDetails toolboxUserDetails;

		try {
			toolboxUserDetails = (ToolboxUserDetails) ApplicationContextHelper.getBean(UserDetailsService.class).loadUserByUsername(username);
		} catch (final UsernameNotFoundException e) {
			return false; // azért adunk itt vissza false-t, mert különben elnyelődik a téves felhasználónév vagy jelszó üzenet
		}

		final UserPassword userPassword = ApplicationContextHelper.getBean(UserPasswordJdbcRepository.class).findUserPassword(toolboxUserDetails.getTenantId(), toolboxUserDetails.getId());

		if (Boolean.TRUE.equals(userPassword.getTwoFactorEnabled())) {
			return true;
		}

		return false;

	}

	/**
	 * minimum 8 karakter, kell legyen benne: kis- és nagybetű, illetve szám is (nem kell hozzá Spring context)
	 *
	 * @param passwordString
	 * @return
	 */
	public static boolean checkPasswordStrengthSimple(final String passwordString) {
		return ((passwordString.length() >= 8) && passwordString.matches(".*[a-z]+.*") && passwordString.matches(".*[A-Z]+.*") && passwordString.matches(".*[0-9]+.*"));
	}

	public static void checkUserPasswordManually(final String username, final String password) {

		final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(username, password);

		ApplicationContextHelper.getApplicationContext().getBean(AuthenticationManager.class).authenticate(usernamePasswordAuthenticationToken);
		// setAuthentication(authentication);
	}

	/**
	 * @param allowSuperRemote
	 * 		true esetén {@link ToolboxSysKeys.UserAuth#ROLE_SUPER_REMOTE_STR} joggal (is) bíró user-t nem blokkolunk
	 * @throws AccessDeniedException
	 */
	public static void denyAccessRemote(final boolean allowSuperRemote) throws AccessDeniedException {

		try {

			if (SecurityUtil.hasRole(ToolboxSysKeys.UserAuth.ROLE_REMOTE_STR)) {

				if (allowSuperRemote && SecurityUtil.hasRole(ToolboxSysKeys.UserAuth.ROLE_SUPER_REMOTE_STR)) {
					return;
				}

				final UI ui = UI.getCurrent();

				SecurityUtil.setAnonymous();

				throw new AccessDeniedException(
						"Users with " + ToolboxSysKeys.UserAuth.ROLE_REMOTE_STR + " cannot access this endpoint/user interface"
								+ (ui != null ? " (" + ui.getClass().getCanonicalName() + ")" : ""));
			}

		} catch (final Exception e) {
			SecurityUtil.setAnonymous();
			throw new AccessDeniedException("denyAccessRemote error", e); // akármi van is, NPE stb. akkor is dobjunk AccessDeniedException-ot, mert hívó oldalon van, ahol erre számítunk
		}

	}

	/**
	 * invalidálja a user összes aktuális HTTP/Spring Security session-jét...
	 * {@link SessionRegistry} kell hozzá!
	 * <p>
	 * (https://mtyurt.net/post/spring-expiring-all-sessions-of-a-user.html alapján)
	 *
	 * @param userId
	 * @see SessionRegistry
	 * @see ApplicationContextHelper#getBean(Class)
	 * @see SessionInformation#expireNow()
	 */
	public static void expireUserSessions(final int userId) {

		// TODO: tesztelni kézzel és unit teszttel, ha megoldható (PT)

		final SessionRegistry sessionRegistry = ApplicationContextHelper.getBean(SessionRegistry.class);

		for (final Object principal : sessionRegistry.getAllPrincipals()) {

			final ToolboxUserDetails userDetails = (ToolboxUserDetails) principal;

			if (userDetails.getId().equals(userId)) {

				for (final SessionInformation information : sessionRegistry.getAllSessions(userDetails, true)) {
					information.expireNow();
				}

				break;
			}

		}

	}

	/**
	 * random jelszó kigenerálása, ami megfelel a {@link #checkPasswordStrengthSimple(String)} számára
	 */
	public static String generateRandomPassword() {
		return generateRandomPassword(10);
	}

	/**
	 * random jelszó kigenerálása, ami megfelel a {@link #checkPasswordStrengthSimple(String)} számára
	 * 
	 * @param length
	 * 		min. 8
	 * @return
	 */
	public static String generateRandomPassword(final int length) {

		ToolboxAssert.isTrue(length >= 8);

		final String lowerCaseChars = RandomStringUtils.random(length - 4, "qwertzuiopasdfghjklyxcvbnm");
		final String upperCaseChars = RandomStringUtils.random(3, "QWERTZUIOPASDFGHJKLYXCVBNM");
		final String numbers = RandomStringUtils.random(2, "0123456789");

		final String concatPass = lowerCaseChars + upperCaseChars + numbers;

		final List<Character> chars = Chars.asList(concatPass.toCharArray());
		Collections.shuffle(chars);
		return RandomStringUtils.random(1, "qwertzuiopasdfghjklyxcvbnm") + StringUtils.join(chars.stream().toArray());

	}

	/**
	 * figyelem saját maga is beszámít!
	 *
	 * @return
	 * @deprecated 
	 * 		használd ezt inkább: {@link #getSessionPrincipalCount()} 
	 */
	@Deprecated
	public static int getLoggedInUserCount() {
		return getSessionPrincipalCount();
	}

	/**
	 * @return
	 * 
	 * @see #getLoggedInUser()
	 */
	public static int getLoggedInUserTenantId() {

		final ToolboxUserDetails loggedInUser = getLoggedInUser();

		if (loggedInUser == null) {
			throw new ToolboxSpringException("Missing loggedInUser (tenantId cannot be inferred)!");
		}

		ToolboxAssert.notNull(loggedInUser.getTenantId());

		return loggedInUser.getTenantId();
	}

	/**
	 * minimum LCU jog kell a metódus hívásához!
	 * 
	 * @return
	 * 
	 * @see #getLoggedInUser()
	 */
	public static String getLoggedInUserTenantName() {

		final ToolboxUserDetails loggedInUser = getLoggedInUser();

		if (loggedInUser == null) {
			throw new ToolboxSpringException("Missing loggedInUser (tenantId/tenantName cannot be inferred)!");
		}

		ToolboxAssert.notNull(loggedInUser.getTenantId());

		final Tenant tenant = ApplicationContextHelper.getBean(TenantService.class).findOne(loggedInUser.getTenantId());

		return tenant.getName();
	}

	/**
	 * hálózati kártya mac address...
	 * soha nem dob exception-t, null-t ad vissza, ha nem tudta megállapítani...
	 * (több kártya esetén nem biztos, hogy az, ami a távoli elérést adja)
	 *
	 * @return
	 */
	public static String getPhysicalAddressHex() {

		try {
			return new String(Hex.encode(NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getHardwareAddress()));
		} catch (final Exception e) {
			log.warn("HardwareAddress cannot be determined");
		}

		return null;

	}

	/**
	 * figyelem saját maga is beszámít!
	 *
	 * @deprecated
	 * 		jelenleg nem megy, megváltozott némileg a {@link SecurityConfig}...
	 *
	 * @return
	 */
	@Deprecated
	public static int getSessionPrincipalCount() {
		return ApplicationContextHelper.getBean(SessionRegistry.class).getAllPrincipals().size();
	}

	// TODO: félkész, nem megy, leragad ennél az egész
	// /**
	// * max 100 user-nyit ad vissza, illetve max 2000 karakter
	// * @return
	// */
	// public static String getSessionPrincipalListStr() {
	//
	// SecurityUtil.limitAccessSuperAdmin();
	//
	// List<Object> allPrincipals = ApplicationContextHelper.getBean(SessionRegistry.class).getAllPrincipals();
	// StringBuilder sb = new StringBuilder();
	//
	// int i = 0;
	//
	// for (Object principal : allPrincipals) {
	// sb.append(Objects.toString(principal, "null"));
	// i++;
	// if (i > 100) {
	// break;
	// }
	// }
	//
	// return StringUtils.abbreviate(sb.toString(), 2000);
	// }

	/**
	 * ellenőrizzük, hogy amennyiben a user-nek be van kapcsolva a two factor, akkor sikeresen megadta-e
	 * 
	 * @param toolboxUserDetails
	 * @param totpStr
	 * 
	 * @return
	 * 		true esetén mehet tovabb (nincs two factor vagy helyesen megadta), false esetén vissza kell dobni a login oldalra (auth error)
	 * 
	 */
	public static boolean handleTwoFactorLogin(final ToolboxUserDetails toolboxUserDetails, final String totpStr) {

		final boolean twoFactorAuthEnabled = ApplicationContextHelper.getConfigProperty("tools.login.two-factor-auth", Boolean.class);

		if (!twoFactorAuthEnabled) {
			return true;
		}

		final UserPassword userPassword = ApplicationContextHelper.getBean(UserPasswordJdbcRepository.class).findUserPassword(toolboxUserDetails.getTenantId(), toolboxUserDetails.getId());

		if (!Boolean.TRUE.equals(userPassword.getTwoFactorEnabled())) {
			return true;
		}

		// ---

		if (StringUtils.isBlank(totpStr) || !StringUtils.isNumeric(totpStr)) {
			return false;
		}

		// ---

		final GoogleAuthenticator gAuth = new GoogleAuthenticator();
		final Integer totp = Integer.parseInt(totpStr);

		final TwoFactorCredential twoFactorCredential = ApplicationContextHelper.getBean(TwoFactorCredentialJdbcRepository.class).getICredential(toolboxUserDetails.getUsername()); // itt kívételsen direktben a repo-t használjuk

		if (gAuth.authorize(twoFactorCredential.getSecretCode(), totp)) {
			return true;
		}

		return false;

	}

	/**
	 * Admin-e a belépett user (van-e {@link ToolboxSysKeys.UserAuth#ROLE_ADMIN_STR} role-ja)?
	 * Security context alapján dolgozik... (=> átírás/update esetén lehet, hogy elavult adatot ad vissza, logout/login kell a frissüléshez)
	 *
	 * @return
	 * @see #getLoggedInUser()
	 * @see ToolboxSysKeys.UserAuth#ROLE_ADMIN_STR
	 */
	public static boolean hasAdminRole() {
		return hasRole(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR);
	}

	/**
	 * legalább egy a megadott role-ok közül megvan-e a belépett usernek...
	 *
	 * @param roles
	 * @return
	 * @see #hasRole(String)
	 */
	public static boolean hasAnyRole(final String... roles) {
		boolean b = false;
		for (final String role : roles) {
			b = b || hasRole(role);
		}
		return b;
	}

	/**
	 * van (bármilyen) belépett user (belértve: system user, anonymous stb.)
	 *
	 * @return
	 */
	public static boolean hasLoggedInUser() {
		return getLoggedInUser() != null;
	}

	/**
	 * @param serviceClass
	 * @param left
	 * @param right
	 * @return
	 * @see RecordAccessCheckService
	 * @deprecated
	 */
	@Deprecated
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <L, R> boolean hasRecordAccess(final Class<? extends RecordAccessCheckService> serviceClass, final L left, final R right) {
		return ApplicationContextHelper.getBean(serviceClass).hasAccess(left, right);
	}

	/**
	 * @param serviceClass
	 * @param leftId
	 * @param rightId
	 * @return
	 * @deprecated
	 */
	@Deprecated
	@SuppressWarnings("rawtypes")
	public static boolean hasRecordAccessByIds(final Class<? extends RecordAccessCheckService> serviceClass, final int leftId, final int rightId) {
		return ApplicationContextHelper.getBean(serviceClass).hasAccessByIds(leftId, rightId);
	}

	/**
	 * @param baseline
	 * @param compared
	 * @return
	 * 		true, ha a compared-nek legalább minden olyan joga megvan, mint a baseline-nak (több lehet!)
	 */
	public static boolean hasAllTheRolesOfTheOtherUser(final ToolboxUserDetails baseline, final ToolboxUserDetails compared) {

		Collection<? extends GrantedAuthority> authoritiesBaseline = baseline.getAuthorities();
		Collection<? extends GrantedAuthority> authoritiesCompared = compared.getAuthorities();

		for (final GrantedAuthority authority1 : authoritiesBaseline) {

			boolean hasMatch = false;

			for (final GrantedAuthority authority2 : authoritiesCompared) {
				if (authority1.getAuthority().equalsIgnoreCase(authority2.getAuthority())) {
					hasMatch = true;
					break;
				}
			}

			if (!hasMatch) {
				return false;
			}

		}

		return true;
	}

	/**
	 * Aktuális (belépett/beléptetett) usernek van-e ilyen role-ja?
	 * Security context alapján dolgozik... (=> átírás/update esetén lehet, hogy elavult adatot ad vissza, logout/login kell a frissüléshez)
	 *
	 * @param role
	 * @return
	 * @see #getLoggedInUser()
	 */
	public static boolean hasRole(final String role) {

		final Authentication auth = getAuthentication();

		if (auth == null) {
			return false;
		}

		final Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();

		for (final GrantedAuthority authority : authorities) {
			if (authority.getAuthority().equalsIgnoreCase(role)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * A param usernek van-e ilyen role-ja?
	 *
	 * @param role
	 * @return
	 * @see #getLoggedInUser()
	 */
	public static boolean hasRole(final User user, final String role) {

		final Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

		for (final GrantedAuthority authority : authorities) {
			if (authority.getAuthority().equalsIgnoreCase(role)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Van-e {@link ToolboxSysKeys.UserAuth#ROLE_SUPER_ADMIN_STR} role-ja a belépett user-nek?
	 * Security context alapján dolgozik...
	 *
	 * @return
	 * @see #getLoggedInUser()
	 * @see ToolboxSysKeys.UserAuth#ROLE_SUPER_ADMIN_STR
	 */
	public static boolean hasSuperAdminRole() {
		return hasRole(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR);
	}

	public static boolean hasSuperTenantOverseerRole() {
		return hasRole(ToolboxSysKeys.UserAuth.ROLE_SUPER_TENANT_OVERSEER_STR);
	}

	public static boolean hasSuperUserRole() {
		return hasRole(ToolboxSysKeys.UserAuth.ROLE_SUPER_USER_STR);
	}

	public static boolean hasTenantOverseerRole() {
		return hasRole(ToolboxSysKeys.UserAuth.ROLE_TENANT_OVERSEER_STR);
	}

	/**
	 * Van-e legalább {@link ToolboxSysKeys.UserAuth#ROLE_SUPER_USER_STR} joga a belépett usernek? 
	 * Figyelem: van ez "alatt" is jog!
	 * 
	 * @see ToolboxSysKeys.UserAuth#ROLE_USER_STR
	 * @see ToolboxSysKeys.UserAuth#ROLE_LCU_STR
	 * @see ToolboxSysKeys.UserAuth#ROLE_ANONYMOUS_STR
	 * 
	 * @return
	 */
	public static boolean hasUserRole() {
		return hasRole(ToolboxSysKeys.UserAuth.ROLE_USER_STR);
	}

	/**
	 * van belépett user (értsd security context) és ő az anonymous user
	 *
	 * @return
	 */
	public static boolean isAnonymous() {
		final ToolboxUserDetails loggedInUser = getLoggedInUser();
		return loggedInUser != null && ToolboxSysKeys.UserAuth.ANONYMOUS_USERNAME.equals(loggedInUser.getUsername());
	}

	/**
	 * belépett user (értsd security context) a "common" tenant-ba tartozik-e? 
	 * (ha nincs belépett user, akkor is false, de ilyen indulásnál fordulhat elő)
	 *
	 * @return
	 */
	public static boolean isCommonTenant() {

		final ToolboxUserDetails loggedInUser = getLoggedInUser();

		if (loggedInUser == null) {
			return false;
		}

		return loggedInUser.getTenantId().equals(ToolboxSysKeys.UserAuth.COMMON_TENANT_ID);
	}

	/**
	 * van belépett user és nem anonymous
	 *
	 * @return
	 */
	public static boolean isNotAnonymous() {
		final ToolboxUserDetails loggedInUser = getLoggedInUser();
		return loggedInUser != null && !ToolboxSysKeys.UserAuth.ANONYMOUS_USERNAME.equals(loggedInUser.getUsername());
	}

	/**
	 * belépett user (értsd security context) az id-val megadott tenant-ba tartozik-e?
	 * (ha nincs belépett user, akkor false, ilyen indulásnál fordulhat elő..,)  
	 * ({@link JdbcRepositoryManager#getTlTenantId()} is számít itt is, de csak system usernél fogadott el)
	 *
	 * @return
	 */
	public static boolean isSameTenant(final int tenantId) {

		final ToolboxUserDetails loggedInUser = getLoggedInUser();

		if (loggedInUser == null) {
			return false;
		}

		if (isSystem() && JdbcRepositoryManager.getTlTenantId() != null) {
			return JdbcRepositoryManager.getTlTenantId().equals(tenantId);
		}

		return loggedInUser.getTenantId().equals(tenantId);
	}

	/**
	 * belépett user (értsd security context) a system user-e... 
	 * ha system user, de úgy van neko jog adva, 
	 * hogy nincs közte a {@link UserAuth#ROLE_ADMIN_STR}, akkor itt false-ot adunk vissza!
	 *
	 * @return
	 * 
	 * @see #setSystemUser()
	 * @see #setSystemUser(String...)
	 * @see #setSystemUserLcu()
	 */
	public static boolean isSystem() {
		return isSystem(UserAuth.ROLE_ADMIN_STR);
	}

	/**
	 * belépett user (értsd security context) a system user-e... 
	 * 
	 * @param checkedRole
	 * 		az is megnézzük, hogy ilyen joga van-e... 
	 * 
	 * @return
	 * 
	 * @see #setSystemUser()
	 * @see #setSystemUser(String...)
	 * @see #setSystemUserLcu()
	 */
	public static boolean isSystem(final String checkedRole) {
		final ToolboxUserDetails loggedInUser = getLoggedInUser();
		return loggedInUser != null &&
				ToolboxSysKeys.UserAuth.SYSTEM_USERNAME.equals(loggedInUser.getUsername()) &&
				hasRole(checkedRole);
	}

	/**
	 * @return
	 * 
	 * @see #setSystemUserLcu()
	 */
	public static boolean isLcuLevelUser() {

		// TODO: tenantId is vizsgálni?

		final Authentication auth = getAuthentication();

		ToolboxAssert.notNull(auth);

		final Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();

		int i = 0;
		int j = 0;

		for (final GrantedAuthority authority : authorities) {
			if (authority.getAuthority().equalsIgnoreCase(UserAuth.ROLE_ANONYMOUS_STR) || authority.getAuthority().equalsIgnoreCase(UserAuth.ROLE_LCU_STR)) {
				j++;
			}
			i++;
		}

		return ((i == 2) && (j == 2)); // pontosan ez a két jog van meg

	}

	public static void checkAgainstLcuGidIfLcu(final String expectedLcuGid) {

		if (SecurityUtil.isLcuLevelUser()) {

			ToolboxAssert.isTrue(StringUtils.isNotBlank(expectedLcuGid));

			final String lcuGid = LcuHelperSessionBean.specialLcuGidRetrieve();

			if (!expectedLcuGid.equals(lcuGid)) {
				throw new AccessDeniedException("LCU GID mismatch... expected: " + expectedLcuGid + ", current: " + lcuGid);
			}

		}

	}

	/**
	 * csak megfelelő userId esetén "enged át" (= nem dob {@link AccessDeniedException}-t)...
	 *
	 * @param requiredUserId
	 * @throws AccessDeniedException
	 * @see #getLoggedInUser()
	 */
	public static void limitAccess(final int requiredUserId) throws AccessDeniedException {

		final ToolboxUserDetails loggedInUser = getLoggedInUser();

		if ((loggedInUser == null) || (requiredUserId != loggedInUser.getId().intValue())) {
			throw new AccessDeniedException("required: userId = " + requiredUserId);
		}
	}

	/**
	 * csak ezen role megléte (aktuális security context usernél) esetén "enged át" (= nem dob {@link AccessDeniedException}-t)...  
	 * pl. {@link ToolboxSysKeys.UserAuth#ROLE_REMOTE_STR} (az STR végű konstans kell itt!)
	 *
	 * @param role
	 * 
	 * @throws AccessDeniedException
	 * 
	 * @see #hasRole(String)
	 * @see #getLoggedInUser()
	 */
	public static void limitAccess(final String role) throws AccessDeniedException {

		if (!(hasRole(role))) {
			throw new AccessDeniedException(role.toUpperCase() + " is required");
		}

	}

	/**
	 * csak megfelelő role és/vagy userId esetén "enged át" (= nem dob {@link AccessDeniedException}-t)...
	 *
	 * @param role1		
	 * 		ha van ilyen joga a usernek, akkor userId-tól függetlenül van hozzáférése
	 * @param role2
	 * 		ha csak ilyen joga a usernek, akkor elvárt pluszban, hogy a userId-ja a requiredUserId paraméterrel egyezzen
	 * @param requiredUserIdForRole2
	 * 		lásd role2 param leírása
	 * 
	 * @throws AccessDeniedException
	 * 
	 * @see #getLoggedInUser()
	 * @see #hasRole (ToolboxSysKeys.UserAuth)
	 */
	public static void limitAccess(final String role1, final String role2, final int requiredUserIdForRole2) throws AccessDeniedException {

		final boolean a1 = hasRole(role1);

		final ToolboxUserDetails loggedInUser = getLoggedInUser();

		final boolean a2 = hasRole(role2) && (loggedInUser != null) && (loggedInUser.getId() != null) && (requiredUserIdForRole2 == loggedInUser.getId().intValue());

		if (!(a1 || a2)) {
			throw new AccessDeniedException("required: role = " + role1 + ", or role = " + role2 + " + userId = " + requiredUserIdForRole2);
		}
	}

	/**
	 * csak {@link ToolboxSysKeys.UserAuth#ROLE_ADMIN_STR} megléte (aktuális security context usernél) esetén "enged át" (= nem dob {@link AccessDeniedException}-t)...
	 *
	 * @throws AccessDeniedException
	 * 
	 * @see #hasRole (ToolboxSysKeys.UserAuth)
	 * 
	 * @see #getLoggedInUser()
	 */
	public static void limitAccessAdmin() throws AccessDeniedException {

		if (!(hasRole(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR))) {
			throw new AccessDeniedException(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR + " is required!");
		}

	}

	/**
	 * csak {@link ToolboxSysKeys.UserAuth#ROLE_ADMIN_STR} és/vagy egyező userId esetén "enged át" (= nem dob {@link AccessDeniedException}-t)... 
	 * (tehát limit access to xy értelemben van) 
	 *
	 * @param requiredUserId ezzel egyező userId rögtön átengedi...
	 * 
	 * @throws AccessDeniedException
	 * 
	 * @see #getLoggedInUser()
	 */
	public static void limitAccessAdminOrOwner(final int requiredUserId) throws AccessDeniedException {
		limitAccess(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR, ToolboxSysKeys.UserAuth.ROLE_LCU_STR, requiredUserId);
	}

	public static void limitAccessRoleUserOrOwner(final int requiredUserId) throws AccessDeniedException {
		limitAccess(ToolboxSysKeys.UserAuth.ROLE_USER_STR, ToolboxSysKeys.UserAuth.ROLE_LCU_STR, requiredUserId);
	}

	/**
	 * többnyire nem kell ez sehol... 
	 * ha gondoskodni akarsz róla, hogy ne legyen belépve rendes user, 
	 * akkor inkább force-olj a {@link #setAnonymous()} metódussal 
	 * (ami kilépteti az eddigi usert (ha volt))
	 * 
	 * @deprecated
	 * 
	 * @throws AccessDeniedException
	 */
	@Deprecated
	public static void limitAccessAnonymousOnly() throws AccessDeniedException {

		if (!isAnonymous()) {
			throw new AccessDeniedException("Only the anonymous user is allowed here!");
		}

	}

	/**
	 * Nem enabled user-t NEM enged! 
	 * Tehát fordítva van a megfogalmazás, mint a többi limitAccess-nél... 
	 */
	public static void limitAccessDisabled() {
		limitAccessDisabled(false);
	}

	/**
	 * Nem enabled user-t NEM enged! 
	 * Tehát fordítva van a megfogalmazás, mint a többi limitAccess-nél...
	 */
	public static void limitAccessDisabled(final boolean noCache) {
		limitAccessDisabled(getLoggedInUser(), noCache);
	}

	/**
	 * Nem enabled user-t NEM enged! 
	 * Tehát fordítva van a megfogalmazás, mint a többi limitAccess-nél...
	 */
	public static void limitAccessDisabled(final ToolboxUserDetails user) {
		limitAccessDisabled(user, false);
	}

	/**
	 * Nem enabled user-t NEM enged! 
	 * Tehát fordítva van a megfogalmazás, mint a többi limitAccess-nél...
	 */
	public static void limitAccessDisabled(ToolboxUserDetails user, final boolean noCache) {

		if (user == null) {
			return;
		}

		if (!ToolboxSysKeys.UserAuth.SYSTEM_USERNAME.equals(user.getUsername()) &&
				!ToolboxSysKeys.UserAuth.LCU_SYSTEM_USERNAME.equals(user.getUsername()) &&
				!ToolboxSysKeys.UserAuth.ANONYMOUS_USERNAME.equals(user.getUsername())) {

			// findUserWithoutTenantId-rel nem lehet a system user-t betölteni DB-ből
			// másrészt az amúgy sem nagyon lehet letiltott, tehát felesleges is ellenőrizni

			if (noCache) {
				user = ApplicationContextHelper.getBean(UserJdbcRepository.class).findUserWithoutTenantId(user.getId()); // refresh from DB, itt kivételesen fontos közvetlenül a repository-t használni
			}

			if (!(Boolean.TRUE.equals(((User) user).getEnabled()))) {
				throw new AccessDeniedException("setUser() was called on a disabled user!");
			}

		}

		// ---

		limitAccessDisabledTenant(user.getTenantId(), true, true, noCache);

	}

	/**
	 * super és/vagy system user-t nem blokkolja... 
	 * disabled (nem enabled) tenant-ot NEM engedi át 
	 * (tehát kicsit félrevezető a neve, a többi hasonló metódussal összevetve)
	 * 
	 * @param tenantId
	 * 
	 * @throws AccessDeniedException
	 */
	public static void limitAccessDisabledTenant(final int tenantId, final boolean allowSystem, final boolean allowSuper, final boolean noCache) {

		if (!ApplicationContextHelper.getBean(TenantJdbcRepository.class).isTenantEnabled(tenantId, noCache)) { // itt kivételesen fontos közvetlenül a repository-t használni

			if (allowSystem && SecurityUtil.isSystem()) {
				log.debug("setUser() was called on a disabled tenant, but the user is the system user (so access was not denied)");
				return;
			}

			if (allowSuper && SecurityUtil.hasSuperUserRole()) {
				log.debug("setUser() was called on a disabled tenant, but the user has SUPER_USER_ROLE (so access was not denied)");
				return;
			}

			throw new AccessDeniedException("setUser() was called on a disabled tenant's user!");

		}

	}

	/**
	 * legalább egy a megadott role-ok közül megvan-e a belépett usernek... 
	 * (akkor engedjük át)
	 * 
	 * @param roles
	 */
	public static void limitAccessHasAnyRole(final String... roles) {
		if (!SecurityUtil.hasAnyRole(roles)) {
			throw new AccessDeniedException("One or more of these roles is required: " + Arrays.toString(roles));
		}
	}

	public static void limitAccessNotAnonymous() throws AccessDeniedException {

		if (isAnonymous()) {
			throw new AccessDeniedException("At least " + ToolboxSysKeys.UserAuth.ROLE_LCU_STR + " is required!");
		}

	}

	public static void limitAccessRoleUser() throws AccessDeniedException {

		if (!(hasRole(ToolboxSysKeys.UserAuth.ROLE_USER_STR))) {
			throw new AccessDeniedException(ToolboxSysKeys.UserAuth.ROLE_USER_STR + " is required!");
		}

	}
		
	/**
	 * (limitAccessNonSystemUser = limit access to non system users) 
	 * 
	 * belépett user (értsd security context) a system user-e, 
	 * ha igen, akkor {@link AccessDeniedException} 
	 *
	 * @return
	 * 
	 * @see #setSystemUser()
	 * @see #setSystemUser(String...)
	 * @see #setSystemUserLcu()
	 * @see #isSystem()
	 * 
	 * @throws AccessDeniedException
	 */
	public static void limitAccessNonSystemUser() throws AccessDeniedException {
		
		final ToolboxUserDetails loggedInUser = getLoggedInUser();
		boolean b = loggedInUser != null && ToolboxSysKeys.UserAuth.SYSTEM_USERNAME.equals(loggedInUser.getUsername());
		
		if (b) {
			throw new AccessDeniedException("System user in not allowed here!");
		}
		
	}

	/**
	 * belépett user (értsd security context) az id-val megadott tenant-ba tartozik-e... 
	 * részletekhez lásd az azonos boolean-t visszadó metódust... 
	 * 
	 * @param tenantId
	 * @throws AccessDeniedException
	 * 
	 * @see {@link #isSameTenant(int)}
	 */
	public static void limitAccessSameTenant(final int tenantId) throws AccessDeniedException {

		if (!(isSameTenant(tenantId))) {
			throw new AccessDeniedException("Same tenant (tenantId: " + tenantId + ") is required");
		}

	}

	/**
	 * csak {@link ToolboxSysKeys.UserAuth#ROLE_SUPER_ADMIN_STR} megléte (aktuális security context usernél) esetén "enged át" (= nem dob {@link AccessDeniedException}-t)...
	 *
	 * @throws AccessDeniedException
	 * @see #hasRole (ToolboxSysKeys.UserAuth)
	 * @see #getLoggedInUser()
	 */
	public static void limitAccessSuperAdmin() throws AccessDeniedException {

		if (!(hasRole(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR))) {
			throw new AccessDeniedException(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR + " is required!");
		}

	}

	public static void limitAccessSuperTenantOverseer() throws AccessDeniedException {

		if (!(hasRole(ToolboxSysKeys.UserAuth.ROLE_SUPER_TENANT_OVERSEER_STR))) {
			throw new AccessDeniedException(ToolboxSysKeys.UserAuth.ROLE_SUPER_TENANT_OVERSEER_STR + " is required!");
		}

	}

	public static void limitAccessSuperUser() throws AccessDeniedException {

		if (!(hasRole(ToolboxSysKeys.UserAuth.ROLE_SUPER_USER_STR))) {
			throw new AccessDeniedException(ToolboxSysKeys.UserAuth.ROLE_SUPER_USER_STR + " is required!");
		}

	}

	public static void limitAccessSystem() throws AccessDeniedException {

		if (!isSystem()) {
			throw new AccessDeniedException("System user is required");
		}

	}

	/**
	 * vagy system vagy super admin kell legyen (legalább az egyik)
	 * 
	 * @throws AccessDeniedException
	 */
	public static void limitAccessSystemOrSuperAdmin() throws AccessDeniedException {

		if (!isSystem() && !hasSuperAdminRole()) {
			throw new AccessDeniedException("System user or " + ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR + " is required");
		}

	}
	
	/**
	 * vagy system vagy super user (de nem feltételenül super admin!) kell legyen (legalább az egyik)
	 * 
	 * @throws AccessDeniedException
	 */
	public static void limitAccessSystemOrSuperUser() throws AccessDeniedException {

		if (!isSystem() && !hasSuperUserRole()) {
			throw new AccessDeniedException("System user or " + ToolboxSysKeys.UserAuth.ROLE_SUPER_USER_STR + " is required");
		}

	}
	
	/**
	 * vagy system, super admin, vagy x (a param) role kell legyen (legalább az egyik)
	 * 
	 * @throws AccessDeniedException
	 */
	public static void limitAccessSystemOrSuperAdminOrRole(final String role) throws AccessDeniedException {

		if (!isSystem() && !hasSuperAdminRole() && !hasRole(role)) {
			throw new AccessDeniedException("System user or " + ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR + " or " + role+ " is required");
		}

	}

	/**
	 * csak olyan user érheti el, akinek pontosan LCU szintű joga van (jobb/erősebb sincs)
	 * 
	 * @throws AccessDeniedException
	 */
	public static void limitAccessLcuLevelUser() throws AccessDeniedException {

		if (!isLcuLevelUser()) {
			throw new AccessDeniedException("System user is required");
		}

	}

	public static void limitAccessTenantOverseer() throws AccessDeniedException {

		if (!(hasRole(ToolboxSysKeys.UserAuth.ROLE_TENANT_OVERSEER_STR))) {
			throw new AccessDeniedException(ToolboxSysKeys.UserAuth.ROLE_TENANT_OVERSEER_STR + " is required!");
		}

	}

	/**
	 * van-e legalább az egyik megadott role (ha nincs egyik sem akkor {@link AccessDeniedException})
	 * 
	 * @param roles
	 * @throws AccessDeniedException
	 * 
	 * @see #hasAnyRole(String)
	 */
	public static void limitAnyAccess(final String... roles) throws AccessDeniedException {

		// TODO: fura neve van ennek a metódusnak

		if (!(hasAnyRole(roles))) {
			throw new AccessDeniedException(Arrays.toString(roles) + " is required");
		}

	}

	/**
	 * csak egyező userId esetén "enged át" (= nem dob {@link AccessDeniedException}-t)...
	 *
	 * @param requiredUserId ezzel egyező userId (belépett) szükséges pluszban
	 * @throws AccessDeniedException
	 * @see #getLoggedInUser()
	 */
	public static void limitOwner(final int requiredUserId) throws AccessDeniedException {
		if (requiredUserId != getLoggedInUser().getId().intValue()) {
			throw new AccessDeniedException("required: userId = " + requiredUserId);
		}
	}

	@SuppressWarnings("rawtypes")
	public static <L, R> void limitRecordAccess(final Class<? extends RecordAccessCheckService> serviceClass, final L left, final R right) {
		if (!hasRecordAccess(serviceClass, left, right)) {
			throw new AccessDeniedException("Record access is denied!");
		}
	}

	@SuppressWarnings("rawtypes")
	public static void limitRecordAccessByIds(final Class<? extends RecordAccessCheckService> serviceClass, final int leftId, final int rightId) {
		if (!hasRecordAccess(serviceClass, leftId, rightId)) {
			throw new AccessDeniedException("Record access is denied!");
		}
	}

	/**
	 * a belépett user userId-ját beírja a megadott mezőbe (reflection...)
	 *
	 * @param t
	 * @param fieldName
	 * @deprecated
	 */
	@Deprecated
	public static <T> void setUserIdField(final T t, final String fieldName) {

		try {

			final ToolboxUserDetails loggedInUser = getLoggedInUser();

			if ((loggedInUser == null) || (loggedInUser.getId() == null)) {
				throw new AccessDeniedException("Missing loggedInUser (or it's id is null)!");
			}

			final int userId = loggedInUser.getId();

			final Field field = FieldUtils.getField(t.getClass(), fieldName, true);

			field.setAccessible(true);
			field.set(t, userId);

		} catch (final Exception e) {
			throw new ToolboxSpringException("setUserIdField failed!", e);
		}

	}

	public static boolean userHasRole(final String username, final String role) {
		// TODO: ez zavaros, tisztázni
		return ApplicationContextHelper.getBean(UserService.class).userHasRole(username, role);
	}

	/* -------------------------------------------------------------------------------------------------------------------------- */
	/* -------------------------------------------------------------------------------------------------------------------------- */
	/* -------------------------------------------------------------------------------------------------------------------------- */

	/**
	 * user beléptetés (username/password ellenőrzéssel), 
	 * SecurityContextHolder.createEmptyContext() nélkül
	 * 
	 * makeNewHttpSession() kell, kivéve, ha amúgy is épp új a HTTP session 
	 * (értsd előtte kézzel újítottuk meg pl. httpServletRequest.getSession(true))
	 *
	 * @param username 
	 * 		szokásos módon tenent neve (valami/fnev) vagy id-ja (11:fnev) legyen benne
	 * @param password
	 */
	public static void loginManually(final String username, final String password) {

		final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(username, password);

		final Authentication authentication = ApplicationContextHelper.getApplicationContext().getBean(AuthenticationManager.class).authenticate(usernamePasswordAuthenticationToken);
		setAuthentication(authentication, false);

		if (!handleTwoFactorLogin((ToolboxUserDetails) authentication.getPrincipal(), null)) { // ezen csak akkor jut át, ha a usernek nincs bekapcsolva a two factor
			SecurityUtil.clearAuthentication();
			throw new InsufficientAuthenticationException("Two factor auth. fail!");
		}

	}

	public static HttpSession makeNewHttpSession() {

		final ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();

		final HttpSession session = attr.getRequest().getSession(false);
		if (session != null) {
			session.invalidate();
		}

		return attr.getRequest().getSession(true); // true == allow create
	}

	/* -------------------------------------------------------------------------------------------------------------------------- */

	/**
	 * SecurityContextHolder aktuális tartalma alapján... 
	 * (lehet, hogy csak az átmentileg berakott user éppen és nem az, aki rendesen be van lépve!)
	 * 
	 * @return
	 */
	public static ToolboxUserDetails getLoggedInUser() {

		final Authentication auth = getAuthentication();

		if ((auth != null) && auth.isAuthenticated() && (auth.getPrincipal() != null)) {

			final Object principal = auth.getPrincipal();

			if (principal instanceof ToolboxUserDetails) {

				final ToolboxUserDetails loggedInUser = (ToolboxUserDetails) principal;

				ToolboxAssert.notNull(loggedInUser.getId());

				return loggedInUser;

			}

		}

		return null;
	}

	/**
	 * {@link SecurityContextHolder#createEmptyContext()}-tel
	 */
	public static void setAnonymous() {

		final ToolboxUserDetails anonymousUser = ApplicationContextHelper.getApplicationContext().getBean(ToolboxUserDetailsService.class).loadUserByUsername(ToolboxSysKeys.UserAuth.COMMON_TENANT_ID + ":" + ToolboxSysKeys.UserAuth.ANONYMOUS_USERNAME);

		if (!anonymousUser.isEnabled()) {
			throw new ToolboxSpringException("Anonymous user is not enabled (setAnonymous failed)!");
		}

		setAuthentication(new AnonymousAuthenticationToken(UUID.randomUUID().toString(), anonymousUser,
				Lists.newArrayList(new SimpleGrantedAuthority(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR))));
	}

	/**
	 * {@link SecurityContextHolder#createEmptyContext()}-tel
	 * 
	 * @see ToolboxSysKeys.UserAuth#LCU_TENANT_ID
	 * @see ToolboxSysKeys.UserAuth#LCU_SYSTEM_USERNAME
	 */
	public static void setSystemUserLcu() {
		setSystemUserLcu(ToolboxSysKeys.UserAuth.ROLE_LCU_STR, ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR);
	}

	/**
	 * {@link SecurityContextHolder#createEmptyContext()}-tel
	 * 
	 * @see ToolboxSysKeys.UserAuth#LCU_TENANT_ID
	 * @see ToolboxSysKeys.UserAuth#LCU_SYSTEM_USERNAME
	 */
	public static void setSystemUserLcu(final String... roles) {
		setSystemUserInner(Sets.newHashSet(roles), ToolboxSysKeys.UserAuth.LCU_TENANT_ID + ":" + ToolboxSysKeys.UserAuth.LCU_SYSTEM_USERNAME);
	}

	/**
	 * {@link SecurityContextHolder#createEmptyContext()}-tel
	 * 
	 * @see ToolboxSysKeys.UserAuth#LCU_TENANT_ID
	 * @see ToolboxSysKeys.UserAuth#LCU_SYSTEM_USERNAME
	 */
	public static void setSystemUserLcu(final Set<String> roles) {
		setSystemUserInner(roles, ToolboxSysKeys.UserAuth.LCU_TENANT_ID + ":" + ToolboxSysKeys.UserAuth.LCU_SYSTEM_USERNAME);
	}

	/**
	 * {@link SecurityContextHolder#createEmptyContext()}-tel
	 * 
	 * @see ToolboxSysKeys.UserAuth#COMMON_TENANT_ID
	 * @see ToolboxSysKeys.UserAuth#SYSTEM_USERNAME
	 */
	public static void setSystemUser() {
		setSystemUser(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR, ToolboxSysKeys.UserAuth.ROLE_USER_STR, ToolboxSysKeys.UserAuth.ROLE_LCU_STR, ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR);
	}

	/**
	 * {@link SecurityContextHolder#createEmptyContext()}-tel
	 * 
	 * @param roles
	 * 
	 * @see ToolboxSysKeys.UserAuth#COMMON_TENANT_ID
	 * @see ToolboxSysKeys.UserAuth#SYSTEM_USERNAME
	 */
	public static void setSystemUser(final String... roles) {
		setSystemUserInner(Sets.newHashSet(roles), ToolboxSysKeys.UserAuth.COMMON_TENANT_ID + ":" + ToolboxSysKeys.UserAuth.SYSTEM_USERNAME);
	}

	/**
	 * {@link SecurityContextHolder#createEmptyContext()}-tel
	 * 
	 * @param roles
	 * 
	 * @see ToolboxSysKeys.UserAuth#COMMON_TENANT_ID
	 * @see ToolboxSysKeys.UserAuth#SYSTEM_USERNAME
	 */
	public static void setSystemUser(final Set<String> roles) {
		setSystemUserInner(roles, ToolboxSysKeys.UserAuth.COMMON_TENANT_ID + ":" + ToolboxSysKeys.UserAuth.SYSTEM_USERNAME);
	}

	/**
	 * {@link SecurityContextHolder#createEmptyContext()}-tel
	 * 
	 * @param roles
	 * @param username
	 * 
	 * @see ToolboxSysKeys.UserAuth#COMMON_TENANT_ID
	 * @see ToolboxSysKeys.UserAuth#SYSTEM_USERNAME
	 * @see ToolboxSysKeys.UserAuth#LCU_TENANT_ID
	 * @see ToolboxSysKeys.UserAuth#LCU_SYSTEM_USERNAME
	 * 
	 * @see #setAuthentication(Authentication)
	 */
	private static void setSystemUserInner(final Set<String> roles, final String username) {

		final User systemUser = (User) ApplicationContextHelper.getApplicationContext().getBean(ToolboxUserDetailsService.class).loadUserByUsername(username);

		if (!systemUser.isEnabled()) {
			throw new ToolboxSpringException("User (system) is not enabled (setSystemUser failed): " + username);
		}

		final CodeStoreItemJdbcRepository codeStoreItemJdbcRepository = ApplicationContextHelper.getBean(CodeStoreItemJdbcRepository.class);

		final Set<SimpleGrantedAuthority> simpleGrantedAuthorities = new HashSet<>();
		final JSONArray ja = new JSONArray();

		for (final String role : roles) {
			simpleGrantedAuthorities.add(new SimpleGrantedAuthority(role));

			final CodeStoreItem codeStoreItem = codeStoreItemJdbcRepository.findOneBy("command", role);
			ja.put(codeStoreItem.getId());

		}

		systemUser.setUserRoles(ja.toString());
		setAuthentication(new UsernamePasswordAuthenticationToken(systemUser, null, simpleGrantedAuthorities));

	}

	/**
	 * {@link SecurityContextHolder#createEmptyContext()}-tel
	 * 
	 * @param user
	 */
	public static void setUser(final ToolboxUserDetails user) {

		limitAccessDisabled(user);

		final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
		setAuthentication(usernamePasswordAuthenticationToken);

	}

	/* -------------------------------------------------------------------------------------------------------------------------- */

	private static Authentication getAuthentication() {

		final SecurityContext securityContext = SecurityContextHolder.getContext();

		if (securityContext != null) {
			return securityContext.getAuthentication();
		}

		return null;
	}

	private static void setAuthentication(final Authentication a) {
		setAuthentication(a, true);
	}

	private static void setAuthentication(final Authentication a, final boolean createEmptyContext) {
		if (createEmptyContext) {
			
			// itt  kell az esetek 99%-ban setContext/createEmptyContext is!
						
			// a kódban gyakran csinálunk setUser(), setSystemUser() stb. hívásokat 
			// annak érdekében, hogy ritkán 1-1 helyen átmentileg magasabb jogú művelet is végrehajtható legyen
			
			// SecurityContextHolder ThreadLocal alapú, ezzel magában nincs gond, 
			// a gond az, hogy a SecurityContext objektum, ami benne van az közös...
			// egy HTTP session-höz csak egy tartozik!
			
			// az egész akkor jön elő, ha 
			// a user mondjuk több fülön böngészik 
			// az egyik fülön csinál egy olyan müveletet (megnyom egy gombot), ami hosszasan  
			// csinál valamilt emelt privélégiummal (pl. setSystemUser()) 
			// pl. egy report generálás (szimulálható debug ponttal)
			
			// ekkor ha egy másik böngésző fülön (ugyanabban a böngészőben, tehát azonos http session) 
		    // párhuzamosan kattintgat, akkor ott olyasmi láthat/csinálhat, amit nem szabadna neki
			// rálát pl. 1-es tenant-os dolgokra
			
			// itt a createEmptyContext() meggátolja ezt
			
			// TODO:
			// most már van egy ilyen is Spring-ben, amivel valószínűleg tiszátábban lehetne intézni az ilyesmit:
			// https://www.baeldung.com/spring-security-run-as-auth
			
			SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
		}
		SecurityContextHolder.getContext().setAuthentication(a);
	}

	/**
	 * null-ra állítja... SecurityContextHolder.createEmptyContext();-tel
	 */
	public static void clearAuthentication() {
		setAuthentication(null);
	}

	/* -------------------------------------------------------------------------------------------------------------------------- */
	/* -------------------------------------------------------------------------------------------------------------------------- */
	/* -------------------------------------------------------------------------------------------------------------------------- */

	private SecurityUtil() {
		//
	}

}