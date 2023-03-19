package hu.lanoga.toolbox.vaadin.util;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AtomicLongMap;
import com.teamunify.i18n.I;
import com.vaadin.data.HasValue;
import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.server.UserError;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComboBox.CaptionFilter;
import com.vaadin.ui.Component;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.ItemCaptionGenerator;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.FileOperationAccessTypeIntent;
import hu.lanoga.toolbox.codestore.CodeStoreItem;
import hu.lanoga.toolbox.codestore.CodeStoreItemService;
import hu.lanoga.toolbox.exception.ManualValidationException;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.user.User;
import hu.lanoga.toolbox.user.UserService;
import hu.lanoga.toolbox.util.BrandUtil;
import hu.lanoga.toolbox.util.DateTimeUtil;
import hu.lanoga.toolbox.util.DiagnosticsHelper;
import hu.lanoga.toolbox.util.NumberUtil;
import hu.lanoga.toolbox.util.ToolboxAssert;
import hu.lanoga.toolbox.vaadin.AbstractToolboxUI;
import hu.lanoga.toolbox.vaadin.component.NumberOnlyTextField;
import hu.lanoga.toolbox.vaadin.component.crud.CreateOnlyCrudFormElement;
import hu.lanoga.toolbox.vaadin.component.crud.CrudFormElementCollection;
import hu.lanoga.toolbox.vaadin.component.crud.EditOnlyCrudFormElement;
import hu.lanoga.toolbox.vaadin.component.crud.FormLayoutCrudFormComponent;
import hu.lanoga.toolbox.vaadin.component.crud.NotEditableCrudFormElement;
import hu.lanoga.toolbox.vaadin.component.crud.SecondaryCrudFormElement;
import hu.lanoga.toolbox.vaadin.component.crud.ViewOnlyCrudFormElement;
import lombok.extern.slf4j.Slf4j;

/**
 * Vaadin UI-hoz util metódusok
 */
@Slf4j
public final class UiHelper { // TODO: rename to VaadinHelper, VaadinUtil...

	private UiHelper() {
		//
	}

	/**
	 * @param addGitInfo
	 * @return
	 * 
	 * @deprecated 
	 * 		helyette {@link BrandUtil#getAppTitle(boolean)}
	 */
	@Deprecated
	public static String getAppTitle(final boolean addGitInfo) {
		return BrandUtil.getAppTitle(addGitInfo);
	}

	/**
	 * @param layout
	 * @param crudFormElementCollection
	 * @param enabled
	 * @param isAddView
	 * @see FormLayoutCrudFormComponent
	 */
	public static void addFields(final AbstractLayout layout, final CrudFormElementCollection<?> crudFormElementCollection, final boolean enabled, final boolean isAddView) {
		final List<AbstractLayout> list = new ArrayList<>();
		list.add(layout);
		addFields(list, crudFormElementCollection, enabled, isAddView);
	}

	/**
	 * @param layouts
	 * @param crudFormElementCollection
	 * @param enabled
	 * @param isAddView
	 * @see FormLayoutCrudFormComponent
	 */
	public static void addFields(final List<AbstractLayout> layouts, final CrudFormElementCollection<?> crudFormElementCollection, final boolean enabled, final boolean isAddView) {

		for (final Field field : crudFormElementCollection.getClass().getFields()) {
			try {

				// field.setAccessible(true); // nem kell itt

				final Object object = field.get(crudFormElementCollection);

				if (!(object instanceof Component)) {
					continue;
				}

				if ((field.isAnnotationPresent(EditOnlyCrudFormElement.class) && !enabled)) {
					continue;
				}

				final Component c = (Component) object;
				c.setWidth(100, Unit.PERCENTAGE);
				c.setEnabled(enabled);

				if ((field.isAnnotationPresent(CreateOnlyCrudFormElement.class) || field.isAnnotationPresent(NotEditableCrudFormElement.class)) && !isAddView) {
					c.setEnabled(false);
				}

				if (field.isAnnotationPresent(ViewOnlyCrudFormElement.class)) {
					c.setEnabled(false);

					if (isAddView) {
						continue;
					}
				}

				if (layouts.size() == 1) {

					layouts.get(0).addComponent(c);

				} else {

					final SecondaryCrudFormElement secondaryCrudFormElementAnnotation = field.getAnnotation(SecondaryCrudFormElement.class);

					if (secondaryCrudFormElementAnnotation != null) {
						layouts.get(secondaryCrudFormElementAnnotation.tabNum()).addComponent(c);
					} else {
						layouts.get(0).addComponent(c);
					}

				}

			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new ToolboxGeneralException(e);
			}
		}

	}

	/**
	 * @param modelObject
	 * @param crudOperation
	 * @param preAction
	 * @param isValidSupplier
	 * @param crudAction
	 * @param afterAction
	 * @return
	 * @see FormLayoutCrudFormComponent
	 */
	public static <D> Button buildCrudFormOpButton(final D modelObject, final ToolboxSysKeys.CrudOperation crudOperation, final Consumer<D> preAction, final Supplier<Boolean> isValidSupplier, final Consumer<D> manualValidation, final Consumer<D> crudAction, final Consumer<D> afterAction) {
		return buildCrudFormOpButton(modelObject, crudOperation, preAction, isValidSupplier, manualValidation, crudAction, afterAction, new HashMap<>());
	}

