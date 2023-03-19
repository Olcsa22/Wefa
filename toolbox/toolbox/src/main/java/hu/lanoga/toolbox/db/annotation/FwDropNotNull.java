package hu.lanoga.toolbox.db.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * NOT NULL constraint leszedése meglévő column-ról
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
// @Inherited // szándékosan nem @Inherited
@Target(ElementType.FIELD)
public @interface FwDropNotNull {

	int versionNumber();

}