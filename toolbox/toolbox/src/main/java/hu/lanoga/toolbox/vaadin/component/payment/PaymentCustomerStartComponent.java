package hu.lanoga.toolbox.vaadin.component.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.vaadin.dialogs.ConfirmDialog;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Striped;
import com.teamunify.i18n.I;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.JavaScriptFunction;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import elemental.json.JsonArray;
import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.codestore.CodeStoreItem;
import hu.lanoga.toolbox.exception.ManualValidationException;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.payment.ApplePayUtil;
import hu.lanoga.toolbox.payment.PaymentManager;
import hu.lanoga.toolbox.payment.PaymentTransaction;
import hu.lanoga.toolbox.payment.PaymentTransactionBuilder;
import hu.lanoga.toolbox.payment.PaymentTransactionService;
import hu.lanoga.toolbox.payment.exception.ToolboxPaymentGeneralException;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.session.LcuHelperSessionBean;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.user.User;
import hu.lanoga.toolbox.util.BrandUtil;
import hu.lanoga.toolbox.util.ToolboxAssert;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@JavaScript({
		"../../../default/assets/apple-pay-v1.js" // ,
// "https://pay.google.com/gp/p/js/pay.js", // TODO: IE 11 komp. gondok vannak e miatt a JS miatt
// "../../../default/assets/google-pay-v1.js"
})
@StyleSheet({ "../../../default/assets/apple-pay-v1.css" })
public class PaymentCustomerStartComponent extends VerticalLayout {
	
	private static final Striped<Lock> concurrentTransactionCheckLock = Striped.lock(8);

	private final PaymentManager paymentManager;

	protected PaymentTransactionBuilder paymentTransactionBuilder;

	private Integer targetTenantId;

	private final Set<Integer> allowedPaymentProviderIds;

	/**
	 * "erősebb", mint a allowedPaymentProviderIds (tehát akkor is kiszedi, ha amúgy ott engedve lenne)
	 */
	private final boolean disableIrlProviders;

	/**
	 * CodeStoreId, HorizontalLayout pár-t vár el a map
	 * ami azt takarja, hogy meg kell adnod a paymentProviderId-t, és a hozzá tartozó horizontalLayout-ot
	 */
	private final Map<Integer, HorizontalLayout> customLayoutMap;
	private VerticalLayout vlPageContent;

	private JavaScriptFunction applePayJsCallback;
	private JavaScriptFunction googlePayJsCallback;

	private boolean didChooseApplePay = false;
	private boolean didChooseGooglePay = false;

	private String emailFieldInitValue = null;
	private boolean isEmailFieldAlwaysRequired = false;
	private boolean allowEmailFieldEdit = true;
	private boolean simplePayDataProtectionConsentWasAlreadyGiven = false;

	// private final AbstractToolboxUI ui;

	private String orderInfoLabelStartsWithStr;
	
	private String startWarningHasOngoingParallel = I.trc("Caption", "Ehhez a rendeléshez már/még tartozik másik folyamatban lévő fizetési tranzakció! Ajánljuk, hogy először várja meg ez (vagy ezek) lezárulását (akár sikeres, akár sikertelen), új próbálkozásba csak utána kezdjen!");
	private String startWarningAlreadyHasSuccessful = I.trc("Caption", "Ehhez a rendeléshez már tartozik sikeres fizetési tranzakció (valószínűleg párhuzamosan másik ablakban, másik számítógépen indított és végigvitt)! Nincs értelme kétszer fizetnie, kérjük töltse újra az oldalt, hogy az aktuális állapot látszódjon!");

	public PaymentCustomerStartComponent() {
		this(null, null, false);
	}

	public PaymentCustomerStartComponent(final Set<Integer> allowedPaymentProviderIds) {
		this(null, allowedPaymentProviderIds, false);
	}

	public PaymentCustomerStartComponent(final boolean disableIrlProviders) {
		this(null, null, disableIrlProviders);
	}

	public PaymentCustomerStartComponent(
			final Map<Integer, HorizontalLayout> customLayout,
			final Set<Integer> allowedPaymentProviderIds) {
		this(customLayout, allowedPaymentProviderIds, false);
	}

	public PaymentCustomerStartComponent(
			final Map<Integer, HorizontalLayout> customLayout,
			final Set<Integer> allowedPaymentProviderIds,
			final boolean disableIrlProviders) {

		this.paymentManager = ApplicationContextHelper.getBean(PaymentManager.class);

		this.customLayoutMap = customLayout;
		this.allowedPaymentProviderIds = allowedPaymentProviderIds;
		this.disableIrlProviders = disableIrlProviders;

		this.orderInfoLabelStartsWithStr = I.trc("Caption", "Order") + ": ";

		// this.ui = ((AbstractToolboxUI) UI.getCurrent());

	}

	/**
	 * @param emailFieldInitValue
	 * @param isEmailFieldAlwaysRequired
	 * 		payment fajtától függetlenül, mindegyiknél (false esetén csak azoknál lesz kötelező az e-mail megadása, ahol a szolgáltató által elvárt, pl. SimplePay)
	 * @param allowEmailFieldEdit
	 */
	@SuppressWarnings("hiding")
	public void toggleEmailField(final String emailFieldInitValue, final boolean isEmailFieldAlwaysRequired, final boolean allowEmailFieldEdit) {

		this.emailFieldInitValue = emailFieldInitValue;
		this.isEmailFieldAlwaysRequired = isEmailFieldAlwaysRequired;
		this.allowEmailFieldEdit = allowEmailFieldEdit;

	}

	public boolean isSimplePayDataProtectionConsentWasAlreadyGiven() {
		return simplePayDataProtectionConsentWasAlreadyGiven;
	}

