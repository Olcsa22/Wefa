package hu.lanoga.toolbox.cache;

import java.time.Duration;

import org.ehcache.UserManagedCache;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.builders.UserManagedCacheBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.Ehcache;

import hu.lanoga.toolbox.util.ToolboxAssert;

class EhCacheMemoryToolboxManualCache<T> implements ToolboxManualCache<T> {

	private final Ehcache<String, T> cache;

	@SuppressWarnings("null")
	EhCacheMemoryToolboxManualCache(final Class<T> clazz, final String cacheName, final Integer heapMaxMiB, final Integer heapMaxCount, final long timeToIdleSeconds) {

		ToolboxAssert.notNull(cacheName);
		ToolboxAssert.notNull(clazz);
		ToolboxAssert.isTrue((heapMaxMiB == null) || (heapMaxCount == null)); // csak az egyik lehet

		UserManagedCacheBuilder<String, T, UserManagedCache<String, T>> cacheBuilder = UserManagedCacheBuilder.newUserManagedCacheBuilder(String.class, clazz);
		cacheBuilder = cacheBuilder.withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(timeToIdleSeconds)));
		
		ResourcePoolsBuilder resourcePoolsBuilder = ResourcePoolsBuilder.newResourcePoolsBuilder();

		if (heapMaxMiB != null) {
			resourcePoolsBuilder = resourcePoolsBuilder.heap(heapMaxMiB, MemoryUnit.MB);
		} else {
			resourcePoolsBuilder = resourcePoolsBuilder.heap(heapMaxCount, EntryUnit.ENTRIES);
		}

		cacheBuilder = cacheBuilder.withResourcePools(resourcePoolsBuilder.build());

		this.cache = (Ehcache<String, T>) cacheBuilder.build(true);
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
	 * Spring automatikusan meghívja az alkalmazás üzemszerű leállításánál 
	 * (ha IDE-ből állítod le, akkor nem!) (itt nem lényeges, a memory only jelleg miatt (nincs mit close-olni)
	 */
	@Override
	public void close() throws Exception {
		this.cache.close();
	}

}