package hu.lanoga.toolbox.vaadin.component;

import com.teamunify.i18n.I;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.quickcontact.QuickContact;
import hu.lanoga.toolbox.quickcontact.QuickContactService;
import hu.lanoga.toolbox.repository.DefaultInMemoryRepository;
import hu.lanoga.toolbox.service.RapidLazyEnhanceCrudService;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.tenant.TenantKeyValueSettings;
import hu.lanoga.toolbox.tenant.TenantKeyValueSettingsService;
import hu.lanoga.toolbox.user.User;
import hu.lanoga.toolbox.user.UserService;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponent;
import hu.lanoga.toolbox.vaadin.component.crud.FormLayoutCrudFormComponent;
import hu.lanoga.toolbox.vaadin.component.crud.MultiFormLayoutCrudFormComponent;

import java.util.Set;

public class QuickContactComponent extends VerticalLayout {

	private final DefaultInMemoryRepository<User> userInMemoryRepository = new DefaultInMemoryRepository<>(User.class);
	private final RapidLazyEnhanceCrudService<User, DefaultInMemoryRepository<User>> userInMemoryService = new RapidLazyEnhanceCrudService<User, DefaultInMemoryRepository<User>>(this.userInMemoryRepository) {
		@Override
		public User enhance(User user) {
			return user;
		}
	};

	private int userRoleId;

	public QuickContactComponent(final int userRoleId) {
		this.userRoleId = userRoleId;
	}

	protected void initLayout() {

		this.removeAllComponents();

		// ---

		this.removeAllComponents();

		final CrudGridComponent<QuickContact> quickContactCrud = new CrudGridComponent<>(
				QuickContact.class,
				ApplicationContextHelper.getBean(QuickContactService.class),
				() -> new MultiFormLayoutCrudFormComponent<>(() -> new QuickContact.VaadinForm(), null),
				true);
		
		quickContactCrud.getGrid().setFrozenColumnCount(3);

		final Boolean quickContactScheduler = ApplicationContextHelper.getConfigProperty("tools.quick-contact.scheduler.enabled", Boolean.class);

		if (quickContactScheduler != null && quickContactScheduler) { // ha nincs bekapcsolva az email küldés, akkor nincs oka megjeleníteni a gombot
			final Button btnSelectReceivers = new Button(I.trc("Caption", "Notification settings"));
			btnSelectReceivers.setStyleName(ValoTheme.BUTTON_FRIENDLY);
			btnSelectReceivers.setIcon(VaadinIcons.ALARM);
			btnSelectReceivers.addClickListener(x -> {

				final Window receiverDialog = new Window(I.trc("Caption", "Notification settings (receivers)"));

				receiverDialog.setWidth("850px");
				receiverDialog.setModal(true);
				
				userInMemoryRepository.initWithData(ApplicationContextHelper.getBean(UserService.class).findAllUserByRole(this.userRoleId));

				final VerticalLayout vlContent = new VerticalLayout();
				vlContent.setWidth("100%");

				final CrudGridComponent<User> userCrud = new CrudGridComponent<>(
						User.class,
						userInMemoryService,
						() -> new FormLayoutCrudFormComponent<>(() -> new User.VaadinForm()),
						true, null, Grid.SelectionMode.MULTI);

				Set<Integer> quickContactEmailReceiverIds = ApplicationContextHelper.getBean(QuickContactService.class).getQuickContactEmailReceiverIds();
				if (quickContactEmailReceiverIds != null) {
					userCrud.setSelectedItemsWithIds(quickContactEmailReceiverIds);
				}

				userCrud.setMargin(true);
				userCrud.toggleButtonVisibility(false, false, false, false, true, true, true, false);

				final Button btnSave = new Button(I.trc("Button", "Save"));
				btnSave.addStyleName(ValoTheme.BUTTON_PRIMARY);
				btnSave.addClickListener(y -> {

					Set<Integer> selectedItemIds = userCrud.getSelectedItemIds();
					
					final TenantKeyValueSettingsService tenantKeyValueSettingsService = ApplicationContextHelper.getBean(TenantKeyValueSettingsService.class);

					TenantKeyValueSettings quickContactEmailReceivers = tenantKeyValueSettingsService.findOneByKey(ToolboxSysKeys.TenantKeyValueSettings.QUICK_CONTACT_RECEIVERS);

					if (quickContactEmailReceivers != null) {
						quickContactEmailReceivers.setKvValue(selectedItemIds.toString());
					} else {
						quickContactEmailReceivers = new TenantKeyValueSettings();
						quickContactEmailReceivers.setKvKey(ToolboxSysKeys.TenantKeyValueSettings.QUICK_CONTACT_RECEIVERS);
						quickContactEmailReceivers.setKvValue(selectedItemIds.toString());
						quickContactEmailReceivers.setManualEditAllowed(false);
					}

					tenantKeyValueSettingsService.save(quickContactEmailReceivers);

					receiverDialog.close();
				});

				vlContent.addComponents(userCrud, btnSave);

				receiverDialog.setContent(vlContent);

				UI.getCurrent().addWindow(receiverDialog);
			});

			quickContactCrud.addAdditionalFooterToolbar(new HorizontalLayout(btnSelectReceivers));
		}

		this.addComponent(quickContactCrud);

	}
}
