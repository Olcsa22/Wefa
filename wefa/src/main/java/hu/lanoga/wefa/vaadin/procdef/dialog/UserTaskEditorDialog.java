package hu.lanoga.wefa.vaadin.procdef.dialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;
import com.teamunify.i18n.I;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.exception.ManualValidationException;
import hu.lanoga.toolbox.util.ToolboxGroovyHelper;
import hu.lanoga.toolbox.util.ToolboxGroovyHelper.ToolboxGroovyHelperException;
import hu.lanoga.toolbox.vaadin.component.codemirror.CodeMirrorComponent.Mode;
import hu.lanoga.toolbox.vaadin.component.codemirror.CodeMirrorComponent.Theme;
import hu.lanoga.toolbox.vaadin.component.codemirror.CodeMirrorField;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import hu.lanoga.wefa.SysKeys;
import hu.lanoga.wefa.SysKeys.ProccessJsonObjectTypes;
import hu.lanoga.wefa.util.WefaStringUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Itt is Groovy scriptet használunk több dologra, de ez saját "maszek" megoldásunk.
 * Az Activiti-ben egyébként is van Groovy script lehetőség, de nem interaktív (UI) taskok kapcsán.
 * 
 * @see ScriptTaskEditorDialog
 */
@Slf4j
public class UserTaskEditorDialog extends Window {

	private final String formKey;
	private final String processDefinitionId;
	private final String assignee;

	private final CodeMirrorField cmf1;
	private final CodeMirrorField cmf2;

	private final String cmf1ObjectKey;
	private final String cmf2ObjectKey;

	enum AssignType {
		USER, ROLE
	}

	private AssignType assignType = AssignType.USER;

