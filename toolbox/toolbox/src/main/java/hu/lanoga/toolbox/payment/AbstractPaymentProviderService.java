package hu.lanoga.toolbox.payment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.access.annotation.Secured;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.FileOperationAccessTypeIntent;
import hu.lanoga.toolbox.codestore.CodeStoreItem;
import hu.lanoga.toolbox.codestore.CodeStoreItemService;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileDescriptorJdbcRepository;
import hu.lanoga.toolbox.file.FileStoreHelper;
import hu.lanoga.toolbox.json.jackson.JacksonHelper;
import hu.lanoga.toolbox.payment.exception.ToolboxPaymentGeneralException;
import hu.lanoga.toolbox.payment.exception.ToolboxPaymentValidationException;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.util.ToolboxAssert;
import lombok.extern.slf4j.Slf4j;

/**
 * payment provider-ek kapcsán service, 
 *
 * ez nem CRUD, hanem jellemezően API hívásokat tartalmaz (ez hívja a PayPal stb. szerverét)... 
 *
 * exception kezelés így kell legyen: 
 * {@link ToolboxPaymentValidationException} dobandó validációs jellegű ellenőrzések/hibák kapcsán,
 * egyéb checked vagy unchecked (runtime) exception-ökkel nem kell foglalkozni {@link PaymentManager} elkapja, logolja őket
 * (mj.: try-with-resources / finally block, vagy egyéb resource close kell, az a konkrét implementáló osztály felelőssége!)
 */
@Slf4j
abstract public class AbstractPaymentProviderService {

	@Autowired
	protected CodeStoreItemService codeStoreItemService;

	@Autowired
	protected PaymentConfigService paymentConfigService;

	/**
	 * használd ezt lombok sl4j helyett, ez {@link PaymentTransactionRawLogService} + normál logolás egyben
	 *
	 * (alapban az info, warn, error level lesz a {@link PaymentTransactionRawLogService} révén mentve,
	 * ha "unit-test" vagy "dev" mód van bekapcsolva, akkor pluszban a debug level is (trace level soha))
	 */
	protected PaymentTransactionRawLogger paymentTransactionRawLogger = new PaymentTransactionRawLogger();

	@Autowired
	protected PaymentTransactionRawLogService paymentTransactionRawLogService;

	@Autowired
	protected PaymentTransactionService paymentTransactionService;

	@Autowired
	protected Environment environment;
	
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public PaymentTransaction checkTransactionStatus(PaymentTransaction paymentTransaction, final boolean makeRequest) {
		return checkTransactionStatus(paymentTransaction, makeRequest, false);
	}

