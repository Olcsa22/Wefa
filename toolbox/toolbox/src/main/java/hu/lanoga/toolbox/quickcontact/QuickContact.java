package hu.lanoga.toolbox.quickcontact;

import com.teamunify.i18n.I;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.CrudOperation;
import hu.lanoga.toolbox.exception.ManualValidationException;
import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.vaadin.component.JumpUrlCopyToClipboardButtonTextField;
import hu.lanoga.toolbox.vaadin.component.crud.CrudFormElementCollection;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridColumn;
import hu.lanoga.toolbox.vaadin.component.crud.SecondaryCrudFormElement;
import hu.lanoga.toolbox.vaadin.component.crud.ViewOnlyCrudFormElement;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.sql.Timestamp;

@Getter
@Setter
public class QuickContact implements ToolboxPersistable {

	public static class VaadinForm implements CrudFormElementCollection<QuickContact> {

		public TextField companyName = new TextField(I.trc("Caption", "Company name"));
		public TextField contactName = new TextField(I.trc("Caption", "Name"));
		public TextField phoneNumber = new TextField(I.trc("Caption", "Phone number"));
		public TextField email = new TextField(I.trc("Caption", "Email"));
		public TextField country = new TextField(I.trc("Caption", "Country"));
		public TextField city = new TextField(I.trc("Caption", "City"));
		public TextField cityDetails = new TextField(I.trc("Caption", "Address (details)"));

		@SecondaryCrudFormElement
		public TextField extraFieldName1 = new TextField(I.trc("Caption", "Extra field 1 (name)"));
		@SecondaryCrudFormElement
		public TextField extraFieldValue1 = new TextField(I.trc("Caption", "Extra field 1 (value)"));
		@SecondaryCrudFormElement
		public TextField extraFieldName2 = new TextField(I.trc("Caption", "Extra field 2 (name)"));
		@SecondaryCrudFormElement
		public TextField extraFieldValue2 = new TextField(I.trc("Caption", "Extra field 2 (value)"));
		@SecondaryCrudFormElement
		public TextField extraFieldName3 = new TextField(I.trc("Caption", "Extra field 3 (name)"));
		@SecondaryCrudFormElement
		public TextField extraFieldValue3 = new TextField(I.trc("Caption", "Extra field 3 (value)"));
		@SecondaryCrudFormElement
		public TextArea note = new TextArea(I.trc("Caption", "Notes"));

		@SecondaryCrudFormElement(tabNum = 2)
		@ViewOnlyCrudFormElement
		public TextField origin = new TextField(I.trc("Caption", "Origin"));

		@SecondaryCrudFormElement(tabNum = 2)
		@ViewOnlyCrudFormElement
		public CheckBox isSent = new CheckBox(I.trc("Caption", "Email notification (sales) was sent"));

		@SecondaryCrudFormElement(tabNum = 2)
		@ViewOnlyCrudFormElement
		public JumpUrlCopyToClipboardButtonTextField id = new JumpUrlCopyToClipboardButtonTextField(I.trc("Caption", "SysID"), "quick-contacts");

		@SecondaryCrudFormElement(tabNum = 2)
		@ViewOnlyCrudFormElement
		public ComboBox<Integer> createdBy = UiHelper.buildUserCombo(I.trc("Caption", "Record created by"), null, false, true);

		@SecondaryCrudFormElement(tabNum = 2)
		@ViewOnlyCrudFormElement
		public DateTimeField createdOn = new DateTimeField(I.trc("Caption", "Record created on"));

		public VaadinForm() {
			this.contactName.setRequiredIndicatorVisible(true);
		}

		@Override
		public Void manualValidation(QuickContact modelObject, CrudOperation crudOperation) {

			if (StringUtils.isBlank(modelObject.getContactName()) || (StringUtils.isBlank(modelObject.getEmail()) && StringUtils.isBlank(modelObject.getPhoneNumber()))) {
				throw new ManualValidationException("missing values: name, email and/or phone", I.trc("Notification", "Please fill these fields: name, email and/or phone number!"));
			}

			return null;

		}
		
		@Override
		public Void preAction(QuickContact modelObject, CrudOperation crudOperation) {
			if (ToolboxSysKeys.CrudOperation.ADD.equals(crudOperation)) {
				modelObject.setOrigin(I.trc("Value", "manual add"));
			}
			return null;
		}

	}

	@CrudGridColumn(translationMsg = "SysID", startHidden = true, columnExpandRatio = 0)
	private Integer id;

	@ExporterIgnore
	private Integer tenantId;

	@Size(max = 50)
	@CrudGridColumn(translationMsg = "Company")
	private String companyName;

	@NotEmpty
	@Size(max = 50)
	@CrudGridColumn(translationMsg = "Name")
	private String contactName;

	@Size(max = 50)
	@CrudGridColumn(translationMsg = "Phone")
	private String phoneNumber;

	@Email
	@CrudGridColumn(translationMsg = "Email")
	private String email;

	@Length(max = 100)
	@CrudGridColumn(translationMsg = "Country")
	private String country;

	@Length(max = 50)
	@CrudGridColumn(translationMsg = "City")
	private String city;

	@Length(max = 100)
	@CrudGridColumn(translationMsg = "Address (details)")
	private String cityDetails;
	
	@Length(max = 100)
	@CrudGridColumn(translationMsg = "Origin")
	private String origin;

	// ---

	@Length(max = 50)
	@CrudGridColumn(translationMsg = "Extra field 1 (name)", startHidden = true)
	private String extraFieldName1;

	@Length(max = 100)
	@CrudGridColumn(translationMsg = "Extra field 1 (value)", startHidden = true)
	private String extraFieldValue1;

	@Length(max = 50)
	@CrudGridColumn(translationMsg = "Extra field 2 (name)", startHidden = true)
	private String extraFieldName2;

	@Length(max = 100)
	@CrudGridColumn(translationMsg = "Extra field 2 (value)", startHidden = true)
	private String extraFieldValue2;

	@Length(max = 50)
	@CrudGridColumn(translationMsg = "Extra field 3 (name)", startHidden = true)
	private String extraFieldName3;

	@Length(max = 100)
	@CrudGridColumn(translationMsg = "Extra field 3 (value)", startHidden = true)
	private String extraFieldValue3;

	// ---

	@Length(max = 500)
	@CrudGridColumn(translationMsg = "Notes")
	private String note;

	@CrudGridColumn(translationMsg = "Email notification (sales) was sent")
	private Boolean isSent;

	@ExporterIgnore
	private Integer createdBy;

	@CrudGridColumn(translationMsg = "Record created on", startHidden = true)
	private Timestamp createdOn;

	@ExporterIgnore
	private Integer modifiedBy;

	@CrudGridColumn(translationMsg = "Record modified on (last)", startHidden = true)
	private Timestamp modifiedOn;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("QuickContact [tenantId=");
		builder.append(tenantId);
		builder.append(", contactName=");
		builder.append(contactName);
		builder.append("]");
		return builder.toString();
	}
	
}
