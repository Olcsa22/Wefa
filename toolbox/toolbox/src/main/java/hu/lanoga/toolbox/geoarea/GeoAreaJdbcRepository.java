package hu.lanoga.toolbox.geoarea;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.RepositoryTenantMode;
import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import java.util.List;

@ConditionalOnMissingBean(name = "geoAreaJdbcRepositoryOverrideBean")
@Repository
public class GeoAreaJdbcRepository extends DefaultJdbcRepository<GeoArea> {

	public GeoAreaJdbcRepository() {
		super(RepositoryTenantMode.NO_TENANT);
	}

	final int country = ToolboxSysKeys.GeoAreaTypes.COUNTRY;
	final int city = ToolboxSysKeys.GeoAreaTypes.CITY;
	final int street = ToolboxSysKeys.GeoAreaTypes.STREET;

	public List<GeoArea> findAllSettlements() {
		String findAllSettlementsQuery = "SELECT * FROM geo_area WHERE geo_area_type = " + city;
		return jdbcTemplate.query(findAllSettlementsQuery, newRowMapperInstance());
	}

	public List<GeoArea> findAllSettlementsByParent(Integer parentId) {
		String findAllSettlementsByParentQuery = "SELECT * FROM geo_area WHERE geo_area_type = " + city + " AND parent_area = " + parentId;
		return jdbcTemplate.query(findAllSettlementsByParentQuery, newRowMapperInstance());
	}

	public List<GeoArea> findAllCountries() {
		String findAllCountriesQuery = "SELECT * FROM geo_area WHERE geo_area_type = " + country;
		return jdbcTemplate.query(findAllCountriesQuery, newRowMapperInstance());
	}

	public List<GeoArea> findAllStreets() {
		String findAllStreetsQuery = "SELECT * FROM geo_area WHERE parent_area IS NOT NULL AND geo_area_type = " + street;
		return jdbcTemplate.query(findAllStreetsQuery, newRowMapperInstance());
	}

	public List<GeoArea> findAllStreetsByParent(Integer parentId) {
		String findAllStreetsByParentQuery = "SELECT * FROM geo_area WHERE parent_area IS NOT NULL AND geo_area_type = " + street + " AND parent_area = " + parentId;
		return jdbcTemplate.query(findAllStreetsByParentQuery, newRowMapperInstance());
	}

}
