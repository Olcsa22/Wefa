package hu.lanoga.toolbox.vaadin.component.crud;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * {@link MultiFormLayoutCrudFormComponent} kapcsán ezzel is meg lehet adni fül neveket 
 * (ilyenkor nyelvesítésre nincs lehetőség, ahhoz a constructor paraméter használható)
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)
public @interface MultiFormLayoutCrudFormComponentTabNames {
	
	// használat: @MultiFormLayoutCrudFormComponentTabNames("első fül felirata;második fül felirata")

	/**
	 * ;-vel kell elválasztani
	 * 
	 * @return
	 */
	public String value() default "";

}