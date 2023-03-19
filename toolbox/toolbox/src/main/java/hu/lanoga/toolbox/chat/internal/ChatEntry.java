package hu.lanoga.toolbox.chat.internal;

import java.sql.Timestamp;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
import hu.lanoga.toolbox.repository.jdbc.View;
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
public class ChatEntry implements ToolboxPersistable {
	
	public static class VaadinForm implements CrudFormElementCollection<ChatEntry> {
		
		public TextArea messageText = new TextArea(I.trc("Caption", "Message text"));
		
		@ViewOnlyCrudFormElement
		public ComboBox<Integer> createdBy = UiHelper.buildUserCombo(I.trc("Caption", "Record created by"), null, false, true);
		
		@ViewOnlyCrudFormElement
		public DateTimeField createdOn = new DateTimeField(I.trc("Caption", "Record created on"));
		
		@ViewOnlyCrudFormElement
		public CheckBox seen = new CheckBox(I.trc("Caption", "Enabled"));
		
		@SecondaryCrudFormElement(tabNum = 2)
		public FileManagerField fileIds = new FileManagerField(I.trc("Caption", "Files"), new FileManagerComponentBuilder()
				.setFileDescriptorSecurityType(ToolboxSysKeys.FileDescriptorSecurityType.READ_AUTHENTICATED_USER_MODIFY_ADMIN_OR_CREATOR)
				.setMaxFileCount(10));
		
		@SecondaryCrudFormElement(tabNum = 3)
		public ComboBox<Integer> targetType = UiHelper.buildCodeStoreCombo(I.trc("Caption", "Target type"), ToolboxSysKeys.ChatTargetType.CODE_STORE_TYPE_ID, null);

		@SecondaryCrudFormElement(tabNum = 3)
		public TextField targetValue = new TextField(I.trc("Caption", "Target"));
		
		@SecondaryCrudFormElement(tabNum = 4)
		@ViewOnlyCrudFormElement
		public TextField id = new TextField(I.trc("Caption", "SysID"));
		
		@SecondaryCrudFormElement(tabNum = 4)
		@ViewOnlyCrudFormElement
		public ComboBox<Integer> modifiedBy = UiHelper.buildUserCombo(I.trc("Caption", "Record modified by"), null, false, true);
		
		@SecondaryCrudFormElement(tabNum = 4)
		@ViewOnlyCrudFormElement
		public DateTimeField modifiedOn = new DateTimeField(I.trc("Caption", "Record modified on"));
		
		public VaadinForm() {
			//
		}

	}

	@ExporterIgnore
	private Integer tenantId;
	
	@CrudGridColumn(translationMsg = "SysID")
	private Integer id;

	@NotNull
	private Integer targetType;
	
	@View
	@CrudGridColumn(translationMsg = "Target type", codeStoreTypeId = ToolboxSysKeys.ChatTargetType.CODE_STORE_TYPE_ID, searchTargetFieldName = "targetType", startHidden = true)
	private String targetTypeCaption;
	
	@NotNull
	@CrudGridColumn(translationMsg = "Target")
	private String targetValue;
	
	@NotNull
	@NotEmpty
	@Length(max = 3000)
	private String messageText;
	
	@View
	@CrudGridColumn(translationMsg = "Message text")
	private String messageTextEllipsis;
	
	@CrudGridColumn(translationMsg = "Seen", startHidden = true)
	private Boolean seen;
	
	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	private String fileIds;

	private Integer createdBy;
	
	@View
	@CrudGridColumn(translationMsg = "Record created by", searchComboMapId = "USERS", searchTargetFieldName = "createdBy")
	private String createdByCaption;

	@CrudGridColumn(translationMsg = "Record created on")
	private Timestamp createdOn;

	private Integer modifiedBy;

	private Timestamp modifiedOn;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ChatEntry [tenantId=");
		builder.append(tenantId);
		builder.append(", id=");
		builder.append(id);
		builder.append(", targetType=");
		builder.append(targetType);
		builder.append(", targetTypeCaption=");
		builder.append(targetTypeCaption);
		builder.append(", targetValue=");
		builder.append(targetValue);
		builder.append(", messageTextEllipsis=");
		builder.append(messageTextEllipsis);
		builder.append(", seen=");
		builder.append(seen);
		builder.append(", fileIds=");
		builder.append(fileIds);
		builder.append(", createdBy=");
		builder.append(createdBy);
		builder.append(", createdByCaption=");
		builder.append(createdByCaption);
		builder.append(", createdOn=");
		builder.append(createdOn);
		builder.append(", modifiedBy=");
		builder.append(modifiedBy);
		builder.append(", modifiedOn=");
		builder.append(modifiedOn);
		builder.append("]");
		return builder.toString();
	}
	
}
