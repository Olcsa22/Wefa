package hu.lanoga.toolbox.i18n;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.ZoneId;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.measure.spi.SystemOfUnits;

import org.apache.commons.collections4.set.UnmodifiableNavigableSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;

import com.vaadin.server.Page;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.config.RequestFilterConfig;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.StartManager;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.tenant.TenantKeyValueSettingsService;
import hu.lanoga.toolbox.user.UserKeyValueSettingsService;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring context kell hozzá... (config property-k stb.)
 */
@Slf4j
public class I18nUtil {

	private static final NavigableSet<String> availableCurrencyStrings;

	static {

		final TreeSet<String> s = new TreeSet<>();
		final Set<Currency> availableCurrencies = java.util.Currency.getAvailableCurrencies();

		for (final Currency currency : availableCurrencies) {
			s.add(currency.getCurrencyCode());
		}

		availableCurrencyStrings = UnmodifiableNavigableSet.unmodifiableNavigableSet(s);
	}

	public static String buildFullName(final String title, final String familyName, final String givenName, final Locale locale, final boolean forceFamilyFirst) {

		String resultStr;

		if (locale.getLanguage().equalsIgnoreCase("hu")) { // TODO: van néhány más nyelv is, ahol a magyar veznév/kernév a sorrend
			resultStr = StringUtils.joinWith(" ", title, familyName, givenName).trim();
		} else if (forceFamilyFirst && StringUtils.isNotBlank(familyName)) {
			resultStr = StringUtils.joinWith(" ", familyName + ", ", title, givenName).trim();
		} else {
			resultStr = StringUtils.joinWith(" ", title, givenName, familyName).trim();
		}

		return StringUtils.normalizeSpace(resultStr);
	}

	public static String buildFullName(final String title, final String familyName, final String givenName, final String localeStr, final boolean forceFamilyFirst) {
		return buildFullName(title, familyName, givenName, localeStrToLocale(localeStr), forceFamilyFirst);
	}

	public static String buildFullName(final ToolboxUserDetails user, final boolean forceFamilyFirst) {

		if (user == null) {
			return null;
		}

		return buildFullName(user, forceFamilyFirst, true);
	}

	/**
	 * @param user
	 * @param forceFamilyFirst
	 * @param useParamUsersLocale 
	 * 		true esetén a paramként beadott user Locale-ját használja (false esetén a belépett user Locale-ját)
	 * @return
	 */
	public static String buildFullName(final ToolboxUserDetails user, final boolean forceFamilyFirst, final boolean useParamUsersLocale) {

		if (user == null) {
			return null;
		}

		return buildFullName(user.getTitle(), user.getFamilyName(), user.getGivenName(), useParamUsersLocale ? getUserLocale(user) : getLoggedInUserLocale(), forceFamilyFirst);
	}

	public static String buildFullName(final ToolboxUserDetails user, final Locale locale, final boolean forceFamilyFirst) {

		if (user == null) {
			return null;
		}

		return buildFullName(user.getTitle(), user.getFamilyName(), user.getGivenName(), locale, forceFamilyFirst);
	}

	public static String buildFullName(final ToolboxUserDetails user, final String localeStr, final boolean forceFamilyFirst) {

		if (user == null) {
			return null;
		}

		return buildFullName(user, localeStrToLocale(localeStr), forceFamilyFirst);
	}

	/**
	 * @param multiLangJsonStr
	 * @return
	 * 
	 * @see #getLoggedInUserLocale()
	 * @see #extractMsgFromMultiLang()
	 */
	public static String extractMsgFromMultiLang(final String multiLangJsonStr) {
		return extractMsgFromMultiLang(multiLangJsonStr, getLoggedInUserLocale().getLanguage(), null);
	}

