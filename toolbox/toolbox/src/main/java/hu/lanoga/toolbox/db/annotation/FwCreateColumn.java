package hu.lanoga.toolbox.db.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
// @Inherited // szándékosan nem @Inherited
@Target(ElementType.FIELD)
public @interface FwCreateColumn {

	/**
	 * ha nincs megadva, akkor az field neve lesz UPPER_UNDERSCORE-ra átírva
	 * 
	 * @return
	 */
	String columnName() default "";

	/**
	 * -1 esetén az osztály {@link FwCreateTable}-nél megadott lesz
	 * 
	 * @return
	 */
	int versionNumber() default -1;

	/**
	 * pl.: "VARCHAR(50)", "INTEGER"... 
	 * amennyiben nincs megadva, akkor a Java típus 
	 * alapján automatikusan lesz kitalálva
	 * 
	 * @return
	 */
	String columnType() default "";
	
	boolean notNull() default false;
	
	/**
	 * true esetén a validator annotációk alapján kitatlálja, 
	 * hogy milyen DB constraint kell (jelenleg: csak NOT NULL)
	 * 
	 * @return
	 */
	boolean detectValidatorAnnotations() default true;
	
}