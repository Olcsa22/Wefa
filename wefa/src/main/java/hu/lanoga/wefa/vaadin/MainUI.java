package hu.lanoga.wefa.vaadin;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import org.vaadin.alump.notify.Notify;
import org.vaadin.alump.notify.NotifyItem;
import org.vaadin.sidemenu.SideMenu;

import com.teamunify.i18n.I;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Audio;
import com.vaadin.ui.Window;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.spring.JmsManager;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.vaadin.AbstractToolboxUI;
import hu.lanoga.toolbox.vaadin.component.ChangeLogView;
import hu.lanoga.toolbox.vaadin.component.MyProfileComponent;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import hu.lanoga.wefa.vaadin.procdef.ProcessDefinitionEditorView;
import hu.lanoga.wefa.vaadin.procdef.ProcessDefinitionListView;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Theme("wefa")
// @PreserveOnRefresh // TODO: megfontolandó
@Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET_XHR)
@SpringUI(path = "/")

// @com.vaadin.annotations.JavaScript(value = "https://cdn.conversejs.org/6.0.0/dist/converse.min.js") // chat próba
// @com.vaadin.annotations.StyleSheet(value = "https://cdn.conversejs.org/6.0.0/dist/converse.min.css")
public class MainUI extends AbstractToolboxUI {

	public static class NavConst {

		public static final String TASK_LIST = "";

		public static final String PROCESS_INSTANCE_LIST = "process-instance-list";

		public static final String PROCESS_DEF_LIST = "process-def-list";
		public static final String PROCESS_DEF_EDITOR = "process-def-editor";

		public static final String USER_LIST = "user-list";
	}

	private final SideMenu sideMenu = new SideMenu();

	private String jmsSubsId;

	private final Consumer<Map<String, ?>> jmsConsumer;

	private Integer loggedInUserId;

	public MainUI() {
		super();

		this.loggedInUserId = SecurityUtil.getLoggedInUser().getId();

		this.jmsConsumer = t -> {

			MainUI.this.access(() -> {

				// final Notification n = new Notification(I.trc("Notification", "Új biztosítás!"), Notification.Type.TRAY_NOTIFICATION);
				// n.setPosition(Notification.POSITION_BOTTOM_LEFT);
				// n.show(MainUI.this.getPage());

				this.audio.play();
				
				try {
					
					Notify.show(new NotifyItem()
							.setTitle(I.trc("Notification", "Új feladat"))
							.setBody(I.trc("Notification", "Új feladatot kapott")) // + ": " + task.getId() + " (" + task.getName() + ")"
							// .setIcon(new ExternalResource(VaadinService.getCurrentRequest().getContextPath() + "/assets/logo-64.png"))
							.setTimeout(8));
					
				} catch (org.vaadin.alump.notify.exceptions.NotificationsDeniedByUserException e) {
					log.warn("Notify.show() tray notif failed, userId: " + loggedInUserId, e); 
				}


			});

		};

	}

	@Override
	public void detach() {
		JmsManager.unsubscribe(this.jmsSubsId);
		super.detach();
	}

	@Override
	public void attach() {
		super.attach();
		this.jmsSubsId = JmsManager.subscribe(JmsManager.buildDestStr(ToolboxSysKeys.JmsDestinationMode.USER, Integer.toString(this.loggedInUserId), "new-proc-inst"), new WeakReference<>(this.jmsConsumer));

		// ---------------------------------
		// ---------------------------------

		// XMPP chat proba
		
		// this.getPage().getJavaScript().execute(
		// "converse.initialize({ bosh_service_url: 'http://localhost:7070/http-bind/', "
		// + "show_controlbox_by_default: true, "
		// + "i18n: 'hu', "
		// + "auto_login: true, jid: 'proba@localhost', password: 'proba', "
		// + "locked_domain: 'localhost', "
		// + "auto_subscribe: true, "
		// + "auto_join_on_invite: true, "
		// // + "auto_join_private_chats: *, "
		// + "auto_reconnect: true, "
		// + "auto_away: 600, "
		// + "auto_xa: 1200, "
		// // + "theme: 'concord', "
		// + "auto_list_rooms: true, "
		// + "trusted: false, "
		// + "allow_logout: false "
		// + "});");

		// ---------------------------------
		// ---------------------------------
	}

