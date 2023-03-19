package hu.lanoga.toolbox.user;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.spring.ToolboxUserDetailsService;
import hu.lanoga.toolbox.tenant.Tenant;
import hu.lanoga.toolbox.tenant.TenantJdbcRepository;
import hu.lanoga.toolbox.util.BrandUtil;
import hu.lanoga.toolbox.util.ToolboxAssert;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Security-hez...
 */
@Slf4j
@ConditionalOnMissingBean(name = "toolboxUserDetailsServiceOverrideBean")
@Service
public class DefaultToolboxUserDetailsService implements ToolboxUserDetailsService {

	@Autowired
	private UserJdbcRepository userJdbcRepository;

	@Autowired
	private TenantJdbcRepository tenantJdbcRepository;

	@Value("${tools.tenant.default-login-tenant-id}")
	private String defaultLoginTenantId;

	@Value("${tools.tenant.allow-login-with-smart-search-tenant-id}")
	private boolean allowLoginWithSmartSearchTenantId;

	/**
	 * Spring Security-nek kell ez, ebben a formában (direktben) API-ról ne legyen elérhető (rendkívüli ritka kivételek lehetnek)! 
	 * Tenant id-t (pl.: 11:jdoe) és nevet (sample-tenant/jdoe) kezel a username-ben.
	 */
	@Override
	public ToolboxUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		// erre nem kell / nem lehet @Secured(), ez a metódus tölti be a User-t login után

		ToolboxAssert.isTrue(StringUtils.isNotEmpty(username));

		Integer tenantId;
		final Tenant tenant;

		if (username.contains("/")) {

			final String[] split = username.split("/");

			final String tenantName = split[0];
			username = split[1];

			tenant = this.tenantJdbcRepository.findOneBy("name", tenantName);

			if (tenant == null) {
				log.debug("missing tenant: " + tenantName);
				throw new UsernameNotFoundException(username);
			}

			if (!Boolean.TRUE.equals(tenant.getEnabled())) {
				throw new UsernameNotFoundException("tenant is not enabled: " + tenant.getName());
			}

			tenantId = tenant.getId();

		} else if (username.contains(":")) {

			final String[] split = username.split(":");

			tenantId = Integer.parseInt(split[0]);
			username = split[1];

			tenant = this.tenantJdbcRepository.findOne(tenantId);

			if (tenant == null) {
				log.debug("missing tenant: " + tenantId);
				throw new UsernameNotFoundException(username);
			}
			
			if (!Boolean.TRUE.equals(tenant.getEnabled())) {
				throw new UsernameNotFoundException("tenant is not enabled: " + tenant.getName());
			}

		} else if (StringUtils.isNotBlank(this.defaultLoginTenantId)) {

			tenantId = null;

			try {

				if (this.defaultLoginTenantId.contains(",")) {

					final String brand = BrandUtil.getBrand(false, true);

					final String[] split1 = this.defaultLoginTenantId.split(";");
					for (final String s : split1) {
						final String[] split2 = s.split(",");
						if (brand.equalsIgnoreCase(split2[0])) {
							tenantId = Integer.parseInt(split2[1]);
							break;
						}
					}

				} else {

					tenantId = Integer.parseInt(this.defaultLoginTenantId);

				}

			} catch (final Exception e) {
				log.debug("missing tenant part from username (and defaultLoginTenantId is not a set correctly): " + username);
				throw new UsernameNotFoundException(username);
			}

			ToolboxAssert.notNull(tenantId);

			tenant = this.tenantJdbcRepository.findOne(tenantId);

			if (tenant == null) {
				log.debug("missing tenant (defaultLoginTenantId): " + tenantId);
				throw new UsernameNotFoundException(username);
			}
			
		} else {

			log.debug("missing tenant part from username (and defaultLoginTenantId is not set): " + username);
			throw new UsernameNotFoundException(username);

		}

		// ---

		User user;

		if (ToolboxSysKeys.UserAuth.SYSTEM_USERNAME.equalsIgnoreCase(username)) {

			user = this.userJdbcRepository.findSystemUser();

			if (user == null) {
				throw new UsernameNotFoundException(username); // Spring Security ezt az exception típust várja el ("if the user could not be found or the user has no GrantedAuthority")
			}
		} else if (ToolboxSysKeys.UserAuth.LCU_SYSTEM_USERNAME.equalsIgnoreCase(username)) {
				
				user = this.userJdbcRepository.findLcuSystemUser();
				
				if (user == null) {
					throw new UsernameNotFoundException(username); // Spring Security ezt az exception típust várja el ("if the user could not be found or the user has no GrantedAuthority")
				}

		} else if (ToolboxSysKeys.UserAuth.ANONYMOUS_USERNAME.equalsIgnoreCase(username)) {
			
			user = this.userJdbcRepository.findAnonymousUser();

			if (user == null) {
				throw new UsernameNotFoundException(username);
			}

		} else {

			user = this.userJdbcRepository.findUser(tenantId, username);
			
			if (user == null && this.allowLoginWithSmartSearchTenantId) {
				user = this.userJdbcRepository.findUserWithoutTenantId2(username);
			}
			
			if ((user == null) || (user.getAuthorities() == null) || user.getAuthorities().isEmpty()) {
				throw new UsernameNotFoundException(username); // Spring Security ezt az exception típust várja el ("if the user could not be found or the user has no GrantedAuthority")
			}
			
		}
		
		SecurityUtil.limitAccessDisabled(user, true);

		return user;
	}

}
