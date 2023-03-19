package hu.lanoga.toolbox.auth.token;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import hu.lanoga.toolbox.tenant.Tenant;
import hu.lanoga.toolbox.tenant.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.email.EmailTemplateService;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.user.PublicTokenHolder;
import hu.lanoga.toolbox.user.User;
import hu.lanoga.toolbox.user.UserJdbcRepository;
import hu.lanoga.toolbox.util.BrandUtil;
import hu.lanoga.toolbox.vaadin.ForgottenPasswordUI;

/**
 * @see ForgottenPasswordUI
 * @see PublicTokenHolder
 * 		hasonló, de különálló dolog (csak in-memory)
 */
@ConditionalOnMissingBean(name = "authTokenServiceOverrideBean")
@Service
public class AuthTokenService {

	/**
	 * másodpercben
	 */
	@Value("${tools.security.auth.token-ttl}")
	private int defaultTokenTtlValue;
	
	@Autowired
	private EmailTemplateService emailTemplateService;

	@Autowired
	private AuthTokenJdbcRepository authTokenJdbcRepository;

	@Autowired
	private UserJdbcRepository userJdbcRepository;

	@Autowired
	private TenantService tenantService;

	/**
	 * {@link AuthToken} létrehozása userId, böngésző (ez a note mezőbe kerül), token típusa és TTL alapján. A TTL lehet null, ekkor az
	 * application.properties-ben megadott alapértékkel készül a token.
	 *
	 * @param userId
	 * @param browser
	 * @param tokenType
	 * @param timeToLive
	 * @return
	 */
	public AuthToken createAuthToken(final int userId, final String browser, final int tokenType, final Integer timeToLive) {

		SecurityUtil.limitAccessSystem();

		final AuthToken at = new AuthToken();
		at.setResourceId1(userId); // melyik user kéri
		at.setToken(UUID.randomUUID().toString());
		at.setNote1(browser);
		at.setTokenType(tokenType);

		if (timeToLive != null) {
			at.setValidUntil(new Timestamp(System.currentTimeMillis() + (1000 * timeToLive.intValue())));
		} else {
			at.setValidUntil(new Timestamp(System.currentTimeMillis() + (1000 * defaultTokenTtlValue)));
		}

		return authTokenJdbcRepository.save(at);
	}

	public void sendForgottenPasswordEmail(String appname, User user, AuthToken authToken) {

		SecurityUtil.limitAccessSystem();

		StringBuilder sbBody = new StringBuilder();

		sbBody.append("<a href=\"");
		sbBody.append(BrandUtil.getRedirectUriHostFrontend());
		sbBody.append("public/reset-password?token=");
		sbBody.append(authToken.getToken());
		sbBody.append("\">");
		sbBody.append(" link</a>");

		Map<String, Object> values = new HashMap<>();
		values.put("recipient", user.getUsername());
		values.put("data", sbBody.toString());
		values.put("appname", appname);

		Tenant tenant = tenantService.findOne(user.getTenantId());
		values.put("tenant", tenant == null ? "" : tenant.getName());

		emailTemplateService.addMail(SecurityUtil.getLoggedInUser(), user.getEmail(), ToolboxSysKeys.EmailTemplateType.FORGOTTEN_PASSWORD, values, "hu");
	}

	public List<User> findUsersByEmail(String email) {

		SecurityUtil.limitAccessSystem();

		return userJdbcRepository.findUsersByEmail(email);
	}

	/**
	 * token string alapján megkeresi az {@link AuthToken} rekordot/objektumot, 
	 * valamint ellenőrzi, hogy a megfelelő típusú-e,
	 * lejárt-e ellenőrzés itt nincs még
	 *
	 * @param token
	 * @param expectedTokenType
	 * @return
	 * @see ToolboxSysKeys.AuthTokenType
	 */
	public AuthToken findAuthTokenByToken(final String token, final int expectedTokenType) {

		SecurityUtil.limitAccessSystem();

		final AuthToken at = authTokenJdbcRepository.findByToken(token);

		if (at == null) {
			throw new AccessDeniedException("No token found. Access denied.");
		}

		if (!at.getTokenType().equals(expectedTokenType)) {
			throw new AccessDeniedException("Token type mismatch. Found token's type (" + at.getTokenType() + ") does not match with expected token type (" + expectedTokenType + ").");
		}

		// if (at.getValidUntil() == null || !at.getValidUntil().after(new Timestamp(System.currentTimeMillis()))) {
		// throw new AccessDeniedException("Token expired. Access denied.");
		// }

		return at;

	}

	public void invalidateToken(AuthToken authToken) {

		SecurityUtil.limitAccessSystem();

		authToken.setValidUntil(new Timestamp(0));
		authTokenJdbcRepository.save(authToken);
	}
}
