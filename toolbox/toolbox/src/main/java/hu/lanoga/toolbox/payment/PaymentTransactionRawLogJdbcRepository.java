package hu.lanoga.toolbox.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;

@ConditionalOnMissingBean(name = "paymentTransactionRawLogJdbcRepositoryOverrideBean")
@Repository
public class PaymentTransactionRawLogJdbcRepository extends DefaultJdbcRepository<PaymentTransactionRawLog> {

	//
	
}
