package hu.lanoga.toolbox.email;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.MultiPartEmail;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileDescriptorJdbcRepository;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.util.ToolboxStringUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ConditionalOnMissingBean(name = "emailSenderOverrideBean")
@ConditionalOnProperty({ "tools.job-runner", "tools.mail.sender.scheduler.enabled" })
@Component
class EmailSender {

	/**
	 * második template változó szint (először a velocity ezt helyettesíti be, az attachment miatt a végső érték csak küldésnél lesz meg...)
	 */
	static final String EMAIL_DEFAULT_LOGO_PLACEHOLDER = "###EMAIL_DEFAULT_LOGO_PLACEHOLDER###";

	@Value("${tools.mail.sender.username}")
	private String username;

	@Value("${tools.mail.sender.fromAddress}")
	private String senderFromAddress;

	@Value("${tools.mail.sender.fromAddress.fixed}")
	private boolean fixedSenderFromAddress;

	@Value("${tools.mail.sender.password}")
	private String password;

	@Value("${tools.mail.sender.host}")
	private String hostName;

	@Value("${tools.mail.sender.port:587}")
	private int smtpPort;

	/**
	 * ez akkor kell, amikor fix SSL/TLS kapcsolat van, 
	 * ez többnyire a 465 port esetén van így
	 */
	@Value("${tools.mail.sender.ssl-on-connect:true}")
	private boolean sslOnConnect;
	
	/**
	 * ez akkor kell, amikor az úgynevezett "STARTTLS" megoldás van, ami 
	 * egy kvázi dinamikus dolog, ha a szerver támogat SSL/TLS, akkor azzal megy,  
	 * ha nem akkor kapcsolat encrpytion nélkül... 
	 * többnyire az 587-es port kapcsán van ez a STARTTLS 
	 * 
	 * lásd pl.: https://sendgrid.com/blog/whats-the-difference-between-ports-465-and-587/
	 */
	@Value("${tools.mail.sender.start-tls-enabled:false}")
	private boolean startTLSEnabled;

	@Value("${tools.mail.logo-path}")
	private String logoPath;

	@Autowired
	private FileStoreService fileStoreService;

	// @Autowired
	// private TenantKeyValueSettingsService tenantKeyValueSettingsService;

	void sendMail(final String fromEmail, final String toEmail, final String subject, String body, final boolean isPlainText) {
		sendMail(fromEmail, toEmail, subject, body, isPlainText, null);
	}

