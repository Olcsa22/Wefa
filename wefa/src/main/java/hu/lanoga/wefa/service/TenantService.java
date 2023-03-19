package hu.lanoga.wefa.service;

import org.springframework.stereotype.Service;

@Service("tenantServiceOverrideBean")
public class TenantService extends hu.lanoga.toolbox.tenant.TenantService {

	@Override
	protected void init2() {
		// itt direkt nincs a szokásos test-tenant
	}
}
