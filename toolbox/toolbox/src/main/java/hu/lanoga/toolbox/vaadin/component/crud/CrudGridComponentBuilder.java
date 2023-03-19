package hu.lanoga.toolbox.vaadin.component.crud;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import hu.lanoga.toolbox.filter.internal.SearchCriteria;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.service.ToolboxCrudService;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponent.CrudGridColumnSetupModel;
import lombok.Getter;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Getter
public class CrudGridComponentBuilder<D extends ToolboxPersistable> {

	private Class<D> modelType;
	private ToolboxCrudService<D> crudService;
	private Supplier<CrudFormComponent<D>> crudFormComponentSupplier;
	private boolean leftRightPaging = true;
	private Integer leftRightPagingPageSize;
	private Set<SearchCriteria> initialFixedSearchCriteriaSet;
	private Grid.SelectionMode selectionMode = SelectionMode.SINGLE;
	private List<CrudGridColumnSetupModel> crudGridColumnSetupModelList;
	private Map<String, ComboBox<Integer>> searchComboMap;
	private List<String> aggregateFieldNameList;
	private String booleanNullChar;

	public CrudGridComponentBuilder<D> setModelType(final Class<D> modelType) {
		this.modelType = modelType;
		return this;
	}

	public CrudGridComponentBuilder<D> setCrudService(final ToolboxCrudService<D> crudService) {
		this.crudService = crudService;
		return this;
	}

	public CrudGridComponentBuilder<D> setCrudFormComponentSupplier(final Supplier<CrudFormComponent<D>> crudFormComponentSupplier) {
		this.crudFormComponentSupplier = crudFormComponentSupplier;
		return this;
	}

	/**
	 * akarunk-e jobbra-balra lapozást (false esetén scoll lesz)
	 * (fontos, hogy a service hívások mindenkép {@link Pageable} alapúak, ez a csak a vizuális megjelenést állítja!)
	 *
	 * @param leftRightPaging
	 * @return
	 */
	public CrudGridComponentBuilder<D> setLeftRightPaging(final boolean leftRightPaging) {
		this.leftRightPaging = leftRightPaging;
		return this;
	}

	public CrudGridComponentBuilder<D> setInitialFixedSearchCriteriaSet(final Set<SearchCriteria> initialFixedSearchCriteriaSet) {
		this.initialFixedSearchCriteriaSet = initialFixedSearchCriteriaSet;
		return this;
	}

	/**
	 * jelenleg csak {@link Grid.SelectionMode#SINGLE} esetén működnek csak az alap crud műveletek
	 * (hozzáadás, delete stb.) (ha nincs megadva külön, akkor ez a default)
	 *
	 * @param selectionMode
	 * @return
	 */
	public CrudGridComponentBuilder<D> setSelectionMode(final Grid.SelectionMode selectionMode) {
		this.selectionMode = selectionMode;
		return this;
	}

	public CrudGridComponentBuilder<D> setCrudGridColumnSetupModelList(final List<CrudGridColumnSetupModel> crudGridColumnSetupModelList) {
		this.crudGridColumnSetupModelList = crudGridColumnSetupModelList;
		return this;
	}

	public CrudGridComponentBuilder<D> setSearchComboMap(final Map<String, ComboBox<Integer>> searchComboMap) {
		this.searchComboMap = searchComboMap;
		return this;
	}


	public CrudGridComponentBuilder<D> setAggregateFieldNameList(final List<String> aggregateFieldNameList) {
		this.aggregateFieldNameList = aggregateFieldNameList;
		return this;
	}
	
	/**
	 * leftRightPaging true esetén mekkora legyen egy oldal (null esetén a default a 10) 
	 * (alap esetben a this.grid.setHeightByRows is erre van állítva, de azt át lehet állítani)
	 * 
	 * @param leftRightPagingPageSize
	 * @return
	 */
	public CrudGridComponentBuilder<D> setLeftRightPagingPageSize(Integer leftRightPagingPageSize) {
		this.leftRightPagingPageSize = leftRightPagingPageSize;
		return this;
	}
	
	public CrudGridComponentBuilder<D> setBooleanNullChar(String booleanNullChar) {
		this.booleanNullChar = booleanNullChar;
		return this;
	}

	
	public CrudGridComponent<D> createCrudGridComponent() {
		return new CrudGridComponent<>(this.modelType, this.crudService, this.crudFormComponentSupplier, this.leftRightPaging, this.leftRightPagingPageSize, this.initialFixedSearchCriteriaSet, this.selectionMode, this.crudGridColumnSetupModelList, this.searchComboMap, this.aggregateFieldNameList, this.booleanNullChar);
	}

}