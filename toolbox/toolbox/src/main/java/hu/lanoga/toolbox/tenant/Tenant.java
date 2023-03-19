package hu.lanoga.toolbox.tenant;

import java.sql.Timestamp;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.repository.jdbc.Column;
import hu.lanoga.toolbox.vaadin.component.crud.SecondaryCrudFormElement;
import hu.lanoga.toolbox.vaadin.component.file.FileManagerComponentBuilder;
import hu.lanoga.toolbox.vaadin.component.file.FileManagerField;
import org.hibernate.validator.constraints.Length;

import com.teamunify.i18n.I;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.TextField;

import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.repository.jdbc.View;
import hu.lanoga.toolbox.vaadin.component.NumberOnlyTextField;
import hu.lanoga.toolbox.vaadin.component.crud.CrudFormElementCollection;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridColumn;
import hu.lanoga.toolbox.vaadin.component.crud.ViewOnlyCrudFormElement;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Tenant implements ToolboxPersistable {

	public static class VaadinForm implements CrudFormElementCollection<Tenant> {
	
		public TextField name = new TextField(I.trc("Caption", "Name"));
		public TextField companyName = new TextField(I.trc("Caption", "Company (full) name"));
		public TextField brand = new TextField(I.trc("Caption", "Brand"));
		public CheckBox enabled = new CheckBox(I.trc("Caption", "Enabled"));

		public TextField contactName = new TextField(I.trc("Caption", "Contact name"));
		public TextField email = new TextField(I.trc("Caption", "Contact email"));
		public TextField phone = new TextField(I.trc("Caption", "Contact phone"));

		@SecondaryCrudFormElement
		public TextField companyTaxNumber = new TextField(I.trc("Caption", "Company tax number"));
		@SecondaryCrudFormElement
		public TextField companyRegistrationNumber = new TextField(I.trc("Caption", "Company registration number"));
		@SecondaryCrudFormElement
		public TextField companyBankAccountNumber = new TextField(I.trc("Caption", "Company bank account number"));

		@SecondaryCrudFormElement
		public Label lblInfo = new Label(VaadinIcons.INFO_CIRCLE_O.getHtml() + " " + I.trc("Caption", "Company address"), ContentMode.HTML);
		@SecondaryCrudFormElement
		public TextField companyAddressCountry = new TextField(I.trc("Caption", "Country"));
		@SecondaryCrudFormElement
		public TextField companyAddressState = new TextField(I.trc("Caption", "State"));
		@SecondaryCrudFormElement
		public TextField companyAddressCounty = new TextField(I.trc("Caption", "County"));
		@SecondaryCrudFormElement
		public TextField companyAddressZipCode = new TextField(I.trc("Caption", "Zip code"));
		@SecondaryCrudFormElement
		public TextField companyAddressDetails = new TextField(I.trc("Caption", "Address details"));

		@SecondaryCrudFormElement(tabNum = 2)
		public TextArea note = new TextArea(I.trc("Caption", "Notes"));
		@SecondaryCrudFormElement(tabNum = 2)
		public TextField extraData1 = new TextField(I.trc("Caption", "Extra data 1"));
		@SecondaryCrudFormElement(tabNum = 2)
		public TextField extraData2 = new TextField(I.trc("Caption", "Extra data 2"));
		@SecondaryCrudFormElement(tabNum = 2)
		public TextField extraData3 = new TextField(I.trc("Caption", "Extra data 3"));
		@SecondaryCrudFormElement(tabNum = 2)
		public NumberOnlyTextField extraData4 = new NumberOnlyTextField(I.trc("Caption", "Extra data 4"), true, false);
		@SecondaryCrudFormElement(tabNum = 2)
		public TextField extraData5 = new TextField(I.trc("Caption", "Extra data 5"));
		@SecondaryCrudFormElement(tabNum = 2)
		public TextField extraData6 = new TextField(I.trc("Caption", "Extra data 6"));

		@SecondaryCrudFormElement(tabNum = 2)
		public TextField extraData7 = new TextField(I.trc("Caption", "Extra data 7"));
		@SecondaryCrudFormElement(tabNum = 2)
		public TextField extraData8 = new TextField(I.trc("Caption", "Extra data 8"));
		@SecondaryCrudFormElement(tabNum = 2)
		public TextField extraData9 = new TextField(I.trc("Caption", "Extra data 9"));
		@SecondaryCrudFormElement(tabNum = 2)
		public TextField extraData10 = new TextField(I.trc("Caption", "Extra data 10"));
		
		@SecondaryCrudFormElement(tabNum = 2)
		public FileManagerField fileIds = new FileManagerField(I.trc("Caption", "Files"), new FileManagerComponentBuilder()
				.setFileDescriptorSecurityType(ToolboxSysKeys.FileDescriptorSecurityType.SUPER_ADMIN_ONLY)
				.setMaxFileCount(100));

		@SecondaryCrudFormElement(tabNum = 2)
		public FileManagerField fileIds2 = new FileManagerField(I.trc("Caption", "Images"), new FileManagerComponentBuilder()
				.setFileDescriptorSecurityType(ToolboxSysKeys.FileDescriptorSecurityType.SUPER_ADMIN_ONLY)
				.setMaxFileCount(100));

		@ViewOnlyCrudFormElement
		@SecondaryCrudFormElement(tabNum = 3)
		public TextField id = new TextField(I.trc("Caption", "SysID"));
		@ViewOnlyCrudFormElement
		@SecondaryCrudFormElement(tabNum = 3)
		public ComboBox<Integer> createdBy = UiHelper.buildUserCombo(I.trc("Caption", "Record created by"), null, false, true);
		@ViewOnlyCrudFormElement
		@SecondaryCrudFormElement(tabNum = 3)
		public DateTimeField createdOn = new DateTimeField(I.trc("Caption", "Record created on"));
		@ViewOnlyCrudFormElement
		@SecondaryCrudFormElement(tabNum = 3)
		public ComboBox<Integer> modifiedBy = UiHelper.buildUserCombo(I.trc("Caption", "Record modified by (last)"), null, false, true);
		@ViewOnlyCrudFormElement
		@SecondaryCrudFormElement(tabNum = 3)
		public DateTimeField modifiedOn = new DateTimeField(I.trc("Caption", "Record modified on"));

		@Override
		public Void afterBind(Tenant modelObject, ToolboxSysKeys.CrudOperation crudOperation) {

			// ezek levannak rejtve tenant felvételekor, mert a tenant létrehozó script ezeket a mezőket nem veszi figyelembe
			// csak szerkesztéskor vihetők fel ezek az adatok
			if (crudOperation.equals(ToolboxSysKeys.CrudOperation.ADD)) {
				this.brand.setVisible(false);
				this.contactName.setVisible(false);
			}

			return null;
		}
	}

	@CrudGridColumn(translationMsg = "SysID")
	private Integer id;

	@CrudGridColumn(translationMsg = "Name (short/tenant)")
	@NotEmpty
	@Length(max = 50)
	private String name;
	
	@CrudGridColumn(translationMsg = "Brand")
	@Length(max = 50)
	private String brand;

	// ---
	
	@CrudGridColumn(translationMsg = "Company tax number", startHidden = true)
	@Length(max = 50)
	@View
	private String companyTaxNumber;

	@CrudGridColumn(translationMsg = "Company registration number", startHidden = true)
	@Length(max = 50)
	@View
	private String companyRegistrationNumber;

	@CrudGridColumn(translationMsg = "Company bank account number", startHidden = true)
	@Length(max = 50)
	@View
	private String companyBankAccountNumber;

	// ---

	@CrudGridColumn(translationMsg = "Company address country", startHidden = true)
	@Length(max = 2)
	@View
	private String companyAddressCountry;

	@CrudGridColumn(translationMsg = "Company address state", startHidden = true)
	@Length(max = 50)
	@View
	private String companyAddressState;

	@CrudGridColumn(translationMsg = "Company address county", startHidden = true)
	@Length(max = 50)
	@View
	private String companyAddressCounty;

	@CrudGridColumn(translationMsg = "Company address zip code", startHidden = true)
	@Length(max = 10)
	@View
	private String companyAddressZipCode;

	@CrudGridColumn(translationMsg = "Company address details", startHidden = true)
	@Length(max = 50)
	@View
	private String companyAddressDetails;

	// ---

	@CrudGridColumn(translationMsg = "Contact name")
	@View
	@Length(max = 50)
	private String contactName;

	@CrudGridColumn(translationMsg = "Contact email")
	@Length(max = 100)
	@Email
	@NotEmpty
	@View
	private String email;

	@CrudGridColumn(translationMsg = "Contact phone")
	@Length(max = 30)
	@View
	private String phone;

	// ---

	@Length(max = 500)
	@View
	private String note;

	@CrudGridColumn(translationMsg = "Extra data 1", startHidden = true)
	@Length(max = 100)
	@Column(name = "extra_data_1")
	@View
	private String extraData1;

	@CrudGridColumn(translationMsg = "Extra data 2", startHidden = true)
	@Length(max = 100)
	@Column(name = "extra_data_2")
	@View
	private String extraData2;

	@CrudGridColumn(translationMsg = "Extra data 3", startHidden = true)
	@Length(max = 100)
	@Column(name = "extra_data_3")
	@View
	private String extraData3;

	@CrudGridColumn(translationMsg = "Extra data 4", startHidden = true)
	@Column(name = "extra_data_4")
	@View
	private Integer extraData4;

	@CrudGridColumn(translationMsg = "Extra data 5", startHidden = true)
	@Length(max = 100)
	@Column(name = "extra_data_5")
	@View
	private String extraData5;
	
	@CrudGridColumn(translationMsg = "Extra data 6", startHidden = true)
	@Length(max = 100)
	@Column(name = "extra_data_6")
	@View
	private String extraData6;
	
	
	@CrudGridColumn(translationMsg = "Extra data 7", startHidden = true)
	@Length(max = 200)
	@Column(name = "extra_data_7")
	@View
	private String extraData7;
	@CrudGridColumn(translationMsg = "Extra data 8", startHidden = true)
	@Length(max = 200)
	@Column(name = "extra_data_8")
	@View
	private String extraData8;
	@CrudGridColumn(translationMsg = "Extra data 9", startHidden = true)
	@Length(max = 200)
	@Column(name = "extra_data_9")
	@View
	private String extraData9;
	@CrudGridColumn(translationMsg = "Extra data 10", startHidden = true)
	@Length(max = 200)
	@Column(name = "extra_data_10")
	@View
	private String extraData10;
	

	@Column(name = "file_ids")
	@View
	private String fileIds;

	@Column(name = "file_ids_2")
	@View
	private String fileIds2;

	@Column(name = "file_ids_3")
	@View
	private String fileIds3;

	@Column(name = "file_ids_4")
	@View
	private String fileIds4;

	@Column(name = "file_ids_5")
	@View
	private String fileIds5;

	@Column(name = "file_ids_6")
	@View
	private String fileIds6;
	
	@Column(name = "file_ids_7")
	@View
	private String fileIds7;

	@CrudGridColumn(translationMsg = "Company (full) name", startHidden = true)
	@View
	private String companyName;

	// ---

	@CrudGridColumn(translationMsg = "Enabled")
	@NotNull
	private Boolean enabled;

	private Boolean anonymizedDeleted;

	@ExporterIgnore
	private Integer createdBy;

	@CrudGridColumn(translationMsg = "Created on")
	private Timestamp createdOn;

	@ExporterIgnore
	private Integer modifiedBy;

	private Timestamp modifiedOn;
	
}
