package hu.lanoga.toolbox.cache;

import org.springframework.cache.annotation.Cacheable;

/**
 * Egyes szituációkban jó lehet a Spring {@link Cacheable} annotációs megoldás is. 
 * {@code ToolboxManualCache} ott kell, ahol finomabb kontrollra van szükség...
 * 
 * @param <T>
 */
public interface ToolboxManualCache<T> extends AutoCloseable {

	/**
	 * "Specifically built and tested to run well under highly concurrent access, as long as you do not modify Element from the multiple threads."
	 * 
	 * @param key
	 * @param value
	 */
	void put(String key, T value);

	/**
	 * "Specifically built and tested to run well under highly concurrent access, as long as you do not modify Element from the multiple threads."
	 * 
	 * @param key
	 * @param value
	 */
	void putIfAbsent(String key, T value);

	void remove(String key);

	/**
	 * "Specifically built and tested to run well under highly concurrent access, as long as you do not modify Element from the multiple threads."
	 * 
	 * "Is it thread safe to modify Element values after retrieval from a Cache?
	 * Remember that a value in a cache element is globally accessible from multiple threads. It is inherently not thread safe to modify the value. It is safer to retrieve a value, delete the cache element and then reinsert the value."
	 * 
	 * @param key
	 * @return
	 */
	T get(String key);

}