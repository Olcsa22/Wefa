package hu.lanoga.toolbox.vaadin.component.crud;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.ui.Grid.Column;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.FIELD)
public @interface CrudGridColumn {

	/**
	 * fallback üzenet, nyelvi fájnál kulcs... (lásd "gettext" formátumú nyelvi fájlok...)
	 * 
	 * @return
	 */
	public String translationMsg() default "";

	/**
	 * 0 = auto... 
	 * 
	 * az auto számozás 1-től indul és kettesével halad (3, 5, 7...) 
	 * tehát lehet manuálissal is keverni (1 alatti számok (akár negatív), 2, 4... 
	 * illetve nagyon magas számok, ha fixen a végére akarjuk tenni)
	 * 
	 * @return
	 */
	public int columnPosition() default 0;

	public boolean allowSearch() default true;
	
	/**
	 * lehessen-e rendezni (csökkenő, növekvő)
	 * 
	 * @return
	 */
	public boolean allowOrder() default true;

	/**
	 * kereséshez, ekkor ennek a codestoretype-nak megfelelő combo lesz a filternél...
	 * 
	 * @return
	 */
	public int codeStoreTypeId() default 0;

	/**
	 * kereséshez, ekkor a crudgrid-hez erre a célra beadott combo-k közül ez lesz a filter fent...
	 * 
	 * @return
	 */
	public String searchComboMapId() default "";

	/**
	 * kereséshez, csak akkor kell megadni, ha nem egyezik azzal, amin az annotáció maga van... 
	 * codeStoreTypeId és/vagy searchComboMapId kapcsán csak 
	 * 
	 * @return
	 */
	public String searchTargetFieldName() default "";
	
	public boolean searchAsJsonArrayContains() default false;

	/**
	 * Vaadin-hoz... 
	 * 0 = épp kiférjen, 1 = alap, 2 = kapjon többet a szabad helyből...
	 * 
	 * @see Column#setExpandRatio(int)
	 * @return
	 */
	public int columnExpandRatio() default 1;

	public boolean allowHide() default true;

	/**
	 * csak akkor működik, ha {@link #allowHide()} is true
	 * 
	 * @return
	 */
	public boolean startHidden() default false;

	/**
	 * jelenleg nincs jelentősége, egy korábbi experimental dolog volt...
	 * 
	 * @return
	 * 
	 * @deprecated
	 */
	@Deprecated
	public boolean lazyLoadFilter() default false;

}