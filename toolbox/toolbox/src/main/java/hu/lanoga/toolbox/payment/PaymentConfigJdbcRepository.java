package hu.lanoga.toolbox.payment;

import java.util.TreeSet;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Repository;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;

@ConditionalOnMissingBean(name = "paymentConfigJdbcRepositoryOverrideBean")
@Repository
public class PaymentConfigJdbcRepository extends DefaultJdbcRepository<PaymentConfig> {

	/**
	 * @see ToolboxSysKeys.PaymentProvider
	 */
	public TreeSet<Integer> findAllEnabledPaymentProvider() {

		try {
			return new TreeSet<>(this.jdbcTemplate.queryForList(fillVariables("SELECT payment_provider FROM payment_config WHERE use_global_config = false AND enabled = true AND #tenantCondition " +
					" UNION " +
					" SELECT payment_provider FROM payment_config WHERE use_global_config = true AND enabled = true AND #tenantCondition AND payment_provider IN (SELECT payment_provider FROM payment_config WHERE enabled = true AND tenant_id = 1)" +
					" ORDER BY payment_provider ASC"), Integer.class));
		} catch (final IncorrectResultSizeDataAccessException e) {
			return null;
		}

	}
	
}
