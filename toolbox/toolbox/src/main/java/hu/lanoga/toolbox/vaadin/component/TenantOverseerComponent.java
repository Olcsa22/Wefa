package hu.lanoga.toolbox.vaadin.component;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.web.context.request.RequestContextHolder;

import com.teamunify.i18n.I;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.UserAuth;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryTenantManager;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.JmsManager;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.tenant.Tenant;
import hu.lanoga.toolbox.tenant.TenantService;
import hu.lanoga.toolbox.user.User;
import hu.lanoga.toolbox.user.UserService;
import hu.lanoga.toolbox.vaadin.util.UiHelper;

/**
 * aka "jump"
 * 
 * @see UserAuth#ROLE_TENANT_OVERSEER_STR
 * @see UserAuth#ROLE_SUPER_TENANT_OVERSEER_STR
 */
public class TenantOverseerComponent extends VerticalLayout {

	public ComboBox<Pair<Tenant, User>> cmbTenantOverseer;
	public Button btnSave;

	public void initLayout() {

		this.removeAllComponents();

		// ---
		
		SecurityUtil.limitAccessTenantOverseer();
		
		// ---

		final List<Pair<Tenant, User>> allowedTenantUserList;

		if (SecurityUtil.hasRole(ToolboxSysKeys.UserAuth.ROLE_SUPER_TENANT_OVERSEER_STR)) {
			allowedTenantUserList = ApplicationContextHelper.getBean(TenantService.class).findAllForSuperOverseer();
		} else {
			allowedTenantUserList = ApplicationContextHelper.getBean(TenantService.class).findAllForOverseer();
		}
		
		// ---

		if (allowedTenantUserList == null || allowedTenantUserList.isEmpty()) {

			final Notification notification = Notification.show(I.trc("Notification", "You don't have avatars in any other company!"), Notification.Type.WARNING_MESSAGE);

			notification.addCloseListener(x -> {
				UiHelper.closeParentWindow(this);
			});

			return;
		}
		
		// ---

		this.cmbTenantOverseer = new ComboBox<>(I.trc("Caption", "Choose company (and user)"));
		this.cmbTenantOverseer.setWidth(100, Unit.PERCENTAGE);
		this.cmbTenantOverseer.setItems(allowedTenantUserList);
		this.cmbTenantOverseer.setItemCaptionGenerator(x -> x.getLeft().getName() + " (" + x.getRight().getUsername() + ", " + I.trc("Caption", "SysID") + ": " + x.getLeft().getId() + ")");
		this.cmbTenantOverseer.setEmptySelectionAllowed(false);
		this.cmbTenantOverseer.setValue(Pair.of(ApplicationContextHelper.getBean(TenantService.class).findOne(SecurityUtil.getLoggedInUserTenantId()), ApplicationContextHelper.getBean(UserService.class).findOne(SecurityUtil.getLoggedInUser().getId())));
		this.addComponent(this.cmbTenantOverseer);

		this.btnSave = new Button(I.trc("Button", "Switch to the selected company"));
		this.btnSave.addStyleName(ValoTheme.BUTTON_FRIENDLY);
		this.btnSave.setIcon(FontAwesome.SAVE);
		this.btnSave.setDisableOnClick(true);
		this.btnSave.setWidth(null);
		this.btnSave.addClickListener(y -> {

			// ---

			final Pair<Tenant, User> cmbValue = this.cmbTenantOverseer.getValue();

			boolean isSuperOverseer = SecurityUtil.hasRole(ToolboxSysKeys.UserAuth.ROLE_SUPER_TENANT_OVERSEER_STR);

			if (cmbValue.getRight().getId() == null && isSuperOverseer) {

				// ha ROLE_SUPER_TENANT_OVERSEER_STR és olyan tenant-ont választott, ahol nincs még usere
				// (mj.: a ComboBox-ban a Pair right részben egy dummy object szerepel, aminek userId-ja null)

				insertAndSetJumpUser(cmbValue);

			} else if (isSuperOverseer) {

				updateAndSetJumpUser(cmbValue.getRight());

			} else {

				SecurityUtil.setUser(cmbValue.getRight());

			}

			// ---

			UiHelper.closeParentWindow(this);

			this.sendOversserChangeJmsMsg();

		});

		this.addComponent(this.btnSave);

	}

