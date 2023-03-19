package hu.lanoga.toolbox.repository.jdbc;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * csak a {@code DefaultJdbcRepository} metódusai veszik figyelembe alap esetben, 
 * kézi repository metódusok írásánál oda kell figyelni... 
 * 
 * (fontos még, hogy az összes {@link ToolboxtJdbcPersistable}}-t feldogozza az ős repository, nem csak az megannotáltakat... 
 * ezen annotáció csak módosító!)
 * 
 * @see JdbcRepositoryManager
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
	
	/**
	 * default ({@link DefaultJdbcRepository}-ban) (ha itt nincs kitöltve): 
	 * a class neve kisbetűs, aláhúzásos formára hozva...
	 * 
	 * @return
	 */
    public String name() default "";
         
}