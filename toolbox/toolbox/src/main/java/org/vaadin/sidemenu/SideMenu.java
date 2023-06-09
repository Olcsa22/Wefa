/**
 * kisebb kiegészítésekkel szinte változatlanul behozva Toolbox-ba, hibás volt a JAR, de kód jó
 */
package org.vaadin.sidemenu;

import com.vaadin.data.TreeData;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Resource;
import com.vaadin.server.Responsive;
import com.vaadin.server.SerializableConsumer;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * A helper component to make it easy to create menus like the one in the
 * quicktickets demo. The SideMenu should be the content of the UI, and all
 * other components should be handled through it. The UI should be annotated
 * with {@code @Viewport("user-scalable=no,initial-scale=1.0")} to provide
 * responsiveness on mobile devices.
 * <p>
 * This component has modification to allow it to be used easily with
 * {@link Navigator}. Pass it as a parameter to the constructor like
 * {@code new Navigator(myUI, sideMenu)}.
 *
 * @author Teemu Suo-Anttila
 */
@SuppressWarnings("serial")
public class SideMenu extends HorizontalLayout {

	/**
	 * A simple lambda compatible handler class for executing code when a menu
	 * entry is clicked.
	 */
	public interface MenuClickHandler extends Serializable {

		/**
		 * This method is called when associated menu entry is clicked.
		 */
		void click();
	}

	/**
	 * Interface to provide operations to existing menu items.
	 *
	 * @since 2.0
	 */
	public interface MenuRegistration extends Serializable {

		/**
		 * Select the menu object associated with this registration. If it's a tree menu item, it'll also be expanded w/ children items
		 */
		void select();

		/**
		 * Removes the menu object associated with this registration.
		 */
		void remove();
	}

	private final class MenuRegistrationImpl<T> implements MenuRegistration {

		private SerializableConsumer<T> selectMethod;
		private SerializableConsumer<T> removeMethod;
		private T menuItem;

		public MenuRegistrationImpl(T menuItem, SerializableConsumer<T> selectMethod,
				SerializableConsumer<T> removeMethod) {
			this.menuItem = menuItem;
			this.selectMethod = selectMethod;
			this.removeMethod = removeMethod;
		}

		@Override
		public void select() {
			selectMethod.accept(menuItem);
		}

		@Override
		public void remove() {
			removeMethod.accept(menuItem);
		}
	}

	/* Class name for hiding the menu when screen is too small */
	private static final String STYLE_VISIBLE = "valo-menu-visible";

	/* Components to handle content and menus */
	private final VerticalLayout contentArea = new VerticalLayout();
	private final CssLayout menuArea = new CssLayout();
	private final CssLayout menuItemsLayout = new CssLayout();

	/* First is the url, second is the menu name */
	private final LinkedHashMap<String, String> menuItems = new LinkedHashMap<>();
	private final MenuBar userMenu = new MenuBar();

	private final Tree<String> treeMenu = new Tree<>();
	private final TreeData<String> treeMenuData = new TreeData<>();
	private final Map<String, MenuClickHandler> treeMenuItemToClick = new HashMap<>();
	private final Map<String, MenuRegistration> treeMenuItemToRegistration = new HashMap<>();

	/* Quick access to user drop down menu */
	private MenuItem userItem;

	/* Caption component for the whole menu */
	private HorizontalLayout logoWrapper;
	private Image menuImage;

	public Navigator navigator;

	/**
	 * Constructor for creating a SideMenu component. This method sets up all 
	 * the components and styles needed for the side menu.
	 */
	public SideMenu() {
		super();
		setSpacing(false);
		addStyleName(ValoTheme.UI_WITH_MENU);
		Responsive.makeResponsive(this);
		setSizeFull();

		menuArea.setPrimaryStyleName("valo-menu");
		menuArea.addStyleName("sidebar");
		menuArea.addStyleName(ValoTheme.MENU_PART);
		menuArea.addStyleName("no-vertical-drag-hints");
		menuArea.addStyleName("no-horizontal-drag-hints");
		menuArea.setWidth(null);
		menuArea.setHeight("100%");

		logoWrapper = new HorizontalLayout();
		logoWrapper.addStyleName("valo-menu-title");
		menuArea.addComponent(logoWrapper);

		userMenu.addStyleName("user-menu");
		userItem = userMenu.addItem("", null);

		menuArea.addComponent(userMenu);

		Button valoMenuToggleButton = new Button("Menu", event -> {
			if (menuArea.getStyleName().contains(STYLE_VISIBLE)) {
				menuArea.removeStyleName(STYLE_VISIBLE);
			} else {
				menuArea.addStyleName(STYLE_VISIBLE);
			}
		});
		valoMenuToggleButton.setIcon(VaadinIcons.LIST);
		valoMenuToggleButton.addStyleName("valo-menu-toggle");
		valoMenuToggleButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		valoMenuToggleButton.addStyleName(ValoTheme.BUTTON_SMALL);
		menuArea.addComponent(valoMenuToggleButton);

		menuItemsLayout.addStyleName("valo-menuitems");

		treeMenu.setTreeData(treeMenuData);
		treeMenu.asSingleSelect().addValueChangeListener(event -> {
			if (!event.isUserOriginated()) {
				return;
			}
			if (null == event.getValue()) {
				// Workaround to disable deselect
				treeMenu.select(event.getOldValue());
			} else {
				Optional.ofNullable(treeMenuItemToClick.get(event.getValue())).ifPresent(MenuClickHandler::click);
			}
		});
		menuArea.addComponent(menuItemsLayout);

		contentArea.setPrimaryStyleName("valo-content");
		contentArea.addStyleName("v-scrollable");
		contentArea.setSizeFull();

		// Remove default margins and spacings
		contentArea.setMargin(false);
		contentArea.setSpacing(false);

		super.addComponent(menuArea);
		super.addComponent(contentArea);
		setExpandRatio(contentArea, 1);
	}

