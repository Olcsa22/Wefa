package hu.lanoga.toolbox.tenant;

import java.util.List;
import java.util.concurrent.Callable;

import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantUtil {

	/**
	 * csak system user számára...
	 * 
	 * @param callable
	 */
	public static void runWithEachTenant(final Callable<Void> callable) {

		SecurityUtil.limitAccessSystemOrSuperUser();

		// ---

		final List<Tenant> tenants = ApplicationContextHelper.getBean(TenantJdbcRepository.class).findAll(); 

		for (final Tenant tenant : tenants) {
			
			if (!Boolean.TRUE.equals(tenant.getEnabled())) {
				continue;
			}

			try {

				JdbcRepositoryManager.setTlTenantId(tenant.getId());

				callable.call();

			} catch (final Exception e) {

				log.error("runWithEachTenant error!", e);

			} finally {
				JdbcRepositoryManager.clearTlTenantId();
			}

		}

	}
	
	private TenantUtil() {
		//
	}

}
