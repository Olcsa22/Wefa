package hu.lanoga.toolbox.vaadin.component;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;

import com.google.common.base.Functions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.teamunify.i18n.I;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.Page;
import com.vaadin.server.Page.BrowserWindowResizeEvent;
import com.vaadin.server.Page.BrowserWindowResizeListener;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.chat.internal.ChatEntry;
import hu.lanoga.toolbox.chat.internal.ChatEntryService;
import hu.lanoga.toolbox.codestore.CodeStoreItem;
import hu.lanoga.toolbox.codestore.CodeStoreItemJdbcRepository;
import hu.lanoga.toolbox.exception.ManualValidationException;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.user.User;
import hu.lanoga.toolbox.user.UserJdbcRepository;
import hu.lanoga.toolbox.user.UserService;
import hu.lanoga.toolbox.vaadin.AbstractToolboxUI;
import hu.lanoga.toolbox.vaadin.component.file.FileManagerComponentBuilder;
import hu.lanoga.toolbox.vaadin.component.file.FileManagerField;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InternalChatComponent extends HorizontalLayout implements BrowserWindowResizeListener {

	public static final Cache<String, Integer> recentUpdatesRoleBasedChats = CacheBuilder.newBuilder().concurrencyLevel(4).expireAfterWrite(120, TimeUnit.SECONDS).build(); // nem cache szó szerint, a timeout és a jó/beépített concurrency kezelés miatt hasznájuk (ConccurentHashMap stb. helyett)
	public static final Cache<String, Integer> recentUpdatesUserIdBasedChats = CacheBuilder.newBuilder().concurrencyLevel(4).expireAfterWrite(120, TimeUnit.SECONDS).build(); // nem cache szó szerint, a timeout és a jó/beépített concurrency kezelés miatt hasznájuk (ConccurentHashMap stb. helyett)

	private final Set<Integer> allowedUserRoleBasedChatGroups;

	private int autoHeightSubtract;

	private Panel panel;

	private VerticalLayout vlPanelInner;

	private volatile Integer msgIdAnchorMin;
	private volatile Integer prevMsgIdAnchorMin;
	private volatile Integer msgIdAnchorMax;

	private volatile Integer currentTargetType;
	private volatile Object currentTargetValue;

	private final ToolboxUserDetails loggedInUser;
	private final int loggedInUserId; // azért kell, mert egyszerű, immmutable (szemben a ToolboxUserDetails-szel), háttérszál miatt jobb
	private final int loggedInUserTenantId; // azért kell, mert egyszerű, immmutable (szemben a ToolboxUserDetails-szel), háttérszál miatt jobb

	private final UI ui;
	private volatile Integer lastSentSavedId;

	private volatile ImmutableSortedMap<Integer, String> lsUsersMapSorted;
	private ListSelect<Integer> lsUsers;

	private final AtomicLong lastLsUsersRefresh = new AtomicLong(0);
	private final AtomicBoolean lastLsUsersRefreshSetWasEmpty = new AtomicBoolean(true);

	private boolean showRoleGroupChats;

	public InternalChatComponent(final Set<Integer> allowedUserRoleBasedChatGroups, boolean showRoleGroupChats) {

		this.allowedUserRoleBasedChatGroups = allowedUserRoleBasedChatGroups;
		this.autoHeightSubtract = 200;

		this.showRoleGroupChats = showRoleGroupChats;

		// ---

		this.loggedInUser = SecurityUtil.getLoggedInUser();

		this.loggedInUserId = this.loggedInUser.getId();
		this.loggedInUserTenantId = this.loggedInUser.getTenantId();

		this.ui = UI.getCurrent();

		if (this.ui.getPushConfiguration().getPushMode().equals(PushMode.DISABLED)) {
			throw new ToolboxGeneralException("Need enabled PUSH!");
		}

	}

	public void initLayout() {

		this.removeAllComponents();

		// ---

		SecurityUtil.limitAccessRoleUser();

		// ---

		this.initLeftSide();

		// ---

		final Label lblSpacer = new Label("");
		this.addComponent(lblSpacer); // TMP/spacer right comp. (amíg nem válasz semmit)
		this.setExpandRatio(lblSpacer, 4f);

		// ---

		this.scheduleBgCheckForNewMessages();

	}

	private void initLeftSide() {

		final VerticalLayout vlLeft = new VerticalLayout();
		vlLeft.setMargin(false);

		final ListSelect<String> lsRoleGroups = new ListSelect<>(I.trc("Caption", "User role based groups"));
		this.lsUsers = new ListSelect<>(I.trc("Caption", "Users (private messages to users)"));

		if (this.showRoleGroupChats) {

			final CodeStoreItemJdbcRepository codeStoreItemJdbcRepository = ApplicationContextHelper.getBean(CodeStoreItemJdbcRepository.class);

			final Map<String, String> lsRoleGroupsMap = new LinkedHashMap<>();

			{

				final JSONArray jsonArray = new JSONArray(this.loggedInUser.getUserRoles());

				for (int k = 0; k < jsonArray.length(); ++k) {

					final int x = jsonArray.getInt(k);

					if (!this.allowedUserRoleBasedChatGroups.contains(x)) {
						continue;
					}

					final CodeStoreItem csi = codeStoreItemJdbcRepository.findOne(x);
					lsRoleGroupsMap.put(csi.getCommand(), csi.getCaptionCaption());
				}

			}

			final Map<String, String> lsRoleGroupsMapSorted = ImmutableSortedMap.copyOf(lsRoleGroupsMap, Ordering.natural().onResultOf(Functions.forMap(lsRoleGroupsMap)));

			lsRoleGroups.setItems(lsRoleGroupsMapSorted.keySet());
			lsRoleGroups.setItemCaptionGenerator(item -> lsRoleGroupsMapSorted.get(item));
			lsRoleGroups.setWidth("100%");
			vlLeft.addComponent(lsRoleGroups);

			lsRoleGroups.addSelectionListener(v -> {

				if (!v.isUserOriginated()) {
					return;
				}

				this.lsUsers.deselectAll();
				this.initRightSide(ToolboxSysKeys.ChatTargetType.AUTH_ROLE, v.getValue().iterator().next(), lsRoleGroups.getItemCaptionGenerator().apply(v.getValue().iterator().next()));
				this.panel.setScrollTop(Integer.MAX_VALUE);
			});

		}

		// ---

		{

			final Map<Integer, String> lsUsersMap = new LinkedHashMap<>();

			final List<User> regularUsers = ApplicationContextHelper.getBean(UserService.class).findAllUserByRole(ToolboxSysKeys.UserAuth.ROLE_USER_CS_ID); // minden nem system, nem LCU user (adminok is)

			for (final User user : regularUsers) {
				if (user.getId().equals(this.loggedInUserId)) {
					continue;
				}
				lsUsersMap.put(user.getId(), I18nUtil.buildFullName(user, true) + " (" + user.getUsername() + ")");
			}

			this.lsUsersMapSorted = ImmutableSortedMap.copyOf(lsUsersMap, Ordering.natural().onResultOf(Functions.forMap(lsUsersMap)));

			this.lsUsers.setWidth("100%");
			vlLeft.addComponent(this.lsUsers);

			this.lsUsers.addSelectionListener(v -> {

				if (!v.isUserOriginated()) {
					return;
				}

				if (this.showRoleGroupChats) {
					lsRoleGroups.deselectAll();
				}

				this.initRightSide(ToolboxSysKeys.ChatTargetType.AUTH_USER, v.getValue().iterator().next(), this.lsUsers.getItemCaptionGenerator().apply(v.getValue().iterator().next()));
				this.panel.setScrollTop(Integer.MAX_VALUE);
			});

			this.refreshFillLsUsers();

		}

		// ---

		vlLeft.setHeight(null);
		vlLeft.setWidth("100%");
		this.addComponent(vlLeft);
		this.setExpandRatio(vlLeft, 1f);

	}

	private void initRightSide(final int targetType, final Object targetValue, final String captionStr) {

		this.currentTargetType = targetType;
		this.currentTargetValue = targetValue;

		this.msgIdAnchorMin = null;
		this.prevMsgIdAnchorMin = null;

		this.removeComponent(this.getComponent(1));

		final VerticalLayout vlRight = new VerticalLayout();
		vlRight.setMargin(false);
		vlRight.setWidth("100%");
		this.addComponent(vlRight);
		this.setExpandRatio(vlRight, 4f);

		this.panel = new Panel(I.trc("Caption", "Chat messages") + ": " + StringUtils.removeStart(captionStr, "(!) "));
		this.panel.setWidth("100%");
		this.panel.setHeight((Page.getCurrent().getBrowserWindowHeight() - this.autoHeightSubtract) + "px");

		vlRight.addComponent(this.panel);

		Page.getCurrent().addBrowserWindowResizeListener(this);

		this.vlPanelInner = new VerticalLayout();
		this.vlPanelInner.setWidth("100%");
		this.vlPanelInner.setHeight(null);
		this.panel.setContent(this.vlPanelInner);

		this.updateVlPanelInner(targetType, targetValue, false);

		// ---

		{

			final HorizontalLayout hl = new HorizontalLayout();
			hl.setWidth("100%");
			vlRight.addComponent(hl);

			final TextArea ta = new TextArea(I.trc("Caption", "New message"), "");
			ta.setWidth("100%");
			ta.setHeight("80px");
			hl.addComponent(ta);
			hl.setExpandRatio(ta, 1f);

			final FileManagerField fmf = new FileManagerField(I.trc("Caption", "Attached files"), new FileManagerComponentBuilder()
					.setFileDescriptorSecurityType(ToolboxSysKeys.FileDescriptorSecurityType.READ_AUTHENTICATED_USER_MODIFY_ADMIN_OR_CREATOR)
					.setMaxFileCount(20));
			fmf.setWidth("150px");
			fmf.setBtnSize("80px");
			hl.addComponentAsFirst(fmf);

			final Button btnSend = new Button(I.trc("Button", "Send (CTRL+ENTER)"));
			btnSend.setStyleName(ValoTheme.BUTTON_PRIMARY);
			btnSend.setWidth("200px");
			btnSend.setHeight("80px");
			btnSend.setDisableOnClick(true);
			hl.addComponent(btnSend);
			hl.setComponentAlignment(btnSend, Alignment.BOTTOM_CENTER);

			btnSend.addClickListener(x -> {

				this.sendMsg(targetType, targetValue, ta, fmf, btnSend);

			});

			btnSend.setClickShortcut(ShortcutAction.KeyCode.ENTER, ShortcutAction.ModifierKey.CTRL);

		}

	}

	private void sendMsg(final int targetType, final Object targetValue, final TextArea ta, final FileManagerField fmf, final Button btnSend) {

		try {

			if (StringUtils.isBlank(ta.getValue())) {
				throw new ManualValidationException("MessageText is blank!", I.trc("Error", "The message text cannot be blank!"));
			}

			final ChatEntry newEntry = new ChatEntry();

			newEntry.setTargetType(targetType);
			newEntry.setTargetValue(targetValue.toString());

			newEntry.setMessageText(ta.getValue().trim());

			if (StringUtils.isNotBlank(fmf.getValue())) {
				newEntry.setFileIds(fmf.getValue());
			}

			if (targetType == ToolboxSysKeys.ChatTargetType.AUTH_USER) {
				newEntry.setSeen(Boolean.FALSE);
			}

			final ChatEntryService chatEntryService = ApplicationContextHelper.getBean(ChatEntryService.class);
			final ChatEntry newEntryAfterSave = chatEntryService.save(newEntry);

			fmf.setValue(null);
			fmf.refreshButtonCounter();
			ta.clear();
			ta.focus();

			this.updateVlPanelInner(targetType, targetValue, false);

			this.panel.setScrollTop(Integer.MAX_VALUE);

			this.lastSentSavedId = newEntryAfterSave.getId();

			if (targetType == ToolboxSysKeys.ChatTargetType.AUTH_USER) {
				recentUpdatesUserIdBasedChats.put(this.loggedInUserId + "-" + targetValue, newEntryAfterSave.getId());
			} else if (targetType == ToolboxSysKeys.ChatTargetType.AUTH_ROLE) {
				recentUpdatesRoleBasedChats.put((String) targetValue, newEntryAfterSave.getId());
			}

		} finally {
			btnSend.setEnabled(true);
		}

	}

	private void updateVlPanelInner(final int targetType, final Object targetValue, final boolean goUp) {

		final ChatEntryService chatEntryService = ApplicationContextHelper.getBean(ChatEntryService.class);

		final List<ChatEntry> entryList;

		Integer msgIdAnchor = null;

		if (this.msgIdAnchorMin != null && this.msgIdAnchorMax != null) {

			this.prevMsgIdAnchorMin = this.msgIdAnchorMin;
			msgIdAnchor = goUp ? this.msgIdAnchorMin : this.msgIdAnchorMax;

		}

		if (ToolboxSysKeys.ChatTargetType.AUTH_USER == targetType) {
			entryList = chatEntryService.enhance(chatEntryService.findAllUserTargetType((Integer) targetValue, msgIdAnchor, goUp));
		} else if (ToolboxSysKeys.ChatTargetType.AUTH_ROLE == targetType) {
			entryList = chatEntryService.enhance(chatEntryService.findAllRoleTargetType((String) targetValue, msgIdAnchor, goUp));
		} else {
			throw new IllegalArgumentException();
		}

		Collections.reverse(entryList);

		final TreeMap<Integer, Component> msgComps = new TreeMap<>();

		final Iterator<Component> it = this.vlPanelInner.iterator();
		while (it.hasNext()) {
			final Component component = it.next();
			if (component instanceof Button) {
				continue;
			}
			msgComps.put(Integer.parseInt(component.getId().substring(3)), component);
		}

		for (final ChatEntry e : entryList) {
			msgComps.put(e.getId(), this.buildChatMassageElement(e));
		}

		this.vlPanelInner.removeAllComponents();

		boolean isFirstLoop = true;

		for (final Entry<Integer, Component> e : msgComps.entrySet()) {

			if (isFirstLoop) {
				this.msgIdAnchorMin = e.getKey();
				isFirstLoop = false;
			}

			this.msgIdAnchorMax = e.getKey();
			this.vlPanelInner.addComponent(e.getValue());

		}

		if (!goUp || (goUp && entryList.size() >= 10)) {

			final Button btnLoadOlder = new Button(I.trc("Button", "Load older messages"));
			btnLoadOlder.setStyleName(ValoTheme.BUTTON_SMALL);
			btnLoadOlder.setWidth("200px");
			btnLoadOlder.setDisableOnClick(true);

			this.vlPanelInner.addComponentAsFirst(btnLoadOlder);
			// this.vlPanelInner.setComponentAlignment(btnLoadOlder, Alignment.MIDDLE_CENTER);

			btnLoadOlder.addClickListener(x -> {

				this.vlPanelInner.removeComponent(btnLoadOlder);
				this.updateVlPanelInner(targetType, targetValue, true);

				if (Objects.equals(this.msgIdAnchorMin, this.prevMsgIdAnchorMin)) {
					Notification.show(I.trc("Button", "There are no more older messages!"));
				}

			});

		}

	}

	private Component buildChatMassageElement(final ChatEntry e) {

		final HorizontalLayout hl = new HorizontalLayout();
		hl.setId("cm-" + e.getId());

		final String caption = I.trc("Caption", "SysID") + ": " + e.getId() + ", " + e.getCreatedOn() + ", " + e.getCreatedByCaption();

		// if (Boolean.FALSE.equals(e.getSeen()) && (loggedInUserId != e.getCreatedBy().intValue())) {
		// caption += " (new)";
		// }

		final TextArea ta = new TextArea(caption, e.getMessageText());
		ta.setWidth("100%");
		ta.setHeight("80px");
		ta.setReadOnly(true);

		hl.setWidth("100%");
		hl.addComponent(ta);
		hl.setExpandRatio(ta, 1f);

		if (StringUtils.isNotBlank(e.getFileIds())) {

			final FileManagerField fmf = new FileManagerField("", new FileManagerComponentBuilder()
					.setFileDescriptorSecurityType(ToolboxSysKeys.FileDescriptorSecurityType.READ_AUTHENTICATED_USER_MODIFY_ADMIN_OR_CREATOR)
					.setMaxFileCount(20));
			fmf.setWidth("185px");
			fmf.setValue(e.getFileIds());
			fmf.setEnabled(false);
			fmf.setBtnSize("80px");

			hl.addComponent(fmf);

		}

		return hl;

	}

	@Override
	public void browserWindowResized(final BrowserWindowResizeEvent event) {
		this.panel.setHeight((Page.getCurrent().getBrowserWindowHeight() - this.autoHeightSubtract) + "px");
	}

	public int getAutoHeightSubtract() {
		return this.autoHeightSubtract;
	}

	public void setAutoHeightSubtract(final int autoHeightSubtract) {
		this.autoHeightSubtract = autoHeightSubtract;
	}

	private void scheduleBgCheckForNewMessages() {

		this.ui.access(() -> {

			if (!this.isAttached()) {
				log.debug("bg thread, not attached (1)");
				return;
			}

			AbstractToolboxUI.scheduleRunnable(() -> {

				// background thread

				this.refreshFillLsUsersFromBgThread();

				// ---

				// log.debug("bg thread, loggedInUserId: " + this.loggedInUserId);

				final Integer c = this.currentTargetType; // currentTargetType volatile, változhat közben is
				final Object t = this.currentTargetValue; // currentTargetValue volatile, változhat közben is

				if (c != null && t != null) {

					boolean needsUpdate = false;

					Integer i = null;

					if (ToolboxSysKeys.ChatTargetType.AUTH_USER == c.intValue()) {
						i = recentUpdatesUserIdBasedChats.getIfPresent(t + "-" + this.loggedInUserId);
					} else if (ToolboxSysKeys.ChatTargetType.AUTH_ROLE == c.intValue()) {
						i = recentUpdatesRoleBasedChats.getIfPresent(t);
					}

					if (i != null && i.equals(this.lastSentSavedId)) {
						// log.debug("bg thread, updateVlPanelInner skipped (current user made the new message, already shown in the list)");
						this.scheduleBgCheckForNewMessages();
						return null;
					}

					if (i != null && this.msgIdAnchorMax != null && this.msgIdAnchorMax >= i) {
						// log.debug("bg thread, updateVlPanelInner skipped (msgIdAnchorMax is higher, already shown in the list)");
						this.scheduleBgCheckForNewMessages();
						return null;
					}

					needsUpdate = (i != null && !i.equals(this.lastSentSavedId));

					if (needsUpdate) {

						log.debug("bg thread, updateVlPanelInner is needed");

						try {

							JdbcRepositoryManager.setTlTenantId(this.loggedInUserTenantId);

							this.ui.access(() -> {

								if (!this.isAttached()) {
									log.debug("bg thread, not attached (2)");
									return;
								}

								SecurityUtil.setUser(ApplicationContextHelper.getBean(UserJdbcRepository.class).findOne(this.loggedInUserId)); // azért UserJdbcRepository és nem servie, mert itt nincs belépve senki

								this.updateVlPanelInner(c, t, false);

								this.panel.setScrollTop(Integer.MAX_VALUE); // TODO: kérdéses, csak akkor kellene, ha épp nem tekert fel

								this.ui.push();

								log.debug("bg thread, updateVlPanelInner + push... loggedInUserId: " + this.loggedInUserId + ", currentTargetType: " + this.currentTargetType + ", currentTargetValue: " + this.currentTargetValue);

								this.scheduleBgCheckForNewMessages();

							});

						} finally {
							SecurityUtil.clearAuthentication();
							JdbcRepositoryManager.clearTlTenantId();
						}

					} else {
						// log.debug("bg thread, updateVlPanelInner is not needed, loggedInUserId: " + loggedInUserId + ", lastSentSavedId: " + lastSentSavedId + ", i: " + i);
						this.scheduleBgCheckForNewMessages();
					}

				} else {

					// log.debug("bg thread, skip (currentTargetType/currentTargetValue is null)");
					this.scheduleBgCheckForNewMessages();
				}

				return null;

			}, 1, TimeUnit.SECONDS);

		});
	}

	private void refreshFillLsUsersFromBgThread() {

		final long lastRefresh = this.lastLsUsersRefresh.get();

		final long currentTimeMillis = System.currentTimeMillis();

		if (currentTimeMillis - lastRefresh < 10000) {
			return;
		}

		this.lastLsUsersRefresh.set(currentTimeMillis);

		Set<Integer> notYetSeenSet;

		try {

			JdbcRepositoryManager.setTlTenantId(this.loggedInUserTenantId);

			SecurityUtil.setUser(ApplicationContextHelper.getBean(UserJdbcRepository.class).findOne(this.loggedInUserId)); // azért UserJdbcRepository és nem servie, mert itt nincs belépve senki

			final ChatEntryService chatEntryService = ApplicationContextHelper.getBean(ChatEntryService.class);
			notYetSeenSet = chatEntryService.findNotSeenChatCreatorsUserTargetType();

		} finally {
			SecurityUtil.clearAuthentication();
			JdbcRepositoryManager.clearTlTenantId();
		}

		if (this.lastLsUsersRefreshSetWasEmpty.get() && notYetSeenSet.isEmpty()) {
			this.lastLsUsersRefreshSetWasEmpty.set(true);
			return;
		}

		this.lastLsUsersRefreshSetWasEmpty.set(false);

		this.ui.access(() -> {

			this.lsUsers.clear();

			this.lsUsers.setItems(this.lsUsersMapSorted.keySet());
			this.lsUsers.setItemCaptionGenerator(item -> {
				String str = this.lsUsersMapSorted.get(item);
				if (notYetSeenSet.contains(item)) {
					str = "(!) " + str;
				}
				return str;
			});

		});

	}

	private void refreshFillLsUsers() {

		this.lastLsUsersRefresh.set(System.currentTimeMillis());

		final ChatEntryService chatEntryService = ApplicationContextHelper.getBean(ChatEntryService.class);
		final Set<Integer> notYetSeenSet = chatEntryService.findNotSeenChatCreatorsUserTargetType();

		this.lastLsUsersRefreshSetWasEmpty.set(notYetSeenSet.isEmpty());

		this.lsUsers.clear();

		this.lsUsers.setItems(this.lsUsersMapSorted.keySet());
		this.lsUsers.setItemCaptionGenerator(item -> {
			String str = this.lsUsersMapSorted.get(item);
			if (notYetSeenSet.contains(item)) {
				str = "(!) " + str;
			}
			return str;
		});

	}

}
