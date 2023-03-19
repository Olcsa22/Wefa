package hu.lanoga.toolbox.tenant;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.Length;

import com.teamunify.i18n.I;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.TextField;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.vaadin.component.crud.CrudFormElementCollection;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridColumn;
import hu.lanoga.toolbox.vaadin.component.crud.ViewOnlyCrudFormElement;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantKeyValueSettings implements ToolboxPersistable {

	public static class VaadinForm implements CrudFormElementCollection<TenantKeyValueSettings> {

		// public ComboBox<Integer> tenantId = UiHelper.buildCombo1(I.trc("Caption", "Tenant"), ApplicationContextHelper.getBean(TenantService.class).findAll(), Tenant::getName);

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
			// this.tenantId.setReadOnly(true);
		}

		@Override
		public Void preBind(TenantKeyValueSettings modelObject, ToolboxSysKeys.CrudOperation crudOperation) {
			final List<String> tenantSettings = new ArrayList<>();

			for (Field field : ToolboxSysKeys.TenantKeyValueSettings.class.getDeclaredFields()) {
				if (!StringUtils.contains(field.getName(), "$")) {

					field.setAccessible(true);

					try {
						tenantSettings.add(field.get(String.class).toString());
					} catch (IllegalAccessException e) {
						//
					}

				}
			}

			this.kvKey = UiHelper.buildCombo2(I.trc("Caption", "Setting"), tenantSettings);
			this.kvKey.setEmptySelectionAllowed(false);

			return null;
		}

	}

	@CrudGridColumn(translationMsg = "SysID", columnExpandRatio = 0)
	private Integer id;

	@ExporterIgnore
	private Integer tenantId;

	@Length(max = 50)
	@NotEmpty
	@NotNull
	@CrudGridColumn(translationMsg = "Setting", columnExpandRatio = 0)
	private String kvKey;

	@Length(max = 50)
	@CrudGridColumn(translationMsg = "Value", columnExpandRatio = 1)
	private String kvValue;

	@NotNull
	private Boolean manualEditAllowed;

	/**
	 * Felületről manuálisan ne lehessen állítani ez mezőt soha (még manualEditAllowed=true esetén sem)! CLOB a DB-ben!
	 */
	private String kvLongText;

	@ExporterIgnore
	private Integer createdBy;

	private Timestamp createdOn;

	@ExporterIgnore
	private Integer modifiedBy;

	private Timestamp modifiedOn;

}