	/**
	 * @param paymentTransaction
	 * 
	 * @param makeRequest
	 * 		történjen-e most új API/SDK hívás, vagy elég csak a saját DB-ből betölteni; 
	 * 		false esetén a {@link PaymentTransactionStatusChangeNotificationReceiver} objektumok értesítése 
	 * 		is ki lesz hagyva!
	 * @return
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public PaymentTransaction checkTransactionStatus(PaymentTransaction paymentTransaction, final boolean makeRequest, final boolean forceRequest) {

		if (paymentTransaction.isNew()) {
			log.error("Invalid id (only existing transaction is allowed here)!");
			throw new ToolboxPaymentValidationException("Invalid id (only existing transaction is allowed here)!");
		}

		try {

			this.paymentTransactionRawLogger.setPaymentTransactionId(paymentTransaction.getId());

			if (paymentTransaction.getStatus() != null && paymentTransaction.getStatus() > ToolboxSysKeys.PaymentTransactionStatus.RECEIVED && !forceRequest) {
				this.paymentTransactionRawLogger.info("skip check status (already reached final status)");
				return paymentTransaction;
			}

			paymentTransaction = this.paymentTransactionService.findOne(paymentTransaction.getId());

			if (!makeRequest) {
				return paymentTransaction;
			}

			// ---

			final Integer oldStatus = paymentTransaction.getStatus();

			paymentTransaction = this.checkTransactionStatusInner(paymentTransaction);
			
			if (!paymentTransaction.getStatus().equals(oldStatus)) {
				callPaymentTransactioNStatusChangeNotificationReceivers(paymentTransaction);
			}

		} catch (final Exception e) {

			final RuntimeException toolboxPaymentException = new ToolboxPaymentGeneralException("payment checkStatus error!", e);
			this.paymentTransactionRawLogger.error(toolboxPaymentException.getMessage(), toolboxPaymentException);
			throw toolboxPaymentException;

		} finally {

			try {
				if (makeRequest) {
					paymentTransaction = this.perisistPaymentTransaction(paymentTransaction);
				}
			} finally {
				this.paymentTransactionRawLogger.setPaymentTransactionId(null);
			}
			

		}

		return paymentTransaction;

	}

	protected final void callPaymentTransactioNStatusChangeNotificationReceivers(PaymentTransaction paymentTransaction) {
		
		try {
			
			paymentTransaction = this.perisistPaymentTransaction(paymentTransaction);
			
			// ---

			final Map<String, PaymentTransactionStatusChangeNotificationReceiver> beans = ApplicationContextHelper.getBeans(PaymentTransactionStatusChangeNotificationReceiver.class);

			Boolean b = null;

			if (paymentTransaction.getStatus().equals(ToolboxSysKeys.PaymentTransactionStatus.SUCCESS) || paymentTransaction.getStatus().equals(ToolboxSysKeys.PaymentTransactionStatus.RECEIVED)) {
				b = true;
			} else if (paymentTransaction.getStatus().equals(ToolboxSysKeys.PaymentTransactionStatus.FAILED) || paymentTransaction.getStatus().equals(ToolboxSysKeys.PaymentTransactionStatus.USER_CANCELED)) {
				b = false;
			}

			for (final PaymentTransactionStatusChangeNotificationReceiver bean : beans.values()) {

				final PaymentTransaction paymentTransactionForNotif = new PaymentTransaction();
				BeanUtils.copyProperties(paymentTransactionForNotif, paymentTransaction); // mj. ez nem deep copy, de itt jó (mert a PaymentTransaction egy "sima" model, nincs mélyebb része)

				bean.transactionStatusChanged(paymentTransactionForNotif, b);
			}

		} catch (final BeansException e) {
			log.debug("no payment transaction notif receiver, skip!", e);
		} catch (Exception e) {
			throw new ToolboxPaymentGeneralException("callPaymentTransactioNStatusChangeNotificationReceivers error", e);
		}
		
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public PaymentTransaction doTransaction(PaymentTransaction paymentTransaction) {

		if (!paymentTransaction.isNew()) {
			log.error("Invalid id (only new transaction is allowed here)!");
			throw new ToolboxPaymentValidationException("Invalid id (only new transaction is allowed here)!");
		}

		try {

			if (paymentTransaction.getOriginalAmount() == null || paymentTransaction.getOriginalAmount().compareTo(new BigDecimal(0)) < 0) {
				throw new ToolboxPaymentValidationException("Missing/invalid amount!");
			}

			if (StringUtils.isBlank(paymentTransaction.getOriginalCurrency()) || paymentTransaction.getOriginalCurrency().length() != 3) {
				throw new ToolboxPaymentValidationException("Missing/invalid currency!");
			}

			if (StringUtils.isBlank(paymentTransaction.getOrderInfoLabel())) {
				throw new ToolboxPaymentValidationException("Missing/invalid orderInfoLabel!");
			}

			final Integer paymentOperationType = paymentTransaction.getPaymentOperationType();

			if (paymentOperationType == null) {
				throw new ToolboxPaymentValidationException("Missing operation type!");
			}

			final boolean isCaptureOrRefund = paymentOperationType.equals(ToolboxSysKeys.PaymentOperationType.CAPTURE) || paymentOperationType.equals(ToolboxSysKeys.PaymentOperationType.REFUND);

			if (paymentTransaction.getReferredTransactionId() == null
					&& isCaptureOrRefund) {
				throw new ToolboxPaymentValidationException("Referred transaction id is null (it is needed for operation: " + paymentOperationType + ")!");
			}

			if (isCaptureOrRefund) {
				SecurityUtil.limitAccessRoleUser();
				SecurityUtil.limitAccessDisabled(true);
			}

		} catch (final Exception e) {

			final boolean isValidationError = (e instanceof ToolboxPaymentValidationException);
			final RuntimeException toolboxPaymentException = isValidationError ? new ToolboxPaymentValidationException("payment operation validation error!", e) : new ToolboxPaymentGeneralException("payment operation other error!", e);
			this.paymentTransactionRawLogger.error(toolboxPaymentException.getMessage(), toolboxPaymentException);
			throw toolboxPaymentException;

		}

		// ---

		paymentTransaction.setStatus(ToolboxSysKeys.PaymentTransactionStatus.GATEWAY_INIT);

		final String walletFullToken = paymentTransaction.getWalletFullToken(); // @View mező... ha nem null, akkor vissza kell setelni
		paymentTransaction = this.perisistPaymentTransaction(paymentTransaction);
		paymentTransaction.setWalletFullToken(walletFullToken);

		try {

			this.paymentTransactionRawLogger.setPaymentTransactionId(paymentTransaction.getId());

			paymentTransaction = this.doTransactionInner(paymentTransaction);

			ToolboxAssert.isTrue(StringUtils.isNotBlank(paymentTransaction.getRawId()), "PaymentTransaction does not have a raw id (it is likely a payment API fail/outage)!"); // eddigre már kell legyen egy rawId biztosan
			ToolboxAssert.isTrue(!paymentTransaction.getStatus().equals(ToolboxSysKeys.PaymentTransactionStatus.GATEWAY_INIT)); // eddigre már legalább egyet előre kellett lépnie a folyamatnak

		} catch (final Exception e) {

			final boolean isValidationError = (e instanceof ToolboxPaymentValidationException);

			paymentTransaction.setStatus(ToolboxSysKeys.PaymentTransactionStatus.FAILED); // TODO: FAILED_OR_UNKNOWN inkább itt

			final RuntimeException toolboxPaymentException = isValidationError ? new ToolboxPaymentValidationException("payment operation validation error!", e) : new ToolboxPaymentGeneralException("payment operation other error!", e);

			this.paymentTransactionRawLogger.error(toolboxPaymentException.getMessage(), toolboxPaymentException);

			throw toolboxPaymentException;

		} finally {

			try {
				paymentTransaction = this.perisistPaymentTransaction(paymentTransaction);
			} finally {
				this.paymentTransactionRawLogger.setPaymentTransactionId(null);
			}

		}

		return paymentTransaction;
	}

	public CodeStoreItem getPaymentProviderCodeStoreItem() {
		return this.codeStoreItemService.findOne(this.getPaymentProviderCodeStoreItemId());
	}

	abstract public int getPaymentProviderCodeStoreItemId();

	abstract public boolean hasAuthFeature();

	abstract public boolean hasCaptureFeature();

	abstract public boolean hasPurchaseFeature();

	abstract public boolean hasRefundFeature();

	abstract public boolean hasVoidFeature();

	abstract public boolean hasWithdrawFeature();
	
	abstract public boolean hasServerToServerNotificationProcessor();
	abstract public boolean hasReturnUrlProcessor();

	@PostConstruct
	public void init() {
		this.paymentTransactionRawLogger.init(this.getClass().getSimpleName(), ApplicationContextHelper.hasDevOrUnitTestProfile(this.environment), this.paymentTransactionRawLogService);
	}

	public boolean isViableForCapture(final PaymentTransaction paymentTransaction) {
		try {

			if (!this.hasCaptureFeature()) {
				return false;
			}

			return this.isViableForCaptureInner(paymentTransaction);
		} catch (final Exception e) {
			final RuntimeException toolboxPaymentException = new ToolboxPaymentGeneralException("isViableForCapture error!", e);
			this.paymentTransactionRawLogger.error(toolboxPaymentException.getMessage(), toolboxPaymentException);
			throw toolboxPaymentException;
		}
	}

	@SuppressWarnings("unused")
	public boolean isViableForCaptureInner(final PaymentTransaction paymentTransaction) {
		throw new UnsupportedOperationException();
	}

	public boolean isViableForRefund(final PaymentTransaction paymentTransaction) {
		try {

			if (!this.hasRefundFeature()) {
				return false;
			}

			return this.isViableForRefundInner(paymentTransaction);
		} catch (final Exception e) {
			final RuntimeException toolboxPaymentException = new ToolboxPaymentGeneralException("isViableForRefund error!", e);
			this.paymentTransactionRawLogger.error(toolboxPaymentException.getMessage(), toolboxPaymentException);
			throw toolboxPaymentException;
		}
	}

	public boolean isViableForRefundInner(final PaymentTransaction paymentTransaction) {

		if (!paymentTransaction.getPaymentOperationType().equals(ToolboxSysKeys.PaymentOperationType.PURCHASE) || !paymentTransaction.getStatus().equals(ToolboxSysKeys.PaymentTransactionStatus.SUCCESS)) {
			return false;
		}

		final List<PaymentTransaction> allByReferredTransactionId = this.paymentTransactionService.findAllByReferredTransactionId(paymentTransaction.getId());
		for (final PaymentTransaction pt : allByReferredTransactionId) {
			if (pt.getPaymentOperationType().equals(ToolboxSysKeys.PaymentOperationType.REFUND) && pt.getStatus().equals(ToolboxSysKeys.PaymentTransactionStatus.SUCCESS)) {
				return false;
			}
		}

		return true;

	}

	public boolean isViableForVoid(final PaymentTransaction paymentTransaction) {

		try {

			if (!this.hasVoidFeature()) {
				return false;
			}

			return this.isViableForVoidInner(paymentTransaction);
			
		} catch (final Exception e) {
			final RuntimeException toolboxPaymentException = new ToolboxPaymentGeneralException("isViableForVoid error!", e);
			this.paymentTransactionRawLogger.error(toolboxPaymentException.getMessage(), toolboxPaymentException);
			throw toolboxPaymentException;
		}
	}

	@SuppressWarnings("unused")
	public boolean isViableForVoidInner(final PaymentTransaction paymentTransaction) {
		throw new UnsupportedOperationException();
	}

	public PaymentTransaction processReturnUrl(PaymentTransaction paymentTransaction, final HttpServletRequest httpServletRequest) {

		if (paymentTransaction.isNew()) {
			log.error("Invalid id (only existing transaction is allowed here)!");
			throw new ToolboxPaymentValidationException("Invalid id (only existing transaction is allowed here)!");
		}
		
		// ---

		try {
			
			this.paymentTransactionRawLogger.setPaymentTransactionId(paymentTransaction.getId());

			this.processReturnUrlInner(paymentTransaction, httpServletRequest);

		} catch (final Exception e) {

			final RuntimeException toolboxPaymentException = new ToolboxPaymentGeneralException("payment processReturnUrl error!", e);
			this.paymentTransactionRawLogger.error(toolboxPaymentException.getMessage(), toolboxPaymentException);
			throw toolboxPaymentException;

		} finally {
			
			try {
				paymentTransaction = this.perisistPaymentTransaction(paymentTransaction);
			} finally {
				this.paymentTransactionRawLogger.setPaymentTransactionId(null);
			}
			
		}
		
		return paymentTransaction;

	}

	/**
	 * Többnyire az, ami itt bejön nem fogadható el extra ellenőrzés nélkül valós értékként! 
	 * Tehát kell egy általunk indított (poll) jellegű status check is és csak annak eredménye számít! 
	 * (mindez security okból fontos) 
	 * (kívétel lehet az, ha a bejeövő üzenet valamilyen módon alá van írva, 
	 * vagy egyéb módon biztosan tudható, hogy hiteles)
	 * 
	 * @param paymentTransaction
	 * @param httpServletReques
	 * @param httpServletResponse
	 * 
	 * @return
	 * 		itt szükség esetén vissza lehet egy plusz objektumot juttani a controller szintig
	 */
	public Object processServerToServerNotification(final PaymentTransaction paymentTransaction,
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) {

		if (paymentTransaction.isNew()) {
			log.error("Invalid id (only existing transaction is allowed here)!");
			throw new ToolboxPaymentValidationException("Invalid id (only existing transaction is allowed here)!");
		}

		// ---

		try {
			
			this.paymentTransactionRawLogger.setPaymentTransactionId(paymentTransaction.getId());

			return this.processServerToServerNotificationInner(paymentTransaction, httpServletRequest, httpServletResponse);

		} catch (final Exception e) {

			final RuntimeException toolboxPaymentException = new ToolboxPaymentGeneralException("payment processServerToServerNotificationInner error!", e);
			this.paymentTransactionRawLogger.error(toolboxPaymentException.getMessage(), toolboxPaymentException);
			throw toolboxPaymentException;

		} finally {
			
			try {
				this.perisistPaymentTransaction(paymentTransaction);
			} finally {
				this.paymentTransactionRawLogger.setPaymentTransactionId(null);
			}
			
		}
		
	}

