package hu.lanoga.toolbox.payment;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.codestore.CodeStoreItemService;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.service.DefaultCrudService;
import hu.lanoga.toolbox.service.LazyEnhanceCrudService;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.user.UserService;
import hu.lanoga.toolbox.util.DateTimeUtil;

/**
 * backoffice és job-ok részére...
 */
@ConditionalOnMissingBean(name = "paymentTransactionServiceOverrideBean")
@Service
public class PaymentTransactionService extends DefaultCrudService<PaymentTransaction, PaymentTransactionJdbcRepository> implements LazyEnhanceCrudService<PaymentTransaction> {

	@Autowired
	private CodeStoreItemService codeStoreItemService;

	@Autowired
	private UserService userService;

	@Autowired
	private FileStoreService fileStoreService;

	@Override
	public PaymentTransaction enhance(PaymentTransaction paymentTransaction) {

		if (paymentTransaction != null) {
			if (paymentTransaction.getPaymentProvider() != null) {
				paymentTransaction.setPaymentProviderCaption(codeStoreItemService.findOne(paymentTransaction.getPaymentProvider()).getCaptionCaption());
			}

			if (paymentTransaction.getPaymentOperationType() != null) {
				paymentTransaction.setPaymentOperationTypeCaption(codeStoreItemService.findOne(paymentTransaction.getPaymentOperationType()).getCaptionCaption());
			}

			if (paymentTransaction.getStatus() != null) {
				paymentTransaction.setStatusCaption(codeStoreItemService.findOne(paymentTransaction.getStatus()).getCaptionCaption());
			}

			if (paymentTransaction.getCreatedBy() != null) {
				paymentTransaction.setCreatedByCaption(I18nUtil.buildFullName(this.userService.findOne(paymentTransaction.getCreatedBy()), true, false));
			}

			if (paymentTransaction.getModifiedBy() != null) {
				paymentTransaction.setModifiedByCaption(I18nUtil.buildFullName(this.userService.findOne(paymentTransaction.getModifiedBy()), true, false));
			}

		}

		return paymentTransaction;
	}

	@Override
	public void delete(final int id) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public PaymentTransaction findOne(int id) {
		
		PaymentTransaction paymentTransaction = super.findOne(id);

		if (paymentTransaction != null) {
			SecurityUtil.checkAgainstLcuGidIfLcu(paymentTransaction.getLcuGid());
		}
		
		return paymentTransaction;
	}

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public PaymentTransaction save(PaymentTransaction paymentTransaction) {

		if (paymentTransaction.isNew()) {
			this.fileStoreService.setToNormal(paymentTransaction.getFileIds());
		} else {

			SecurityUtil.checkAgainstLcuGidIfLcu(paymentTransaction.getLcuGid());

			final PaymentTransaction oldPaymentTransaction = this.findOne(paymentTransaction.getId());
			this.fileStoreService.setToNormalOrDelete(oldPaymentTransaction.getFileIds(), paymentTransaction.getFileIds());
		}

		return super.save(paymentTransaction);
	}

	/**
	 * tenant független lekérés (óvatosan vele!)
	 * 
	 * @param paymentTransactionGid
	 * @return
	 * 		null, ha nincs ilyen...
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public PaymentTransaction findByGid(final String paymentTransactionGid) {
		return this.repository.findByGid(paymentTransactionGid);
	}

	public List<PaymentTransaction> findAllForCheckStatusScheduler() {

		SecurityUtil.limitAccessSystem();

		// ---

		return this.repository.findAllForCheckStatusScheduler();

	}

	public List<PaymentTransaction> findAllByReferredTransactionId(final int referredTransactionId) {
		return this.repository.findAllBy("referredTransactionId", referredTransactionId);
	}
	
	public List<PaymentTransaction> findAllForOrderId(final String orderId) {
		return this.repository.findAllBy("orderId", orderId);
	}
	
	/**
	 * @param orderId
	 * @return
	 * 		left: true = van-e sikeres tranzakció már
	 * 		right: true = van-e folyamatban lévő tranzakció még
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR)
	public Pair<Boolean, Boolean> shouldTransactionStartUiHalt(final String orderId) {
		
		List<PaymentTransaction> list = this.repository.findAllBy("orderId", orderId, "status", ToolboxSysKeys.PaymentTransactionStatus.SUCCESS);
		
		if (!list.isEmpty()) {
			return Pair.of(Boolean.TRUE, Boolean.FALSE);
		}
		
		list = this.repository.findAllBy("orderId", orderId);
		
		if (list.isEmpty()) {
			return Pair.of(Boolean.FALSE, Boolean.FALSE);
		}
		
		PaymentTransaction paymentTransaction = list.get(list.size() - 1);
		
		if (paymentTransaction.getModifiedOn().getTime() > (System.currentTimeMillis() - DateTimeUtil.DAY_IN_MS) && paymentTransaction.getStatus().intValue() < 1730) {
			return Pair.of(Boolean.FALSE, Boolean.TRUE);
		}
		
		return Pair.of(Boolean.FALSE, Boolean.FALSE);
		
	}

}
