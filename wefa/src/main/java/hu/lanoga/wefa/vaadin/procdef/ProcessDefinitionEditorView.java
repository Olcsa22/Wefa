package hu.lanoga.wefa.vaadin.procdef;

import java.util.UUID;

import org.activiti.engine.repository.ProcessDefinition;
import org.apache.commons.lang3.StringUtils;

import com.teamunify.i18n.I;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.exception.ManualValidationException;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.util.ToolboxGroovyHelper;
import hu.lanoga.toolbox.vaadin.component.codemirror.CodeMirrorComponent.Mode;
import hu.lanoga.toolbox.vaadin.component.codemirror.CodeMirrorComponent.Theme;
import hu.lanoga.toolbox.vaadin.component.codemirror.CodeMirrorField;
import hu.lanoga.wefa.SysKeys.ProccessJsonObjectTypes;
import hu.lanoga.wefa.service.ActivitiHelperService;
import hu.lanoga.wefa.vaadin.MainUI;
import lombok.extern.slf4j.Slf4j;

/**
 * WF ({@link ProcessDefinition}) BPMN szerkesztés...
 */
@Slf4j
public class ProcessDefinitionEditorView extends HorizontalLayout implements View {


	private final ActivitiHelperService activitiHelperService;

	private VerticalLayout vlArea;
	private ProcessDefinitionEditorComponent procDefEditorComponent;

	private VerticalLayout vlRightPanel;

	private TextField txtProcDefKey;
	private TextField txtProcDefVer;
	private TextField txtProcDefName;

	private Button btnCancel;
	private Button btnSave;

	private CodeMirrorField cmf1;
	private CodeMirrorField cmf2;
	private CodeMirrorField cmf3;
	private CodeMirrorField cmf4;

	public ProcessDefinitionEditorView() {
		super();

		this.activitiHelperService = ApplicationContextHelper.getBean(ActivitiHelperService.class);

		this.setSizeFull();
		this.setSpacing(false);
	}

