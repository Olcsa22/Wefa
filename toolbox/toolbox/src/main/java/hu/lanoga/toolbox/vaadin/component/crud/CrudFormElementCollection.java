package hu.lanoga.toolbox.vaadin.component.crud;

import java.util.List;

import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;

import hu.lanoga.toolbox.ToolboxSysKeys;

@SuppressWarnings("unused")
public interface CrudFormElementCollection<T> {

	// TODO: itt a nagybetűs Void-nak (ret. value) már nincs jelentősége, refactorral szedjük ki

	/**
	 * bind előtt 
	 * 
	 * @param modelObject
	 * @param crudOperation
	 * @return
	 */
	default public Void preBind(final T modelObject, final ToolboxSysKeys.CrudOperation crudOperation) {
		return null;
	}

	/**
	 * bind után, de még a layout építése előtt
	 * 
	 * @param modelObject
	 * @param crudOperation
	 * @return
	 */
	default public Void afterBind(final T modelObject, final ToolboxSysKeys.CrudOperation crudOperation) {
		return null;
	}

	/**
	 * bind és a layout építése után... 
	 * hasonló a {@link #afterBind(Object, hu.lanoga.toolbox.ToolboxSysKeys.CrudOperation)}-hoz, annyi a plusz, 
	 * hogy itt a layout is manipulálható már némileg;
	 * vigyázni, mert több füles CRUD ablak/form esetén annyiszor hívódik meg, ahány fül van... 
	 * (vagy használd az {@link #afterLayoutBuild(Object, hu.lanoga.toolbox.ToolboxSysKeys.CrudOperation, Button, List)} változatot, ami csak egyszer hívódik)
	 * 
	 * @param modelObject
	 * @param crudOperation
	 * @param crudFormOpButton
	 * @param layout
	 * @return
	 */
	default public Void afterLayoutBuild(final T modelObject, final ToolboxSysKeys.CrudOperation crudOperation, final Button crudFormOpButton, final AbstractLayout layout) {
		return null;
	}

	/**
	 * bind és a layout építése után... 
	 * hasonló a {@link #afterBind(Object, hu.lanoga.toolbox.ToolboxSysKeys.CrudOperation)}-hoz, annyi a plusz, 
	 * hogy itt a layout is manipulálható már némileg
	 * 
	 * @param modelObject
	 * @param crudOperation
	 * @param crudFormOpButton
	 * @param layouts
	 * @return
	 */
	default public Void afterLayoutBuild(final T modelObject, final ToolboxSysKeys.CrudOperation crudOperation, final Button crudFormOpButton, final List<AbstractLayout> layouts) {

		for (final AbstractLayout layout : layouts) {
			afterLayoutBuild(modelObject, crudOperation, crudFormOpButton, layout);
		}

		return null;
	}

	/**
	 * {@link ToolboxSysKeys.CrudOperation#UPDATE} és {@link ToolboxSysKeys.CrudOperation#ADD} esetén a service save előtt (és még a validáció előtt) hívódik meg, 
	 * használatható fix értékek beírásárára a modelObject-be... 
	 * ide is mehet manuális validáció (de ekkor figyelni kell a null értékekre)... 
	 *  
	 * @param modelObject
	 * @return
	 * 
	 * @see #manualValidation(Object, hu.lanoga.toolbox.ToolboxSysKeys.CrudOperation)
	 */
	default public Void preAction(final T modelObject) {
		return null;
	}

	/**
	 * {@link ToolboxSysKeys.CrudOperation#UPDATE} és {@link ToolboxSysKeys.CrudOperation#ADD} esetén a service save előtt (és még a validáció előtt) hívódik meg, 
	 * használatható fix értékek beírásárára a modelObject-be... 
	 * ide is mehet manuális validáció (de ekkor figyelni kell a null értékekre)... 
	 * 
	 * @param modelObject
	 * @return
	 * 
	 * @see #manualValidation(Object, hu.lanoga.toolbox.ToolboxSysKeys.CrudOperation)
	 */
	default public Void preAction(final T modelObject, final ToolboxSysKeys.CrudOperation crudOperation) {
		return preAction(modelObject);
	}

	/**
	 * {@link ToolboxSysKeys.CrudOperation#UPDATE} és {@link ToolboxSysKeys.CrudOperation#ADD} esetén a service save előtt (de már a normál (annotációs) validáció után) hívódik meg, 
	 * manuális validációkra (illetve ilyeneket lehet a service metódusba is tenni, szituáció függő, hogy melyik kell, pl. ha fontos az tranzkació izoláció, akkor menjen a service-ba... 
	 * ide a VaadinForm manualValidation-be inkább a lazább GUI orientált ellenőrzések mehetnek)
	 * 
	 * @param modelObject
	 * @return
	 */
	default public Void manualValidation(final T modelObject, final ToolboxSysKeys.CrudOperation crudOperation) {
		return null;
	}

	/**
	 * {@link ToolboxSysKeys.CrudOperation#UPDATE}, {@link ToolboxSysKeys.CrudOperation#ADD} és {@link ToolboxSysKeys.CrudOperation#DELETE} után hívódik meg... 
	 * pl. kézi notification-ök feldobására lehet jó
	 * 
	 * @param modelObject
	 * @return
	 */
	default public Void afterAction(final T modelObject, final ToolboxSysKeys.CrudOperation crudOperation) {
		return null;
	}

}
