package hu.lanoga.toolbox.vaadin.component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.vaadin.addons.ComboBoxMultiselect;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.UserAuth;
import hu.lanoga.toolbox.codestore.CodeStoreItem;
import hu.lanoga.toolbox.codestore.CodeStoreItemService;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.user.User;

public class UserRoleField extends CustomField<String> {

	private String value;

	private ComboBoxMultiselect<CodeStoreItem> cmbUserRoles;

	/**
	 * akin épp változatatni akarunk
	 */
	@SuppressWarnings("unused")
	private User userAsIsNow;

	public UserRoleField(final String caption) {
		this.setCaption(caption);
	}

	public void setUserAsIsNow(User userAsIsNow) {
		this.userAsIsNow = userAsIsNow;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Component initContent() {

		final CodeStoreItemService codeStoreItemService = ApplicationContextHelper.getBean(CodeStoreItemService.class);

		this.cmbUserRoles = new ComboBoxMultiselect<>();
		this.cmbUserRoles.setWidth("100%");
		this.cmbUserRoles.setItemCaptionGenerator(codeStoreItem -> codeStoreItem.getCaptionCaption());

		final List<CodeStoreItem> possibleUserRoleList = codeStoreItemService.findAllByType(ToolboxSysKeys.UserAuth.CODE_STORE_TYPE_ID);

		// itt leszedünk pár kiválasztható role-t
		// lásd még hu.lanoga.toolbox.user.UserService.addAdditionalRoles(User)
		
		{

			Iterator<CodeStoreItem> iterator1 = possibleUserRoleList.iterator();
			while (iterator1.hasNext()) {
				CodeStoreItem codeStoreItem = iterator1.next();
				if (codeStoreItem.getCommand().toUpperCase().equals(UserAuth.ROLE_ANONYMOUS_STR)) {
					iterator1.remove();
				}
				if (codeStoreItem.getCommand().toUpperCase().equals(UserAuth.ROLE_LCU_STR)) {
					iterator1.remove();
				}
			}

		}

		if (!SecurityUtil.hasSuperAdminRole()) {

			// ezeket a role-okat csak a superadmin teheti rá egy adott user-re

			Iterator<CodeStoreItem> iterator3 = possibleUserRoleList.iterator();
			while (iterator3.hasNext()) {
				CodeStoreItem codeStoreItem = iterator3.next();
				if (codeStoreItem.getCommand().toUpperCase().equals(UserAuth.ROLE_TENANT_OVERSEER_STR)) {
					iterator3.remove();
				}
			}

		}
		
		if (!SecurityUtil.hasSuperAdminRole() || SecurityUtil.getLoggedInUserTenantId() != 1) {
			
			// ezeket a role-okat csak a superadmin teheti rá egy adott user-re 
			// és
			// ezeket a role-okat csak akkor lehet user-re tenni, ha épp az 1-es tenant-ot szerkesztjük (a super felületről stb.)
			
			Iterator<CodeStoreItem> iterator2 = possibleUserRoleList.iterator();
			while (iterator2.hasNext()) {
				CodeStoreItem codeStoreItem = iterator2.next();
				if (codeStoreItem.getCommand().toUpperCase().startsWith("ROLE_SUPER")) {
					iterator2.remove();
				}
			}
			
		}

		// ---

		this.cmbUserRoles.setItems(possibleUserRoleList);

		this.cmbUserRoles.addValueChangeListener(x -> {

			final Set<CodeStoreItem> roles = this.cmbUserRoles.getValue();

			final JSONArray jsonArray = new JSONArray();
			for (final CodeStoreItem role : roles) {
				jsonArray.put(role.getId());
			}

			final String oldValue = this.value;

			this.value = jsonArray.toString();

			final Collection<?> listeners = this.getListeners(ValueChangeEvent.class);
			for (final Object listener : listeners) {
				((ValueChangeListener<String>) listener).valueChange(new ValueChangeEvent<>(this, oldValue, true));
			}

		});

		// ---

		if (this.value != null) {

			final Set<CodeStoreItem> initialRoles = new HashSet<>();

			final JSONArray jsonArray = new JSONArray(this.value);

			for (int i = 0; i < jsonArray.length(); i++) {

				final int roleId = jsonArray.getInt(i);

				for (final CodeStoreItem userRole : possibleUserRoleList) {
					if (roleId == userRole.getId().intValue()) {
						initialRoles.add(userRole);
						break;
					}
				}

			}

			this.cmbUserRoles.setValue(initialRoles);
		}

		// ---

		return this.cmbUserRoles;
	}

	@Override
	public String getValue() {
		return this.value;

	}

	@SuppressWarnings("hiding")
	@Override
	protected void doSetValue(final String value) {
		this.value = value;
	}

}