	public void setSimplePayDataProtectionConsentWasAlreadyGiven(boolean simplePayDataProtectionConsentWasAlreadyGiven) {
		this.simplePayDataProtectionConsentWasAlreadyGiven = simplePayDataProtectionConsentWasAlreadyGiven;
	}

	/**
	 * @param targetTenantId
	 * @param orderId
	 * @param customerUserId
	 * @param customerId
	 * @param amount
	 * @param currency
	 * @param returnToAfterUrl
	 * @param oldDeprecatedParam
	 * @param orderInfoLabel
	 * 
	 * @see #initLayout(int, String, Integer, String, BigDecimal, String, String, String)
	 */
	@Deprecated
	public void initLayout(
			@SuppressWarnings("hiding") final int targetTenantId, final String orderId, //
			final Integer customerUserId, final String customerId, //
			final BigDecimal amount, final String currency, //
			final String returnToAfterUrl, @SuppressWarnings("unused") @Deprecated final String oldDeprecatedParam, final String orderInfoLabel) {
		this.initLayout(targetTenantId, orderId, customerUserId, customerId, amount, currency, returnToAfterUrl, orderInfoLabel);
	}

	/**
	 * @param targetTenantId
	 * @param orderId
	 * @param customerUserId
	 * 		Toolbox {@link User} record-ra mutat (lehet null, de akkor legalább a másik lazább id legyen kitöltve)
	 * @param customerId
	 * 		lazább id (ha a keret nem Toolbox-os vagy még nincs userId stb.)
	 * @param amount
	 * @param currency
	 * @param returnToAfterUrl
	 * @param orderInfoLabel
	 * 		HTML label, de {@link Whitelist#basic()}-kel van sanitize-olva itt kijelzéshez, és 
	 * 		{@link Whitelist#none()}-nal {@link PaymentTransactionBuilder}-be rakás előtt 
	 * 		(ez utóbbinál 75 kar-ra le is van vágva már itt))
	 */
	public void initLayout(
			@SuppressWarnings("hiding") final int targetTenantId, final String orderId, //
			final Integer customerUserId, final String customerId, //
			final BigDecimal amount, final String currency, //
			final String returnToAfterUrl, final String orderInfoLabel) {

		this.targetTenantId = targetTenantId;

		// ---

		{
			final int p = UI.getCurrent().getPollInterval();
			ToolboxAssert.isTrue(p != -1 && p < 10000, "Please enable polling (10 sec or less)!");
		}

		// ---

		this.removeAllComponents();

		// ---

		final Label lblOrderInfo = new Label(this.orderInfoLabelStartsWithStr + Jsoup.clean(orderInfoLabel, Safelist.basic()));
		lblOrderInfo.addStyleName("wrap-text");
		lblOrderInfo.setWidth("100%");
		lblOrderInfo.addStyleName(ValoTheme.LABEL_BOLD);
		lblOrderInfo.setContentMode(ContentMode.HTML);
		this.addComponent(lblOrderInfo);

		ToolboxAssert.notNull(amount, "Amount is null (cannot proceed with payment)!");

		final BigDecimal amountScale0 = amount.setScale(0, RoundingMode.HALF_UP);
		final BigDecimal amountScale2 = amount.setScale(2, RoundingMode.HALF_UP);

		String strAmount = (amountScale0.compareTo(amountScale2) == 0 ? amountScale0 : amountScale2).toPlainString();

		try {

			final Locale loggedInUserLocale = I18nUtil.getLoggedInUserLocale();

			@SuppressWarnings("unused")
			final Currency currencyObject = Currency.getInstance(currency);

			if (loggedInUserLocale.getLanguage().equals(new Locale("hu").getLanguage())) {

				if ("HUF".equalsIgnoreCase(currency)) {
					strAmount += " Forint";
				} else {
					strAmount += " " + currency;
				}

			} else if (loggedInUserLocale.getLanguage().equals(new Locale("en").getLanguage())) {
				strAmount += " " + currency + " (" + currencyObject.getDisplayName(new Locale("en")) + ")";
			} else {
				strAmount += " " + currency;
			}

		} catch (final Exception e) {
			log.error("strAmount build failed (unknown currency?)", e);
		}

		final Label lblAmount = new Label(strAmount);
		lblAmount.addStyleName(ValoTheme.LABEL_H3);
		lblAmount.addStyleName(ValoTheme.LABEL_COLORED);
		lblAmount.setWidth("100%");
		this.addComponent(lblAmount);

		// this.addComponent(new Label()); // spacer

		// ---

		this.paymentTransactionBuilder = new PaymentTransactionBuilder();
		this.paymentTransactionBuilder.setGid(UUID.randomUUID().toString());
		this.paymentTransactionBuilder.setPaymentOperationType(ToolboxSysKeys.PaymentOperationType.PURCHASE);
		this.paymentTransactionBuilder.setOrderId(orderId);
		this.paymentTransactionBuilder.setCustomerUserId(customerUserId);
		this.paymentTransactionBuilder.setCustomerId(customerId != null ? customerId : customerUserId.toString());
		this.paymentTransactionBuilder.setOriginalAmount(amount);
		this.paymentTransactionBuilder.setOriginalCurrency(currency.toUpperCase());
		this.paymentTransactionBuilder.setStatus(ToolboxSysKeys.PaymentTransactionStatus.GATEWAY_INIT);

		// ---

		String afterUrl2 = BrandUtil.getRedirectUriHostFrontend() + returnToAfterUrl + "/IDX" + this.paymentTransactionBuilder.getGid();
		afterUrl2 = StringUtils.replace(afterUrl2, "//", "/");
		afterUrl2 = StringUtils.replace(afterUrl2, ":/", "://");

		this.paymentTransactionBuilder.setReturnUrl(afterUrl2);

		// ---

		this.paymentTransactionBuilder.setOrderInfoLabel(StringUtils.abbreviate(Jsoup.clean(orderInfoLabel, Safelist.none()), 75));

		// ---

		final List<CodeStoreItem> availablePaymentProviders;

		try {

			JdbcRepositoryManager.setTlTenantId(targetTenantId);

			if (this.allowedPaymentProviderIds != null) {
				availablePaymentProviders = this.paymentManager.getAvailablePaymentProviders(this.allowedPaymentProviderIds);
			} else {
				availablePaymentProviders = this.paymentManager.getAvailablePaymentProviders();
			}

		} finally {
			JdbcRepositoryManager.clearTlTenantId();
		}

		// ---

		if (this.disableIrlProviders) {
			availablePaymentProviders.removeIf(t -> t.getId().equals(ToolboxSysKeys.PaymentProvider.IRL));
		}

		// ---

		this.vlPageContent = new VerticalLayout();
		this.vlPageContent.setWidth("100%");
		this.vlPageContent.setMargin(false);
		this.setMargin(new MarginInfo(false, false, true, false));
		this.addComponent(this.vlPageContent);

		// ---

		this.initPaymentButtons(availablePaymentProviders);

	}

