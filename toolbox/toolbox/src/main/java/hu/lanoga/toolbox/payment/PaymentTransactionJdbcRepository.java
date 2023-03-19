package hu.lanoga.toolbox.payment;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;
import hu.lanoga.toolbox.spring.SecurityUtil;

@ConditionalOnMissingBean(name = "paymentTransactionJdbcRepositoryOverrideBean")
@Repository
public class PaymentTransactionJdbcRepository extends DefaultJdbcRepository<PaymentTransaction> {

	/**
	 * tenant független lekérés (óvatosan vele!)
	 *
	 * @param paymentTransactionGid
	 * @return null, ha nincs ilyen...
	 */
	public PaymentTransaction findByGid(final String paymentTransactionGid) {

		try {
			final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("gid", paymentTransactionGid);
			return namedParameterJdbcTemplate.queryForObject("SELECT * FROM payment_transaction WHERE gid = :gid", namedParameters, newRowMapperInstance());
		} catch (final IncorrectResultSizeDataAccessException e) {
			return null;
		}

	}

	public List<PaymentTransaction> findAllForCheckStatusScheduler() {

		lockTable();

		// ---

		String reservedFor = StringUtils.defaultIfBlank(SecurityUtil.getPhysicalAddressHex(), "na");
		reservedFor += "-" + UUID.randomUUID().toString();
		
		// ---

		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("s", ToolboxSysKeys.PaymentTransactionStatus.PENDING).addValue("reservedFor", reservedFor);
		namedParameterJdbcTemplate.update(fillVariables(
				"UPDATE payment_transaction SET last_status_check_reserved_for = :reservedFor, last_status_check_request_on = NOW() "
				+ "WHERE tenant_id = #tenantParam "
				+ "AND status_check_disabled_poll IS DISTINCT FROM true "
				+ "AND created_on <= NOW() - interval '3 minute' " // 3 percnél régebbi
				+ "AND created_on > NOW() - interval '5 day' " // de 5 napnál újabb
				+ "AND ((last_status_check_request_on IS NULL) OR ("
				+ "last_status_check_request_on <= NOW() - ("
				+ "EXTRACT(EPOCH FROM (NOW() - created_on)) / 2 * interval '1 second'" 
				
				// EPOCH = másodperc extract; 
				//
				// amit itt csinálunk az egy lináris sorozat, ellenőrzések ideje (tf., hogy a job percenként fut): 
				// perc1: rekord létrejön (ez lesz a created_on)
				// perc1: job fut, nem foglkozik vele (mert created_on <= NOW() - interval '3 minute' " nem igaz)
				// perc2: job fut, nem foglkozik vele (mert created_on <= NOW() - interval '3 minute' " nem igaz)
				// perc3: job fut, nem foglkozik vele (mert created_on <= NOW() - interval '3 minute' " nem igaz)
				// perc4: job fut... created_on <= NOW() - interval '3 minute már igaz... ellenőrzi a CIB stb. banknál...(még pending), 
				// beírja a last_status_check_request_on-t mostra (tehát last_status_check_request_on = perc4)
				// perc5: job fut, számítás/kérdés: perc4 <= perc5 - (perc5 - perc1)/2, nem igaz
				// perc6: job fut, számítás/kérdés: perc4 <= perc6 - (perc6 - perc1)/2, nem igaz
				// perc7: job fut, számítás/kérdés: perc4 <= perc7 - (perc7 - perc1)/2, igaz, teszt/check, új last_status_check_request_on perc7 lesz
				// perc8: perc7 <= perc8 - (perc8 - perc1)/2, hamis
				// perc9: perc7 <= perc9 - (perc9 - perc1)/2, hamis
				// perc10: perc7 <= perc10 - (perc10 - perc1)/2, hamis
				// perc11: perc7 <= perc11 - (perc11 - perc1)/2, hamis
				// perc12: perc7 <= perc12 - (perc12 - perc1)/2, hamis
				// perc13: perc7 <= perc13 - (perc13 - perc1)/2, igaz
				//
				// sorozat elemei: 4, 7, 13, 25, 49, 97, 193, 385, 769, 1537, 3073, ~6000 perc (after created_on)
				
				+ ")"
				+ ")) "
				+ "AND status = :s"), namedParameters);

		// ---
		
		final SqlParameterSource namedParametersForSelect = new MapSqlParameterSource().addValue("reservedFor", reservedFor);
		return namedParameterJdbcTemplate.query(fillVariables(
				"SELECT * FROM payment_transaction WHERE tenant_id = #tenantParam "
				+ "AND last_status_check_reserved_for = :reservedFor"), namedParametersForSelect, newRowMapperInstance());

	}
	
}
