package hu.lanoga.toolbox.codestore;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.teamunify.i18n.I;
import com.vaadin.ui.CheckBox;
import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.json.jackson.StringValueDeserializer;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.repository.jdbc.View;
import hu.lanoga.toolbox.vaadin.component.LangEditField;
import hu.lanoga.toolbox.vaadin.component.crud.CrudFormElementCollection;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridColumn;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.sql.Timestamp;

@Getter
@Setter
public class CodeStoreType implements ToolboxPersistable {

	public static class VaadinForm implements CrudFormElementCollection<CodeStoreType> {
		public LangEditField caption = new LangEditField(I.trc("Caption", "Caption"), 100);
		public CheckBox expandable = new CheckBox(I.trc("Caption", "Expandable"));
		public CheckBox enabled = new CheckBox(I.trc("Caption", "Enabled"));

		public VaadinForm() {

			// jelen pillanatba, ha nem enabled valami hibát fog okozni közel minden rendszerben
			// ha lesz projekt ahol ezt engedélyezni kell, akkor meg kell gondolni
			this.enabled.setEnabled(false);
		}
	}

	@CrudGridColumn(translationMsg = "Identifier")
	private Integer id;

	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	@ExporterIgnore
	private String caption;

	@View
	@CrudGridColumn(translationMsg = "Caption")
	private String captionCaption;

	@NotNull
	@CrudGridColumn(translationMsg = "Expandable")
	private Boolean expandable;

	@NotNull
	@CrudGridColumn(translationMsg = "Enabled")
	private Boolean enabled;

	@ExporterIgnore
	private Integer createdBy;

	private Timestamp createdOn;

	@ExporterIgnore
	private Integer modifiedBy;

	private Timestamp modifiedOn;

}
