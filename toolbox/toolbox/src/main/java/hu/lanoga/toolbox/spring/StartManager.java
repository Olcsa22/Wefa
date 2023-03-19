package hu.lanoga.toolbox.spring;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.teamunify.i18n.I;

import hu.lanoga.toolbox.util.ToolboxAssert;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Boot start helper... "user.timezone" -> "UTC" állít be...
 * 
 * @see LetsEncryptUtil
 */
@Slf4j
public class StartManager {

	private StartManager() {
		//
	}

	private static String[] originalArgs;
	private static ConfigurableApplicationContext applicationContext;
	private static Class<?> springBootApplicationAnnotatedClass;
	
	public static AtomicLong startedOnMillis = new AtomicLong(0);

	public static void restart() {

		log.info("Restart...");

		ToolboxAssert.notNull(applicationContext);
		ToolboxAssert.notNull(originalArgs);

		Thread restartThread = new Thread(() -> {

			log.info("applicationContext.close()");

			applicationContext.close();

			start(springBootApplicationAnnotatedClass, originalArgs);

		});

		restartThread.setDaemon(false);
		restartThread.start();

	}

	public static ConfigurableApplicationContext start(@SuppressWarnings("hiding") Class<?> springBootApplicationAnnotatedClass, final String[] args) {

		// log.info("SpringApplication start...");

		StartManager.originalArgs = args;
		StartManager.springBootApplicationAnnotatedClass = springBootApplicationAnnotatedClass;
		
		Locale.setDefault(Locale.US);

		System.setProperty("user.timezone", "UTC"); // ez a server időzóna (nem a belépett user default-ja!)
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		
		String iInitLogStr = I.init();
		
		// LetsEncryptUtil.aquireAndSetCert(); // TODO: kiiktatva, mert nem használjuk sehol
		
		applicationContext = SpringApplication.run(springBootApplicationAnnotatedClass, args);
				
		// ---
		
		try {
			FileUtils.forceMkdir(new File(applicationContext.getEnvironment().getProperty("spring.servlet.multipart.location")));
		} catch (IOException e) {
			log.error("spring.servlet.multipart.location folder mkdir error", e);
		}
				
		// ---
		
		log.info(iInitLogStr);
		
		// ---
		
		log.info("SpringApplication started.");
		
		startedOnMillis.set(System.currentTimeMillis());
		
		return applicationContext;

	}
}