	private void initPaymentButtons(final List<CodeStoreItem> availablePaymentProviders) {

		final List<HorizontalLayout> listA = new ArrayList<>();
		final List<HorizontalLayout> listB = new ArrayList<>();
		final List<HorizontalLayout> listC = new ArrayList<>();

		for (final CodeStoreItem availablePaymentProviderCsi : availablePaymentProviders) {

			boolean addClickListenerToStartPayment = true;

			HorizontalLayout hlPaymentProviderRow = this.customLayoutMap != null ? this.customLayoutMap.get(availablePaymentProviderCsi.getId()) : null;

			String positionPriority = "C";

			if (hlPaymentProviderRow == null) {

				// ez a default, ha nem adott meg custom layout-ot

				hlPaymentProviderRow = new HorizontalLayout();
				hlPaymentProviderRow.setWidth("100%");
				hlPaymentProviderRow.setHeight("80px");
				hlPaymentProviderRow.setMargin(false);
				// hlPaymentProviderRow.addStyleName("wrap-text");
				hlPaymentProviderRow.addStyleName(ValoTheme.LAYOUT_WELL);
				hlPaymentProviderRow.addStyleName("grey-border");
				hlPaymentProviderRow.setId(availablePaymentProviderCsi.getId().toString());
				hlPaymentProviderRow.addStyleName("payment-logo-img-container-layout");

				hlPaymentProviderRow.setCaption(availablePaymentProviderCsi.getCaptionCaption());

				// az IRL payment egy külön saját dolog, amíg nem találunk ki jobb opciót, addig ez a régi dízájn marad rajta
				// a többi helyen már képekkel van megoldva

				if (availablePaymentProviderCsi.getId().equals(ToolboxSysKeys.PaymentProvider.IRL)) {

					// IRL = IN REAL LIFE, itt értsd bármi egyéb készpénz, helyszínen bemutatott utalvány stb.

					final Button btn = new Button(availablePaymentProviderCsi.getCaptionCaption());

					btn.setWidth("100%");
					btn.setHeight("100%");
					btn.addStyleName(ValoTheme.BUTTON_PRIMARY);
					btn.addStyleName(ValoTheme.BUTTON_BORDERLESS);

					hlPaymentProviderRow.addComponent(btn);
					hlPaymentProviderRow.setExpandRatio(btn, 0.5f);

				} else if (availablePaymentProviderCsi.getId().equals(ToolboxSysKeys.PaymentProvider.PAYU_APPLE_PAY)) {

					// ------------------------------------------------------------
					// ---APPLE-PAY------------------------------------------------
					// ------------------------------------------------------------

					positionPriority = "A";
					addClickListenerToStartPayment = false;

					// ---

					final HorizontalLayout hlInner = new HorizontalLayout();
					hlInner.setWidth("200px");
					hlInner.addStyleName("apple-pay-box");
					hlPaymentProviderRow.addComponent(hlInner);
					hlPaymentProviderRow.setComponentAlignment(hlInner, Alignment.MIDDLE_CENTER);

					// ---

					final StringBuilder sbJs = new StringBuilder();
					sbJs.append("apPaymentRequest = ");

					{

						final JSONObject joApPaymentRequest = new JSONObject();
						joApPaymentRequest.put("countryCode", "HU"); // nagybetűs!
						joApPaymentRequest.put("currencyCode", this.paymentTransactionBuilder.getOriginalCurrency());
						joApPaymentRequest.put("supportedNetworks", // https://developer.apple.com/documentation/apple_pay_on_the_web/applepaypaymentrequest/1916122-supportednetworks
								new JSONArray(Lists.newArrayList("masterCard", "visa", "maestro", "cartesBancaires", "discover", "eftpos", "electron", "interac", "jcb", "vPay")));
						joApPaymentRequest.put("merchantCapabilities", // https://developer.apple.com/documentation/apple_pay_on_the_web/applepaymerchantcapability
								new JSONArray(Lists.newArrayList("supports3DS")));

						final JSONObject joApPaymentRequestTotal = new JSONObject();

						ToolboxAssert.notNull(this.paymentTransactionBuilder.getOriginalAmount());
						joApPaymentRequestTotal.put("amount", this.paymentTransactionBuilder.getOriginalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());

						ToolboxAssert.notNull(this.paymentTransactionBuilder.getOrderId());
						// joApPaymentRequestTotal.put("label", paymentTransactionBuilder.getOrderId() + ", " + paymentTransactionBuilder.getOrderInfoLabel());
						joApPaymentRequestTotal.put("label", this.paymentTransactionBuilder.getOrderId()); // TODO: tisztázni

						joApPaymentRequest.put("total", joApPaymentRequestTotal);

						// try {
						// ToolboxAssert.notNull(paymentTransactionBuilder.getOrderId());
						// joApPaymentRequest.put("applicationData", Base64Utils.encodeToString(paymentTransactionBuilder.getOrderId().getBytes("UTF-8"))); // base64, custom field, can hold whatever we need later (orderId etc.) (bár hol lehet visszakapni?)
						// } catch (JSONException | UnsupportedEncodingException e) {
						// throw new ToolboxPaymentGeneralException("Apple Pay applicationData error", e);
						// }

						sbJs.append(joApPaymentRequest.toString());

					}

					sbJs.append("; showApplePayIfViable();");

					// ---

					this.applePayJsCallback = new JavaScriptFunction() {

						@Override
						public void call(final JsonArray arguments) {

							try {

								final String s = arguments.getString(0);

								if (s.equals("step1")) {

									try {

										log.debug("applePayJsCallback1");

										final String appleValidationUrlFromJs = arguments.getString(1);
										final String responseFromApple = ApplePayUtil.createSession(appleValidationUrlFromJs, true); // TODO: második param legyen per tenant alapon

										com.vaadin.ui.JavaScript.eval("apSession.completeMerchantValidation(" + responseFromApple + ")");

									} catch (final Exception e) {
										throw new ToolboxPaymentGeneralException("applePayJsCallback1 error!", e);
									}

								} else if (s.equals("step2")) {

									try {

										log.debug("applePayJsCallback2");

										final String applePayWalletFullToken = arguments.getObject(1).getObject("token").getObject("paymentData").toJson();

										PaymentCustomerStartComponent.this.didChooseApplePay = true;
										PaymentCustomerStartComponent.this.paymentTransactionBuilder.setWalletFullToken(applePayWalletFullToken);

										PaymentCustomerStartComponent.this.vlPageContent.removeAllComponents();

										// PaymentCustomerStartComponent.this.pushToFrontend();

										PaymentCustomerStartComponent.this.startPayment(ToolboxSysKeys.PaymentProvider.PAYU_APPLE_PAY);

									} catch (final Exception e) {
										throw new ToolboxPaymentGeneralException("applePayJsCallback2 error!", e);
									}

								}

							} catch (final Exception e) {

								if (e instanceof ToolboxPaymentGeneralException) {
									throw e;
								} else {
									throw new ToolboxPaymentGeneralException("applePayJsCallback error!", e);
								}

							}

						}

					};

					// ---

					Page.getCurrent().getJavaScript().addFunction("hu.lanoga.toolbox.vaadin.component.payment.PaymentCustomerStartComponent.applePayJsCallback", this.applePayJsCallback);
					com.vaadin.ui.JavaScript.eval(sbJs.toString());

					// ------------------------------------------------------------
					// ------------------------------------------------------------
					// ------------------------------------------------------------

				} else if (availablePaymentProviderCsi.getId().equals(ToolboxSysKeys.PaymentProvider.PAYU_GOOGLE_PAY)) {

					// ------------------------------------------------------------
					// ---GOOGLE-PAY-----------------------------------------------
					// ------------------------------------------------------------

					positionPriority = "B";
					addClickListenerToStartPayment = false;

					// ---

					final HorizontalLayout hlInner = new HorizontalLayout();
					hlInner.setWidth("240px");
					hlInner.addStyleName("google-pay-box");
					hlPaymentProviderRow.addComponent(hlInner);
					hlPaymentProviderRow.setComponentAlignment(hlInner, Alignment.MIDDLE_CENTER);

					// ---

					final StringBuilder sbJs = new StringBuilder();
					sbJs.append("gpTransInfo = ");

					{

						final JSONObject joGpPaymentRequest = new JSONObject();
						joGpPaymentRequest.put("totalPriceStatus", "FINAL"); // nagybetűs!
						joGpPaymentRequest.put("countryCode", "HU"); // nagybetűs!
						joGpPaymentRequest.put("currencyCode", this.paymentTransactionBuilder.getOriginalCurrency());

						joGpPaymentRequest.put("totalPriceLabel", SecurityUtil.getLoggedInUserTenantName() + ", " + this.paymentTransactionBuilder.getOrderId()); // TODO: tisztázni
						joGpPaymentRequest.put("totalPrice", this.paymentTransactionBuilder.getOriginalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());

						// try {
						// ToolboxAssert.notNull(paymentTransactionBuilder.getOrderId());
						// joApPaymentRequest.put("applicationData", Base64Utils.encodeToString(paymentTransactionBuilder.getOrderId().getBytes("UTF-8"))); // base64, custom field, can hold whatever we need later (orderId etc.) (bár hol lehet visszakapni?)
						// } catch (JSONException | UnsupportedEncodingException e) {
						// throw new ToolboxPaymentGeneralException("Apple Pay applicationData error", e);
						// }

						sbJs.append(joGpPaymentRequest.toString());

					}

					sbJs.append("; onGooglePayLoaded();");

					// ---

					this.googlePayJsCallback = new JavaScriptFunction() {

						@Override
						public void call(final JsonArray arguments) {

							try {

								log.debug("googlePayJsCallback"); // itt csak egy lépés van, ez az, ami az Apple Pay-nél a második

								final String googlePayWalletFullToken = arguments.getString(0);

								PaymentCustomerStartComponent.this.didChooseGooglePay = true;
								PaymentCustomerStartComponent.this.paymentTransactionBuilder.setWalletFullToken(googlePayWalletFullToken);

								PaymentCustomerStartComponent.this.vlPageContent.removeAllComponents();

								// PaymentCustomerStartComponent.this.pushToFrontend();

								PaymentCustomerStartComponent.this.startPayment(ToolboxSysKeys.PaymentProvider.PAYU_GOOGLE_PAY);

							} catch (final Exception e) {
								throw new ToolboxPaymentGeneralException("googlePayJsCallback error!", e);
							}

						}
					};

					// ---

					Page.getCurrent().getJavaScript().addFunction("hu.lanoga.toolbox.vaadin.component.payment.PaymentCustomerStartComponent.googlePayJsCallback", this.googlePayJsCallback);
					com.vaadin.ui.JavaScript.eval(sbJs.toString());

					// ------------------------------------------------------------
					// ------------------------------------------------------------
					// ------------------------------------------------------------

				} else {

					Image image = null;

					if (availablePaymentProviderCsi.getId().equals(ToolboxSysKeys.PaymentProvider.PAY_PAL)) {
						image = new Image(null, new ExternalResource(VaadinService.getCurrentRequest().getContextPath() + "/assets/paypal-logo-v1.png")); // TODO: ez szebb, jobban illik a stílushoz, de a felület szétcsúszik tőle /assets/paypal-logo-v2.png
						image.setAlternateText("PayPal");
					} else if (availablePaymentProviderCsi.getId().equals(ToolboxSysKeys.PaymentProvider.CIB)) {
						image = new Image(null, new ExternalResource(VaadinService.getCurrentRequest().getContextPath() + "/assets/cib-logo-30px.png"));
						image.setAlternateText("CIB");
					} else if (availablePaymentProviderCsi.getId().equals(ToolboxSysKeys.PaymentProvider.PAYU)) {
						image = new Image(null, new ExternalResource(VaadinService.getCurrentRequest().getContextPath() + "/assets/payu-2.svg"));
						image.setAlternateText("PayU");
					} else if (availablePaymentProviderCsi.getId().equals(ToolboxSysKeys.PaymentProvider.BARION)) {
						image = new Image(null, new ExternalResource(VaadinService.getCurrentRequest().getContextPath() + "/assets/barion-card-strip-intl.svg"));
						image.setAlternateText("Barion");
					} else if (availablePaymentProviderCsi.getId().equals(ToolboxSysKeys.PaymentProvider.SIMPLEPAY2)) {

						image = new Image(null, new ExternalResource(VaadinService.getCurrentRequest().getContextPath() + "/assets/simplepay_bankcard_logos_30px.png"));
						// image = new Image(null, new ExternalResource(VaadinService.getCurrentRequest().getContextPath() + "/assets/pay_button.png"));
						image.setAlternateText("SimplePay");

						// korábban itt a SIMPLEPAY2-nél lehet, hogy vizuálisan a BEEEMO projektre lett igazítva, ami lehet, hogy más projektben nem idális, tehát ezt szükség estén tisztázni kell még

						// final Button btnPay = new Button(I.trc("Button", "Pay (SimplePay)"));
						// btnPay.setWidth("");
						// btnPay.addStyleName(ValoTheme.BUTTON_LARGE);
						// btnPay.addStyleName(ValoTheme.BUTTON_PRIMARY);
						// btnPay.setDisableOnClick(true);
						//
						// hlPaymentProviderRow.addComponent(btnPay);
						// hlPaymentProviderRow.setComponentAlignment(btnPay, Alignment.MIDDLE_CENTER);

					}

					// ez az ellenőrzés az IRL payment miatt van itt

					if (image != null) {

						image.setHeightUndefined();
						image.addStyleName("payment-logo-img");
						image.addStyleName("max-width-400px");
						image.setId(availablePaymentProviderCsi.getId().toString()); // csak debug célra, így böngészőben is látszik a payment provider id is

						hlPaymentProviderRow.addComponent(image);
						hlPaymentProviderRow.setComponentAlignment(image, Alignment.MIDDLE_CENTER);
					}
				}

			}

			if (addClickListenerToStartPayment) {

				hlPaymentProviderRow.addLayoutClickListener(x -> {
					if (MouseButton.LEFT.equals(x.getButton())) {
						this.startPayment(availablePaymentProviderCsi.getId());
					}
				});

				for (int i = 0; i < hlPaymentProviderRow.getComponentCount(); i++) {
					final Component component = hlPaymentProviderRow.getComponent(i);

					if (component instanceof Button) {
						final Button btn = (Button) component;
						btn.setDisableOnClick(true);
						btn.addClickListener(x -> {
							try {
								this.startPayment(availablePaymentProviderCsi.getId());
							} catch (final ManualValidationException e) {
								btn.setEnabled(true);
								throw e;
							}

						});
					}
				}

			}

			if (positionPriority.equals("A")) {
				listA.add(hlPaymentProviderRow);
			} else if (positionPriority.equals("B")) {
				listB.add(hlPaymentProviderRow);
			} else {
				listC.add(hlPaymentProviderRow);
			}

		}

		listA.addAll(listB);
		listA.addAll(listC);

		for (final HorizontalLayout hl : listA) {
			this.vlPageContent.addComponent(hl);
		}

	}

