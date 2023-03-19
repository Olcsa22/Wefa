package hu.lanoga.toolbox.codestore;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;

import hu.lanoga.toolbox.ToolboxSysKeys.RepositoryTenantMode;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.util.ToolboxAssert;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ConditionalOnMissingBean(name = "codeStoreItemJdbcRepositoryOverrideBean")
@Repository
public class CodeStoreItemJdbcRepository extends DefaultJdbcRepository<CodeStoreItem> {
	
	private static List<CodeStoreItem> copyProp(final List<CodeStoreItem> result) {
		
		final List<CodeStoreItem> result2 = new ArrayList<>();

		try {

			for (final CodeStoreItem csi : result) {
				final CodeStoreItem csi2 = new CodeStoreItem();
				BeanUtils.copyProperties(csi2, csi);
				result2.add(csi2);
			}

		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new ToolboxGeneralException("BeanUtils error!", e);
		}
		
		return result2;
	}

	private static Cache<Integer, CodeStoreItem> cache1 = CacheBuilder.newBuilder().concurrencyLevel(4).maximumSize(1000)/* .softValues() */.expireAfterWrite(3, TimeUnit.HOURS).build();
	private static Cache<Pair<String, Object>, List<CodeStoreItem>> cache2 = CacheBuilder.newBuilder().concurrencyLevel(4).maximumSize(1000)/* .softValues() */.expireAfterWrite(3, TimeUnit.HOURS).build();

	@Value("${tools.code-store.cache.enabled}")
	private boolean codeStoreCacheEnabled;

	public CodeStoreItemJdbcRepository() {
		super(RepositoryTenantMode.NO_TENANT);
	}

	public void clearCache() {

		SecurityUtil.limitAccessSuperAdmin();

		cache1.invalidateAll();
		cache2.invalidateAll();
	}

	@Override
	public String getInnerSelect() {
		return "SELECT c.*, extr_from_lang(c.caption, '#lang1', '#lang2') AS caption_caption, extr_from_lang(csi1.caption, '#lang1', '#lang2') AS code_store_type_caption FROM code_store_item c INNER JOIN code_store_type csi1 ON c.code_store_type_id = csi1.id";
	}

	/**
	 * (cache gyorsítás)
	 */
	@Override
	public CodeStoreItem findOne(final Integer id) {

		// long millis = System.currentTimeMillis();

		CodeStoreItem result;

		if (!this.codeStoreCacheEnabled) {

			result = super.findOne(id);

		} else {

			try {

				result = cache1.get(id, () -> {
					final CodeStoreItem c = CodeStoreItemJdbcRepository.super.findOne(id);
					ToolboxAssert.notNull(c); // meg kell vigyálni itt, com.google.common.cache.Cache-nek a null nem jó soha (lásd javadoc a .get() metódusán)
					return c;
				});

				result = copyProp(Lists.newArrayList(result)).get(0);

			} catch (final Exception e) {
				log.warn("CodeStoreItemJdbcRepository findOne cache error", e);
				result = super.findOne(id);
			}

		}

		// log.debug("CodeStoreItemJdbcRepository findOne: " + (System.currentTimeMillis() - millis) + " millis, (codeStoreCacheEnabled: " + codeStoreCacheEnabled + ")");

		return result;

	}

	/**
	 * (cache gyorsítás)
	 */
	@Override
	public List<CodeStoreItem> findAllBy(final String fieldName, final Object value) {

		// long millis = System.currentTimeMillis();

		List<CodeStoreItem> result;

		if (!this.codeStoreCacheEnabled) {

			result = super.findAllBy(fieldName, value);

		} else {

			try {

				result = cache2.get(Pair.of(fieldName, value), () -> {
					return CodeStoreItemJdbcRepository.super.findAllBy(fieldName, value);
				});

				result = copyProp(result);

			} catch (final Exception e) {
				log.warn("CodeStoreItemJdbcRepository findAllBy cache error", e);
				result = super.findAllBy(fieldName, value);
			}

		}

		// log.debug("CodeStoreItemJdbcRepository findAllBy: " + (System.currentTimeMillis() - millis) + " millis, (codeStoreCacheEnabled: " + codeStoreCacheEnabled + ")");
		
		return result;

	}

}
