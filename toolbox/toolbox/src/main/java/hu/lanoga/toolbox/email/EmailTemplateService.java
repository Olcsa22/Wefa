package hu.lanoga.toolbox.email;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.service.AdminOnlyCrudService;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.tenant.TenantJdbcRepository;
import hu.lanoga.toolbox.util.BrandUtil;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@ConditionalOnMissingBean(name = "emailTemplateServiceOverrideBean")
@Service
public class EmailTemplateService extends AdminOnlyCrudService<EmailTemplate, EmailTemplateJdbcRepository> {

	@Autowired
	private TenantJdbcRepository tenantJdbcRepository;

	@Autowired
	private EmailJdbcRepository emailJdbcRepository;

	@Value("${tools.mail.sender.username}")
	private String sender;

	@Value("${tools.misc.application-name}")
	private String applicationName;

	@Value("${tools.email-template.simple}")
	private Boolean isSimpleMode;

	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	public void enable(final Integer id) {
		repository.enable(id);
	}

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR)
	public void delete(int id) {
		super.delete(id);
	}

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR)
	public EmailTemplate save(EmailTemplate emailTemplate) {
		return super.save(emailTemplate);
	}

	/**
	 * template fill és hozzáadás a küldendőkhöz...
	 *
	 * @param senderUser
	 * @param toEmail
	 * @param templCode
	 * @param valueMap
	 * @param localeStr
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public void addMail(final ToolboxUserDetails senderUser, final String toEmail, final int templCode, final Map<String, Object> valueMap, final String localeStr) {
		addMail(senderUser, toEmail, templCode, valueMap, I18nUtil.localeStrToLocale(localeStr), ToolboxSysKeys.EmailStatus.CREATED, null);
	}

	/**
	 * template fill és hozzáadás a küldendőkhöz...
	 *
	 * @param senderUser
	 * @param toEmail
	 * @param templCode
	 * @param valueMap
	 * @param locale
	 * @see EmailService#addMail(String, String, String, Boolean)
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public void addMail(final ToolboxUserDetails senderUser, final String toEmail, final int templCode, final Map<String, Object> valueMap, final Locale locale) {
		addMail(senderUser, toEmail, templCode, valueMap, locale, ToolboxSysKeys.EmailStatus.CREATED, null);
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public Email addMail(final ToolboxUserDetails senderUser, final String toEmail, final int templCode, final Map<String, Object> valueMap, final Locale locale, final Integer initialStatus) {
		return addMail(senderUser, toEmail, templCode, valueMap, locale, initialStatus, null);
	}

	/**
	 * template fill és hozzáadás a küldendőkhöz...
	 *
	 * @param senderUser
	 * @param toEmail
	 * @param templCode
	 * @param valueMap
	 * @param locale
	 * @see EmailService#addMail(String, String, String, Boolean)
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public Email addMail(final ToolboxUserDetails senderUser, final String toEmail, final int templCode, final Map<String, Object> valueMap, final Locale locale, final Integer initialStatus, final String fileIds) {

		// néhány fix mező (ha a template-ben nincs benne a key, akkor nem jelenik meg)

		final Map<String, Object> valueMap2 = new HashMap<>(valueMap);

		if (!valueMap2.containsKey("currentDate")) {
			valueMap2.put("currentDate", DateFormat.getDateInstance(DateFormat.SHORT, locale).format(new Date()));
		}

		if (!valueMap2.containsKey("principal")) {
			valueMap2.put("principal", I18nUtil.buildFullName(senderUser, locale, false));
		}

		if (!valueMap2.containsKey("tenantName")) {
			valueMap2.put("tenantName", tenantJdbcRepository.findOne(senderUser.getTenantId()).getName());
		}

		if (!valueMap2.containsKey("logoImg")) {
			valueMap2.put("logoImg", EmailSender.EMAIL_DEFAULT_LOGO_PLACEHOLDER);
		}

		if (!valueMap2.containsKey("appName")) {
			valueMap2.put("appName", BrandUtil.getAppTitle(false));
		}

		// ---

		final VelocityContext velocityCtx = new VelocityContext();

		for (final Map.Entry<String, Object> entry : valueMap2.entrySet()) {
			final String key = entry.getKey();
			final Object value = entry.getValue();
			velocityCtx.put(key, value);
		}

		String subject;
		String body;

		if (Boolean.TRUE.equals(isSimpleMode)) {

			try (StringWriter swOut = new StringWriter()) {

				Velocity.mergeTemplate(getSimpleTemplateResourcePath(templCode, true, locale.getLanguage()), "UTF-8", velocityCtx, swOut);
				subject = swOut.toString();

			} catch (final Exception e) {
				throw new EmailException(e);
			}

			try (StringWriter swOut = new StringWriter()) {
				Velocity.mergeTemplate(getSimpleTemplateResourcePath(templCode, false, locale.getLanguage()), "UTF-8", velocityCtx, swOut);
				body = swOut.toString();

			} catch (final Exception e) {
				throw new EmailException(e);
			}

		} else {

			final EmailTemplate emailTemplate = repository.findOneBy("templCode", templCode, "enabled", true);

			if (emailTemplate == null) {
				throw new EmailException("Template is unavailable!");
			}

			try (StringWriter swOut = new StringWriter()) {
				Velocity.evaluate(velocityCtx, swOut, "", I18nUtil.extractMsgFromMultiLang(emailTemplate.getSubject(), locale.getLanguage(), null));
				subject = swOut.toString();

			} catch (final Exception e) {
				throw new EmailException(e);
			}

			try (StringWriter swOut = new StringWriter()) {
				Velocity.evaluate(velocityCtx, swOut, "", I18nUtil.extractMsgFromMultiLang(emailTemplate.getTemplContent(), locale.getLanguage(), null));
				body = swOut.toString();

			} catch (final Exception e) {
				throw new EmailException(e);
			}
		}

		// ---

		final Email email = new Email();
		email.setFromEmail(null);
		email.setToEmail(toEmail); // ;"-vel elválasztva lehet több címet is
		email.setSubject(subject);
		email.setBody(body);
		email.setAttempt(0);
		email.setIsPlainText(false);
		email.setStatus(initialStatus);
		email.setFileIds(fileIds);

		return emailJdbcRepository.save(email);

	}



	private String getSimpleTemplateResourcePath(final int emailTemplateCode, final boolean isSubject, final String langCode) {

		// a végeredmény hasonlót fog vissza adni: "email_templates/generated_password/subject-hu.vm"
		StringBuilder strPathBuilder = new StringBuilder();
		strPathBuilder.append("email_templates/");

		strPathBuilder.append(emailTemplateCode);
		strPathBuilder.append("/");

		if (isSubject) {
			strPathBuilder.append("subject-");
		} else {
			strPathBuilder.append("body-");
		}

		strPathBuilder.append(langCode);
		strPathBuilder.append(".vm");

		return strPathBuilder.toString();
	}

}