	/**
	 * @param modelObject
	 * @param crudOperation
	 * @param preAction
	 * @param isValidSupplier
	 * @param crudAction
	 * @param afterAction
	 * @return
	 * @see FormLayoutCrudFormComponent
	 */
	public static <D> Button buildCrudFormOpButton(final D modelObject, final ToolboxSysKeys.CrudOperation crudOperation, final Consumer<D> preAction, final Supplier<Boolean> isValidSupplier, final Consumer<D> manualValidation, final Consumer<D> crudAction, final Consumer<D> afterAction, final Map<ToolboxSysKeys.CrudOperation, String> notifMap) {

		ToolboxAssert.notNull(crudAction);
		ToolboxAssert.notNull(modelObject);
		ToolboxAssert.notNull(crudOperation);

		final Button btn;
		final String strSuccessMsg;

		if (crudOperation.equals(ToolboxSysKeys.CrudOperation.ADD)) {
			btn = new Button(I.trc("Button", "Save"), FontAwesome.SAVE);
			btn.addStyleName(ValoTheme.BUTTON_PRIMARY);

			final String addNotification = notifMap.get(ToolboxSysKeys.CrudOperation.ADD);
			if (addNotification != null) {
				strSuccessMsg = addNotification;
			} else {
				strSuccessMsg = I.trc("Notification", "Successful save (add)!");
			}

		} else if (crudOperation.equals(ToolboxSysKeys.CrudOperation.UPDATE)) {
			btn = new Button(I.trc("Button", "Save"), FontAwesome.SAVE);
			btn.addStyleName(ValoTheme.BUTTON_PRIMARY);

			final String updateNotification = notifMap.get(ToolboxSysKeys.CrudOperation.UPDATE);
			if (updateNotification != null) {
				strSuccessMsg = updateNotification;
			} else {
				strSuccessMsg = I.trc("Notification", "Successful save (update)!");
			}

		} else if (crudOperation.equals(ToolboxSysKeys.CrudOperation.DELETE)) {
			btn = new Button(I.trc("Button", "Delete"), VaadinIcons.TRASH);
			btn.addStyleName(ValoTheme.BUTTON_DANGER);

			final String deleteNotification = notifMap.get(ToolboxSysKeys.CrudOperation.DELETE);
			if (deleteNotification != null) {
				strSuccessMsg = deleteNotification;
			} else {
				strSuccessMsg = I.trc("Notification", "Successful delete!");
			}

		} else {
			throw new ToolboxGeneralException("Unknown CrudOperation!");
			// btn = new Button(I.trc("Button", "Close"), VaadinIcons.CLOSE);
			// strSuccessMsg = null;
			// break;
		}

		btn.setWidth(null);
		btn.addStyleName("min-width-150px");
		btn.addStyleName("max-width-400px");

		btn.addClickListener(x -> {

			try {

				if (ToolboxSysKeys.CrudOperation.ADD.equals(crudOperation) || ToolboxSysKeys.CrudOperation.UPDATE.equals(crudOperation)) {

					if (preAction != null) {
						preAction.accept(modelObject);
					}

					// ---

					// log.debug("modelObject: " + modelObject);

					if (!Boolean.TRUE.equals(isValidSupplier.get())) {
						Notification.show(I.trc("Notification", "Invalid/missing data!"), Notification.Type.WARNING_MESSAGE);
						log.debug("Invalid/missing data!");
						return;
					}

					if (manualValidation != null) {
						manualValidation.accept(modelObject);
					}

				}

				// ---

				if (ToolboxSysKeys.CrudOperation.READ.equals(crudOperation)) {

					try {
						crudAction.accept(modelObject);
					} catch (final ManualValidationException e) {
						log.debug("ManualValidationException in read!", e);
					}

				} else {

					crudAction.accept(modelObject);

				}

				// ---

				if (afterAction != null) {
					afterAction.accept(modelObject);
				}

				// ---

				if (StringUtils.isNotBlank(strSuccessMsg)) {
					Notification.show(strSuccessMsg);
				}

			} catch (final Exception e) {

				UI.getCurrent().access(() -> {

					// TODO: tisztázni...
					// nem background thread, de valahogy (Supplier, Consumer stb. lambda dolgok miatt) elveszett a kapcsolat az ős UI példánnyal (pedig a felületen látszik...),
					// a .access() helyrehozza

					throw e;
				});

			}

		});

		return btn;
	}

	/**
	 * minden a layout-on levő componenst enabled=false-ra állít (nem rekurzív, belső layout-okon belül nem működik)
	 *
	 * @param layout
	 */
	public static void setEverytingDisabled(final AbstractLayout layout) {

		final Iterator<Component> iterator = layout.iterator();

		while (iterator.hasNext()) {
			final Component component = iterator.next();
			component.setEnabled(false);
		}

	}

	/**
	 * minden a layout-on levő componenst visible=false-ra állít (nem rekurzív, belső layout-okon belül nem működik)
	 *
	 * @param layout
	 */
	public static void setEverytingHidden(final AbstractLayout layout) {

		final Iterator<Component> iterator = layout.iterator();

		while (iterator.hasNext()) {
			final Component component = iterator.next();
			component.setVisible(false);
		}

	}

	/**
	 * {@link UI#close()} + átirányít az /api/logout URL-re (amit a Spring Security is észrevesz)
	 *
	 * @param ui
	 */
	public static void logout(final UI ui) {

		final Page page = ui.getPage();

		// final VaadinRequest currentRequest = VaadinService.getCurrentRequest();

		final VaadinSession vaadinSession = VaadinSession.getCurrent();

		page.open(BrandUtil.getRedirectUriHostFrontend() + "api/logout", "_self");

		// SecurityUtil.clearAuthentication();

		vaadinSession.close(); // VaadinSession != HttpSession

		// currentRequest.getWrappedSession().invalidate();

		// ui.close();

	}

