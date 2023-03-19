package hu.lanoga.toolbox.config;

import javax.annotation.PreDestroy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mashape.unirest.http.Unirest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class UnirestConfig {

	public static class UnirestConfigShutdownHookBean {

		@PreDestroy
		public void destroy() {

			try {

				// Unirest starts a background event loop and your Java application won't be able to exit until you manually shutdown all the threads by invoking...

				Unirest.shutdown();

			} catch (final Exception e) {
				log.warn("Unirest.shutdown error", e);
			}

		}
	}

	@Bean
	public UnirestConfigShutdownHookBean unirestConfigShutdownHookBean() {
		return new UnirestConfigShutdownHookBean();
	}
}