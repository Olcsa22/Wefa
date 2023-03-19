package hu.lanoga.wefa.config;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEntityEventImpl;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.codestore.CodeStoreItemService;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.json.jackson.JacksonHelper;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.JmsManager;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.user.User;
import hu.lanoga.toolbox.user.UserService;
import hu.lanoga.wefa.model.WefaProcessInstance;
import hu.lanoga.wefa.service.ActivitiHelperService;
import hu.lanoga.wefa.service.WefaProcessInstanceService;
import lombok.extern.slf4j.Slf4j;

@Configuration
// @AutoConfigureAfter(FlywayAutoConfiguration.class)
@Slf4j
public class ProcessEngineConfig {

	private static final String BPMN_PATH = "processes/";

	@Autowired
	private DataSource dataSource;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private org.springframework.core.io.Resource[] getBpmnFiles() throws IOException {
		final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
		return resourcePatternResolver.getResources("classpath*:" + BPMN_PATH + "**/*.bpmn");
	}

	@Bean
	public SpringProcessEngineConfiguration processEngineConfiguration() {

		final SpringProcessEngineConfiguration config = new SpringProcessEngineConfiguration();

		try {
			config.setDeploymentResources(this.getBpmnFiles());
		} catch (final IOException e) {
			log.error("processEngineConfiguration failed: ", e);
		}

		config.setDataSource(this.dataSource);
		config.setTransactionManager(this.transactionManager);

		config.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE);
		config.setHistory("audit");

