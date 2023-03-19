package hu.lanoga.toolbox.payment;

import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.codestore.CodeStoreItemService;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.service.AdminOnlyCrudService;
import hu.lanoga.toolbox.service.LazyEnhanceCrudService;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.user.UserJdbcRepository;

@ConditionalOnMissingBean(name = "paymentConfigServiceOverrideBean")
@Service
public class PaymentConfigService extends AdminOnlyCrudService<PaymentConfig, PaymentConfigJdbcRepository> implements LazyEnhanceCrudService<PaymentConfig> {

	@Autowired
	private UserJdbcRepository userJdbcRepository;

	@Autowired
	private CodeStoreItemService codeStoreItemService;

	@Autowired
	private FileStoreService fileStoreService;

	@Override
	public PaymentConfig enhance(PaymentConfig paymentConfig) {

		if (paymentConfig != null) {

			if (paymentConfig.getPaymentProvider() != null) {
				paymentConfig.setPaymentProviderCaption(codeStoreItemService.findOne(paymentConfig.getPaymentProvider()).getCaptionCaption());
			}

			if (paymentConfig.getCreatedBy() != null) {
				paymentConfig.setCreatedByCaption(I18nUtil.buildFullName(this.userJdbcRepository.findOne(paymentConfig.getCreatedBy()), true, false));
			}

			if (paymentConfig.getModifiedBy() != null) {
				paymentConfig.setModifiedByCaption(I18nUtil.buildFullName(this.userJdbcRepository.findOne(paymentConfig.getModifiedBy()), true, false));
			}

		}

		return paymentConfig;
	}

	/**
	 * @see ToolboxSysKeys.PaymentProvider
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public TreeSet<Integer> findAllEnabledPaymentProvider() {
		return repository.findAllEnabledPaymentProvider();
	}

	/**
	 * {@link PaymentConfig#getUseGlobalConfig()} figyelembe vételével
	 * 
	 * @param paymentProviderCsiId
	 * @return
	 * 
	 * @see ToolboxSysKeys.PaymentProvider
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public PaymentConfig findOneForPaymentProvider(int paymentProviderCsiId) {
		PaymentConfig paymentConfig = repository.findOneBy("paymentProvider", paymentProviderCsiId);

		// ---

		if (Boolean.TRUE.equals(paymentConfig.getUseGlobalConfig())) {

			ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();
			Integer tlTenantId = JdbcRepositoryManager.getTlTenantId();

			try {

				SecurityUtil.setSystemUser();
				JdbcRepositoryManager.setTlTenantId(1);

				paymentConfig = repository.findOneBy("paymentProvider", paymentProviderCsiId);

			} finally {

				if (tlTenantId != null) {
					JdbcRepositoryManager.setTlTenantId(tlTenantId);
				} else {
					JdbcRepositoryManager.clearTlTenantId();
				}

				SecurityUtil.setUser(loggedInUser);

			}

		}

		// ---

		return paymentConfig;
	}

	@Override
	public PaymentConfig save(PaymentConfig paymentConfig) {

		if (Boolean.TRUE.equals(paymentConfig.getUseGlobalConfig())) {
			SecurityUtil.limitAccessSuperAdmin();
		}

		if (paymentConfig.isNew()) {
			this.fileStoreService.setToNormal(paymentConfig.getFileIds());
		} else {
			final PaymentConfig oldPaymentConfig = this.findOne(paymentConfig.getId());
			this.fileStoreService.setToNormalOrDelete(oldPaymentConfig.getFileIds(), paymentConfig.getFileIds());
		}

		return super.save(paymentConfig);
	}
}
