package hu.lanoga.toolbox.db.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
// @Inherited // szándékosan nem @Inherited
@Target(ElementType.TYPE)
public @interface FwCreateIndex {

	/**
	 * -1 esetén az osztály {@link FwCreateTable}-nél megadott lesz
	 * @return
	 */
	int versionNumber() default -1;

	/**
	 * ","-vel elválasztva fel kell sorolni az indexhez felvenni kívánt mezőket
	 * @return
	 */
	String[] indexFields() default "";

}