package hu.lanoga.toolbox.quickcontact;

import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class QuickContactJdbcRepository extends DefaultJdbcRepository<QuickContact> {

	public List<QuickContact> findAllForEmailNotification() {
		final SqlParameterSource namedParameters = new MapSqlParameterSource();
		return namedParameterJdbcTemplate.query(fillVariables("SELECT * FROM quick_contact WHERE #tenantCondition AND (is_sent = false OR is_sent IS NULL) AND (origin IS NULL OR origin != 'manual add') "), namedParameters, newRowMapperInstance());
	}

	
	
}