package hu.lanoga.toolbox.payment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.codestore.CodeStoreItem;
import hu.lanoga.toolbox.codestore.CodeStoreItemService;
import hu.lanoga.toolbox.payment.exception.ToolboxPaymentGeneralException;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.util.ToolboxAssert;
import hu.lanoga.toolbox.util.ToolboxStringUtil;
import hu.lanoga.toolbox.vaadin.component.payment.PaymentCustomerEndComponent;

@Service
public class PaymentManager {

	@Autowired
	private CodeStoreItemService codeStoreItemService;

	@Autowired
	private PaymentConfigService paymentConfigService;
	
	/**
	 * előállítja a normál tranzakció refund stb. "párját"
	 * (csak létrehozza és kitölti a model objektumot, nem ment DB-be, nem hív API-t...)
	 * 
	 * @param paymentTransaction
	 * @return
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public PaymentTransaction buildChildTransaction(final PaymentTransaction paymentTransaction, final int paymentOperationType) {

		ToolboxAssert.isTrue(this.isViableForRefund(paymentTransaction));

		// ---

		// tehát a refund stb. is egy külön PaymentTransaction
		// setReferredTransactionId() köti hozzá az deposit párjához

		final PaymentTransactionBuilder ptb = new PaymentTransactionBuilder();
		ptb.setReferredTransactionId(paymentTransaction.getId());
		ptb.setGid(UUID.randomUUID().toString());
		ptb.setOrderId(paymentTransaction.getOrderId());
		ptb.setPaymentProvider(paymentTransaction.getPaymentProvider());
		ptb.setPaymentOperationType(paymentOperationType);
		ptb.setOriginalAmount(paymentTransaction.getOriginalAmount());
		ptb.setCustomerId(paymentTransaction.getCustomerId());
		ptb.setCustomerUserId(paymentTransaction.getCustomerUserId());
		ptb.setCustomerEmail(paymentTransaction.getCustomerEmail());
		ptb.setOriginalCurrency(paymentTransaction.getOriginalCurrency());
		ptb.setReturnUrl(null);
		ptb.setOrderInfoLabel("-"); // később kötelező paraméter, ezért kell valamit idetenni ilyenkor is
		ptb.setStatus(ToolboxSysKeys.PaymentTransactionStatus.GATEWAY_INIT);

		return ptb.createPaymentTransaction();

	}
	
	@Transactional
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public PaymentTransaction checkTransactionStatusForUiPoll(final PaymentTransaction paymentTransaction, final boolean makeRequest) {
		
		if (!Boolean.TRUE.equals(paymentTransaction.getStatusCheckDisabledPoll())) {
			
			return this.checkTransactionStatus(paymentTransaction, makeRequest);
			
		} else {
			
			// ilyen getStatusCheckDisabledPoll() true esetekben
			// nem végzünk érdemi status check-et (nem hívjuk meg a payment provider API-ját)
			
			// viszont DB-ből befrissítjük a PaymentTransaction objektumot
			// (azért, hogy észrevegyük, amikor más mechanizmus, notif. receive módosította)
			
			// tehát ilyenkor fixen makeRequest = false
			
			return this.checkTransactionStatus(paymentTransaction, false);
			
		}
		
	}

	/**
	 * pénz tranzakció statuszának lekérése (API-k hívása, mentése DB-be stb.) a paraméter objektumban megadott providerrel...
	 * 
	 * @param paymentTransaction
	 * @param makeRequest
	 * 		hívjuk-e meg a provider API-ját, vagy csak a saját DB-ben tárolt értékre vagyunk kíváncsiak, 
	 * 		lásd még {@link PaymentTransaction#getStatusCheckDisabledPoll()}
	 * @return
	 */
	@Transactional
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public PaymentTransaction checkTransactionStatus(final PaymentTransaction paymentTransaction, final boolean makeRequest) {
		return this.getPaymentProviderService(paymentTransaction).checkTransactionStatus(paymentTransaction, makeRequest);
	}
	
	@Transactional
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public PaymentTransaction checkTransactionStatus(final PaymentTransaction paymentTransaction, final boolean makeRequest, final boolean forceRequest) {
		return this.getPaymentProviderService(paymentTransaction).checkTransactionStatus(paymentTransaction, makeRequest, forceRequest);
	}

