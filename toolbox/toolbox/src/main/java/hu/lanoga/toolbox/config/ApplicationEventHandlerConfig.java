package hu.lanoga.toolbox.config;

import java.util.Properties;

import org.apache.velocity.app.Velocity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import hu.lanoga.toolbox.holiday.HolidayService;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.tenant.TenantService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ApplicationEventHandlerConfig {

	@Value("${tools.calendar.holiday-sync.sync-on-start}")
	private boolean holidaySyncSyncOnStart;

	@EventListener(ApplicationReadyEvent.class)
	private void applicationReadyEventHandler() {

		try {

			SecurityUtil.setSystemUser();

			// ---

			ApplicationContextHelper.getBean(TenantService.class).init();

			// ---

			if (this.holidaySyncSyncOnStart) {
				ApplicationContextHelper.getBean(HolidayService.class).syncHolidays(true);
			}

			// ---

			final Properties velocityProp = new Properties();
			velocityProp.setProperty("resource.loader", "class");
			velocityProp.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

			Velocity.init(velocityProp);

		} catch (final Exception e) {
			log.error("applicationReadyEventHandler error!", e);
		} finally {
			SecurityUtil.clearAuthentication();
		}

	}

}
