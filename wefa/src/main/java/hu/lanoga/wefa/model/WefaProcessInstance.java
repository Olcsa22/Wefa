package hu.lanoga.wefa.model;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.teamunify.i18n.I;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.TextField;

import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.json.jackson.StringValueDeserializer;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.vaadin.component.JumpUrlCopyToClipboardButtonTextField;
import hu.lanoga.toolbox.vaadin.component.crud.CrudFormElementCollection;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridColumn;
import hu.lanoga.toolbox.vaadin.component.crud.SecondaryCrudFormElement;
import hu.lanoga.toolbox.vaadin.component.crud.ViewOnlyCrudFormElement;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// @FwCreateTable(versionNumber = 1005) // ki lett generálva és be lett hozva .sql fájlként azóta
public class WefaProcessInstance implements ToolboxPersistable {

	public static class VaadinForm implements CrudFormElementCollection<WefaProcessInstance> {

		public TextField processInstanceId = new TextField(I.trc("Caption", "Munkaf. példány azonosítója"));
		public TextField processDefinitionId = new TextField(I.trc("Caption", "Munkafolyamat def. azonosítója"));
		public TextField processDefinitionName = new TextField(I.trc("Caption", "Munkafolyamat def. neve"));

		public DateTimeField startTime = new DateTimeField(I.trc("Caption", "Munkafolyamat példány indítás ideje"));
		public DateTimeField endTime = new DateTimeField(I.trc("Caption", "Munkafolyamat példány vége"));

		@SecondaryCrudFormElement
		public TextField taskName = new TextField(I.trc("Caption", "Jelenlegi feladat"));
		@SecondaryCrudFormElement
		public TextField taskWorker = new TextField(I.trc("Caption", "Feladaton dolgozó"));

		@SecondaryCrudFormElement
		@ViewOnlyCrudFormElement
		public JumpUrlCopyToClipboardButtonTextField id = new JumpUrlCopyToClipboardButtonTextField(I.trc("Caption", "SysID (rekord)"), "process-instance-list");
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
	}

	@ExporterIgnore
	private Integer tenantId;

	@CrudGridColumn(translationMsg = "SysID (rekord)", startHidden = true)
	private Integer id;

	@CrudGridColumn(translationMsg = "Munkaf. példány azon.", columnExpandRatio = 0)
	private String processInstanceId;

	@CrudGridColumn(translationMsg = "Munkafolyamat def. azonosítója", columnExpandRatio = 0)
	private String processDefinitionId;

	@CrudGridColumn(translationMsg = "Munkafolyamat def. neve")
	private String processDefinitionName;

	// ---

	@CrudGridColumn(translationMsg = "Jelenlegi feladat")
	private String taskName;

	@CrudGridColumn(translationMsg = "Feladaton dolgozó", startHidden = true)
	private String taskWorker;

	// ---

	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	private String processVariables;

	@CrudGridColumn(translationMsg = "Indítás ideje")
	private Timestamp startTime;

	@CrudGridColumn(translationMsg = "Vége")
	private Timestamp endTime;

	@CrudGridColumn(translationMsg = "Lezárult")
	private Boolean isClosed;

	@CrudGridColumn(translationMsg = "Létrehozta", startHidden = true)
	private Integer createdBy;

	@CrudGridColumn(translationMsg = "Létrehozva", startHidden = true)
	private Timestamp createdOn;

	@CrudGridColumn(translationMsg = "Utoljára módosította", startHidden = true)
	private Integer modifiedBy;

	@CrudGridColumn(translationMsg = "Utoljára módosítva", startHidden = true)
	private Timestamp modifiedOn;

}
