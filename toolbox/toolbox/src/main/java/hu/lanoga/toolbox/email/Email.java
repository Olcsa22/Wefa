package hu.lanoga.toolbox.email;

import java.sql.Timestamp;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.teamunify.i18n.I;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.RichTextArea;
import com.vaadin.ui.TextField;

import hu.lanoga.toolbox.ToolboxSysKeys.CrudOperation;
import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.json.jackson.StringValueDeserializer;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.vaadin.component.NumberOnlyTextField;
import hu.lanoga.toolbox.vaadin.component.crud.CrudFormElementCollection;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridColumn;
import hu.lanoga.toolbox.vaadin.component.crud.ViewOnlyCrudFormElement;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Email implements ToolboxPersistable, ToolboxEmail {

	public static class VaadinForm implements CrudFormElementCollection<Email> {

		public TextField fromEmail = new TextField(I.trc("Caption", "From email"));
		public TextField toEmail = new TextField(I.trc("Caption", "To email"));
		public TextField subject = new TextField(I.trc("Caption", "Subject"));
		public RichTextArea body = new RichTextArea(I.trc("Caption", "Body"));
		public CheckBox isPlainText = new CheckBox(I.trc("Caption", "Plain text"));
		public NumberOnlyTextField status = new NumberOnlyTextField(I.trc("Caption", "Status"), false, false);
		public NumberOnlyTextField attempt = new NumberOnlyTextField(I.trc("Caption", "Attempt"), false, false);
		public TextField errorMessage = new TextField(I.trc("Caption", "Error message"));

		@ViewOnlyCrudFormElement
		public ComboBox<Integer> createdBy = UiHelper.buildUserCombo(I.trc("Caption", "Record created by"), null, false, true);
		@ViewOnlyCrudFormElement
		public DateTimeField createdOn = new DateTimeField(I.trc("Caption", "Record created on"));
		@ViewOnlyCrudFormElement
		public ComboBox<Integer> modifiedBy = UiHelper.buildUserCombo(I.trc("Caption", "Record modified by (last)"), null, false, true);
		@ViewOnlyCrudFormElement
		public DateTimeField modifiedOn = new DateTimeField(I.trc("Caption", "Record modified on"));

		public VaadinForm() {
			//
		}
		
		@Override
		public Void afterLayoutBuild(Email modelObject, CrudOperation crudOperation, Button crudFormOpButton, List<AbstractLayout> layouts) {
			body.setHeight("500px");
			return null;
		}

	}

	@ExporterIgnore
	private Integer tenantId;

	@CrudGridColumn(translationMsg = "SysID")
	private Integer id;

	@CrudGridColumn(translationMsg = "From email")
	private String fromEmail;

	/**
	 * ;"-vel elválasztva lehet több címet is
	 */
	@CrudGridColumn(translationMsg = "To email")
	private String toEmail;

	@CrudGridColumn(translationMsg = "Subject")
	private String subject;

	private String body;

	@CrudGridColumn(translationMsg = "Plain text")
	private Boolean isPlainText;

	@CrudGridColumn(translationMsg = "Status")
	private Integer status;

	@CrudGridColumn(translationMsg = "Attempt")
	private Integer attempt;

	@CrudGridColumn(translationMsg = "Error message")
	private String errorMessage;

	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	private String fileIds;

	@CrudGridColumn(translationMsg = "Record created by")
	private Integer createdBy;

	@CrudGridColumn(translationMsg = "Record created on")
	private Timestamp createdOn;

	@CrudGridColumn(translationMsg = "Record modified by")
	private Integer modifiedBy;

	@CrudGridColumn(translationMsg = "Record modified on")
	private Timestamp modifiedOn;

}