	protected final String buildServerToServerNotificationReceiverControllerUrl(final PaymentTransaction paymentTransaction) {
		return ServerToServerNotificationReceiverController.buildServerToServerNotificationReceiverControllerUrl(paymentTransaction);
	}

	/**
	 * adott API/SDK-val lekérni, hogy mi az aktuális status (pl.: befejeződött-e közben a tranzakció)
	 *
	 * @param paymentTransaction
	 * @return
	 * @throws Exception
	 */
	abstract protected PaymentTransaction checkTransactionStatusInner(PaymentTransaction paymentTransaction) throws Exception;

	/**
	 * lefoglalt keretből pénz levonása
	 *
	 * @param paymentTransaction
	 * @param referredTransaction 
	 * @return
	 */
	abstract protected PaymentTransaction doCaptureInner(final PaymentTransaction paymentTransaction, final PaymentTransaction referredTransaction) throws Exception;

	/**
	 * purchase (= auth + capture egyben (tehát normál fizetés)),
	 * vagy auth (két "lépcsős" fizetés, ahol a capture időben később történik, értsd csak egy keret "lefoglalás")
	 *
	 * @param paymentTransaction
	 * @return
	 */
	abstract protected PaymentTransaction doDepositInner(PaymentTransaction paymentTransaction) throws Exception;

