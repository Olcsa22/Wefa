package hu.lanoga.toolbox.log;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import hu.lanoga.toolbox.spring.SecurityUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// TODO: ezt átnézni (EB)

/**
 * régi log fájlok törlése (a napok száma állítható properties-ből (lásd: tools.log.cleanup.scheduler.to-be-deleted-after))
 */
@SuppressWarnings("static-method")
@Slf4j
@NoArgsConstructor
@ConditionalOnMissingBean(name = "logCleanUpSchedulerOverrideBean")
@ConditionalOnProperty({ "tools.job-runner", "tools.log.cleanup.scheduler.enabled" })
@Component
public class LogCleanUpScheduler {

	@Value("${tools.log.cleanup.scheduler.to-be-deleted-after}")
	private int days;

	@Value("${logging.file.path}")
	private String loggingPath;

	@PostConstruct
	private void init() {
		log.info("LogCleanUpScheduler initialized.");
	}
	
	public static AtomicLong lastExecutionMillis = new AtomicLong(0);

	@Scheduled(cron = "${tools.log.cleanup.scheduler.cronExpression:0 0 0 * * *}")
	private void run() {

		try {

			SecurityUtil.setSystemUser();

			File folder = new File(loggingPath);
			File[] listOfFiles = folder.listFiles();

			for (File file : listOfFiles) {
				
				if (file.isFile()) {

					long diff = System.currentTimeMillis() - file.lastModified();
					long diffLimit = days * 24L * 60L * 60L * 1000L;
					
					if (diff > diffLimit) {
						
						boolean deleted = file.delete();
						
						if (deleted) {
							log.info(file + " is deleted");
						} else {
							log.error(file + " cannot be deleted!");
						}
						
					} else {
						log.debug(file + " is within the time limit");
					}
					
				}
			}

			lastExecutionMillis.set(System.currentTimeMillis());
			
		} catch (final Exception e) {
			log.error("LogCleanUpScheduler error!", e);
		} finally {
			SecurityUtil.clearAuthentication();
		}

	}

}