	/**
	 * @param paymentTransactionList
	 * @return
	 * 
	 * @see #checkTransactionStatus(PaymentTransaction, boolean)
	 */
	@Transactional
	public List<PaymentTransaction> checkTransactionStatusList(final List<PaymentTransaction> paymentTransactionList) {
		
		SecurityUtil.limitAccessSystem();
		
		// ---
		
		final List<PaymentTransaction> paymentTransactionChecked = new ArrayList<>();

		for (final PaymentTransaction paymentTransaction : paymentTransactionList) {
			paymentTransactionChecked.add(this.checkTransactionStatus(paymentTransaction, true));
		}

		paymentTransactionList.clear();
		paymentTransactionList.addAll(paymentTransactionChecked);

		return paymentTransactionList;
	}
	
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public JSONObject extractConfig1(int paymentProvider) {
		return this.getPaymentProviderService(paymentProvider).extractConfig1();
	}

	/**
	 * tranzakció végrehajtása (API-k hívása, mentése DB-be stb.) a paraméter objektumban megadott providerrel...
	 * 
	 * @param paymentTransaction
	 * @return
	 * 
	 * @see PaymentTransactionBuilder
	 */
	// @Transactional // TODO: zavaros, hogy kell-e itt 
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public PaymentTransaction doTransaction(final PaymentTransaction paymentTransaction) {
		return this.getPaymentProviderService(paymentTransaction).doTransaction(paymentTransaction);
	}
	
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public List<CodeStoreItem> getAvailablePaymentProviders() {
		return this.getAvailablePaymentProviders(null);
	}

	/**
	 * lényeg, hogy két dolog kell: 
	 * 1) legyen elérhető (Maven) az adott module a projektben
	 * 2) legyen (helyes) {@link PaymentConfig} record az adott tenant-hoz
	 * 
	 * @param allowedPaymentProviderIds
	 * 		itt további filterezésre van lehetőség, az amúgy a projektben az adott tenant számára elérhető provider lista tovább szűkíthető ezzel 
	 * 		(null paraméter estén nincs ez a fajta extra szűrés)
	 * @return
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public List<CodeStoreItem> getAvailablePaymentProviders(final Set<Integer> allowedPaymentProviderIds) {

		final TreeSet<Integer> enabledPaymentProviders = this.paymentConfigService.findAllEnabledPaymentProvider();
		final Map<String, AbstractPaymentProviderService> beanMap = ApplicationContextHelper.getBeans(AbstractPaymentProviderService.class);

		final List<CodeStoreItem> resultList = new ArrayList<>();
		
		CodeStoreItem irlCodeStoreItem = null;

		for (final Entry<String, AbstractPaymentProviderService> entry : beanMap.entrySet()) {

			// csak azokat az értékeket tartjuk meg, amelyek megtalálhatóak az enabledPaymentProviders-ben

			final CodeStoreItem codeStoreItem = entry.getValue().getPaymentProviderCodeStoreItem();

			if (enabledPaymentProviders.contains(codeStoreItem.getId())) {
				if (allowedPaymentProviderIds == null || allowedPaymentProviderIds.contains(codeStoreItem.getId())) {
					
					if (codeStoreItem.getId().equals(ToolboxSysKeys.PaymentProvider.IRL)) {
						irlCodeStoreItem = codeStoreItem;
					} else {
						resultList.add(codeStoreItem);
					}
					
				}
			}

		}
		
		List<CodeStoreItem> orderedResultList = resultList.stream().sorted((x, y) -> {
			return x.getCommand().compareTo(y.getCommand());
		}).collect(Collectors.toList());

		if (irlCodeStoreItem != null) {
			orderedResultList.add(irlCodeStoreItem); // azért, hogy ez mindig az utolsó legyen
		}
		
		return orderedResultList;
	}
	
	/**
	 * elérhető-e a capture művelet ehhez a rekordhoz 
	 * (támogat-e a provider refund-ot és ez a rekord olyan-e, hogy refund felmerülhet nála)
	 * 
	 * @param paymentTransaction
	 * @return
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public boolean isViableForCapture(final PaymentTransaction paymentTransaction) {
		return this.getPaymentProviderService(paymentTransaction).isViableForCapture(paymentTransaction);
	}

	/**
	 * elérhető-e a refund művelet ehhez a rekordhoz 
	 * (támogat-e a provider refund-ot és ez a rekord olyan-e, hogy refund felmerülhet nála)
	 * 
	 * @param paymentTransaction
	 * 		a normál tranzakció (purchase)
	 * @return
	 * 
	 * @see #buildChildTransaction(PaymentTransaction)
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public boolean isViableForRefund(final PaymentTransaction paymentTransaction) {
		return this.getPaymentProviderService(paymentTransaction).isViableForRefund(paymentTransaction);
	}

	/**
	 * elérhető-e a void művelet ehhez a rekordhoz 
	 * (támogat-e a provider refund-ot és ez a rekord olyan-e, hogy refund felmerülhet nála)
	 * 
	 * @param paymentTransaction
	 * @return
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public boolean isViableForVoid(final PaymentTransaction paymentTransaction) {
		return this.getPaymentProviderService(paymentTransaction).isViableForVoid(paymentTransaction);
	}
	
	/**
	 * Amennyiben a payment provider a customer visszairánytás URL-be tesz paramétereket (olyat, ami kardinális), 
	 * akkor itt lehet feldolgozni. A UI {@link PaymentCustomerEndComponent} betolja ide. 
	 * 
	 * Szükség estén itt fel kell dolognzi és a {@link PaymentTransaction} modelbe beírni, ami szükséges.
	 * Hasznos lehet tárolásra: {@link PaymentTransaction#setProviderSpecificOutput(String)} 
	 * vagy {@link PaymentTransaction#setProviderSpecificProcessingData(String)}.
	 * 
	 * 
	 * @param paymentTransaction
	 * @param httpServletRequest
	 * @return
	 */
	@Transactional
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public PaymentTransaction processReturnUrl(final PaymentTransaction paymentTransaction, final HttpServletRequest httpServletRequest) {
		return this.getPaymentProviderService(paymentTransaction).processReturnUrl(paymentTransaction, httpServletRequest);
	}
	
