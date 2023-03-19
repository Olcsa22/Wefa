package hu.lanoga.toolbox.filter.internal;

import java.util.LinkedHashSet;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import hu.lanoga.toolbox.ToolboxSysKeys;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * {@code org.springframework.data.domain.PageRequest} kiegészítve filtereléssel...
 * a mezőnevek már SQL injection safe-ek... (whitelist alapján ellenőrizve, csak a cél Java class mezőneve megengedettek)
 * 
 * (lehet null is bármelyik épp nem használt rész, csak paging, csak search, csak sort... ezek bármilyen kombinációja...)
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BasePageRequest<T> extends PageRequest {

	private ToolboxSysKeys.SearchCriteriaLogicalOperation searchCriteriaLogicalOperation;
	private LinkedHashSet<SearchCriteria> searchCriteriaSet;

	private boolean doQueryLastModifiedOnly = false;

	private boolean doQuery = true;
	private boolean doCount = true;
	
	private String aggregateFieldName;
	private String aggregateSqlFunctionName;

	public BasePageRequest(int page, int size, Sort sort) {
		this(page, size, sort, (LinkedHashSet<SearchCriteria>) null, ToolboxSysKeys.SearchCriteriaLogicalOperation.OR);
	}

	public BasePageRequest(int page, int size, Sort sort, LinkedHashSet<SearchCriteria> searchCriteriaSet, ToolboxSysKeys.SearchCriteriaLogicalOperation searchCriteriaLogicalOperation) {
		super(page, size, sort);
		this.searchCriteriaSet = searchCriteriaSet;
		this.searchCriteriaLogicalOperation = searchCriteriaLogicalOperation;
	}

	public BasePageRequest(int page, int size) {
		this(page, size, (LinkedHashSet<SearchCriteria>) null, ToolboxSysKeys.SearchCriteriaLogicalOperation.OR);
	}

	public BasePageRequest(int page, int size, LinkedHashSet<SearchCriteria> searchCriteriaSet, ToolboxSysKeys.SearchCriteriaLogicalOperation searchCriteriaLogicalOperation) {
		super(page, size, Sort.unsorted());
		this.searchCriteriaSet = searchCriteriaSet;
		this.searchCriteriaLogicalOperation = searchCriteriaLogicalOperation;
	}
	
	public BasePageRequest(LinkedHashSet<SearchCriteria> searchCriteriaSet) {
		super(0, Integer.MAX_VALUE, Sort.unsorted());
		this.searchCriteriaSet = searchCriteriaSet;
		this.searchCriteriaLogicalOperation = ToolboxSysKeys.SearchCriteriaLogicalOperation.AND;
	}
	
	public BasePageRequest(SearchCriteria searchCriteria) {
		super(0, Integer.MAX_VALUE, Sort.unsorted());
		this.searchCriteriaSet = new LinkedHashSet<> ();
		this.searchCriteriaSet.add(searchCriteria);
		this.searchCriteriaLogicalOperation = ToolboxSysKeys.SearchCriteriaLogicalOperation.AND;
	}

	@Override
	public PageRequest next() {
		return new BasePageRequest<T>(getPageNumber() + 1, getPageSize(), getSort(), searchCriteriaSet, searchCriteriaLogicalOperation);
	}

	@Override
	public PageRequest previous() {
		return getPageNumber() == 0 ? this : new BasePageRequest<T>(getPageNumber() - 1, getPageSize(), getSort(), searchCriteriaSet, searchCriteriaLogicalOperation);
	}

	@Override
	public PageRequest first() {
		return new BasePageRequest<T>(0, getPageSize(), getSort(), searchCriteriaSet, searchCriteriaLogicalOperation);
	}

	/**
	 * {@code SearchCriteria}} equals/hashCode-ra figyelni a használatnál!
	 * 
	 * @param searchCriteria
	 */
	public void addSearchCriteria(SearchCriteria searchCriteria) {

		if (searchCriteriaSet == null) {
			searchCriteriaSet = new LinkedHashSet<>();
		}

		searchCriteriaSet.add(searchCriteria);
	}

	/**
	 * (lásd {@link SearchCriteria} equals/hashCode)
	 * 
	 * @param searchCriteria
	 * @return 
	 * 		true, ha benne volt (és most kiszedtük), false, ha nem volt ilyen elem (esetleg teljesen üres folt még a collection)
	 */
	public boolean removeSearchCriteria(SearchCriteria searchCriteria) {

		if (searchCriteriaSet == null) {
			return false;
		}

		return searchCriteriaSet.remove(searchCriteria);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasePageRequest [searchCriteriaLogicalOperation=");
		builder.append(searchCriteriaLogicalOperation);
		builder.append(", searchCriteriaSet=");
		builder.append(searchCriteriaSet);
		builder.append(", doQuery=");
		builder.append(doQuery);
		builder.append(", doCount=");
		builder.append(doCount);
		builder.append("]");
		return builder.toString();
	}

	// TODO: esetleg további kényelmi metódusok, pl.: removeSearchCriteria(String columnName)... ugyanezek több elemmel...

}