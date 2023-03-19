package hu.lanoga.toolbox.holiday;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.sql.Date;
import java.time.YearMonth;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.domain.Page;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import hu.lanoga.toolbox.service.ToolboxCrudService;
import lombok.extern.slf4j.Slf4j;

/**
 * Állami/hivatalos ünnepek és hétvégi/spec. munkanapok (kvázi "fordított" ünnep)...
 * (jelenleg Google-től szedjük)
 */
@Slf4j
@ConditionalOnMissingBean(name = "holidayServiceOverrideBean")
@Service
public class HolidayService implements ToolboxCrudService<Holiday> {

	@Value("${tools.calendar.holiday-sync.google-holidays-url:}")
	private String holidaysUrl;

	@Value("${tools.calendar.holiday.default-query-country}")
	private String defaultQueryCountry;

	@Autowired
	private HolidayJdbcRepository holidayJdbcRepository;

	/**
	 * @param lightSync true esetén csak akkor van sync, ha teljesen üres a tábla
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	@Transactional
	public void syncHolidays(final boolean lightSync) {

		if (StringUtils.isBlank(this.holidaysUrl)) {
			log.warn("Holiday url is blank!");
			return;
		}

		if (lightSync && this.holidayJdbcRepository.count() > 0) {
			return;
		}

		this.holidayJdbcRepository.lockTable();

		final String[] langCountryPairs = this.holidaysUrl.split(";");

		for (final String langCountryPair : langCountryPairs) {

			final String[] langCountrySplit = langCountryPair.split(",");

			final String langCode = langCountrySplit[0]; // a nyelv amibe kérjük le az ünnepeket ("hu" esetén magyarul kapjuk meg)
			final String countryNameForGoogle = langCountrySplit[1]; // az ország, aminek az ünnepeit kérjük le
			final String countryNameInDb = langCountrySplit[2]; // az ország, aminek az ünnepeit kérjük le

			final StringBuilder urlBuilder = new StringBuilder();
			urlBuilder.append("https://calendar.google.com/calendar/ical/");
			urlBuilder.append(langCode);
			urlBuilder.append(".");
			urlBuilder.append(countryNameForGoogle);
			urlBuilder.append("%23holiday%40group.v.calendar.google.com/public/basic.ics");

			try (InputStream input = new BufferedInputStream(new URL(urlBuilder.toString()).openStream(), 131072)) {

				final ICalendar iCal = Biweekly.parse(input).first();

				for (final VEvent event : iCal.getEvents()) {

					final Date eventDate = Date.valueOf(event.getDateStart().getValue().getRawComponents().getYear() + "-" + event.getDateStart().getValue().getRawComponents().getMonth() + "-" + event.getDateStart().getValue().getRawComponents().getDate());

					Holiday holiday = this.holidayJdbcRepository.findOneBy("eventDate", eventDate, "eventCountry", countryNameInDb);

					if (holiday == null) {
						holiday = new Holiday();
						holiday.setEventCountry(countryNameInDb);
						holiday.setEventDate(eventDate);
					}

					// ha már erre a dátumra volt az országban ünnep, akkor azt módosítjuk
					// TODO: lehet egy országban 2 nemzeti ünnep ugyanazon a napon?

					if ((event.getSummary() != null) && StringUtils.isNotBlank(event.getSummary().getValue())) {

						final String summaryValueFromGoogle = event.getSummary().getValue();

						final JSONObject jo;

						if (StringUtils.isNotBlank(holiday.getSummary())) {
							jo = new JSONObject(holiday.getSummary());
						} else {
							jo = new JSONObject();
						}

						jo.put(langCode, summaryValueFromGoogle);

						holiday.setSummary(jo.toString());

						if (summaryValueFromGoogle.contains("Munkanap") || summaryValueFromGoogle.contains("Extra Work Day")) {
							holiday.setWorkday(true);
						} else {
							holiday.setWorkday(false);
						}

					}

					this.holidayJdbcRepository.save(holiday);

				}

				log.info("syncHolidays successful.");

			} catch (final Exception e) {
				throw new ToolboxGeneralException(e);
			}

		}

	}

	/**
	 * @param yearMonth
	 * @param eventCountry
	 * 		két betűs "hu" stb.
	 * @return
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR)
	public List<Holiday> findAllByYear(final YearMonth yearMonth, final String eventCountry) {
		return this.holidayJdbcRepository.findAllByYear(yearMonth, eventCountry);
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR)
	public List<Holiday> findAllByYear(final YearMonth yearMonth) {
		return this.holidayJdbcRepository.findAllByYear(yearMonth, this.defaultQueryCountry);
	}

	// ---

	@Override
	public void delete(final int id) {
		this.holidayJdbcRepository.delete(id);
	}

	@Override
	public Holiday findOne(final int id) {
		return this.holidayJdbcRepository.findOne(id);
	}

	@Override
	public List<Holiday> findAll() {
		return this.holidayJdbcRepository.findAll();
	}

	@Override
	public Page<Holiday> findAll(final BasePageRequest<Holiday> pageRequest) {
		return this.holidayJdbcRepository.findAll(pageRequest);
	}

	@Override
	public Holiday save(final Holiday t) {
		return this.holidayJdbcRepository.save(t);
	}

	@Override
	public long count() {
		return this.holidayJdbcRepository.count();
	}
}
