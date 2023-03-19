package hu.lanoga.toolbox.payment;

import org.apache.commons.lang3.StringUtils;

import hu.lanoga.toolbox.util.ToolboxAssert;

/**
 * {@link PaymentTransactionRawLogService} + normál logolás egyben
 */
public class PaymentTransactionRawLogger {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractPaymentProviderService.class);

	private boolean debugLevelPersistToDb;
	private String className;

	private PaymentTransactionRawLogService paymentRawLogService;

	private ThreadLocal<Integer> tlPaymentTransactionId = new ThreadLocal<>();

	@SuppressWarnings("hiding")
	void init(final String className, final boolean debugLevelPersistToDb, final PaymentTransactionRawLogService paymentRawLogService) {
		this.className = className;
		this.debugLevelPersistToDb = debugLevelPersistToDb;
		this.paymentRawLogService = paymentRawLogService;
	}

	public Integer getPaymentTransactionId() {
		return this.tlPaymentTransactionId.get();
	}

	public void setPaymentTransactionId(final Integer paymentTransactionId) {
		this.tlPaymentTransactionId.set(paymentTransactionId);
	}

	/**
	 * @param msg
	 * 		max. kb. 1900 char hossz (levágja egyébként)
	 * @param t
	 * @param persistToDb
	 * @return
	 */
	private String enhanceAndPersistInDb(final String msg, final Throwable t, final boolean persistToDb) {

		final Integer paymentTransactionId = this.tlPaymentTransactionId.get();

		ToolboxAssert.notNull(paymentTransactionId);

		final StringBuilder sb = new StringBuilder("[provider: ");
		sb.append(this.className);
		sb.append(", ptid: ");
		sb.append(paymentTransactionId);
		sb.append("] ");
		sb.append(msg);

		final StringBuilder sb2 = new StringBuilder(sb);

		if (persistToDb) {

			try {

				if (t != null) {
					sb.append(" [");
					sb.append(t.getMessage());
					sb.append("] ");
				}

				final PaymentTransactionRawLog paymentRawLog = this.paymentRawLogService.save(new PaymentTransactionRawLog(paymentTransactionId,
						StringUtils.abbreviate(sb.toString(), 2000)));
				sb2.append(" [rawLogId: ");
				sb2.append(paymentRawLog.getId());
				sb2.append("]");

			} catch (final Exception e) {
				log.error("PaymentTransactionRawLog persist error!", e);
			}

		}

		return sb2.toString();
	}

	public void trace(final String msg) {
		log.trace(this.enhanceAndPersistInDb(msg, null, false));
	}

	public void trace(final String msg, final Throwable t) {
		log.trace(this.enhanceAndPersistInDb(msg, t, false), t);
	}

	public void debug(final String msg) {
		log.debug(this.enhanceAndPersistInDb(msg, null, this.debugLevelPersistToDb));
	}

	public void debug(final String msg, final Throwable t) {
		log.debug(this.enhanceAndPersistInDb(msg, t, this.debugLevelPersistToDb), t);
	}

	public void info(final String msg) {
		log.info(this.enhanceAndPersistInDb(msg, null, true));
	}

	public void info(final String msg, final Throwable t) {
		log.info(this.enhanceAndPersistInDb(msg, t, true), t);
	}

	public void warn(final String msg) {
		log.warn(this.enhanceAndPersistInDb(msg, null, true));
	}

	public void warn(final String msg, final Throwable t) {
		log.warn(this.enhanceAndPersistInDb(msg, t, true), t);
	}

	public void error(final String msg) {
		log.error(this.enhanceAndPersistInDb(msg, null, true));
	}

	public void error(final String msg, final Throwable t) {
		log.error(this.enhanceAndPersistInDb(msg, t, true), t);
	}

}
