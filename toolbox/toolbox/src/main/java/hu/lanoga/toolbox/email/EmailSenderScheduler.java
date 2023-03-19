package hu.lanoga.toolbox.email;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.file.FileDescriptorJdbcRepository;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.tenant.TenantUtil;
import hu.lanoga.toolbox.util.JobUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @see ToolboxEmail
 * @see ToolboxEmailService
 * @see EmailSender
 */
@Slf4j
@NoArgsConstructor
@ConditionalOnMissingBean(name = "emailSenderSchedulerOverrideBean")
@ConditionalOnProperty({ "tools.job-runner", "tools.mail.sender.scheduler.enabled" })
@Component
public final class EmailSenderScheduler {

	public static AtomicLong lastExecutionMillis = new AtomicLong(0);

	@Autowired
	private ToolboxEmailService emailService;

	@Autowired
	private EmailSender emailSender;

	@Value("${tools.mail.sender.scheduler.sub-threads}")
	private int nThreads;

	@Value("${tools.mail.sender.scheduler.health-check.delay}")
	private long healthCheckDelay;

	private ExecutorService executor;
	
	@SuppressWarnings("unused")
	private ScheduledExecutorService executorHealthCheck;

	@PostConstruct
	private void init() {

		this.executor = Executors.newFixedThreadPool(nThreads);

		this.executorHealthCheck = JobUtil.buildJobHealthChecker(healthCheckDelay, lastExecutionMillis, "EmailSenderScheduler", true);

		log.info("EmailSenderScheduler initialized.");
		
	}
	
	private class EmailSenderTask implements Runnable {

		private final int tenantId;
		private final ToolboxEmail email;

		public EmailSenderTask(final int tenantId, final ToolboxEmail email) {
			this.tenantId = tenantId;
			this.email = email;
		}

		@Override
		public void run() {

			try {

				SecurityUtil.setSystemUser();
				JdbcRepositoryManager.setTlTenantId(tenantId);
				FileDescriptorJdbcRepository.setTlAllowCommontTenantFallbackLookup(true);

				log.debug("EmailSenderScheduler (subthread) sending... emailId: " + email.getId());

				emailService.incrementAttempt(email.getId(), email.getAttempt());

				emailSender.sendMail(email.getFromEmail(), email.getToEmail(), email.getSubject(), email.getBody(), email.getIsPlainText(), email.getFileIds());

				emailService.updateStatus(email.getId(), ToolboxSysKeys.EmailStatus.SENT, null);

				log.debug("EmailSenderScheduler (subthread) sending success, emailId: " + email.getId());

			} catch (final EmailSendAttemptCountMistmatchException e) {

				// ilyen csak akkor lehet, ha a scheduler-nél több szál, "ráfutás" van... (fontos, hogy maga a scheduler egy szálas legyen... a küldő task-ok lehetnek több szálon)

				log.warn("EmailSenderScheduler (subthread) sending failed (email send attempt count mistmatch)!", e);

			} catch (final Exception e) {

				log.error("EmailSenderScheduler (subthread) sending failed!", e);

				try {

					emailService.updateStatus(email.getId(), ToolboxSysKeys.EmailStatus.ERROR, e.getMessage());

				} catch (Exception e2) {
					log.error("EmailSenderScheduler (subthread) sending failed (record status update failed too)!", e2);
				}

			} finally {

				SecurityUtil.clearAuthentication();
				JdbcRepositoryManager.clearTlTenantId();
				FileDescriptorJdbcRepository.clearTlAllowCommontTenantFallbackLookup();

			}

		}

	}

	@Scheduled(cron = "${tools.mail.sender.scheduler.cronExpression:*/20 * * * * *}")
	private void scheduledEmailSending() {

		// fontos, hogy maga a scheduler fix egy szálas legyen

		try {

			SecurityUtil.setSystemUser();

			TenantUtil.runWithEachTenant(() -> {

				final List<ToolboxEmail> emails = emailService.findAllSendableEmail();

				for (final ToolboxEmail email : emails) {
					executor.execute(new EmailSenderTask(JdbcRepositoryManager.getTlTenantId(), email)); // itt már lehet több szál (de limitált, lásd pl. Gmail párhuzamos SMTP kapcsolat limit...)
				}

				return null;

			});
			
			lastExecutionMillis.set(System.currentTimeMillis());

		} finally {
			SecurityUtil.clearAuthentication();
		}

	}
}
