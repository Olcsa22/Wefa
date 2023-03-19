package hu.lanoga.toolbox.payment;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.teamunify.i18n.I;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.TextField;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.CrudOperation;
import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.json.jackson.StringValueDeserializer;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.repository.jdbc.View;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.vaadin.component.codemirror.CodeMirrorComponent;
import hu.lanoga.toolbox.vaadin.component.codemirror.CodeMirrorField;
import hu.lanoga.toolbox.vaadin.component.crud.CrudFormElementCollection;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridColumn;
import hu.lanoga.toolbox.vaadin.component.crud.SecondaryCrudFormElement;
import hu.lanoga.toolbox.vaadin.component.crud.ViewOnlyCrudFormElement;
import hu.lanoga.toolbox.vaadin.component.file.FileManagerComponentBuilder;
import hu.lanoga.toolbox.vaadin.component.file.FileManagerField;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentConfig implements ToolboxPersistable {

	public static class VaadinForm implements CrudFormElementCollection<PaymentConfig> {

		public ComboBox<Integer> paymentProvider = UiHelper.buildCodeStoreCombo(I.trc("Caption", "Payment provider"), ToolboxSysKeys.PaymentProvider.CODE_STORE_TYPE_ID, null);

		public CheckBox useGlobalConfig = new CheckBox(I.trc("Caption", "Global config"));

		public CodeMirrorField configJson = new CodeMirrorField(I.trc("Caption", "Config"), CodeMirrorComponent.Mode.JAVASCRIPT, CodeMirrorComponent.Theme.ELEGANT);
		public FileManagerField fileIds = new FileManagerField(I.trc("Caption", "Files"), new FileManagerComponentBuilder().setFileDescriptorSecurityType(ToolboxSysKeys.FileDescriptorSecurityType.AUTHENTICATED_USER).setMaxFileCount(10).setAddToCartEnabled(false));

		public CheckBox enabled = new CheckBox(I.trc("Caption", "Enabled"));

		@SecondaryCrudFormElement
		@ViewOnlyCrudFormElement
		public TextField id = new TextField(I.trc("Caption", "SysID"));

		@SecondaryCrudFormElement
		@ViewOnlyCrudFormElement
		public ComboBox<Integer> createdBy = UiHelper.buildUserCombo(I.trc("Caption", "Record created by"), null, false, true);

		@SecondaryCrudFormElement
		@ViewOnlyCrudFormElement
		public DateTimeField createdOn = new DateTimeField(I.trc("Caption", "Record created on"));

		@SecondaryCrudFormElement
		@ViewOnlyCrudFormElement
		public ComboBox<Integer> modifiedBy = UiHelper.buildUserCombo(I.trc("Caption", "Record modified by (last)"), null, false, true);

		@SecondaryCrudFormElement
		@ViewOnlyCrudFormElement
		public DateTimeField modifiedOn = new DateTimeField(I.trc("Caption", "Record modified on"));

		public VaadinForm() {
			
			this.paymentProvider.setEmptySelectionAllowed(false);
			
			// TODO: paymentProvider, ezt le kellene szűrni a project specifikusan engedélyezettekre (ne legyen ott a teljes codestoreitem lista) (de ez nehézkes, problémás... mi van, ha utólag szűkítés van stb.)

			this.useGlobalConfig.addValueChangeListener(x -> {

				if (Boolean.TRUE.equals(x.getValue())) {
					this.configJson.setVisible(false);
					this.fileIds.setVisible(false);
				} else {
					this.configJson.setVisible(true);
					this.fileIds.setVisible(true);
				}
				
				if (x.isUserOriginated()) {
					UiHelper.centerParentWindow(x.getComponent().getParent().getParent());
				}
			});
			
		}
		
		@Override
		public Void preBind(PaymentConfig modelObject, CrudOperation crudOperation) {
		
			if ((SecurityUtil.getLoggedInUserTenantId() < 5 && crudOperation.equals(CrudOperation.ADD)) || (modelObject.getTenantId() != null && modelObject.getTenantId() < 5)) {
				this.useGlobalConfig.setVisible(false); // az 1-es tenant-ba magába már értelmetlen useGlobalConfig=true elemet felvenni... az már nem tud "feljebb" mutatni...
			}
			
			// else {
				// csak super admin vehet fel useGlobalConfig=true értéket (jump.admin-ként)
				// this.useGlobalConfig.setEnabled(SecurityUtil.hasSuperAdminRole() && (crudOperation.equals(CrudOperation.ADD) || crudOperation.equals(CrudOperation.UPDATE)));
			//}
			
			return null;
		}
		
		@Override
		public Void preAction(PaymentConfig modelObject, CrudOperation crudOperation) {
			
			if (!this.useGlobalConfig.isVisible()) {
				modelObject.setUseGlobalConfig(Boolean.FALSE);
			}
			
			return null;
		}
		
	}

	@CrudGridColumn(translationMsg = "SysID")
	private Integer id;

	@ExporterIgnore
	private Integer tenantId;

	// ---
	
	// FIXME tenantId + paymentProvider unique constraint is kell

	@NotNull
	private Integer paymentProvider;

	@View
	@CrudGridColumn(translationMsg = "Payment provider", codeStoreTypeId = ToolboxSysKeys.PaymentProvider.CODE_STORE_TYPE_ID)
	private String paymentProviderCaption;

	// ---

	/**
	 * szükség estén ide is mehet fájl is (base64-ben a JSON mező egy JSON key value-ja...)
	 */
	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	private String configJson;

	/**
	 *  várjunk el fix fájlneveket (értsd: pl. kell egy privát kulcs valamely payment providerhez, 
	 *  akkor egy fájl mező lesz a config GUI-n, de elvárjuk, hogy pl. "priv.ppk" legyen a pontos fájlnév... 
	 *  ez alapján meg tudjuk találni a kódból is)
	 */
	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	private String fileIds;

	/**
	 * true estén az 1-es tenant beállítását fogja a payment engine használni 
	 * (ezt csak super_admin engdélyezheti) 
	 * (mj.: amennyiben az 1-es tenant-ban nincs ilyen config (vagy disabled), akkor nem fog működni jól...) 
	 */
	@NotNull
	@CrudGridColumn(translationMsg = "Global config")
	private Boolean useGlobalConfig;

	// ---

	@NotNull
	@CrudGridColumn(translationMsg = "Enabled")
	private Boolean enabled;

	// ---

	private Integer createdBy;

	@View
	@CrudGridColumn(translationMsg = "Record created by", searchComboMapId = "USERS", searchTargetFieldName = "createdBy")
	private String createdByCaption;

	@CrudGridColumn(translationMsg = "Record created on")
	private java.sql.Timestamp createdOn;

	private Integer modifiedBy;

	@View
	@CrudGridColumn(translationMsg = "Record modified by (last)", searchComboMapId = "USERS", searchTargetFieldName = "modifiedBy")
	private String modifiedByCaption;

	@CrudGridColumn(translationMsg = "Record modified on (last)")
	private java.sql.Timestamp modifiedOn;
}
