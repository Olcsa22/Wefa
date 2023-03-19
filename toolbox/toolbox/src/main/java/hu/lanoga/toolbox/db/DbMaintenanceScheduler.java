package hu.lanoga.toolbox.db;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import hu.lanoga.toolbox.spring.SecurityUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * postgres REINDEX-et hív
 */
@Component
@NoArgsConstructor
@ConditionalOnMissingBean(name = "dbMaintenanceSchedulerOverrideBean")
@ConditionalOnProperty({ "tools.job-runner", "tools.db.maintenance.scheduler.enabled" })
@Slf4j
public class DbMaintenanceScheduler {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Value("${datasource.database-name}")
	private String databaseName;
	
	public static AtomicLong lastExecutionMillis = new AtomicLong(0);

	@Scheduled(cron = "${tools.db.maintenance.scheduler.cronExpression}")
	public void maintenance() {
		
		// TODO: tesztelni, hogy tényleg ér-e valamit... nem ront-e el semmit stb.

		log.info("DB maintenance started.");

		try {

			this.jdbcTemplate.execute("REINDEX DATABASE " + databaseName + ";");
			this.jdbcTemplate.execute("REINDEX SYSTEM " + databaseName + ";");
			
			log.info("DB maintenance finished.");
			
			lastExecutionMillis.set(System.currentTimeMillis());

		} catch (final Exception e) {
			log.error("DbMaintenanceScheduler error.", e);
		} finally {
			SecurityUtil.clearAuthentication();
		}

	}

}