	/**
	 * langCode -> (fallback) fallbackLangCode -> (fallback) bármelyik nyelv, ami elérhető a multiLangJsonStr-ben
	 * 
	 * @param multiLangJsonStr JSON...
	 * @param langCode
	 * @param fallbackLangCode
	 * @return nyelvesített szöveg, vagy null (ha semelyik fallback megoldás sem talált semmit, üres a JSON stb.)
	 * 
	 * @see #packageIntoMultiLang(Iterable)
	 */
	public static String extractMsgFromMultiLang(final String multiLangJsonStr, final String langCode, final String fallbackLangCode) {
		try {

			if (multiLangJsonStr == null) {
				return null;
			}
			
			final JSONObject jsonObject = new JSONObject(multiLangJsonStr);

			// ---

			if (StringUtils.isNotBlank(langCode)) {
				try {
					// ha a langCode a nem megfelelo formaban erkezik be (pl. hu_HU, en_GB), akkor az elso ket karaktert adja vissza
					if (langCode.length() > 2) {
						final String shortenedLangCode = langCode.substring(0, 2);
						return jsonObject.getString(shortenedLangCode);
					}

					return jsonObject.getString(langCode);
				} catch (final org.json.JSONException e) {
					// ha nincs ilyen key (langCode), akkor ilyen ex lesz
				}
			}

			if (StringUtils.isNotBlank(fallbackLangCode)) {
				try {
					return jsonObject.getString(fallbackLangCode);
				} catch (final org.json.JSONException e) {
					// ha nincs ilyen key (fallbackLangCode), akkor ilyen ex lesz
				}
			}

			final String[] keyNames = JSONObject.getNames(jsonObject);

			if (keyNames.length > 0) {
				return jsonObject.getString(keyNames[0]);
			}

			return null;

		} catch (final Exception e) {
			throw new I18nException(e);
		}
	}

	/**
	 * java.util.Currency alapján... 
	 * régi metódus, fértevezető a neve, ez a világ összes currency-je 
	 * (nem csak az, ami a projektben kell, vagy a prop. fájlban állítva van)
	 * 
	 * @return
	 */
	@Deprecated
	public static NavigableSet<String> getAvailableCurrencyStrings() {

		// TODO: átnevezni, tisztázni (most már vannak toolbox prop. fájlos felsorolások is a lehetőségekre)

		return availableCurrencyStrings;
	}

	/**
	 * (soha sem null) 
	 * 
	 * @return "USD"
	 */
	public static Currency getServerCurrency() {
		return java.util.Currency.getInstance("USD");
	}

	/**
	 * szerver nyelve
	 * (nálunk mindig en_US, mert az indulásnál direkt arra állítjuk, link {@link StartManager}) 
	 * (soha sem null)
	 * 
	 * @return
	 * 
	 * @see Locale#getDefault()
	 */
	public static Locale getServerLocale() {
		return Locale.getDefault();
	}

	/**
	 * (nálunk mindig UTC, mert az indulásnál direkt arra állítjuk, link {@link StartManager}) 
	 * (soha sem null) 
	 * 
	 * @return
	 * 
	 * @see StartManager#start(Class, String[])
	 * @see TimeZone#getDefault()
	 */
	public static TimeZone getServerTimeZone() {
		return TimeZone.getDefault();
	}

	/**
	 * (soha sem null) 
	 * 
	 * @return
	 */
	public static SystemOfUnits getServerMeasurementSystem() {
		return toSystemOfUnits("metric");
	}

	/**
	 * {@link TenantKeyValueSettingsService} alapján 
	 * (soha nem null, tools.preferences.tenant-fallback-..., illetve vegul a {@link #getServerCurrency()} az auto fallbackje) 
	 * (belépett user tenantja)
	 * 
	 * @return
	 */
	public static Currency getTenantCurrency() {

		Currency currency = null;

		try {

			final String str = ApplicationContextHelper.getBean(TenantKeyValueSettingsService.class).findValue(ToolboxSysKeys.TenantKeyValueSettings.PREFERRED_CURRENCY, null);
			currency = StringUtils.isNotBlank(str) ? java.util.Currency.getInstance(str) : null;

		} catch (final Exception e) {
			log.warn("getTenantCurrency error 1 (will use fallback)!");
		}

		try {

			if (currency == null) {
				final String str = ApplicationContextHelper.getConfigProperty("tools.preferences.tenant-fallback-currency");
				currency = StringUtils.isNotBlank(str) ? java.util.Currency.getInstance(str) : null;
			}

		} catch (final Exception e) {
			log.warn("getTenantCurrency error 2 (will use fallback)!");
		}

		if (currency == null) {
			currency = getServerCurrency();
		}

		return currency;

	}

