package hu.lanoga.toolbox.vaadin.component.crud;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.teamunify.i18n.I;
import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.DateField;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.PopupView;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.filter.internal.SearchCriteria;
import hu.lanoga.toolbox.filter.internal.SearchCriteria.SearchCriteriaBuilder;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.vaadin.component.NumberOnlyTextField;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@SuppressWarnings("rawtypes")
public class HeaderRowSearchComponent<D extends ToolboxPersistable> extends HorizontalLayout implements ValueChangeListener {

	/**
	 * ha az alap, eredeti (a model osztály annotáció alapján lévő) felirat nem jó, 
	 * akkor ezzel utólag át lehet nevezni (de úgy, hogy a keresés stb. ne romoljon el)
	 *
	 * @param crud
	 * @param list
	 * 		a pair bal oldala a módosítani kívánt column columnId-ja (pl.: "partnerIdCaption");
	 * 		jobb oldalon szerepel amire módosítani szeretnénk a column látható feliratát
	 */
	public static void changeHeaderRowSearchComponentColumnCaptions(final CrudGridComponent crud, final List<Pair<String, String>> list) {

		for (Pair<String, String> pair : list) {

			PopupView popup = (PopupView) crud.getGrid().getDefaultHeaderRow().getCell(pair.getLeft()).getComponent();
			PopupView.Content content = popup.getContent();

			PopupView.Content alteredContent = new PopupView.Content() {

				@Override
				public String getMinimizedValueAsHTML() {
					return pair.getRight();
				}

				@Override
				public Component getPopupComponent() {
					return content.getPopupComponent();
				}
			};

			popup.setContent(alteredContent);

		}

	}

	private final CrudGridComponent<D> crudGridComponent;
	private final ToolboxBackEndDataProvider<D> toolboxBackEndDataProvider;

	private final Class<?> searchValueClass;
	private final String fieldName;

	private ToolboxSysKeys.SearchCriteriaOperation searchOperation;

	private AbstractField searchField1;
	private AbstractField searchField2;

	private ComboBox<Integer> cmbSearch;

	private Button btnReset;

	private boolean freezeSearch = false;
	private Integer codeStoreTypeId;
	private String searchComboMapId;
	private String searchTargetFieldName;

	private boolean wasInitialized = false;
	private Triple<String, String, Boolean> interpretedSearchUriFragment;
	private boolean interpretedSearchUriFragmentIsRelevantForThisComp;
	private Consumer<String> atuiCallbackConsumer;

	HeaderRowSearchComponent(final Class<?> searchValueClass, final String fieldName, final CrudGridComponent<D> crudGridComponent, final ToolboxBackEndDataProvider<D> toolboxBackEndDataProvider, final Integer codeStoreTypeId, final String searchComboMapId, final String searchTargetFieldName, final Triple<String, String, Boolean> interpretedSearchUriFragment) {

		super();

		// ---

		this.searchValueClass = searchValueClass;
		this.fieldName = fieldName;

		this.crudGridComponent = crudGridComponent;
		this.toolboxBackEndDataProvider = toolboxBackEndDataProvider;
		this.codeStoreTypeId = codeStoreTypeId;
		this.searchComboMapId = searchComboMapId;
		this.searchTargetFieldName = searchTargetFieldName;

		// ---

		this.setMargin(false);
		this.setSpacing(true);
		this.setWidth(null);
		this.setHeight(null);

		// ---

		this.interpretedSearchUriFragment = interpretedSearchUriFragment;

		this.interpretedSearchUriFragmentIsRelevantForThisComp = this.interpretedSearchUriFragment != null && fieldName.equals(this.interpretedSearchUriFragment.getLeft());

		if (this.interpretedSearchUriFragmentIsRelevantForThisComp) {

			HeaderRowSearchComponent.this.init(); // ilyenkor rögtön kell init, nem elég később ultra lazy módón

		}
	}

