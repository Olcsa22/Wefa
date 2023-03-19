package hu.lanoga.wefa.vaadin;

import java.util.LinkedHashMap;
import java.util.Map;

import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamunify.i18n.I;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import hu.lanoga.toolbox.ToolboxSysKeys.CrudOperation;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.util.ToolboxGroovyHelper;
import hu.lanoga.toolbox.vaadin.component.crud.MultiFormLayoutCrudFormComponent;
import hu.lanoga.wefa.SysKeys;
import hu.lanoga.wefa.exception.WefaGeneralException;
import hu.lanoga.wefa.service.ActivitiHelperService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GroovyProcessInstanceViewFormDialog extends Window {

	private final ActivitiHelperService activitiHelperService;

	private final String processInstanceId;

	private ProcessInstance processInstance; // vagy ez van (a másik null)
	private HistoricProcessInstance historicProcessInstance; // vagy ez van (a másik null)

	private final String processDefinitionId;
	private final ProcessDefinition processDefinition;

	private final Map<String, Object> processVariables;

	private String procDefDataModelGroovyScriptStr;
	private String procDefFormModelGroovyScriptStr;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public GroovyProcessInstanceViewFormDialog(final String processInstanceId) {
		super();

		try {

			this.activitiHelperService = ApplicationContextHelper.getBean(ActivitiHelperService.class);

			// ---

			this.processInstanceId = processInstanceId;
			this.processInstance = this.activitiHelperService.getProcessInstance(this.processInstanceId);

			if (this.processInstance != null) {
				this.processVariables = this.processInstance.getProcessVariables();
				this.processDefinitionId = this.processInstance.getProcessDefinitionId();
				this.processDefinition = this.activitiHelperService.getProcessDefinition(this.processDefinitionId);
			} else {
				this.historicProcessInstance = this.activitiHelperService.getHistoricProcessInstance(this.processInstanceId);
				this.processVariables = this.historicProcessInstance.getProcessVariables();
				this.processDefinitionId = this.historicProcessInstance.getProcessDefinitionId();
				this.processDefinition = this.activitiHelperService.getProcessDefinition(this.processDefinitionId);
			}

			final LinkedHashMap<String, String> procDefGroovyScriptStrings = this.activitiHelperService.loadBackDeployedProcDefGroovyScriptStrings(this.processDefinitionId);
			this.procDefDataModelGroovyScriptStr = procDefGroovyScriptStrings.get(SysKeys.ProccessJsonObjectTypes.PROC_DEF_DATA_MODEL);
			this.procDefFormModelGroovyScriptStr = procDefGroovyScriptStrings.get(SysKeys.ProccessJsonObjectTypes.PROC_DEF_FORM_MODEL);

			// ---

			this.setCaption(I.trc("Caption", "Munkafolyamat (visszanéző űrlap): ") + this.processDefinition.getName());

			// TODO: több adat a feliratban, processIsntanceId stb.
			// esetleg egy nagyobb label a form felett, több sorban adatok (mikor lett indítva stb.)
			// vagy popupview (mint a file carousel-nél az infók)

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

			final Class procDefDomainClass = ToolboxGroovyHelper.buildClass(this.procDefDataModelGroovyScriptStr);
			final Object procInstanceDomainObject;

			if (this.processVariables != null && !this.processVariables.isEmpty()) {
				final ObjectMapper mapper = new ObjectMapper(); // Jackson
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				procInstanceDomainObject = mapper.convertValue(this.processVariables, procDefDomainClass);
			} else {
				procInstanceDomainObject = procDefDomainClass.newInstance();
			}

			final MultiFormLayoutCrudFormComponent crudFormComponent = new MultiFormLayoutCrudFormComponent(() -> {
				return ToolboxGroovyHelper.buildClassInstance(this.procDefFormModelGroovyScriptStr);
			}, null);
			crudFormComponent.setDomainObject((ToolboxPersistable) procInstanceDomainObject);
			crudFormComponent.setCrudOperation(CrudOperation.READ);
			crudFormComponent.setCrudAction(v -> {
				this.close();
			});

			vlContent.addComponent(crudFormComponent);
			crudFormComponent.init();
			crudFormComponent.setMargin(true);

		} catch (final Exception e) {
			throw new WefaGeneralException("GroovyProcessInstanceViewFormDialog init error!", e);
		}

	}

}