	/**
	 * doWithdraw speciális alesete, amikor egy korábbi befizetést küldünk csak vissza
	 * (pl.: visszahozta a rendelt terméket)
	 *
	 * @param paymentTransaction
	 * 		ez a maga a mostani tranzackió (maga a refund)
	 * @param referredTransaction 
	 * 		ez az eredeti fizetés, amit refund-olunk most (tehát ez külön rekord!)
	 * @return
	 */
	abstract protected PaymentTransaction doRefundInner(final PaymentTransaction paymentTransaction, final PaymentTransaction referredTransaction) throws Exception;

	protected PaymentTransaction doTransactionInner(PaymentTransaction paymentTransaction) throws Exception {

		ToolboxAssert.notNull(paymentTransaction.getPaymentOperationType());

		final int paymentOperationType = paymentTransaction.getPaymentOperationType().intValue();

		if (ToolboxSysKeys.PaymentOperationType.PURCHASE == paymentOperationType) {

			paymentTransaction = this.doDepositInner(paymentTransaction);

		} else if (ToolboxSysKeys.PaymentOperationType.REFUND == paymentOperationType) {

			final PaymentTransaction referredTransaction = this.paymentTransactionService.findOne(paymentTransaction.getReferredTransactionId());

			if (!this.isViableForRefund(referredTransaction)) {
				throw new ToolboxPaymentValidationException("Not viable for refund!");
			}

			if (paymentTransaction.getOriginalAmount().compareTo(referredTransaction.getOriginalAmount()) != 0) {
				throw new ToolboxPaymentValidationException("Partial refunds are not supported currently!");
			}

			paymentTransaction = this.doRefundInner(paymentTransaction, referredTransaction);

		} else if (ToolboxSysKeys.PaymentOperationType.AUTH == paymentOperationType) {

			if (!this.hasAuthFeature()) {
				throw new ToolboxPaymentValidationException("Auth feature not available!");
			}

			paymentTransaction = this.doDepositInner(paymentTransaction);

		} else if (ToolboxSysKeys.PaymentOperationType.CAPTURE == paymentOperationType) {

			final PaymentTransaction referredTransaction = this.paymentTransactionService.findOne(paymentTransaction.getReferredTransactionId());

			if (!this.isViableForCapture(referredTransaction)) {
				throw new ToolboxPaymentValidationException("Not viable for capture!");
			}

			paymentTransaction = this.doCaptureInner(paymentTransaction, referredTransaction);

		} else if (ToolboxSysKeys.PaymentOperationType.VOID == paymentOperationType) {

			final PaymentTransaction referredTransaction = this.paymentTransactionService.findOne(paymentTransaction.getReferredTransactionId());

			if (!this.isViableForVoid(referredTransaction)) {
				throw new ToolboxPaymentValidationException("Not viable for void!");
			}

			paymentTransaction = this.doVoidInner(paymentTransaction, referredTransaction);

		} else if (ToolboxSysKeys.PaymentOperationType.WITHDRAW == paymentOperationType) {

			if (!this.hasWithdrawFeature()) {
				throw new ToolboxPaymentValidationException("Withdraw feature not available!");
			}

			paymentTransaction = this.doWithdrawInner(paymentTransaction);

		} else {
			throw new UnsupportedOperationException();
		}

		return paymentTransaction;
	}