	/**
	 * ha ezt a metódust ráhívod az adott projekt UI-ba, akkor kifognak jelölődni a kiválasztott menüpontok
	 * és lehetőséget ad a #createMenuDivider és #changeNotificationForMenuItem metódusra
	 *
	 * @param navigator
	 */
	public void setNavigator(Navigator navigator) {

		this.navigator = navigator;

		this.navigator.addViewChangeListener(new ViewChangeListener() {

			@Override
			public boolean beforeViewChange(ViewChangeListener.ViewChangeEvent event) {
				return true;
			}

			@Override
			public void afterViewChange(ViewChangeEvent event) {

				for (Iterator<Component> it = menuItemsLayout.iterator(); it
						.hasNext();) {
					it.next().removeStyleName("selected");
				}

				for (Map.Entry<String, String> item : menuItems.entrySet()) {
					if (event.getViewName().equals(item.getKey())) {

						for (Iterator<Component> it = menuItemsLayout
								.iterator(); it.hasNext();) {

							Component c = it.next();
							if (c.getCaption() != null
									&& c.getCaption().startsWith(
											item.getValue())) {
								c.addStyleName("selected");
								break;
							}

						}
						break;
					}
				}
				menuArea.removeStyleName("valo-menu-visible");
			}
		});

	}

	public void createMenuDivider(final String navigationState, final String dividerName) {
		createMenuDivider(navigationState, dividerName, false);
	}

	/**
	 * menüpontok elválasztására szolgál
	 *
	 * @param navigationState
	 * 		az url ami fölé kell tenni a dividert
	 * @param dividerName
	 * 		ez lesz a divider caption-je, ami a menüpont fölött fog látszani
	 * @param isBelow
	 * 		ez dönti el, hogy a menü fölé vagy alá kerüljön az elválasztó
	 */
	public void createMenuDivider(final String navigationState, final String dividerName, final boolean isBelow) {
		for (final Map.Entry<String, String> item : menuItems.entrySet()) {

			if (item.getKey().equals(navigationState)) {
				Label label = new Label(dividerName, ContentMode.HTML);
				label.setValue(label.getValue() + " <span class=\"valo-menu-badge\"></span>");
				label.setPrimaryStyleName(ValoTheme.MENU_SUBTITLE);
				label.addStyleName(ValoTheme.LABEL_H4);
				label.setSizeUndefined();

				for (Component component : menuItemsLayout) {
					if (component.getCaption() != null && component.getCaption().equalsIgnoreCase(item.getValue())) {
						int componentIndex = menuItemsLayout.getComponentIndex(component);

						if (isBelow) {
							componentIndex++;
						}

						menuItemsLayout.addComponent(label, componentIndex);
						break;
					}
				}

			}
		}
	}

	/**
	 *	a menüpont mellé ilyen notification szerűen megjelenik a beadott üzenet
	 *
	 * @param navigationState
	 * 		ami alapján megkeresi (url)
	 * @param notificationStr
	 * 		amit megjelenítünk a menüpont mellett (figyelj a hosszra)
	 * @param icon
	 * 		a menüpont ikonja
	 */
	public void changeNotificationForMenuItem(final String navigationState, final String notificationStr, final VaadinIcons icon) {

		for (final Map.Entry<String, String> item : menuItems.entrySet()) {

			if (item.getKey().equals(navigationState)) {

				final Button b = new Button(item.getValue(), (Button.ClickListener) event -> navigator.navigateTo(item.getKey()));

				if (StringUtils.isNotBlank(notificationStr)) {
					b.setCaption(b.getCaption() + " <span class=\"valo-menu-badge\">" + notificationStr + "</span>");
				}

				b.setCaptionAsHtml(true);
				b.setPrimaryStyleName(ValoTheme.MENU_ITEM);
				b.setIcon(icon);

				for (Component component : menuItemsLayout) {
					if (component.getCaption() != null && component.getCaption().equalsIgnoreCase(item.getValue())) {
						menuItemsLayout.replaceComponent(component, b);
						break;
					}
				}
			}
		}
	}

