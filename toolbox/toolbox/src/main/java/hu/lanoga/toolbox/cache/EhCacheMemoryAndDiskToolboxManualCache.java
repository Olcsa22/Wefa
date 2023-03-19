package hu.lanoga.toolbox.cache;

import java.io.Closeable;
import java.io.File;
import java.time.Duration;

import javax.annotation.PreDestroy;

import org.ehcache.PersistentUserManagedCache;
import org.ehcache.UserManagedCache;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.builders.UserManagedCacheBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.spi.service.LocalPersistenceService;
import org.ehcache.impl.config.persistence.DefaultPersistenceConfiguration;
import org.ehcache.impl.config.persistence.UserManagedPersistenceContext;
import org.ehcache.impl.persistence.DefaultLocalPersistenceService;

import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.util.ToolboxAssert;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class EhCacheMemoryAndDiskToolboxManualCache<T> implements ToolboxManualCache<T> {

	private final UserManagedCache<String, T> cache;
	
	@SuppressWarnings("null")
	EhCacheMemoryAndDiskToolboxManualCache(final Class<T> clazz, final String cacheName, final Integer heapMaxMiB, final Integer heapMaxCount, final Integer diskMaxMiB, final boolean jvmRestartPersistent, final long timeToIdleSeconds) {

		ToolboxAssert.notNull(cacheName);
		ToolboxAssert.notNull(clazz);
		ToolboxAssert.isTrue((heapMaxMiB == null) || (heapMaxCount == null)); // csak az egyik lehet
		ToolboxAssert.notNull(diskMaxMiB);

		UserManagedCacheBuilder<String, T, UserManagedCache<String, T>> cacheBuilder = UserManagedCacheBuilder.newUserManagedCacheBuilder(String.class, clazz);
		cacheBuilder = cacheBuilder.withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(timeToIdleSeconds)));
		
		final LocalPersistenceService persistenceService = new DefaultLocalPersistenceService(new DefaultPersistenceConfiguration(new File(ApplicationContextHelper.getConfigProperty("tools.file.disk.cachedir"))));
		UserManagedCacheBuilder<String, T, PersistentUserManagedCache<String, T>> persistentCacheBuilder = cacheBuilder.with(new UserManagedPersistenceContext<String, T>(cacheName, persistenceService));
		
		ResourcePoolsBuilder resourcePoolsBuilder = ResourcePoolsBuilder.newResourcePoolsBuilder();

		if (heapMaxMiB != null) {
			resourcePoolsBuilder = resourcePoolsBuilder.heap(heapMaxMiB, MemoryUnit.MB);
		} else {
			resourcePoolsBuilder = resourcePoolsBuilder.heap(heapMaxCount, EntryUnit.ENTRIES);
		}
		resourcePoolsBuilder = resourcePoolsBuilder.disk(diskMaxMiB, MemoryUnit.MB, jvmRestartPersistent);

		persistentCacheBuilder = persistentCacheBuilder.withResourcePools(resourcePoolsBuilder.build());

		this.cache = persistentCacheBuilder.build(true);
				
	}

	@Override
	public void put(final String key, final T value) {
		this.cache.put(key, value);
	}

	@Override
	public void putIfAbsent(final String key, final T value) {
		this.cache.putIfAbsent(key, value);
	}

	@Override
	public void remove(final String key) {
		this.cache.remove(key);
	}

	@Override
	public T get(final String key) {
		return this.cache.get(key);
	}

	/**
	 * akkor fontos igazán a close, ha a "disk" részen persistent=true is megadtunk 
	 * (ami csak akkor fog menni/visszatölteni köv. indulásnál, ha előző leállásnál close-olva is volt (EhCache sajátosság)
	 * (a close-t a Spring a maga általal managelt bean-ek esetén automatikusan meghívja az alkalmazás üzemszerű leállításánál (ha IDE-ből állítod le, akkor nem!), 
	 * más esetekre (ahol nem {@link Closeable} / {@link AutoCloseable} van) lásd {@link PreDestroy}) 
	 */
	@Override
	public void close() throws Exception {				
		
		this.cache.close();
		
		log.debug("cache properly closed");
		
	}
	
}