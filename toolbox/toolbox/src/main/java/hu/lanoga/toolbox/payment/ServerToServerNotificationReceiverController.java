package hu.lanoga.toolbox.payment;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;

import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.util.BrandUtil;
import hu.lanoga.toolbox.util.ToolboxAssert;

public abstract class ServerToServerNotificationReceiverController {
	
	protected static final String S2S_URL = "public/payment/{paymentTransactionGid}/s2s/";

	public static String buildServerToServerNotificationReceiverControllerUrl(final PaymentTransaction paymentTransaction) {

		ToolboxAssert.notNull(paymentTransaction.getGid());

		return BrandUtil.getRedirectUriHostBackend() +
				StringUtils.replace(ServerToServerNotificationReceiverController.S2S_URL, "{paymentTransactionGid}", paymentTransaction.getGid());

	}
	
	protected void clearHttpSession(HttpServletRequest httpServletRequest, boolean createNewSession) {

		// TODO: meg kellene próbálni a SpringSecurity .sessionCreationPolicy(SessionCreationPolicy.STATELESS)-et erre az pár endpointra (nem egyszerű összelőni, hogy a több rész (UI) ne romoljon el)
		// addig azért van itt biztos, ami biztos a session.invalidate()...

		// TODO: van már a SecurityUtil-ban is egy hasonló metódus.. azzal összehozni?

		SecurityUtil.clearAuthentication();
		
		final HttpSession session = httpServletRequest.getSession(false);

		if (session != null) {

			session.invalidate();

			if (createNewSession) {
				httpServletRequest.getSession(true);
			}

		}
	}


	// --------------------------------

	@Autowired
	protected PaymentTransactionService paymentTransactionService;

	@Autowired
	protected PaymentManager paymentManager;
	
	protected Object handleServerToServerNotification(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse, final PaymentTransaction paymentTransaction) {

		SecurityUtil.limitAccessSystem();
		
		ToolboxAssert.notNull(paymentTransaction);
		
		// ---

		try {
			JdbcRepositoryManager.setTlTenantId(paymentTransaction.getTenantId());
			return this.paymentManager.processServerToServerNotification(paymentTransaction, httpServletRequest, httpServletResponse);
		} finally {
			JdbcRepositoryManager.clearTlTenantId();
		}

	}

	public void receive(@PathVariable("paymentTransactionGid") final String paymentTransactionGid,
			final HttpServletRequest httpServletRequest,
			final HttpServletResponse httpServletResponse) {
		
		clearHttpSession(httpServletRequest, true);

		try {
			
			SecurityUtil.setSystemUser();

			final PaymentTransaction paymentTransaction = this.paymentTransactionService.findByGid(paymentTransactionGid);

			handleServerToServerNotification(httpServletRequest, httpServletResponse, paymentTransaction);

		} finally {
			clearHttpSession(httpServletRequest, false);
		}

	}

}