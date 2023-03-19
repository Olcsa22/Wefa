package hu.lanoga.toolbox.holiday;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import hu.lanoga.toolbox.spring.SecurityUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Google publikus calendarból letolti (ICS fajl) DB-be holiday-eket 
 * (állami/hivatalos ünnepek és hétvégi/spec. munkanapok)
 *
 */
@SuppressWarnings("static-method")
@Slf4j
@NoArgsConstructor
@ConditionalOnMissingBean(name = "HolidaySyncSchedulerOverrideBean")
@ConditionalOnProperty({"tools.job-runner", "tools.calendar.holiday-sync.scheduler.enabled"})
@Component
public class HolidaySyncScheduler {
	
	@Autowired
	private HolidayService holidayService;
	
	@PostConstruct
	private void init() {
		log.info("HolidaySyncScheduler initialized.");
	}

	@Scheduled(cron = "${tools.calendar.holiday-sync.scheduler.cronExpression:0 0 0 * * MON}")
	public void syncHolidays() {
		
		log.info("HolidaySyncScheduler...");

		try {
			
			SecurityUtil.setSystemUser();
			
			holidayService.syncHolidays(false);
			
		} catch (final Exception e) {
			log.error("HolidaySyncScheduler error!", e);
		} finally {
			SecurityUtil.clearAuthentication();
		}

	}

}
