package hu.lanoga.wefa.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.activiti.validation.ValidationError;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Striped;
import com.teamunify.i18n.I;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.exception.StaleDataException;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileDescriptorJdbcRepository;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.json.jackson.JacksonHelper;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.user.UserService;
import hu.lanoga.toolbox.util.ToolboxAssert;
import hu.lanoga.toolbox.util.ToolboxStringUtil;
import hu.lanoga.wefa.SysKeys;
import hu.lanoga.wefa.SysKeys.UserAuth;
import hu.lanoga.wefa.exception.WefaGeneralException;
import hu.lanoga.wefa.model.ExportImportDeployModel;
import hu.lanoga.wefa.util.WefaStringUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ProcessDefinition} (a workflow típus/definíció, BPMN) kezelése;  
 * {@link ProcessInstance} (konkrét workflow példányok, futó vagy historic) kezelése;  
 * {@link Task} (konkrét task példányok) kezelése
 */
@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
@Transactional
@Service
@Slf4j
public class ActivitiHelperService {
	
	// hogyan kell elágazást csinálni: 
	
	// https://stackoverflow.com/questions/36766569/activiti-exclusive-gateway-using-in-java
	// https://www.activiti.org/userguide/#apiExpressions
	// https://docs.oracle.com/javaee/6/tutorial/doc/gjddd.html
	// https://docs.oracle.com/javaee/6/tutorial/doc/bnaik.html
	// ${var == 1}
	
	// default (else) flow esetén nem kell ide írni semmit 
	// jobb gomb és a kis wrench gombbal kell default flow-nak megjelölni a vonalat
	
	// ------------------------------------------------------------------------------------------
	
	@Autowired
	private org.activiti.engine.HistoryService historyService;

	@Autowired
	private org.activiti.engine.RepositoryService repositoryService;

	@Autowired
	private org.activiti.engine.RuntimeService runtimeService;

	@Autowired
	private org.activiti.engine.TaskService taskService;

	@Autowired
	private FileStoreService fileStoreService;

	@Autowired
	private UserService userService;
	
	@Autowired
	private FileDescriptorJdbcRepository fileDescriptorJdbcRepository;

	private static final Striped<Lock> stripedLock = Striped.lock(10);

	/**
	 * task lezárása
	 *
	 * @param taskId
	 * @see #setProcessInstanceVars
	 */
	@SuppressWarnings("unchecked")
	public void completeTask(final String taskId, final String processInstanceId, final Object domainObject, final Long staleDataCheckTs) {

		final Lock lock = stripedLock.get(processInstanceId);

		try {
			lock.lock();

			// TODO: a taskLocalVariables nem erre van?

			final HistoricProcessInstance processInstanceFromTheDb = this.getHistoricProcessInstance(processInstanceId);
			final Map<String, Object> procVarMapCurrentDbState = processInstanceFromTheDb.getProcessVariables();

			if (staleDataCheckTs != null) {

				final Long staleDataCheckTsFromTheDb = (Long) procVarMapCurrentDbState.get(SysKeys.STALE_DATA_CHECK_TS_VAR_MAP_KEY);

				if (!staleDataCheckTs.equals(staleDataCheckTsFromTheDb)) {
					throw new StaleDataException("staleDataCheckTs check... save abort...", I.trc("Notification", "Elavult adat! (más már elvégezte ezt a feladatot?)"));
				}

			}

			final Map<String, Object> procVarMapThisTask = new ObjectMapper().convertValue(domainObject, Map.class);
			procVarMapCurrentDbState.putAll(procVarMapThisTask);

			procVarMapCurrentDbState.put(SysKeys.STALE_DATA_CHECK_TS_VAR_MAP_KEY, System.nanoTime());

			this.setProcessInstanceVars(processInstanceId, procVarMapCurrentDbState);
			this.taskService.complete(taskId);

		} finally {
			lock.unlock();
		}
	}