	/**
	 * {@link TenantKeyValueSettingsService} alapján 
	 * (soha nem null, tools.preferences.tenant-fallback-..., illetve vegul a {@link #getServerLocale()} az auto fallbackje) 
	 * (belépett user tenantja)
	 * 
	 * @return
	 */
	public static Locale getTenantLocale() {

		Locale locale = null;

		if (SecurityUtil.hasLoggedInUser() && !SecurityUtil.isAnonymous()) {
			try {

				final String str = ApplicationContextHelper.getBean(TenantKeyValueSettingsService.class).findValue(ToolboxSysKeys.TenantKeyValueSettings.PREFERRED_LOCALE, null);
				locale = StringUtils.isNotBlank(str) ? localeStrToLocale(str) : null;

			} catch (final Exception e) {
				// log.warn("getTenantLocale error 1 (will use fallback)!");
			}
		}

		try {

			if (locale == null) {
				final String str = ApplicationContextHelper.getConfigProperty("tools.preferences.tenant-fallback-locale");
				locale = StringUtils.isNotBlank(str) ? localeStrToLocale(str) : null;
			}

		} catch (final Exception e) {
			// log.warn("getTenantLocale error 2 (will use fallback)!");
		}

		if (locale == null) {
			locale = getServerLocale();
		}

		return locale;

	}

	public static Locale localeStrToLocale(final String localeStr) {
		
		if (StringUtils.isBlank(localeStr)) {
			return null;
		}
		
		return Locale.forLanguageTag(localeStr.replace('_', '-'));
	}

	/**
	 * {@link TenantKeyValueSettingsService} alapján 
	 * (soha nem null, tools.preferences.tenant-fallback-..., illetve vegul a {@link #getServerTimeZone()} az auto fallbackje) 
	 * (belépett user tenantja)
	 * 
	 * @return
	 */
	public static TimeZone getTenantTimeZone() {

		TimeZone timeZone = null;

		try {

			final String str = ApplicationContextHelper.getBean(TenantKeyValueSettingsService.class).findValue(ToolboxSysKeys.TenantKeyValueSettings.PREFERRED_TIME_ZONE, null);
			timeZone = StringUtils.isNotBlank(str) ? TimeZone.getTimeZone(str) : null;

		} catch (final Exception e) {
			log.warn("getTenantTimeZone error 1 (will use fallback)!");
		}

		try {

			if (timeZone == null) {
				final String str = ApplicationContextHelper.getConfigProperty("tools.preferences.tenant-fallback-timezone");
				timeZone = StringUtils.isNotBlank(str) ? TimeZone.getTimeZone(str) : null;
			}

		} catch (final Exception e) {
			log.warn("getTenantTimeZone error 2 (will use fallback)!");
		}

		if (timeZone == null) {
			timeZone = getServerTimeZone();
		}

		return timeZone;
	}

	private static SystemOfUnits toSystemOfUnits(String measurementSystemStr) {

		SystemOfUnits systemOfUnits;

		measurementSystemStr = measurementSystemStr.trim().toLowerCase();

		if ("metric".equals(measurementSystemStr)) {

			// itt ugyanaz a kettő, csak az indriya újabb
			// mégis a másikat hsználjuk, hogy egy implmentáció libből legyen mind (ez a sima "Units" és a többi is)

			// systemOfUnits = tech.units.indriya.unit.Units.getInstance();
			systemOfUnits = tec.uom.se.unit.Units.getInstance();

		} else if ("us".equals(measurementSystemStr) || "us_custom".equals(measurementSystemStr) || "us-custom".equals(measurementSystemStr)) {
			systemOfUnits = systems.uom.common.USCustomary.getInstance();
		} else if ("imperial".equals(measurementSystemStr)) {
			systemOfUnits = systems.uom.common.Imperial.getInstance();
		} else {
			throw new I18nException("Unknown measurementSystemStr!");
		}

		return systemOfUnits;
	}

