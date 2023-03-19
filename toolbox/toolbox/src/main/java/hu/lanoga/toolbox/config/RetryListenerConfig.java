package hu.lanoga.toolbox.config;

import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
// @EnableRetry // nincs bekapcsolva... projekt specifikusan kell!
@Configuration
public class RetryListenerConfig {

	@Bean
	public List<RetryListener> retryListeners() {

		// see https://stackoverflow.com/questions/49066706/spring-retryable-how-to-log-when-it-is-invoked

		return Collections.singletonList(new RetryListener() {

			@Override
			public <T, E extends Throwable> boolean open(final RetryContext context, final RetryCallback<T, E> callback) {

				// The 'context.name' attribute has not been set on the context yet. So we have to use reflection.

				// Field labelField = ReflectionUtils.findField(callback.getClass(), "val$label");
				// ReflectionUtils.makeAccessible(labelField);
				// String label = (String) ReflectionUtils.getField(labelField, callback);
				// log.debug("Retryable method open: " + label);

				return true;
			}

			@Override
			public <T, E extends Throwable> void onError(final RetryContext context, final RetryCallback<T, E> callback, final Throwable throwable) {
				log.debug("Retryable method onError (getRetryCount: " + context.getRetryCount() + "): " + context.getAttribute("context.name"), throwable);
			}

			@Override
			public <T, E extends Throwable> void close(final RetryContext context, final RetryCallback<T, E> callback, final Throwable throwable) {
				// log.debug("Retryable method close (getRetryCount: " + context.getRetryCount() + "): " + context.getAttribute("context.name"));
			}
		});
	}

}
