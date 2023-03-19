package hu.lanoga.toolbox.tenant;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.service.AdminOnlyCrudService;
import hu.lanoga.toolbox.spring.SecurityUtil;

@ConditionalOnMissingBean(name = "tenantKeyValueSettingsServiceOverrideBean")
@Service
public class TenantKeyValueSettingsService extends AdminOnlyCrudService<TenantKeyValueSettings, TenantKeyValueSettingsJdbcRepository> {

	public static boolean checkIfSettingsAreValid(TenantKeyValueSettings... tenantKeyValueSettings) {
		boolean b = true;
		for (TenantKeyValueSettings tenantKeyValueSetting : tenantKeyValueSettings) {
			b = b & checkIfSettingIsValid(tenantKeyValueSetting);
			if (!b) {
				break;
			}
		}
		return b;
	}
	
	/**
	 * @param tenantKeyValueSetting
	 * @return
	 * 		ha nem létezik a tenantKeyValueSetting rekord (null a paramként beadott objektum) 
	 * 		vagy nincs értéke (a kvVakue null, üres string, blank...) akkor FALSE
	 */
	public static boolean checkIfSettingIsValid(final TenantKeyValueSettings tenantKeyValueSetting) {
		if (tenantKeyValueSetting == null || StringUtils.isBlank(tenantKeyValueSetting.getKvValue())) {
			return false;
		}
	
		return true;
	}
	
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public String findValue(final String kvKey, final String defaultValue) {

		final TenantKeyValueSettings tenantKeyValueSettings = this.repository.findOneBy("kvKey", kvKey);

		if (tenantKeyValueSettings != null && tenantKeyValueSettings.getKvValue() != null) {
			return tenantKeyValueSettings.getKvValue();
		}

		return defaultValue;

	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public TenantKeyValueSettings findOneByKey(final String key) {
		return this.repository.findOneBy("kvKey", key);
	}

	@Override
	public TenantKeyValueSettings save(TenantKeyValueSettings tenantKeyValueSettings) {

		if (SecurityUtil.hasSuperAdminRole()) {
			try {

				JdbcRepositoryManager.setTlTenantId(tenantKeyValueSettings.getTenantId());

				return super.save(tenantKeyValueSettings);

			} finally {
				JdbcRepositoryManager.clearTlTenantId();
			}
		} else {
			return super.save(tenantKeyValueSettings);
		}

	}

}