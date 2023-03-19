package hu.lanoga.wefa.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.activiti.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.email.EmailTemplateService;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.user.User;
import hu.lanoga.toolbox.user.UserService;
import hu.lanoga.wefa.SysKeys;
import hu.lanoga.wefa.exception.WefaGeneralException;
import hu.lanoga.wefa.service.ActivitiHelperService;

public class ActivitiHelperUtil {

	private ActivitiHelperUtil() {
		//
	}

	public static void uploadFileIntoExternalSystem(final String url, final String fileIdsFieldName, final ExecutionEntityImpl execution) {

		final ActivitiHelperService activitiHelperService = ApplicationContextHelper.getBean(ActivitiHelperService.class);
		final ProcessInstance processInstance = activitiHelperService.getProcessInstanceByExecId(execution.getId());
		final Map<String, Object> m = processInstance.getProcessVariables();

		final List<File> files = getFiles(fileIdsFieldName, m);
		
		if (files.isEmpty()) {
			return;
		}

		try {

			final CloseableHttpClient httpClient = HttpClients.createDefault();

			final HttpPost uploadFile;
			if (StringUtils.isNotBlank(url)) {
				uploadFile = new HttpPost(ApplicationContextHelper.getConfigProperty(url));
			} else {
				uploadFile = new HttpPost(ApplicationContextHelper.getConfigProperty("egodoq.upload-url"));
			}

			final MultipartEntityBuilder builder = MultipartEntityBuilder.create();

			for (File file : files) {
				builder.addBinaryBody(
						"file",
						new FileInputStream(file), // TODO: ez mikor lesz lezárva
						ContentType.APPLICATION_OCTET_STREAM,
						file.getName());
			}

			org.apache.http.HttpEntity multipart = builder.build();
			uploadFile.setEntity(multipart);

			final CloseableHttpResponse response = httpClient.execute(uploadFile);
			org.apache.http.HttpEntity responseEntity = response.getEntity();

		} catch (Exception e) {
			throw new WefaGeneralException("Error during POST request! ", e);
		}

		// másik próba, ami nem működött
		// try {
		//
		// final HttpHeaders headers = new HttpHeaders();
		// headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		//
		// final MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		// for (File file : files) {
		// body.add("file", file);
		// }
		//
		// final HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
		//
		// String serverUrl = ApplicationContextHelper.getConfigProperty("egodoq.upload-url");
		//
		// RestTemplate restTemplate = new RestTemplate();
		// ResponseEntity<String> response = restTemplate.postForEntity(serverUrl, requestEntity, String.class); // , params
		//
		// } catch (Exception e) {
		// throw new WefaGeneralException("Error during POST request! ", e);
		// }
	}

	private static List<File> getFiles(String fileIdsFieldName, Map<String, Object> processVariables) {

		Object object = processVariables.get(fileIdsFieldName);
		
		if (object == null) {
			return new ArrayList<>();
		}
		
		String fileIds = object.toString(); // pl: "fileIds"

		final JSONArray array = new JSONArray(fileIds);
		final List<File> files = new ArrayList<>();
		for (Object o : array) {
			FileDescriptor file = ApplicationContextHelper.getBean(FileStoreService.class).getFile2(Integer.valueOf(o.toString()));
			files.add(file.getFile());
		}
		return files;
	}

	public static void sendInternalNotifEmail(final int userId, final ExecutionEntityImpl execution) {

		final UserService userService = ApplicationContextHelper.getBean(UserService.class);
		final User user = userService.findOne(userId);

		sendInternalNotifEmail(user.getEmail(), null, false, execution);
	}

	public static void sendInternalNotifEmail(final String toEmail, final ExecutionEntityImpl execution) {
		sendInternalNotifEmail(toEmail, null, false, execution);
	}

