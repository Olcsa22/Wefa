/**
 * based on https://github.com/alejandro-du/crudui (Apache License 2.0, https://github.com/alejandro-du/crudui/blob/master/LICENSE.txt)
 */
package hu.lanoga.toolbox.vaadin.component.crud;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.data.domain.Pageable;
import org.vaadin.grid.cellrenderers.view.ConverterRenderer;
import org.vaadin.teemusa.gridextensions.paging.PagedDataProvider;
import org.vaadin.teemusa.gridextensions.paging.PagingControls;

import com.google.common.collect.Sets;
import com.teamunify.i18n.I;
import com.vaadin.data.Converter;
import com.vaadin.data.HasValue;
import com.vaadin.data.Result;
import com.vaadin.data.ValueContext;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.GridSortOrderBuilder;
import com.vaadin.data.provider.Query;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PopupView;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.components.grid.HeaderRow;
import com.vaadin.ui.components.grid.MultiSelectionModel;
import com.vaadin.ui.components.grid.SingleSelectionModel;
import com.vaadin.ui.renderers.LocalDateTimeRenderer;
import com.vaadin.ui.renderers.NumberRenderer;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.codestore.CodeStoreItem;
import hu.lanoga.toolbox.codestore.CodeStoreItemService;
import hu.lanoga.toolbox.exception.ToolboxException;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.export.ExporterException;
import hu.lanoga.toolbox.export.ExporterMananger;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileDescriptorJdbcRepository;
import hu.lanoga.toolbox.file.FileStoreHelper;
import hu.lanoga.toolbox.filter.internal.SearchCriteria;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.service.LazyEnhanceCrudService;
import hu.lanoga.toolbox.service.ToolboxCrudService;
import hu.lanoga.toolbox.session.GridFilterSessionBean;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.util.ToolboxAssert;
import hu.lanoga.toolbox.vaadin.AbstractToolboxUI;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class CrudGridComponent<D extends ToolboxPersistable> extends VerticalLayout {

	@Getter
	@Setter
	public static class CrudGridColumnSetupModel {

		private int columnPosition;
		private String fieldName;
		private Class<?> fieldType;
		private String translationMsg;
		private boolean allowSearch;
		private boolean allowOrder;
		private Integer codeStoreTypeId;

		private Integer columnExpandRatio;
		private boolean allowHide;
		private boolean startHidden;

		private String searchComboMapId;

		private String searchTargetFieldName;

		@Deprecated
		private boolean lazyLoadFilter;

		public CrudGridColumnSetupModel(final int columnPosition, final String fieldName, final Class<?> fieldType, final String translationMsg, final Integer codeStoreTypeId, final boolean allowSearch, boolean allowOrder, final Integer columnExpandRatio, final boolean allowHide, final boolean startHidden, final String searchComboMapId, final String searchTargetFieldName, final boolean lazyLoadFilter) {
			this.columnPosition = columnPosition;
			this.fieldName = StringUtils.isBlank(fieldName) ? null : fieldName;
			this.fieldType = fieldType;
			this.translationMsg = StringUtils.isBlank(translationMsg) ? null : translationMsg;
			this.codeStoreTypeId = codeStoreTypeId;
			this.allowSearch = allowSearch;
			this.allowOrder = allowOrder;
			this.columnExpandRatio = columnExpandRatio;
			this.allowHide = allowHide;
			this.startHidden = startHidden;
			this.searchComboMapId = StringUtils.isBlank(searchComboMapId) ? null : searchComboMapId;
			this.searchTargetFieldName = StringUtils.isBlank(searchTargetFieldName) ? null : searchTargetFieldName;
			this.lazyLoadFilter = lazyLoadFilter;
		}

		public CrudGridColumnSetupModel(final int columnPosition, final String fieldName, final Class<?> fieldType, final String translationMsg, final Integer codeStoreTypeId, final boolean allowSearch, final Integer columnExpandRatio, final boolean allowHide, final boolean startHidden, final String searchComboMapId, final String searchTargetFieldName, final boolean lazyLoadFilter) {
			this(columnPosition, fieldName, fieldType, translationMsg, codeStoreTypeId, allowSearch, true, columnExpandRatio, allowHide, startHidden, searchComboMapId, searchTargetFieldName, lazyLoadFilter);
		}

		public CrudGridColumnSetupModel(final int columnPosition, final String fieldName, final Class<?> fieldType, final String translationMsg, final Integer codeStoreTypeId, final boolean allowSearch) {
			this(columnPosition, fieldName, fieldType, translationMsg, codeStoreTypeId, allowSearch, true, 1, true, false, null, null, false);
		}

		public CrudGridColumnSetupModel(final int columnPosition, final String fieldName, final Class<?> fieldType, final String translationMsg, final Integer codeStoreTypeId) {
			this(columnPosition, fieldName, fieldType, translationMsg, codeStoreTypeId, true);
		}

		public CrudGridColumnSetupModel(final int columnPosition, final String fieldName, final Class<?> fieldType, final String translationMsg) {
			this(columnPosition, fieldName, fieldType, translationMsg, null, true);
		}

	}

	@Getter
	@Setter
	private class CrudGridColumnSetupInnerModel {

		private CrudGridColumnSetupModel crudGridColumnSetupModel;
		private HeaderRowSearchComponent<D> headerRowSearchComponent;

		public CrudGridColumnSetupInnerModel(final CrudGridColumnSetupModel crudGridColumnSetupModel, final HeaderRowSearchComponent<D> headerRowSearchComponent) {
			this.crudGridColumnSetupModel = crudGridColumnSetupModel;
			this.headerRowSearchComponent = headerRowSearchComponent;
		}

	}

	/**
	 * fontos, hogy {@link Class#newInstance()} módon példányosítható kell legyen (pl. az add művelet miatt)
	 */
	private final Class<D> modelType;
	private final ToolboxCrudService<D> crudService;
	private final Supplier<CrudFormComponent<D>> crudFormComponentSupplier;

	/**
	 * akarunk-e jobbra-balra lapozást...
	 */
	private final boolean leftRightPaging;

	private int leftRightPagingPageSize;

	/**
	 * ide kerülnek a fő vezérló gombok (frissítés, szerkesztés) stb.
	 */
	private final HorizontalLayout hlHeader;

	private final Button btnDeSelectAll;
	private final Button btnRefresh;
	private final Button btnView;
	private final Button btnAdd;
	private final Button btnUpdate;
	private final Button btnDelete;
	private final Button btnClearFilters;
	private Button btnExport;

	private ProgressBar pbForExport;
	
	private final Grid<D> grid;

	/**
	 * csak leftRight mód esetén
	 */
	private CrudGridPageControlComponent<D> crudGridPageControlComponent;

	/**
	 * ide kerül a lapozó (ha van) stb.
	 */
	private HorizontalLayout hlFooter;

	/**
	 * ebben fogjuk a megjeleníteni a formot
	 *
	 * @see #crudFormComponentSupplier
	 */
	private Window formDialog;

	/**
	 * ez az érdemi {@link DataProvider}
	 */
	private final ToolboxBackEndDataProvider<D> toolboxBackEndDataProvider;

	/**
	 * ez csak egy wrapper/decorator jellegű {@link DataProvider}
	 *
	 * @see #toolboxBackEndDataProvider
	 */
	private PagedDataProvider<D, Object> pagedDataProvider;

	/**
	 * {@link PagedDataProvider}-hez kapcsolódó lapozó (ez csak a vezérlő manager kód, nem a felületi elem!)
	 */
	private PagingControls<D> pagingControls;

	/**
	 * elemenkénti írásvédettséget tesz lehetővé (opcionális), grid elem kijelöléskor hívódik meg
	 */
	private Predicate<D> readOnlyPredicate;

	/**
	 * grid elem kijelöléskor hívódik meg (csak single select mód esetén)
	 */
	private Consumer<D> selectionConsumer;

	/**
	 * grid elem kijelöléskor hívódik meg (csak multi select mód esetén)
	 */
	private Consumer<Set<D>> multiSelectionConsumer;

	/**
	 * elem megnyitás (értsd amikor már form megjelenik) előtt...
	 */
	private Consumer<D> beforeOpenOperator;

	/**
	 * elem mentés (értsd amikor már form bezáródott) előtt (közvetlenül a service save előtt)...
	 */
	private Consumer<D> beforeSaveOperator;

	private String formDialogWidthOverride;

	/**
	 * spec. combobox-ok a fejléc filter részbe
	 */
	private Map<String, ComboBox<Integer>> searchComboMap;

	private List<String> aggregateFieldNameList;

	private Set<SearchCriteria> initialFixedSearchCriteriaSet;

	private CrudFooterAggregate<D> crudFooterAggregate;

	private final UI ui;
	private final ToolboxUserDetails loggedInUser;

	private boolean asyncExport;

	/**
	 * @see #exportSliceSize
	 */
	private int exportAmountLimit = 5000;
	
	/**
	 * megsokszorozza (ha kisebb mint az amount limit) az exportálhat (pl. XLSX) rekordok számát: 
	 * több szeletben, több export fájl képződik és a végén ezek egy ZIP-be leszenek rakva 
	 * 
	 * @see #exportAmountLimit
	 */
	private int exportSliceSize = 1000;

	/**
	 * -1 esetén soha nem lesz background check; 
	 * a CrudGrid attach (addComponent a külső layout-on) előtt kell a setter-t hívni
	 */
	private volatile long markRefreshButtonCheckSeconds = 600;
	
	/**
	 * Boolean mező esetén null érték mivel legyen mutastva (default "(?)"), 
	 * mj. String nem char (tehát ilyen értelemben félrevezető a neve kissé)
	 */
	private String booleanNullChar;

	public CrudGridComponent(final Class<D> modelType, final ToolboxCrudService<D> crudService, final Supplier<CrudFormComponent<D>> crudFormComponentSupplier, final boolean leftRightPaging) {
		this(modelType, crudService, crudFormComponentSupplier, leftRightPaging, null, null, SelectionMode.SINGLE, null, null, null, null);
	}

	public CrudGridComponent(final Class<D> modelType, final ToolboxCrudService<D> crudService, final Supplier<CrudFormComponent<D>> crudFormComponentSupplier, final boolean leftRightPaging, final Set<SearchCriteria> initialFixedSearchCriteriaSet) {
		this(modelType, crudService, crudFormComponentSupplier, leftRightPaging, null, initialFixedSearchCriteriaSet, SelectionMode.SINGLE, null, null, null, null);
	}

	public CrudGridComponent(final Class<D> modelType, final ToolboxCrudService<D> crudService, final Supplier<CrudFormComponent<D>> crudFormComponentSupplier, final boolean leftRightPaging, final Integer leftRightPagingPageSize, final Set<SearchCriteria> initialFixedSearchCriteriaSet) {
		this(modelType, crudService, crudFormComponentSupplier, leftRightPaging, leftRightPagingPageSize, initialFixedSearchCriteriaSet, SelectionMode.SINGLE, null, null, null, null);
	}

	public CrudGridComponent(final Class<D> modelType, final ToolboxCrudService<D> crudService, final Supplier<CrudFormComponent<D>> crudFormComponentSupplier, final boolean leftRightPaging, final Set<SearchCriteria> initialFixedSearchCriteriaSet, final SelectionMode selectionMode) {
		this(modelType, crudService, crudFormComponentSupplier, leftRightPaging, null, initialFixedSearchCriteriaSet, selectionMode, null, null, null, null);
	}

	public CrudGridComponent(final Class<D> modelType, final ToolboxCrudService<D> crudService, final Supplier<CrudFormComponent<D>> crudFormComponentSupplier, final boolean leftRightPaging, final Set<SearchCriteria> initialFixedSearchCriteriaSet, final SelectionMode selectionMode, final List<CrudGridColumnSetupModel> crudGridColumnSetupModelList) {
		this(modelType, crudService, crudFormComponentSupplier, leftRightPaging, null, initialFixedSearchCriteriaSet, selectionMode, crudGridColumnSetupModelList, null, null, null);
	}

	public CrudGridComponent(final Class<D> modelType, final ToolboxCrudService<D> crudService, final Supplier<CrudFormComponent<D>> crudFormComponentSupplier, final boolean leftRightPaging, final Set<SearchCriteria> initialFixedSearchCriteriaSet, final SelectionMode selectionMode, final List<CrudGridColumnSetupModel> crudGridColumnSetupModelList, final Map<String, ComboBox<Integer>> searchComboMap) {
		this(modelType, crudService, crudFormComponentSupplier, leftRightPaging, null, initialFixedSearchCriteriaSet, selectionMode, crudGridColumnSetupModelList, searchComboMap, null, null);
	}

	public CrudGridComponent(final Class<D> modelType, final ToolboxCrudService<D> crudService, final Supplier<CrudFormComponent<D>> crudFormComponentSupplier, final boolean leftRightPaging, final Set<SearchCriteria> initialFixedSearchCriteriaSet, final SelectionMode selectionMode, final List<CrudGridColumnSetupModel> crudGridColumnSetupModelList, final Map<String, ComboBox<Integer>> searchComboMap, final List<String> aggregateFieldNameList) {
		this(modelType, crudService, crudFormComponentSupplier, leftRightPaging, null, initialFixedSearchCriteriaSet, selectionMode, crudGridColumnSetupModelList, searchComboMap, aggregateFieldNameList, null);
	}

	/**
	 * @param modelType
	 * @param crudService
	 * @param crudFormComponentSupplier
	 * @param leftRightPaging
	 * 		akarunk-e jobbra-balra lapozást (false esetén scoll lesz) (fontos, hogy a service hívások mindenkép {@link Pageable} alapúak, ez a csak a vizuális megjelenést állítja!)
	 * @param leftRightPagingPageSize
	 * 		leftRightPaging true esetén mekkora legyen egy oldal (null esetén a default a 10) (alap esetben a this.grid.setHeightByRows is erre van állítva, de azt át lehet állítani)
	 * @param initialFixedSearchCriteriaSet
	 * @param selectionMode
	 * 		jelenleg csak {@link SelectionMode#SINGLE} esetén működnek csak az alap crud műveletek (hozzáadás, delete stb.)
	 * @param crudGridColumnSetupModelList
	 * 		null esetén az annotációkat dolgozza fel...
	 * @param searchComboMap
	 * 		{@link CrudGridColumn#searchComboMapId()} kapcsán {@link Map} (opcionális)
	 * @param aggregateFieldNameList
	 * 		columnId list, ezzel egy alsó, sum/avg/min/max sort lehet betenni, az itt megadott oszlopoknál fog megjelenni (egyelőre experimental)
	 */
	@SuppressWarnings({ "unused" })
	public CrudGridComponent(final Class<D> modelType, final ToolboxCrudService<D> crudService, final Supplier<CrudFormComponent<D>> crudFormComponentSupplier, final boolean leftRightPaging, final Integer leftRightPagingPageSize, final Set<SearchCriteria> initialFixedSearchCriteriaSet, final SelectionMode selectionMode, List<CrudGridColumnSetupModel> crudGridColumnSetupModelList, final Map<String, ComboBox<Integer>> searchComboMap, final List<String> aggregateFieldNameList) {
		this(modelType, crudService, crudFormComponentSupplier, leftRightPaging, leftRightPagingPageSize, initialFixedSearchCriteriaSet, selectionMode, crudGridColumnSetupModelList, searchComboMap, aggregateFieldNameList, null);
	}

	/**
	 * @param modelType
	 * @param crudService
	 * @param crudFormComponentSupplier
	 * @param leftRightPaging
	 * 		akarunk-e jobbra-balra lapozást (false esetén scoll lesz) (fontos, hogy a service hívások mindenkép {@link Pageable} alapúak, ez a csak a vizuális megjelenést állítja!)
	 * @param leftRightPagingPageSize
	 * 		leftRightPaging true esetén mekkora legyen egy oldal (null esetén a default a 10) (alap esetben a this.grid.setHeightByRows is erre van állítva, de azt át lehet állítani)
	 * @param initialFixedSearchCriteriaSet
	 * @param selectionMode
	 * 		jelenleg csak {@link SelectionMode#SINGLE} esetén működnek csak az alap crud műveletek (hozzáadás, delete stb.)
	 * @param crudGridColumnSetupModelList
	 * 		null esetén az annotációkat dolgozza fel...
	 * @param searchComboMap
	 * 		{@link CrudGridColumn#searchComboMapId()} kapcsán {@link Map} (opcionális)
	 * @param aggregateFieldNameList
	 * 		columnId list, ezzel egy alsó, sum/avg/min/max sort lehet betenni, az itt megadott oszlopoknál fog megjelenni (egyelőre experimental)
	 * @param booleanNullChar
	 * 		null Boolean érték miként jelenjken meg a gridben ("?", nothing etc.)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
	public CrudGridComponent(final Class<D> modelType, final ToolboxCrudService<D> crudService, final Supplier<CrudFormComponent<D>> crudFormComponentSupplier, final boolean leftRightPaging, final Integer leftRightPagingPageSize, final Set<SearchCriteria> initialFixedSearchCriteriaSet, final SelectionMode selectionMode, List<CrudGridColumnSetupModel> crudGridColumnSetupModelList, final Map<String, ComboBox<Integer>> searchComboMap, final List<String> aggregateFieldNameList, final String booleanNullChar) {

		this.modelType = modelType;
		this.crudService = crudService;
		this.crudFormComponentSupplier = crudFormComponentSupplier;
		this.leftRightPaging = leftRightPaging;

		// if (leftRightPagingPageSize == null) {
		this.leftRightPagingPageSize = 10;
		// } else {
		// this.leftRightPagingPageSize = leftRightPagingPageSize; // TODO: csak a fix 10 működik jól, tisztázni
		// }

		this.initialFixedSearchCriteriaSet = initialFixedSearchCriteriaSet;

		this.searchComboMap = searchComboMap;
		this.aggregateFieldNameList = aggregateFieldNameList;

		this.ui = UI.getCurrent();
		this.loggedInUser = SecurityUtil.getLoggedInUser();

		this.asyncExport = false;
		
		this.booleanNullChar = booleanNullChar != null ? booleanNullChar : I.trc("Caption", "(?)");

		// ---

		this.setSizeFull();
		this.setMargin(false);
		this.setSpacing(true);

		// ---

		this.hlHeader = new HorizontalLayout();
		this.hlHeader.setSpacing(true);
		this.hlHeader.setWidth("100%");
		this.addComponent(this.hlHeader);

		this.btnView = new Button("", e -> this.viewButtonClicked());
		this.btnView.setIcon(VaadinIcons.EYE);
		this.btnView.setWidth("60px");
		this.hlHeader.addComponent(this.btnView);

		this.btnAdd = new Button("", e -> this.addButtonClicked());
		this.btnAdd.setIcon(VaadinIcons.PLUS);
		this.btnAdd.setWidth("60px");
		this.hlHeader.addComponent(this.btnAdd);

		this.btnUpdate = new Button("", e -> this.updateButtonClicked());
		this.btnUpdate.setIcon(VaadinIcons.PENCIL);
		this.btnUpdate.setWidth("60px");
		this.hlHeader.addComponent(this.btnUpdate);

		this.btnDelete = new Button("", e -> this.deleteButtonClicked());
		this.btnDelete.setIcon(VaadinIcons.TRASH);
		this.btnDelete.setWidth("60px");
		this.hlHeader.addComponent(this.btnDelete);

		if (crudFormComponentSupplier == null || SelectionMode.MULTI.equals(selectionMode)) {

			// ha nincs szerkesztő komponens beadva, akkor "élből" felesleges kitenni a gombokat (teljesen eltűntetjük őket)

			this.btnView.setVisible(false);
			this.btnAdd.setVisible(false);
			this.btnUpdate.setVisible(false);
			this.btnDelete.setVisible(false);

		}

		this.btnRefresh = new Button("", e -> this.refreshButtonClicked());
		this.btnRefresh.setIcon(VaadinIcons.REFRESH);
		this.btnRefresh.setWidth("60px");
		this.hlHeader.addComponent(this.btnRefresh);
		this.hlHeader.setComponentAlignment(this.btnRefresh, Alignment.MIDDLE_RIGHT);
		this.hlHeader.setExpandRatio(this.btnRefresh, 1f);

		this.btnClearFilters = new Button("", e -> this.clearFiltersButtonClicked());
		this.btnClearFilters.setIcon(VaadinIcons.ERASER);
		this.btnClearFilters.setWidth("60px");
		this.hlHeader.addComponent(this.btnClearFilters);
		this.btnClearFilters.setEnabled(false);
		this.hlHeader.setComponentAlignment(this.btnClearFilters, Alignment.MIDDLE_RIGHT);

		this.btnDeSelectAll = new Button("", e -> this.deSelectAllButtonClicked());
		this.btnDeSelectAll.setIcon(VaadinIcons.THIN_SQUARE);
		this.btnDeSelectAll.setWidth("60px");
		this.hlHeader.addComponent(this.btnDeSelectAll);
		this.hlHeader.setComponentAlignment(this.btnDeSelectAll, Alignment.MIDDLE_RIGHT);

		if (!SelectionMode.MULTI.equals(selectionMode)) {
			this.btnDeSelectAll.setVisible(false);
		}

		// ---

		this.grid = new Grid<>(modelType);
		this.grid.setSizeFull();
		this.grid.setSelectionMode(selectionMode);
		this.grid.addSelectionListener(x -> this.selectionChange());
		this.grid.setHeightByRows(this.leftRightPagingPageSize);

		this.addComponent(this.grid);
		this.setExpandRatio(this.grid, 1f);

		this.grid.setColumnReorderingAllowed(true);

		this.hlFooter = new HorizontalLayout();
		this.hlFooter.setSpacing(true);
		this.hlFooter.setWidth("100%");
		this.addComponent(this.hlFooter);

		// ---

		this.toolboxBackEndDataProvider = new ToolboxBackEndDataProvider<>(crudService);

		if (this.aggregateFieldNameList != null && !this.aggregateFieldNameList.isEmpty()) {
			this.crudFooterAggregate = new CrudFooterAggregate(this.grid, this.toolboxBackEndDataProvider, this.aggregateFieldNameList);
			this.toolboxBackEndDataProvider.setCrudFooterAggregate(this.crudFooterAggregate);
		}

		if (initialFixedSearchCriteriaSet != null) {
			this.toolboxBackEndDataProvider.getFixedSearchCriteriaSet().addAll(initialFixedSearchCriteriaSet);
		}

		// ---

		// további FixedSearchCriteriaSet elemek, egy session bean alapján

		final Set<SearchCriteria> sessionFilters = ApplicationContextHelper.getBean(GridFilterSessionBean.class).getAll();

		if (!sessionFilters.isEmpty()) {

			for (final SearchCriteria sessionFilter : sessionFilters) {

				try {

					final Field declaredField = modelType.getDeclaredField(sessionFilter.getFieldName());

					if (declaredField != null) {
						this.toolboxBackEndDataProvider.getFixedSearchCriteriaSet().add(sessionFilter);
					}

				} catch (final NoSuchFieldException e) {
					//
				}

			}
		}

		// ---

		if (this.leftRightPaging) {

			this.pagedDataProvider = new ToolboxPagedDataProvider<>(this.toolboxBackEndDataProvider, this.leftRightPagingPageSize);
			this.setPagingControls(this.pagedDataProvider.getPagingControls());
			this.grid.setDataProvider(this.pagedDataProvider);

			this.crudGridPageControlComponent = new CrudGridPageControlComponent<>(this.pagedDataProvider.getPagingControls());
			this.hlFooter.addComponent(this.crudGridPageControlComponent);
			this.crudGridPageControlComponent.setWidth("100%");

		} else {
			this.grid.setDataProvider(this.toolboxBackEndDataProvider);
		}

		// ---

		this.btnExport = this.buildExportButton();
		this.btnExport.setVisible(false);
		this.hlHeader.addComponent(this.btnExport);
		this.hlHeader.setComponentAlignment(this.btnExport, Alignment.MIDDLE_RIGHT);

		// ---

		this.btnAdd.setDescription(I.trc("Tooltip", "Add"), ContentMode.TEXT);
		this.btnUpdate.setDescription(I.trc("Tooltip", "Modify"), ContentMode.TEXT);
		this.btnDelete.setDescription(I.trc("Tooltip", "Delete"), ContentMode.TEXT);
		this.btnView.setDescription(I.trc("Tooltip", "View details"), ContentMode.TEXT);
		this.btnDeSelectAll.setDescription(I.trc("Tooltip", "Clear selection"), ContentMode.TEXT);
		this.btnClearFilters.setDescription(I.trc("Tooltip", "Clear filters"), ContentMode.TEXT);
		this.btnRefresh.setDescription(I.trc("Tooltip", "Refresh"), ContentMode.TEXT);
		this.btnExport.setDescription(I.trc("Tooltip", "Export"), ContentMode.TEXT);

		// ---

		// itt állítjuk be, az oszlopsorrendet, oszlopfeliratokat, fejléceket stb. (annotáció, reflection stb. alapú)

		this.grid.removeAllColumns();

		{

			if (crudGridColumnSetupModelList == null) {

				crudGridColumnSetupModelList = new ArrayList<>();

				final List<Field> allFields = new ArrayList<>(Arrays.asList(modelType.getDeclaredFields()));

				int countedColumnPosition = 1;

				for (final Field field : allFields) {
					if (field.getDeclaredAnnotations().length > 0) {

						final Annotation[] annotations = field.getDeclaredAnnotations();

						for (final Annotation annotation : annotations) {

							if (annotation.annotationType().equals(CrudGridColumn.class)) {

								final CrudGridColumn cgc = (CrudGridColumn) annotation;

								// log.debug("field.getName(): " + field.getName() + ", cgc.columnPosition(): " + cgc.columnPosition() + ", countedColumnPosition: " + countedColumnPosition);

								crudGridColumnSetupModelList.add(
										new CrudGridColumnSetupModel(
												cgc.columnPosition() != 0 ? cgc.columnPosition() : countedColumnPosition,
												field.getName(),
												field.getType(),
												cgc.translationMsg(),
												(cgc.codeStoreTypeId() == 0 ? null : cgc.codeStoreTypeId()),
												cgc.allowSearch(),
												cgc.allowOrder(),
												cgc.columnExpandRatio(),
												cgc.allowHide(),
												cgc.startHidden(),
												cgc.searchComboMapId(),
												cgc.searchTargetFieldName(), cgc.lazyLoadFilter()));

								countedColumnPosition += 2;
							}
						}

					}
				}

			}

			// ---

			final Triple<String, String, Boolean> interpretedSearchUriFragment = UiHelper.interpreteSearchUriFragment();

			// ---

			// log.debug("millis (6): " + (System.currentTimeMillis() - millis));
			// millis = System.currentTimeMillis();

			final TreeMap<Integer, CrudGridColumnSetupInnerModel> columnSetupInnerModelMap = new TreeMap<>();

			for (final CrudGridColumnSetupModel crudGridColumnSetupModel : crudGridColumnSetupModelList) {

				HeaderRowSearchComponent<D> headerRowSearchComponent = null;

				if (crudGridColumnSetupModel.isAllowSearch()) {
					headerRowSearchComponent = new HeaderRowSearchComponent(
							crudGridColumnSetupModel.getFieldType(),
							crudGridColumnSetupModel.getFieldName(),
							this,
							this.toolboxBackEndDataProvider,
							crudGridColumnSetupModel.getCodeStoreTypeId(),
							crudGridColumnSetupModel.getSearchComboMapId(),
							crudGridColumnSetupModel.getSearchTargetFieldName(),
							interpretedSearchUriFragment);
				}

				// ---

				if (columnSetupInnerModelMap.containsKey(crudGridColumnSetupModel.getColumnPosition())) {
					log.warn("columnSetupInnerModelMap already contains this column position: " + crudGridColumnSetupModel.getColumnPosition() + ", " + columnSetupInnerModelMap.get(crudGridColumnSetupModel.getColumnPosition()).getCrudGridColumnSetupModel().getFieldName() + " vs " + crudGridColumnSetupModel.getFieldName());
				}

				columnSetupInnerModelMap.put(crudGridColumnSetupModel.getColumnPosition(), new CrudGridColumnSetupInnerModel(crudGridColumnSetupModel, headerRowSearchComponent));

			}

			// ---

			// final HeaderRow hrSearch = this.grid.appendHeaderRow();
			final HeaderRow hrSearch = this.grid.getDefaultHeaderRow();

			final int maximumWidth = Math.max(2560 / columnSetupInnerModelMap.values().size(), 500);

			for (final CrudGridColumnSetupInnerModel columnSetupModel : columnSetupInnerModelMap.values()) {

				final CrudGridColumnSetupModel crudGridColumnSetupModel = columnSetupModel.getCrudGridColumnSetupModel();
				final HeaderRowSearchComponent<D> headerRowSearchComponent = columnSetupModel.getHeaderRowSearchComponent();

				Column<D, ?> column = null;

				if (Boolean.class.equals(crudGridColumnSetupModel.getFieldType())) {

					final String strTrue = I.trc("Caption", "yes");
					final String strFalse = I.trc("Caption", "no");
					final String strNull = getBooleanNullChar();

					final ConverterRenderer renderer = new ConverterRenderer(new Converter<String, Boolean>() {

						@Override
						public Result<Boolean> convertToModel(final String value, final ValueContext context) {
							return Result.ok(Boolean.parseBoolean(value));
						}

						@Override
						public String convertToPresentation(final Boolean value, final ValueContext context) {

							if (value == null) {
								// return VaadinIcons.QUESTION.getHtml();
								return strNull;
							}

							if (value) {
								// return VaadinIcons.CHECK.getHtml();
								return strTrue;
							}

							// return VaadinIcons.CLOSE.getHtml();
							return strFalse;

						}
					});

					column = this.grid.addColumn(crudGridColumnSetupModel.getFieldName(), renderer);

				} else if (Timestamp.class.equals(crudGridColumnSetupModel.getFieldType())) {

					column = this.grid.addColumn(crudGridColumnSetupModel.getFieldName());

					column.setRenderer(ts -> {
						return UiHelper.adjustToPageTimeZone((java.sql.Timestamp) ts);
					}, new LocalDateTimeRenderer());

				} else if (BigDecimal.class.equals(crudGridColumnSetupModel.getFieldType())) {

					column = this.grid.addColumn(crudGridColumnSetupModel.getFieldName());
					column.setRenderer(bd -> (BigDecimal) bd, new NumberRenderer(UiHelper.getCurrentUiDecimalFormat()));

				} else {

					column = this.grid.addColumn(crudGridColumnSetupModel.getFieldName());

				}

				column.setCaption(I.tr(crudGridColumnSetupModel.getTranslationMsg())); // speciális, itt kívételesen I.tr kell, mert nem akarjuk, hogy megtalálja a kereső (de a CrudGridColumnSetupModel annotációkat igen!)

				column.setExpandRatio(crudGridColumnSetupModel.getColumnExpandRatio());
				column.setMaximumWidth(maximumWidth);
				column.setMinimumWidth(150d);
				column.setMinimumWidthFromContent(true); // ez a default

				column.setSortable(crudGridColumnSetupModel.isAllowOrder());

				if (crudGridColumnSetupModel.isAllowHide()) {
					column.setHidable(true);

					if (interpretedSearchUriFragment != null && crudGridColumnSetupModel.getFieldName().equals(interpretedSearchUriFragment.getLeft())) {
						column.setHidden(false);
					} else {
						column.setHidden(crudGridColumnSetupModel.isStartHidden());
					}

				}

				// ---

				if (headerRowSearchComponent != null) {

					headerRowSearchComponent.setMargin(true);
					headerRowSearchComponent.setHeight(58, Unit.PIXELS);
					headerRowSearchComponent.setWidth(null);

					final PopupView pv = new PopupView(column.getCaption(), headerRowSearchComponent);
					pv.setData(pv.getContent().getMinimizedValueAsHTML());
					pv.setHideOnMouseOut(false);
					pv.addStyleName("text-ellipsis");
					pv.setWidth(100, Unit.PERCENTAGE);
					pv.setDescription(I.trc("Tooltip", "Filter") + ": " + crudGridColumnSetupModel.getTranslationMsg());

					if (headerRowSearchComponent.getBtnReset() != null && headerRowSearchComponent.getBtnReset().isEnabled()) {
						pv.addStyleName("w-filtered");
					}

					// ---

					{

						// ez egy elég csúnya megoldás
						// ha dialogban vagyunk, akkor nem szabad engedni, hogy az uriFragment leszűrje a gridet
						// ezt nem tudtam megoldani, csak azt, hogy itt kiszedjük a szűrőt

						if (headerRowSearchComponent.getBtnReset() != null) { // ha van már reset button, az azt jelenti, hogy van aktív szűrő (tehát "játékban" van az uriFragment)

							pv.addAttachListener(x -> {

								final boolean isInWindow = UiHelper.isInWindow(this);

								// log.debug("isInWindow.... " + isInWindow);

								if (isInWindow) {
									headerRowSearchComponent.getBtnReset().click();
								}
							});

						}
					}

					// ---

					pv.addPopupVisibilityListener(x -> {

						headerRowSearchComponent.init();

						if (headerRowSearchComponent.getComponentCount() > 0) {

							final Component component = headerRowSearchComponent.getComponent(0);
							if (component instanceof Component.Focusable) {
								((Component.Focusable) component).focus();
							}

						}

					});

					hrSearch.getCell(column.getId()).setComponent(pv);
				}

			}

			if (hrSearch.getComponents().isEmpty()) {
				this.grid.removeHeaderRow(hrSearch);
			}

		}

		// ---

		this.selectionChange();

	}

	public void addAdditionalHeaderToolbar(final HorizontalLayout hlToolbar) {
		this.addAdditionalHeaderToolbar(hlToolbar, false);
	}

	public void addAdditionalHeaderToolbar(final HorizontalLayout hlToolbar, final boolean addSeparatorInFront) {

		if (addSeparatorInFront) {

			final Label lblSep = new Label("|");
			lblSep.addStyleName(ValoTheme.LABEL_LARGE);
			lblSep.addStyleName(ValoTheme.LABEL_LIGHT);

			this.hlHeader.addComponent(lblSep, this.hlHeader.getComponentIndex(this.btnDelete) + 1);
			this.hlHeader.addComponent(hlToolbar, this.hlHeader.getComponentIndex(lblSep) + 1);

			this.hlHeader.setComponentAlignment(lblSep, Alignment.MIDDLE_LEFT);
			this.hlHeader.setComponentAlignment(hlToolbar, Alignment.MIDDLE_LEFT);

		} else {
			this.hlHeader.addComponent(hlToolbar, this.hlHeader.getComponentIndex(this.btnDelete) + 1);
			this.hlHeader.setComponentAlignment(hlToolbar, Alignment.MIDDLE_LEFT);
		}

		final Iterator<Component> it = this.hlHeader.iterator();
		while (it.hasNext()) {
			this.hlHeader.setExpandRatio(it.next(), 0f);
		}

		this.hlHeader.setExpandRatio(hlToolbar, 1f);
	}

	/**
	 * csak a leftRight paging-el kombinálva működik
	 *
	 * @param hlToolbar
	 */
	public void addAdditionalFooterToolbar(final HorizontalLayout hlToolbar) {

		if (this.leftRightPaging) {
			this.hlFooter.setExpandRatio(this.crudGridPageControlComponent, 0f);
			this.crudGridPageControlComponent.setWidth("440px");
		}

		this.hlFooter.addComponentAsFirst(hlToolbar);
		this.hlFooter.setExpandRatio(hlToolbar, 1f);
	}

	public void refreshGridWithAfterSelect(final D selectAfterClear) {

		this.refreshGrid();

		this.grid.asSingleSelect().clear();
		this.grid.asSingleSelect().select(selectAfterClear); // igazából a select serValue szerűen működik (ami zavaros, de ez van), tehát nem csak kiválasztja az azonos hashCode/equals elemet, hanem konkrétan ez lesz

	}

	public void refreshGridWithAfterSelectAuto() {

		final Set<Integer> selectedItemIds = this.getSelectedItemIds();

		this.refreshGrid();

		this.grid.asSingleSelect().clear();

		try {

			if (!selectedItemIds.isEmpty() && this.grid.getSelectionModel() instanceof SingleSelectionModel) {

				this.grid.asSingleSelect().select(this.crudService.findOne(selectedItemIds.iterator().next())); // igazából a select serValue szerűen működik (ami zavaros, de ez van), tehát nem csak kiválasztja az azonos hashCode/equals elemet, hanem konkrétan ez lesz
			}
		} catch (final Exception e) {
			//
		}

	}

	public void refreshGrid() {

		this.btnRefresh.setDescription(I.trc("Tooltip", "Refresh"), ContentMode.TEXT);
		// this.btnRefresh.removeStyleName(ValoTheme.BUTTON_FRIENDLY);

		// ---

		if (this.leftRightPaging) {

			this.toolboxBackEndDataProvider.refreshAll();
			this.pagingControls.setPageNumber(0);
			this.crudGridPageControlComponent.refreshControlsAndLabels();

		} else {
			this.toolboxBackEndDataProvider.refreshAll();
		}

		if (this.selectionConsumer != null) {
			this.selectionConsumer.accept(null);
		}

		final int userFilterCount = this.toolboxBackEndDataProvider.getSearchCriteriaSet().size(); // csak a kézi/user beírt/látható, a "fixed"-ek nem számítanak itt (azokat nem is lehet törölni úgysem)

		if (userFilterCount > 0) {
			this.btnClearFilters.setEnabled(true);
			this.btnClearFilters.setDescription(I.trc("Tooltip", "Clear filters") + " (" + I.trc("Tooltip", "currently active filters") + ": " + userFilterCount + ")");
		} else {
			this.btnClearFilters.setEnabled(false);
			this.btnClearFilters.setDescription(I.trc("Tooltip", "Clear filters"));
		}
	}

	private Button buildExportButton() {

		final Button btn = new Button("", e -> {

			final Window waitDialog = new Window(!this.asyncExport ? I.trc("Title", "Export") : I.trc("Title", "Export (async)"));
			waitDialog.setModal(true);
			waitDialog.setResizable(false);
			waitDialog.setClosable(false);

			waitDialog.setWidth("300px");
			waitDialog.setHeight(null);

			final VerticalLayout vlWaitDialog = new VerticalLayout();
			vlWaitDialog.setWidth("100%");
			vlWaitDialog.setHeight(null);
			waitDialog.setContent(vlWaitDialog);

			// ---

			final ComboBox<Integer> cmbExportType = UiHelper.buildCombo1(I.trc("Caption", "Export type"), ApplicationContextHelper.getBean(CodeStoreItemService.class).findAllByType(ToolboxSysKeys.ExportType.CODE_STORE_TYPE_ID), CodeStoreItem::getCaptionCaption);
			cmbExportType.setValue(ToolboxSysKeys.ExportType.XLSX);
			cmbExportType.setEmptySelectionAllowed(false);
			cmbExportType.setWidth("100%");
			vlWaitDialog.addComponent(cmbExportType);

			final Button btnStartExport = new Button(I.trc("Button", "Export!"));
			btnStartExport.setWidth("100%");
			btnStartExport.setDisableOnClick(true);

			vlWaitDialog.addComponent(btnStartExport);

			final Button btnClose = new Button(I.trc("Button", "Close"));
			btnClose.setWidth("100%");
			btnClose.setDisableOnClick(true);
			btnClose.addClickListener(y -> {
				waitDialog.close();
			});

			vlWaitDialog.addComponent(btnClose);

			btnStartExport.addClickListener(exp -> {

				if (!checkExportLimit()) {
					return;
				}
				
				// ---
				
				final ExporterMananger exporterMananger = ApplicationContextHelper.getBean(ExporterMananger.class);
				
				if (!this.asyncExport) {
					
					// sync export

					FileDescriptor fileDescriptor;

					try {

						List<D> list = this.toolboxBackEndDataProvider.fetchFromBackEndLastQueryAll();		
						fileDescriptor = btnExportClickInner(cmbExportType, exporterMananger, list);
							
					} catch (final Exception ex) {
						throw new ExporterException("Exporter exception", ex);
					}

					waitDialog.close();

					JavaScript.eval(FileStoreHelper.generateJsDownloadScript(fileDescriptor.getId()));

				} else {
					
					// async export

					final long currentTimeMillisWhenStarted = System.currentTimeMillis();

					btnClose.setVisible(false);
					cmbExportType.setVisible(false);
					btnStartExport.setVisible(false);

					pbForExport = new ProgressBar();
					pbForExport.setIndeterminate(true);
					pbForExport.setWidth(null);
					pbForExport.setHeight(null);

					vlWaitDialog.addComponent(pbForExport);
					vlWaitDialog.setComponentAlignment(pbForExport, Alignment.MIDDLE_CENTER);

					final Button btnCancel = new Button(I.trc("Button", "Cancel"));
					btnCancel.setDisableOnClick(true);
					btnCancel.setWidth("100%");
					vlWaitDialog.addComponent(btnCancel);

					// ---

					final ScheduledFuture<Void> future = AbstractToolboxUI.scheduleRunnable(() -> {

						final boolean b = SecurityUtil.hasLoggedInUser();

						FileDescriptor fileDescriptor = null;
						Exception exForUi = null;

						try {

							if (!b) {

								// úgy a ui.access kapcsán csak akkor kell kézzel beléptetni (és kiléptetni) a user-t, ha nincs...
								// (az igazi gond nem a beléptetés, hanem a finally block-ban a clear)

								SecurityUtil.setUser(this.loggedInUser);
							}
							
							try {
								
								List<D> list = this.toolboxBackEndDataProvider.fetchFromBackEndLastQueryAll();
								fileDescriptor = btnExportClickInner(cmbExportType, exporterMananger, list);
								
							} catch (final Exception ex) {
								log.error("Exporter exception (async)", ex);
								exForUi = ex;
							}

						} finally {
							if (!b) {
								SecurityUtil.clearAuthentication();
							}
						}

						try {
							Thread.sleep(Math.max(0, 120 - (System.currentTimeMillis() - currentTimeMillisWhenStarted))); // szándékos várakozás (azért, hogy legalább egy pillanatra látható legyen a dialog és a progress anim)
						} catch (final InterruptedException ex2) {
							// ide inkább ne legyen Thread.currentThread().interrupt();
						}

						final FileDescriptor fileDescriptorFinal = fileDescriptor;
						final Exception exForUiFinal = exForUi;

						this.ui.access(() -> {

							btnCancel.setEnabled(false);
							waitDialog.close();

							if (exForUiFinal != null) {
								
								if ((exForUiFinal instanceof ToolboxException) && (exForUiFinal instanceof RuntimeException)) {
									throw (RuntimeException) exForUiFinal;
								} else {
									throw new ExporterException("Exporter exception (async)", exForUiFinal);
								}
								
							}

							JavaScript.eval(FileStoreHelper.generateJsDownloadScript(fileDescriptorFinal.getId()));

						});

						return null;

					}, 1, TimeUnit.SECONDS);

					AbstractToolboxUI.scheduleSecondaryRunnable(() -> {

						future.cancel(true);
						AbstractToolboxUI.purgeScheduledExecutorService();

						log.error("CrudGridComponent export timeout!");

						this.ui.access(() -> {
							waitDialog.close();
							this.ui.showNotification(I.trc("Notification", "Timeout!"), Notification.Type.ERROR_MESSAGE);
						});

						return null;

					}, 60, TimeUnit.MINUTES);

					btnCancel.addClickListener(x -> {

						future.cancel(true);
						AbstractToolboxUI.purgeScheduledExecutorService();

						waitDialog.close();
						log.debug("Exporter cancel (cancel button click)!");

					});

					vlWaitDialog.addComponent(btnCancel);
					vlWaitDialog.setComponentAlignment(btnCancel, Alignment.MIDDLE_CENTER);

				}

			});

			this.ui.addWindow(waitDialog);
		});

		btn.setIcon(VaadinIcons.FILE_TABLE);
		btn.setWidth("60px");

		return btn;

	}

	private FileDescriptor btnExportClickInner(final ComboBox<Integer> cmbExportType, final ExporterMananger exporterMananger, List<D> list) { // TODO: ExporterManager-be kellene
		
		if (list.size() <= this.exportSliceSize) {
			
			return exporterMananger.export(
					list,
					this.modelType,
					cmbExportType.getValue()).getLeft();
			
		} else {
			
			List<List<D>> partitionLists = ListUtils.partition(list, this.exportSliceSize);
			
			// ---
			
			log.debug("Export will be sliced (slice size: " + this.exportSliceSize + ", slice count: " + partitionLists.size() + ")!");

			ui.access(() -> {
				
				ui.showNotification(I.trc("Notification", "Sliced export (because of large item count)")
						+ " (" + I.trc("Notification", "records per slice") + ": " + this.exportSliceSize + ","
						+ " " + I.trc("Notification", "slice count") + ": " + partitionLists.size() + ")!",
						Notification.TYPE_TRAY_NOTIFICATION);
				
				if (pbForExport != null) {
					pbForExport.setValue(0);
					pbForExport.setIndeterminate(false);
					pbForExport.setWidth("100%");
				}
				
			});

			// ---
			
			List<FileDescriptor> fileDescriptorXlsxList = new ArrayList<>();
			
			int filenameSuffixNumber = 1;
			
			String commonFilenameToUse = null; // a fájlnév eleje közös lesz, sőt a .zip neve is ez lesz (csak .zip)
			
			for (List<D> partitionList : partitionLists) {
				
				FileDescriptor fd = exporterMananger.export(
						partitionList,
						this.modelType,
						cmbExportType.getValue()).getLeft();
				
				if (filenameSuffixNumber == 1) {
					commonFilenameToUse = FilenameUtils.getBaseName(fd.getFilename());
				}
				
				fd.setFilename(commonFilenameToUse  + "-" + filenameSuffixNumber + "." + FilenameUtils.getExtension(fd.getFilename()));
				ApplicationContextHelper.getBean(FileDescriptorJdbcRepository.class).save(fd); // itt kívtelesen direktben a repo-hoz nyúlunk (ez szándékos)
				
				fileDescriptorXlsxList.add(fd);
				
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					//
				}
				
				float pbPercent = new Float(filenameSuffixNumber).floatValue() /  new Float(partitionLists.size()).floatValue() ;
				
				ui.access(() -> {
					if (pbForExport != null) {
						pbForExport.setValue(pbPercent);
					}
				});
				
				filenameSuffixNumber++;
			}
										
			return FileStoreHelper.createZipFd(fileDescriptorXlsxList, null, commonFilenameToUse + ".zip", 
					ToolboxSysKeys.FileDescriptorLocationType.PROTECTED_FOLDER, ToolboxSysKeys.FileDescriptorSecurityType.ADMIN_OR_CREATOR, 
					false);
			
		}
		

	}

	/**
	 * @param sizeInBackEndLastQuery
	 * @return
	 * 		true, ha átmenet a check-en (ha nincs gond)
	 */
	private boolean checkExportLimit() { // TODO: ExporterManager-be kellene

		final int sizeInBackEndLastQuery = this.toolboxBackEndDataProvider.sizeInBackEndLastQuery();
		
		if (sizeInBackEndLastQuery > this.exportAmountLimit) {

			log.warn("Export amount limit violation (limit: " + this.exportAmountLimit + ", actual: " + sizeInBackEndLastQuery + ")!");

			ui.showNotification(I.trc("Notification", "Export amount limit violation")
					+ " (" + I.trc("Notification", "limit") + ": " + this.exportAmountLimit + ","
					+ " " + I.trc("Notification", "actual") + ": " + sizeInBackEndLastQuery + ")!",
					Notification.TYPE_ERROR_MESSAGE);

			return false;
		}

		return true;

	}

	@Override
	public void attach() {
		super.attach();

		// ---

		this.scheduleMarkRefreshButtonCheck();
	}

	/**
	 * ebben a metódusban kapcsolhatoak ki az alap gombok a grid felett
	 *
	 * @param viewVisible
	 * @param addVisible
	 * @param updateVisible
	 * @param deleteVisible
	 * @param deSelectAllVisible
	 * @param refreshAllVisible
	 * @param clearFilterVisible
	 * @param exportVisible
	 * @see #toggleEnableButton(boolean, boolean, boolean, boolean)
	 * @see #toggleEnableButton(boolean, boolean, boolean, boolean, boolean, boolean, boolean, boolean)
	 */
	public void toggleButtonVisibility(final boolean viewVisible, final boolean addVisible, final boolean updateVisible, final boolean deleteVisible, final boolean deSelectAllVisible, final boolean refreshAllVisible, final boolean clearFilterVisible, final boolean exportVisible) {

		this.btnView.setVisible(viewVisible);
		this.btnAdd.setVisible(addVisible);
		this.btnUpdate.setVisible(updateVisible);
		this.btnDelete.setVisible(deleteVisible);

		this.btnDeSelectAll.setVisible(deSelectAllVisible);
		this.btnRefresh.setVisible(refreshAllVisible);
		this.btnClearFilters.setVisible(clearFilterVisible);
		this.btnExport.setVisible(exportVisible);

	}

	/**
	 * ez csak a 4 alap műveletet állítja
	 *
	 * @param viewEnable
	 * @param addEnable
	 * @param updateEnable
	 * @param deleteEnable
	 * @see #toggleEnableButton(boolean, boolean, boolean, boolean, boolean, boolean, boolean, boolean)
	 * @see #toggleButtonVisibility(boolean, boolean, boolean, boolean, boolean, boolean, boolean, boolean)
	 */
	public void toggleEnableButton(final boolean viewEnable, final boolean addEnable, final boolean updateEnable, final boolean deleteEnable) {

		this.btnView.setEnabled(viewEnable);
		this.btnAdd.setEnabled(addEnable);
		this.btnUpdate.setEnabled(updateEnable);
		this.btnDelete.setEnabled(deleteEnable);

	}

	/**
	 * @param viewEnable
	 * @param addEnable
	 * @param updateEnable
	 * @param deleteEnable
	 * @param deSelectAllEnable
	 * @param refreshAllEnable
	 * @param clearFilterEnable
	 * @param exportEnable
	 * @see #toggleEnableButton(boolean, boolean, boolean, boolean)
	 * @see #toggleButtonVisibility(boolean, boolean, boolean, boolean, boolean, boolean, boolean, boolean)
	 */
	public void toggleEnableButton(final boolean viewEnable, final boolean addEnable, final boolean updateEnable, final boolean deleteEnable, final boolean deSelectAllEnable, final boolean refreshAllEnable, final boolean clearFilterEnable, final boolean exportEnable) {

		this.btnView.setEnabled(viewEnable);
		this.btnAdd.setEnabled(addEnable);
		this.btnUpdate.setEnabled(updateEnable);
		this.btnDelete.setEnabled(deleteEnable);

		this.btnDeSelectAll.setEnabled(deSelectAllEnable);
		this.btnRefresh.setEnabled(refreshAllEnable);
		this.btnClearFilters.setEnabled(clearFilterEnable);
		this.btnExport.setEnabled(exportEnable);

	}

	private void addButtonClicked() {

		try {

			final D domainObject = this.modelType.newInstance();

			this.showWindow(ToolboxSysKeys.CrudOperation.ADD, domainObject, v -> {

				this.formDialog.close();

				ToolboxAssert.isNull(domainObject.getId());

				if (this.beforeSaveOperator != null) {
					this.beforeSaveOperator.accept(domainObject);
				}

				final D domainObjectAfterSave = this.crudService.save(domainObject);

				if (this.crudService instanceof LazyEnhanceCrudService) {
					((LazyEnhanceCrudService<D>) this.crudService).enhance(domainObjectAfterSave);
				}

				// ---

				// módszer 1:
				// this.refreshGrid();

				// ---

				// módszer 2:

				// TODO: talán működik jól, de meg kell beszélni

				if (this.grid.getColumn("id") != null) {

					this.grid.setSortOrder(new GridSortOrderBuilder<D>().thenAsc(this.grid.getColumn("id")));
					this.refreshGrid();
					this.clearFiltersButtonClicked();

					if (this.leftRightPaging) {
						this.pagingControls.setPageNumber(this.pagingControls.getPageCount() - 1);
						this.crudGridPageControlComponent.refreshControlsAndLabels();
					}

					this.grid.asSingleSelect().select(domainObjectAfterSave);

				} else {
					this.refreshGrid();
				}

			});

		} catch (InstantiationException | IllegalAccessException e) {
			throw new ToolboxGeneralException("CrudGrid error!", e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void clearFiltersButtonClicked() {

		this.toolboxBackEndDataProvider.getSearchCriteriaSet().clear();

		final HeaderRow hrSearch = this.grid.getDefaultHeaderRow();

		for (final Component component : hrSearch.getComponents()) {

			final PopupView pv = (PopupView) component;

			pv.setPopupVisible(false);
			pv.removeStyleName("w-filtered");

			final HeaderRowSearchComponent headerRowSearchComponent = (HeaderRowSearchComponent) (pv.getContent().getPopupComponent());
			headerRowSearchComponent.setFreezeSearch(true);

			final Iterator<Component> componentIterator = headerRowSearchComponent.getComponentIterator();

			while (componentIterator.hasNext()) {

				final Component component2 = componentIterator.next();

				if (component2 instanceof HasValue) {
					final HasValue field = (HasValue) component2;
					field.setValue(field.getEmptyValue());
				}

			}

			headerRowSearchComponent.setFreezeSearch(false);

		}

		// ---

		this.grid.deselectAll();
		this.refreshGrid();
	}

	private void selectionChange() {

		// ha nem SingleSelection a grid, akkor

		final boolean isSingleSelect = (this.grid.getSelectionModel() instanceof SingleSelectionModel);

		final boolean rowSelected = isSingleSelect && !this.grid.asSingleSelect().isEmpty();

		if (rowSelected) {

			this.btnView.setEnabled(true);

			final D selectedObject = this.grid.getSelectedItems().iterator().next();

			boolean isReadOnlyElement = false;

			if (this.readOnlyPredicate != null) {
				isReadOnlyElement = this.readOnlyPredicate.test(selectedObject);
			}

			this.btnUpdate.setEnabled(!isReadOnlyElement);
			this.btnDelete.setEnabled(!isReadOnlyElement);

			if (this.selectionConsumer != null) {
				this.selectionConsumer.accept(selectedObject);
			}

		} else {

			this.btnView.setEnabled(false);
			this.btnUpdate.setEnabled(false);
			this.btnDelete.setEnabled(false);

			if (this.selectionConsumer != null) {
				this.selectionConsumer.accept(null);
			}

		}

		// ---

		final boolean isMultiSelect = (this.grid.getSelectionModel() instanceof MultiSelectionModel);

		if (isMultiSelect && this.multiSelectionConsumer != null) {

			this.multiSelectionConsumer.accept(this.grid.getSelectedItems());

		}

	}

	/**
	 * műküdik single és multi select esetén (single estén ritkán kellhet csak, akkor egy egyelemű {@link Set}-et ad vissza)
	 *
	 * @return ha semmi nincs kijeleölve, vagy tiltva van a kijelölés, akkor üres {@link Set}
	 */
	@SuppressWarnings("unchecked")
	public Set<D> getSelectedItems() {

		final boolean isSingleSelect = (this.grid.getSelectionModel() instanceof SingleSelectionModel);
		final boolean isMultiSelect = (this.grid.getSelectionModel() instanceof MultiSelectionModel);

		if (isMultiSelect) {
			return this.grid.asMultiSelect().getValue();
		} else if (isSingleSelect) {

			final D value = this.grid.asSingleSelect().getValue();

			if (value != null) {
				return Sets.newHashSet(value);
			}

		}

		return Sets.newHashSet();

	}

	/**
	 * műküdik single és multi select esetén (single estén ritkán kellhet csak, akkor egy egyelemű {@link Set}-et ad vissza)
	 *
	 * @return ha semmi nincs kijeleölve, vagy tiltva van a kijelölés, akkor üres {@link Set}
	 */
	public Set<Integer> getSelectedItemIds() {

		final Set<D> selectedItems = this.getSelectedItems();

		final Set<Integer> selectedItemIds = new HashSet<>();

		for (final D d : selectedItems) {
			selectedItemIds.add(d.getId());
		}

		return selectedItemIds;

	}

	/**
	 * csak multi select esetén
	 */
	public void setSelectedItemsWithIds(final Set<Integer> itemIds) {

		ToolboxAssert.isTrue(this.grid.getSelectionModel() instanceof MultiSelectionModel);

		this.grid.deselectAll();

		if (itemIds != null) {
			final Set<D> selectedItems = new HashSet<>();

			for (final Integer id : itemIds) {
				selectedItems.add(this.crudService.findOne(id)); // TODO: tisztázni
			}

			this.grid.asMultiSelect().setValue(selectedItems);
		}

	}

	/**
	 * csak single select esetén
	 *
	 * @param itemId
	 */
	public void setSelectedItemWithId(final Integer itemId) {

		ToolboxAssert.isTrue(this.grid.getSelectionModel() instanceof SingleSelectionModel);

		this.grid.deselectAll();

		if (itemId != null) {
			this.grid.asSingleSelect().setValue(this.crudService.findOne(itemId));
		}

	}

	public void showWindow(final ToolboxSysKeys.CrudOperation operation, final D domainObject, final Consumer<D> crudAction) {

		if (domainObject == null) {
			return;
		}

		ToolboxAssert.notNull(operation);

		if (this.beforeOpenOperator != null) {
			this.beforeOpenOperator.accept(domainObject);
		}

		final CrudFormComponent<D> crudFormComponent = this.crudFormComponentSupplier.get();

		crudFormComponent.setDomainObject(domainObject);
		crudFormComponent.setCrudOperation(operation);
		crudFormComponent.setCrudAction(crudAction);
		crudFormComponent.init();

		// ---

		final String strWindowCaption;

		switch (operation) {
		case ADD:
			strWindowCaption = I.trc("Caption", "Add");
			break;
		case UPDATE:
			strWindowCaption = I.trc("Caption", "Edit") + " (#" + domainObject.getId() + ")";
			break;
		case DELETE:
			strWindowCaption = I.trc("Caption", "Delete it?") + " (#" + domainObject.getId() + ")";
			break;
		case READ:
			strWindowCaption = I.trc("Caption", "View") + " (#" + domainObject.getId() + ")";
			break;
		default:
			throw new ToolboxGeneralException("Unknown CRUD operation!");
		}

		final VerticalLayout vlWindow = new VerticalLayout(crudFormComponent);
		vlWindow.setWidth("100%");
		vlWindow.setMargin(false);
		vlWindow.setSpacing(false);

		// ---

		final Button mainButton = crudFormComponent.getMainButton();

		if (mainButton != null) {

			((AbstractLayout) crudFormComponent).removeComponent(mainButton);

			final HorizontalLayout hlButtons = new HorizontalLayout();
			hlButtons.setWidth("100%");
			hlButtons.setMargin(true);

			hlButtons.addComponent(mainButton);
			hlButtons.setComponentAlignment(mainButton, Alignment.MIDDLE_LEFT);

			mainButton.addStyleName("min-width-150px");
			mainButton.addStyleName("max-width-400px");

			mainButton.setWidth(null);

			vlWindow.addComponent(hlButtons);
		}

		this.formDialog = new Window(strWindowCaption, vlWindow);

		this.formDialog.setWidth(this.formDialogWidthOverride != null ? this.formDialogWidthOverride : "650px");
		// this.formDialog.setModal(operation != CrudOperation.READ);
		this.formDialog.setModal(true);

		UI.getCurrent().addWindow(this.formDialog);
	}

	public void refreshItemWithCrudService(final D item) {

		if (item == null) {
			return;
		}

		final D itemFromService = this.crudService.findOne(item.getId());

		if (this.crudService instanceof LazyEnhanceCrudService) {
			((LazyEnhanceCrudService<D>) this.crudService).enhance(itemFromService);
		}

		this.grid.getDataProvider().refreshItem(itemFromService);
	}

	public void refreshSelectedItemWithCrudService() {
		final D item = this.grid.asSingleSelect().getValue();
		this.refreshItemWithCrudService(item);
	}

	private void updateButtonClicked() {

		this.refreshSelectedItemWithCrudService();

		// ---

		final D gridObject = this.grid.asSingleSelect().getValue();

		// itt csinálunk egy másolatot, hogy az ablakban az legyen manipulálva (függetlenül), lásd még save után
		
		// úgy néz ki, hogy a ModelMapper nem rendes deep copy-t csinál, ezért nem az a library van már használva
		// a BeanUtils.copyProperties szintén shallow
		
		// final D domainObject = (D) JacksonHelper.deepCopy(gridObject, this.modelType); // mi van, ha @JsonIgnore van a model osztályon? (ezért inkább DB-
		
		final D domainObject;
		
		if (this.crudService instanceof LazyEnhanceCrudService) {
			D d = this.crudService.findOne(gridObject.getId());
			domainObject = ((LazyEnhanceCrudService<D>) this.crudService).enhance(d);
		} else {
			domainObject = this.crudService.findOne(gridObject.getId());
		}
		
		this.showWindow(ToolboxSysKeys.CrudOperation.UPDATE, domainObject, v -> {

			this.formDialog.close();

			ToolboxAssert.notNull(domainObject.getId());

			if (this.beforeSaveOperator != null) {
				this.beforeSaveOperator.accept(domainObject);
			}

			// módszer 2b miatt kell save előtt

			this.markRefreshButtonCheck(); // ezt azért tesszük ide, hogy még lehetőleg értesüljün arról, ha másik szálon épp most módosított valaki

			// ---

			final D afterUpdateObject = this.crudService.save(domainObject);

			// módszer 1 ---
			// baj: a markRefeshButton aktiválódik később (tehát az az ellenőrzés azt hiszi, hogy más módosította)
			// baj: leszűrés nem frissül (ez talán jó inkább)
			// baj: kell egy oda-vissza kattintás

			// try {
			// // sikeres mentés után visszírunk mindent az eredeti objektumba is, azért, hogy a grid is frissüljön
			// // (itt nem annyira lényeges a deep copy, mert az adott afterUpdateObject nem lesz többet használva már, köv. "körben" új lesz úgyis)
			// BeanUtils.copyProperties(gridObject, afterUpdateObject);
			// } catch (IllegalAccessException | InvocationTargetException e) {
			// throw new ToolboxGeneralException("BeanUtils error!", e);
			// }
			//
			// Set<D> selectedItems = this.grid.getSelectedItems();
			// this.grid.deselectAll();
			// if (!selectedItems.isEmpty()) {
			// this.grid.select(selectedItems.iterator().next());
			// }

			// módszer 2 ---
			// baj: a markRefeshButton aktiválódik később (tehát az az ellenőrzés azt hiszi, hogy más módosította)
			// baj: leszűrés nem frissül (ez talán jó)

			// this.refreshSelectedItemWithCrudService();

			// módszer 2b ---
			// baj: leszűrés nem frissül (ez talán jó?)

			this.toolboxBackEndDataProvider.overrideBackEndModifiedDate(afterUpdateObject.getModifiedOn()); // elvben, ha épp másik szálon is módosítás volt, akkor itt van egy kis esély arra, hogy azt nem vesszük észre
			this.refreshSelectedItemWithCrudService();

			if (this.selectionConsumer != null) {
				this.selectionConsumer.accept(afterUpdateObject);
			}

			// módszer 3 ---
			// baj: durva és elveszik a kijelölés

			// this.grid.deselectAll();
			// this.refreshGrid();

			// ---

			// ha nincs refreshGrid hívás, akkor kellhetnek ezek:

			if (this.crudFooterAggregate != null) {
				this.crudFooterAggregate.refreshAggregateCells();
			}

		});

	}

	private void viewButtonClicked() {

		this.refreshSelectedItemWithCrudService();

		// ---

		final D gridObject = this.grid.asSingleSelect().getValue();

		this.showWindow(ToolboxSysKeys.CrudOperation.READ, gridObject, v -> {

			// nincs itt jelentősége READ módban

		});

	}

	private void deleteButtonClicked() {

		this.refreshSelectedItemWithCrudService();

		// ---

		// itt is a view-hoz hasonlóan read-only módon megmutatjuk az adatok (így mégegyszer megnézheti, hogy biztosan akarja-e törölni)

		final D gridObject = this.grid.asSingleSelect().getValue();

		this.showWindow(ToolboxSysKeys.CrudOperation.DELETE, gridObject, v -> {

			this.formDialog.close();

			this.crudService.delete(gridObject.getId());

			// módszer 3
			// baj: durva és elveszik a kijelölés

			final int pageNumberWhereWeStoodBeforeDel = 0;

			if (this.leftRightPaging) {
				this.pagingControls.getPageNumber();
			}

			this.grid.deselectAll();
			this.refreshGrid();

			if (this.leftRightPaging) {
				final int pageCountAfterDel = this.pagingControls.getPageCount();
				final int lastPageAfterDel = pageCountAfterDel - 1;

				this.pagingControls.setPageNumber(lastPageAfterDel < pageNumberWhereWeStoodBeforeDel ? lastPageAfterDel : pageNumberWhereWeStoodBeforeDel);
				this.crudGridPageControlComponent.refreshControlsAndLabels();
			}

		});

	}

	private void refreshButtonClicked() {

		// this.refreshGrid();
		// this.grid.asSingleSelect().clear();
		this.refreshGridWithAfterSelectAuto();

		Notification.show(I.trc("Notification", "Elements") + ": " + this.toolboxBackEndDataProvider.size(new Query<D, Object>()));

	}

	private void deSelectAllButtonClicked() {
		this.grid.deselectAll();
	}

	/**
	 * vesszővel elválasztva (experimental, untested)
	 *
	 * @return
	 */
	@SuppressWarnings("unused")
	private String getColumnOrderAsString() {

		final StringBuilder sb = new StringBuilder();

		final List<Column<D, ?>> columns = this.grid.getColumns();

		boolean isFirst = true;

		for (final Column<D, ?> column : columns) {

			if (!isFirst) {
				sb.append(",");
			}

			sb.append(column.getId());

			isFirst = false;
		}

		return sb.toString();
	}

	/**
	 * (experimental, untested)
	 *
	 * @param columnOrderString
	 */
	@SuppressWarnings("unused")
	private void loadColumnOrderFromString(final String columnOrderString) {
		this.grid.setColumns(columnOrderString.split(";"));
	}

	public void markRefreshButton() {

		this.btnRefresh.setDescription(I.trc("Tooltip", "Refresh (new or modified records!)"), ContentMode.TEXT);
		this.btnRefresh.addStyleName(ValoTheme.BUTTON_FRIENDLY);

	}

	public void markRefreshButtonCheck() {

		final boolean needsMark;

		// ---

		final boolean b = SecurityUtil.hasLoggedInUser(); // TODO: megnézni, néha volt valamiért? Vaadinnál... InheretableThreadLocal dolog lehet (evlileg nem arra van téve)? vagy csak JMS esetekben volt néha, hogy a fő szálon ment?

		try {

			if (!b) {

				// úgy a ui.access kapcsán csak akkor kell kézzel beléptetni (és kiléptetni) a user-t, ha nincs...
				// (az igazi gond nem a beléptetés, hanem a finally block-ban a clear)

				SecurityUtil.setUser(this.loggedInUser);
			}

			needsMark = this.toolboxBackEndDataProvider.checkBackEndModifiedDate();

		} finally {
			if (!b) {
				SecurityUtil.clearAuthentication();
			}
		}

		// ---

		if (needsMark) {

			// log.debug("markRefreshButtonCheck, needsMark");

			this.ui.access(() -> {

				// final boolean b = SecurityUtil.hasLoggedInUser();

				// try {

				// if (!b) {
				//
				// // úgy a ui.access kapcsán csak akkor kell kézzel beléptetni (és kiléptetni) a user-t, ha nincs...
				// // (az igazi gond nem a beléptetés, hanem a finally block-ban a clear)
				//
				// SecurityUtil.setUser(this.loggedInUser);
				// }

				if (this.isAttached()) {
					// this.markRefreshButton();
					Notification.show(I.trc("Caption", "A record has been created or modified, please refresh the grid!"), Notification.Type.TRAY_NOTIFICATION);
				}

				// } finally {
				// if (!b) {
				// SecurityUtil.clearAuthentication();
				// }
				// }

			});

		}

		// ---

		if (this.isAttached()) {

			this.scheduleMarkRefreshButtonCheck();

		}

	}

	private void scheduleMarkRefreshButtonCheck() {

		if (this.markRefreshButtonCheckSeconds < 0) {
			return;
		}

		AbstractToolboxUI.scheduleRunnable(() -> {

			this.markRefreshButtonCheck();

			return null;

		}, this.markRefreshButtonCheckSeconds, TimeUnit.SECONDS);

	}
	
	public void setAsyncExport(boolean asyncExport) {
		this.asyncExport = asyncExport;
		// this.asyncExport = false; // átmenetileg, ameddig memory leaket okozhat az async (XLSX export) (ha kell még netán)
	}

}