	/**
	 * Adds a menu entry. The given handler is called when the user clicks the
	 * entry.
	 *
	 * @param text
	 *            menu text
	 * @param handler
	 *            menu click handler
	 *
	 * @return menu registration
	 */
	public MenuRegistration addMenuItem(String text, MenuClickHandler handler) {
		return addMenuItem(text, null, handler);
	}

	/**
	 * Adds a menu entry with given icon. The given handler is called when the
	 * user clicks the entry.
	 *
	 * @param text
	 *            menu text
	 * @param icon
	 *            menu icon
	 * @param handler
	 *            menu click handler
	 *
	 * @return menu registration
	 */
	public MenuRegistration addMenuItem(String text, Resource icon, final MenuClickHandler handler) {
		Button button = new Button(text, event -> {
			handler.click();
			menuArea.removeStyleName(STYLE_VISIBLE);
		});
		button.setIcon(icon);
		button.setPrimaryStyleName("valo-menu-item");
		menuItemsLayout.addComponent(button);
		return new MenuRegistrationImpl<>(button, Button::click, menuItemsLayout::removeComponent);
	}

	/**
	 * custom Lanoga
	 * 
	 * @return
	 */
	public CssLayout getMenuItemsLayout() {
		return menuItemsLayout;
	}

	public LinkedHashMap<String, String> getMenuItems() {
		return menuItems;
	}

	public CssLayout getMenuArea() {
		return menuArea;
	}

	/**
	 * Add a root tree item to the menu. If it already exists, nothing happens and the existing {@link MenuRegistration} is returned
	 *
	 * @param rootItem caption of the root item, must be unique among all items incl. root/sub items
	 * @param clickHandler triggered if the specified item is selected
	 * @return MenuRegistration of the added item
	 */
	public MenuRegistration addTreeItem(String rootItem, MenuClickHandler clickHandler) {
		ensureTreeAdded();
		MenuRegistration existingRegistration = treeMenuItemToRegistration.get(rootItem);
		if (null != existingRegistration) {
			return existingRegistration;
		}
		treeMenuData.addRootItems(rootItem);
		return registerTreeMenuItem(rootItem, clickHandler);
	}

	/**
	 * Add a sub tree item to the menu. If it already exists, nothing happens and the existing {@link MenuRegistration} is returned
	 *
	 * @param parent caption of the parent item of the item to be added
	 * @param item caption of the sub item, must be unique among all items incl. root/sub items
	 * @param clickHandler triggered if the specified item is selected
	 * @return MenuRegistration of the added item
	 */
	public MenuRegistration addTreeItem(String parent, String item, MenuClickHandler clickHandler) {
		ensureTreeAdded();
		MenuRegistration existingRegistration = treeMenuItemToRegistration.get(item);
		if (null != existingRegistration) {
			return existingRegistration;
		}
		treeMenuData.addItem(parent, item);
		return registerTreeMenuItem(item, clickHandler);
	}

	private MenuRegistration registerTreeMenuItem(String treeItem, MenuClickHandler clickHandler) {
		treeMenu.getDataProvider().refreshAll();
		treeMenuItemToClick.put(treeItem, clickHandler);
		MenuRegistration registration = new MenuRegistrationImpl<>(treeItem, item -> {
			// Tree "Bug": all parents must be explicitly expanded
			for (String parent = item; null != parent; parent = treeMenuData.getParent(parent)) {
				treeMenu.expand(parent);
			}
			treeMenu.select(item);
		}, remove -> {
			removeRegistration(remove);
			treeMenuData.removeItem(remove);
			treeMenu.getDataProvider().refreshAll();
		});
		treeMenuItemToRegistration.put(treeItem, registration);
		return registration;
	}

	private void removeRegistration(String remove) {
		treeMenuItemToRegistration.remove(remove);
		treeMenuItemToClick.remove(remove);
		treeMenuData.getChildren(remove).stream().filter(Objects::nonNull).forEach(this::removeRegistration);
	}

