package hu.lanoga.toolbox.vaadin.binder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.lang3.tuple.Pair;

import com.teamunify.i18n.I;
import com.vaadin.data.Converter;
import com.vaadin.data.Result;
import com.vaadin.data.converter.DateToSqlDateConverter;
import com.vaadin.data.converter.LocalDateTimeToDateConverter;
import com.vaadin.data.converter.LocalDateToDateConverter;
import com.vaadin.data.converter.StringToIntegerConverter;

import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.vaadin.binder.converter.LocalDateTimeToSqlDateConverter;
import hu.lanoga.toolbox.vaadin.binder.converter.LocalDateTimeToSqlTimestampConverter;
import hu.lanoga.toolbox.vaadin.binder.converter.LocalDateToSqlDateConverter;
import hu.lanoga.toolbox.vaadin.binder.converter.LocalDateToSqlTimestampConverter;

class ConverterRegistry {

	private static <PRESENTATIONTYPE, MODELTYPE> void registerConverter(final Map<Pair<Class<?>, Class<?>>, Converter<?, ?>> converters, final Class<PRESENTATIONTYPE> presentationType, final Class<MODELTYPE> modelType, final Converter<PRESENTATIONTYPE, MODELTYPE> converter) {
		converters.put(Pair.of(presentationType, modelType), converter);
	}

	private Map<Pair<Class<?>, Class<?>>, Converter<?, ?>> converters;

	ConverterRegistry() {

		final Converter<String, Integer> c = Converter.from(e -> {
			if (e.length() == 0) {
				return Result.error(I.trc("Notification", "Must be a number"));
			}
			try {
				return Result.ok(Integer.parseInt(e));
			} catch (final NumberFormatException ex) {
				return Result.error(I.trc("Notification", "Must be a number"));
			}
		}, e -> Integer.toString(e));

		final Map<Pair<Class<?>, Class<?>>, Converter<?, ?>> cMap = new HashMap<>();

		registerConverter(cMap, String.class, int.class, c);
		registerConverter(cMap, String.class, Integer.class, new NullConverter<>("").chain(new StringToIntegerConverter(I.trc("Notification", "Conversion failed"))));

		registerConverter(cMap, String.class, Character.class, Converter.from(e -> e.length() == 0 ? Result.ok(null) : (e.length() == 1 ? Result.ok(e.charAt(0)) : Result.error(I.trc("Notification", "Must be one character"))), f -> f == null ? "" : "" + f));

		// ---

		// dátum...

		registerConverter(cMap, java.util.Date.class, java.sql.Date.class, new DateToSqlDateConverter()); // kell ez?

		final ZoneId loggedInUserZoneId = I18nUtil.getLoggedInUserTimeZone().toZoneId();
		final ZoneId zoneIdForDayPrecision = ZoneId.systemDefault();

		registerConverter(cMap, LocalDateTime.class, java.util.Date.class, new LocalDateTimeToDateConverter(loggedInUserZoneId));
		registerConverter(cMap, LocalDateTime.class, java.sql.Date.class, new LocalDateTimeToSqlDateConverter(loggedInUserZoneId));
		registerConverter(cMap, LocalDateTime.class, java.sql.Timestamp.class, new LocalDateTimeToSqlTimestampConverter(loggedInUserZoneId));

		registerConverter(cMap, LocalDate.class, java.util.Date.class, new LocalDateToDateConverter(zoneIdForDayPrecision));
		registerConverter(cMap, LocalDate.class, java.sql.Date.class, new LocalDateToSqlDateConverter(zoneIdForDayPrecision));
		registerConverter(cMap, LocalDate.class, java.sql.Timestamp.class, new LocalDateToSqlTimestampConverter(zoneIdForDayPrecision));

		this.converters = UnmodifiableMap.unmodifiableMap(cMap);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	<PRESENTATIONTYPE, MODELTYPE> Converter<PRESENTATIONTYPE, MODELTYPE> getConverter(final Class<PRESENTATIONTYPE> presentationType, final Class<MODELTYPE> modelType) {
		return (Converter) this.converters.get(Pair.of(presentationType, modelType));
	}

}
