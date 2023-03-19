package hu.lanoga.toolbox.tenant;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.UserAuth;
import hu.lanoga.toolbox.config.ApplicationEventHandlerConfig;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.service.AdminOnlyCrudService;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.user.User;
import hu.lanoga.toolbox.user.UserJdbcRepository;
import hu.lanoga.toolbox.util.ToolboxGroovyHelper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ConditionalOnMissingBean(name = "tenantServiceOverrideBean")
@Service
public class TenantService extends AdminOnlyCrudService<Tenant, TenantJdbcRepository> {

	@Autowired
	private TenantInfoJdbcRepository tenantInfoJdbcRepository;

	@Autowired
	private Environment springEnvironment;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserJdbcRepository userJdbcRepository;

	@Value("${tools.tenant.create-when-fresh.list:}")
	private String createWhenFreshList;

	@Value("${tools.tenant.init.groovy-script.location:}")
	private String tenantInitGroovyScriptLocation;

	@Autowired
	private FileStoreService fileStoreService;

	/**
	 * ha még nincs egy "rendes" tenant sem (értsd nem spec., nem a "common" tenant stb.), 
	 * tehát első érdemi indulás... 
	 * @return
	 */
	protected boolean isFresh() {
		return !this.repository.exists(11);
	}

	/**
	 * (ne hívd meg kézzel)
	 * 
	 * @see ApplicationEventHandlerConfig
	 */
	@Transactional
	public void init() {

		SecurityUtil.limitAccessSystem();

		if (this.isFresh()) {

			this.init1();
			this.init2();
			this.init3();

		}
	}

	protected void init1() {

		//

	}

	/**
	 * test tenant létrehozás...
	 */
	protected void init2() {

		if (this.springEnvironment.acceptsProfiles("dev", "unit-test")) {

			try {

				log.info("creating test tenant...");

				this.repository.createTenantTest();

				// ---

				final int tenantId = this.repository.findLastTenantId();

				try {

					JdbcRepositoryManager.setTlTenantId(tenantId);

					this.afterTestTenantCreate(this.repository.findLastTenantId());

				} finally {
					JdbcRepositoryManager.clearTlTenantId();
				}

				// ---

				log.info("test tenant was created");

			} catch (final Exception e) {
				log.error("could not create test tenant", e);
				throw new ToolboxGeneralException("could not create test tenant", e);
			}

		}

	}

