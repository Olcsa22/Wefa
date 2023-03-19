package hu.lanoga.toolbox.vaadin.component.crud;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

import com.vaadin.data.provider.AbstractBackEndDataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.data.provider.QuerySortOrder;
import com.vaadin.shared.data.sort.SortDirection;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import hu.lanoga.toolbox.filter.internal.SearchCriteria;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.service.LazyEnhanceCrudService;
import hu.lanoga.toolbox.service.ToolboxCrudService;
import lombok.Getter;

/**
 * {@link ToolboxCrudService#findAll(BasePageRequest)} hívásokra épít
 * 
 * @see SearchCriteria
 */
@Getter
public class ToolboxBackEndDataProvider<D extends ToolboxPersistable> extends AbstractBackEndDataProvider<D, Object> {

	private final LinkedHashSet<SearchCriteria> searchCriteriaSet;

	/**
	 * fix szűrők (értsd amiket nem user befolyásol a header stb. szűrőkkel)
	 */
	private final LinkedHashSet<SearchCriteria> fixedSearchCriteriaSet;

	private final ToolboxCrudService<D> service;

	private Query<D, Object> lastFetchQuery;
	private Timestamp tsLastBackEndModifiedDate;

	private CrudFooterAggregate<D> crudFooterAggregate;

	private BasePageRequest<D> lastPageRequest;

	public ToolboxBackEndDataProvider(final ToolboxCrudService<D> service) {
		this.service = service;
		this.searchCriteriaSet = new LinkedHashSet<>();
		this.fixedSearchCriteriaSet = new LinkedHashSet<>();
	}
	
	protected int sizeInBackEndLastQuery() {
		return this.sizeInBackEnd(this.lastFetchQuery);
	}

	protected List<D> fetchFromBackEndLastQueryAll() {
		return this.fetchFromBackEnd(this.lastFetchQuery, 0, Integer.MAX_VALUE, false).collect(Collectors.toList());
	}

	@Override
	protected Stream<D> fetchFromBackEnd(final Query<D, Object> query) {
		return this.fetchFromBackEnd(query, null, 10, true);
	}

	private Stream<D> fetchFromBackEnd(final Query<D, Object> query, final Integer overrideOffset, final int size, final boolean refreshAggregateCells) {

		final List<Order> orders = new ArrayList<>();

		for (final QuerySortOrder querySortOrder : query.getSortOrders()) {
			orders.add(new Order(SortDirection.ASCENDING == querySortOrder.getDirection() ? Direction.ASC : Direction.DESC, querySortOrder.getSorted()));
		}

		final Sort sort = orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);

		final LinkedHashSet<SearchCriteria> searchCriteriaSet2 = new LinkedHashSet<>();
		searchCriteriaSet2.addAll(this.searchCriteriaSet);
		searchCriteriaSet2.addAll(this.fixedSearchCriteriaSet);

		final LinkedHashSet<SearchCriteria> searchCriteriaSet3 = searchCriteriaSet2.isEmpty() ? null : searchCriteriaSet2;

		final int page = ((overrideOffset == null ? query.getOffset() : overrideOffset) / size);

		final BasePageRequest<D> pageRequest = new BasePageRequest<>(page, size, sort, searchCriteriaSet3, ToolboxSysKeys.SearchCriteriaLogicalOperation.AND);
		pageRequest.setDoCount(false);

		final List<D> content = this.service.findAll(pageRequest).getContent();

		if (this.service instanceof LazyEnhanceCrudService) {

			((LazyEnhanceCrudService<D>) this.service).enhance(content);

			// log.debug("fetchFromBackEnd, enhance successful");
		}

		// log.debug("fetchFromBackEnd, size: " + content.size());

		this.lastPageRequest = pageRequest;
		this.lastFetchQuery = query;

		{
			this.checkBackEndModifiedDate();
		}

		if (refreshAggregateCells && this.crudFooterAggregate != null) {
			this.crudFooterAggregate.refreshAggregateCells(); // TODO: this is very ugly, but works
		}

		return content.stream();
	}

	@Override
	protected int sizeInBackEnd(final Query<D, Object> query) {

		final LinkedHashSet<SearchCriteria> searchCriteriaSet2 = new LinkedHashSet<>();
		searchCriteriaSet2.addAll(this.searchCriteriaSet);
		searchCriteriaSet2.addAll(this.fixedSearchCriteriaSet);

		final LinkedHashSet<SearchCriteria> searchCriteriaSet3 = searchCriteriaSet2.isEmpty() ? null : searchCriteriaSet2;

		final int page = query.getOffset() / 10;
		final int size = 10;

		final BasePageRequest<D> pageRequest = new BasePageRequest<>(page, size, searchCriteriaSet3, ToolboxSysKeys.SearchCriteriaLogicalOperation.AND);
		pageRequest.setDoQuery(false);

		final int totalCount = (int) this.service.findAll(pageRequest).getTotalElements();

		// log.debug("sizeInBackEnd, totalCount: " + totalCount);

		return totalCount;
	}

	private Timestamp backEndModifiedDate() {

		final LinkedHashSet<SearchCriteria> searchCriteriaSet2 = new LinkedHashSet<>();
		searchCriteriaSet2.addAll(this.searchCriteriaSet);
		searchCriteriaSet2.addAll(this.fixedSearchCriteriaSet);

		final LinkedHashSet<SearchCriteria> searchCriteriaSet3 = searchCriteriaSet2.isEmpty() ? null : searchCriteriaSet2;

		final BasePageRequest<D> pageRequest = new BasePageRequest<>(0, 1, searchCriteriaSet3, ToolboxSysKeys.SearchCriteriaLogicalOperation.AND);
		pageRequest.setDoCount(false);
		pageRequest.setDoQueryLastModifiedOnly(true);

		final List<D> l = this.service.findAll(pageRequest).getContent();
		if (!l.isEmpty()) {
			return l.get(0).getModifiedOn();
		}

		return null;
	}

	/**
	 * @return true, ha volt "mostanában" módosítás
	 */
	protected boolean checkBackEndModifiedDate() {

		if (this.lastFetchQuery == null) { // ha még érdemben nem volt használva a grid, akkor nem nézzünk "van-e újabb dolog a DB-ben"-t
			// log.debug("checkBackEndModifiedDate (" + service.getClass().getSimpleName() + "), skip, not yet visbile grid");
			return false;
		}

		// ---

		final Timestamp ts = this.backEndModifiedDate();

		// log.debug("checkBackEndModifiedDate (" + service.getClass().getSimpleName() + "), current Java value: " + tsLastBackEndModifiedDate);
		// log.debug("checkBackEndModifiedDate (" + service.getClass().getSimpleName() + "), fresh from DB: " + ts);

		if (!Objects.equals(tsLastBackEndModifiedDate, ts)) {

			// log.debug("checkBackEndModifiedDate (" + service.getClass().getSimpleName() + "), current Java value is different/outdated");

			this.tsLastBackEndModifiedDate = ts;
			return true;

		}

		return false;

	}

	protected void overrideBackEndModifiedDate(final Timestamp ts) {
		this.tsLastBackEndModifiedDate = ts;
	}

	@Override
	public Object getId(final D item) {
		return item.getId();
	}

	public void setCrudFooterAggregate(final CrudFooterAggregate<D> crudFooterAggregate) {
		this.crudFooterAggregate = crudFooterAggregate;
	}

}
