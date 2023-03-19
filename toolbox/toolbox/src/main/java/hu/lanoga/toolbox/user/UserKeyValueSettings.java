package hu.lanoga.toolbox.user;

import com.teamunify.i18n.I;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.TextField;
import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.repository.jdbc.Table;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.vaadin.component.crud.CrudFormElementCollection;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridColumn;
import hu.lanoga.toolbox.vaadin.component.crud.ViewOnlyCrudFormElement;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Table(name = "auth_user_key_value_settings")
public class UserKeyValueSettings implements ToolboxPersistable { // TODO: itt kivételesen többes számban van a név... (átnevezni alter SQL-lel, ha könnyen lehet postgres-ben)

	public static class VaadinForm implements CrudFormElementCollection<UserKeyValueSettings> {

		public ComboBox<Integer> userId = UiHelper.buildUserCombo(I.trc("Caption", "Choose user"), ApplicationContextHelper.getBean(UserService.class).findAll(), false, true);

		public ComboBox<String> kvKey;

		public TextField kvValue = new TextField(I.trc("Caption", "Value"));

		public CheckBox manualEditAllowed = new CheckBox(I.trc("Caption", "Manual edit allowed"));

		@ViewOnlyCrudFormElement
		public TextField id = new TextField(I.trc("Caption", "SysID"));
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
		public Void preBind(UserKeyValueSettings modelObject, ToolboxSysKeys.CrudOperation crudOperation) {
			final List<String> userSettings = new ArrayList<>();

			for (Field field : ToolboxSysKeys.UserKeyValueSettings.class.getDeclaredFields()) {
				if (!StringUtils.contains(field.getName(),"$")) {

					field.setAccessible(true);

					try {
						userSettings.add(field.get(String.class).toString());
					} catch (IllegalAccessException e) {
						//
					}

				}
			}

			this.kvKey = UiHelper.buildCombo2(I.trc("Caption", "Setting"), userSettings);
			this.kvKey.setEmptySelectionAllowed(false);

			return null;
		}

		@Override
		public Void afterBind(UserKeyValueSettings modelObject, ToolboxSysKeys.CrudOperation crudOperation) {
			this.userId.setEnabled(false);

			return null;
		}
	}

	@CrudGridColumn(translationMsg = "SysID",  columnExpandRatio = 0)
	@ExporterIgnore
	private Integer id;

	@ExporterIgnore
	private Integer tenantId;

	@ExporterIgnore
	@NotNull
	private Integer userId;

	@Length(max = 50)
	@NotEmpty
	@NotNull
	@CrudGridColumn(translationMsg = "Setting")
	private String kvKey;

	@Length(max = 50)
	@NotEmpty
	@CrudGridColumn(translationMsg = "Value")
	private String kvValue;
	
	@NotNull
	private Boolean manualEditAllowed;
	
	/**
	 * Felületről manuálisan ne lehessen állítani ez mezót soha (még manualEditAllowed=true esetén sem)! CLOB a DB-ben!
	 */
	private String kvLongText;

	@ExporterIgnore
	private Integer createdBy;

	private Timestamp createdOn;

	@ExporterIgnore
	private Integer modifiedBy;

	private Timestamp modifiedOn;

}
