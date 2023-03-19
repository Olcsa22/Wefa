package hu.lanoga.toolbox.user.sso;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.lanoga.toolbox.user.UserInfoJdbcRepository;
import hu.lanoga.toolbox.user.UserJdbcRepository;
import hu.lanoga.toolbox.user.UserKeyValueSettingsJdbcRepository;

/**
 * @see UserS
 */
@SuppressWarnings("all")
@ConditionalOnMissingBean(name = "userSsoServiceOverrideBean")
@Service
public class UserSsoService {

	@Autowired
	private UserJdbcRepository userJdbcRepository;

	@Autowired
	private UserInfoJdbcRepository userInfoJdbcRepository;

	@Autowired
	private UserKeyValueSettingsJdbcRepository userKeyValueSettingsJdbcRepository;

	@Autowired
	private UserSsoJdbcRepository userSsoJdbcRepository;

	@Transactional
	public Object extractPrincipal(final Map<String, Object> sourceMap) {

		throw new UnsupportedOperationException();
		
		// TODO: tisztázni kell, már nincs LCU tenant, máshogy lett megoldva

		// try {
		//
		// JdbcRepositoryManager.setTlTenantId(ToolboxSysKeys.UserAuth.LCU_TENANT_ID);
		//
		// final String ssoId = sourceMap.get("id").toString();
		// final String ssoType = "Facebook";
		//
		// UserSso userSso = userSsoJdbcRepository.findOneBy("ssoType", ssoType, "ssoId", ssoId);
		// User user = null;
		//
		// if (userSso == null) {
		//
		// try {
		//
		// final Object fullName = sourceMap.get("name");
		// final String fullNameString = fullName.toString();
		// final String[] nameArray = fullNameString.split(" ");
		//
		// user = new User();
		// user.setUsername(UUID.randomUUID().toString());
		// user.setUserRoles("[" + ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_CS_ID + ", " + ToolboxSysKeys.UserAuth.ROLE_LCU_CS_ID + "]");
		// user.setEnabled(true);
		// user.setAnonymizedDeleted(false);
		// user = userJdbcRepository.save(user);
		//
		// final UserInfo userInfo = new UserInfo();
		// userInfo.setUserId(user.getId());
		// userInfo.setGivenName(nameArray[0]);
		// userInfo.setFamilyName(nameArray[1]);
		// userInfo.setEmail("example@example.com");
		// userInfoJdbcRepository.save(userInfo);
		//
		// userSso = new UserSso();
		// userSso.setUserId(user.getId());
		// userSso.setSsoId(ssoId);
		// userSso.setSsoType(ssoType);
		// userSsoJdbcRepository.save(userSso);
		//
		// } catch (final Exception e) {
		// log.error("SSO error!", e);
		// throw new ToolboxGeneralException("SSO error!", e);
		// }
		//
		// } else {
		// user = userJdbcRepository.findOne(userSso.getUserId());
		// }
		//
		// // ---
		//
		// SecurityUtil.setUser(user);
		//
		// // ---
		//
		// return user;
		//
		// } finally {
		// JdbcRepositoryManager.clearTlTenantId();
		// }

	}

}
