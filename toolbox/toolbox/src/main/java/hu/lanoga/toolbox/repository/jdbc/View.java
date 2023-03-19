package hu.lanoga.toolbox.repository.jdbc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Az elnevezése nem a legjobb (de jobbat nem találtunk). Lényeg az, hogy ezek a mezők nem simán a táblából jönnek és nem is lesznek oda mentve sima save()-nél.   
 * (csak az {@code DefaultJdbcRepository} veszi figyelembe alap esetben... kézi repository metódusok írásánál oda kell figyelni... alternatívaként van lehetőség csak átmenetileg kihagyni mezőt (a save() hívásnál)... )
 * 
 * @see DefaultJdbcRepository#save(hu.lanoga.toolbox.repository.ToolboxPersistable, java.util.Set, boolean)
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface View {
	
	//
    
}