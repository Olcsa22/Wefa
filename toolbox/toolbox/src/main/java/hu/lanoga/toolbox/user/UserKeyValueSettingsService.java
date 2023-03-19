package hu.lanoga.toolbox.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.JdbcInsertConflictMode;
import hu.lanoga.toolbox.service.AdminOnlyCrudService;
import hu.lanoga.toolbox.spring.SecurityUtil;

@ConditionalOnMissingBean(name = "userKeyValueSettingsServiceOverrideBean")
@Service
public class UserKeyValueSettingsService extends AdminOnlyCrudService<UserKeyValueSettings, UserKeyValueSettingsJdbcRepository> {

	@Override
	@Transactional
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public UserKeyValueSettings save(UserKeyValueSettings userKeyValueSettings) {

		SecurityUtil.limitAccessAdminOrOwner(userKeyValueSettings.getUserId());

		// ON_CONFLICT_DO_UPDATE azért jó itt, mert így több helyeről egyszerre is lehet szerkeszteni (pl.: egy admin és maga a user is írja/visz fel újat egy időben)

		Integer t = (Integer) repository.insert(userKeyValueSettings, null, JdbcInsertConflictMode.ON_CONFLICT_DO_UPDATE, "user_id, lower(kv_key::text)", null);
		return repository.findOne(t);
	}

	@Transactional
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public void saveAll(UserKeyValueSettings... userKeyValueSettingItems) {

		for (UserKeyValueSettings userKeyValueSettings : userKeyValueSettingItems) {
			this.save(userKeyValueSettings);
		}

	}

	/**
	 * 
	 * @param userId
	 * @param key
	 * @return
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public UserKeyValueSettings findOneByKey(final int userId, final String key) {

		SecurityUtil.limitAccessAdminOrOwner(userId);

		return this.repository.findOneBy("userId", userId, "kvKey", key);
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public String findValue(final int userId, final String key, final String defaultValue) {

		SecurityUtil.limitAccessAdminOrOwner(userId);

		final UserKeyValueSettings userKeyValueSettings = this.repository.findOneBy("userId", userId, "kvKey", key);

		if (userKeyValueSettings != null && userKeyValueSettings.getKvValue() != null) {
			return userKeyValueSettings.getKvValue();
		}

		return defaultValue;

	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public List<UserKeyValueSettings> findAllByUserId(final int userId) {

		SecurityUtil.limitAccessAdminOrOwner(userId);
		return this.repository.findAllBy("userId", userId);
		
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public Map<String, String> findMap(final int userId) {

		SecurityUtil.limitAccessAdminOrOwner(userId);

		final List<UserKeyValueSettings> list = this.repository.findAllBy("userId", userId);

		final Map<String, String> map = new HashMap<>();

		for (final UserKeyValueSettings ukvs : list) {
			map.put(ukvs.getKvKey(), ukvs.getKvValue());
		}

		return map;

	}

}