	/**
	 * {@link TenantKeyValueSettingsService} alapján 
	 * (soha nem null, tools.preferences.tenant-fallback-..., illetve vegul a {@link #getServerMeasurementSystem()} az auto fallbackje) 
	 * (belépett user tenantja)
	 * 
	 * @return
	 */
	public static SystemOfUnits getTenantMeasurementSystem() {

		String measurementSystemStr = null;

		try {

			final String str = ApplicationContextHelper.getBean(TenantKeyValueSettingsService.class).findValue(ToolboxSysKeys.TenantKeyValueSettings.PREFERRED_MEASUREMENT_SYSTEM, null);
			measurementSystemStr = StringUtils.isNotBlank(str) ? str : null;

		} catch (final Exception e) {
			log.warn("getTenantMeasurementSystem error 1 (will use fallback)!");
		}

		try {

			if (measurementSystemStr == null) {

				final String str = ApplicationContextHelper.getConfigProperty("tools.preferences.tenant-fallback-measurement-system");
				measurementSystemStr = StringUtils.isNotBlank(str) ? str : null;

			}

		} catch (final Exception e) {
			log.warn("getTenantMeasurementSystem error 2 (will use fallback)!");
		}

		SystemOfUnits systemOfUnits = null;

		try {

			if (measurementSystemStr != null) {
				systemOfUnits = toSystemOfUnits(measurementSystemStr);
			}

		} catch (final Exception e) {
			log.warn("getTenantMeasurementSystem error 3 (will use fallback)!");
		}

		if (systemOfUnits == null) {
			systemOfUnits = getServerMeasurementSystem();
		}

		return systemOfUnits;
	}

	/**
	 * {@link UserKeyValueSettingsService} alapján 
	 * (soha nem null, {@link #getTenantCurrency()} az auto fallbackje) 
	 * (csak belépett user tenantján belüli userekre)
	 * 
	 * @return
	 */
	public static Currency getUserCurrency(final int userId) {

		Currency currency = null;

		try {

			final String str = ApplicationContextHelper.getBean(UserKeyValueSettingsService.class).findValue(userId, ToolboxSysKeys.UserKeyValueSettings.PREFERRED_CURRENCY, null);
			currency = StringUtils.isNotBlank(str) ? java.util.Currency.getInstance(str) : null;

		} catch (final Exception e) {
			log.warn("getUserCurrency error (will use fallback)!");
		}

		if (currency == null) {
			currency = getTenantCurrency();
		}

		return currency;
	}

	/**
	 * @param user
	 * @return
	 * 
	 * @see #getUserCurrency(int)
	 */
	public static Currency getUserCurrency(final ToolboxUserDetails user) {
		return getUserCurrency(user.getId());
	}

	/**
	 * {@link UserKeyValueSettingsService} alapján 
	 * (soha nem null, {@link #getTenantLocale()} az auto fallbackje) 
	 * (csak belépett user tenantján belüli userekre)
	 * 
	 * @return
	 */
	public static Locale getUserLocale(final int userId) {

		Locale locale = null;

		try {

			final String str = ApplicationContextHelper.getBean(UserKeyValueSettingsService.class).findValue(userId, ToolboxSysKeys.UserKeyValueSettings.PREFERRED_LOCALE, null);

			if (StringUtils.isNotBlank(str)) {
				locale = localeStrToLocale(str);
			}

		} catch (final Exception e) {
			// log.warn("getUserLocale error (will use fallback)!");
		}

		if (locale == null) {
			locale = getTenantLocale();
		}

		return locale;
	}

	/**
	 * @param user
	 * @return
	 * 
	 * @see #getUserLocale(int)
	 */
	public static Locale getUserLocale(final ToolboxUserDetails user) {
		return getUserLocale(user.getId());
	}

	/**
	 * {@link UserKeyValueSettingsService} alapján 
	 * (soha nem null, {@link #getTenantTimeZone()} az auto fallbackje) 
	 * (csak belépett user tenantján belüli userekre)
	 * 
	 * @return
	 */
	public static TimeZone getUserTimeZone(final int userId) {

		TimeZone timeZone = null;

		try {

			final String str = ApplicationContextHelper.getBean(UserKeyValueSettingsService.class).findValue(userId, ToolboxSysKeys.UserKeyValueSettings.PREFERRED_TIME_ZONE, null);

			if (StringUtils.isNotBlank(str)) {
				timeZone = TimeZone.getTimeZone(str);
			}

		} catch (final Exception e) {
			log.warn("getUserTimeZone error (will use fallback)!");
		}

		if (timeZone == null) {
			timeZone = getTenantTimeZone();
		}

		return timeZone;
	}

	/**
	 * @param user
	 * @return
	 * 
	 * @see #getUserTimeZone(int)
	 */
	public static TimeZone getUserTimeZone(final ToolboxUserDetails user) {
		return getUserTimeZone(user.getId());
	}

