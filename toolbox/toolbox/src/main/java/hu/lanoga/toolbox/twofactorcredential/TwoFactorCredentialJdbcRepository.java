package hu.lanoga.toolbox.twofactorcredential;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.user.UserPassword;
import hu.lanoga.toolbox.user.UserPasswordJdbcRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TwoFactorCredentialJdbcRepository extends DefaultJdbcRepository<TwoFactorCredential> implements com.warrenstrange.googleauth.ICredentialRepository {

	@Autowired
	private UserPasswordJdbcRepository userPasswordJdbcRepository;

	@Override
	public String getSecretKey(String username) {
		return getICredential(username).getSecretCode();
	}

	public TwoFactorCredential getICredential(String username) {
		return this.findOneBy("username", username);
	}

	@Override
	public void saveUserCredentials(String username, String secretCode, int validationCode, List<Integer> scratchCodes) {

		StringBuilder scratchCodesArray = new StringBuilder();

		scratchCodesArray.append("{");

		for (Integer scratchCode : scratchCodes) {
			scratchCodesArray.append(scratchCode);
			scratchCodesArray.append(",");
		}

		scratchCodesArray = new StringBuilder(scratchCodesArray.toString().substring(0, scratchCodesArray.toString().length() - 1));
		scratchCodesArray.append("}");

		final TwoFactorCredential registerTwoFactorCredential = new TwoFactorCredential();
		registerTwoFactorCredential.setUsername(username);
		registerTwoFactorCredential.setSecretCode(secretCode);
		registerTwoFactorCredential.setValidationCode(validationCode);
		registerTwoFactorCredential.setScratchCodes(scratchCodesArray.toString());

		this.save(registerTwoFactorCredential);

		final UserPassword userPassword = userPasswordJdbcRepository.findUserPassword(SecurityUtil.getLoggedInUser().getTenantId(), SecurityUtil.getLoggedInUser().getId());
		userPassword.setTwoFactorType(ToolboxSysKeys.TwoFactorAuthentication.GOOGLE);
		userPassword.setTwoFactorEnabled(true);

		userPasswordJdbcRepository.save(userPassword);

	}

	public void removeTwoFactorAuth(String username) {

		final TwoFactorCredential twoFactorCredential = this.getICredential(username);

		this.delete(twoFactorCredential);

		final UserPassword userPassword = userPasswordJdbcRepository.findUserPassword(SecurityUtil.getLoggedInUser().getTenantId(), SecurityUtil.getLoggedInUser().getId());
		userPassword.setTwoFactorType(null);
		userPassword.setTwoFactorEnabled(false);

		userPasswordJdbcRepository.save(userPassword);

	}

}
