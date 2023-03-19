package hu.lanoga.toolbox.user;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.teamunify.i18n.I;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.controller.DefaultCrudController;
import hu.lanoga.toolbox.controller.UtilController;
import hu.lanoga.toolbox.exception.ManualValidationException;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;

@RequestMapping(value = "api/users")
@ConditionalOnMissingBean(name = "userControllerOverrideBean")
@ConditionalOnProperty(name="tools.user.controller.enabled", matchIfMissing = true)
@RestController
public class UserController extends DefaultCrudController<User, UserService> {

	@Autowired
	private UtilController utilController;

	@Autowired
	private PasswordEncoder passwordEncoder;
	
	/**
	 * belépett user...
	 * (a {@link UtilController} azonos nevű metódusa ugyanezt adja vissza)
	 * 
	 * @return
	 * 
	 * @see UtilController#getLoggedInUser()
	 * @see UtilController#getLoggedInUserFullName()
	 */
	@RequestMapping(value = "/cu", method = RequestMethod.GET)
	public ToolboxUserDetails getLoggedInUser() {
		return this.utilController.getLoggedInUser();
	}

	@RequestMapping(value = "/password", method = RequestMethod.POST)
	public User savePasswordForLoggedInUser(@RequestBody final SavePassword1 savePassword) {

		if (this.passwordEncoder.matches(savePassword.getOldPassword(), SecurityUtil.getLoggedInUser().getPassword())) {

			if (savePassword.getNewPassword().equals(savePassword.getNewPasswordAgain())) {

				if (SecurityUtil.checkPasswordStrengthSimple(savePassword.getNewPassword())) {
					return this.service.savePasswordForLoggedInUser(savePassword);
				}

				throw new ManualValidationException("Password is too simple (min length: 8 character, necessary: lowercase, uppercase, number)!", I.trc("Error", "Password is too simple (min length: 8 character, necessary: lowercase, uppercase, number)!"));
			}
			
			throw new ManualValidationException("New password values are not equal!", I.trc("Error", "New password values are not equal!"));
		}
		
		throw new ManualValidationException("Invalid old password!", I.trc("Error", "Invalid old password!"));
	}
	
	@RequestMapping(value = "/upload-profile-img", method = RequestMethod.POST)
	public Integer saveProfileImgForLoggedInUser(final MultipartHttpServletRequest request) {
		
		final List<FileDescriptor> tmpFiles = FileStoreHelper.saveAsTmpFiles(request, 
				ToolboxSysKeys.FileDescriptorLocationType.PROTECTED_FOLDER, 
				ToolboxSysKeys.FileDescriptorSecurityType.AUTHENTICATED_USER);
		
		return this.service.saveProfileImgForLoggedInUser(tmpFiles.get(0)).getId();
		
	}

}