	/**
	 * {@link TenantKeyValueSettingsService} alapján 
	 * (soha nem null, {@link #getServerMeasurementSystem()} az auto fallbackje) 
	 * (csak belépett user tenantján belüli userekre) 
	 * 
	 * @return
	 */
	public static SystemOfUnits getUserMeasurementSystem(final int userId) {

		String measurementSystemStr = null;

		try {

			final String str = ApplicationContextHelper.getBean(UserKeyValueSettingsService.class).findValue(userId, ToolboxSysKeys.UserKeyValueSettings.PREFERRED_MEASUREMENT_SYSTEM, null);
			measurementSystemStr = StringUtils.isNotBlank(str) ? str : null;

		} catch (final Exception e) {
			log.warn("getUserMeasurementSystem error 1 (will use fallback)!");
		}

		SystemOfUnits systemOfUnits = null;

		try {

			if (measurementSystemStr != null) {
				systemOfUnits = toSystemOfUnits(measurementSystemStr);
			}

		} catch (final Exception e) {
			log.warn("getUserMeasurementSystem error 2 (will use fallback)!");
		}

		if (systemOfUnits == null) {
			systemOfUnits = getTenantMeasurementSystem();
		}

		return systemOfUnits;

	}

	/**
	 * @param user
	 * @return
	 * 
	 * @see #getUserMeasurementSystem(int)
	 */
	public static SystemOfUnits getUserMeasurementSystem(final ToolboxUserDetails user) {
		return getUserMeasurementSystem(user.getId());
	}

	/**
	 * @return
	 * 
	 * @see #getUserCurrency(ToolboxUserDetails)
	 */
	public static Currency getLoggedInUserCurrency() {
		return getUserCurrency(SecurityUtil.getLoggedInUser());
	}

	/**
	 * komplex fallback logika... 
	 * (első a user kv setting, második a http request (ha van) locale, 
	 * harmadik a tenant nyelve (aminek pedig a szerver alap locale a fallbackje (végső)))
	 * 
	 * @return
	 * 
	 * @see UserKeyValueSettingsService
	 * @see RequestFilterConfig#getRequestLocale(ToolboxUserDetails)
	 * @see #getTenantLocale()
	 * @see UiHelper#getCurrentUiLocale()
	 */
	public static Locale getLoggedInUserLocale() {

		Locale locale = null;

		try {

			final ToolboxUserDetails user = SecurityUtil.getLoggedInUser();

			if (user != null && !SecurityUtil.isAnonymous() && !SecurityUtil.isSystem()) {

				final String str = ApplicationContextHelper.getBean(UserKeyValueSettingsService.class).findValue(user.getId(), ToolboxSysKeys.UserKeyValueSettings.PREFERRED_LOCALE, null);

				if (StringUtils.isNotBlank(str)) {
					locale = localeStrToLocale(str);
				}

			} else {
				// log.debug("getLoggedInUserLocale skip 0 (will use fallback)!"); // ez normális, pl. login képernyőn
			}

		} catch (final Exception e) {
			log.warn("getLoggedInUserLocale error 1 (will use fallback)!");
		}

		try {
			if (locale == null) {

				final Locale requestLocale = RequestFilterConfig.getRequestLocale();

				if (requestLocale != null && StringUtils.stripToEmpty(requestLocale.getLanguage()).length() == 2) {
					locale = requestLocale;
				}
			}

		} catch (final Exception e) {
			log.warn("getLoggedInUserLocale error 2 (will use fallback)!");
		}

		if (locale == null) {
			locale = getTenantLocale();
		}

		return locale;
	}

