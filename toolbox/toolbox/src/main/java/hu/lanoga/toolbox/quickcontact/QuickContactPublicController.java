package hu.lanoga.toolbox.quickcontact;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.util.concurrent.RateLimiter;
import com.teamunify.i18n.I;

import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.tenant.Tenant;
import hu.lanoga.toolbox.tenant.TenantJdbcRepository;
import hu.lanoga.toolbox.util.BrandUtil;
import hu.lanoga.toolbox.util.ToolboxAssert;

@ConditionalOnMissingBean(name = "quickContactControllerOverrideBean")
@ConditionalOnProperty(name = "tools.quick-contact.controller.enabled")
@RestController
public class QuickContactPublicController {

	private static final String SERVER_SUCCESS_FORWARD_PREFIX = "fw-";

	private static RateLimiter rateLimiter = RateLimiter.create(1.0);

	@Autowired
	private QuickContactService quickContactService;

	@Autowired
	private TenantJdbcRepository tenantJdbcRepository;

//	@Autowired
//	private GoogleDriveManager googleDriveManager;

	private Properties velocityProp;

	@PostConstruct
	private void init() {
		this.velocityProp = new Properties();
		this.velocityProp.setProperty("resource.loader", "class");
		this.velocityProp.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
	}

	@RequestMapping(value = "public/qc/{tenant-name}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String getQuickContact(@PathVariable("tenant-name") final String tenantName, final HttpServletRequest request, final HttpServletResponse response) {

		Velocity.init(this.velocityProp);

		ToolboxAssert.isTrue(StringUtils.isNotBlank(tenantName));

		final VelocityContext context = new VelocityContext();
		context.put("brand", BrandUtil.getBrand(false, true));
		context.put("devmode", request.getParameter("dev") != null);
		context.put("gs", request.getParameter("gs") != null);
		context.put("origin", request.getParameter("s"));

		setLabelsToContext(context);
		setThemeToContext(context, request);

		final StringWriter writer = new StringWriter();
		Velocity.mergeTemplate("html_templates/quick-contact-form.vm", "UTF-8", context, writer);

		response.addHeader("X-Frame-Options", "ALLOWALL");

		return writer.toString();

	}

