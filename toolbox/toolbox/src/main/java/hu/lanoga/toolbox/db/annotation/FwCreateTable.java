package hu.lanoga.toolbox.db.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import hu.lanoga.toolbox.repository.jdbc.View;

@Documented
@Retention(RetentionPolicy.RUNTIME)
// @Inherited // szándékosan nem @Inherited
@Target(ElementType.TYPE)
public @interface FwCreateTable {

	/**
	 * ha nincs megadva, akkor az osztály neve lesz UPPER_UNDERSCORE-ra átírva
	 * @return
	 */
	String tableName() default "";

	int versionNumber() default 1000;

	/**
	 * false esetén csak a {@link FwCreateColumn} annotációval jelölt field-nek 
	 * lesz létrehozva DB column; 
	 * 
	 * true esetén minden Java field-nek lesz létrehozva DB column 
	 * (ez esetben az {@link FwCreateColumn} módosítóként használható); 
	 * 
	 * mindig kivételek (true és false esetén is) a {@link View} field-ek 
	 * 
	 * @return
	 */
	boolean autoColumnMode() default true;

}