	/**
	 * komplex fallback logika... 
	 * (első a Vaadin UI (Page) nyelve (ha van), második a user kv setting, 
	 * harmadik a tenant locale (aminek pedig a szerver alap locale a fallbackje))
	 * 
	 * @return
	 * 
	 * @see #getUserTimeZone(ToolboxUserDetails)
	 */
	public static TimeZone getLoggedInUserTimeZone() {

		TimeZone timeZone = null;

		try {

			// ha Vaadin...

			if (Page.getCurrent() != null) {
				
				ZoneId zoneId;
				
				try {
					zoneId = ZoneId.of(Page.getCurrent().getWebBrowser().getTimeZoneId());
				} catch (Exception e) {
					zoneId = ZoneId.of("CET");
				}
				
				timeZone = TimeZone.getTimeZone(zoneId);
				// timeZone = TimeZone.getTimeZone(ZoneOffset.ofTotalSeconds((Page.getCurrent().getWebBrowser().getTimezoneOffset()) / 1000));
			}

		} catch (final Exception e) {
			log.warn("getLoggedInUserLocale error (Page ZoneOffset determination failed) (will use fallback)!");
		}

		if (timeZone == null) {
			timeZone = getUserTimeZone(SecurityUtil.getLoggedInUser());
		}

		return timeZone;
	}

	/**
	 * @return
	 * 
	 * @see #getUserMeasurementSystem(ToolboxUserDetails)
	 */
	public static SystemOfUnits getLoggedInUserMeasurementSystem() {
		return getUserMeasurementSystem(SecurityUtil.getLoggedInUser());
	}

	/**
	 * @param msgs
	 * @return
	 * 
	 * @see #extractMsgFromMultiLang(String, String, String)
	 */
	public static String packageIntoMultiLang(final Iterable<Pair<String, String>> msgs) {

		if (msgs == null) {
			return null;
		}

		final JSONObject jsonObject = new JSONObject();

		for (final Pair<String, String> msg : msgs) {
			jsonObject.put(msg.getKey().substring(0, 2), msg.getValue()); // a substring az en_US, hu_HU... _US/_HU levágáshoz kell (elvileg már nem kaphat ilyet ez a metódus, de biztos, ami biztos levágjuk)
		}

		return jsonObject.toString();
	}

	/**
	 * @param msgs
	 * @return
	 *
	 * @see #extractMsgFromMultiLang(String, String, String)
	 */
	public static String packageIntoMultiLangString(final Map<String, String> msgs) {

		if (msgs == null) {
			return null;
		}

		final JSONObject jsonObject = new JSONObject();

		for (final Entry<String, String> msg : msgs.entrySet()) {
			jsonObject.put(msg.getKey().substring(0, 2), msg.getValue()); // a substring az en_US, hu_HU... _US/_HU levágáshoz kell (elvileg már nem kaphat ilyet ez a metódus, de biztos, ami biztos levágjuk)
		}

		return jsonObject.toString();
	}

	/**
	 *
	 * @param jsonStr
	 * @return
	 */
	public static Map<String, String> multiLangToMapString(final String jsonStr) {

		if (jsonStr == null) {
			return null;
		}

		final JSONObject jsonObject = new JSONObject(jsonStr);

		final Map<String, String> map = new HashMap<>();

		for (final String langCode : JSONObject.getNames(jsonObject)) {
			map.put(langCode, jsonObject.getString(langCode));
		}

		return map;
	}

	/**
	 * @param msgs
	 * @return
	 * 
	 * @see #extractMsgFromMultiLang(String, String, String)
	 */
	public static String packageIntoMultiLang(final Map<Locale, String> msgs) {

		if (msgs == null) {
			return null;
		}

		final JSONObject jsonObject = new JSONObject();

		for (final Entry<Locale, String> msg : msgs.entrySet()) {
			jsonObject.put(msg.getKey().getLanguage().substring(0, 2), msg.getValue()); // a substring az en_US, hu_HU... _US/_HU levágáshoz kell (elvileg már nem kaphat ilyet ez a metódus, de biztos, ami biztos levágjuk)
		}

		return jsonObject.toString();
	}

	/**
	 * 
	 * @param jsonStr
	 * @return
	 */
	public static Map<Locale, String> multiLangToMap(final String jsonStr) {

		if (jsonStr == null) {
			return null;
		}

		final JSONObject jsonObject = new JSONObject(jsonStr);

		final Map<Locale, String> map = new HashMap<>();

		for (final String langCode : JSONObject.getNames(jsonObject)) {
			final Locale locale = Locale.forLanguageTag(langCode);
			map.put(locale, jsonObject.getString(langCode));
		}

		return map;
	}

	public static DecimalFormatSymbols getLoggedInUserDecimalFormatSymbols() {
		return ((DecimalFormat) DecimalFormat.getInstance(getLoggedInUserLocale())).getDecimalFormatSymbols();
	}

	private I18nUtil() {
		//
	}

}
