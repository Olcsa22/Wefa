package hu.lanoga.toolbox.payment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.tenant.TenantUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
@ConditionalOnMissingBean(name = "paymentTransactionCheckStatusSchedulerOverrideBean")
@ConditionalOnProperty({ "tools.job-runner", "tools.payment.transaction.check-status.scheduler.enabled" })
@Component
public class PaymentTransactionCheckStatusScheduler {

	@Autowired
	private PaymentTransactionService paymentTransactionService;

	@Autowired
	private PaymentManager paymentManager;

	@PostConstruct
	private void init() {
		log.info("PaymentTransactionCheckStatusScheduler initialized.");
	}

	public static AtomicLong lastExecutionMillis = new AtomicLong(0);

	private static final ExecutorService executor = Executors.newFixedThreadPool(1);
	private static AtomicBoolean isItRunningAlready = new AtomicBoolean(false);

	@Scheduled(cron = "${tools.payment.transaction.check-status.scheduler.cronExpression}")
	private void run() {

		if (!isItRunningAlready.get()) {

			executor.execute(() -> {

				try {

					if (!isItRunningAlready.getAndSet(true)) {

						SecurityUtil.setSystemUser();

						TenantUtil.runWithEachTenant(() -> {

							try {

								SecurityUtil.setSystemUser();

								this.paymentManager.checkTransactionStatusList(this.paymentTransactionService.findAllForCheckStatusScheduler());

							} finally {
								SecurityUtil.clearAuthentication();
							}

							return null;
						});

					} else {
						log.warn("PaymentTransactionCheckStatusScheduler() skip (2), job run overlap");
					}

					lastExecutionMillis.set(System.currentTimeMillis());
					
				} catch (final Exception e) {
					log.error("PaymentTransactionCheckStatusScheduler error!", e);
				} finally {
					SecurityUtil.clearAuthentication();
					isItRunningAlready.set(false);
				}

			});

		} else {
			log.warn("PaymentTransactionCheckStatusScheduler() skip (1), job run overlap");
		}

	}
}