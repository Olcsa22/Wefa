package hu.lanoga.toolbox.util;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.controller.ToolboxHttpUtils;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.tenant.TenantJdbcRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * brand = 
 * 		ha az alkalmazás több domain alatt, 
 * 		több kinézettel (dinamikus domain függvényében) kell megjelenjen
 * 		(kvázi egy kód az ügyfelek egy részének lényegileg külön termékként látszik)
 */
@Slf4j
public class BrandUtil {

	private BrandUtil() {
		//
	}

	/**
	 * @param removePrefix
	 * @param toLowerCase
	 * 		ez a metódus alapban upperCase output-ot ad, ezzel lehet lowerCase-t...
	 * @return
	 */
	public static String getBrand(final boolean removePrefix, final boolean toLowerCase) {

		final boolean isDynamicMulti = ApplicationContextHelper.getConfigProperty("tools.brand.dynamic-multi.enabled", Boolean.class);
		final String brandFromProps = ApplicationContextHelper.getConfigProperty("tools.brand", String.class).trim();

		if (!isDynamicMulti) {
			return getBrandStringHandle(brandFromProps, removePrefix, toLowerCase);
		}
		
		// ---
		
		// isDynamicMulti

		// első szint

		final ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();
		
		if (loggedInUser != null && !loggedInUser.getTenantId().equals(ToolboxSysKeys.UserAuth.COMMON_TENANT_ID)) {
					
			final String brandFromTenant = ApplicationContextHelper.getBean(TenantJdbcRepository.class).findOne(SecurityUtil.getLoggedInUserTenantId()).getBrand();		
			
			if (StringUtils.isNotBlank(brandFromTenant)) {
				return getBrandStringHandle(brandFromTenant, removePrefix, toLowerCase);
			}
			
		}
		
		// második szint
		
		try {
			final HttpServletRequest httpServletRequest = ToolboxHttpUtils.getCurrentHttpServletRequest();
			final String currentUrl = httpServletRequest.getRequestURL().toString();
			
			final String urlToBrandStr = ApplicationContextHelper.getConfigProperty("tools.brand.dynamic-multi.urltobrand", String.class);
			final String[] split1 = urlToBrandStr.split(";");
			
			for (final String urlBrandPairStr : split1) {
				final String[] split2 = urlBrandPairStr.split(",");
				if (currentUrl.contains(split2[0])) {
					return getBrandStringHandle(split2[1], removePrefix, toLowerCase);
				}
			}
			
			log.debug("getBrand dynamicMulti second level failed (no match) (might be normal)");
			
		} catch (final Exception e) {
			log.debug("getBrand dynamicMulti second level failed (error) (might be normal)");
		}
		
		// végső fallback a props
		
		return getBrandStringHandle(brandFromProps, removePrefix, toLowerCase);

	}

	private static String getBrandStringHandle(final String brand, final boolean removePrefix, final boolean toLowerCase) {

		String brand2 = brand.toUpperCase();
		
		if (removePrefix) {
			brand2 = StringUtils.removeStart(brand2, "B-");
		}

		if (toLowerCase) {
			brand2 = brand2.toLowerCase();
		}

		return brand2;
	}

	/**
	 * ha nincs redirectUriHostBackendBrand property, akkor vissza adjuk a redirectUriHostBackend-et... 
	 * 	mindig lesz '/' a végén
	 *
	 * @return
	 */
	public static String getRedirectUriHostBackend() {
				
		// getCurrentRequest().getContextPath() ide is kellhet, de egylőre inkább jobb, ha kézzel be van írva ebbe a property-be is a contextPath adott esetben (ha nem root a contextPath)
		
		final String redirectUriHostBackendBasic = ApplicationContextHelper.getConfigProperty("tools.redirect-uri-host-backend");
		final String redirectUriHostBackendBrand = ApplicationContextHelper.getConfigProperty("tools.redirect-uri-host-backend-brand");

		// ha csak egy darab brand van megadva, akkor ez nem kerül használatra
		// fallback redirectUriHostBackend

		if (StringUtils.isNotBlank(redirectUriHostBackendBrand) && StringUtils.contains(redirectUriHostBackendBrand, ";")) {

			final String brand = BrandUtil.getBrand(false, true);

			String[] brandUrlPairs = redirectUriHostBackendBrand.split(";");

			// ha nem található a brand, a property-ben, akkor fallback redirectUriHostBackend
			for (String brandUrlPair : brandUrlPairs) {

				String[] brandAndUrl = brandUrlPair.split(",");
				if (brand.equalsIgnoreCase(brandAndUrl[0])) {
					
					return StringUtils.appendIfMissing(brandAndUrl[1], "/");
				}

			}

			return StringUtils.appendIfMissing(redirectUriHostBackendBasic, "/");

		} else {
			return StringUtils.appendIfMissing(redirectUriHostBackendBasic, "/");
		}

	}

	/**
	 * ha nincs redirectUriHostFrontendBrand property, akkor vissza adjuk a redirectUriHostFrontend-et... 
	 * mindig lesz '/' a végén
	 *
	 * @return
	 */
	public static String getRedirectUriHostFrontend() {
		
		// getCurrentRequest().getContextPath() ide is kellhet, de egylőre inkább jobb, ha kézzel be van írva ebbe a property-be is a contextPath adott esetben (ha nem root a contextPath)
	
		final String redirectUriHostFrontendBasic = ApplicationContextHelper.getConfigProperty("tools.redirect-uri-host-frontend");
		final String redirectUriHostFrontendBrand = ApplicationContextHelper.getConfigProperty("tools.redirect-uri-host-frontend-brand");

		// ha csak egy darab brand van megadva, akkor ez nem kerül használatra
		// fallback redirectUriHostFrontend
		
		if (StringUtils.isNotBlank(redirectUriHostFrontendBrand) && StringUtils.contains(redirectUriHostFrontendBrand, ";")) {

			final String brand = BrandUtil.getBrand(false, true);

			String[] brandUrlPairs = redirectUriHostFrontendBrand.split(";");

			// ha nem található a brand, a property-ben, akkor fallback redirectUriHostFrontend
			for (String brandUrlPair : brandUrlPairs) {

				String[] brandAndUrl = brandUrlPair.split(",");
				if (brand.equalsIgnoreCase(brandAndUrl[0])) {
					return StringUtils.appendIfMissing(brandAndUrl[1], "/");
				}

			}

			return StringUtils.appendIfMissing(redirectUriHostFrontendBasic, "/");

		} else {
			return StringUtils.appendIfMissing(redirectUriHostFrontendBasic, "/");
		}

	}

	/**
	 * @param addGitInfo
	 * @return
	 */
	public static String getAppTitle(final boolean addGitInfo) {
	
		final StringBuilder sbResult = new StringBuilder();
	
		final String brand = getBrand(true, false);
		final String appName = ApplicationContextHelper.getConfigProperty("tools.misc.application-name", String.class).trim().toUpperCase();
	
		if (!"DEFAULT".equals(brand)) {
			sbResult.append(brand);
			sbResult.append(" ");
		}
	
		sbResult.append(appName);
	
		if (addGitInfo) {
			sbResult.append(" ");
			sbResult.append(DiagnosticsHelper.getShortBuildAndGitCommitInfo());
		}
	
		return sbResult.toString();
	
	}

}
