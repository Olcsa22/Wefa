package hu.lanoga.toolbox.util;

import org.apache.commons.lang3.StringUtils;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.tenant.TenantKeyValueSettingsService;
import hu.lanoga.toolbox.user.UserKeyValueSettingsService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThemeUtil {

	private ThemeUtil() {
		//
	}

	/**
	 * {@link TenantKeyValueSettingsService} alapján 
	 * (tools.preferences.tenant-fallback-..., ha ez is ures, akkor null) 
	 * (belépett user tenantja)
	 * 
	 * @return
	 */
	public static String getTenantTheme() {

		String themeStr = null;

		try {

			if (SecurityUtil.hasLoggedInUser()) {
				final String str = ApplicationContextHelper.getBean(TenantKeyValueSettingsService.class).findValue(ToolboxSysKeys.TenantKeyValueSettings.PREFERRED_THEME, null);
				themeStr = StringUtils.isNotBlank(str) ? str : null;
			}

		} catch (final Exception e) {
			log.warn("getTenantTheme error 1 (will use fallback)!", e);
		}

		try {

			if (themeStr == null) {
				final String str = ApplicationContextHelper.getConfigProperty("tools.preferences.tenant-fallback-theme");
				themeStr = StringUtils.isNotBlank(str) ? str : null;
			}

		} catch (final Exception e) {
			log.warn("getTenantTheme error 2 (will use fallback)!", e);
		}

		return themeStr;

	}

	/**
	 * {@link UserKeyValueSettingsService} alapján 
	 * ({@link #getTenantTheme()} a fallback, ha ez is ures, akkor null) 
	 * 
	 * @return
	 */
	public static String getUserTheme(final int userId) {

		String themeStr = null;

		try {

			if (SecurityUtil.hasLoggedInUser()) {
				final String str = ApplicationContextHelper.getBean(UserKeyValueSettingsService.class).findValue(userId, ToolboxSysKeys.UserKeyValueSettings.PREFERRED_THEME, null);
				themeStr = StringUtils.isNotBlank(str) ? str : null;
			}

		} catch (final Exception e) {
			log.warn("getUserTheme error (will use fallback)!", e);
		}

		if (themeStr == null) {
			themeStr = getTenantTheme();
		}

		return themeStr;

	}

	/**
	 * (lehet null)
	 * 
	 * @return
	 * 
	 * see #getUserTeme(int)
	 */
	public static String getUserTheme(final ToolboxUserDetails user) {
		return getUserTheme(user.getId());
	}

	/**
	 * (lehet null)
	 * 
	 * @return
	 * 
	 *  @see #getUserTeme(int)
	 */
	public static String getLoggedInUserTheme() {
		return getUserTheme(SecurityUtil.getLoggedInUser());
	}

}
