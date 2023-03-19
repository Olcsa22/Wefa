package hu.lanoga.toolbox.user;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import hu.lanoga.toolbox.auth.token.AuthTokenService;
import hu.lanoga.toolbox.file.FileStoreController;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.util.ToolboxAssert;

/**
 * egodoc projekt kapcsán készült, a desktop kis app fájl letöltés/feltöltés... 
 * in-memory... 
 * 
 * a story kb. ez: 
 * 1) kap egy spec. token-es URL-t a user 
 * (valahol, egodoc-ban a desktop app generál egyet, de lehet email stb.) 
 * (fontos, hogy secure random szintű legyen)
 * 2) ezzel megnyitja az oldalt, a rendes login oldalon belép (sikeresen)
 * 3) mi itt megjegyezzük, hogy ez a token melyik userhez tartozik
 * 4) ennek révén egyes szituációkban tudjuk, hogy a második kliens 
 * (pl. a desktop kiegészítő app) is ehhez a userhez tartozik
 * 
 * @see AuthTokenService
 * 		hasonló, de különálló dolog (inkább elfelejtett jelszó stb. célra van, DB-be ment)
 */
@Component
public class PublicTokenHolder {
	
	// TODO: oneTimeTokenHolder le lett cserélve CCHM-ről Guava Cache-re, azóta nem volt tesztelve
	
	@Value("${tools.security.public-token-holder.enabled}")
	boolean publicTokenHolderEnabled;

	@Autowired
	private UserJdbcRepository userJdbcRepository;

	/**
	 * experimental... 
	 * tenantId:userId -> token
	 * 
	 * @see FileStoreController
	 */
	private static final Cache<String, String> oneTimeTokenHolder = CacheBuilder.newBuilder().concurrencyLevel(4).expireAfterAccess(7, TimeUnit.DAYS).build();
	
	/**
	 * login után, amikor már tudjuk, hogy a user valid/belépett => a token is valid
	 * 
	 * @param request
	 */
	public void storeToken(final String tokenValue) {
		
		ToolboxAssert.isTrue(this.publicTokenHolderEnabled);

		final ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();

		oneTimeTokenHolder.put(loggedInUser.getTenantId() + ":" + loggedInUser.getId(), tokenValue);

	}
	
	public void removeToken() {
		
		try {
			final ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();
			oneTimeTokenHolder.invalidate(loggedInUser.getTenantId() + ":" + loggedInUser.getId());
		} catch (final Exception e) {
			//
		}
	
	}

	/**
	 * @param request
	 * 
	 * @see SecurityUtil#setUser(ToolboxUserDetails)
	 */
	public void setUserBasedOnToken(final HttpServletRequest request) {
		
		ToolboxAssert.isTrue(this.publicTokenHolderEnabled);

		final String tokenValue = request.getParameter("pt");

		if (StringUtils.isBlank(tokenValue)) {
			throw new BadCredentialsException("Bad token!");
		}

		final Optional<String> o = oneTimeTokenHolder.asMap().entrySet().stream().filter(entry -> tokenValue.equals(entry.getValue())).map(Map.Entry::getKey).findAny();
		if (o.isPresent()) {
			final String[] split = o.get().split(":");
			final ToolboxUserDetails userFromToken = this.userJdbcRepository.findUser(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
			SecurityUtil.setUser(userFromToken);
		} else {
			throw new BadCredentialsException("Bad token!");
		}

	}

	public String getTokenForLoggedInUser() {
		
		ToolboxAssert.isTrue(this.publicTokenHolderEnabled);
		
		final ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();
		return oneTimeTokenHolder.getIfPresent(loggedInUser.getTenantId() + ":" + loggedInUser.getId());
	}

}
