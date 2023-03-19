package hu.lanoga.toolbox.spring;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Spring helper... Spring bean-ekhez, property-khez való alternatív hozzájutás
 * (ahol lehet, ott az autowired és a value annotáció legyen használva, ez a helper azokra az esetekre van, ahol az annotáció nem alkalmazhatóak, pl.: static metódusokból, Vaadin UI)
 */
@ConditionalOnMissingBean(name = "applicationContextHelperOverrideBean")
@Slf4j
@Component
public class ApplicationContextHelper implements ApplicationContextAware {

	private static ApplicationContext context; // direkt static, csak egy van belőle ezért jó így

	@Override
	public void setApplicationContext(final ApplicationContext context) {
	
		ApplicationContextHelper.context = context;

		((AbstractApplicationContext) context).registerShutdownHook(); // TODO: kipróbálni, hogy ez kell-e itt (értsd a funckió kell, de lehet, hogy már ez a default és nem is kell kiírni) 

		log.info("ApplicationContextHelper setApplicationContext()");
		
	}

	public static ApplicationContext getApplicationContext() {
		return context;
	}

	public static <T> T getBean(final Class<T> requiredType) {
		return context.getBean(requiredType);
	}

	public static <T> Map<String, T> getBeans(final Class<T> requiredType) {
		return context.getBeansOfType(requiredType);
	}

	/**
	 * vigyázni... 
	 * ha van scope annotáció benne scopedTarget, akkor lehet, hogy a bean name string más lesz defaultban (scopedTarget.valamiOsztaly)... 
	 * 
	 * @param name
	 * @param requiredType
	 * @return
	 */
	public static <T> T getBean(final String name, final Class<T> requiredType) {
		return context.getBean(name, requiredType);
	}

	/**
	 * kapcsos zároljel, dollárjel nem kell, csak a property neve
	 * 
	 * @param key
	 * @return
	 */
	public static String getConfigProperty(final String key) {
		return context.getEnvironment().getProperty(key);
	}

	public static <T> T getConfigProperty(final String key, final Class<T> targetType) {
		return context.getEnvironment().getProperty(key, targetType);
	}
	
	/**
	 * {@code PostConstruct} részekben használd ezt a változatot (úgy, hogy az adott osztályban {@code Autowired} annotációval behozod az {@code Environment}-et)... 
	 * (bean-ek init sorrendje miatt fontos ez... lehet, hogy a te bean-ed előbb jön létre és előbb hívódik meg a {@code PostConstruct} metódus benne, 
	 * mint ahogy az {@code ApplicationContextHelper} rendesen inicializálódna)
	 * 
	 * @see Environment#getActiveProfiles()
	 * 
	 * @return
	 */
	public static boolean hasDevProfile(Environment environment) {
		String[] activeProfiles = environment.getActiveProfiles();
		for (String activeProfile : activeProfiles) {
			if (activeProfile.equalsIgnoreCase("dev")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * {@code PostConstruct} részekben ne ezt a változatod használd!
	 * 
	 * @see Environment#getActiveProfiles()
	 * 
	 * @return
	 */
	public static boolean hasDevProfile() {
		return hasDevProfile(getBean(Environment.class));
	}
	
	/**
	 * {@code PostConstruct} részekben használd ezt a változatot (úgy, hogy az adott osztályban {@code Autowired} annotációval behozod az {@code Environment}-et)... 
	 * (bean-ek init sorrendje miatt fontos ez... lehet, hogy a te bean-ed előbb jön létre és előbb hívódik meg a {@code PostConstruct} metódus benne, 
	 * mint ahogy az {@code ApplicationContextHelper} rendesen inicializálódna)
	 * 
	 * @see Environment#getActiveProfiles()
	 * 
	 * @return
	 */
	public static boolean hasUnitTestProfile(Environment environment) {
		String[] activeProfiles = environment.getActiveProfiles();
		for (String activeProfile : activeProfiles) {
			if (activeProfile.equalsIgnoreCase("unit-test")) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * {@code PostConstruct} részekben ne ezt a változatod használd!
	 * 
	 * @see Environment#getActiveProfiles()
	 * 
	 * @return
	 */
	public static boolean hasUnitTestProfile() {
		return hasUnitTestProfile(getBean(Environment.class));
	}
	
	/**
	 * {@code PostConstruct} részekben használd ezt a változatot (úgy, hogy az adott osztályban {@code Autowired} annotációval behozod az {@code Environment}-et)... 
	 * (bean-ek init sorrendje miatt fontos ez... lehet, hogy a te bean-ed előbb jön létre és előbb hívódik meg a {@code PostConstruct} metódus benne, 
	 * mint ahogy az {@code ApplicationContextHelper} rendesen inicializálódna)
	 * 
	 * @see Environment#getActiveProfiles()
	 * 
	 * @return
	 */
	public static boolean hasDevOrUnitTestProfile(Environment environment) {
		String[] activeProfiles = environment.getActiveProfiles();
		for (String activeProfile : activeProfiles) {
			if (activeProfile.equalsIgnoreCase("dev") || activeProfile.equalsIgnoreCase("unit-test")) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * {@code PostConstruct} részekben ne ezt a változatod használd!
	 * 
	 * @see Environment#getActiveProfiles()
	 * 
	 * @return
	 */
	public static boolean hasDevOrUnitTestProfile() {
		return hasDevOrUnitTestProfile(getBean(Environment.class));
	}
}