	/**
	 * ékezeteket nem veszi figyelembe (értsd akkor is talál, ha "alma" vagy "álma" a szó a {@link ComboBox} stb. tartalma)...
	 *
	 * @return
	 */
	public static CaptionFilter buildOptimizedCaptionFilter() {

		return new CaptionFilter() {

			@Override
			public boolean test(final String itemCaption, final String filterText) {

				String itemCaptionNormalized = Normalizer.normalize(itemCaption, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
				itemCaptionNormalized = itemCaptionNormalized.toLowerCase();
				itemCaptionNormalized = itemCaptionNormalized.replaceAll("\\(", "");
				itemCaptionNormalized = itemCaptionNormalized.replaceAll("\\)", "");
				itemCaptionNormalized = itemCaptionNormalized.replaceAll("-", "");

				String filterTextNormalized = Normalizer.normalize(filterText, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
				filterTextNormalized = filterTextNormalized.toLowerCase();
				filterTextNormalized = filterTextNormalized.replaceAll("\\(", "");
				filterTextNormalized = filterTextNormalized.replaceAll("\\)", "");
				filterTextNormalized = filterTextNormalized.replaceAll("-", "");

				filterTextNormalized = ".*\\b" + Pattern.quote(filterTextNormalized) + ".*";

				// log.debug("caption: " + itemCaptionNormalized + ", pattern: " + filterTextNormalized);

				return itemCaptionNormalized.matches(filterTextNormalized);
			}

		};

	}

	/**
	 * Vaadin UI/Page kliens időzónája (böngésző időzónája)...
	 *
	 * @return másodpercben
	 * @see Page#getCurrent()
	 */
	public static int getPageZoneDST() {
		return Page.getCurrent().getWebBrowser().getDSTSavings() / 1000;
	}

	/**
	 * időzóna nélküli (DB/szerver) millis mennyi a Vaadin felület kliens idejére (időzónájára) váltva...
	 *
	 * @param ts
	 * @return
	 */
	public static LocalDateTime adjustToPageTimeZone(final long millis) {
		
		ZoneId toZoneId;
		
		try {
			toZoneId = ZoneId.of(Page.getCurrent().getWebBrowser().getTimeZoneId());
		} catch (Exception e) {
			toZoneId = ZoneId.of("CET");
		}

		return DateTimeUtil.adjustTimeZone(
				new Timestamp(millis), null,
				toZoneId).toLocalDateTime();
	}

	public static LocalDate adjustToPageTimeZoneDate(final long millis) {
		return LocalDate.from(adjustToPageTimeZone(millis));
	}

	/**
	 * időzóna nélküli (DB/szerver) {@link Timestamp} mennyi a Vaadin felület kliens idejére (időzónájára) váltva...
	 *
	 * @param ts
	 * @return
	 */
	public static LocalDateTime adjustToPageTimeZone(final java.sql.Timestamp ts) {

		if (ts == null) {
			return null;
		}

		return adjustToPageTimeZone(ts.getTime());
	}

	/**
	 * a Vaadin felület kliens ideje mennyi szerver időben (időzóna nélkül, átváltva UTC-re (értsd UTC+0...))
	 *
	 * @param ts
	 * @return
	 */
	public static java.sql.Timestamp adjustToServerTimeZone(final LocalDateTime localDateTime) {
		return Timestamp.valueOf(localDateTime.atOffset(ZoneOffset.ofTotalSeconds((Page.getCurrent().getWebBrowser().getTimezoneOffset()) / 1000)).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
	}

	/**
	 * @return
	 * @see Page#getCurrent()
	 */
	public static boolean useTouchMode() {
		return useTouchMode(Page.getCurrent());
	}

	/**
	 * @param page
	 * @return
	 */
	public static boolean useTouchMode(Page page) {
		return page.getWebBrowser().isTouchDevice();
	}

	/**
	 * touch device és width < 800 px // TODO: width már nincs nézve
	 * 
	 * @return
	 * @see Page#getCurrent()
	 */
	public static boolean useCompactMobileMode() {
		return useCompactMobileMode(Page.getCurrent());
	}

	/**
	 * touch device és width < 800 px // TODO: width már nincs nézve
	 * 
	 * @param page
	 * @return
	 */
	public static boolean useCompactMobileMode(Page page) {
		return useTouchMode(page) /* && (page.getBrowserWindowWidth() < 1000) */; // TODO: finomítani kell getBrowserWindowWidth() 1400 volt mobilon is (Chrome simualtor)?
	}

	/**
	 * rekurzívan megy végig (minden belsőbb layout-on is)
	 * 
	 * @see #useCompactMobileMode()
	 */
	public static void removeEveryTooltip(final HasComponents layout) {

		final Iterator<Component> iterator = layout.iterator();
		while (iterator.hasNext()) {
			final Component component = iterator.next();

			if (component instanceof HasComponents) {
				removeEveryTooltip((HasComponents) component);
			} else if (component instanceof AbstractComponent) {
				((AbstractComponent) component).setDescription("");
			}

		}

	}

	/**
	 * sima tooltip állítás, viszont iOS esetén nem teszi rá, 
	 * régebben használtuk (valószínűleg más mobile OS esetén sem jó, ha ott vannak a tooltipek)
	 *
	 * @param component
	 * @param descText
	 * @param mode
	 */
	@Deprecated
	public static void setDescriptionIfNotIOS(final AbstractComponent component, final String descText, final ContentMode mode) {

		if (!Page.getCurrent().getWebBrowser().isIOS()) {
			component.setDescription(descText, mode);
		}

	}

	/**
	 * @param caption
	 * @param list
	 * @param itemCaptionFunction
	 * @return
	 * @see CrudFormElementCollection
	 */
	public static <T extends ToolboxPersistable> ComboBox<Integer> buildCombo1(final String caption, final Collection<T> list, final Function<T, String> itemCaptionFunction) {

		// ToolboxAssert.isTrue(StringUtils.isNotBlank(caption)); // ennél az esetél nincs default // TODO: tisztázni

		final ComboBox<Integer> cmb = new ComboBox<>(caption);
		cmb.setWidth(100, Unit.PERCENTAGE);

		final Map<Integer, T> itemMap = new LinkedHashMap<>();

		for (final T t : list) {
			itemMap.put(t.getId(), t);
		}

		cmb.setDataProvider(buildOptimizedCaptionFilter(), new ListDataProvider<>(itemMap.keySet()));

		cmb.setItemCaptionGenerator(new ItemCaptionGenerator<Integer>() {

			@Override
			public String apply(final Integer i) {

				if (i == null) {
					return "";
				}

				final T t = itemMap.get(i);

				if (t == null) {
					return "";
				}

				return itemCaptionFunction.apply(t);
			}
		});

		return cmb;

	}

	public static ComboBox<String> buildCombo2(final String caption, final Collection<String> list) {
		final ComboBox<String> cmb = new ComboBox<>(caption);
		cmb.setItems(list);
		cmb.setWidth(100, Unit.PERCENTAGE);

		return cmb;
	}

	/**
	 * @param caption
	 * 		ennél a változatnál nincs default
	 * @param codeStoreTypeId
	 * @param excludeIds
	 * 		ezeket a cs id-kat hagyja ki
	 * @return
	 */
	public static ComboBox<Integer> buildCodeStoreCombo(final String caption, final int codeStoreTypeId, final Set<Integer> excludeIds) {

		final List<CodeStoreItem> list = ApplicationContextHelper.getBean(CodeStoreItemService.class).findAllByType(codeStoreTypeId);

		final Iterator<CodeStoreItem> iterator = list.iterator();

		if (excludeIds != null && !excludeIds.isEmpty()) {
			while (iterator.hasNext()) {
				final CodeStoreItem codeStoreItem = iterator.next();
				if (excludeIds.contains(codeStoreItem.getId())) {
					iterator.remove();
				}
			}
		}

		return UiHelper.buildCombo1(caption, list, CodeStoreItem::getCaptionCaption);

	}

	/**
	 * @param caption
	 * 		ennél a változatnál nincs default
	 * @param codeStoreTypeId
	 * @param includeIds
	 * 		csak ezeket a cs id-kat hagyja meg
	 * @return
	 */
	public static ComboBox<Integer> buildCodeStoreCombo2(final String caption, final int codeStoreTypeId, final Set<Integer> includeIds) {

		final List<CodeStoreItem> list = ApplicationContextHelper.getBean(CodeStoreItemService.class).findAllByType(codeStoreTypeId);

		final Iterator<CodeStoreItem> iterator = list.iterator();

		if (includeIds != null && !includeIds.isEmpty()) {
			while (iterator.hasNext()) {
				final CodeStoreItem codeStoreItem = iterator.next();
				if (!includeIds.contains(codeStoreItem.getId())) {
					iterator.remove();
				}
			}
		} else {
			list.clear();
		}

		return UiHelper.buildCombo1(caption, list, CodeStoreItem::getCaptionCaption);

	}

	/**
	 * @param caption
	 * 		null esetén default szöveg
	 * @param countryCodeStrList
	 * 		null esetén össze country (Java {@link Locale#getISOCountries} alapján)
	 * @param fullCountryNameFirst
	 * @return
	 */
	public static ComboBox<String> buildCountryCodeCombo(final String caption, Collection<String> countryCodeStrList, final boolean fullCountryNameFirst) {

		// TODO: valami cache a fix részeknek

		if (countryCodeStrList == null) {

			final String[] isoCountries = Locale.getISOCountries();
			countryCodeStrList = new LinkedHashSet<>();

			for (final String isoCountry : isoCountries) {

				if (!isoCountry.equalsIgnoreCase("AN")) { // nincs zászlónk hozzá, ez ("AN") elvileg egy spanyol tartomány, jelenleg nem kell sehol
					countryCodeStrList.add(isoCountry);
				}

			}

		}

		final ComboBox<String> cmbCountryCode = new ComboBox<>(caption == null ? I.trc("Caption", "Country code") : caption, countryCodeStrList);
		cmbCountryCode.setStyleGenerator(cc -> "w-combo-flag-" + cc.toLowerCase());

		final Locale inLocale = I18nUtil.getLoggedInUserLocale();

		if (fullCountryNameFirst) {
			cmbCountryCode.setItemCaptionGenerator(x -> StringUtils.isNotBlank(x) ? x + " (" + new Locale("", x).getDisplayCountry(inLocale) + ")" : "");
		} else {
			cmbCountryCode.setItemCaptionGenerator(x -> StringUtils.isNotBlank(x) ? new Locale("", x).getDisplayCountry(inLocale) + " (" + x + ")" : "");
		}

		cmbCountryCode.addValueChangeListener(new ValueChangeListener<String>() {

			private String lastCssClassName = null;

			@Override
			public void valueChange(final ValueChangeEvent<String> x) {

				if (this.lastCssClassName != null) {

					cmbCountryCode.removeStyleName("w-combo-value-flag");
					cmbCountryCode.removeStyleName(this.lastCssClassName);

				}

				if (StringUtils.isNotBlank(x.getValue())) {

					cmbCountryCode.addStyleName("w-combo-value-flag");

					this.lastCssClassName = "w-combo-value-flag-" + x.getValue().toLowerCase();
					cmbCountryCode.addStyleName(this.lastCssClassName);

				}

			}
		});

		return cmbCountryCode;
	}

	/**
	 * {@link I18nUtil#getLoggedInUserLocale()} alapján dönti el, hogy a nyelvek elnevezéseit milyen nyelven kell kiírni
	 *
	 * @param caption
	 * 		null esetén default caption
	 * @param langCodeList
	 * 		null esetén össze nyelv (Java {@link Locale#getISOLanguages} alapján)
	 * @return
	 */
	public static ComboBox<String> buildLangCodeCombo(final String caption, Collection<String> langCodeList) {

		// TODO: valami cache a fix részeknek

		if (langCodeList == null) {
			langCodeList = Sets.newLinkedHashSet(Lists.newArrayList(Locale.getISOLanguages()));
		}

		final ComboBox<String> cmbLangCode = new ComboBox<>(caption == null ? I.trc("Caption", "Language") : caption);
		cmbLangCode.setItems(langCodeList);
		cmbLangCode.setEmptySelectionAllowed(true);
		cmbLangCode.setWidth(100, Unit.PERCENTAGE);

		final Locale inLocale = I18nUtil.getLoggedInUserLocale();

		cmbLangCode.setItemCaptionGenerator(langCode -> {

			if (StringUtils.isBlank(langCode)) {
				return "";
			}

			final Locale locale = new Locale(langCode);
			return locale.getDisplayName(inLocale) + " (" + locale.toString() + ")";

		});

		return cmbLangCode;

	}

	/**
	 * @param caption
	 * 		null esetén default felirat lesz
	 * @param users
	 * 		null esetén mind (a belépett user tenant-ból csak)
	 * @param fullNameFirst
	 * @param forceFamilyFirstInTheFullName
	 * @return
	 */
	public static ComboBox<Integer> buildUserCombo(final String caption, List<User> users, final boolean fullNameFirst, final boolean forceFamilyFirstInTheFullName, final boolean allowDisabledUsers) {

		if (users == null && !allowDisabledUsers) {
			users = ApplicationContextHelper.getBean(UserService.class).findAllEnabledUser();
		}

		return buildUserCombo(caption, users, fullNameFirst, forceFamilyFirstInTheFullName);

	}

	/**
	 * @param caption
	 * 		null esetén default felirat lesz
	 * @param users
	 * 		null esetén mind (a belépett user tenant-ból csak)
	 * @param fullNameFirst
	 * @param forceFamilyFirstInTheFullName
	 * @return
	 */
	public static ComboBox<Integer> buildUserCombo(final String caption, List<User> users, final boolean fullNameFirst, final boolean forceFamilyFirstInTheFullName) {

		if (users == null) {
			users = ApplicationContextHelper.getBean(UserService.class).findAll();
		}

		Function<User, String> itemCaptionFunction = null;

		if (fullNameFirst) {
			itemCaptionFunction = user -> user != null ? I18nUtil.buildFullName(user, forceFamilyFirstInTheFullName, false) + " (" + user.getUsername() + ")" : "";
		} else {
			itemCaptionFunction = user -> user != null ? user.getUsername() + " (" + I18nUtil.buildFullName(user, forceFamilyFirstInTheFullName, false) + ")" : "";
		}

		return UiHelper.buildCombo1(caption == null ? I.trc("Caption", "User") : caption, users, itemCaptionFunction);

	}

	/**
	 * @param caption
	 * @param currencyCodes
	 * 		null esetén "minden" (amit a Java {@link Currency} ismer)
	 * @return
	 */
	public static ComboBox<String> buildCurrencyCombo(final String caption, Collection<String> currencyCodes) {

		if (currencyCodes == null) {
			currencyCodes = I18nUtil.getAvailableCurrencyStrings();
		}

		final ComboBox<String> cmbCurrencies = new ComboBox<>(caption == null ? I.trc("Caption", "Currency") : caption);
		cmbCurrencies.setItems(currencyCodes);
		cmbCurrencies.setEmptySelectionAllowed(true);
		cmbCurrencies.setWidth(100, Unit.PERCENTAGE);

		final Locale inLocale = I18nUtil.getLoggedInUserLocale();

		cmbCurrencies.setItemCaptionGenerator(currencyCode -> {

			if (StringUtils.isBlank(currencyCode)) {
				return "";
			}

			final Currency currency = Currency.getInstance(currencyCode);

			return currency.getCurrencyCode() + " (" + Currency.getInstance(currencyCode).getDisplayName(inLocale) + ")";

		});

		return cmbCurrencies;

	}

	/**
	 * fix Vaadin bug-ra...
	 * amikor új dialog jön fel és az nem setModal(true), akkor az ESC gomb az alatta levőt dialogot zárja be...
	 */
	public static void forceDialogFocus(final Window dialog) {

		if (dialog.isModal()) {

			// ilyenkor nincs probléma

			return;
		}

		try {
			dialog.focus();
		} catch (final Exception e) {

			// TODO: nem mindig megy...

			log.debug("dialog focus failed", e);
		}

	}

	/**
	 * dialog-on ({@link Window}) van-e a felületi komponens
	 * 
	 * @param c
	 * @return
	 */
	public static boolean isInWindow(final Component c) {

		Component parent = c.getParent();

		while (true) {

			if (parent == null) {
				return false;
			} else if (parent instanceof Window) {
				return true;
			}

			parent = parent.getParent();
		}

	}

	public static void centerParentWindow(final Component c) {

		if (c.getParent() != null && c.getParent() instanceof Window) {
			((Window) c.getParent()).center();
		} else if (c.getParent() != null && c.getParent().getParent() != null && c.getParent().getParent() instanceof Window) {
			((Window) c.getParent().getParent()).center();
		} else if (c.getParent() != null && c.getParent().getParent() != null && c.getParent().getParent().getParent() != null && c.getParent().getParent().getParent() instanceof Window) {
			((Window) c.getParent().getParent().getParent()).center();
		}

	}

	public static void closeParentWindow(final Component c) {

		if (c.getParent() != null && c.getParent() instanceof Window) {
			((Window) c.getParent()).close();
		} else if (c.getParent() != null && c.getParent().getParent() != null && c.getParent().getParent() instanceof Window) {
			((Window) c.getParent().getParent()).close();
		} else if (c.getParent() != null && c.getParent().getParent() != null && c.getParent().getParent().getParent() != null && c.getParent().getParent().getParent() instanceof Window) {
			((Window) c.getParent().getParent().getParent()).close();
		}

	}

	public static String buildJumpUrlStr(final String viewNameStr, final String paramName, final String paramValue, final boolean makeAbsoluteUrl, final boolean openViewDialog) {
		return buildJumpUrlStr(viewNameStr, paramName, paramValue, makeAbsoluteUrl, openViewDialog, true);
	}

	public static String buildJumpUrlStr(final String viewNameStr, final String paramName, final String paramValue, final boolean makeAbsoluteUrl, final boolean openViewDialog, final boolean isBackOffice) {

		// példa: http://localhost/#!mn-incoming/id=3

		final StringBuilder sb = new StringBuilder();

		if (makeAbsoluteUrl || VaadinService.getCurrentRequest() == null) {
			final String u = BrandUtil.getRedirectUriHostFrontend();
			sb.append(StringUtils.appendIfMissing(u, "/"));
		} else {
			sb.append(VaadinService.getCurrentRequest().getContextPath() + "/");
		}

		if (isBackOffice) {
			final String u = ApplicationContextHelper.getConfigProperty("tools.redirect-uri-host-frontend-back-office-relative");
			if (StringUtils.isNotBlank(u)) {
				sb.append(StringUtils.appendIfMissing(u, "/"));
			}
		}

		sb.append("#!");
		sb.append(viewNameStr);

		if (paramName != null && paramValue != null) {
			sb.append("/?");
			sb.append(paramName);
			sb.append("=");
			sb.append(paramValue);
			if (openViewDialog) {
				// sb.append("&ovd=true"); // TODO: tisztázni, bugos (rosszul hozza fel az ablakot pl: matDetails üresen jön fel)
			}
		}

		return sb.toString();
	}

	public static boolean isTheCurrentUiTheFirst() {

		final int currentUiId = UI.getCurrent().getUIId();

		final Collection<UI> uis = VaadinSession.getCurrent().getUIs();

		for (final UI ui : uis) {

			if (ui.isAttached() && !ui.isClosing()) {
				return ui.getUIId() == currentUiId;
			}

		}

		return false;
	}

	/**
	 * @return mező, érték, kell-rögtön nézegető ablak hármasok...
	 */
	public static Triple<String, String, Boolean> interpreteSearchUriFragment() {

		final String uriFragment = Page.getCurrent().getUriFragment();

		if (StringUtils.isBlank(uriFragment)) {
			return null;
		}

		final String[] split1 = uriFragment.split("/\\?");

		if (split1.length != 2) {
			return null;
		}

		final String[] split2 = split1[1].split("&");

		if (split2.length < 1) {
			return null;
		}

		final String[] split3 = split2[0].split("=");

		return Triple.of(split3[0], split3[1], split2.length >= 2 && split2[1].toLowerCase().startsWith("ovd=true"));

	}

	public static AbstractOrderedLayout buildFileDescriptInfoBox(final int fileDescriptorId) {
		return buildFileDescriptInfoBox(ApplicationContextHelper.getBean(FileStoreService.class).getFile2(fileDescriptorId, FileOperationAccessTypeIntent.READ_ONLY_INTENT));
	}

	public static AbstractOrderedLayout buildFileDescriptInfoBox(final FileDescriptor fileDescriptor) {

		final User createdByUser;
		final User modifiedByUser;

		if (!SecurityUtil.isAnonymous()) {
			if (!fileDescriptor.getCreatedBy().equals(fileDescriptor.getModifiedBy())) {
				createdByUser = ApplicationContextHelper.getBean(UserService.class).findOne(fileDescriptor.getCreatedBy());
				modifiedByUser = ApplicationContextHelper.getBean(UserService.class).findOne(fileDescriptor.getModifiedBy());
			} else {

				// ha egyezik a createdBy/modifiedBy, akkor meg lehet spórolni egy DB hívást

				final User user = ApplicationContextHelper.getBean(UserService.class).findOne(fileDescriptor.getCreatedBy());
				createdByUser = user;
				modifiedByUser = user;
			}
		} else {
			createdByUser = null;
			modifiedByUser = null;
		}

		final FormLayout fl = new FormLayout();
		fl.setMargin(true);

		final TextField txtFileName = new TextField(I.trc("Caption", "Filename"), fileDescriptor.getFilename());
		txtFileName.setWidth("100%");
		txtFileName.setReadOnly(true);
		fl.addComponent(txtFileName);

		final TextField txtFileSize = new NumberOnlyTextField(I.trc("Caption", "File size (KiB)"), true, true);
		txtFileSize.setValue(Long.toString((fileDescriptor.getFile().length() / 1024L)));
		txtFileSize.setWidth("100%");
		txtFileSize.setReadOnly(true);
		fl.addComponent(txtFileSize);

		if (createdByUser != null) {

			final TextField txtCreatedBy = new TextField(I.trc("Caption", "Created by"), createdByUser.getUsername() + " (" + I18nUtil.buildFullName(createdByUser, true, false) + ")");
			txtCreatedBy.setWidth("100%");
			txtCreatedBy.setReadOnly(true);
			fl.addComponents(txtCreatedBy);
		}

		final DateTimeField dtfCreatedOn = new DateTimeField(I.trc("Caption", "Created on"), UiHelper.adjustToPageTimeZone(fileDescriptor.getCreatedOn()));
		dtfCreatedOn.setWidth("100%");
		dtfCreatedOn.setReadOnly(true);
		fl.addComponent(dtfCreatedOn);

		if (modifiedByUser != null) {

			final TextField txtModifiedBy = new TextField(I.trc("Caption", "Modified by (last)"), modifiedByUser.getUsername() + " (" + I18nUtil.buildFullName(modifiedByUser, true, false) + ")");
			txtModifiedBy.setWidth("100%");
			txtModifiedBy.setReadOnly(true);
			fl.addComponent(txtModifiedBy);
		}

		final DateTimeField dtfModifiedOn = new DateTimeField(I.trc("Caption", "Modified on (last)"), UiHelper.adjustToPageTimeZone(fileDescriptor.getModifiedOn()));
		dtfModifiedOn.setWidth("100%");
		dtfModifiedOn.setReadOnly(true);
		fl.addComponent(dtfModifiedOn);

		fl.setWidth("500px");

		return fl;
	}

	/**
	 * XMLHttpRequest...
	 * másodlagos, helyi szerver hívása 
	 * (értsd valamilyen embedded desktop app-ban szerver)... 
	 * experimental
	 *
	 * @param url
	 */
	public static void callLocalhostServer(final String url) {

		final UI ui = UI.getCurrent();
		if (ui != null) {

			final StringBuilder sbScript = new StringBuilder();
			sbScript.append("var xhttp = new XMLHttpRequest();");
			sbScript.append("xhttp.open('GET', '" + url + "', true);");
			sbScript.append("xhttp.send();");

			ui.getPage().getJavaScript().execute(sbScript.toString());
			ui.push();
		}

	}

	/**
	 * @return
	 * 
	 * @see I18nUtil#getLoggedInUserLocale()
	 */
	public static Locale getCurrentUiLocale() {

		Locale locale = I18nUtil.getLoggedInUserLocale();

		if (locale == null && UI.getCurrent() != null) {
			locale = UI.getCurrent().getLocale();
		}

		return locale;
	}

	public static String formatBigDecimalCurrentUiLocale(final BigDecimal value, final int decimalScale) {
		return NumberUtil.formatBigDecimal(value, getCurrentUiLocale(), decimalScale, RoundingMode.HALF_UP);
	}

	public static String formatBigDecimalCurrentUiLocale(final BigDecimal value, final int decimalScale, final RoundingMode roundingMode) {
		return NumberUtil.formatBigDecimal(value, getCurrentUiLocale(), decimalScale, roundingMode);
	}

	/**
	 * @deprecated 
	 * 		{@link #getCurrentUiDecimalFormatSymbols()} 
	 */
	@Deprecated
	public static DecimalFormatSymbols getLoggedInUserDecimalFormatSymbols() {
		return getCurrentUiDecimalFormatSymbols();
	}

	public static DecimalFormatSymbols getCurrentUiDecimalFormatSymbols() {
		return ((DecimalFormat) DecimalFormat.getInstance(getCurrentUiLocale())).getDecimalFormatSymbols();
	}

	public static DecimalFormat getCurrentUiDecimalFormat() {
		return ((DecimalFormat) DecimalFormat.getInstance(getCurrentUiLocale()));
	}

	/**
	 * validátor hozzáadása component-re (arra az esetre, ha nem szerepel model-ben a mező, ezért nem tudunk annotációt tenni rá), 
	 * FIGYELEM: HA BINDER IS HASZNÁLVA VAN, AKKOR NEM ÉRVÉNYESÜL A VALIDATOR!
	 *
	 * @param field
	 * @param validator
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void addValidator(final AbstractField field, final Validator validator) {

		field.addValueChangeListener(event -> {
			final ValidationResult result = validator.apply(event.getValue(), new ValueContext(field));

			if (result.isError()) {
				final UserError error = new UserError(result.getErrorMessage());
				field.setComponentError(error);
			} else {
				field.setComponentError(null);
			}
		});

	}

	/**
	 * csak {@link ToolboxSysKeys.UserAuth#ROLE_SUPER_ADMIN_STR} usereknek... 
	 * figyelem csak Vaadin {@link UI} példányokra értendő (csak ott látszik a figyelmeztetés jeleneleg)!
	 * 
	 * @return
	 */
	public static Button buildShutdownMessageWarningTriggerBtn() {

		final Button btn = new Button(I.trc("Caption", "Show shutdown/restart warning on every UI"), x -> {

			SecurityUtil.limitAccessSuperAdmin();

			final Collection<WeakReference<AbstractToolboxUI>> uiWrSet = AbstractToolboxUI.getUiWrSet();
			for (final WeakReference<AbstractToolboxUI> wrUi : uiWrSet) {
				final AbstractToolboxUI ui = wrUi.get();
				if (ui != null && ui.isAttached() && !ui.isClosing()) {

					try {
						Thread.sleep(1); // 1 millisec várakozás, hogy ne egyszerre legyen minden UI-on websocket/push aktvivitás
					} catch (final InterruptedException e) {
						//
					}

					ui.access(() -> {
						ui.showShutdownWarning();
					});
				}
			}

		});

		btn.setVisible(SecurityUtil.hasSuperAdminRole());
		btn.addStyleName(ValoTheme.BUTTON_DANGER);

		return btn;

	}

	public static AbstractOrderedLayout buildMemDiagnosticAndManualGcComponent() {

		final HorizontalLayout hlFreeRam = new HorizontalLayout();
		hlFreeRam.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);

		final Label lblPresumableFreeMemory = new Label("presumably free mem (MiB): " + DiagnosticsHelper.getPresumableFreeMemory());
		hlFreeRam.addComponent(lblPresumableFreeMemory);

		final Button btnManualGc = new Button(VaadinIcons.TRASH);
		btnManualGc.addClickListener(x -> {
			System.gc();
			lblPresumableFreeMemory.setValue("presumably free mem (MiB): " + DiagnosticsHelper.getPresumableFreeMemory());
		});
		btnManualGc.setVisible(SecurityUtil.hasSuperAdminRole());
		btnManualGc.setDescription("manual System.gc() call");
		btnManualGc.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
		btnManualGc.addStyleName(ValoTheme.BUTTON_TINY);
		hlFreeRam.addComponent(btnManualGc);

		final VerticalLayout vl = new VerticalLayout(new Label("total mem (MiB): " + DiagnosticsHelper.getMaxMemory()), hlFreeRam);
		vl.setMargin(false);

		return vl;
	}

	public static Label buildActiveSessionCountAndVaadinUiCountDiagnosticLabel() {

		final Pair<Integer, AtomicLongMap<String>> pair = UiHelper.getActiveUiCount();

		final Label lbl = new Label("active session count: " + "?" + " (Vaadin UI count: " + pair.getLeft() + ")");
		// lbl.setDescription(SecurityUtil.getSessionPrincipalListStr() + " | " + pair.getRight().toString());
		lbl.setDescription(pair.getRight().toString());

		return lbl;
	}

	/**
	 * csak {@link ToolboxSysKeys.UserAuth#ROLE_SUPER_ADMIN_STR} usereknek elérhető metódus
	 * 
	 * @return
	 */
	public static Pair<Integer, AtomicLongMap<String>> getActiveUiCount() {

		try {

			SecurityUtil.limitAccessSuperAdmin();

			final TreeSet<String> classSet = new TreeSet<>();
			final AtomicLongMap<String> classMap = AtomicLongMap.create();
			int i = 0;

			final Collection<WeakReference<AbstractToolboxUI>> uiWrSet = AbstractToolboxUI.getUiWrSet();
			for (final WeakReference<AbstractToolboxUI> wrUi : uiWrSet) {
				final AbstractToolboxUI ui = wrUi.get();
				if (ui != null && ui.isAttached() && !ui.isClosing()) {
					final String canonicalName = ui.getClass().getCanonicalName();
					if (canonicalName != null) {
						classSet.add(canonicalName);
						classMap.incrementAndGet(canonicalName);
						i++;
					}
				}
			}

			return Pair.of(i, classMap);

		} catch (final Exception e) {
			log.error("getActiveUiCount error", e);
			return Pair.of(-1, AtomicLongMap.create());
		}

	}

	/**
	 * azonos mezőnevek esetén setValue()-val beírja a Vaadin mezőbe a data model mező tartalmát
	 * 
	 * @param from
	 * @param toCrudFormElementCollection
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void copyProperties(Object from, CrudFormElementCollection toCrudFormElementCollection) {

		try {

			Field[] fields1 = from.getClass().getDeclaredFields();
			Field[] fields2 = toCrudFormElementCollection.getClass().getDeclaredFields();

			for (Field field1 : fields1) {
				for (Field field2 : fields2) {

					if (field1.getName().equals(field2.getName())) {

						field1.setAccessible(true);
						field2.setAccessible(true);

						Object o1 = field1.get(from);
						Object o2 = field2.get(toCrudFormElementCollection);

						if (o2 instanceof HasValue) {
							((HasValue) o2).setValue(o1);
						}

						break;
					}
				}
			}

		} catch (Exception e) {
			throw new ToolboxGeneralException(e);
		}

	}

	/**
	 * setValue()-val beírja a Vaadin mezőbe a data model mező tartalmát
	 * 
	 * @param from
	 * 		model object
	 * @param toCrudFormElementCollection
	 * 		Vaadin form {@link CrudFormElementCollection}
	 * @param fieldMappings
	 * 		from (key), to (value) Java mezőnevek (megengedő... ha nem találja a mezőnevet valamelyik oldalon, akkor kihagyja, nincs Exception) 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void copyProperties(Object from, CrudFormElementCollection toCrudFormElementCollection, Map<String, String> fieldMappings) {

		try {

			Field[] fields1 = from.getClass().getDeclaredFields();
			Field[] fields2 = toCrudFormElementCollection.getClass().getDeclaredFields();

			Map<String, Field> fields1Map = new HashMap<>();
			Map<String, Field> fields2Map = new HashMap<>();

			for (Field field1 : fields1) {
				fields1Map.put(field1.getName(), field1);
			}

			for (Field field2 : fields2) {
				fields2Map.put(field2.getName(), field2);
			}

			for (Entry<String, String> fieldMapping : fieldMappings.entrySet()) {

				Field field1 = fields1Map.get(fieldMapping.getKey());

				if (field1 == null) {
					continue;
				}

				Field field2 = fields2Map.get(fieldMapping.getValue());

				if (field2 == null) {
					continue;
				}

				field1.setAccessible(true);
				field2.setAccessible(true);

				Object o1 = field1.get(from);
				Object o2 = field2.get(toCrudFormElementCollection);

				if (o2 instanceof HasValue) {

					try {

						if (o1 instanceof java.sql.Timestamp) {
							((HasValue) o2).setValue(UiHelper.adjustToPageTimeZone(((java.sql.Date) o1).getTime()));
						} else if (o1 instanceof java.sql.Date) {
							((HasValue) o2).setValue(UiHelper.adjustToPageTimeZoneDate(((java.sql.Date) o1).getTime()));
						} else {
							((HasValue) o2).setValue(o1);
						}

					} catch (java.lang.ClassCastException e) {
						// Vaadin mező nem tudja fogadni az értéket
						log.warn("copyProperties() set field value failed", e);
					}

				}

			}

		} catch (Exception e) {
			throw new ToolboxGeneralException(e);
		}

	}

	public static Window buildInfoBoxDialog(final String dialogCaption, final String infoStr, final ContentMode contentMode) {
		return buildInfoBoxDialog(dialogCaption, infoStr, contentMode, "600px");
	}

	public static Window buildInfoBoxDialog(final String dialogCaption, final String infoStr, final ContentMode contentMode, final String dialogWidth) {

		final VerticalLayout vlDialog = new VerticalLayout();
		vlDialog.setWidth("100%");
		vlDialog.setHeight(null);
		vlDialog.setMargin(true);
		// vlDialog.setSpacing(true);

		final Label lbl = new Label();
		lbl.setWidth("100%");
		lbl.setContentMode(contentMode);

		if (ContentMode.HTML.equals(contentMode)) {
			lbl.setValue(Jsoup.clean(infoStr, Safelist.relaxed()));
		} else {
			lbl.setValue(infoStr);
		}

		vlDialog.addComponent(lbl);

		final Window dialog = new Window(dialogCaption, vlDialog);
		dialog.setWidth(dialogWidth);
		dialog.setHeight(null);
		dialog.setModal(true);

		return dialog;

	}

	public static void removeEveryClickListner(Button btn) {
		Collection<?> listeners = btn.getListeners(com.vaadin.ui.Button.ClickEvent.class);
		for (Object cl : listeners) {
			btn.removeClickListener(((ClickListener) cl));
		}
	}

	public static GridLayout alignAll(GridLayout layout, Alignment alignment) {
		for (Component component : layout) {
			layout.setComponentAlignment(component, alignment);
		}
		return layout;
	}

	public static HorizontalLayout alignAll(HorizontalLayout layout, Alignment alignment) {
		for (Component component : layout) {
			layout.setComponentAlignment(component, alignment);
		}
		return layout;
	}

	public static VerticalLayout alignAll(VerticalLayout layout, Alignment alignment) {
		for (Component component : layout) {
			layout.setComponentAlignment(component, alignment);
		}
		return layout;
	}

}
