package hu.lanoga.toolbox.config;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * ez csak a Spring {@link Cacheable} annotációs megoldásra vonatkozik
 */
@EnableCaching // TODO: nem nagyon használjuk, ki lehetne szedni toolbox szintről, vagy meg kellene csinálni property-vel kikapcsolhatóra
@Configuration
public class CacheableConfig  {

	//
	
}