	@RequestMapping(value = "public/qc/{tenant-name}", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity<String> postQuickContact(@PathVariable("tenant-name") String tenantName, final HttpServletRequest request) {

		// TODO: védelem, egy durva felső limit mennyiség (per perc / per óra limit) (nehogy egy támadó ilyen módon leterhelje a rendszert és a DB-t...) (ez viszont projekt specifikus kell legyen)

		rateLimiter.acquire();

		// ---

		ToolboxAssert.isTrue(StringUtils.isNotBlank(tenantName));

		Velocity.init(this.velocityProp);

		// ---

		// mj.: itt nem kell a request.getLocale()-lal foglalkozni, mert van már a hu.lanoga.toolbox.config.RequestFilterConfig megoldja

		// ---

		final VelocityContext context = new VelocityContext();
		context.put("brand", BrandUtil.getBrand(false, true));

		setLabelsToContext(context);
		setThemeToContext(context, request);

		// ---

		tenantName = Jsoup.clean(tenantName, Safelist.none());
		tenantName = tenantName.trim().toLowerCase();

		// ---

		String gs = request.getParameter("gs");
		if (StringUtils.isNotBlank(gs)) {
			gs = Jsoup.clean(gs, Safelist.none());
		}

		String origin = request.getParameter("origin");
		if (StringUtils.isNotBlank(origin)) {
			origin = Jsoup.clean(origin, Safelist.none());
		}

		// ---

		String companyName = request.getParameter("companyName");
		if (StringUtils.isNotBlank(companyName)) {
			companyName = Jsoup.clean(companyName, Safelist.none());
		}

		String contactName = request.getParameter("contactName");
		if (StringUtils.isNotBlank(contactName)) {
			contactName = Jsoup.clean(contactName, Safelist.none());
		}

		String email = request.getParameter("email");
		if (StringUtils.isNotBlank(email)) {
			email = Jsoup.clean(email, Safelist.none());
		}

		String phoneNumber = request.getParameter("phoneNumber");
		if (StringUtils.isNotBlank(phoneNumber)) {
			phoneNumber = Jsoup.clean(phoneNumber, Safelist.none());
		}

		String country = request.getParameter("country");
		if (StringUtils.isNotBlank(country)) {
			country = Jsoup.clean(country, Safelist.none());
		}

		String city = request.getParameter("city");
		if (StringUtils.isNotBlank(city)) {
			city = Jsoup.clean(city, Safelist.none());
		}

		String cityDetails = request.getParameter("cityDetails");
		if (StringUtils.isNotBlank(cityDetails)) {
			cityDetails = Jsoup.clean(cityDetails, Safelist.none());
		}

		// ---

		String extraFieldName1 = request.getParameter("extraFieldName1");
		if (StringUtils.isNotBlank(extraFieldName1)) {
			extraFieldName1 = Jsoup.clean(extraFieldName1, Safelist.none());
		}

		String extraFieldValue1 = request.getParameter("extraFieldValue1");
		if (StringUtils.isNotBlank(extraFieldValue1)) {
			extraFieldValue1 = Jsoup.clean(extraFieldValue1, Safelist.none());
		}

		String extraFieldName2 = request.getParameter("extraFieldName2");
		if (StringUtils.isNotBlank(extraFieldName2)) {
			extraFieldName2 = Jsoup.clean(extraFieldName2, Safelist.none());
		}

		String extraFieldValue2 = request.getParameter("extraFieldValue2");
		if (StringUtils.isNotBlank(extraFieldValue2)) {
			extraFieldValue2 = Jsoup.clean(extraFieldValue2, Safelist.none());
		}

		String extraFieldName3 = request.getParameter("extraFieldName3");
		if (StringUtils.isNotBlank(extraFieldName3)) {
			extraFieldName3 = Jsoup.clean(extraFieldName3, Safelist.none());
		}

		String extraFieldValue3 = request.getParameter("extraFieldValue3");
		if (StringUtils.isNotBlank(extraFieldValue3)) {
			extraFieldValue3 = Jsoup.clean(extraFieldValue3, Safelist.none());
		}

		// ---

		String note = request.getParameter("note");
		if (StringUtils.isNotBlank(note)) {
			note = Jsoup.clean(note, Safelist.none());
		}

		String serverValidationErrorMsg = request.getParameter("serverValidationErrorMsg");
		if (StringUtils.isNotBlank(serverValidationErrorMsg)) {
			serverValidationErrorMsg = Jsoup.clean(serverValidationErrorMsg, Safelist.none());
		}

		String serverSuccessMsg = request.getParameter("serverSuccessMsg");
		if (StringUtils.isNotBlank(serverSuccessMsg)) {
			serverSuccessMsg = Jsoup.clean(serverSuccessMsg, Safelist.none());
		}

		companyName = StringUtils.abbreviate(companyName, 50);
		contactName = StringUtils.abbreviate(contactName, 50);
		email = StringUtils.abbreviate(email, 100);
		phoneNumber = StringUtils.abbreviate(phoneNumber, 50);
		country = StringUtils.abbreviate(country, 100);
		city = StringUtils.abbreviate(city, 50);
		cityDetails = StringUtils.abbreviate(cityDetails, 100);

		extraFieldName1 = StringUtils.abbreviate(extraFieldName1, 50);
		extraFieldValue1 = StringUtils.abbreviate(extraFieldValue1, 100);
		extraFieldName2 = StringUtils.abbreviate(extraFieldName2, 50);
		extraFieldValue2 = StringUtils.abbreviate(extraFieldValue2, 100);
		extraFieldName3 = StringUtils.abbreviate(extraFieldName3, 50);
		extraFieldValue3 = StringUtils.abbreviate(extraFieldValue3, 100);

		origin = StringUtils.abbreviate(origin, 100);
		note = StringUtils.abbreviate(note, 500);

		serverValidationErrorMsg = StringUtils.abbreviate(serverValidationErrorMsg, 100);
		serverSuccessMsg = StringUtils.abbreviate(serverSuccessMsg, 100);

		if (StringUtils.isBlank(serverValidationErrorMsg)) {
			serverValidationErrorMsg = I.trc("Notification", I.trc("Notification", "Please fill these fields: name, email and/or phone"));
		}

		if (StringUtils.isBlank(serverSuccessMsg)) {
			serverSuccessMsg = I.trc("Notification", I.trc("Notification", "Successful submit!"));
		}

		// ---

		if (StringUtils.isBlank(contactName) || (StringUtils.isBlank(email) && StringUtils.isBlank(phoneNumber))) {

			context.put("serverValidationErrorMsg", serverValidationErrorMsg);

		} else {

			final QuickContact quickContact = new QuickContact();
			quickContact.setCompanyName(companyName);
			quickContact.setContactName(contactName);
			quickContact.setPhoneNumber(phoneNumber);
			quickContact.setEmail(email);
			quickContact.setCountry(country);
			quickContact.setCity(city);
			quickContact.setCityDetails(cityDetails);
			quickContact.setOrigin(origin);
			quickContact.setExtraFieldName1(extraFieldName1);
			quickContact.setExtraFieldValue1(extraFieldValue1);
			quickContact.setExtraFieldName2(extraFieldName2);
			quickContact.setExtraFieldValue2(extraFieldValue2);
			quickContact.setExtraFieldName3(extraFieldName3);
			quickContact.setExtraFieldValue3(extraFieldValue3);
			quickContact.setNote(note);

			{

				if (Boolean.parseBoolean(gs)) {
					// googleDriveManager.updateQuickContactFile(null, "sales-public-form-list", quickContact); // TODO: outdated, clean-up if needed again
				} else {

					Tenant selectedTenant = null;

					try {

						SecurityUtil.setSystemUser();
						selectedTenant = tenantJdbcRepository.findByTenantName(tenantName);

						ToolboxAssert.notNull(selectedTenant);

						JdbcRepositoryManager.setTlTenantId(selectedTenant.getId());

						this.quickContactService.save(quickContact);

					} finally {
						JdbcRepositoryManager.clearTlTenantId();
						SecurityUtil.clearAuthentication();
					}

				}
			}

			if (serverSuccessMsg.startsWith(SERVER_SUCCESS_FORWARD_PREFIX)) {

				try {
					HttpHeaders responseHeaders = new HttpHeaders();
					responseHeaders.add("Location", new String(Base64Utils.decodeFromUrlSafeString(StringUtils.removeStart(serverSuccessMsg, SERVER_SUCCESS_FORWARD_PREFIX)), "UTF-8"));
					return new ResponseEntity<>(responseHeaders, HttpStatus.FOUND);
				} catch (UnsupportedEncodingException e) {
					//
				}
			}

			context.put("serverSuccessMsg", serverSuccessMsg);

		}

		// ---

		final StringWriter writer = new StringWriter();
		Velocity.mergeTemplate("html_templates/quick-contact-form.vm", "UTF-8", context, writer);

		return new ResponseEntity<>(writer.toString(), HttpStatus.OK);

	}

	private static void setLabelsToContext(final VelocityContext context) {

		context.put("lblCompanyName", I.trc("Label", "Company name:"));
		context.put("lblContactName", I.trc("Label", "Name:"));
		context.put("lblEmail", I.trc("Label", "Email:"));
		context.put("lblPhoneNumber", I.trc("Label", "Phone:"));
		context.put("lblCountry", I.trc("Label", "Country:"));
		context.put("lblCity", I.trc("Label", "City:"));
		context.put("lblAddress", I.trc("Label", "Address details:"));
		context.put("lblNote", I.trc("Label", "Notes:"));

		context.put("lblSubmit", I.trc("Button", "Submit"));
		context.put("lblGoBack", I.trc("Button", "Go back"));

	}

	/**
	 * bootstrap 3 theme-ek innen: https://www.bootstrapcdn.com/legacy/bootswatch/
	 * 
	 * @param context
	 * @param request
	 */
	private static void setThemeToContext(final VelocityContext context, final HttpServletRequest request) {

		String theme = request.getParameter("theme");
		if (StringUtils.isNotBlank(theme)) {
			theme = Jsoup.clean(theme, Safelist.none()).toLowerCase();
		} else {
			theme = "readable";
		}
		context.put("theme", theme);

		// ---

		String cs = request.getParameter("cs");

		if (StringUtils.isNotBlank(cs)) {

			try {
				cs = new String(Base64Utils.decodeFromUrlSafeString(cs), "UTF-8");
			} catch (Exception e) {
				throw new ToolboxGeneralException("decodeFromString error", e);
			}
			
			// boolean validInput = ESAPI.validator().isValidInput("QC CSS param", cs, "CSS", 1500, false);
			// if (!validInput) {
			// throw new ToolboxGeneralException("ESAPI.validator().isValidInput error");
			// }

			cs = Jsoup.clean(cs, Safelist.none());

			context.put("cs", cs);
		}

	}

}