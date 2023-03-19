package hu.lanoga.toolbox.codestore;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.teamunify.i18n.I;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.TextField;
import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.json.jackson.StringValueDeserializer;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.repository.jdbc.View;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.vaadin.component.LangEditField;
import hu.lanoga.toolbox.vaadin.component.crud.CrudFormElementCollection;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridColumn;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class CodeStoreItem implements ToolboxPersistable {

	public static class VaadinForm implements CrudFormElementCollection<CodeStoreItem> {

		public ComboBox<Integer> codeStoreTypeId = UiHelper.buildCombo1(I.trc("Caption", "Code store type"), ApplicationContextHelper.getBean(CodeStoreTypeService.class).findAll(), CodeStoreType::getCaptionCaption);
		public LangEditField caption = new LangEditField(I.trc("Caption", "Caption"), 100);
		public TextField command = new TextField(I.trc("Caption", "Command"));
		public CheckBox enabled = new CheckBox(I.trc("Caption", "Enabled"));

		public VaadinForm() {
			this.codeStoreTypeId.setEmptySelectionAllowed(false);
			this.codeStoreTypeId.setReadOnly(true);

			// jelen pillanatba, ha nem enabled valami hibát fog okozni közel minden rendszerben
			// ha lesz projekt ahol ezt engedélyezni kell, akkor meg kell gondolni
			this.enabled.setReadOnly(true);

			this.command.setReadOnly(true);

		}

		@Override
		public Void afterLayoutBuild(CodeStoreItem modelObject, ToolboxSysKeys.CrudOperation crudOperation, Button crudFormOpButton, List<AbstractLayout> layouts) {
			if (crudOperation.equals(ToolboxSysKeys.CrudOperation.ADD)) {
				this.enabled.setValue(true);
			}

			return null;
		}
	}

	@CrudGridColumn(translationMsg = "Identifier")
	private Integer id;

	@NotNull
	@ExporterIgnore
	private Integer codeStoreTypeId;

	@View
	@CrudGridColumn(translationMsg = "Code store type")
	private String codeStoreTypeCaption;

	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	@ExporterIgnore
	@NotEmpty
	private String caption;

	@View
	@CrudGridColumn(translationMsg = "Caption")
	private String captionCaption;

	@NotNull
	@CrudGridColumn(translationMsg = "Enabled")
	private Boolean enabled;

	@CrudGridColumn(translationMsg = "Command")
	private String command;

	@ExporterIgnore
	private Integer createdBy;

	private Timestamp createdOn;

	@ExporterIgnore
	private Integer modifiedBy;

	private Timestamp modifiedOn;

}
