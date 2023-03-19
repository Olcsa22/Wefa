package hu.lanoga.toolbox.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import hu.lanoga.toolbox.email.EmailErrorReportManager;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobUtil {

	private JobUtil() {
		//
	}

	/**
	 * @param healthCheckDelay
	 * 		milliseconds
	 * @param jobLastExecutionMillisHolder
	 * @param watchedSchedulerNameForLog
	 * @param sendLogEmail
	 * @return
	 */
	public static ScheduledExecutorService buildJobHealthChecker(long healthCheckDelay, AtomicLong jobLastExecutionMillisHolder, String watchedSchedulerNameForLog, boolean sendLogEmail) {
		
		ScheduledExecutorService executorHealthCheck = Executors.newScheduledThreadPool(1);
		executorHealthCheck.scheduleWithFixedDelay(() -> {

			try {

				if ((System.currentTimeMillis() - jobLastExecutionMillisHolder.get()) > healthCheckDelay) {

					final String errorStr = watchedSchedulerNameForLog + ": executorHealthCheck() fail! The watched scheduler seemingly stopped (or slowed down extremely)!";

					log.error(errorStr);

					if (sendLogEmail) {
						try {
							ApplicationContextHelper.getBean(EmailErrorReportManager.class).addLogMail(errorStr, new RuntimeException(errorStr));
						} catch (final org.springframework.beans.factory.NoSuchBeanDefinitionException e2) {
							log.debug("EmailErrorReportManager missing (log email skipped...)!");
						} catch (Exception e) {
							log.error("EmailErrorReportManager addLogMail() failed!", e);
						}
					}

				} else {
					log.info(watchedSchedulerNameForLog + ": executorHealthCheck() passed! scheduledEmailSending() was executed recently (as designed).");
				}

			} catch (Exception e) {
				log.error("executorHealthCheck() failed!", e);
			}

		}, healthCheckDelay, healthCheckDelay, TimeUnit.MILLISECONDS);
		
		return executorHealthCheck;
	}

}
