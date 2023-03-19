package hu.lanoga.toolbox.quickcontact;

import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.tenant.TenantUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * email notification bejövő quick contact elemekről...
 */
@Slf4j
@NoArgsConstructor
@ConditionalOnMissingBean(name = "quickContactSchedulerOverrideBean")
@ConditionalOnProperty({"tools.job-runner", "tools.quick-contact.scheduler.enabled"})
@Component
public class QuickContactScheduler {

	@Autowired
	private QuickContactService quickContactService;

	@PostConstruct
	private void init() {
		log.info("QuickContactScheduler initialized.");
	}

	@Scheduled(cron = "${tools.quick-contact.scheduler.cronExpression}")
	private void run() {

		try {
			log.info("QuickContactScheduler started.");
			SecurityUtil.setSystemUser();

			TenantUtil.runWithEachTenant(() -> {
				this.quickContactService.sendQuickContactEmailNotifications();
				return null;
			});

			log.info("QuickContactScheduler finished.");

		} catch (final Exception e) {
			log.error("QuickContactScheduler error!", e);
		} finally {
			SecurityUtil.clearAuthentication();
		}

	}

}