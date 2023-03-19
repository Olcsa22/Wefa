package hu.lanoga.toolbox.user;

import java.util.Date;

import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;

/**
 * Spring persistent_login ("maradjak bejelentkezve" funkció)...
 */
public class ToolboxPersistentRememberMeToken extends PersistentRememberMeToken {

	public ToolboxPersistentRememberMeToken() {
		super(null, null, null, null);
	}
	
	public ToolboxPersistentRememberMeToken(String username, String series, String tokenValue, Date date) {
		super(username, series, tokenValue, date);
	}

}
