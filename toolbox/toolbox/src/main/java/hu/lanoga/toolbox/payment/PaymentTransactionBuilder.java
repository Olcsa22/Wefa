package hu.lanoga.toolbox.payment;

import java.math.BigDecimal;

import hu.lanoga.toolbox.util.BrandUtil;
import lombok.Getter;

@Getter
public class PaymentTransactionBuilder {

	private String gid;

	private Integer referredTransactionId;

	private Integer customerUserId;
	private String customerId;
	private String customerEmail;

	private BigDecimal originalAmount;
	private String originalCurrency;

	private String orderId;
	private String orderInfoLabel;
	/**
	 * JSON, opcionális, egyes alkalmazásokhoz... 
	 */
	private String orderDetails;

	private Integer paymentProvider;
	private Integer paymentOperationType;

	private String providerSpecificInput;

	private String returnUrl = BrandUtil.getRedirectUriHostFrontend();

	private Integer status;

	private Integer additionalDepositStageCount = null;
	private Integer additionalDepositStageFinished = null;

	private Boolean manualStatusChange = false;

	private String meta1;
	private String meta2;

	/**
	 * még nem decryptelt token (Apple Pay, Google Pay stb. full kapott adat)
	 */
	private String walletFullToken;

	/* -------------------------------------------------- */

	public PaymentTransactionBuilder setGid(final String gid) {
		this.gid = gid;
		return this;
	}

	public PaymentTransactionBuilder setReferredTransactionId(final Integer referredTransactionId) {
		this.referredTransactionId = referredTransactionId;
		return this;
	}

	public PaymentTransactionBuilder setCustomerId(final String customerId) {
		this.customerId = customerId;
		return this;
	}

	public PaymentTransactionBuilder setCustomerUserId(final Integer customerUserId) {
		this.customerUserId = customerUserId;
		return this;
	}

	public PaymentTransactionBuilder setCustomerEmail(String customerEmail) {
		this.customerEmail = customerEmail;
		return this;
	}

	public PaymentTransactionBuilder setOrderId(final String orderId) {
		this.orderId = orderId;
		return this;
	}

	public PaymentTransactionBuilder setOrderInfoLabel(final String orderInfoLabel) {
		this.orderInfoLabel = orderInfoLabel;
		return this;
	}

	public PaymentTransactionBuilder setOrderDetails(String orderDetails) {
		this.orderDetails = orderDetails;
		return this;
	}

	public PaymentTransactionBuilder setPaymentProvider(final Integer paymentProvider) {
		this.paymentProvider = paymentProvider;
		return this;
	}

	public PaymentTransactionBuilder setPaymentOperationType(final Integer paymentOperationType) {
		this.paymentOperationType = paymentOperationType;
		return this;
	}

	public PaymentTransactionBuilder setProviderSpecificInput(final String providerSpecificInput) {
		this.providerSpecificInput = providerSpecificInput;
		return this;
	}

	public PaymentTransactionBuilder setReturnUrl(final String returnUrl) {
		this.returnUrl = returnUrl;
		return this;
	}

	public PaymentTransactionBuilder setOriginalAmount(final BigDecimal originalAmount) {
		this.originalAmount = originalAmount;
		return this;
	}

	public PaymentTransactionBuilder setOriginalCurrency(final String originalCurrency) {
		this.originalCurrency = originalCurrency;
		return this;
	}

	public PaymentTransactionBuilder setStatus(final Integer status) {
		this.status = status;
		return this;
	}

	public PaymentTransactionBuilder setAdditionalDepositStageCount(final Integer additionalDepositStageCount) {
		this.additionalDepositStageCount = additionalDepositStageCount;
		return this;
	}

	public PaymentTransactionBuilder setAdditionalDepositStageFinished(final Integer additionalDepositStageFinished) {
		this.additionalDepositStageFinished = additionalDepositStageFinished;
		return this;
	}

	public PaymentTransactionBuilder setManualStatusChange(final Boolean manualStatusChange) {
		this.manualStatusChange = manualStatusChange;
		return this;
	}

	public PaymentTransactionBuilder setWalletFullToken(String walletFullToken) {
		this.walletFullToken = walletFullToken;
		return this;
	}

	public PaymentTransactionBuilder setMeta1(String meta1) {
		this.meta1 = meta1;
		return this;
	}

	public PaymentTransactionBuilder setMeta2(String meta2) {
		this.meta2 = meta2;
		return this;
	}

	/* -------------------------------------------------- */

	public PaymentTransaction createPaymentTransaction() {
		return new PaymentTransaction(
				this.gid, this.referredTransactionId,
				this.orderId, this.orderInfoLabel, this.orderDetails,
				this.customerUserId, this.customerId, this.customerEmail,
				this.originalAmount, this.originalCurrency,
				this.paymentProvider, this.paymentOperationType, this.providerSpecificInput,
				this.returnUrl,
				this.status,
				this.additionalDepositStageCount, this.additionalDepositStageFinished,
				this.manualStatusChange,
				this.walletFullToken,
				this.meta1, this.meta2);
	}

}