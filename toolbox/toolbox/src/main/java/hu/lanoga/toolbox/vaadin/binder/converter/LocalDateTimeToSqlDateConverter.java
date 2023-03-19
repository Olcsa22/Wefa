package hu.lanoga.toolbox.vaadin.binder.converter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.data.Converter;
import com.vaadin.data.Result;
import com.vaadin.data.ValueContext;
import com.vaadin.data.converter.LocalDateTimeToDateConverter;

/**
 * experimental, {@link LocalDateTimeToDateConverter}-re visszavezetve
 */
public class LocalDateTimeToSqlDateConverter implements Converter<LocalDateTime, java.sql.Date> {

	private final LocalDateTimeToDateConverter inner;
	
	public LocalDateTimeToSqlDateConverter(ZoneId zoneId) {
		inner = new LocalDateTimeToDateConverter(zoneId);
	}

	@Override
	public Result<java.sql.Date> convertToModel(final LocalDateTime value, final ValueContext context) {

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
	public LocalDateTime convertToPresentation(final java.sql.Date value, final ValueContext context) {
		return this.inner.convertToPresentation(new java.util.Date(value.getTime()), context);
	}

}
