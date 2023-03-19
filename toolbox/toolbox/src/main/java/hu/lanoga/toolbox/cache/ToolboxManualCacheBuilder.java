package hu.lanoga.toolbox.cache;

import java.util.UUID;

import hu.lanoga.toolbox.ToolboxSysKeys.CacheEngine;
import hu.lanoga.toolbox.util.ToolboxAssert;

public class ToolboxManualCacheBuilder<T> {

	public static <X> ToolboxManualCacheBuilder<X> newToolboxCacheBuilder(final Class<X> clazz, final CacheEngine cacheEngine) {
		return new ToolboxManualCacheBuilder<>(clazz, cacheEngine);
	}

	private final Class<T> clazz;
	private final CacheEngine cacheEngine;

	private String cacheName = UUID.randomUUID().toString();

	private Integer heapMaxMiB;
	private Integer heapMaxCount;

	private Integer peristentMaxMib;
	
	@SuppressWarnings("unused")
	@Deprecated
	private Integer persistentMaxCount;

	private boolean jvmRestartPersistent = false;
	private Long timeToIdleSeconds;

	ToolboxManualCacheBuilder(final Class<T> clazz, final CacheEngine cacheEngine) {

		super();

		ToolboxAssert.notNull(clazz);
		ToolboxAssert.notNull(cacheEngine);

		this.clazz = clazz;
		this.cacheEngine = cacheEngine;

	}

	public ToolboxManualCacheBuilder<T> setCacheName(final String cacheName) {
		this.cacheName = cacheName;
		return this;
	}

	public ToolboxManualCacheBuilder<T> setHeapMaxMiB(final int heapMaxMiB) {
		this.heapMaxMiB = heapMaxMiB;
		return this;
	}

	public ToolboxManualCacheBuilder<T> setHeapMaxCount(final int heapMaxCount) {
		this.heapMaxCount = heapMaxCount;
		return this;
	}

	public ToolboxManualCacheBuilder<T> setPeristentMaxMib(final int peristentMaxMib) {
		this.peristentMaxMib = peristentMaxMib;
		return this;
	}

	@Deprecated
	public ToolboxManualCacheBuilder<T> setPersistentMaxCount(final int persistentMaxCount) {
		
		// később lehet, hogy újra lesz ilyen is
		
		this.persistentMaxCount = persistentMaxCount;
		return this;
	}

	/**
	 * nem megbízható, csak akkor megy, ha a JVM "szépen" volt leállítva
	 * 
	 * @param jvmRestartPersistent
	 * @return
	 */
	public ToolboxManualCacheBuilder<T> setJvmRestartPersistent(final boolean jvmRestartPersistent) {
		this.jvmRestartPersistent = jvmRestartPersistent;
		return this;
	}

	public ToolboxManualCacheBuilder<T> setTimeToIdleSeconds(final long timeToIdleSeconds) {
		this.timeToIdleSeconds = timeToIdleSeconds;
		return this;
	}

	public ToolboxManualCache<T> build() {

		if (this.cacheEngine.equals(CacheEngine.EHCACHE_MEMORY)) {
			return new EhCacheMemoryToolboxManualCache<>(this.clazz, this.cacheName, this.heapMaxMiB, this.heapMaxCount, this.timeToIdleSeconds);
		} else if (this.cacheEngine.equals(CacheEngine.EHCACHE_MEMORY_AND_DISK)) {
			return new EhCacheMemoryAndDiskToolboxManualCache<>(this.clazz, this.cacheName, this.heapMaxMiB, this.heapMaxCount, this.peristentMaxMib, this.jvmRestartPersistent, this.timeToIdleSeconds);
		}

		return null;

	}
}