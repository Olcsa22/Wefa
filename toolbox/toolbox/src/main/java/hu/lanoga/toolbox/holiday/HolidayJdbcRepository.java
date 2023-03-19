package hu.lanoga.toolbox.holiday;

import java.time.YearMonth;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import hu.lanoga.toolbox.ToolboxSysKeys.RepositoryTenantMode;
import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;

@ConditionalOnMissingBean(name = "holidayJdbcRepositoryOverrideBean")
@Repository
public class HolidayJdbcRepository extends DefaultJdbcRepository<Holiday> {
	
	public HolidayJdbcRepository() {
		super(RepositoryTenantMode.NO_TENANT);
	}

	public List<Holiday> findAllByYear(final YearMonth yearMonth, String eventCountry) {
		
		eventCountry = eventCountry.toLowerCase();
		
		final SqlParameterSource namedParameters = 
				new MapSqlParameterSource()
				.addValue("actualYear", yearMonth.getYear())
				.addValue("actualMonth", yearMonth.getMonthValue())
				.addValue("eventCountry", eventCountry);
		
		return namedParameterJdbcTemplate.query("SELECT * FROM holiday WHERE "
				+ "date_part('year', event_date) = :actualYear "
				+ "AND date_part('month', event_date) = :actualMonth "
				+ "AND event_country = :eventCountry", namedParameters, newRowMapperInstance());
	}
}