	@Override
	public void enter(final ViewChangeEvent event) {

		this.removeAllComponents();

		// ---

		final String processDefinitonId = event.getParameters();

		{

			this.vlArea = new VerticalLayout();
			this.vlArea.setSizeFull();
			this.vlArea.setMargin(true);
			this.vlArea.setId("procdefeditor-" + UUID.randomUUID());

			this.procDefEditorComponent = new ProcessDefinitionEditorComponent(this.vlArea.getId(), processDefinitonId, false);

			// mj.: a processDefinitonId alapján a ProcessDefinitionEditorComponent megkeresi magát a ProcDefiniton objektumot
			// onnan már el lehet kérni getterekkel mindent
			// kicsit nyakatekert így (de most elmegy)
			// lényeg, hogy a ProcessDefinitionEditorComponent-ben van a fő "műsor"

			// ---

			// elvileg minden process definitonnél van: id, key, ver és name is
			// minden key+ver eredményez egy id-t
			// a name meg csak extra, szöveges (de nem feltétlenül egyedi talán) // TODO: tisztázni, jobb magyarázat

			// TODO: az id is legyen itt read-only-ban (ha értelmes)

			this.txtProcDefKey = new TextField(I.trc("Caption", "Munkaf. definíció azonosító kulcsa"));
			this.txtProcDefKey.setWidth(100, Unit.PERCENTAGE);
			this.txtProcDefKey.setValue(this.procDefEditorComponent.getProcessDefinitionKey() == null ? I.trc("Caption", "Nincs (új folyamat)") : this.procDefEditorComponent.getProcessDefinitionKey());
			this.txtProcDefKey.setEnabled(false);

			this.txtProcDefVer = new TextField(I.trc("Caption", "Munkafolyamat definíció verziója"));
			this.txtProcDefVer.setValue(this.procDefEditorComponent.getProcessDefinitionVersion() == null ? I.trc("Caption", "Nincs (új folyamat)") : this.procDefEditorComponent.getProcessDefinitionVersion());
			this.txtProcDefVer.setWidth(100, Unit.PERCENTAGE);
			this.txtProcDefVer.setEnabled(false);

			this.txtProcDefName = new TextField(I.trc("Caption", "Munkafolyamat definíció elnevezése"));
			this.txtProcDefName.setValue(this.procDefEditorComponent.getProcessDefinitionName() == null ? "" : this.procDefEditorComponent.getProcessDefinitionName());
			this.txtProcDefName.setWidth(100, Unit.PERCENTAGE);
			this.txtProcDefName.setMaxLength(20);
			this.txtProcDefName.setRequiredIndicatorVisible(true);

			this.vlRightPanel = new VerticalLayout();
			this.vlRightPanel.setWidth(300, Unit.PIXELS);
			this.vlRightPanel.setSpacing(true);
			this.vlRightPanel.setHeight(null);
			this.vlRightPanel.setMargin(true);

			this.btnCancel = new Button(I.trc("Caption", "Vissza/Mégsem"));
			this.btnCancel.setWidth(100, Unit.PERCENTAGE);

			this.btnSave = new Button(I.trc("Caption", "Mentés"));
			this.btnSave.addStyleName(ValoTheme.BUTTON_PRIMARY);
			this.btnSave.setWidth(100, Unit.PERCENTAGE);

			this.cmf1 = new CodeMirrorField(I.trc("Caption", "Munkafolyamat adatmodellje"), Mode.GROOVY, Theme.DARCULA);
			this.cmf1.setRequiredIndicatorVisible(true);
			this.cmf1.setValue(StringUtils.defaultIfBlank(this.procDefEditorComponent.getProcDefGroovyScriptStrings().get(ProccessJsonObjectTypes.PROC_DEF_DATA_MODEL), ""));
			this.cmf1.addValueChangeListener(v -> {
				this.procDefEditorComponent.getProcDefGroovyScriptStrings().put(ProccessJsonObjectTypes.PROC_DEF_DATA_MODEL, v.getValue());
			});

			this.cmf2 = new CodeMirrorField(I.trc("Caption", "Közös (visszanéző stb.) űrlap"), Mode.GROOVY, Theme.DARCULA);
			this.cmf2.setRequiredIndicatorVisible(true);
			this.cmf2.setValue(StringUtils.defaultIfBlank(this.procDefEditorComponent.getProcDefGroovyScriptStrings().get(ProccessJsonObjectTypes.PROC_DEF_FORM_MODEL), ""));
			this.cmf2.addValueChangeListener(v -> {
				this.procDefEditorComponent.getProcDefGroovyScriptStrings().put(ProccessJsonObjectTypes.PROC_DEF_FORM_MODEL, v.getValue());	
			});
			
			this.cmf3 = new CodeMirrorField(I.trc("Caption", "Publikus start adatmodellje (opcionális)"), Mode.GROOVY, Theme.DARCULA);
			this.cmf3.setValue(StringUtils.defaultIfBlank(this.procDefEditorComponent.getProcDefGroovyScriptStrings().get(ProccessJsonObjectTypes.PROC_DEF_PUBLIC_START_DATA_MODEL), ""));
			this.cmf3.addValueChangeListener(v -> {
				this.procDefEditorComponent.getProcDefGroovyScriptStrings().put(ProccessJsonObjectTypes.PROC_DEF_PUBLIC_START_DATA_MODEL, v.getValue());	
			});
			
			
			this.cmf4 = new CodeMirrorField(I.trc("Caption", "Publikus start űrlap (opcionális)"), Mode.GROOVY, Theme.DARCULA);
			this.cmf4.setValue(StringUtils.defaultIfBlank(this.procDefEditorComponent.getProcDefGroovyScriptStrings().get(ProccessJsonObjectTypes.PROC_DEF_PUBLIC_START_FORM_MODEL), ""));
			this.cmf4.addValueChangeListener(v -> {
				this.procDefEditorComponent.getProcDefGroovyScriptStrings().put(ProccessJsonObjectTypes.PROC_DEF_PUBLIC_START_FORM_MODEL, v.getValue());	
			});
			
			this.vlRightPanel.addComponent(this.txtProcDefKey);
			this.vlRightPanel.addComponent(this.txtProcDefVer);
			this.vlRightPanel.addComponent(this.txtProcDefName);
			this.vlRightPanel.addComponent(this.cmf1);
			this.vlRightPanel.addComponent(this.cmf2);
			this.vlRightPanel.addComponent(this.cmf3);
			this.vlRightPanel.addComponent(this.cmf4);
			this.vlRightPanel.addComponent(new Label(""));
			this.vlRightPanel.addComponent(this.btnSave);
			this.vlRightPanel.addComponent(this.btnCancel);

			this.addComponents(this.vlArea, this.procDefEditorComponent, this.vlRightPanel);
			this.setExpandRatio(this.vlArea, 1f);

		}

		{

			this.btnCancel.addClickListener(t -> UI.getCurrent().getNavigator().navigateTo(MainUI.NavConst.PROCESS_DEF_LIST));

			this.btnSave.addClickListener(t -> {

				// innen indul a mentés, de valójában érdemben a ProcessDefinitionEditorComponent-ben van
				// itt csak átdobjuk azokat az értékeket, amelyeket itt a keret részen ad meg (elnevezés stb.)

				if (StringUtils.isNoneBlank(this.cmf1.getValue(), this.cmf2.getValue(), this.txtProcDefName.getValue())) {
				 	
					if (!StringUtils.isAllBlank(this.cmf3.getValue(), this.cmf4.getValue()) && StringUtils.isAnyBlank(this.cmf3.getValue(), this.cmf4.getValue())) {
						throw new ManualValidationException("Missing required values!", I.trc("Caption", "Publikus start modellek egyike ki van töltve, a másik hiányzik!"));
					}

					try {
						ToolboxGroovyHelper.buildClass(this.cmf1.getValue());
					} catch (final ToolboxGroovyHelper.ToolboxGroovyHelperException e) {
						throw new ManualValidationException("Syntax error!", I.trc("Error", "Hibás szintaxis!"));
					}

					try {
						ToolboxGroovyHelper.buildClass(this.cmf2.getValue());
					} catch (final ToolboxGroovyHelper.ToolboxGroovyHelperException e) {
						throw new ManualValidationException("Syntax error!", I.trc("Error", "Hibás szintaxis!"));
					}

					try {
						ToolboxGroovyHelper.buildClass(this.cmf3.getValue());
					} catch (final ToolboxGroovyHelper.ToolboxGroovyHelperException e) {
						throw new ManualValidationException("Syntax error!", I.trc("Error", "Hibás szintaxis!"));
					}

					try {
						ToolboxGroovyHelper.buildClass(this.cmf4.getValue());
					} catch (final ToolboxGroovyHelper.ToolboxGroovyHelperException e) {
						log.error("Syntax error!", e);
						throw new ManualValidationException("Syntax error!", I.trc("Error", "Hibás szintaxis!"));
					}

					
					this.procDefEditorComponent.getProcDefGroovyScriptStrings().put(ProccessJsonObjectTypes.PROC_DEF_DATA_MODEL, this.cmf1.getValue());
					this.procDefEditorComponent.getProcDefGroovyScriptStrings().put(ProccessJsonObjectTypes.PROC_DEF_FORM_MODEL, this.cmf2.getValue());
					this.procDefEditorComponent.getProcDefGroovyScriptStrings().put(ProccessJsonObjectTypes.PROC_DEF_PUBLIC_START_DATA_MODEL, this.cmf3.getValue());
					this.procDefEditorComponent.getProcDefGroovyScriptStrings().put(ProccessJsonObjectTypes.PROC_DEF_PUBLIC_START_FORM_MODEL, this.cmf4.getValue());

					this.procDefEditorComponent.doSave(this.txtProcDefName.getValue());
				
				} else {

					boolean isCmf1Empty = false;
					boolean isCmf2Empty = false;
					boolean isTxtProcDefNameEmpty = false;

					if (StringUtils.isBlank(this.cmf1.getValue())) {
						isCmf1Empty = true;
					}

					if (StringUtils.isBlank(this.cmf2.getValue())) {
						isCmf2Empty = true;
					}

					if (StringUtils.isBlank(this.txtProcDefName.getValue())) {
						isTxtProcDefNameEmpty = true;
					}

					StringBuilder strBuilder = new StringBuilder();
					strBuilder.append(I.trc("Caption", "A következő mező(k) hiányoznak"));
					strBuilder.append(": ");

					if (isCmf1Empty) {
						strBuilder.append(I.trc("Caption", "Munkafolyamat adatmodellje"));
					}

					if (isCmf2Empty) {

						if (isCmf1Empty) {
							strBuilder.append(", ");
						}

						strBuilder.append(I.trc("Caption", "Visszanéző űrlap"));
					}

					if (isTxtProcDefNameEmpty) {

						if (isCmf1Empty || isCmf2Empty) {
							strBuilder.append(", ");
						}

						strBuilder.append(I.trc("Caption", "Munkafolyamat definíció elnevezése"));
					}

					throw new ManualValidationException("Missing required values!", strBuilder.toString());
				}

			});

		}

	}

}