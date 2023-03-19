package hu.lanoga.toolbox.payment;

import java.math.BigDecimal;
import java.util.UUID;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Sets;
import com.teamunify.i18n.I;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.json.jackson.StringValueDeserializer;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.repository.jdbc.Column;
import hu.lanoga.toolbox.repository.jdbc.View;
import hu.lanoga.toolbox.vaadin.component.NumberOnlyTextField;
import hu.lanoga.toolbox.vaadin.component.codemirror.CodeMirrorComponent;
import hu.lanoga.toolbox.vaadin.component.codemirror.CodeMirrorField;
import hu.lanoga.toolbox.vaadin.component.crud.CrudFormElementCollection;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridColumn;
import hu.lanoga.toolbox.vaadin.component.crud.SecondaryCrudFormElement;
import hu.lanoga.toolbox.vaadin.component.crud.ViewOnlyCrudFormElement;
import hu.lanoga.toolbox.vaadin.component.file.FileManagerComponentBuilder;
import hu.lanoga.toolbox.vaadin.component.file.FileManagerField;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import hu.lanoga.toolbox.validation.ValidationUtil;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentTransaction implements ToolboxPersistable {

	PaymentTransaction( // TODO: biztosan kellenek itt mégegyszer a validációs annotációk?
			@NotNull @Length(max = 150) final String gid, final Integer referredTransactionId,
			@NotNull @Length(max = 150) final String orderId, @Length(max = 75) final String orderInfoLabel, final String orderDetails,
			final Integer customerUserId, @NotNull @Length(max = 150) final String customerId, @Length(max = 100) final String customerEmail,
			@NotNull final BigDecimal originalAmount, @Length(min = 3, max = 3) @NotNull final String originalCurrency,
			@NotNull final Integer paymentProvider, @NotNull final Integer paymentOperationType, final String providerSpecificInput,
			@Length(max = 1000) final String returnUrl,
			@NotNull final Integer status,
			final Integer additionalDepositStageCount, final Integer additionalDepositStageFinished,
			final Boolean manualStatusChange,
			final String walletFullToken,
			final String meta1, final String meta2) {

		this.gid = UUID.fromString(gid).toString(); // ezzel itt is validáljuk, hogy UUID formátum-e
		this.referredTransactionId = referredTransactionId;

		this.customerUserId = customerUserId;
		this.customerId = customerId;
		this.customerEmail = customerEmail;

		this.originalAmount = originalAmount;
		this.originalCurrency = originalCurrency;

		this.orderId = orderId;
		this.orderInfoLabel = orderInfoLabel;
		this.orderDetails = orderDetails;

		this.paymentProvider = paymentProvider;
		this.paymentOperationType = paymentOperationType;
		this.providerSpecificInput = providerSpecificInput;

		this.returnUrl = returnUrl;

		this.status = status;

		this.additionalDepositStageCount = additionalDepositStageCount;
		this.additionalDepositStageFinished = additionalDepositStageFinished;

		this.manualStatusChange = manualStatusChange;

		this.walletFullToken = walletFullToken;

		this.meta1 = meta1;
		this.meta2 = meta2;

		ValidationUtil.validateObject(this);

	}

	public PaymentTransaction() {
		// Jackson needs it
	}

	public static class VaadinForm implements CrudFormElementCollection<PaymentTransaction> {

		@ViewOnlyCrudFormElement
		public TextField gid = new TextField(I.trc("Caption", "GID"));

		@ViewOnlyCrudFormElement
		public TextField id = new TextField(I.trc("Caption", "SysID"));

		@ViewOnlyCrudFormElement
		public TextField referredTransactionId = new TextField(I.trc("Caption", "Referred transaction record"));

		@ViewOnlyCrudFormElement
		public TextField orderId = new TextField(I.trc("Caption", "Order ID"));

		@ViewOnlyCrudFormElement
		public TextArea orderInfoLabel = new TextArea(I.trc("Caption", "Order info label"));

		@ViewOnlyCrudFormElement
		public TextField customerUserId = new TextField(I.trc("Caption", "Customer user"));

		@ViewOnlyCrudFormElement
		public TextField customerId = new TextField(I.trc("Caption", "Customer ID"));

		@ViewOnlyCrudFormElement
		public TextField customerEmail = new TextField(I.trc("Caption", "Customer email"));

		@ViewOnlyCrudFormElement
		public TextField lcuGid = new TextField(I.trc("Caption", "LCU GID"));

		@ViewOnlyCrudFormElement
		public ComboBox<Integer> paymentProvider = UiHelper.buildCodeStoreCombo(I.trc("Caption", "Payment provider"), ToolboxSysKeys.PaymentProvider.CODE_STORE_TYPE_ID, null);

		@ViewOnlyCrudFormElement
		public TextField rawId = new TextField(I.trc("Caption", "Raw ID (payment prov.)"));

		@ViewOnlyCrudFormElement
		public ComboBox<Integer> paymentOperationType = UiHelper.buildCodeStoreCombo(I.trc("Caption", "Payment operation type"), ToolboxSysKeys.PaymentOperationType.CODE_STORE_TYPE_ID, null);

		@ViewOnlyCrudFormElement
		public TextField originalCurrency = new TextField(I.trc("Caption", "Original currency"));

		@ViewOnlyCrudFormElement
		public NumberOnlyTextField originalAmount = new NumberOnlyTextField(I.trc("Caption", "Original amount"), true, 10, 2);

		public ComboBox<Integer> status = UiHelper.buildCodeStoreCombo(I.trc("Caption", "Status"),
				ToolboxSysKeys.PaymentTransactionStatus.CODE_STORE_TYPE_ID,
				Sets.newHashSet(ToolboxSysKeys.PaymentTransactionStatus.GATEWAY_INIT,
						ToolboxSysKeys.PaymentTransactionStatus.PENDING)); // FIXME: read-nél ott kellene lennie!

		// ---

		@SecondaryCrudFormElement
		@ViewOnlyCrudFormElement
		public CodeMirrorField orderDetails = new CodeMirrorField(I.trc("Caption", "Order details"), CodeMirrorComponent.Mode.JAVASCRIPT, CodeMirrorComponent.Theme.ELEGANT);

		@SecondaryCrudFormElement
		@ViewOnlyCrudFormElement
		public TextField backOfficeNotes = new TextField(I.trc("Caption", "Back office notes"));

		@SecondaryCrudFormElement
		@ViewOnlyCrudFormElement
		public FileManagerField fileIds = new FileManagerField(I.trc("Caption", "Documents"), new FileManagerComponentBuilder().setFileDescriptorSecurityType(ToolboxSysKeys.FileDescriptorSecurityType.AUTHENTICATED_USER).setMaxFileCount(999).setIsSelectionAllowed(true).setDownloadEnabled(true).setAddToCartEnabled(false));

		// ---

		@SecondaryCrudFormElement(tabNum = 2)
		@ViewOnlyCrudFormElement
		public CodeMirrorField providerSpecificInput = new CodeMirrorField(I.trc("Caption", "Provider specific input"), CodeMirrorComponent.Mode.JAVASCRIPT, CodeMirrorComponent.Theme.ELEGANT);

		@SecondaryCrudFormElement(tabNum = 2)
		@ViewOnlyCrudFormElement
		public CodeMirrorField providerSpecificProcessingData = new CodeMirrorField(I.trc("Caption", "Provider specific processing data"), CodeMirrorComponent.Mode.JAVASCRIPT, CodeMirrorComponent.Theme.ELEGANT);

		@SecondaryCrudFormElement(tabNum = 2)
		@ViewOnlyCrudFormElement
		public CodeMirrorField providerSpecificOutput = new CodeMirrorField(I.trc("Caption", "Provider specific output"), CodeMirrorComponent.Mode.JAVASCRIPT, CodeMirrorComponent.Theme.ELEGANT);

		// ---

		@SecondaryCrudFormElement(tabNum = 3)
		@ViewOnlyCrudFormElement
		public TextField paymentProviderConfirmUiUrl = new TextField(I.trc("Caption", "Payment provider confirm UI URL"));

		@SecondaryCrudFormElement(tabNum = 3)
		@ViewOnlyCrudFormElement
		public TextField returnUrl = new TextField(I.trc("Caption", "Return URL"));

		// ---

		@SecondaryCrudFormElement(tabNum = 3)
		@ViewOnlyCrudFormElement
		public ComboBox<Integer> createdBy = UiHelper.buildUserCombo(I.trc("Caption", "Record created by"), null, false, true);

		@SecondaryCrudFormElement(tabNum = 3)
		@ViewOnlyCrudFormElement
		public DateTimeField createdOn = new DateTimeField(I.trc("Caption", "Record created on"));

		@SecondaryCrudFormElement(tabNum = 3)
		@ViewOnlyCrudFormElement
		public ComboBox<Integer> modifiedBy = UiHelper.buildUserCombo(I.trc("Caption", "Record modified by (last)"), null, false, true);

		@SecondaryCrudFormElement(tabNum = 3)
		@ViewOnlyCrudFormElement
		public DateTimeField modifiedOn = new DateTimeField(I.trc("Caption", "Record modified on"));

		// ---
		
		@SecondaryCrudFormElement(tabNum = 3)
		private CheckBox statusCheckDisabledPoll = new CheckBox(I.trc("Caption", "Status check disabled poll"));

		@SecondaryCrudFormElement(tabNum = 3)
		@ViewOnlyCrudFormElement
		public DateTimeField lastStatusCheckRequestOn = new DateTimeField(I.trc("Caption", "Last status check request"));

		@SecondaryCrudFormElement(tabNum = 3)
		@ViewOnlyCrudFormElement
		public TextField lastStatusCheckReservedFor = new TextField(I.trc("Caption", "Last status check reserved for"));

		// ---

		public VaadinForm() {
			this.status.setEmptySelectionAllowed(false);
		}

	}

	@CrudGridColumn(translationMsg = "SysID")
	private Integer id;

	@NotNull
	@Length(max = 150)
	@CrudGridColumn(translationMsg = "GID", startHidden = true)
	private String gid;

	@ExporterIgnore
	private Integer tenantId;

	// --------------

	/**
	 * pl. refund esetén az eredeti (amit refund-olunk) tranzackióra mutat... (annak id mezőjére)
	 */
	@CrudGridColumn(translationMsg = "Referred transaction record (SysID)")
	private Integer referredTransactionId;

	// --------------

	@NotNull
	@Length(max = 150)
	@CrudGridColumn(translationMsg = "Customer ID")
	private String customerId;

	@CrudGridColumn(translationMsg = "Customer user")
	private Integer customerUserId;

	@Email
	@Length(max = 100)
	@CrudGridColumn(translationMsg = "Customer email")
	private String customerEmail;

	@CrudGridColumn(translationMsg = "LCU GID")
	private String lcuGid;

	// --------------

	@NotNull
	@Length(max = 150)
	@CrudGridColumn(translationMsg = "Order ID")
	private String orderId;

	/**
	 * info, amit a customer lát a payment provider oldalán (átirányítás után)
	 */
	@Length(max = 75)
	@CrudGridColumn(translationMsg = "Order info label", startHidden = true)
	private String orderInfoLabel;

	/**
	 * opcionális, részletes adatok a rendelésről 
	 * (alkalmazás függő, hogy mire van használva)
	 */
	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	private String orderDetails;

	// --------------

	/**
	 * SimplePay, PayPal stb.
	 */
	@NotNull
	private Integer paymentProvider;

	@View
	@CrudGridColumn(translationMsg = "Payment provider", codeStoreTypeId = ToolboxSysKeys.PaymentProvider.CODE_STORE_TYPE_ID, searchTargetFieldName = "paymentProvider")
	private String paymentProviderCaption;

	// --------------

	/**
	 * payment provider API/SDK által adott ID 
	 * (értsd: amit a PayPal stb. ad/generál) 
	 * (későbbi status kérésekhez fontos például)
	 */
	@Length(max = 500)
	@CrudGridColumn(translationMsg = "Raw ID (payment prov.)", startHidden = true)
	private String rawId;

	// --------------

	@NotNull
	private Integer paymentOperationType;

	@View
	@CrudGridColumn(translationMsg = "Payment operation type", codeStoreTypeId = ToolboxSysKeys.PaymentOperationType.CODE_STORE_TYPE_ID, searchTargetFieldName = "paymentOperationType")
	private String paymentOperationTypeCaption;

	// --------------

	@Length(min = 3, max = 3)
	@NotNull
	@CrudGridColumn(translationMsg = "Original currency")
	private String originalCurrency;

	@NotNull
	@CrudGridColumn(translationMsg = "Original amount")
	private BigDecimal originalAmount;

	// --------------

	/**
	 * @see ToolboxSysKeys.PaymentTransactionStatus
	 */
	@NotNull
	private Integer status;

	/**
	 * @see ToolboxSysKeys.PaymentTransactionStatus
	 */
	@View
	@CrudGridColumn(translationMsg = "Status", codeStoreTypeId = ToolboxSysKeys.PaymentTransactionStatus.CODE_STORE_TYPE_ID, searchTargetFieldName = "status")
	private String statusCaption;

	// --------------

	/**
	 * fájlok a tranzikció kapcsán (ritka, de lehet, hogy pár payment provider kapcsán kell valamire)
	 */
	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	private String fileIds;

	/**
	 * speciális input adat, ami csak egyes ritka payment provider-ek kapcsán kell
	 */
	// @CrudGridColumn(translationMsg = "Special input")
	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	private String providerSpecificInput;

	/**
	 * speciális egyéb/köztes adat, ami csak egyes ritka payment provider-ek kapcsán kell
	 */
	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	private String providerSpecificProcessingData;

	/**
	 * speciális ouput adat, ami csak egyes ritka payment provider-ek kapcsán kell, csak pár alkalmazásban stb.
	 */
	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	private String providerSpecificOutput;

	// --------------

	/**
	 * PayPal stb. URL, ahol jováhagyja a vásárló a fizetést (itt írja be a kártyaadatoakt stb.)
	 */
	@Length(max = 1000)
	@CrudGridColumn(translationMsg = "Payment provider confirm UI URL", startHidden = true)
	private String paymentProviderConfirmUiUrl;

	/**
	 * customer GUI redirect
	 */
	@Length(max = 1000)
	@CrudGridColumn(translationMsg = "Return URL", startHidden = true)
	private String returnUrl;

	// --------------
	
	/**
	 * egyes payment fajtáknál tilos poll-ozni a statust, ilyenkor az ő hívásukra várunk
	 * 
	 * @see PaymentTransactionCheckStatusScheduler
	 * @see ServerToServerNotificationReceiverController
	 */
	@CrudGridColumn(translationMsg = "Status check disabled poll", startHidden = true)
	private Boolean statusCheckDisabledPoll;

	/**
	 * ez csak az automata háttérjob-ra értendő
	 */
	// @CrudGridColumn(translationMsg = "Last status check request", startHidden = true)
	private java.sql.Timestamp lastStatusCheckRequestOn;

	/**
	 * az automata háttérjob-ra értendő, számára fontos
	 */
	// @CrudGridColumn(translationMsg = "Last status check reserved for", startHidden = true)
	private String lastStatusCheckReservedFor;

	// --------------

	private Integer additionalDepositStageCount;

	private Integer additionalDepositStageFinished;

	// --------------

	/**
	 * belső extra információ admin/ügyintéző számára (backoffice felületen)
	 */
	@Length(max = 500)
	@CrudGridColumn(translationMsg = "Back office notes")
	private String backOfficeNotes;

	/**
	 * manuális status váltás kell-e/felmerülhet-e (ennél a tranzakciónál) 
	 * (pl. készpénzes esetében lehet ilyen, amikor bekerül a kasszába a pénz, 
	 * akkor a backoffiec felüelten átbillenti fizetett-re) 
	 */
	@CrudGridColumn(translationMsg = "Manual status change", startHidden = true)
	private Boolean manualStatusChange;

	// --------------

	private Integer createdBy;

	@View
	@CrudGridColumn(translationMsg = "Record created by", startHidden = true)
	private String createdByCaption;

	@CrudGridColumn(translationMsg = "Record created on")
	private java.sql.Timestamp createdOn;

	private Integer modifiedBy;

	@View
	@CrudGridColumn(translationMsg = "Record modified by")
	private String modifiedByCaption;

	@CrudGridColumn(translationMsg = "Record modified on (last)")
	private java.sql.Timestamp modifiedOn;

	// --------------

	/**
	 * beeemo projektben volt például használva, 
	 * itt a rendelés fizikai állapota (összekészítés alatt stb.)
	 */
	@Length(max = 200)
	@Column(name = "meta_1")
	private String meta1;

	@Length(max = 200)
	@Column(name = "meta_2")
	private String meta2;

	// --------------

	/**
	 * még nem decryptelt token (Apple Pay token)
	 */
	@View
	private String walletFullToken;

}
