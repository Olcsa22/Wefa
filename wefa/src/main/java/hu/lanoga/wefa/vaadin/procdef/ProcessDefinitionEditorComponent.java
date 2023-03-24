package hu.lanoga.wefa.vaadin.procdef;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;

import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.validation.ValidationError;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Base64Utils;

import com.teamunify.i18n.I;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.JavaScriptFunction;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

import elemental.json.JsonArray;
import elemental.json.impl.JreJsonNull;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.wefa.exception.WefaGeneralException;
import hu.lanoga.wefa.service.ActivitiHelperService;
import hu.lanoga.wefa.vaadin.MainUI;
import hu.lanoga.wefa.vaadin.procdef.dialog.ConditionEditorDialog;
import hu.lanoga.wefa.vaadin.procdef.dialog.ScriptTaskEditorDialog;
import hu.lanoga.wefa.vaadin.procdef.dialog.UserTaskEditorDialog;
import lombok.extern.slf4j.Slf4j;

/**
 * WF ({@link ProcessDefinition}) BPMN szerkesztés...
 */
@com.vaadin.annotations.JavaScript({ "../../../webjars/jquery/1.12.4/jquery.min.js", "../../../s/procdef/bower_components/bpmn-js/dist/bpmn-modeler.min.js", "../../../s/procdef/wfeditor.js" })


@Slf4j
public class ProcessDefinitionEditorComponent extends com.vaadin.ui.AbstractJavaScriptComponent {

	/**
	 * debug célra...
	 *
	 * @param ja
	 * @return
	 */
	private static String jsonArrayToString(final elemental.json.JsonArray ja) {

		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ja.length(); ++i) {
			sb.append("[");
			sb.append(ja.get(i).asString());
			sb.append("]");
		}