	public static void sendInternalNotifEmail(final String toEmail, final String fileIdsFieldName, final boolean includeJSONAsAttachment, final ExecutionEntityImpl execution) {

		final ActivitiHelperService activitiHelperService = ApplicationContextHelper.getBean(ActivitiHelperService.class);
		final EmailTemplateService emailTemplateService = ApplicationContextHelper.getBean(EmailTemplateService.class);

		final ProcessInstance processInstance = activitiHelperService.getProcessInstanceByExecId(execution.getId());
		final Map<String, Object> m = processInstance.getProcessVariables();

		final StringBuilder sb = new StringBuilder();

		for (final Entry<String, Object> entry : m.entrySet()) {
			if (!"staleDataCheckTs".equalsIgnoreCase(entry.getKey()) && entry.getValue() != null) {
				sb.append("<b>");
				sb.append(Jsoup.clean(entry.getKey(), Safelist.none()));
				sb.append("</b>:&nbsp;");
				sb.append(Jsoup.clean(entry.getValue().toString(), Safelist.none()));
				sb.append("<br/>");
			}
		}

		m.put("recipient", toEmail);
		m.put("notifText", sb.toString());

		// ---

		FileDescriptor tempFile = null;

		if (includeJSONAsAttachment) {

			final JSONObject jsonVariables = new JSONObject(m);

			tempFile = ApplicationContextHelper.getBean(FileStoreService.class).createTmpFile2("variables.json", ToolboxSysKeys.FileDescriptorLocationType.PROTECTED_FOLDER, ToolboxSysKeys.FileDescriptorSecurityType.ADMIN_OR_CREATOR);

			try {
				FileUtils.writeStringToFile(tempFile.getFile(), jsonVariables.toString(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new WefaGeneralException(e);
			}

		}

		// ---

		JSONArray fileArray;

		if (StringUtils.isNotBlank(fileIdsFieldName)) {
			fileArray = new JSONArray(m.get(fileIdsFieldName).toString());
		} else {
			fileArray = new JSONArray();
		}

		if (tempFile != null) {
			fileArray.put(tempFile.getId());
		}

		if (fileArray.length() > 0) {
			emailTemplateService.addMail(SecurityUtil.getLoggedInUser(), toEmail, SysKeys.EmailTemplateType.EMAIL_NOTIF, m, I18nUtil.getLoggedInUserLocale(), ToolboxSysKeys.EmailStatus.CREATED, fileArray.toString());
		} else {
			emailTemplateService.addMail(SecurityUtil.getLoggedInUser(), toEmail, SysKeys.EmailTemplateType.EMAIL_NOTIF, m, I18nUtil.getLoggedInUserLocale());
		}

	}

	public static void sendExternalNotifEmail(final String toEmailFieldId, final int emailTemplateId, final List<String> includeFieldsInEmail, final String notifText, final ExecutionEntityImpl execution) {

		final ActivitiHelperService activitiHelperService = ApplicationContextHelper.getBean(ActivitiHelperService.class);
		final EmailTemplateService emailTemplateService = ApplicationContextHelper.getBean(EmailTemplateService.class);

		final ProcessInstance processInstance = activitiHelperService.getProcessInstanceByExecId(execution.getId());
		final Map<String, Object> m = processInstance.getProcessVariables();

		final Set<String> includeFieldsInEmailSet = new HashSet<>(includeFieldsInEmail);
		String toEmail = null;
		
		for (final Entry<String, Object> entry : m.entrySet()) {

			if (entry.getValue() == null) {
				continue;
			}

			if (includeFieldsInEmailSet.contains(entry.getKey())) {
				m.put(entry.getKey(), Jsoup.clean(entry.getValue().toString(), Safelist.none()));
			}

			if (toEmailFieldId.equals(entry.getKey())) {
				toEmail = entry.getValue().toString();
			}

		}

		m.put("recipient", Jsoup.clean(toEmail, Safelist.none())); // TODO: ez nem email cím sanitizer... az email cím külső inputból származik, meg kellene jobban szűrni itt még, Jsoup nem erre való... megnézni az OWASP library-t stb.
		m.put("notifText", Jsoup.clean(notifText, Safelist.none()));

		emailTemplateService.addMail(SecurityUtil.getLoggedInUser(), toEmail, emailTemplateId, m, "hu"); // TODO: egyelőre itt fix magyar

	}

}
