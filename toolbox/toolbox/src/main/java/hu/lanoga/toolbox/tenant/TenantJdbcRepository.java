package hu.lanoga.toolbox.tenant;

import java.util.LinkedHashSet;
import java.util.concurrent.TimeUnit;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.RepositoryTenantMode;
import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import hu.lanoga.toolbox.filter.internal.SearchCriteria;
import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;
import hu.lanoga.toolbox.util.ToolboxAssert;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class TenantJdbcRepository extends DefaultJdbcRepository<Tenant> {

	public TenantJdbcRepository() {
		super(RepositoryTenantMode.NO_TENANT);
	}

	@Override
	public String getInnerSelect() {
		return "SELECT t.*, " +
				"ti.email, ti.phone, ti.company_tax_number, ti.company_registration_number, ti.company_bank_account_number, ti.company_address_country, ti.company_address_state, ti.company_address_county, ti.company_address_zip_code, ti.company_address_details, ti.contact_name, ti.note, ti.extra_data_1, ti.extra_data_2, ti.extra_data_3, ti.extra_data_4, ti.extra_data_5, ti.extra_data_6, ti.extra_data_7, ti.extra_data_8, ti.extra_data_9, ti.extra_data_10, ti.file_ids, ti.file_ids_2, ti.file_ids_3, ti.file_ids_4, ti.file_ids_5, ti.file_ids_6, ti.file_ids_7, ti.company_name " +
				"FROM tenant t LEFT JOIN tenant_info ti ON t.id = ti.tenant_id";
	}

	// --------------------------------------------------------------------------------------------------------------------------
	// "kézi" tenant paraméteres metódusok:

	public void createTenant(final String tenantName, final String tenantEmail, final String passwordForAdmin) {

		ToolboxAssert.notNull(tenantName);
		ToolboxAssert.notNull(tenantEmail);
		ToolboxAssert.notNull(passwordForAdmin);

		log.info("create tenant: " + tenantName);

		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("tenantName", tenantName).addValue("tenantEmail", tenantEmail).addValue("passwordForAdmin", passwordForAdmin);
		namedParameterJdbcTemplate.queryForObject("SELECT create_tenant(:tenantName, :tenantEmail, :passwordForAdmin)", namedParameters, String.class);

	}

	public void createTenantTest() {

		log.info("create tenant (test)");

		final SqlParameterSource namedParameters = new MapSqlParameterSource();
		namedParameterJdbcTemplate.queryForObject("SELECT create_tenant_test()", namedParameters, String.class);

	}

	public int findLastTenantId() {
		return jdbcTemplate.queryForObject("SELECT MAX(ID) FROM tenant", Integer.class);

	}

	public Tenant findByTenantName(String name) {

		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("name", name);

		return namedParameterJdbcTemplate.queryForObject(fillVariables("SELECT * FROM tenant WHERE tenant.name = :name", false), namedParameters, newRowMapperInstance());
	}

	// ---

	private static Cache<Integer, Boolean> isTenantEnabledCache = CacheBuilder.newBuilder().concurrencyLevel(10).expireAfterWrite(3, TimeUnit.MINUTES).build();

	/**
	 * @param id
	 * @param noCache
	 * 		true = direktben DB-ből nézze meg
	 * @return
	 */
	public boolean isTenantEnabled(final int id, boolean noCache) {

		if (noCache) {

			boolean b = isTenantEnabledInner(id);
			isTenantEnabledCache.put(id, b);
			return b;
			
		} else {

			try {
				return isTenantEnabledCache.get(id, () -> isTenantEnabledInner(id));
			} catch (final Exception e) {
				log.warn("TenantJdbcRepository isTenantEnabled() cache error", e);
				return isTenantEnabledInner(id);
			}

		}

	}

	private boolean isTenantEnabledInner(final int id) {

		final SearchCriteria searchCriteria1 = SearchCriteria.builder()
				.criteriaType(java.sql.Timestamp.class)
				.fieldName("id")
				.operation(ToolboxSysKeys.SearchCriteriaOperation.EQ)
				.value(id)
				.build();

		final SearchCriteria searchCriteria2 = SearchCriteria.builder()
				.criteriaType(java.sql.Timestamp.class)
				.fieldName("enabled")
				.operation(ToolboxSysKeys.SearchCriteriaOperation.EQ)
				.value(true)
				.build();

		final LinkedHashSet<SearchCriteria> searchCriteriaSet = new LinkedHashSet<>();
		searchCriteriaSet.add(searchCriteria1);
		searchCriteriaSet.add(searchCriteria2);

		BasePageRequest<Tenant> basePageRequest = new BasePageRequest<>(searchCriteriaSet);
		basePageRequest.setDoQuery(false);
		basePageRequest.setDoCount(true);

		long totalElements = this.findAll(basePageRequest).getTotalElements();

		return (totalElements == 1L);

	}

}