	/**
	 * @param fromEmail
	 * 		null esetén tools.mail.sender.senderFromAddress property lesz, 
	 * 		illetve a tools.mail.sender.fromAddress.fixed=true esetén is
	 * @param toEmail
	 * 		";"-vel elválasztva lehet több címet is (max 10, a 10 felettiek ignorálva vannak)
	 * @param subject
	 * @param body
	 * @param isPlainText
	 * 		false estén HTML a body
	 * @param fileIds
	 * 		{@link EmailAttachment}-hez...
	 */
	void sendMail(final String fromEmail, final String toEmail, final String subject, String body, final boolean isPlainText, final String fileIds) {

		try {

			final String[] toEmails = toEmail.contains(";") ? toEmail.split(";") : new String[] { toEmail };

			String localHostName = this.hostName;
			int localSmtpPort = this.smtpPort;
			String localUsername = this.username;
			String localSenderFromAddress = this.senderFromAddress;
			String localPassword = this.password;
			boolean localSslOnConnect = this.sslOnConnect;
			boolean localStartTLSEnabled = this.startTLSEnabled;

			// TODO: tisztázni
			// {
			//
			// // tenant specifikus override lehetőség
			//
			// final TenantKeyValueSettings kvHostName = tenantKeyValueSettingsService.findOneByKey(ToolboxSysKeys.TenantKeyValueSettings.TOOLS_MAIL_SENDER_HOST);
			// final TenantKeyValueSettings kvSmtpPort = tenantKeyValueSettingsService.findOneByKey(ToolboxSysKeys.TenantKeyValueSettings.TOOLS_MAIL_SENDER_PORT);
			// final TenantKeyValueSettings kvUsername = tenantKeyValueSettingsService.findOneByKey(ToolboxSysKeys.TenantKeyValueSettings.TOOLS_MAIL_SENDER_USERNAME);
			// final TenantKeyValueSettings kvSenderFromAddress = tenantKeyValueSettingsService.findOneByKey(ToolboxSysKeys.TenantKeyValueSettings.TOOLS_MAIL_SENDER_FROM_ADDRESS);
			// final TenantKeyValueSettings kvPassword = tenantKeyValueSettingsService.findOneByKey(ToolboxSysKeys.TenantKeyValueSettings.TOOLS_MAIL_SENDER_PASSWORD);
			// final TenantKeyValueSettings kvSslOnConnect = tenantKeyValueSettingsService.findOneByKey(ToolboxSysKeys.TenantKeyValueSettings.TOOLS_MAIL_SENDER_SSL_ON_CONNECT);
			//
			// if (TenantKeyValueSettingsService.checkIfSettingIsValid(kvSenderFromAddress)) {
			// localSenderFromAddress = kvSenderFromAddress.getKvValue();
			// }
			//
			// // ezeknél, csak ha minden beállítás ki volt töltve, akkor cseréljük le az email küldő beállításokat a tenant specifikusra
			//
			// if (TenantKeyValueSettingsService.checkIfSettingsAreValid(kvHostName, kvSmtpPort, kvUsername, kvPassword, kvSslOnConnect)) {
			//
			// localHostName = kvHostName.getKvValue();
			// localSmtpPort = Integer.parseInt(kvSmtpPort.getKvValue());
			// localUsername = kvUsername.getKvValue();
			// localPassword = kvPassword.getKvValue();
			// localSslOnConnect = Boolean.parseBoolean(kvSslOnConnect.getKvValue());
			// localStartTLSEnabled = ... // TODO: a startTLSEnabled akkor került be, amikor ez a TenantKeyValueSettings override lehetőség már ki volt kommentelve... meg kell csinálni TenantKeyValueSettings-ben, ha vissze lesz kommentelve ez a rész
			//
			// }
			//
			// }
			
			int x = 0;
			
			for (final String te : toEmails) {
				
				++x;
				if (x > 10) {
					log.error("Email send skipped (max 10 recipients): " + te);
					break;
				}

				if (te != null && te.toLowerCase().endsWith("@example.com")) {
					log.info("Email send skipped (example.com domain): " + te);
					continue;
				}

				final MultiPartEmail email = new MultiPartEmail();

				email.setHostName(localHostName);
				email.setSmtpPort(localSmtpPort);
				email.setSSLOnConnect(localSslOnConnect);
				email.setStartTLSEnabled(localStartTLSEnabled);
				email.setAuthenticator(new DefaultAuthenticator(localUsername, localPassword));

				if (fileIds != null) {

					final JSONArray fileIdArray = new JSONArray(fileIds);

					for (int i = 0; i < fileIdArray.length(); i++) {

						final FileDescriptor fileDescriptor;

						try {
							FileDescriptorJdbcRepository.setTlAllowCommontTenantFallbackLookup(true); // TODO: ezt még átnézni, megfontolni (secure?)
							fileDescriptor = fileStoreService.getFile2(fileIdArray.getInt(i), true);
						} finally {
							FileDescriptorJdbcRepository.clearTlAllowCommontTenantFallbackLookup();
						}

						EmailAttachment emailAttachment = new EmailAttachment();
						emailAttachment.setPath(fileDescriptor.getFile().getAbsolutePath());

						String name = StringUtils.isNotBlank(fileDescriptor.getMeta1()) ? fileDescriptor.getMeta1() : FilenameUtils.removeExtension(fileDescriptor.getFilename());
						String ext = FilenameUtils.getExtension(fileDescriptor.getFilename());

						name = ToolboxStringUtil.convertToUltraSafe(name, "-");
						ext = ToolboxStringUtil.convertToUltraSafe(ext, "-");

						emailAttachment.setName(name + "." + ext);

						email.attach(emailAttachment);

					}

				}

				if (fromEmail != null && !fixedSenderFromAddress) {
					email.setFrom(fromEmail);
				} else {
					email.setFrom(localSenderFromAddress);
				}

				email.addTo(te);
				email.setSubject(subject);

				// ---

				if (!isPlainText && body.contains(EMAIL_DEFAULT_LOGO_PLACEHOLDER) && StringUtils.isNotBlank(this.logoPath)) {

					try {

						final String cid = UUID.randomUUID().toString();
						final MimeMultipart mimeMultipart = this.attachLogo(cid);

						email.addPart(mimeMultipart);
						body = body.replace(EMAIL_DEFAULT_LOGO_PLACEHOLDER, "<img src=\"cid:" + cid + "\" />");

					} catch (final Exception e) {
						log.warn("Logo addition failed!");
					}

				}

				// ---

				if (StringUtils.isNotBlank(body)) {
					if (isPlainText) {
						email.addPart(body, "text/plain; charset=UTF-8");
					} else {
						email.addPart(body, "text/html; charset=UTF-8");
					}
				}

				email.send();

			}

		} catch (final Exception e) {
			throw new EmailSenderException("Email sending failed!", e);
		}

	}

	private MimeMultipart attachLogo(final String cid) throws IOException, MessagingException {

		final MimeBodyPart imagePart = new MimeBodyPart();

		byte[] ba = null;
		try (InputStream is = new PathMatchingResourcePatternResolver().getResources(this.logoPath)[0].getInputStream()) {
			ba = IOUtils.toByteArray(is);
		}

		final ByteArrayDataSource bds = new ByteArrayDataSource(ba, "AttName");
		imagePart.setDataHandler(new DataHandler(bds));
		imagePart.setFileName("logo." + FilenameUtils.getExtension(this.logoPath));

		imagePart.setContentID("<" + cid + ">");
		imagePart.setDisposition(MimeBodyPart.INLINE);

		final MimeMultipart mimeMultipart = new MimeMultipart("related");
		mimeMultipart.addBodyPart(imagePart);

		return mimeMultipart;

	}

}
