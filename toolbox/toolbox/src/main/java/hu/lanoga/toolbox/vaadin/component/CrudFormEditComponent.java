package hu.lanoga.toolbox.vaadin.component;

import java.util.List;
import java.util.function.Supplier;

import com.vaadin.ui.VerticalLayout;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.service.ToolboxCrudService;
import hu.lanoga.toolbox.vaadin.component.crud.CrudFormElementCollection;
import hu.lanoga.toolbox.vaadin.component.crud.FormLayoutCrudFormComponent;
import hu.lanoga.toolbox.vaadin.component.crud.MultiFormLayoutCrudFormComponent;

public class CrudFormEditComponent<D extends ToolboxPersistable> extends VerticalLayout {


	/**
	 * Ez a komponens csak a grid-ek szerkesztő felületét tartalmazza (értsd. amit egy grid-nél ADD és UPDATE esetén felhozol)
	 * Itt az összes mező meg fog jelenni egy VerticalLayout-on
	 * (hasznos lehet kirakott iFrame-ek esetére, vagy olyan helyeken ahol nem kell a crud csak maga a szerkesztő része)
	 *
	 * @param selectedValue
	 * 		az érték, amit szerkeszteni szeretnénk (lehet egy üres modell is)
	 * @param crudService
	 * 		a service, amibe a mentés zajlani fog
	 * @param crudFormElementSupplier
	 * 		VaadinForm, amit meg kell jeleníteni
	 *
	 * 	példa felhasználásra: new CrudFormEditComponent<>(new User(), ApplicationContextHelper.getBean(UserService.class), () -> new User.VaadinForm());
	 */
	public CrudFormEditComponent(final D selectedValue, final ToolboxCrudService<D> crudService, final Supplier<CrudFormElementCollection<?>> crudFormElementSupplier) {

		final FormLayoutCrudFormComponent<D, CrudFormElementCollection<?>> formLayoutCrudFormComponent = new FormLayoutCrudFormComponent<>(crudFormElementSupplier);

		formLayoutCrudFormComponent.setDomainObject(selectedValue);
		formLayoutCrudFormComponent.setCrudOperation(ToolboxSysKeys.CrudOperation.UPDATE);
		formLayoutCrudFormComponent.setCrudAction(v -> {
			crudService.save(selectedValue);
		});

		formLayoutCrudFormComponent.init();

		this.addComponent(formLayoutCrudFormComponent);
	}

	/**
	 * lásd javadoc följebb
	 *		a különbség a kettő között, hogy ez több tabfüles form esetén kell használni
	 * @param selectedValue
	 * 		az érték, amit szerkeszteni szeretnénk (lehet egy üres modell is)
	 * @param crudService
	 *		a service, amibe a mentés zajlani fog
	 * @param crudFormElementSupplier
	 * 		VaadinForm, amit meg kell jeleníteni
	 * @param tabNames
	 * 		a megjelenítendő neve a tabfüleknek
	 */
	public CrudFormEditComponent(final D selectedValue, final ToolboxCrudService<D> crudService, final Supplier<CrudFormElementCollection<?>> crudFormElementSupplier, final List<String> tabNames) {

		final MultiFormLayoutCrudFormComponent<D, CrudFormElementCollection<?>> formLayoutCrudFormComponent = new MultiFormLayoutCrudFormComponent<>(crudFormElementSupplier, tabNames);
		formLayoutCrudFormComponent.setDomainObject(selectedValue);
		formLayoutCrudFormComponent.setCrudOperation(ToolboxSysKeys.CrudOperation.UPDATE);
		formLayoutCrudFormComponent.setCrudAction(v -> {
			crudService.save(selectedValue);
		});

		formLayoutCrudFormComponent.init();

		this.addComponent(formLayoutCrudFormComponent);
	}

}
