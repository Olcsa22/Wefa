package hu.lanoga.toolbox.vaadin.component.crud;

import org.vaadin.teemusa.gridextensions.paging.PagedDataProvider;

import com.vaadin.data.provider.DataProvider;

import hu.lanoga.toolbox.repository.ToolboxPersistable;

class ToolboxPagedDataProvider<T extends ToolboxPersistable> extends PagedDataProvider<T, Object> {

	ToolboxPagedDataProvider(final DataProvider<T, Object> dataProvider, final int pageLength) {
		super(dataProvider, pageLength);
	}

	@Override
	public Object getId(final T item) {
		return item.getId();
	}

}