	/**
	 * Amennyiben a payment provider egy server to server notification-t küld (pl. SimplePay esetén "IPN"), amit fontos feldolgoznunk, 
	 * akkor itt lehet megtenni.
	 * 
	 * A {@link PaymentTransaction} modelbe beírni, ami szükséges... 
	 * Hasznos lehet tárolásra: {@link PaymentTransaction#setProviderSpecificOutput(String)} 
	 * vagy {@link PaymentTransaction#setProviderSpecificProcessingData(String)}.
	 * 
	 * @param paymentTransaction
	 * @param httpServletRequest
	 * 		az eredeti request
	 * @param httpServletResponse
	 * 		a {@link HttpServletResponse} objektum, ide írd bele a válasz body-t és header-öket (ha vannak), 
	 * 		ha nem kell visszadni a payment provider-nek (akinek a szervere a hívó itt), akkor egy HTTP 200-as status elég
	 * 
	 * @return
	 * 		itt szükség esetén vissza lehet egy plusz objektumot juttani a controller szintig
	 * 
	 * @see ServerToServerNotificationReceiverController
	 */
	@Transactional
	public Object processServerToServerNotification(final PaymentTransaction paymentTransaction,
			HttpServletRequest httpServletRequest, 
			HttpServletResponse httpServletResponse) {
		
		SecurityUtil.limitAccessSystem();
		
		// ---
		
		return this.getPaymentProviderService(paymentTransaction).processServerToServerNotification(paymentTransaction, httpServletRequest, httpServletResponse);
	}

	private AbstractPaymentProviderService getPaymentProviderService(final PaymentTransaction paymentTransaction) {		
		return getPaymentProviderService(paymentTransaction.getPaymentProvider());

	}

	private AbstractPaymentProviderService getPaymentProviderService(int paymentProvider) {
		
		this.limitPaymentProvider(paymentProvider);
		
		try {
			
			final CodeStoreItem codeStoreItem = this.codeStoreItemService.findOne(paymentProvider);
			
			final String beanName = ToolboxStringUtil.underscoreToCamelCase(codeStoreItem.getCommand()) + "PaymentProviderService";
			
			return ApplicationContextHelper.getBean(beanName, AbstractPaymentProviderService.class);

		} catch (final NoSuchBeanDefinitionException e) {
			throw new ToolboxPaymentGeneralException("Missing payment provider (missing JAR?)!", e);
		}
		
	}

	/**
	 * DB config alapján mi van egedélyezve az aktuális tenant-nak 
	 * (mj.: ez még magában nem elég, az is kell majd, hogy az adott provider service bean elérhető legyen a projektben..)
	 *
	 * @return
	 * 
	 * @throws ToolboxPaymentGeneralException ha nincs engedélyezve
	 */
	private void limitPaymentProvider(final int paymentProviderCsiId) {
		if (!this.paymentConfigService.findAllEnabledPaymentProvider().contains(paymentProviderCsiId)) {
			throw new ToolboxPaymentGeneralException("Disabled payment provider (for tenantId: " + SecurityUtil.getLoggedInUserTenantId() + ")");
		}
	}

}
