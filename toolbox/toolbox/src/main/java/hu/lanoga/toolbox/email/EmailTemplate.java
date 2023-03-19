package hu.lanoga.toolbox.email;

import java.sql.Timestamp;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.teamunify.i18n.I;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.json.jackson.StringValueDeserializer;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.repository.jdbc.View;
import hu.lanoga.toolbox.vaadin.component.LangEditField;
import hu.lanoga.toolbox.vaadin.component.crud.CrudFormElementCollection;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridColumn;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailTemplate implements ToolboxPersistable {

	public static class VaadinForm implements CrudFormElementCollection<EmailTemplate> {

		public ComboBox<Integer> templCode = UiHelper.buildCodeStoreCombo(I.trc("Caption", "Template code"), ToolboxSysKeys.EmailTemplateType.CODE_STORE_TYPE_ID, null);
		public LangEditField subject = new LangEditField(I.trc("Caption", "Subject"), 100);
		public LangEditField templContent = new LangEditField(I.trc("Caption", "Content"), 20000, false, true);
		public CheckBox enabled = new CheckBox(I.trc("Caption", "Enabled"));

		public VaadinForm() {
			this.templCode.setEmptySelectionAllowed(false);
		}

	}

	@CrudGridColumn(translationMsg = "Identifier")
	private Integer id;

	@NotNull
	@ExporterIgnore
	private Integer templCode;

	@View
	@CrudGridColumn(translationMsg = "Template code")
	private String templCodeCaption;

	@NotEmpty
	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
//	@CrudGridColumn(translationMsg = "Template content")
	private String templContent;

	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	@CrudGridColumn(translationMsg = "Subject")
	private String subject;

	@CrudGridColumn(translationMsg = "Enabled")
	private Boolean enabled;

	@ExporterIgnore
	private Integer createdBy;

	private Timestamp createdOn;

	@ExporterIgnore
	private Integer modifiedBy;

	private Timestamp modifiedOn;
}
