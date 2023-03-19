package org.vaadin.teemusa.gridextensions.paging;

import java.util.stream.Stream;

import com.vaadin.data.provider.AbstractDataProvider;
import com.vaadin.data.provider.DataChangeEvent.DataRefreshEvent;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.Query;

public class PagedDataProvider<T, F> extends AbstractDataProvider<T, F> {

	private final PagingControls<T> pagingControls;
	final DataProvider<T, F> dataProvider;
	Integer backendSize;

	public PagedDataProvider(DataProvider<T, F> dataProvider) {
		this(dataProvider, 10);
	}

	public PagedDataProvider(DataProvider<T, F> dataProvider, int pageLength) {
		pagingControls = new PagingControls<>(this, pageLength);
		this.dataProvider = dataProvider;
		this.dataProvider.addDataProviderListener(event -> {
			if (event instanceof DataRefreshEvent) {
				fireEvent(event);
			} else {
				setBackendSize(null);
				refreshAll();
			}
		});
	}

	@Override
	public boolean isInMemory() {
		return dataProvider.isInMemory();
	}

	@Override
	public int size(Query<T, F> query) {
		int s = getPagingControls().getSizeOfPage(query);

		// ---

		// "maszek" "javítás"

		if (backendSize != null && backendSize.intValue() == 0) {
			return 0;
		}

		// ---

		return s;
	}

	public PagingControls<T> getPagingControls() {
		return pagingControls;
	}

	void setBackendSize(Integer size) {
		backendSize = size;
		if (size != null) {
			getPagingControls().updatePageNumber();
		}
	}

	@Override
	public Stream<T> fetch(Query<T, F> query) {

		Query<T, F> newQuery = getPagingControls().alignQuery(query);

		// ---

		// "maszek" "javítás"

		// System.out.println("1a: " + query.getOffset() + ", " + query.getLimit());
		// System.out.println("1b: " + newQuery.getOffset() + ", " + newQuery.getLimit());

		// Query<T, F> newQuery2 = new Query<>(newQuery.getOffset() >= 0 ? newQuery.getOffset() : 0, newQuery.getLimit(), newQuery.getSortOrders(), newQuery.getInMemorySorting(), newQuery.getFilter().orElse(null));

		// System.out.println("1c: " + newQuery2.getOffset() + ", " + newQuery2.getLimit());

		// ---

		return dataProvider.fetch(newQuery);

	}

	int getBackendSize() {

		// System.out.println("2");

		if (backendSize == null) {

			// System.out.println("2b");

			setBackendSize(dataProvider.size(new Query<>()));
		}
		return backendSize;
	}

	@Override
	public void refreshAll() {

		// ---

		// "maszek" "javítás"

		setBackendSize(null); // fontos, pl. akkor, ha menet (lapozás) közben lesz több/kevesebb elem az adatbázisban!

		// ---

		super.refreshAll();
	}

}