	private Audio audio;

	@Override
	protected void init(final VaadinRequest vaadinRequest) {
		
		SecurityUtil.limitAccessRoleUser();
		
		// ---

		super.init(vaadinRequest);
		this.setLocale(new Locale("hu"));

		this.setContent(this.sideMenu);

		// ---

		this.sideMenu.addStyleName("side-menu-min-width");
		this.sideMenu.setMenuCaption(null, new ExternalResource("assets/banner_logo.png"));

		this.sideMenu.setUserName(SecurityUtil.getLoggedInUser().getUsername());
		this.sideMenu.setUserIcon(FontAwesome.USER);

		this.sideMenu.clearUserMenu();

		this.sideMenu.addUserMenuItem(I.trc("Caption", "Beállításaim"), FontAwesome.COG, () -> {

			final Window profileDialog = new Window();
			profileDialog.setWidth("850px");
			profileDialog.setModal(true);
			profileDialog.setContent(new MyProfileComponent());

			this.addWindow(profileDialog);

		});

		this.sideMenu.addUserMenuItem(I.trc("Caption", "Kijelentkezés"), FontAwesome.SIGN_OUT, () -> UiHelper.logout(this));

		// ---

		final Navigator navigator = new Navigator(this, this.sideMenu);
		this.setNavigator(navigator);
		this.sideMenu.setNavigator(navigator);

		// ---

		this.sideMenu.addNavigation(I.trc("Button", I.trc("Caption", "Feladataim")), VaadinIcons.TASKS, NavConst.TASK_LIST);
		navigator.addView(NavConst.TASK_LIST, TaskListView.class);

		this.sideMenu.addNavigation(I.trc("Button", I.trc("Caption", "Munkafolyamatok (futó, archív)")), VaadinIcons.TWIN_COL_SELECT, NavConst.PROCESS_INSTANCE_LIST);
		navigator.addView(NavConst.PROCESS_INSTANCE_LIST, ProcessInstanceListView.class);

		this.sideMenu.addNavigation(I.trc("Button", I.trc("Caption", "Munkafolyamat definíciók")), VaadinIcons.COGS, NavConst.PROCESS_DEF_LIST);
		navigator.addView(NavConst.PROCESS_DEF_LIST, ProcessDefinitionListView.class);

		// this.sideMenu.addNavigation(I.trc("Button", I.trc("Caption", "Munkafolyamat szerkesztése")), VaadinIcons.TWIN_COL_SELECT, NavConst.PROCESS_DEF_EDITOR); // ez menüben nem jelenik meg közvetlenül
		navigator.addView(NavConst.PROCESS_DEF_EDITOR, ProcessDefinitionEditorView.class);

		this.sideMenu.addNavigation(I.trc("Button", I.trc("Caption", "Felhasználók")), VaadinIcons.USER_CARD, NavConst.USER_LIST);
		navigator.addView(NavConst.USER_LIST, UserView.class);

		this.sideMenu.createMenuDivider(NavConst.USER_LIST, "", true);

		this.sideMenu.addNavigation(I.trc("Caption", "Dokumentáció, változáslista"), VaadinIcons.NEWSPAPER, "changelog");
		navigator.addView("changelog", new ChangeLogView());

		// ---

		this.audio = new Audio();
		this.audio.addSource(new ExternalResource("assets/light-beep.mp3"));
		this.audio.setAutoplay(false);
		this.audio.addStyleName("hide-with-visibility");
		this.audio.setWidth("1px");
		this.audio.setHeight("1px");

		this.sideMenu.getMenuItemsLayout().addComponent(this.audio);

		// ---

		VaadinServletResponse.getCurrent().addHeader("X-Frame-Options", "deny"); // TODO: SecurityConfig-ban zavaros, ezért itt letiltva

	}

}
