package hu.lanoga.toolbox.vaadin.component.payment;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.teamunify.i18n.I;
import com.vaadin.event.UIEvents;
import com.vaadin.event.UIEvents.PollEvent;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.PaymentProvider;
import hu.lanoga.toolbox.codestore.CodeStoreItem;
import hu.lanoga.toolbox.codestore.CodeStoreItemService;
import hu.lanoga.toolbox.payment.PaymentManager;
import hu.lanoga.toolbox.payment.PaymentTransaction;
import hu.lanoga.toolbox.payment.PaymentTransactionService;
import hu.lanoga.toolbox.payment.exception.ToolboxPaymentGeneralException;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryTenantManager;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.util.ToolboxAssert;
import hu.lanoga.toolbox.vaadin.AbstractToolboxUI;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class PaymentCustomerEndComponent extends VerticalLayout implements UIEvents.PollListener {

	private static final long LOOP_STOP_AFTER = 1000L * 60L * 5L; // millisec

	private final String paymentTransactionGid;
	private final AbstractToolboxUI ui;

	private volatile PaymentTransaction paymentTransaction;

	protected final ProgressBar pbWaiting;

	private final PaymentTransactionService paymentTransactionService;
	private final PaymentManager paymentManager;
	private final CodeStoreItemService codeStoreItemService;

	private CodeStoreItem statusCsi;
	private long millisStartedOn;

	public PaymentCustomerEndComponent() {

		this.ui = (AbstractToolboxUI) UI.getCurrent();
		ToolboxAssert.notNull(this.ui);

		// ---

		this.paymentTransactionService = ApplicationContextHelper.getBean(PaymentTransactionService.class);
		this.paymentManager = ApplicationContextHelper.getBean(PaymentManager.class);
		this.codeStoreItemService = ApplicationContextHelper.getBean(CodeStoreItemService.class);

		// ---

		{

			// final String parameter = VaadinServletRequest.getCurrent().getParameter("paymentTransactionGid");
			String parameter = VaadinServletRequest.getCurrent().getRequestURL().toString();
			final String[] split = parameter.split("/");
			parameter = split[split.length - 1];

			this.paymentTransactionGid = UUID.fromString(Jsoup.clean(StringUtils.removeStart(parameter.toLowerCase(), "idx"), Safelist.basic())).toString();
			ToolboxAssert.notNull(this.paymentTransactionGid);

			this.paymentTransaction = this.paymentTransactionService.findByGid(this.paymentTransactionGid);
			ToolboxAssert.notNull(this.paymentTransaction);

			if (paymentTransaction.getPaymentProvider().equals(PaymentProvider.SIMPLEPAY2))	{

				try {

					JdbcRepositoryTenantManager.setTlTenantId(this.paymentTransaction.getTenantId());

					this.paymentTransaction = this.paymentManager.processReturnUrl(this.paymentTransaction, VaadinServletRequest.getCurrent().getHttpServletRequest());

				} finally {
					JdbcRepositoryTenantManager.clearTlTenantId();
				}


			}

		}

		// ---

		this.pbWaiting = new ProgressBar();
		this.pbWaiting.setCaption(I.trc("Caption", "Please wait..."));
		this.pbWaiting.setIndeterminate(true);

		this.addComponent(this.pbWaiting);

		// ---

		{
			
			// itt egy nagyon egyszerű poller logikával dolgozunk, push helyett
			// stabilabb stb.
			// így nem kell a loggedInUser háttér thread-en való beléptetésével sem foglakozni
			
			final int p = this.ui.getPollInterval();
			ToolboxAssert.isTrue(p != -1 && p < 10000, "Please enable polling (10 sec or less)!");

			this.millisStartedOn = System.currentTimeMillis();

			this.ui.addPollListener(this);
		}

	}

	/**
	 * alap handleResult
	 * 
	 * @param orderId
	 * @param isSuccessful
	 */
	protected abstract void handleResult(String orderId, boolean isSuccessful);

	/**
	 * ebben a változat-ban a paymentProvider is elérhető, 
	 * használd ezt (Override)m, ha erre is szükséged van a saját felületeden
	 * 
	 * @param orderId
	 * @param paymentProvider
	 * 		{@link PaymentProvider}
	 * @param isSuccessful
	 */
	@SuppressWarnings("unused")
	protected void handleResult(final String orderId, final int paymentProvider, final boolean isSuccessful) {
		this.handleResult(orderId, isSuccessful);
	}

	/**
	 * ebben a változat-ban az egész {@link PaymentTransaction} objektum elérhető...  
	 * csak rendkívüli esetekben kell ez! körültekintő használatot igényel...  
	 * gondolj arra is, ha a {@link PaymentTransaction} model változna... 
	 * 
	 * @param paymentTransaction
	 */
	protected void handleResult(final PaymentTransaction paymentTransactionForUi) {

		this.handleResult(
				paymentTransactionForUi.getOrderId(),
				paymentTransactionForUi.getPaymentProvider(),
				wasSuccess(paymentTransactionForUi));

	}

	protected boolean wasSuccess(final PaymentTransaction paymentTransactionForUi) {
		return paymentTransactionForUi.getStatus().equals(ToolboxSysKeys.PaymentTransactionStatus.SUCCESS) ||
				paymentTransactionForUi.getStatus().equals(ToolboxSysKeys.PaymentTransactionStatus.RECEIVED);
	}

	@Override
	public void poll(final PollEvent event) {

		final long millisSinceStart = System.currentTimeMillis() - this.millisStartedOn;

		boolean isFinal = false;

		if (millisSinceStart > LOOP_STOP_AFTER) {

			log.debug("timeout... ending check loop...");

			this.ui.removePollListener(this);
			isFinal = true;
			
		} else {

			// ---

			try {

				JdbcRepositoryTenantManager.setTlTenantId(this.paymentTransaction.getTenantId());
				
				this.paymentTransaction = this.paymentManager.checkTransactionStatusForUiPoll(this.paymentTransaction, true);
				
				final Integer currentStatus = this.paymentTransaction.getStatus();

				if (currentStatus != null && !currentStatus.equals(ToolboxSysKeys.PaymentTransactionStatus.PENDING)) {
					this.statusCsi = this.codeStoreItemService.findOne(this.paymentTransaction.getStatus());

					this.ui.removePollListener(this);
					isFinal = true;
				}

			} finally {
				JdbcRepositoryTenantManager.clearTlTenantId();
			}

		}

		// ---

		if (isFinal) {

			final Label lblResult = new Label(
					I.trc("Caption", "Payment transaction") +
							" (" + this.paymentTransactionGid + "): " +
							((this.statusCsi != null) ? (this.statusCsi.getCaptionCaption()) : ("(" + I.trc("Label", "error/timeout") + "?)")));

			this.replaceComponent(this.pbWaiting, lblResult);

			final PaymentTransaction paymentTransactionForUi = new PaymentTransaction();
			try {
				BeanUtils.copyProperties(paymentTransactionForUi, this.paymentTransaction); // mj. ez nem deep copy, de itt jó (mert a PaymentTransaction egy "sima" model, nincs mélyebb része)
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new ToolboxPaymentGeneralException("PaymentCustomerEndComponent copyProperties() failed!", e);
			}

			// ---
			
			this.handleResult(paymentTransactionForUi);
			
		}
	}

}