	public void startPayment(final int paymentProviderCsiId) {

		if (this.isEmailFieldAlwaysRequired || 
				(paymentProviderCsiId == ToolboxSysKeys.PaymentProvider.SIMPLEPAY2 && (StringUtils.isBlank(this.emailFieldInitValue) || (!simplePayDataProtectionConsentWasAlreadyGiven)))) {

			final Window emailAndConstentDialog = new Window(I.trc("Title", "Email"));
			emailAndConstentDialog.setModal(true);
			emailAndConstentDialog.setWidth("400px");
			emailAndConstentDialog.setClosable(false);
			emailAndConstentDialog.setResizable(false);
			
			VerticalLayout vlEmailAndConstentDialog = new VerticalLayout();
			emailAndConstentDialog.setContent(vlEmailAndConstentDialog);

			TextField txtEmail = new TextField();
			txtEmail.setWidth("100%");
			txtEmail.setCaption(I.trc("Label", "Customer's email address (mandatory)"));
			// txtEmail.setCaption(I.trc("Label", "Customer's email (mandat. for some paym. types)"));
			txtEmail.setRequiredIndicatorVisible(true);
			txtEmail.setEnabled(this.allowEmailFieldEdit);
			vlEmailAndConstentDialog.addComponent(txtEmail);
			if (StringUtils.isNotBlank(this.emailFieldInitValue)) {
				txtEmail.setValue(this.emailFieldInitValue.trim());
			}

			CheckBox chkSimplePayDataProtectionConsent = new CheckBox();

			if (paymentProviderCsiId == ToolboxSysKeys.PaymentProvider.SIMPLEPAY2 && (!simplePayDataProtectionConsentWasAlreadyGiven)) {

				Label lblConsentDesc = new Label();
				lblConsentDesc.setWidth("100%");
				lblConsentDesc.setContentMode(ContentMode.HTML);
				lblConsentDesc.setValue(getSimplePay2UiDateShareConsentDescription());
				vlEmailAndConstentDialog.addComponent(lblConsentDesc);

				final Link linkToSimplePayDocs = new Link("http://simplepay.hu/vasarlo-aff", new ExternalResource("http://simplepay.hu/vasarlo-aff"));
				linkToSimplePayDocs.setTargetName("_blank");
				vlEmailAndConstentDialog.addComponent(linkToSimplePayDocs);

				chkSimplePayDataProtectionConsent.setCaption(I.trc("Caption", "Feltételek elfogadása"));
				chkSimplePayDataProtectionConsent.setValue(Boolean.FALSE);
				vlEmailAndConstentDialog.addComponent(chkSimplePayDataProtectionConsent);

			}

			final Button btnOk = new Button(I.trc("Button", "Ok"));
			btnOk.setWidth("100%");
			btnOk.setStyleName(ValoTheme.BUTTON_PRIMARY);
			btnOk.setDisableOnClick(true);
			btnOk.addClickListener(e -> {

				String emailStr = txtEmail.getValue();

				if (StringUtils.isBlank(emailStr)) {
					
					btnOk.setEnabled(true);
					
					throw new ManualValidationException(
							"Customer email is mandatory!",
							I.trc("Error", "Customer email is mandatory!"));
				}
				
				paymentTransactionBuilder.setCustomerEmail(emailStr.trim());

				if (paymentProviderCsiId == ToolboxSysKeys.PaymentProvider.SIMPLEPAY2 && (!simplePayDataProtectionConsentWasAlreadyGiven)) {

					if (!Boolean.TRUE.equals(chkSimplePayDataProtectionConsent.getValue())) {
						
						btnOk.setEnabled(true);
						
						throw new ManualValidationException(
								"Checking chkSimplePayDataProtectionConsent is mandatory (in case of SimplePay)!",
								I.trc("Notification", "Kérjük az összes jelölőnégyzet töltse ki!"));

					}

				}

				emailAndConstentDialog.close();
				startPaymentOuterInner(paymentProviderCsiId);
				
			});
			vlEmailAndConstentDialog.addComponent(btnOk);

			UI.getCurrent().addWindow(emailAndConstentDialog);

		} else {
			
			startPaymentOuterInner(paymentProviderCsiId);
			
		}

	}
	