	/**
	 * tools.tenant.create-when-fresh-list tenant-ok létrehozása (ha van)
	 */
	protected void init3() {

		if (StringUtils.isNotBlank(this.createWhenFreshList)) {

			final String[] s1 = this.createWhenFreshList.split(";");

			for (final String s2 : s1) {

				final String[] s3 = s2.split(",");

				log.info("init3: " + Arrays.toString(s3));

				final Tenant t = new Tenant();
				t.setName(s3[0]);
				t.setEmail(s3[1]);
				t.setPhone(s3[2]);
				t.setEnabled(Boolean.parseBoolean(s3[3]));
				this.save(t);

			}

		}

	}

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR)
	public void delete(final int id) {
		super.delete(id);
	}

	@Override
	@Secured(UserAuth.ROLE_LCU_STR)
	public Tenant findOne(final int id) {

		if (!(SecurityUtil.hasAnyRole(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR) || SecurityUtil.isSystem() || SecurityUtil.isSameTenant(id))) {
			throw new AccessDeniedException(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR + ", system user, or same tenant is required!");
		}

		return super.findOne(id);
	}
	
	/**
	 * tenant id mentes metódus, system usernek...
	 * 
	 * @param str
	 * @return
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public Tenant findOneByExtraData1(final String str) {
		SecurityUtil.limitAccessSystem();
		return repository.findOneBy("extraData1", str);
	}

	/**
	 * tenant id mentes metódus, system usernek...
	 *
	 * @param str
	 * @return
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public Tenant findOneByExtraData2(final String str) {
		SecurityUtil.limitAccessSystem();
		return repository.findOneBy("extraData2", str);
	}

	/**
	 * tenant id mentes metódus, system usernek...
	 *
	 * @param str
	 * @return
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public Tenant findOneByExtraData3(final String str) {
		SecurityUtil.limitAccessSystem();
		return repository.findOneBy("extraData3", str);
	}
	
	/**
	 * tenant id mentes metódus, system usernek...
	 *
	 * @param str
	 * @return
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public Tenant findOneByExtraData4(final Integer t) {
		SecurityUtil.limitAccessSystem();
		return repository.findOneBy("extraData4", t);
	}
	
	/**
	 * tenant id mentes metódus, system usernek...
	 *
	 * @param str
	 * @return
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public Tenant findOneByExtraData5(final String str) {
		SecurityUtil.limitAccessSystem();
		return repository.findOneBy("extraData5", str);
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_TENANT_OVERSEER_STR)
	public List<Pair<Tenant, User>> findAllForOverseer() {

		final ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();
		
		// ---

		final List<User> userList;

		if (loggedInUser.getParentId() != null) {

			// ha van parentId-ja, akkor már egy avatárban vagyunk (már át vagyunk lépve egy másik tenant-ba),
			// akkor a parent alapján keresünk usereket

			userList = this.userJdbcRepository.findAllByParentId(loggedInUser.getParentId());
		} else {
			userList = this.userJdbcRepository.findAllByParentId(loggedInUser.getId());
		}

		if (userList == null) {
			return new ArrayList<>();
		}
		
		// ---

		final Map<Integer, User> tenantWithAvatarUserMap = new HashMap<>();

		for (final User user : userList) {

			if (!Boolean.TRUE.equals(user.getEnabled())) {
				continue;
			}

			tenantWithAvatarUserMap.put(user.getTenantId(), user);
			
			// TODO: ha egy tenant-ban több avatar-ja is van, akkor csak az utolsó marad meg (most nem akarunk még ilyet, de jövőben gond lehet)
		}
		
		// ---

		final List<Tenant> allTenantList = this.repository.findAll();
		final List<Pair<Tenant, User>> retList = new ArrayList<>();

		for (final Tenant tenant : allTenantList) {

			if (Boolean.TRUE.equals(tenant.getEnabled()) 
					&& !tenant.getId().equals(ToolboxSysKeys.UserAuth.LCU_TENANT_ID) 
					&& !tenant.getId().equals(ToolboxSysKeys.UserAuth.COMMON_TENANT_ID)) {
				
				final User u = tenantWithAvatarUserMap.get(tenant.getId());
				
				if (u != null) {
					retList.add(Pair.of(tenant, u));
				}
				
			}

		}

		return retList;

	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_SUPER_TENANT_OVERSEER_STR)
	public List<Pair<Tenant, User>> findAllForSuperOverseer() {

		final ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();
		
		// ---

		final List<User> userList;

		if (loggedInUser.getParentId() != null) {

			// ha van parentId-ja, akkor már egy avatárban vagyunk (már át vagyunk lépve egy másik tenant-ba),
			// akkor a parent alapján keresünk usereket

			userList = this.userJdbcRepository.findAllByParentId(loggedInUser.getParentId());
		} else {
			userList = this.userJdbcRepository.findAllByParentId(loggedInUser.getId());
		}

		if (userList == null) {
			return new ArrayList<>();
		}

		// ---

		final Map<Integer, User> tenantWithAvatarUserMap = new HashMap<>();

		for (final User user : userList) {

			if (!Boolean.TRUE.equals(user.getEnabled())) {
				continue;
			}

			tenantWithAvatarUserMap.put(user.getTenantId(), user);

			// TODO: ha egy tenant-ban több avatar-ja is van, akkor csak az utolsó marad meg (most nem akarunk még ilyet, de jövőben gond lehet)
		}
		
		// ---

		final List<Tenant> allTenantList = this.repository.findAll();

		final List<Pair<Tenant, User>> retList = new ArrayList<>();

		for (final Tenant tenant : allTenantList) {

			if (Boolean.TRUE.equals(tenant.getEnabled()) 
					&& !tenant.getId().equals(ToolboxSysKeys.UserAuth.LCU_TENANT_ID)) {

				final User u = tenantWithAvatarUserMap.get(tenant.getId());

				if (u != null) {
					retList.add(Pair.of(tenant, u));
				} else {
					User emptyUser = new User(); // empty/dummy user objektum
					emptyUser.setUsername("-");
					retList.add(Pair.of(tenant, emptyUser));
				}

			}

		}

		return retList;

	}

	@Override
	@Secured(UserAuth.ROLE_SUPER_USER_STR)
	public List<Tenant> findAll() {
		return super.findAll();
	}

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_SUPER_USER_STR)
	public Page<Tenant> findAll(final BasePageRequest<Tenant> pageRequest) {
		return super.findAll(pageRequest);
	}

	@Override
	@Transactional
	@Secured(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR)
	public Tenant save(final Tenant t) {

		if (t.isNew()) {

			log.info("creating tenant...");

			// ---

			final String encodedPassword = this.passwordEncoder.encode("admin");
			t.setName(t.getName().replace(" ", "-").toLowerCase());
			this.repository.createTenant(t.getName(), t.getEmail(), encodedPassword);

			// ---

			try {

				final int tenantId = this.repository.findLastTenantId();
				
				log.debug("findLastTenantId: " + tenantId);

				JdbcRepositoryManager.setTlTenantId(tenantId);

				this.afterTenantCreate(this.repository.findLastTenantId());

			} finally {

				JdbcRepositoryManager.clearTlTenantId();

			}

			// ---

			final Tenant tret = this.repository.findOneBy("name", t.getName());
			
			this.fileStoreService.setToNormal(tret.getFileIds());
			this.fileStoreService.setToNormal(tret.getFileIds2());
			this.fileStoreService.setToNormal(tret.getFileIds3());
			
			log.info("tenant was created: " + tret.getName() + ", " + tret.getId());

			return tret;

		} else {

			final Tenant tenant = super.save(t);
			
			{
				final Tenant oldTenant = this.repository.findOne(t.getId());
				this.fileStoreService.setToNormalOrDelete(oldTenant.getFileIds(), t.getFileIds());
				this.fileStoreService.setToNormalOrDelete(oldTenant.getFileIds2(), t.getFileIds2());
				this.fileStoreService.setToNormalOrDelete(oldTenant.getFileIds3(), t.getFileIds3());
			}

			final TenantInfo tenantInfo = this.tenantInfoJdbcRepository.findOneBy("tenantId", tenant.getId());

			tenantInfo.setCompanyTaxNumber(t.getCompanyTaxNumber());
			tenantInfo.setCompanyRegistrationNumber(t.getCompanyRegistrationNumber());
			tenantInfo.setCompanyBankAccountNumber(t.getCompanyBankAccountNumber());

			tenantInfo.setCompanyAddressCountry(t.getCompanyAddressCountry());
			tenantInfo.setCompanyAddressState(t.getCompanyAddressState());
			tenantInfo.setCompanyAddressCounty(t.getCompanyAddressCounty());
			tenantInfo.setCompanyAddressZipCode(t.getCompanyAddressZipCode());
			tenantInfo.setCompanyAddressDetails(t.getCompanyAddressDetails());

			tenantInfo.setContactName(t.getContactName());
			tenantInfo.setEmail(t.getEmail());
			tenantInfo.setPhone(t.getPhone());

			tenantInfo.setNote(t.getNote());
			tenantInfo.setExtraData1(t.getExtraData1());
			tenantInfo.setExtraData2(t.getExtraData2());
			tenantInfo.setExtraData3(t.getExtraData3());
			tenantInfo.setExtraData4(t.getExtraData4());
			tenantInfo.setExtraData5(t.getExtraData5());
			tenantInfo.setExtraData6(t.getExtraData6());
			tenantInfo.setExtraData7(t.getExtraData7());
			tenantInfo.setExtraData8(t.getExtraData8());
			tenantInfo.setExtraData9(t.getExtraData9());
			tenantInfo.setExtraData10(t.getExtraData10());
			tenantInfo.setFileIds(t.getFileIds());
			tenantInfo.setFileIds2(t.getFileIds2());
			tenantInfo.setFileIds3(t.getFileIds3());
			tenantInfo.setFileIds4(t.getFileIds4());
			tenantInfo.setFileIds5(t.getFileIds5());
			tenantInfo.setFileIds6(t.getFileIds6());
			tenantInfo.setFileIds7(t.getFileIds7());
			tenantInfo.setCompanyName(t.getCompanyName());

			this.tenantInfoJdbcRepository.save(tenantInfo);

			log.info("tenant was modified: " + t.getName());

			return tenant;
		}

	}

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR)
	public long count() {
		return super.count();
	}

	/**
	 * SQL műveletek után további tenant inicializáció
	 * 
	 * @see #afterTestTenantCreate
	 */
	protected void afterTenantCreate(final int tenantId) {

		if (StringUtils.isNotBlank(this.tenantInitGroovyScriptLocation)) {

			final Tenant tenant = this.repository.findOne(tenantId);

			try {

				this.tenantGroovyScriptRun(this.tenantInitGroovyScriptLocation + "/" + tenant.getName() + ".groovy");

				if (this.springEnvironment.acceptsProfiles("dev", "unit-test")) {
					this.tenantGroovyScriptRun(this.tenantInitGroovyScriptLocation + "/" + tenant.getName() + ".test.groovy");
				}

			} catch (final Exception e) {
				throw new ToolboxGeneralException("afterTenantCreate failed", e);
			}

		}

	}

	private void tenantGroovyScriptRun(final String locationPattern) throws IOException {

		try {

			final Resource[] resources = new PathMatchingResourcePatternResolver().getResources(locationPattern);

			if (resources.length > 0) {

				String scriptStr = null;

				try (InputStream is = resources[0].getInputStream()) {
					scriptStr = IOUtils.toString(is, StandardCharsets.UTF_8);
				}

				ToolboxGroovyHelper.evaluate(scriptStr);

				log.info("afterTenantCreate Groovy script success: " + locationPattern);

			}

		} catch (final java.io.FileNotFoundException e) {
			log.debug("afterTenantCreate Groovy script not found: " + locationPattern);
		}

	}

	/**
	 * SQL műveletek után további tenant inicializáció 
	 * (test-tenant, mj.: alap esetben ilyenkor csak ez hívódik meg, a {@link #afterTenantCreate(int)} automatikusan nem) 
	 */
	@SuppressWarnings("unused")
	protected void afterTestTenantCreate(final int tenantId) {
		//
	}

}