		final ActivitiEventListener eventListener = new ActivitiEventListener() {

			/**
			 * az activity nem használ extra threadet a listener hívásokhoz,
			 * ebből következően annak a usernek a nevében fog futni, aki az adott műveletet végezte (script stb. taskoknál nem biztos)
			 *
			 * @param event
			 */
			@Override
			public void onEvent(final ActivitiEvent event) {

				// itt ENTITY_CREATED-re ellenőrzünk, mert a PROCESS_START nem a ProcessInstance objektumot adja vissza, hanem EXECUTION-t
				if (event.getType().equals(ActivitiEventType.ENTITY_CREATED)) {

					final Object object = ((ActivitiEntityEventImpl) event).getEntity();
					if (object instanceof ProcessInstance) {

						// final HistoricProcessInstance hpi = activitiHelperService.getHistoricProcessInstance(event.getProcessInstanceId());

						final ProcessInstance pi = (ProcessInstance) object;

						if (StringUtils.isNotEmpty(pi.getProcessDefinitionName())) {

							final WefaProcessInstance processInstance = new WefaProcessInstance();
							processInstance.setProcessInstanceId(pi.getId());
							processInstance.setProcessDefinitionId(pi.getProcessDefinitionId());
							processInstance.setProcessDefinitionName(pi.getProcessDefinitionName());
							processInstance.setStartTime(new Timestamp(pi.getStartTime().getTime()));
							processInstance.setIsClosed(false);

							final Map<String, Object> processVariables = pi.getProcessVariables();

							ApplicationContextHelper.getBean(WefaProcessInstanceService.class).save(processInstance);
						}
					}

				} else if (event.getType().equals(ActivitiEventType.TASK_CREATED)) {

					final ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();
					final User receiver;
					final Task task;

					try {

						SecurityUtil.setSystemUser();

						final ActivitiEntityEventImpl event1 = (ActivitiEntityEventImpl) event;
						task = (Task) event1.getEntity();

						JdbcRepositoryManager.setTlTenantId(Integer.parseInt(task.getTenantId()));

						final String assignee = task.getAssignee();
						final boolean isRoleAssignee = assignee.contains(":"); // csoportnak (ROLE) van-e kiosztva

						final WefaProcessInstanceService wefaProcessInstanceService = ApplicationContextHelper.getBean(WefaProcessInstanceService.class);
						final UserService userService = ApplicationContextHelper.getBean(UserService.class);
						final CodeStoreItemService codeStoreItemService = ApplicationContextHelper.getBean(CodeStoreItemService.class);

						final WefaProcessInstance processInstance = wefaProcessInstanceService.findOneByProcessInstanceId(task.getProcessInstanceId());

						processInstance.setTaskName(task.getName());

						if (isRoleAssignee) {

							final String[] split = assignee.split(":");
							final int roleId = Integer.parseInt(split[1]);

							processInstance.setTaskWorker(codeStoreItemService.findOne(roleId).getCaptionCaption());
							final List<User> list = userService.findAllUserByRole(roleId);
							for (final User u : list) {
								this.notifyUser(u, task);
							}

						} else {
							processInstance.setTaskWorker(I18nUtil.buildFullName(userService.findOne(Integer.parseInt(assignee)), true));

							receiver = ApplicationContextHelper.getBean(UserService.class).findOne(Integer.parseInt(task.getAssignee()));
							this.notifyUser(receiver, task);
						}

						wefaProcessInstanceService.save(processInstance);

					} finally {
						JdbcRepositoryManager.clearTlTenantId();
						SecurityUtil.setUser(loggedInUser);
					}

				} else if (event.getType().equals(ActivitiEventType.TASK_COMPLETED)) {

					final ActivitiEntityEventImpl event1 = (ActivitiEntityEventImpl) event;
					final Task task = (Task) event1.getEntity();

					final HistoricProcessInstance taskFromDB = ApplicationContextHelper.getBean(ActivitiHelperService.class).getHistoricProcessInstance(task.getProcessInstanceId());

					final WefaProcessInstanceService wefaProcessInstanceService = ApplicationContextHelper.getBean(WefaProcessInstanceService.class);
					final WefaProcessInstance processInstance = wefaProcessInstanceService.findOneByProcessInstanceId(task.getProcessInstanceId());

					final Map<String, Object> processVariables = taskFromDB.getProcessVariables();

					processInstance.setProcessVariables(JacksonHelper.toJson(processVariables));

					wefaProcessInstanceService.save(processInstance);

				} else if (event.getType().equals(ActivitiEventType.PROCESS_COMPLETED)) {

					final Object object = ((ActivitiEntityEventImpl) event).getEntity();
					final ProcessInstance pi = (ProcessInstance) object;

					final WefaProcessInstanceService wefaProcessInstanceService = ApplicationContextHelper.getBean(WefaProcessInstanceService.class);
					final WefaProcessInstance processInstance = wefaProcessInstanceService.findOneByProcessInstanceId(pi.getProcessInstanceId());

					processInstance.setIsClosed(true);
					processInstance.setEndTime(new Timestamp(System.currentTimeMillis()));

					wefaProcessInstanceService.save(processInstance);

				}

			}

			private void notifyUser(final User receiver, final Task task) {

				// if (Boolean.TRUE.equals(ApplicationContextHelper.getConfigProperty("...", Boolean.class))) {
				//
				// final Map<String, Object> hashMap = new HashMap<>();
				// hashMap.put("url", BrandUtil.getRedirectUriHostFrontend());
				// hashMap.put("taskId", task.getId());
				// hashMap.put("taskName", task.getName());
				// hashMap.put("recipient", receiver.getUsername());
				// hashMap.put("tenantName", ApplicationContextHelper.getBean(TenantJdbcRepository.class).findOne(Integer.parseInt(task.getTenantId())).getName());
				//
				// ApplicationContextHelper.getBean(EmailTemplateService.class).addMail(null, receiver.getEmail(), SysKeys.EmailTemplateType.EMAIL_NOTIF, hashMap, new Locale("hu"));
				//
				// }

				JmsManager.send(JmsManager.buildDestStr(ToolboxSysKeys.JmsDestinationMode.USER, Integer.toString(receiver.getId()), "new-proc-inst"), new HashMap<String, String>());
			}

			@Override
			public boolean isFailOnException() {
				return false;
			}
		};

		final List<ActivitiEventListener> eventListeners = new ArrayList<>();
		eventListeners.add(eventListener);

		config.setEventListeners(eventListeners);

		// config.setJobExecutorActivate(true); // látszólag nem kell

		return config;

	}

}