	@SuppressWarnings("unchecked")
	void init() {

		if (this.wasInitialized) {
			return;
		}

		this.wasInitialized = true;

		if (this.codeStoreTypeId != null || this.searchComboMapId != null) {

			if (this.codeStoreTypeId != null) {
				this.cmbSearch = UiHelper.buildCodeStoreCombo(I.trc("Caption", "Filter"), this.codeStoreTypeId, null);
			} else if (this.searchComboMapId != null) {

				this.cmbSearch = this.crudGridComponent.getSearchComboMap() == null ? null : this.crudGridComponent.getSearchComboMap().get(this.searchComboMapId);

				if (this.cmbSearch == null) {

					if ("USERS".equals(this.searchComboMapId)) {
						this.cmbSearch = UiHelper.buildUserCombo(I.trc("Caption", "Filter"), null, false, true);
					}

				}

				if (this.cmbSearch == null) {
					log.warn("missing searchComboMap value: " + this.searchComboMapId);
				}
			}

			if (this.cmbSearch != null) {

				this.cmbSearch.setCaption(null);
				this.cmbSearch.addValueChangeListener(this);
				this.cmbSearch.setEmptySelectionAllowed(false);
				this.cmbSearch.setWidth(100, Unit.PERCENTAGE);
				this.cmbSearch.addStyleName(ValoTheme.COMBOBOX_SMALL);

				this.cmbSearch.setData(this.searchTargetFieldName);

				this.addComponent(this.cmbSearch);
				this.setExpandRatio(this.cmbSearch, 1f);

				this.addResetBtn(clickEvent -> {
					this.cmbSearch.clear();
				});

			}

		} else if (String.class.equals(this.searchValueClass)) {

			final TextField txtSearch = new TextField();

			txtSearch.addStyleName(ValoTheme.TEXTFIELD_SMALL);
			txtSearch.setValueChangeMode(ValueChangeMode.LAZY);
			txtSearch.setValueChangeTimeout(200);
			txtSearch.addValueChangeListener(this);
			txtSearch.setPlaceholder(I.trc("Placeholder", "Filter"));

			this.searchField1 = txtSearch;

			this.addFieldsAndResetBtn(false);

		} else if (Integer.class.equals(this.searchValueClass) || Long.class.equals(this.searchValueClass)) {

			final NumberOnlyTextField txtNumberSearch = new NumberOnlyTextField(null, true, false);
			txtNumberSearch.setTurnBackToInternationalFormatOnGetValue(true);
			
			txtNumberSearch.addStyleName(ValoTheme.TEXTFIELD_SMALL);
			txtNumberSearch.setValueChangeMode(ValueChangeMode.LAZY);
			txtNumberSearch.setValueChangeTimeout(200);
			txtNumberSearch.addValueChangeListener(this);
			txtNumberSearch.addStyleName("text-align-left");
			txtNumberSearch.setPlaceholder(I.trc("Placeholder", "Filter"));

			this.searchField1 = txtNumberSearch;

			this.addFieldsAndResetBtn(false);

		} else if (BigDecimal.class.equals(this.searchValueClass) || Float.class.equals(this.searchValueClass) || Double.class.equals(this.searchValueClass)) {

			final NumberOnlyTextField txtNumberSearch = new NumberOnlyTextField(null, true, true);
			txtNumberSearch.setTurnBackToInternationalFormatOnGetValue(true);

			txtNumberSearch.addStyleName(ValoTheme.TEXTFIELD_SMALL);
			txtNumberSearch.setValueChangeMode(ValueChangeMode.LAZY);
			txtNumberSearch.setValueChangeTimeout(200);
			txtNumberSearch.addValueChangeListener(this);
			txtNumberSearch.addStyleName("text-align-left");
			txtNumberSearch.setPlaceholder(I.trc("Placeholder", "Filter"));

			this.searchField1 = txtNumberSearch;

			this.addFieldsAndResetBtn(false);

		} else if (Boolean.class.equals(this.searchValueClass)) {

			final CheckBox chkSearch = new CheckBox();

			chkSearch.addStyleName(ValoTheme.TEXTFIELD_SMALL);
			chkSearch.addValueChangeListener(this);
			chkSearch.setWidth(100, Unit.PERCENTAGE);

			this.searchField1 = chkSearch;

			this.addFieldsAndResetBtn(false);

		} else if (java.sql.Date.class.equals(this.searchValueClass) || java.sql.Timestamp.class.equals(this.searchValueClass)) { //

			if (this.searchField1 != null) {
				this.searchField1.clear();
			}
			if (this.searchField2 != null) {
				this.searchField2.clear();
			}

			// első keresési feltétel

			if (java.sql.Date.class.equals(this.searchValueClass)) {
				final DateField dateSearch1 = new DateField();
				dateSearch1.setPlaceholder(I.trc("Placeholder", "Filter value"));
				this.searchField1 = dateSearch1;
			} else {
				final DateTimeField dateSearch1 = new DateTimeField();
				dateSearch1.setPlaceholder(I.trc("Placeholder", "Filter value"));
				this.searchField1 = dateSearch1;
			}

			this.searchField1.setWidth(100, Unit.PERCENTAGE);
			this.searchField1.addStyleName(ValoTheme.TEXTFIELD_SMALL);
			this.searchField1.addStyleName("text-align-left");
			this.searchField1.addValueChangeListener(this);

			// második keresési feltétel

			if (java.sql.Date.class.equals(this.searchValueClass)) {
				final DateField dateSearch2 = new DateField();
				dateSearch2.setPlaceholder(I.trc("Placeholder", "Filter value"));
				this.searchField2 = dateSearch2;
			} else {
				final DateTimeField dateSearch2 = new DateTimeField();
				dateSearch2.setPlaceholder(I.trc("Placeholder", "Filter value"));
				this.searchField2 = dateSearch2;
			}

			this.searchField2.setWidth(100, Unit.PERCENTAGE);
			this.searchField2.addStyleName(ValoTheme.TEXTFIELD_SMALL);
			this.searchField2.addStyleName("text-align-left");
			this.searchField2.addValueChangeListener(this);

			// ---

			this.addFieldsAndResetBtn(true);
		}

		// ---

		if (this.searchField1 != null) {

			if (this.interpretedSearchUriFragmentIsRelevantForThisComp) {

				this.searchField1.setValue(this.interpretedSearchUriFragment.getMiddle());

				if (Boolean.TRUE.equals(this.interpretedSearchUriFragment.getRight()) && this.fieldName.equals("id")) {

					this.crudGridComponent.setSelectedItemWithId(Integer.parseInt(this.interpretedSearchUriFragment.getMiddle()));

					if (this.crudGridComponent.getBtnView().isVisible() && this.crudGridComponent.getBtnView().isEnabled()) {
						this.crudGridComponent.getBtnView().click();
					}

				}

			}

		}
	}

