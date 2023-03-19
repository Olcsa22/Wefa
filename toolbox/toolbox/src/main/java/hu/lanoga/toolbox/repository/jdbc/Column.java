package hu.lanoga.toolbox.repository.jdbc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Csak az {@code DefaultJdbcRepository} veszi figyelembe alap esetben... 
 * kézi repository metódusok írásánál oda kell figyelni... 
 * 
 * (fontos, hogy az összes mező bekerül, nem csak az annotáltak... az annotáció csak módosító)
 * 
 * @see JdbcRepositoryManager
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
		
	/**
	 * default: a Java mezőnév kisbetűs, aláhúzásos formára hozva...
	 * 
	 * @return
	 */
    public String name() default "";
         
    public boolean isId() default false;
    public boolean isTenantId() default false;
    
}