package hu.lanoga.toolbox.user;

import lombok.Getter;
import lombok.Setter;

/**
 * belépett user módosítja saját jelszavát...
 * 
 * @see UserService#savePasswordForLoggedInUser(SavePassword1)
 */
@Getter
@Setter
public class SavePassword1 {
	
	private String oldPassword;
	private String newPassword;
	private String newPasswordAgain;
	
}