	private void startPaymentOuterInner(final int paymentProviderCsiId) { // startPayment és startPaymentInner között, de később lett behozva, ezért a fura név

		this.paymentTransactionBuilder.setCustomerUserId(SecurityUtil.getLoggedInUser().getId());

		// ---

		log.debug("startPayment: " + paymentProviderCsiId);
		this.paymentTransactionBuilder.setPaymentProvider(paymentProviderCsiId);

		if (SecurityUtil.isLcuLevelUser()) {
			final String adHocLcuUserGid = ApplicationContextHelper.getBean(LcuHelperSessionBean.class).getLcuGid();
			log.info("startPayment, LCU GID: " + adHocLcuUserGid);
		}

		// ---

		// disable buttons

		final Iterator<Component> it = this.vlPageContent.iterator();
		while (it.hasNext()) {
			final Component component = it.next();
			component.setEnabled(false);
			component.addStyleName("opacity-50");
		}

		// ---

		// pushToFrontend();

		// ---

		this.startPaymentInner(paymentProviderCsiId);
		
		final PaymentTransaction paymentTransaction = this.paymentTransactionBuilder.createPaymentTransaction();
		final PaymentTransaction paymentTransaction2;
		
		Lock lock = null;
		
		try {

			lock = concurrentTransactionCheckLock.get(new JSONObject().put("tenantId", paymentTransaction.getTenantId()).put("orderId", paymentTransaction.getOrderId()).toString());
			lock.lock();

			this.startPaymentInner(paymentProviderCsiId, paymentTransaction);

			{

				try {

					Pair<Boolean, Boolean> pair = ApplicationContextHelper.getBean(PaymentTransactionService.class).shouldTransactionStartUiHalt(paymentTransaction.getOrderId());

					if (pair.getLeft().booleanValue() || pair.getRight().booleanValue()) {

						String confirmStr;

						if (pair.getRight().booleanValue()) {

							confirmStr = startWarningHasOngoingParallel;

							ConfirmDialog.show(UI.getCurrent(), I.trc("Caption", "Megerősítés"),
									confirmStr,
									I.trc("Button", "Fizetés folytatása"), I.trc("Button", "Megszakít"), new ConfirmDialog.Listener() {

										@Override
										public void onClose(final ConfirmDialog dialog) {

											if (dialog.isConfirmed()) {
												final PaymentTransaction paymentTransaction2 = startPaymentOuterInnerEnd(paymentTransaction);
												PaymentCustomerStartComponent.this.afterPayment(paymentTransaction2);
											} else {
												VaadinSession.getCurrent().setAttribute("paymentOnGoingParallelWarningAbortClick", Long.valueOf(System.currentTimeMillis()));
												Page.getCurrent().reload();
											}

										}
									});

							return;

						} else {

							confirmStr = startWarningAlreadyHasSuccessful;

							ConfirmDialog confirmDialog = ConfirmDialog.show(UI.getCurrent(), I.trc("Caption", "Megerősítés"),
									confirmStr,
									I.trc("Button", "Újratölt (frissít)"), I.trc("Button", "Mégse"), new ConfirmDialog.Listener() {

										@Override
										public void onClose(final ConfirmDialog dialog) {

											if (dialog.isConfirmed()) {
												Page.getCurrent().reload();
											}

										}
									});

							confirmDialog.getCancelButton().setVisible(false);

							return;

						}

					}

				} catch (Exception e) {
					log.error("shouldTransactionStartUiHalt() error!", e);
				}

				paymentTransaction2 = startPaymentOuterInnerEnd(paymentTransaction);

			}

		} finally {
			if (lock != null) {
				lock.unlock();
			}
		}
		
		this.afterPayment(paymentTransaction2);

	}

