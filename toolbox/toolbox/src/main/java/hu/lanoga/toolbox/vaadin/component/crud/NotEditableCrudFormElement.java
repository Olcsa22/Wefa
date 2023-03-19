package hu.lanoga.toolbox.vaadin.component.crud;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * crud-ban csak az add dialogban lehet egyszer megadni értéket, de később már nem lehet módosítani
 * (tehát a modify dialogban ez a mező read-onlyként jelenik meg) 
 * 
 * ugyanaz, mint a {@link CreateOnlyCrudFormElement}, csak azért kapott egy második annotációt is, mert a 
 * Kárrendezőben a Groovy scriptekben is megjelenik és ott a CreateOnly név furán mutat 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.FIELD)
public @interface NotEditableCrudFormElement {

	//

}