	private void addResetBtn(final ClickListener clickListener) {

		this.btnReset = new Button();
		this.btnReset.addStyleName(ValoTheme.BUTTON_TINY);
		this.btnReset.setIcon(VaadinIcons.ERASER);
		this.btnReset.setWidth(40, Unit.PIXELS);
		this.btnReset.setEnabled(false);

		this.btnReset.addClickListener(clickListener);
		this.btnReset.addClickListener(x -> {
			if (this.getParent() instanceof PopupView) {
				((PopupView) this.getParent()).setPopupVisible(false);
			}
		});

		this.addComponent(this.btnReset);
		this.setComponentAlignment(this.btnReset, Alignment.MIDDLE_RIGHT);

	}

	private void addFieldsAndResetBtn(final boolean allowSearchField2) {

		// combobox esetben nem ezzel rakjuk fel, azt lásd fentebb...

		this.searchField1.setWidth(100, Unit.PERCENTAGE);
		this.addComponent(this.searchField1);
		this.setComponentAlignment(this.searchField1, Alignment.MIDDLE_CENTER);
		this.setExpandRatio(this.searchField1, 1f);

		// ---

		if (allowSearchField2) {
			this.searchField2.setWidth(100, Unit.PERCENTAGE);
			this.addComponent(this.searchField2);
			this.setComponentAlignment(this.searchField2, Alignment.MIDDLE_CENTER);
			this.setExpandRatio(this.searchField2, 1f);
		}

		// ---

		this.addResetBtn(clickEvent -> {
			this.searchField1.clear();
			if (allowSearchField2) {
				this.searchField2.clear();
			}
		});

	}