	private PaymentTransaction startPaymentOuterInnerEnd(PaymentTransaction paymentTransaction) {
		try {
			
			JdbcRepositoryManager.setTlTenantId(this.targetTenantId);

			paymentTransaction = this.paymentManager.doTransaction(paymentTransaction);

		} finally {
			JdbcRepositoryManager.clearTlTenantId();
		}
		return paymentTransaction;
	}

	/**
	 * ha kell még payment start előtt valami (egyedi esetben), 
	 * még a {@link PaymentTransaction} objektum build előtt
	 * 
	 * @param paymentProviderCsiId
	 */
	@SuppressWarnings("unused")
	protected void startPaymentInner(final int paymentProviderCsiId) {
		//
	}

	/**
	 * ha kell még payment start előtt valami (egyedi esetben), 
	 * de itt már megvan a {@link PaymentTransaction} objektum
	 * 
	 * @param paymentProviderCsiId
	 */
	@SuppressWarnings("unused")
	protected void startPaymentInner(final int paymentProviderCsiId, final PaymentTransaction paymentTransaction) {
		//
	}

	// private void pushToFrontend() {
	//
	// // TODO: nagyon ritkán lehet, hogy megakasztja a folyamatot... és a Page.getCurrent().open sem megy
	//
	// try {
	// this.getUI().push(); // azért, hogy a component.setEnabled(false); rögtön érvényesüljön
	// } catch (final Exception e) {
	// //
	// }
	//
	// }

