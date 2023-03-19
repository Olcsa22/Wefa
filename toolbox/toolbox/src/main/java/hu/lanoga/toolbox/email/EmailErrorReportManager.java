package hu.lanoga.toolbox.email;

import static hu.lanoga.toolbox.spring.ApplicationContextHelper.hasDevProfile;

import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import hu.lanoga.toolbox.spring.SecurityUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ConditionalOnMissingBean(name = "emailErrorReportManagerOverrideBean")
@ConditionalOnProperty(name = "tools.mail.error-report.enabled")
@Component
public class EmailErrorReportManager {

	// TODO: erre nem vontakozik tools.job-runner beállítás!

	@Value("${tools.mail.error-report.receiver}")
	private String receivers;

	@Value("${tools.mail.error-report.subject}")
	private String subject;

	@Value("${tools.misc.application-name}")
	private String applicationName;

	@Autowired
	private EmailSender emailSender;

	private final ExecutorService exService1 = Executors.newSingleThreadExecutor();

	private ScheduledExecutorService exService2;

	@PostConstruct
	public void init() {
		exService2 = Executors.newScheduledThreadPool(1);
		exService2.scheduleWithFixedDelay(() -> handleLogMail(false) /* itt már eleve background thread van... */, 5L, 15L, TimeUnit.MINUTES);
	}

	private void sendMail(final String toAddress, final String subjectStr, final String content, final boolean runInBackground) {
		final Runnable r = () -> {

			try {

				emailSender.sendMail(null, toAddress, subjectStr, content, false);

				log.info("EmailErrorReportManager sendMail (OK)");

			} catch (final Exception e) {
				log.error("EmailErrorReportManager sendMail (FAIL)", e);
			}

		};

		if (runInBackground) {
			exService1.submit(r);
		} else {
			r.run();
		}

	}

	private ConcurrentLinkedQueue<String> logMailQueue = new ConcurrentLinkedQueue<>();
	private long lastLogMailSend = 0L;

	private synchronized void handleLogMail(final boolean sendInBackground) {

		try {

			log.debug("handleLogMail (sendInBackground later: " + sendInBackground + ")");

			final long currentTimeMillis = System.currentTimeMillis();

			if ((currentTimeMillis - lastLogMailSend) > (60L * 60L * 1000L)) {

				lastLogMailSend = currentTimeMillis;

				// ---

				final StringBuilder sb = new StringBuilder();
				final StringBuilder sbSubject = new StringBuilder();

				int i = 0;
				String polled = logMailQueue.poll();

				sb.append("<b>");
				sbSubject.append(subject);
				sbSubject.append(" - application: ");

				String appName = applicationName;
				if (hasDevProfile()) {
					appName = applicationName + " (dev)";
				}

				sbSubject.append(appName);
				sbSubject.append(" - date: ");
				sbSubject.append(new Date());

				sbSubject.append(" - mac address: ");
				
				String physicalAddressHex = SecurityUtil.getPhysicalAddressHex();
				sbSubject.append(physicalAddressHex != null ? physicalAddressHex : "?");

				sb.append(sbSubject);
				sb.append("</b><br><br><hr><br>");

				while ((polled != null) && (i < 10)) { // max 10 log elemet küldünk el

					sb.append(polled.replaceAll("(\r\n|\n)", "<br>"));
					sb.append("<br>");
					sb.append("<hr>");
					sb.append("<br>");
					polled = logMailQueue.poll();
					i = i + 1;
				}

				logMailQueue.clear();

				if (i == 10) {
					sb.append("(email limit reached (10), see the server (ie.: Actuator) for more log elements)");
				}

				if (i > 0) {

					String[] receiversArray = receivers.split(",");

					for (String receiver : receiversArray) {
						sendMail(receiver, sbSubject.toString(), sb.toString(), sendInBackground);
					}

				}

			} else {
				log.debug("handleLogMail wait (using logMailQueue)");
			}

		} catch (final Exception e) {
			log.error("handleLogMail error", e);
		}
	}

	/**
	 * logMailQueue-hoz hozzáadja... soha nem dob exception-t
	 *
	 * @param msg
	 * @param throwable
	 */
	public void addLogMail(final String msg, final Throwable throwable) {
		try {

			if (logMailQueue.size() < 20) {
				logMailQueue.add(msg + ":<br><br>" + ExceptionUtils.getStackTrace(throwable));
			}

			handleLogMail(true);

		} catch (Exception e) {
			log.error("addLogMail error", e);
		}
	}

}