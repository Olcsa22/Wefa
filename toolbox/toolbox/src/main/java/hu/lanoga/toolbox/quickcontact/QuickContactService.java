package hu.lanoga.toolbox.quickcontact;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamunify.i18n.I;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.email.EmailTemplateService;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.service.DefaultCrudService;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.tenant.TenantKeyValueSettings;
import hu.lanoga.toolbox.tenant.TenantKeyValueSettingsService;
import hu.lanoga.toolbox.user.User;
import hu.lanoga.toolbox.user.UserService;
import hu.lanoga.toolbox.vaadin.util.UiHelper;

@Service
public class QuickContactService extends DefaultCrudService<QuickContact, QuickContactJdbcRepository> {

	@Autowired
	private UserService userService;

	@Autowired
	private EmailTemplateService emailTemplateService;

	@Autowired
	private TenantKeyValueSettingsService tenantKeyValueSettingsService;

	@Transactional
	public void sendQuickContactEmailNotifications() {
		
		SecurityUtil.limitAccessSystem();

		StringBuilder sbBody = new StringBuilder();

		this.repository.lockTable(); // TODO: ennél szebben kellene... lásd EmailScheduler...
		
		final List<QuickContact> list = this.repository.findAllForEmailNotification();

		if (!list.isEmpty()) {
			
			for (QuickContact qc : list) {
			
				sbBody.append("<p>");
				
				sbBody.append(I.trc("Caption", "Name") + ": " + qc.getContactName());
				
				if (StringUtils.isNotBlank(qc.getEmail())) {
					sbBody.append("<br>");
					sbBody.append(I.trc("Caption", "Email") + ": " + qc.getEmail());
				}

				if (StringUtils.isNotBlank(qc.getPhoneNumber())) {
					sbBody.append("<br>");
					sbBody.append(I.trc("Caption", "Phone") + ": " + qc.getPhoneNumber());
				}

				String jumpUrlStr = UiHelper.buildJumpUrlStr("quick-contacts", "id", qc.getId().toString(), true, false);
				
				sbBody.append("<br>");
				sbBody.append("<br>");
				sbBody.append(I.trc("Caption", "Please log in to see the details"));
				sbBody.append(":<br>");
				sbBody.append("<a href=\"" +  jumpUrlStr + "\">" + jumpUrlStr + "</a>");
				
				sbBody.append("</p>");

				qc.setIsSent(true);
				
				this.repository.save(qc);
			}

			Set<Integer> quickContactEmailReceiverIds = getQuickContactEmailReceiverIds();
			if (quickContactEmailReceiverIds != null && !quickContactEmailReceiverIds.isEmpty()) {
				for (Integer quickContactEmailReceiverId : quickContactEmailReceiverIds) {
					final User user = userService.findOne(quickContactEmailReceiverId);

					final Map<String, Object> values = new HashMap<>();
					values.put("recipient", user.getUsername());
					values.put("data", sbBody.toString());

					emailTemplateService.addMail(SecurityUtil.getLoggedInUser(), user.getEmail(), ToolboxSysKeys.EmailTemplateType.QUICK_CONTACT_NOTIFICATION, values, I18nUtil.getTenantLocale());
				}
			}

		}
	}

	public Set<Integer> getQuickContactEmailReceiverIds() {
		final TenantKeyValueSettings quickContactEmailReceiversForInit = tenantKeyValueSettingsService.findOneByKey(ToolboxSysKeys.TenantKeyValueSettings.QUICK_CONTACT_RECEIVERS);

		if (quickContactEmailReceiversForInit != null) {
			final Set<Integer> userIds = new HashSet<>();
			final JSONArray jsonArray = new JSONArray(quickContactEmailReceiversForInit.getKvValue());

			for (int i = 0; i < jsonArray.length(); i++) {
				final int userId = jsonArray.getInt(i);
				userIds.add(userId);
			}

			return userIds;
		}

		return null;
	}

}