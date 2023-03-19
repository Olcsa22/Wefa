package hu.lanoga.toolbox.payment;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.PaymentTransactionStatus;

@ConditionalOnMissingBean(name = "irlPaymentProviderServiceOverrideBean")
@ConditionalOnProperty(name = "tools.payment.provider.irl.enabled", matchIfMissing = true)
@Service
public class IrlPaymentProviderService extends AbstractPaymentProviderService {

	@Override
	protected PaymentTransaction doDepositInner(final PaymentTransaction paymentTransaction) throws Exception {

		paymentTransaction.setStatus(PaymentTransactionStatus.PENDING);
		paymentTransaction.setManualStatusChange(true);
		paymentTransaction.setRawId(UUID.randomUUID().toString());

		return paymentTransaction;

	}

	@Override
	protected PaymentTransaction doRefundInner(final PaymentTransaction paymentTransaction, final PaymentTransaction referredTransaction) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	protected PaymentTransaction checkTransactionStatusInner(final PaymentTransaction paymentTransaction) throws Exception {
		paymentTransaction.setStatus(PaymentTransactionStatus.RECEIVED);
		return paymentTransaction;
	}

	@Override
	public int getPaymentProviderCodeStoreItemId() {
		return ToolboxSysKeys.PaymentProvider.IRL;
	}

	@Override
	public boolean hasAuthFeature() {
		return false;
	}

	@Override
	public boolean hasCaptureFeature() {
		return false;
	}

	@Override
	public boolean hasPurchaseFeature() {
		return true;
	}

	@Override
	public boolean hasRefundFeature() {
		return false;
	}

	@Override
	public boolean hasVoidFeature() {
		return false;
	}

	@Override
	public boolean hasWithdrawFeature() {
		return false;
	}
	
	@Override
	public boolean hasServerToServerNotificationProcessor() {
		return false;
	}

	@Override
	public boolean hasReturnUrlProcessor() {
		return false;
	}

	@Override
	protected PaymentTransaction doCaptureInner(final PaymentTransaction paymentTransaction, final PaymentTransaction referredTransaction) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	protected PaymentTransaction doVoidInner(final PaymentTransaction paymentTransaction, final PaymentTransaction referredTransaction) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	protected PaymentTransaction doWithdrawInner(final PaymentTransaction paymentTransaction) throws Exception {
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected Object processServerToServerNotificationInner(PaymentTransaction paymentTransaction, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected PaymentTransaction processReturnUrlInner(PaymentTransaction paymentTransaction, HttpServletRequest httpServletRequest) {
		throw new UnsupportedOperationException();
	}
}
