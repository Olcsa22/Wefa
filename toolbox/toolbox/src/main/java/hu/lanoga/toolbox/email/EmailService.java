package hu.lanoga.toolbox.email;

import java.util.List;

import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.PictureHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ConditionalOnMissingBean(name = "emailServiceOverrideBean")
@Service
public class EmailService implements ToolboxEmailService {
	
	@Value("${tools.mail.sender.fromAddress}")
	private String senderFromAddress;

	@Autowired
	private EmailJdbcRepository emailJdbcRepository;

	/**
	 * {@link EmailSenderScheduler}-hez kell, ebben a formában API-ról ne legyen elérhető (vagy alaposan át kell nézni/gondolni)!
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	@Transactional // kell mert rögtön update is van...
	public List<ToolboxEmail> findAllSendableEmail() {

		final List<Email> emails = this.emailJdbcRepository.findAllSendableEmail();

		if (!emails.isEmpty()) {
			final int lastId = emails.get(emails.size() - 1).getId(); // így gyengébb tranzakció izolációs szinten sem lesz probléma
			this.emailJdbcRepository.updateSendableEmailStatusToPending(lastId);
		}
		
		return (List) emails; // Email implmentálja a ToolboxEmail interface-t, ezért ez működik itt
	}

	/**
	 * {@link EmailSenderScheduler}-hez kell, ebben a formában API-ról ne legyen elérhető (vagy alaposan át kell nézni/gondolni)!
	 * 	
	 * @throws EmailSendAttemptCountMistmatchException
	 */
	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	@Transactional
	public void incrementAttempt(final int id, final int currentAttemptCount) throws EmailSendAttemptCountMistmatchException {
		this.emailJdbcRepository.incrementAttempt(id, currentAttemptCount);
	}

	/**
	 * {@link EmailSenderScheduler}-hez kell, ebben a formában API-ról ne legyen elérhető (vagy alaposan át kell nézni/gondolni)!
	 */
	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	@Transactional
	public void updateStatus(final int id, final int status, final String errorMessage) {
		this.emailJdbcRepository.updateStatus(id, status, errorMessage);
	}
		
	/**
	 * szimpla (nem template alapú) email küldése
	 * 
	 * @param toEmail
	 * @param subject
	 * @param body
	 * @param isPlainText
	 * 
	 * @see EmailTemplateService#addMail(hu.lanoga.toolbox.spring.ToolboxUserDetails, String, int, java.util.Map, String)
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	@Transactional
	public void addMail(final String toEmail, final String subject, final String body, final Boolean isPlainText) {
		this.addMail(toEmail, subject, body, isPlainText, ToolboxSysKeys.EmailStatus.CREATED, null);
	}
	
	/**
	 * szimpla (nem template alapú) email küldése
	 * 
	 * @param toEmail
	 * @param subject
	 * @param body
	 * @param isPlainText
	 * @param fileIds
	 * 
	 * @see EmailTemplateService#addMail(hu.lanoga.toolbox.spring.ToolboxUserDetails, String, int, java.util.Map, String)
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	@Transactional
	public void addMail(final String toEmail, final String subject, final String body, final Boolean isPlainText, final String fileIds) {
		this.addMail(toEmail, subject, body, isPlainText, ToolboxSysKeys.EmailStatus.CREATED, fileIds);
	}
	
	/**
	 * szimpla (nem template alapú) email küldése
	 *
	 * @param toEmail
	 * @param subject
	 * @param body
	 * @param isPlainText
	 * @param initialStatus
	 *
	 * @see EmailTemplateService#addMail(hu.lanoga.toolbox.spring.ToolboxUserDetails, String, int, java.util.Map, String)
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	@Transactional
	public Email addMail(final String toEmail, final String subject, final String body, final Boolean isPlainText, final Integer initialStatus) {
		return this.addMail(toEmail, subject, body, isPlainText, initialStatus, null);
	}

	/**
	 * szimpla (nem template alapú) email küldése
	 *
	 * @param toEmail
	 * @param subject
	 * @param body
	 * @param isPlainText
	 * @param initialStatus
	 * @param fileIds
	 *
	 * @see EmailTemplateService#addMail(hu.lanoga.toolbox.spring.ToolboxUserDetails, String, int, java.util.Map, String)
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	@Transactional
	public Email addMail(final String toEmail, final String subject, final String body, final Boolean isPlainText, final Integer initialStatus, final String fileIds) {

		final Email email = new Email();
		email.setFromEmail(this.senderFromAddress);
		email.setToEmail(toEmail);
		email.setSubject(subject);
		email.setBody(body);
		email.setAttempt(0);
		email.setStatus(initialStatus);
		email.setIsPlainText(isPlainText);
		email.setFileIds(fileIds);

		log.debug("addMail");

		return this.emailJdbcRepository.save(email);

	}

	public Email findOne(final Integer id) {

		SecurityUtil.limitAccessSystem();

		return this.emailJdbcRepository.findOne(id);
	}

	public Email save(final Email email) {

		SecurityUtil.limitAccessSystem();

		return this.emailJdbcRepository.save(email);
	}
	
	public void sentTestEmail(final String toEmail) {
		
		SecurityUtil.limitAccessSystem();
		
		final FileDescriptor dummyPngPictFd = PictureHelper.generateDummyPngPict();

		this.addMail(toEmail, "test", "test", true, new JSONArray().put(dummyPngPictFd.getId()).toString());
		
	}

}