	private void ensureTreeAdded() {
		if (menuItemsLayout.getComponentIndex(treeMenu) == -1) {
			menuItemsLayout.addComponent(treeMenu);
		}
	}

	/**
	 * Adds a menu entry to the user drop down menu. The given handler is called when the user clicks the entry.
	 *
	 * @param text menu text
	 * @param handler menu click handler
	 * @return menu registration
	 */
	public MenuRegistration addUserMenuItem(String text, MenuClickHandler handler) {
		return addUserMenuItem(text, null, handler);
	}

	/**
	 * Adds a menu entry to the user drop down menu with given icon. The given
	 * handler is called when the user clicks the entry.
	 *
	 * @param text
	 *            menu text
	 * @param icon
	 *            menu icon
	 * @param handler
	 *            menu click handler
	 *
	 * @return menu registration
	 */
	public MenuRegistration addUserMenuItem(String text, Resource icon, final MenuClickHandler handler) {
		Command menuCommand = selectedItem -> handler.click();
		MenuItem menuItem = userItem.addItem(text, icon, menuCommand);
		return new MenuRegistrationImpl<>(menuItem, menuCommand::menuSelected, userItem::removeChild);
	}

	/**
	 * Sets the user name to be displayed in the menu.
	 *
	 * @param userName
	 *            user name
	 */
	public void setUserName(String userName) {
		userItem.setText(userName);
	}

	/**
	 * Sets the portrait of the user to be displayed in the menu.
	 *
	 * @param icon
	 *            portrait of the user
	 */
	public void setUserIcon(Resource icon) {
		userItem.setIcon(icon);
	}

	/**
	 * Sets the visibility of the whole user menu. This includes portrait, user
	 * name and the drop down menu.
	 *
	 * @param visible
	 *            user menu visibility
	 */
	public void setUserMenuVisible(boolean visible) {
		userMenu.setVisible(visible);
	}

	/**
	 * Gets the visibility of the user menu.
	 *
	 * @return {@code true} if visible; {@code false} if hidden
	 */
	public boolean isUserMenuVisible() {
		return userMenu.isVisible();
	}

	/**
	 * Sets the title text for the menu
	 *
	 * @param caption
	 *            menu title
	 */
	public void setMenuCaption(String caption) {
		setMenuCaption(caption, null);
	}

	/**
	 * Sets the title caption and logo for the menu
	 *
	 * @param caption
	 *            menu caption
	 * @param logo
	 *            menu logo
	 */
	public void setMenuCaption(String caption, Resource logo) {
		if (menuImage != null) {
			logoWrapper.removeComponent(menuImage);
		}
		menuImage = new Image(caption, logo);
		menuImage.setWidth("100%");
		logoWrapper.addComponent(menuImage);
	}

	/**
	 * Removes all content from the user drop down menu.
	 */
	public void clearUserMenu() {
		userItem.removeChildren();
	}

	/**
	 * Removes all content from the navigation menu.
	 */
	public void clearMenu() {
		menuItemsLayout.removeAllComponents();
		treeMenuData.clear();
		treeMenuItemToClick.clear();
		treeMenuItemToRegistration.clear();
	}

	/**
	 * Adds a menu entry to navigate to given navigation state.
	 *
	 * @param text
	 *            text to display in menu
	 * @param navigationState
	 *            state to navigate to
	 *
	 * @return menu registration
	 */
	public MenuRegistration addNavigation(String text, String navigationState) {
		return addNavigation(text, null, navigationState);
	}

	/**
	 * Adds a menu entry with given icon to navigate to given navigation state.
	 *
	 * @param text
	 *            text to display in menu
	 * @param icon
	 *            icon to display in menu
	 * @param navigationState
	 *            state to navigate to
	 *
	 * @return menu registration
	 */
	public MenuRegistration addNavigation(String text, Resource icon, final String navigationState) {
		menuItems.put(navigationState, text);
		return addMenuItem(text, icon, () -> getUI().getNavigator().navigateTo(navigationState));
	}

	/**
	 * Removes all components from the content area.
	 */
	@Override
	public void removeAllComponents() {
		contentArea.removeAllComponents();
	}

	/**
	 * Adds a component to the content area.
	 */
	@Override
	public void addComponent(Component c) {
		contentArea.addComponent(c);
	}

	/**
	 * Removes all content from the content area and replaces everything with
	 * given component.
	 *
	 * @param content
	 *            new content to display
	 */
	public void setContent(Component content) {
		contentArea.removeAllComponents();
		contentArea.addComponent(content);
	}

	public HorizontalLayout getLogoWrapper() {
		return logoWrapper;
	}

	public void setLogoWrapper(HorizontalLayout logoWrapper) {
		this.logoWrapper = logoWrapper;
	}

}