	/**
	 * void, ha értelmezhető (általában auth/capture típusnál, ha "mégsem")
	 * (ez nem egészen ugyanaz, mint a refund, mert itt csak a "foglalást" engedjük el, refund-nál meg a már megkapott pénzt küldjük vissza)
	 *
	 * @param paymentTransaction
	 * @param referredTransaction 
	 * @return
	 * @throws Exception
	 */
	abstract protected PaymentTransaction doVoidInner(final PaymentTransaction paymentTransaction, final PaymentTransaction referredTransaction) throws Exception;

	/**
	 * pénz kiküldés a vásárlós/ügyfél fele (tehát itt a mi egyenlegünk csökken)
	 * (ez nem mindig refund jellegű, lehet pl. egy online kaszinóban nyeremény)
	 *
	 * @param paymentTransaction
	 * @return
	 */
	abstract protected PaymentTransaction doWithdrawInner(final PaymentTransaction paymentTransaction) throws Exception;

	/**
	 * JSON alapú config (többnyire ez van használva, ritkábban a fájl alapú)
	 *
	 * @return
	 */
	public JSONObject extractConfig1() {

		final PaymentConfig paymentConfig = this.paymentConfigService.findOneForPaymentProvider(this.getPaymentProviderCodeStoreItemId());
		ToolboxAssert.isTrue(Boolean.TRUE.equals(paymentConfig.getEnabled()));

		if (StringUtils.isNotBlank(paymentConfig.getConfigJson())) {
			return new JSONObject(paymentConfig.getConfigJson());
		}

		return null;
	}