	@Override
	public void valueChange(final ValueChangeEvent event) {

		if (this.freezeSearch) {
			log.debug("search skipped (freezeSearch=true)");
			return;
		}

		final SearchCriteriaBuilder searchCriteriaBuilder = SearchCriteria.builder().fieldName(this.fieldName);
		this.toolboxBackEndDataProvider.getSearchCriteriaSet().remove(searchCriteriaBuilder.build());

		this.btnReset.setEnabled(true);
		if (this.getParent() instanceof PopupView) {
			((PopupView) this.getParent()).addStyleName("w-filtered");
		}

		if (this.cmbSearch != null) {

			final String s = (String) this.cmbSearch.getData();

			if (StringUtils.isNotBlank(s)) {

				String sType = null;
				String sFieldName = s;

				if (s.contains(":")) {
					String[] split = s.split(":");
					sType = split[0];
					sFieldName = split[1];
				}

				searchCriteriaBuilder.fieldName(sFieldName);
				this.toolboxBackEndDataProvider.getSearchCriteriaSet().remove(searchCriteriaBuilder.build());

				if (this.cmbSearch.getValue() != null) {

					final Integer value = this.cmbSearch.getValue();

					searchCriteriaBuilder.criteriaType(Integer.class).value(value);

					if (sType != null && sType.equalsIgnoreCase("jsonarr")) {
						searchCriteriaBuilder.operation(ToolboxSysKeys.SearchCriteriaOperation.JSON_CONTAINS);
					} else {
						searchCriteriaBuilder.operation(ToolboxSysKeys.SearchCriteriaOperation.EQ);
					}

					this.toolboxBackEndDataProvider.getSearchCriteriaSet().add(searchCriteriaBuilder.build());

				}

			} else {

				if (this.cmbSearch.getValue() != null) {

					final String caption = this.cmbSearch.getItemCaptionGenerator().apply(this.cmbSearch.getValue());

					log.debug("cmbSearch (String/Caption based): " + this.fieldName + ", " + caption);

					this.toolboxBackEndDataProvider.getSearchCriteriaSet().add(searchCriteriaBuilder
							.criteriaType(this.searchValueClass)
							.operation(ToolboxSysKeys.SearchCriteriaOperation.EQ)
							.value(caption)
							.build());
				}

			}

		} else if (java.sql.Timestamp.class.equals(this.searchValueClass)) {

			if ((this.searchField1.getValue() != null) && (this.searchField2.getValue() == null)) {

				this.toolboxBackEndDataProvider.getSearchCriteriaSet().add(searchCriteriaBuilder
						.criteriaType(this.searchValueClass)
						.operation(ToolboxSysKeys.SearchCriteriaOperation.BIGGER_THAN_EQUALS)
						.value(UiHelper.adjustToServerTimeZone((LocalDateTime) this.searchField1.getValue()))
						.build());

			} else if ((this.searchField1.getValue() == null) && (this.searchField2.getValue() != null)) {

				this.toolboxBackEndDataProvider.getSearchCriteriaSet().add(searchCriteriaBuilder
						.criteriaType(this.searchValueClass)
						.operation(ToolboxSysKeys.SearchCriteriaOperation.SMALLER_THAN_EQUALS)
						.value(UiHelper.adjustToServerTimeZone((LocalDateTime) this.searchField2.getValue()))
						.build());

			} else if ((this.searchField2.getValue() != null) && StringUtils.isNotBlank(this.searchField2.getValue().toString()) && (this.searchField1.getValue() != null)) {

				this.toolboxBackEndDataProvider.getSearchCriteriaSet().add(searchCriteriaBuilder
						.criteriaType(this.searchValueClass)
						.operation(ToolboxSysKeys.SearchCriteriaOperation.BETWEEN)
						.value(UiHelper.adjustToServerTimeZone((LocalDateTime) this.searchField1.getValue()))
						.secondValue(UiHelper.adjustToServerTimeZone((LocalDateTime) this.searchField2.getValue()))
						.build());

			}

		} else if (java.sql.Date.class.equals(this.searchValueClass)) {

			if ((this.searchField1.getValue() != null) && (this.searchField2.getValue() == null)) {

				this.toolboxBackEndDataProvider.getSearchCriteriaSet().add(searchCriteriaBuilder
						.criteriaType(this.searchValueClass)
						.operation(ToolboxSysKeys.SearchCriteriaOperation.BIGGER_THAN_EQUALS)
						.value(this.searchField1.getValue().toString())
						.build());

			} else if ((this.searchField1.getValue() == null) && (this.searchField2.getValue() != null)) {

				this.toolboxBackEndDataProvider.getSearchCriteriaSet().add(searchCriteriaBuilder
						.criteriaType(this.searchValueClass)
						.operation(ToolboxSysKeys.SearchCriteriaOperation.SMALLER_THAN_EQUALS)
						.value(this.searchField2.getValue().toString())
						.build());

			} else if ((this.searchField2.getValue() != null) && StringUtils.isNotBlank(this.searchField2.getValue().toString()) && (this.searchField1.getValue() != null)) {

				this.toolboxBackEndDataProvider.getSearchCriteriaSet().add(searchCriteriaBuilder
						.criteriaType(this.searchValueClass)
						.operation(ToolboxSysKeys.SearchCriteriaOperation.BETWEEN)
						.value(this.searchField1.getValue().toString())
						.secondValue(this.searchField2.getValue().toString())
						.build());

			}

		} else if ((this.searchField1 != null) && (this.searchField1.getValue() != null) && StringUtils.isNotBlank(this.searchField1.getValue().toString())) {

			if (String.class.equals(this.searchValueClass)) {

				this.toolboxBackEndDataProvider.getSearchCriteriaSet().add(searchCriteriaBuilder
						.criteriaType(this.searchValueClass)
						.operation(ToolboxSysKeys.SearchCriteriaOperation.LIKE)
						.value(this.searchField1.getValue().toString())
						.build());

			} else if (Integer.class.equals(this.searchValueClass) || Long.class.equals(this.searchValueClass) || BigDecimal.class.equals(this.searchValueClass) || Float.class.equals(this.searchValueClass) || Double.class.equals(this.searchValueClass)) {

				this.toolboxBackEndDataProvider.getSearchCriteriaSet().add(searchCriteriaBuilder
						.criteriaType(this.searchValueClass)
						.operation(ToolboxSysKeys.SearchCriteriaOperation.EQ)
						.value(this.searchField1.getValue().toString())
						.build());

			} else {

				this.toolboxBackEndDataProvider.getSearchCriteriaSet().add(searchCriteriaBuilder
						.criteriaType(this.searchValueClass)
						.operation(ToolboxSysKeys.SearchCriteriaOperation.EQ)
						.value(this.searchField1.getValue())
						.build());

			}

		}

		// ---

		if ((this.cmbSearch == null || this.cmbSearch.getValue() == null) &&
				(this.searchField1 == null || this.searchField1.getValue() == null || StringUtils.isBlank(this.searchField1.getValue().toString())) &&
				(this.searchField2 == null || this.searchField2.getValue() == null || StringUtils.isBlank(this.searchField2.getValue().toString()))) {

			// ha már kvázi kézzel reset-re tette (kézzel ürítette a mezőt)

			this.btnReset.setEnabled(false);
			if (this.getParent() instanceof PopupView) {
				((PopupView) this.getParent()).removeStyleName("w-filtered");
			}
		}

		// ---

		this.crudGridComponent.refreshGrid();

	}

}
