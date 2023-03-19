package hu.lanoga.toolbox.controller;

import java.util.Currency;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableMap;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.user.UserController;

@ConditionalOnMissingBean(name = "utilControllerOverrideBean")
@ConditionalOnProperty(name = "tools.util.controller.enabled", matchIfMissing = true)
@RestController
public class UtilController {

	@RequestMapping(value = "api/public/action/determine-ip-address", method = RequestMethod.GET)
	public Map<String, String> determineIpAddress(final HttpServletRequest request) {
		return ImmutableMap.of("ipAddress", ToolboxHttpUtils.determineIpAddress(request)); // TODO: kell ez még?
	}

	@RequestMapping(value = "api/public/currencies", method = RequestMethod.GET)
	public Set<Currency> getCurrencies() {
		return java.util.Currency.getAvailableCurrencies(); // TODO: kell ez még?
	}

	/**
	 * belépett user... 
	 * (a {@link UserController} azonos nevű metódusa ugyanezt adja vissza)
	 * 
	 * @return
	 * 
	 * @see UserController#getLoggedInUser()
	 */
	@RequestMapping(value = "api/cu", method = RequestMethod.GET)
	public ToolboxUserDetails getLoggedInUser() {

		final ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();

		if (loggedInUser == null || loggedInUser.getUsername().equals(ToolboxSysKeys.UserAuth.SYSTEM_USERNAME) || loggedInUser.getUsername().equals(ToolboxSysKeys.UserAuth.ANONYMOUS_USERNAME)) {
			throw new InsufficientAuthenticationException("Missing or anonymous/system user!");
		}

		return loggedInUser;
	}

	/**
	 * belépett user... 
	 * 
	 * @return
	 */
	@RequestMapping(value = "api/cufullname", method = RequestMethod.GET)
	public String getLoggedInUserFullName() {

		final ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();

		if (loggedInUser == null || loggedInUser.getUsername().equals(ToolboxSysKeys.UserAuth.SYSTEM_USERNAME) || loggedInUser.getUsername().equals(ToolboxSysKeys.UserAuth.ANONYMOUS_USERNAME)) {
			// throw new InsufficientAuthenticationException("Not a regular user!");
			return null;
		}

		String result;

		// ha a bejelentkezett user-nek nincs vezeték vagy keresztneve, akkor a username-t jelenítjük meg
		if (StringUtils.isBlank(loggedInUser.getFamilyName()) || StringUtils.isBlank(loggedInUser.getGivenName())) {
			result = loggedInUser.getUsername();
		} else {
			result = I18nUtil.buildFullName(loggedInUser, false);
		}

		return "{\"name\": \"" + result + "\"}";

	}

}