	private void insertAndSetJumpUser(final Pair<Tenant, User> tenantUserPair) {

		final ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();
		final UserService userService = ApplicationContextHelper.getBean(UserService.class);
		final User loggedInUserFreshFromDb = userService.findOne(loggedInUser.getId());

		try {

			SecurityUtil.setSystemUser();
			JdbcRepositoryTenantManager.setTlTenantId(tenantUserPair.getLeft().getId());

			final User newUserToBeInsertedNow = new User();

			BeanUtils.copyProperties(newUserToBeInsertedNow, loggedInUserFreshFromDb);
			newUserToBeInsertedNow.setTenantId(null);
			newUserToBeInsertedNow.setId(null);

			// ---

			if (!loggedInUserFreshFromDb.getUsername().startsWith("jump.")) {
				// van egy olyan konvenciónk, hogy ezek a userek "jump." prefix-szel kezdődjenek,
				// így biztosan nem fog másik userrel ütközni (értsd ami esetleg van már ebben a cél tenant-ban)
				newUserToBeInsertedNow.setUsername("jump." + newUserToBeInsertedNow.getUsername());
			}

			// ---

			if (loggedInUserFreshFromDb.getParentId() != null) {

				newUserToBeInsertedNow.setParentId(loggedInUserFreshFromDb.getParentId());

			} else if (loggedInUserFreshFromDb.getTenantId().equals(1)) {

				newUserToBeInsertedNow.setParentId(loggedInUserFreshFromDb.getId());

			}

			// ---

			final User newUserInsertedNow = userService.save(newUserToBeInsertedNow, true);

			// ---

			// TODO: ha több művelet van, akkor kerüljön a service-ba @Transactional-lel

			// TODO: gondoljuk át mi lesz a child userek jelszava
			// TODO: egyelőre kiszedtem inkább, maradjon az a random, amit a userService.save generál
			// userService.updateUserPasswordByUserId(newUserInsertedNow.getId(), loggedInUserFreshFromDb.getPassword());

			// TODO: mi a helyzet a user kv settings-szel stb.? szerintem ezt le kellene másolni (de nem csak itt, hanem minden átlépésnél is akár)

			SecurityUtil.setUser(newUserInsertedNow);

		} catch (final Exception e) {
			throw new ToolboxGeneralException("Error during insertAndSetJumpUser!", e);
		} finally {
			
			JdbcRepositoryTenantManager.clearTlTenantId();
			
			if (SecurityUtil.isSystem()) {  // ha exception miatt nem sikerült beléptetni senkit és pont a köztes system user maradt volna
				SecurityUtil.setUser(loggedInUserFreshFromDb);			
			}
			
			
		}
	}

	private void updateAndSetJumpUser(User jumpUser) {

		final ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();
		final UserService userService = ApplicationContextHelper.getBean(UserService.class);
		final User loggedInUserFreshFromDb = userService.findOne(loggedInUser.getId());

		try {

			SecurityUtil.setSystemUser();
			JdbcRepositoryTenantManager.setTlTenantId(jumpUser.getTenantId());

			if (jumpUser.getUsername().startsWith("jump.") && !SecurityUtil.hasAllTheRolesOfTheOtherUser(loggedInUserFreshFromDb, jumpUser)) {
				
				// TODO: itt hasAllTheRolesOfTheOtherUser helyett pontos egyezést kellene nézni és akkor is rámenti a jump-osra, ha kevesebb joga van már a usernek
				
				jumpUser.setUserRoles(loggedInUserFreshFromDb.getUserRoles());
				jumpUser = userService.save(jumpUser);
			}

			// ---

			SecurityUtil.setUser(jumpUser);

		} catch (final Exception e) {
			throw new ToolboxGeneralException("Error during updateAndSetJumpUser!", e);
		} finally {
			
			JdbcRepositoryTenantManager.clearTlTenantId();
			
			if (SecurityUtil.isSystem()) { // ha exception miatt nem sikerült beléptetni senkit és pont a köztes system user maradt volna
				SecurityUtil.setUser(loggedInUserFreshFromDb);			
			}
			
			
		}
	}

	public void sendOversserChangeJmsMsg() {
		final HashMap<String, Object> hm = new HashMap<>();
		hm.put("type", ToolboxSysKeys.JmsMsgType.TENANT_OVERSEER_MSG.name());
		JmsManager.send(JmsManager.buildDestStr(ToolboxSysKeys.JmsDestinationMode.HTTP_SESSION, RequestContextHolder.currentRequestAttributes().getSessionId(), "notification"), hm);
	}

}