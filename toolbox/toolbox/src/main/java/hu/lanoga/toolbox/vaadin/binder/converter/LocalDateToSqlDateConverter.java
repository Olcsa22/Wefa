package hu.lanoga.toolbox.vaadin.binder.converter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.data.Converter;
import com.vaadin.data.Result;
import com.vaadin.data.ValueContext;
import com.vaadin.data.converter.LocalDateToDateConverter;

/**
 * experimental, {@link LocalDateToDateConverter}-re visszavezetve
 */
public class LocalDateToSqlDateConverter implements Converter<LocalDate, java.sql.Date> {

	private final LocalDateToDateConverter inner;

	public LocalDateToSqlDateConverter(ZoneId zoneId) {
		inner = new LocalDateToDateConverter(zoneId);
	}
	
	@Override
	public Result<java.sql.Date> convertToModel(final LocalDate value, final ValueContext context) {

		final Result<java.util.Date> result = this.inner.convertToModel(value, context);

		final List<java.sql.Date> retVal = new ArrayList<>();

		result.ifOk(c -> {
			if (c != null) {
				retVal.add(new java.sql.Date(c.getTime()));
			}
		});

		if (retVal.isEmpty()) {
			return Result.ok(null);
		} else {
			return Result.ok(retVal.get(0));
		}
	}

	@Override
	public LocalDate convertToPresentation(final java.sql.Date value, final ValueContext context) {
		return this.inner.convertToPresentation(new java.util.Date(value.getTime()), context);
	}

}
