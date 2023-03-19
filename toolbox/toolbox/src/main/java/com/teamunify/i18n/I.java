package com.teamunify.i18n; // 

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import ch.poole.poparser.ParseException;
import ch.poole.poparser.Po;
import ch.poole.poparser.PoParser;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.i18n.I18nUtil;

/**
 * Már nincs közünk a teamunify.i18n lib-hez, 
 * de ez maradt a package neve komp. okokból... 
 * 
 * A teljes munka flow így néz ki: 
 * 
 * 1) 
 * Poedit beszerzése (https://github.com/vslavik/poedit, https://poedit.net/download) 
 * (van több azonos nevű szoftver, mi a desktop szoftvert használtuk) 
 * 
 * 2) 
 * Ezzel kell pásztáztatni a kódot. 
 * 
 * "File" -> "New" -> "Extract from existing sources" 
 * "Sources keywords" fülre rámenni -> "Additional keywords"-höz hozzáadni a következőket: 
 * I.trc:1c,2 és CrudGridColumn:1 
 * 
 * Ne felejtsd el kikapcsolni az alsó checkbox-ot ("Also use default keywords for supported languages"). 
 * 
 * 3) 
 * A "Sources paths" részben kell a Java src folder-t kiválasztani 
 * 
 * 4) 
 * Ne felejts el menteni! 
 * 
 * 5) 
 * Az elkészült fájlokat ide kell tenni: 
 * a) toolbox\src\main\resources\translation\toolbox-xy.po (xy = kis kétbetűs nyelvkód: hu, en stb.) 
 * b) toolbox\src\main\resources\translation\project-xy.po (xy = kis kétbetűs nyelvkód: hu, en stb.) 
 * 
 * -------------- 
 * 
 * felülírási sorrend: 
 * project felirata > ha ez nincs toolbox felirata -> ha ez sincs, akkor a default (ami, az I.trc paramétere) 
 */
public class I {

	private static final ThreadLocal<Locale> cacheLocale = new ThreadLocal<>();

	private static Locale getCurrentLocale() {

		Locale locale = cacheLocale.get();

		if (locale == null) {

			try {

				locale = I18nUtil.getLoggedInUserLocale();
				locale = new Locale(StringUtils.abbreviate(locale.getLanguage(), "", 2)); // itt levágjuk a változatot (ha netán van), pl.: hu_HU -> hu lesz

			} catch (final Exception e) {

				locale = Locale.ENGLISH;

			}

			cacheLocale.set(locale);

			// log.debug("locale: " + locale);

		}

		return locale;
	}

	/**
	 * {@link ThreadLocal} alapú
	 * 
	 * @param l
	 */
	public static void setLocale(final Locale l) {
		cacheLocale.set(l);
	}

	/**
	 * {@link ThreadLocal} alapú
	 */
	public static void clearLocale() {
		cacheLocale.remove();
	}

	// -------------------------

	// itt több helyen sima HashMap van most (a Po lib-en belül is)
	// ez elvileg ok, mert itt egyszer írjuk be az egészet
	// TODO: de még friss, nem tesztelt kellően

	private static volatile Map<String, Po> poMap;
	private static Object lockbox = new Object();

	/**
	 * ajánlott még Spring init előtt hívni
	 * 
	 * @return
	 * 		némi log információ
	 */
	public static String init() {
		
		final StringBuilder sbLog = new StringBuilder("I init(): ");
		sbLog.append(System.lineSeparator());

		final Map<String, Po> newMap = new HashMap<>();
		
		synchronized (lockbox) { // https://www.ibm.com/developerworks/library/j-hashmap/index.html "Use HashMap for better multithreaded performance"

			try {

				for (final String langCode : Locale.getISOLanguages()) {

					final Resource[] poFileResProject = new PathMatchingResourcePatternResolver().getResources("classpath:/translation/project-" + langCode + ".po");
					Resource primary = null;

					if (poFileResProject.length > 0) {

						primary = poFileResProject[0];

						if (primary != null && !primary.exists()) {
							primary = null;
						}
					}

					final Resource[] poFileResToolbox = new PathMatchingResourcePatternResolver().getResources("classpath:/translation/toolbox-" + langCode + ".po");
					Resource secondary = null;

					if (poFileResToolbox.length > 0) {

						if (primary == null) {
							primary = poFileResToolbox[0];
						} else {
							secondary = poFileResToolbox[0];
						}

					}

					if (primary != null && !primary.exists()) {
						primary = null;
					}

					if (secondary != null && !secondary.exists()) {
						secondary = null;
					}

					if (primary != null) {

						Po p;

						try (InputStream is = primary.getInputStream()) {

							sbLog.append("langCode: ");
							sbLog.append(langCode);
							sbLog.append(", primary source: ");
							sbLog.append(primary.toString());
							sbLog.append(System.lineSeparator());

							p = new Po(is);

						}

						if (secondary != null) {

							try (InputStream is = secondary.getInputStream()) {

								sbLog.append("langCode: ");
								sbLog.append(langCode);
								sbLog.append(", secondary source: ");
								sbLog.append(secondary.toString());
								sbLog.append(System.lineSeparator());

								// p.addFallback(is); // nem pont azt csinálja, ami nekünk kell
								addFallbackSpecial(p.getMap(), is);

							}

						}

						newMap.put(langCode, p);

					}

				}

			} catch (final Exception e) {
				throw new ToolboxGeneralException("I.setLanguageSettingsProvider() failed!", e);
			}

		}

		poMap = newMap;
		
		return sbLog.toString();

	}

	public static void addFallbackSpecial(final Map<String, HashMap<String, String>> m, final InputStream is) throws ParseException {

		final PoParser pp = new PoParser(is);

		final Map<String, HashMap<String, String>> fallback = pp.getMap();

		for (final String ctxt : fallback.keySet()) { // loop over contexts

			final HashMap<String, String> fallbackTranslations = fallback.get(ctxt);

			if (fallbackTranslations == null) { // skip
				continue;
			}

			HashMap<String, String> translations = m.get(ctxt);

			if (translations == null) {
				translations = new HashMap<>();
				m.put(ctxt, translations);
			}

			for (final String untranslated : new TreeSet<>(fallbackTranslations.keySet())) { // shallow copy needed

				if (translations.get(untranslated) == null) {

					final String fallbackTranslation = fallbackTranslations.get(untranslated);

					if (fallbackTranslation != null) {
						translations.put(untranslated, fallbackTranslation);
					}
				}
			}

		}
	}

	public static String trc(final String contextStr, final String defaultStrAndId) {

		final Map<String, Po> actualMap = poMap;
		
		if (actualMap == null) {
			return defaultStrAndId;
		}
		
		// ---
		
		final Locale locale = getCurrentLocale();
		final String langCode = locale.getLanguage();

		// ---

		String retVal = defaultStrAndId;

		final Po p = actualMap.get(langCode);

		if (p != null) {

			if (contextStr != null) {
				retVal = p.t(contextStr, defaultStrAndId);
			} else {
				retVal = p.t(defaultStrAndId);
			}

		}

		// log.info("trc, langCode: " + langCode + ", retVal: " + retVal);

		return retVal;

	}

	public static String tr(final String defaultStrAndId) {
		return trc(null, defaultStrAndId);
	}

}