	/**
	 * folyamat példány változók beállítása (a folyamat task-jaira nézve közösek!)
	 *
	 * @param executionId
	 * 		ez ugyanaz mint a processInstanceId? // TODO: tiszázni Nem ugyanaz, ez az act_ru_execution táblára mutat
	 * @param variables
	 */
	private void setProcessInstanceVars(final String executionId, final Map<String, ? extends Object> variables) {
		if (MapUtils.isNotEmpty(variables)) {
			this.runtimeService.setVariables(executionId, variables);
		}
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	public void deleteProcessDefinitionById(final String processDefinitionId) {


		this.repositoryService.deleteDeployment(processDefinitionId);
	}

	/**
	 * @param processInstanceId
	 * @param deleteReasonComment
	 * 		lehet null
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	public void deleteProcessInstance(final String processInstanceId, final String deleteReasonComment) {
		this.runtimeService.deleteProcessInstance(processInstanceId, deleteReasonComment);
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	public String importProcessDefinition(final FileDescriptor importFd) {

		String jsonStr;
		try {
			jsonStr = FileUtils.readFileToString(importFd.getFile(), StandardCharsets.UTF_8);
		} catch (final IOException e) {
			throw new WefaGeneralException(e);
		}

		final ExportImportDeployModel exportImportDeployModel = JacksonHelper.fromJson(jsonStr, ExportImportDeployModel.class);
		final ProcessDefinition processDefinition = this.deployProcessDefinition(exportImportDeployModel);

		return processDefinition.getId();
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	public FileDescriptor exportProcessDefinition(final String processDefinitionId) {

		final byte[] bpmnXmlData = this.loadBackDeployedProcDefBpmnXmlData(processDefinitionId);
		final LinkedHashMap<String, String> procDefGroovyScriptStrings = this.loadBackDeployedProcDefGroovyScriptStrings(processDefinitionId);
		// byte[] procDefDiagramImg = this.loadBackDeployedProcDefDiagramImg(processDefinitionId);

		final ProcessDefinition processDefinition = this.getProcessDefinition(processDefinitionId);

		final ExportImportDeployModel exportImportDeployModel = new ExportImportDeployModel();
		exportImportDeployModel.setProcDefName(processDefinition.getName());
		exportImportDeployModel.setBpmnXmlData(bpmnXmlData);
		exportImportDeployModel.setProcDefGroovyScriptStrings(procDefGroovyScriptStrings);

		final String jsonStr = JacksonHelper.toJson(exportImportDeployModel);

		final String fn = ToolboxStringUtil.convertToUltraSafe(processDefinition.getName(), "-");
		final FileDescriptor fd = this.fileStoreService.createTmpFile2("export-" + fn + ".json");

		try {
			FileUtils.writeStringToFile(fd.getFile(), jsonStr, StandardCharsets.UTF_8);
		} catch (final IOException e) {
			throw new WefaGeneralException(e);
		}

		return fd;
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	public ProcessDefinition deployProcessDefinition(final ExportImportDeployModel exportImportDeployModel) {
		return this.deployProcessDefinition(null, exportImportDeployModel.getProcDefName(), exportImportDeployModel.getBpmnXmlData(), exportImportDeployModel.getProcDefGroovyScriptStrings());
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	public ProcessDefinition deployProcessDefinition(final String procDefKey, final String procDefName, final byte[] bpmnXmlData, final LinkedHashMap<String, String> procDefGroovyScriptStrings) {

		// lementés Activiti-vel

		final DeploymentBuilder deploymentBuilder = this.repositoryService.createDeployment().name(procDefName).tenantId(Integer.toString(SecurityUtil.getLoggedInUserTenantId()));

		deploymentBuilder.addBytes("main.bpmn", bpmnXmlData);
		// deploymentBuilder.key(procDefKey); // itt a procDefKey nem hat rá, a sablon (üres) bpmn (start) fájlunkból szedi ki és az marad fixen...

		procDefGroovyScriptStrings.forEach((x, y) -> {
			deploymentBuilder.addBytes(x + ".groovy", y.getBytes(StandardCharsets.UTF_8));
		});

		final Deployment deployment = deploymentBuilder.deploy();

		log.info(String.format("ActivitiService deployment: %s", deployment));

		final ProcessDefinitionQuery processDefinitionQuery = this.repositoryService.createProcessDefinitionQuery();
		return processDefinitionQuery.deploymentId(deployment.getId()).singleResult();

	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	public List<ValidationError> deployProcessDefinitionPreValidate(final byte[] bpmnXmlData) {

		try (InputStreamReader in = new InputStreamReader(new ByteArrayInputStream(bpmnXmlData), StandardCharsets.UTF_8)) {

			final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

			// or disallow DTDs entirely
			xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE); // a SonarLint azt mondja, ezt FALSE-ra kell tenni security okból
			// xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE); // vagy akár ezt...

			final XMLStreamReader xtr = xmlInputFactory.createXMLStreamReader(in);
			final BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);

			return this.repositoryService.validateProcess(bpmnModel);

		} catch (final Exception e) {
			throw new WefaGeneralException("deployProcessDefinitionPreValidate error!", e);
		}
	}

	public long countHistoricProcessInstances() {
		return this.historyService.createHistoricProcessInstanceQuery().processInstanceTenantId(Integer.toString(SecurityUtil.getLoggedInUserTenantId())).count();
	}

	/**
	 * utolsó befejezett (értsd finished szerint visszafele rendezeve), folyamatok ({@link HistoricProcessInstance}),  
	 * pl.: 0, 50 az utolsó 50 finished processInstance
	 * 
	 * @param from
	 * @param count
	 * @return
	 */
	public List<HistoricProcessInstance> findHistoricProcessInstances(final int from, final int count) {
		// ha nincs itt a /*.finished()*/, akkor minden benne van, az aktuálisak is
		return this.historyService.createHistoricProcessInstanceQuery().includeProcessVariables().orderByProcessInstanceStartTime().processInstanceTenantId(Integer.toString(SecurityUtil.getLoggedInUserTenantId()))/* .finished() */.desc().listPage(from, count);
	}

	// /**
	// * minden folyó/aktuális/befejezetlen folyamat ({@link ProcessInstance})...
	// *
	// * @return
	// */
	// public List<ProcessInstance> findRunningProcessInstances() {
	// return this.runtimeService.createProcessInstanceQuery().includeProcessVariables().orderByProcessInstanceId().processInstanceTenantId(Integer.toString(SecurityUtil.getLoggedInUserTenantId())).desc().list();
	// }

	/**
	 * {@link Task} objektum kikeresése a taskId alapján
	 *  
	 * @param taskId
	 * @return
	 */
	public Task findTask(final String taskId) {
		final String t = Integer.toString(SecurityUtil.getLoggedInUserTenantId());
		return this.taskService.createTaskQuery().taskId(taskId).taskTenantId(t).list().get(0);

		// .includeProcessVariables().includeTaskLocalVariables()
		// includeProcessVariables nem kell talán, mert getProcessInstance révén úgyis meglesz
		// az includeTaskLocalVariables viszont nem tudom mi // TODO: kinyomozni
	}

	/**
	 * {@link Task} objektum kikeresése a processInstanceId alapján
	 * @param processDefinitionId
	 * @return
	 */
	public Task findTaskByProcDefId(final String processInstanceId) {
		final String t = Integer.toString(SecurityUtil.getLoggedInUserTenantId());
		final List<Task> list = this.taskService.createTaskQuery().processInstanceId(processInstanceId).taskTenantId(t).list();
		if (!list.isEmpty()) {
			return list.get(0);
		}
		return null;
	}

	/**
	 * user aktuális, active task-jai
	 *
	 * @param username
	 * @return
	 */
	public List<Task> findTasks(final int userId) {

		final String u = Integer.toString(userId);
		final String t = Integer.toString(SecurityUtil.getLoggedInUserTenantId());

		List<Task> tasks = this.taskService.createTaskQuery().taskAssignee(u).taskTenantId(t).orderByTaskCreateTime().active().desc().list();

		for (final String roleTag : WefaStringUtil.getUserRoleAssigneeTags(this.userService.findOne(userId))) {
			tasks = ListUtils.union(tasks, this.taskService.createTaskQuery().taskAssignee(roleTag).taskTenantId(t).orderByTaskCreateTime().active().desc().list());
		}

		return tasks;
	}

	/**
	 * régi/historic folyamat példány a processInstanceId-ja alapján
	 *
	 * @param processInstanceId
	 * @return
	 */
	public HistoricProcessInstance getHistoricProcessInstance(final String processInstanceId) {
		return this.historyService.createHistoricProcessInstanceQuery().includeProcessVariables().processInstanceId(processInstanceId).singleResult();
	}

	/**
	 * {@link ProcessDefinition} a processDefinitionId alapján
	 *
	 * @param processDefinitionId
	 * @return
	 */
	public ProcessDefinition getProcessDefinition(final String processDefinitionId) {

		final ProcessDefinitionQuery processDefinitionQuery = this.repositoryService.createProcessDefinitionQuery();
		return processDefinitionQuery.processDefinitionId(processDefinitionId).singleResult();

	}

	/**
	 * {@link ProcessDefinition} a processDefinitionKey alapján, az utolsó verziót adja (ha van már több)
	 * 
	 * @param processDefinitionKey
	 * @param tenantId TODO
	 * @return
	 */
	public ProcessDefinition getProcessDefinitionByKey(final String processDefinitionKey, final String tenantId) {
		final ProcessDefinitionQuery processDefinitionQuery = this.repositoryService.createProcessDefinitionQuery();
		return processDefinitionQuery.processDefinitionTenantId(tenantId).processDefinitionKey(processDefinitionKey).latestVersion().singleResult();

	}

	/**
	 * futó/aktuális folyamat példány ({@link ProcessInstance}) a processInstanceId alapján
	 *
	 * @param processInstanceId
	 * @return
	 */
	public ProcessInstance getProcessInstance(final String processInstanceId) {
		return this.runtimeService.createProcessInstanceQuery().includeProcessVariables().processInstanceId(processInstanceId).processInstanceTenantId(Integer.toString(SecurityUtil.getLoggedInUserTenantId())).singleResult();
	}

	/**
	 * futó/aktuális folyamat példány az executionId-ja alapján
	 *
	 * @param execId
	 * @return
	 */
	public ProcessInstance getProcessInstanceByExecId(final String executionId) {

		// TODO: az executionId és processInstanceId hogyan viszonyul egymáshoz?

		final Execution execution = this.runtimeService.createExecutionQuery().executionId(executionId).singleResult();
		return this.getProcessInstance(execution.getProcessInstanceId());
	}

	/**
	 * folyamat példány ({@link ProcessInstance}) kommentjei
	 * 
	 * @param processInstanceId
	 * @return
	 */
	public List<Comment> getProcessInstanceComments(final String processInstanceId) {
		return this.taskService.getProcessInstanceComments(processInstanceId);
	}

	/**
	 * folyamat def.-ek listája...
	 *
	 * @param onlyLatestVersions
	 * 		Activi-ben van verziózás a {@link ProcessDefinition} kapcsán
	 * @return
	 */
	public List<ProcessDefinition> listProcessDefinitions(final boolean onlyLatestVersions) {

		// TODO: lehet indítani régebbi verziót/változatot is? nem engedtük, de lehet, hogy engedné

		final ProcessDefinitionQuery processDefinitionQuery = this.repositoryService.createProcessDefinitionQuery();

		if (onlyLatestVersions) {
			processDefinitionQuery.latestVersion();
		}

		return processDefinitionQuery.processDefinitionTenantId(Integer.toString(SecurityUtil.getLoggedInUserTenantId())).orderByProcessDefinitionKey().asc().list();
	}

	/**
	 * @param processDefinitionId
	 * @return
	 * 		BPMN (XML) fájl ({@link ProcessDefinition} leíró fájl) (null, ha nincs/nincs még)
	 */
	public byte[] loadBackDeployedProcDefBpmnXmlData(final String processDefinitionId) {
		try {
			return this.loadBackDeployedProcessResource(processDefinitionId, ".bpmn").values().iterator().next();
		} catch (final java.util.NoSuchElementException e) {
			return null;
		}
	}

	/**
	 * @param processDefinitionId
	 * @return
	 * 		PNG kép (null, ha nincs/nincs még)
	 */
	public byte[] loadBackDeployedProcDefDiagramImg(final String processDefinitionId) {
		try {
			return this.loadBackDeployedProcessResource(processDefinitionId, ".png").values().iterator().next();
		} catch (final java.util.NoSuchElementException e) {
			return null;
		}
	}

	/**
	 * @param processDefinitionId
	 * @return
	 * 		(üres list/map, ha nincs/nincs még egy sem (pl.: új proc. def létrehozás közben köztes mentési lépéseknél)
	 */
	public LinkedHashMap<String, String> loadBackDeployedProcDefGroovyScriptStrings(final String processDefinitionId) {

		final LinkedHashMap<String, byte[]> map1 = this.loadBackDeployedProcessResource(processDefinitionId, ".groovy");
		final LinkedHashMap<String, String> map2 = new LinkedHashMap<>();

		try {

			for (final Entry<String, byte[]> entry : map1.entrySet()) {
				map2.put(StringUtils.removeEnd(entry.getKey(), ".groovy"), new String(entry.getValue(), StandardCharsets.UTF_8));
			}

		} catch (final Exception e) {
			throw new WefaGeneralException("loadBackDeployedProcessGroovyScript error!", e);
		}

		return map2;
	}

	/**
	 * új folyamat példány ({@link ProcessInstance}) indítása...
	 *
	 * @param processDefinitionId
	 * @param variables
	 * 		amennyiben nem null/üres, akkor ezeket rögtön beállítjuk a processInstance-nak
	 * @return
	 */
	public ProcessInstance startProcess(final String processDefinitionId, final Map<String, ? extends Object> intialProcVariables) {

		final ProcessInstance processInstance = this.runtimeService.startProcessInstanceById(processDefinitionId);

		if (MapUtils.isNotEmpty(intialProcVariables)) {
			this.setProcessInstanceVars(processInstance.getId(), intialProcVariables);
		}

		return processInstance;
	}

	@Secured(UserAuth.ROLE_ANONYMOUS_STR)
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ProcessInstance startProcessFromPublicForm(final int tenantId, final String processDefinitionId, final Map intialProcVariables) {

		ToolboxAssert.isTrue(SecurityUtil.isAnonymous());

		// ---

		try {

			SecurityUtil.setSystemUser();

			if (MapUtils.isNotEmpty(intialProcVariables) && intialProcVariables.get("fileIds") != null) {

				final JSONArray jsonArray = new JSONArray(intialProcVariables.get("fileIds").toString());
				final JSONArray jsonArray2 = new JSONArray();

				for (int i = 0; i < jsonArray.length(); i++) {

					final int fileDescriptorId = jsonArray.getInt(i);

					final FileDescriptor fd = this.fileStoreService.getFile2(fileDescriptorId);
					this.fileDescriptorJdbcRepository.delete(fd);

					final FileDescriptor fd2 = new FileDescriptor();

					try {
						BeanUtils.copyProperties(fd2, fd);
						fd2.setId(null);
						fd2.setTenantId(null);
						fd2.setCreatedBy(null);
						fd2.setCreatedOn(null);
						fd2.setModifiedBy(null);
						fd2.setModifiedOn(null);
						fd2.setStatus(ToolboxSysKeys.FileDescriptorStatus.NORMAL);

						JdbcRepositoryManager.setTlTenantId(tenantId);

						jsonArray2.put(this.fileDescriptorJdbcRepository.save(fd2).getId());
						intialProcVariables.put("fileIds", jsonArray2.toString()); // TODO: most csak fix fileIds névre működik

					} catch (final Exception e) {
						throw new WefaGeneralException(e);
					} finally {
						JdbcRepositoryManager.clearTlTenantId();
					}

				}

			}

			// ---

			try {
				JdbcRepositoryManager.setTlTenantId(tenantId);

				final ProcessInstance processInstance = this.runtimeService.startProcessInstanceById(processDefinitionId);
				this.setProcessInstanceVars(processInstance.getId(), intialProcVariables);

				return processInstance;

			} finally {
				JdbcRepositoryManager.clearTlTenantId();
			}

		} finally {
			SecurityUtil.setAnonymous();
		}

	}

	/**
	 * @param processDefinitionId
	 * @param fileExtension
	 * 		ponttal kezdve, pl.: ".png")
	 * @return
	 */
	private LinkedHashMap<String, byte[]> loadBackDeployedProcessResource(final String processDefinitionId, final String fileExtension) {

		final ProcessDefinitionQuery processDefinitionQuery = this.repositoryService.createProcessDefinitionQuery();
		final ProcessDefinition processDefinition = processDefinitionQuery.processDefinitionId(processDefinitionId).singleResult();

		log.debug(String.format("getDeploymentResourceNames: %s", this.repositoryService.getDeploymentResourceNames(processDefinition.getDeploymentId())));

		final LinkedHashMap<String, byte[]> retMap = new LinkedHashMap<>();

		this.repositoryService.getDeploymentResourceNames(processDefinition.getDeploymentId()).stream().filter(x -> x.toLowerCase().endsWith(fileExtension.toLowerCase())).forEach(x -> {

			try (InputStream is = this.repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), x)) {
				retMap.put(x, IOUtils.toByteArray(is));

			} catch (final Exception e) {
				throw new WefaGeneralException("loadBackDeployedProcessResource error!", e);
			}

		});

