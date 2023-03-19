package hu.lanoga.toolbox.payment;

import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.SearchCriteriaOperation;
import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import hu.lanoga.toolbox.filter.internal.SearchCriteria;
import hu.lanoga.toolbox.service.AdminOnlyCrudService;
import hu.lanoga.toolbox.service.LazyEnhanceCrudService;
import hu.lanoga.toolbox.util.ToolboxAssert;

@ConditionalOnMissingBean(name = "paymentTransactionRawLogServiceOverrideBean")
@Service
public class PaymentTransactionRawLogService extends AdminOnlyCrudService<PaymentTransactionRawLog, PaymentTransactionRawLogJdbcRepository> implements LazyEnhanceCrudService<PaymentTransactionRawLog> {

	@Override
	public PaymentTransactionRawLog enhance(final PaymentTransactionRawLog paymentTransactionRawLog) {
		
		String abbreviatedStr = StringUtils.abbreviate(paymentTransactionRawLog.getLogData(), 200);
		abbreviatedStr = abbreviatedStr.replaceAll("\r", "");
		abbreviatedStr = abbreviatedStr.replaceAll("\n", "[br]");

		paymentTransactionRawLog.setLogDataShort(abbreviatedStr);
		
		return paymentTransactionRawLog;
		
	}

	@Override
	// @Transactional(propagation = Propagation.REQUIRES_NEW) // TODO: gazos, pl. mapfre-ben... most csak akkor megy jól, ha az egész egy tranz. (nincs második vagy al trans.)
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public PaymentTransactionRawLog save(final PaymentTransactionRawLog t) {
		ToolboxAssert.isTrue(t.isNew());
		return super.save(t);
	}

	@Override
	public void delete(final int id) {
		throw new UnsupportedOperationException();
	}

	public List<PaymentTransactionRawLog> findByPaymentTransactionIdAndText(final int paymentTransactionId, final String text) {
		return this.findByPaymentTransactionIdAndText(paymentTransactionId, text, SearchCriteriaOperation.LIKE);
	}
	
	public List<PaymentTransactionRawLog> findByPaymentTransactionIdAndText(final int paymentTransactionId, final String text, final SearchCriteriaOperation likeSearchCriteriaOperation) {

		ToolboxAssert.isTrue(StringUtils.isNotBlank(text));
		ToolboxAssert.notNull(likeSearchCriteriaOperation);
		
		// ---
		
		final LinkedHashSet<SearchCriteria> searchCriteriaSet = new LinkedHashSet<>();
		
		{
			final SearchCriteria searchCriteria = SearchCriteria.builder().criteriaType(Integer.class).fieldName("paymentTransactionId").operation(SearchCriteriaOperation.EQ).value(paymentTransactionId).build();
			searchCriteriaSet.add(searchCriteria);
		}
		
		{
			final SearchCriteria searchCriteria = SearchCriteria.builder().criteriaType(String.class).fieldName("logData").operation(likeSearchCriteriaOperation).value(text).build();
			searchCriteriaSet.add(searchCriteria);
		}
		
		return this.repository.findAll(new BasePageRequest<>(searchCriteriaSet)).getContent();
		
	}

}