	protected void afterPayment(final PaymentTransaction paymentTransaction) {

		if (this.didChooseApplePay) {

			this.afterPaymentWallet(paymentTransaction, true);

		} else if (this.didChooseGooglePay) {

			this.afterPaymentWallet(paymentTransaction, false);

		} else {

			if (StringUtils.isNotBlank(paymentTransaction.getPaymentProviderConfirmUiUrl())) {
				Page.getCurrent().open(paymentTransaction.getPaymentProviderConfirmUiUrl(), "_self");
			} else {
				Page.getCurrent().open(paymentTransaction.getReturnUrl(), "_self");
			}

		}

	}

	private void afterPaymentWallet(final PaymentTransaction paymentTransaction, final boolean isApplePay) {

		final Label lblApplePayStatus = new Label();
		lblApplePayStatus.setWidth("100%");
		this.vlPageContent.addComponent(lblApplePayStatus);

		this.vlPageContent.addComponent(new Label()); // spacer

		final Button btnNext = new Button(I.trc("Button", "Proceed"));
		btnNext.setIcon(VaadinIcons.ARROW_CIRCLE_RIGHT);
		btnNext.setWidth("100%");
		btnNext.setHeight("35px");
		this.vlPageContent.addComponent(btnNext);

		btnNext.addClickListener(x -> {
			Page.getCurrent().open(paymentTransaction.getReturnUrl(), "_self");
		});

		// ---

		if (paymentTransaction.getStatus().intValue() == ToolboxSysKeys.PaymentTransactionStatus.SUCCESS) {

			lblApplePayStatus.setValue(I.trc("Label", "Successful payment!"));
			lblApplePayStatus.addStyleName(ValoTheme.LABEL_SUCCESS);

			if (isApplePay) {
				// https://developer.apple.com/documentation/apple_pay_on_the_web/applepaysession/1778010-status_success
				com.vaadin.ui.JavaScript.eval("apSession.completePayment({\"status\": ApplePaySession.STATUS_SUCCESS})"); // itt kívételesen kézzel rakjuk össze a JSON object-et (mert kell egy JS konstans bele)
			}

		} else if (paymentTransaction.getStatus().intValue() == ToolboxSysKeys.PaymentTransactionStatus.FAILED) {

			lblApplePayStatus.setValue(I.trc("Label", "Failed payment!"));
			lblApplePayStatus.addStyleName(ValoTheme.LABEL_FAILURE);

			if (isApplePay) {
				// https://developer.apple.com/documentation/apple_pay_on_the_web/applepaysession/1778010-status_success
				com.vaadin.ui.JavaScript.eval("apSession.completePayment({\"status\": ApplePaySession.STATUS_FAILURE})"); // itt kívételesen kézzel rakjuk össze a JSON object-et (mert kell egy JS konstans bele)
			}

		} else {

			lblApplePayStatus.setValue(I.trc("Label", "Pending or uncertain payment!"));
			lblApplePayStatus.setIcon(VaadinIcons.QUESTION_CIRCLE);

			// PENDING eset... itt nem hívunk rá semmit (apSession), megvárjuk, amig lejárt a UI timeout

			// elvileg itt nem lehet a PaymentTransactionStatus.PENDING
			// amennyiben mégis zavar van, akkor megelégszünk a szokásos Apple Pay timeout-tal (ez kifejezi a usernek, hogy kavarás/bizonytalanság van)

		}

	}

