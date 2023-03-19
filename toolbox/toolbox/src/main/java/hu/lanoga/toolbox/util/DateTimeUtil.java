package hu.lanoga.toolbox.util;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.vaadin.binder.converter.LocalDateTimeToSqlTimestampConverter;

public class DateTimeUtil {

	public static final int DAY_IN_SEC = 86400;

	public static final int DAY_IN_MS = 86400000;

	public static final long WEEK_IN_MS = DAY_IN_MS * 7L;

	private DateTimeUtil() {
		//
	}

	// TODO: jobb metódus nevek, javadoc magyarázat (melyik mit is csinál itt), a metódus neve ige kell legyen...
	
	public static String dateConverterWithHoursAndMinutes(final java.util.Date date, final TimeZone timeZone) {
		// java.sql.Date is lehet, aminek nincs date.toInstant() implementációja, lásd java.sql.Date.toInstant()
		// ezért kell itt az ofEpochMilli megoldás
		final LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), timeZone.toZoneId());
		return localDateTime.toString();
	}
	
	public static String dateConverterWithHoursAndMinutes2(final java.util.Date date, final TimeZone timeZone) {
		// java.sql.Date is lehet, aminek nincs date.toInstant() implementációja, lásd java.sql.Date.toInstant()
		// ezért kell itt az ofEpochMilli megoldás
		final LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), timeZone.toZoneId());
		return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
	}
	
	public static String dateConverterWithHoursAndMinutes3(final java.util.Date date, final TimeZone timeZone) {
		// java.sql.Date is lehet, aminek nincs date.toInstant() implementációja, lásd java.sql.Date.toInstant()
		// ezért kell itt az ofEpochMilli megoldás
		final LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), timeZone.toZoneId());
		return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
	}
	
	public static String dateConverter(final String date) {
		final LocalDateTime dateTime = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"));
		return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
	}
	
	public static String dateConverter2(final java.sql.Date date) {
		final LocalDate localDate = date.toLocalDate();
		return localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
	}

	/**
	 * hány éves (betöltött évek) az illető az adott napon 
	 * (óra, perc, mp nincs figyelembe véve a számításnál)
	 * 
	 * @param birthDate
	 * @param date
	 * @return
	 */
	public static int getAgeInYears(final java.util.Date birthDate, final java.util.Date date) {

		Calendar dob = Calendar.getInstance();
		dob.setTime(birthDate);

		Calendar d = Calendar.getInstance();
		d.setTime(date);

		int age = d.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

		if (d.get(Calendar.MONTH) < dob.get(Calendar.MONTH) || 
			(d.get(Calendar.MONTH) == dob.get(Calendar.MONTH) && d.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
			age = age - 1;
		}

		return age;

	}
	
	/**
	 * hány hónapos (betöltött hónapok) az illető az adott napon 
	 * (óra, perc, mp nincs figyelembe véve a számításnál)
	 * 
	 * @param birthDate
	 * @param date
	 * @return
	 * 
	 * @see #getAgeInYears(Date, Date)
	 */
	public static int getAgeInMonths(final java.sql.Date birthDate, final java.sql.Date date) {
		
		Period period = Period.between(LocalDate.parse(birthDate.toString()), LocalDate.parse(date.toString()));

		// TODO: ez nagyon komplex téma... szökőévek és egyéb dolgok bekavarhatnak, eleve abban sem vagyok biztos, hogy a 28,29,30,31 napos hónapokat kezeli rendesen... (vagy ha igen, akkor az a jó?)
		// TODO: ha változtatsz, akkor írj új metódust, akármilyen is ez itt, erre a műkdöésre építettünk
		
		return 12 * period.getYears() + period.getMonths();

	}
		
	/**
	 * @param ts
	 * @param fromTz
	 * @param toTz
	 * 		null esetén a server default-ja lesz
	 * @return
	 */
	public static java.sql.Timestamp adjustTimeZone(final java.sql.Timestamp ts, ZoneId fromTz, ZoneId toTz) {
		
		if (ts == null) {
			return null;
		}
		
		if (fromTz == null) {
			fromTz = TimeZone.getDefault().toZoneId();
		}
		
		if (toTz == null) {
			toTz = TimeZone.getDefault().toZoneId();
		}
		
//		System.out.println("---");
//		System.out.println(ts);
//		System.out.println(TimeZone.getTimeZone(fromTz).getDisplayName());
//		System.out.println(TimeZone.getTimeZone(fromTz).getOffset(ts.getTime()));
//		System.out.println(TimeZone.getTimeZone(toTz).getDisplayName());
//		System.out.println(TimeZone.getTimeZone(toTz).getOffset(ts.getTime()));
//		System.out.println(new Timestamp (ts.getTime() + TimeZone.getTimeZone(toTz).getOffset(ts.getTime()) - TimeZone.getTimeZone(fromTz).getOffset(ts.getTime())));
	
		return new Timestamp (ts.getTime() + TimeZone.getTimeZone(toTz).getOffset(ts.getTime()) - TimeZone.getTimeZone(fromTz).getOffset(ts.getTime())); 
					
	}
	
	/**
	 * szerződésekhez, egy napot mindig levon 
	 * (nem univerzális megoldás, egyes konkrét alkalmazásokhoz kell ez, 
	 * gondold át, hogy neked tényleg ez kell-e!)
	 * 
	 * pl.:  
	 * 2020-09-01 + 12 hónap = 2021-08-31, 
	 * 2020-05-01 + 6 hónap = 2021-10-31 
	 * 
	 * @param d
	 * @param months
	 * @return
	 */
	public static java.sql.Date addMonthsSpecial(java.sql.Date d, int months) {
		
		final Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.add(Calendar.MONTH, months);
		
		final long l = cal.getTimeInMillis() - (1000L * 60L * 60L * 24L);
		
		return new java.sql.Date(l);
		
	}
	
	public static java.sql.Date addDays(java.sql.Date d, int days) {
		
		final Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.add(Calendar.DAY_OF_MONTH, days);
		
		final long l = cal.getTimeInMillis();
		
		return new java.sql.Date(l);
		
	}
	
//	public static java.sql.Date addDays(final java.sql.Date date, int days) {
//		return new java.sql.Date(date.getTime() + days * DateTimeUtil.DAY_IN_MS);
//	}
	
	public static boolean isItFutureDateTimeConsideringUserTimezone(LocalDateTime ldt) {
		
		if (ldt == null) {
			return false;
		}
		
		final ZoneId loggedInUserZoneId = I18nUtil.getLoggedInUserTimeZone().toZoneId();
		LocalDateTimeToSqlTimestampConverter localDateTimeToSqlTimestampConverter = new LocalDateTimeToSqlTimestampConverter(loggedInUserZoneId);
				
		return isItFutureDateTimeConsideringUserTimezone(localDateTimeToSqlTimestampConverter, ldt);
		
	}
	
	public static boolean isItFutureDateTimeConsideringUserTimezone(LocalDateTimeToSqlTimestampConverter localDateTimeToSqlTimestampConverter, LocalDateTime ldt) {
		
		if (ldt == null) {
			return false;
		}
				
		final long l = localDateTimeToSqlTimestampConverter.convertToModel(ldt, null).getOrThrow(t -> new ToolboxGeneralException("isItFutureDateTimeConsideringUserTimezone conversion error")).getTime();
		
		return l > System.currentTimeMillis();
		
	}
	
}
