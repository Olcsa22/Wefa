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
public @interface FwDropColumn {

	int versionNumber();

	String[] columnNames();

}