	public String getOrderInfoLabelStartsWithStr() {
		return orderInfoLabelStartsWithStr;
	}

	public void setOrderInfoLabelStartsWithStr(String orderInfoLabelStartsWithStr) {
		this.orderInfoLabelStartsWithStr = orderInfoLabelStartsWithStr;
	}
	
	public static String getSimplePay2UiDateShareConsentDescription() {
		
		PaymentManager paymentManager = ApplicationContextHelper.getBean(PaymentManager.class);
		
		String langCode = I18nUtil.getLoggedInUserLocale().getLanguage().toUpperCase();
		JSONObject jsonObject = paymentManager.extractConfig1(ToolboxSysKeys.PaymentProvider.SIMPLEPAY2);

		String companyNameForDataProtectionConsent = Jsoup.clean(jsonObject.getString("companyNameForDataProtectionConsent"), Safelist.none());
		String companyAddressForDataProtectionConsent = Jsoup.clean(jsonObject.getString("companyAddressForDataProtectionConsent"), Safelist.none());
		String payingAcceptanceWebSiteForDataProtectionConsent = Jsoup.clean(jsonObject.getString("payingAcceptanceWebSiteForDataProtectionConsent"), Safelist.none()); // értsd: ahol most vagyunk (úgy megadva, ahogy a SimplePay-nél a cég megadta, csak a domain/gyökér)

		String strConsentDesc;

		if ("HU".equals(langCode)) {

			strConsentDesc = "Tudomásul veszem, hogy a(z) [Kereskedő cégneve] ([székhelye]) adatkezelő által a(z) [Fizetési Elfogadóhely webcíme] felhasználói adatbázisában tárolt alábbi személyes adataim átadásra kerülnek az OTP Mobil Kft., mint adatfeldolgozó részére.<br><br>"
					+ "Az adatkezelő által továbbított adatok köre az alábbi: [Kereskedő által továbbított adatok megnevezése]<br><br>"
					+ "Az adatfeldolgozó által végzett adatfeldolgozási tevékenység jellege és célja a SimplePay Adatkezelési tájékoztatóban, az alábbi linken tekinthető meg:";

			strConsentDesc = StringUtils.replaceOnce(strConsentDesc, "[Kereskedő cégneve]", companyNameForDataProtectionConsent);
			strConsentDesc = StringUtils.replaceOnce(strConsentDesc, "[székhelye]", companyAddressForDataProtectionConsent);
			strConsentDesc = StringUtils.replaceOnce(strConsentDesc, "[Fizetési Elfogadóhely webcíme]", payingAcceptanceWebSiteForDataProtectionConsent);
			strConsentDesc = StringUtils.replaceOnce(strConsentDesc, "[Kereskedő által továbbított adatok megnevezése]", "e-mail cím");

		} else {

			strConsentDesc = "I acknowledge the following personal data stored in the user account of [CompanyName] ([Company address]) in the user database of [Paying Acceptance Web site] will be handed over to OTP Mobil Ltd. and is trusted as data processor.<br><br>"
					+ "The data transferred by the data controller are the following: [data transmitted by the trader]<br><br>"
					+ "The nature and purpose of the data processing activity performed by the data processor in the SimplePay Privacy Policy can be found at the following link:";

			strConsentDesc = StringUtils.replaceOnce(strConsentDesc, "[Kereskedő cégneve]", companyNameForDataProtectionConsent);
			strConsentDesc = StringUtils.replaceOnce(strConsentDesc, "[székhelye]", companyAddressForDataProtectionConsent);
			strConsentDesc = StringUtils.replaceOnce(strConsentDesc, "[Fizetési Elfogadóhely webcíme]", payingAcceptanceWebSiteForDataProtectionConsent);
			strConsentDesc = StringUtils.replaceOnce(strConsentDesc, "[Kereskedő által továbbított adatok megnevezése]", "email address");

		}
		
		return strConsentDesc;
		
	}

	
	public void setStartWarningHasOngoingParallel(String startWarningHasOngoingParallel) {
		this.startWarningHasOngoingParallel = startWarningHasOngoingParallel;
	}

	public void setStartWarningAlreadyHasSuccessful(String startWarningAlreadyHasSuccessful) {
		this.startWarningAlreadyHasSuccessful = startWarningAlreadyHasSuccessful;
	}
	
}