		return retMap;

	}

	// /**
	// * debug célra...
	// */
	// public void exportBaTable() {
	//
	// final ProcessDefinitionQuery processDefinitionQuery = this.repositoryService.createProcessDefinitionQuery();
	// final List<ProcessDefinition> processDefinitions = processDefinitionQuery.list();
	//
	// try {
	// FileUtils.cleanDirectory(new File("D://TMP//71//"));
	// } catch (final IOException e) {
	// throw new WefaServiceException(e);
	// }
	//
	// for (final ProcessDefinition processDefinition : processDefinitions) {
	// final List<String> deploymentResourceNames = this.repositoryService.getDeploymentResourceNames(processDefinition.getDeploymentId());
	// for (final String deploymentResourceName : deploymentResourceNames) {
	// try (InputStream is = this.repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), deploymentResourceName)) {
	//
	// String str = deploymentResourceName;
	//
	// if (str.contains("\\")) {
	// final String[] split = deploymentResourceName.split("\\\\");
	// str = split[split.length - 1];
	// }
	//
	// FileUtils.copyInputStreamToFile(is, new File("D://TMP//71//" + processDefinition.getId().replaceAll(":", "-") + " - " + str));
	// } catch (final IOException e) {
	// throw new WefaServiceException(e);
	// }
	// }
	// }
	//
	// }
	
}