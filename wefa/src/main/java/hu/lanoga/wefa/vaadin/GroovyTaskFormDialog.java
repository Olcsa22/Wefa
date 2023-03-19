package hu.lanoga.wefa.vaadin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.beanutils.BeanUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.teamunify.i18n.I;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import hu.lanoga.toolbox.ToolboxSysKeys.CrudOperation;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.user.UserService;
import hu.lanoga.toolbox.util.ToolboxGroovyHelper;
import hu.lanoga.toolbox.vaadin.component.crud.MultiFormLayoutCrudFormComponent;
import hu.lanoga.wefa.SysKeys;
import hu.lanoga.wefa.exception.WefaGeneralException;
import hu.lanoga.wefa.service.ActivitiHelperService;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("unused")
@Slf4j
public class GroovyTaskFormDialog extends Window {

	public static final Cache<String, Integer> recentlyStartedTaskFills = CacheBuilder.newBuilder().concurrencyLevel(4).expireAfterWrite(120, TimeUnit.SECONDS).build(); // nem cache szó szerint, a timeout és a jó/beépített concurrency kezelés miatt hasznájuk (ConccurentHashMap stb. helyett)

	private final ActivitiHelperService activitiHelperService;

	private final String taskId;
	private final Task task;
	private final String processInstanceId;
	private final ProcessInstance processInstance;
	private final String processDefinitionId;
	private final ProcessDefinition processDefinition;

	private String stepDataModelGroovyScriptStr;
	private String stepFormModelGroovyScriptStr;

	private boolean finishedFlag;

	@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
	public GroovyTaskFormDialog(final String taskId) {
		super();

		try {

			this.finishedFlag = false;
			this.taskId = taskId;

			// ---

			final ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();

			final Integer t = recentlyStartedTaskFills.getIfPresent(taskId);
			if (t != null && !loggedInUser.getId().equals(t)) {
				UI.getCurrent().showNotification(
						I.trc("Notification", "Egy másik felhasználó már elkezdte ez a feladatot/űrlapot")
								+ ": "
								+ I18nUtil.buildFullName(ApplicationContextHelper.getBean(UserService.class).findOne(t), false));
			}

			recentlyStartedTaskFills.put(taskId, loggedInUser.getId());

			this.addCloseListener(c -> {
				recentlyStartedTaskFills.invalidate(taskId);
			});

			// ---

			this.activitiHelperService = ApplicationContextHelper.getBean(ActivitiHelperService.class);

			this.task = this.activitiHelperService.findTask(taskId);

			this.processInstanceId = this.task.getProcessInstanceId();
			this.processInstance = this.activitiHelperService.getProcessInstance(this.processInstanceId);
			this.processDefinitionId = this.task.getProcessDefinitionId();
			this.processDefinition = this.activitiHelperService.getProcessDefinition(this.processDefinitionId);

			final LinkedHashMap<String, String> procDefGroovyScriptStrings = this.activitiHelperService.loadBackDeployedProcDefGroovyScriptStrings(this.processDefinitionId);
			final String formKey = this.task.getFormKey();

			procDefGroovyScriptStrings.get(SysKeys.ProccessJsonObjectTypes.PROC_DEF_DATA_MODEL);
			this.stepDataModelGroovyScriptStr = procDefGroovyScriptStrings.get(SysKeys.ProccessJsonObjectTypes.PROC_DEF_STEP_DATA_MODEL + "-" + formKey);
			this.stepFormModelGroovyScriptStr = procDefGroovyScriptStrings.get(SysKeys.ProccessJsonObjectTypes.PROC_DEF_STEP_FORM_MODEL + "-" + formKey);

			// ---

			this.setCaption(I.trc("Caption", "Lépés (űrlap): ") + this.task.getName());

			// TODO: több adat a feliratban, processIsntanceId stb.
			// esetleg egy nagyobb label a form felett, több sorban adatok (mikor lett indítva, mikori a task stb.)
			// vagy popupview (mint a file carousel-nél az infók)

			this.setWidth("850px");
			this.setHeight(null);
			this.setModal(true);

			final VerticalLayout vlContent = new VerticalLayout();
			vlContent.setWidth("100%");
			vlContent.setHeight(null);
			vlContent.setSpacing(true);
			vlContent.setMargin(true);
			this.setContent(vlContent);

			// ---

			final Class domainClass = ToolboxGroovyHelper.buildClass(this.stepDataModelGroovyScriptStr);
			final Object domainObject = domainClass.newInstance();

			final Long staleDataCheckTs;

			final Map<String, Object> processVariables = this.processInstance.getProcessVariables();
			if (processVariables != null && !processVariables.isEmpty()) {
				final Class procDefDomainClass = ToolboxGroovyHelper.buildClass(this.stepDataModelGroovyScriptStr);

				final ObjectMapper mapper = new ObjectMapper(); // Jackson
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				final Object procInstanceDomainObject = mapper.convertValue(processVariables, procDefDomainClass);

				BeanUtils.copyProperties(domainObject, procInstanceDomainObject);

				staleDataCheckTs = (Long) processVariables.get(SysKeys.STALE_DATA_CHECK_TS_VAR_MAP_KEY);

			} else {

				staleDataCheckTs = null;

			}

			final MultiFormLayoutCrudFormComponent crudFormComponent = new MultiFormLayoutCrudFormComponent(() -> {
				return ToolboxGroovyHelper.buildClassInstance(this.stepFormModelGroovyScriptStr);
			}, null);
			crudFormComponent.setDomainObject((ToolboxPersistable) domainObject);
			crudFormComponent.setCrudOperation(CrudOperation.UPDATE); // itt ez nem az, mint normálisan lenne egy normál CRUD-ban... egyelőre az UPDATE és a READ kell csak
			crudFormComponent.setCrudAction(v -> {

				// ---

				this.activitiHelperService.completeTask(taskId, this.processInstanceId, domainObject, staleDataCheckTs);

				// ---

				UI.getCurrent().getNavigator().navigateTo(MainUI.NavConst.TASK_LIST);

				this.finishedFlag = true;

				GroovyTaskFormDialog.this.close();

			});

			vlContent.addComponent(crudFormComponent);
			crudFormComponent.init();
			crudFormComponent.setMargin(true);

		} catch (final Exception e) {
			throw new WefaGeneralException("GroovyTaskFormDialog init error!", e);
		}

	}

}