	/**
	 * fájl alapú config (ha van) (pl.: privát kulcs fájl)
	 *
	 * @return
	 */
	protected List<FileDescriptor> extractConfig2() {

		final PaymentConfig paymentConfig = this.paymentConfigService.findOneForPaymentProvider(this.getPaymentProviderCodeStoreItemId());
		ToolboxAssert.isTrue(Boolean.TRUE.equals(paymentConfig.getEnabled()));

		// ---

		List<FileDescriptor> fileDescriptorList;

		final ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();

		try {

			SecurityUtil.setSystemUser();

			FileDescriptorJdbcRepository.setTlAllowCommontTenantFallbackLookup(true);

			if (paymentConfig.getFileIds() != null) {
				fileDescriptorList = FileStoreHelper.toFileDescriptorList(FileOperationAccessTypeIntent.READ_ONLY_INTENT, paymentConfig.getFileIds());
			} else {
				fileDescriptorList = new ArrayList<>();
			}

		} finally {
			FileDescriptorJdbcRepository.clearTlAllowCommontTenantFallbackLookup();
			SecurityUtil.setUser(loggedInUser);
		}

		// ---

		return fileDescriptorList;
	}

	protected JSONObject extractProviderSpecificInput(final PaymentTransaction paymentTransaction) {
		return new JSONObject(paymentTransaction.getProviderSpecificInput());
	}

	protected JSONObject extractProviderSpecificProcessingData(final PaymentTransaction paymentTransaction) {
		return new JSONObject(paymentTransaction.getProviderSpecificProcessingData());
	}

	protected PaymentTransaction perisistPaymentTransaction(final PaymentTransaction paymentTransaction) {
		return this.paymentTransactionService.save(paymentTransaction);
	}

	abstract protected PaymentTransaction processReturnUrlInner(final PaymentTransaction paymentTransaction, final HttpServletRequest httpServletRequest) throws Exception;

	/**
	 * @param paymentTransaction
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * 
	 * @return
	 * 		itt szükség esetén vissza lehet egy plusz objektumot juttani a controller szintig (ha nincs szükség rá, akkor return null)
	 * 
	 * @throws Exception
	 */
	protected abstract Object processServerToServerNotificationInner(PaymentTransaction paymentTransaction, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception;

	protected void writeProviderSpecificOutput(final PaymentTransaction paymentTransaction, final Object providerSpecificOutputObject) {
		paymentTransaction.setProviderSpecificOutput(JacksonHelper.toJson(providerSpecificOutputObject));
	}

	protected void writeProviderSpecificProcessingData(final PaymentTransaction paymentTransaction, final JSONObject providerSpecificProcessingData) {
		paymentTransaction.setProviderSpecificProcessingData(providerSpecificProcessingData.toString());
	}

	protected void writeProviderSpecificProcessingData(final PaymentTransaction paymentTransaction, final Object providerSpecificProcessingDataObject) {
		paymentTransaction.setProviderSpecificProcessingData(JacksonHelper.toJson(providerSpecificProcessingDataObject));
	}

}