	public UserTaskEditorDialog(final String processDefinitionId, final String elementId, final String assignee, final String formKey, final LinkedHashMap<String, String> processGroovyScript) {
		super();

		this.setCaption(I.trc("Caption", "Lépés (űrlap)"));

		this.processDefinitionId = processDefinitionId;
		this.assignee = assignee;
		this.formKey = StringUtils.defaultIfBlank(formKey, UUID.randomUUID().toString());

		// ---

		this.cmf1ObjectKey = SysKeys.ProccessJsonObjectTypes.PROC_DEF_STEP_DATA_MODEL + "-" + this.formKey;
		this.cmf2ObjectKey = SysKeys.ProccessJsonObjectTypes.PROC_DEF_STEP_FORM_MODEL + "-" + this.formKey;

		// ---

		log.debug(String.format("processDefinitionId init: %s", this.processDefinitionId));
		log.debug(String.format("assignee init: %s", this.assignee));
		log.debug(String.format("formKey init: %s", this.formKey));

		this.setWidth(800, Unit.PIXELS);
		this.setHeight(null);
		this.setModal(true);

		final VerticalLayout vlContent = new VerticalLayout();
		vlContent.setWidth(100, Unit.PERCENTAGE);
		vlContent.setHeight(null);
		vlContent.setSpacing(true);
		vlContent.setMargin(true);
		this.setContent(vlContent);
		
		// ---

		final String assignUserStr = I.trc("Caption", "Személy");
		final String assignRoleStr = I.trc("Caption", "Jogosultság");

		final RadioButtonGroup<String> rbgAssignType = new RadioButtonGroup<>();
		rbgAssignType.addStyleName(ValoTheme.OPTIONGROUP_HORIZONTAL);

		final List<String> assignTypes = new ArrayList<>();
		assignTypes.add(assignUserStr);
		assignTypes.add(assignRoleStr);

		rbgAssignType.setItems(assignTypes);
		vlContent.addComponent(rbgAssignType);

		final ComboBox<Integer> cmbUser = UiHelper.buildUserCombo(I.trc("Caption", "Felelős személy"), null, true, true);
		cmbUser.setEmptySelectionAllowed(false);
		cmbUser.setWidth("100%");
		vlContent.addComponent(cmbUser);

		HashSet<Integer> includeRoles = Sets.newHashSet(
				ToolboxSysKeys.UserAuth.ROLE_ADMIN_CS_ID,
				ToolboxSysKeys.UserAuth.ROLE_USER_CS_ID,
				SysKeys.UserAuth.ROLE_CLERK_CS_ID,
				SysKeys.UserAuth.ROLE_APPROVER_CS_ID);

		final ComboBox<Integer> cmbUserRole = UiHelper.buildCodeStoreCombo2(I.trc("Caption", "Jogosultság"), ToolboxSysKeys.UserAuth.CODE_STORE_TYPE_ID, includeRoles);
		cmbUserRole.setEmptySelectionAllowed(false);
		cmbUserRole.setWidth("100%");
		vlContent.addComponent(cmbUserRole);

		rbgAssignType.addValueChangeListener(x -> {
			if (StringUtils.equalsIgnoreCase(x.getValue(), assignUserStr)) {
				cmbUser.setVisible(true);
				this.assignType = AssignType.USER;
			} else {
				cmbUser.setVisible(false);
			}
			if (StringUtils.equalsIgnoreCase(x.getValue(), assignRoleStr)) {
				cmbUserRole.setVisible(true);
				this.assignType = AssignType.ROLE;
			} else {
				cmbUserRole.setVisible(false);
			}
		});

		if (this.assignee != null && this.assignee.contains(SysKeys.USER_ROLE_TAG)) {
			rbgAssignType.setValue(assignRoleStr);
		} else {
			rbgAssignType.setValue(assignUserStr);
		}

		if (AssignType.USER.equals(this.assignType) && this.assignee != null) {
			cmbUser.setValue(Integer.parseInt(this.assignee));
		}
		if (AssignType.ROLE.equals(this.assignType) && this.assignee != null) {
			cmbUserRole.setValue(Integer.parseInt(WefaStringUtil.getRoleIdByUserRoleTag(this.assignee)));
		}
		
		// ---

		{

			this.cmf1 = new CodeMirrorField(I.trc("Caption", "Lépés adatmodelje"), Mode.GROOVY, Theme.DARCULA);
			this.cmf2 = new CodeMirrorField(I.trc("Caption", "Lépés űrlap modellje"), Mode.GROOVY, Theme.DARCULA);

			this.cmf1.setValue(StringUtils.defaultIfEmpty(processGroovyScript.get(this.cmf1ObjectKey), ""));
			this.cmf2.setValue(StringUtils.defaultIfEmpty(processGroovyScript.get(this.cmf2ObjectKey), ""));

			vlContent.addComponent(this.cmf1);
			
			if (/*StringUtils.isBlank(cmf1.getValue()) &&*/ StringUtils.isNotBlank(processGroovyScript.get(ProccessJsonObjectTypes.PROC_DEF_DATA_MODEL))) {
				final Button btnCopy = new Button(I.trc("Button", "másolás a fő modellről"));
				btnCopy.setIcon(VaadinIcons.COPY_O);
				btnCopy.setStyleName(ValoTheme.BUTTON_SMALL);
				btnCopy.addClickListener(x -> {
					this.cmf1.setValue(processGroovyScript.get(ProccessJsonObjectTypes.PROC_DEF_DATA_MODEL));
					this.cmf1.initDialog();
				});
				vlContent.addComponent(btnCopy);
			}
			
			vlContent.addComponent(this.cmf2);
			
			if (/*StringUtils.isBlank(cmf2.getValue()) &&*/ StringUtils.isNotBlank(processGroovyScript.get(ProccessJsonObjectTypes.PROC_DEF_FORM_MODEL))) {
				final Button btnCopy = new Button(I.trc("Button", "másolás a fő űrlapról"));
				btnCopy.setIcon(VaadinIcons.COPY_O);
				btnCopy.setStyleName(ValoTheme.BUTTON_SMALL);
				btnCopy.addClickListener(x -> {
					this.cmf2.setValue(processGroovyScript.get(ProccessJsonObjectTypes.PROC_DEF_FORM_MODEL));
					this.cmf2.initDialog();
				});
				vlContent.addComponent(btnCopy);
			}

		}
		
		// ---

		final Button btnOk = new Button(I.trc("Button", "OK"));
		btnOk.setStyleName(ValoTheme.BUTTON_FRIENDLY);
		vlContent.addComponent(btnOk);

		btnOk.addClickListener(x -> {

			final String dataModelToBeSaved = this.cmf1.getValue();

			if (StringUtils.isBlank(dataModelToBeSaved)) {
				throw new ManualValidationException("Data model cannot be null/empty!", I.trc("Error", "Az adatmodell nem lehet null/üres!"));
			}

			try {
				ToolboxGroovyHelper.buildClass(dataModelToBeSaved);
			} catch (final ToolboxGroovyHelperException e) {
				throw new ManualValidationException("Syntax error!", I.trc("Error", "Hibás szintaxis!"));
			}

			final String formModelToBeSaved = this.cmf2.getValue();

			if (StringUtils.isBlank(formModelToBeSaved)) {
				throw new ManualValidationException("Form model cannot be null/empty!", I.trc("Error", "A form model/leíró nem lehet null/üres!"));
			}

			try {
				ToolboxGroovyHelper.buildClass(formModelToBeSaved);
			} catch (final ToolboxGroovyHelperException e) {
				log.error("Syntax error!", e);
				throw new ManualValidationException("Syntax error!", I.trc("Error", "Hibás szintaxis!"));
			}

			// ---

			String assignTag = "";
			if (AssignType.USER.equals(this.assignType)) {
				assignTag = cmbUser.getValue().toString();
			} else if (AssignType.ROLE.equals(this.assignType)) {
				assignTag = WefaStringUtil.getUserRoleTagByRoleId(Integer.parseInt(cmbUserRole.getValue().toString()));
			}
			JavaScript.eval("wfeUserTaskNodeBack('" + processDefinitionId + "', '" + elementId + "', '" + assignTag + "', '" + this.formKey + "');");

			processGroovyScript.put(this.cmf1ObjectKey, dataModelToBeSaved);
			processGroovyScript.put(this.cmf2ObjectKey, formModelToBeSaved);

			UserTaskEditorDialog.this.close();

		});

		// TODO: setRequiredIndicatorVisible + legalább manual validation ok nyomás előtt
	}
}
