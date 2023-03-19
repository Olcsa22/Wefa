package hu.lanoga.toolbox.vaadin.component.crud;

import java.lang.reflect.Field;
import java.util.List;

import com.vaadin.ui.Grid;
import com.vaadin.ui.components.grid.FooterRow;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.service.ToolboxCrudService;
import hu.lanoga.toolbox.util.ToolboxStringUtil;

public class CrudFooterAggregate<D extends ToolboxPersistable> {

	private Grid<D> grid;

	private ToolboxBackEndDataProvider<D> toolboxBackEndDataProvider;
	private List<String> aggregateFieldNameList;

	private FooterRow footerRow;

	/**
	 * @param grid
	 * 		rá is teszi "magát" erre a {@code Grid}-re rögtön
	 * @param toolboxBackEndDataProvider
	 * @param aggregateFieldNameList
	 */
	public CrudFooterAggregate(final Grid<D> grid, final ToolboxBackEndDataProvider<D> toolboxBackEndDataProvider, final List<String> aggregateFieldNameList) {
		this.grid = grid;

		this.toolboxBackEndDataProvider = toolboxBackEndDataProvider;
		this.aggregateFieldNameList = aggregateFieldNameList;

		this.footerRow = this.grid.appendFooterRow();
	}

	public void refreshAggregateCells() {

		for (String t : this.aggregateFieldNameList) {

			try {

				BasePageRequest<D> pageRequest = this.toolboxBackEndDataProvider.getLastPageRequest();
				
				String aggregateSqlFunctionName = ToolboxSysKeys.AggregateSqlFunctionNameType.SUM;
				String aggregateFieldName = t;

				if (t.contains(":")) {
					String[] split = t.split(":");
					aggregateSqlFunctionName = split[0];
					aggregateFieldName = split[1];
				}
				
				aggregateSqlFunctionName = aggregateSqlFunctionName.toUpperCase();
				
				pageRequest.setAggregateFieldName(aggregateFieldName);
				pageRequest.setAggregateSqlFunctionName(aggregateSqlFunctionName);
				final String aggregateViewFieldName = aggregateFieldName + ToolboxStringUtil.underscoreToCamelCaseBig(aggregateSqlFunctionName);

				ToolboxCrudService<D> service = toolboxBackEndDataProvider.getService();

				final List<D> l = service.findAll(pageRequest).getContent();
				if (!l.isEmpty()) {

					D item = l.get(0);

					final Field field = item.getClass().getDeclaredField(aggregateViewFieldName);
					field.setAccessible(true);
					Object fieldValue = field.get(item);

					this.footerRow.getCell(aggregateFieldName).setText(aggregateSqlFunctionName + ": " + fieldValue); // SUM jel: "\u03A3 "
				}

			} catch (Exception e) {
				throw new ToolboxGeneralException("CrudFooterAggregate error!", e);
			}

		}

	}

	public FooterRow getFooterRow() {
		return this.footerRow;
	}

}