		return sb.toString();
	}

	// ------------

	/**
	 * háttérszálas eléréshez stb.
	 */
	@SuppressWarnings("unused")
	private final UI ui;

	private final String processDefinitonId;
	private ProcessDefinition processDefinition;

	private final ActivitiHelperService activitiHelperService;

	private LinkedHashMap<String, String> procDefGroovyScriptStrings;

	private String procDefKey;

	// ------------

	public String getProcessDefinitionKey() {
		return this.processDefinition != null ? this.processDefinition.getKey() : null;
	}

	public String getProcessDefinitionVersion() {
		return this.processDefinition != null ? Integer.toString(this.processDefinition.getVersion()) : null;
	}

	public String getProcessDefinitionName() {
		return this.processDefinition != null ? this.processDefinition.getName() : null;
	}

	public LinkedHashMap<String, String> getProcDefGroovyScriptStrings() {
		return this.procDefGroovyScriptStrings;
	}

	// ------------

	/**
	 * @param areaId
	 * @param processDefinitonId
	 * @param viewOnly
	 */
	public ProcessDefinitionEditorComponent(final String areaId, final String processDefinitonId, final boolean viewOnly) {
		super();

		// ---

		this.activitiHelperService = ApplicationContextHelper.getBean(ActivitiHelperService.class);
		this.ui = UI.getCurrent();

		// ---

		if (!processDefinitonId.equals("new")) {
			this.processDefinitonId = processDefinitonId;
			this.processDefinition = this.activitiHelperService.getProcessDefinition(this.processDefinitonId);
		} else {
			this.processDefinitonId = RandomStringUtils.randomAlphanumeric(10);
		}

		this.procDefKey = this.getProcessDefinitionKey();
		if (this.procDefKey == null) {
			this.procDefKey = RandomStringUtils.randomAlphanumeric(10);
		}

		if (this.processDefinition != null && this.processDefinition.getId() != null) {
			this.procDefGroovyScriptStrings = this.activitiHelperService.loadBackDeployedProcDefGroovyScriptStrings(this.processDefinition.getId());
		} else {
			this.procDefGroovyScriptStrings = new LinkedHashMap<>();
		}

		// ---

		this.getState().areaId = areaId;
		this.getState().processDefinitionId = this.processDefinitonId;

		// ---

		JavaScript.getCurrent().addFunction("wfeLoadOrig", (JavaScriptFunction) arguments -> this.wfeLoadOrig(arguments));
		JavaScript.getCurrent().addFunction("wfeConditionalFlowClick", (JavaScriptFunction) arguments -> this.wfeConditionalFlowClick(arguments)); // elágazásból kijövő nyilakhoz
		JavaScript.getCurrent().addFunction("wfeUserTaskNodeClick", (JavaScriptFunction) arguments -> this.wfeUserTaskNodeClick(arguments));
		JavaScript.getCurrent().addFunction("wfeScriptTaskNodeClick", (JavaScriptFunction) arguments -> this.wfeScriptTaskNodeClick(arguments));
		JavaScript.getCurrent().addFunction("wfeSaveWorkflowClick", (JavaScriptFunction) arguments -> this.wfeSaveWorkflowClick(arguments));

		// ---

		// tömören összefoglalva itt a lényeg, az, hogy oda-vissza küldözgetünk adatokat
		// a JavasScript kódunknak (ami pedig a bpmn-modeler nevű lib-et használja), van:
		// 1) betöltés (visszatöltés)
		// 2) mentés
		// 3) szerkesztés közbeni kattintások a bpmn-modeler-en

	}

	@Override
	protected ProcessDefinitionEditorComponentState getState() {
		return (ProcessDefinitionEditorComponentState) super.getState();
	}

	private void wfeLoadOrig(final JsonArray arguments) {

		log.debug(String.format("wfeLoadOrig, arguments: %s", jsonArrayToString(arguments)));

		{

			byte[] byteArray;

			if (this.processDefinition != null) {
				byteArray = this.activitiHelperService.loadBackDeployedProcDefBpmnXmlData(this.processDefinition.getId());
			} else {
				try {
					byteArray = IOUtils.toByteArray(this.getClass().getClassLoader().getResourceAsStream("process_templates/starter.bpmn"));
				} catch (final IOException e) {
					throw new WefaGeneralException(e);
				}
			}

			try {
				JavaScript.eval("wfeLoadOrigBack('" + this.processDefinitonId + "', '" + URLEncoder.encode(new String(byteArray, StandardCharsets.UTF_8), StandardCharsets.UTF_8.name()).replace("+", " ") + "');");
			} catch (final UnsupportedEncodingException e) {
				throw new WefaGeneralException(e);
			}

		}

	}

	private void wfeUserTaskNodeClick(final JsonArray arguments) {

		log.debug(String.format("wfeUserTaskNodeClick, arguments: %s", jsonArrayToString(arguments)));

		final String elementId = arguments.getString(1);
		final String assignee = arguments.get(2) instanceof JreJsonNull ? null : arguments.getString(2); // jelenlegi érték, ha van
		final String formKey = arguments.get(3) instanceof JreJsonNull ? null : arguments.getString(3); // jelenlegi érték, ha van

		UI.getCurrent().addWindow(new UserTaskEditorDialog(this.processDefinitonId, elementId, assignee, formKey, this.procDefGroovyScriptStrings));

	}

	private void wfeScriptTaskNodeClick(final JsonArray arguments) {

		log.debug(String.format("wfeScriptTaskNodeClick, arguments: %s", jsonArrayToString(arguments))); // workflowId, e.element.id, e.element.businessObject.conditionExpression (a jelenlegi)

		final String elementId = arguments.getString(1);
		final String scriptStr = arguments.get(2) instanceof JreJsonNull ? null : arguments.get(2).asString(); // jelenlegi érték, ha van

		UI.getCurrent().addWindow(new ScriptTaskEditorDialog(this.processDefinitonId, elementId, scriptStr));

	}

	private void wfeConditionalFlowClick(final JsonArray arguments) {

		log.debug(String.format("wfeConditionalFlowClick, arguments: %s", jsonArrayToString(arguments))); // workflowId, e.element.id

		final String elementId = arguments.getString(1);
		final String condExpr = arguments.get(2) instanceof JreJsonNull ? null : arguments.get(2).asString(); // jelenlegi érték, ha van

		UI.getCurrent().addWindow(new ConditionEditorDialog(this.processDefinitonId, elementId, condExpr));

	}

	/**
	 * 
	 * @param procDefName
	 * 		ez bármi lehet, amit a user ad... (ez nem id, nem kulcs...)
	 */
	public void doSave(final String procDefName) {

		try {
			JavaScript.eval("wfePreSaveWorkflowBack('" + this.getState().processDefinitionId + "', '" + URLEncoder.encode(procDefName, "UTF-8").replace("+", " ") + "');");
		} catch (final Exception e) {
			throw new WefaGeneralException(e);
		}

	}

	private void wfeSaveWorkflowClick(final JsonArray arguments) {

		log.debug(String.format("wfeSaveWorkflowClick, arguments: %s", jsonArrayToString(arguments)));

		// this.checkWfIdEquality(arguments.getString(0)); //

		boolean isSuccessful = false;

		try {

			final String procDefName = arguments.getString(0);
			byte[] bpmnXmlData = Base64Utils.decodeFromString(arguments.getString(1));

			{

				String s = new String(bpmnXmlData, StandardCharsets.UTF_8);
				s = StringUtils.replace(s, "\"Process_1\"", "\"" + procDefKey + "\"");
				bpmnXmlData = s.getBytes(StandardCharsets.UTF_8);
			}

			//

			final List<ValidationError> validationErrors = this.activitiHelperService.deployProcessDefinitionPreValidate(bpmnXmlData);

			if (!validationErrors.isEmpty()) {
				log.warn("wfeSaveWorkflowClick validation error: " + validationErrors);
				Notification.show(I.trc("Notification", "Mentés hiba (folyamat definíció hibás)!"), Type.WARNING_MESSAGE);
			} else {

				this.activitiHelperService.deployProcessDefinition(this.procDefKey, procDefName, bpmnXmlData, this.procDefGroovyScriptStrings);

				isSuccessful = true;
			}

		} catch (final Exception e) {
			throw new WefaGeneralException(e);
		}

		JavaScript.eval("wfeSaveWorkflowBack('" + this.processDefinitonId + "', '" + isSuccessful + "');"); // ez csak debug célra, JavaScript-ben is van némi logolás

		if (isSuccessful) {
			Notification.show(I.trc("Notification", "Munkafolyamat mentve!"));
		} else {
			Notification.show(I.trc("Notification", "Munkafolyamat mentés nem sikerült!"), Type.ERROR_MESSAGE);
		}

		UI.getCurrent().getNavigator().navigateTo(MainUI.NavConst.PROCESS_DEF_LIST);
	}

}
