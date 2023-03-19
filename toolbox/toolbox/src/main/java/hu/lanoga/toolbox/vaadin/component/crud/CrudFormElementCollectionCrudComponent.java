package hu.lanoga.toolbox.vaadin.component.crud;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.flywaydb.core.internal.util.Pair;

import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.vaadin.binder.AutoBinder;
import hu.lanoga.toolbox.vaadin.util.UiHelper;

@SuppressWarnings("rawtypes")
public interface CrudFormElementCollectionCrudComponent<D extends ToolboxPersistable, C extends CrudFormElementCollection> extends CrudFormComponent<D> {

	Supplier<C> getCrudFormElementCollectionSupplier();

	void setCrudFormElementCollectionSupplier(Supplier<C> crudFormElementCollectionSupplier);

	default Pair<Button, C> initInner(final ToolboxSysKeys.CrudOperation crudOperation, final List<AbstractLayout> layouts, final Supplier<C> crudFormElementCollectionSupplier, final D modelObject, final Consumer<D> crudAction) {
		return initInner(crudOperation, layouts, crudFormElementCollectionSupplier, modelObject, crudAction, new HashMap<>());
	}

	@SuppressWarnings("unchecked")
	default Pair<Button, C> initInner(final ToolboxSysKeys.CrudOperation crudOperation, final List<AbstractLayout> layouts, final Supplier<C> crudFormElementCollectionSupplier, final D modelObject, final Consumer<D> crudAction, final Map<ToolboxSysKeys.CrudOperation, String> notificationMap) {

		final boolean isEditAllowed = !(ToolboxSysKeys.CrudOperation.READ.equals(crudOperation) || ToolboxSysKeys.CrudOperation.DELETE.equals(crudOperation));
		final boolean isAddView = ToolboxSysKeys.CrudOperation.ADD.equals(crudOperation);
		final boolean isReadView = ToolboxSysKeys.CrudOperation.READ.equals(crudOperation);

		// ---

		for (AbstractLayout layout : layouts) {
			layout.removeAllComponents();
		}
		
		// ---

		final C crudFormElementCollection = crudFormElementCollectionSupplier.get();

		crudFormElementCollection.preBind(modelObject, crudOperation);

		final AutoBinder binder = new AutoBinder(modelObject.getClass());
		binder.bindInstanceFields(crudFormElementCollection);
		binder.setBean(modelObject);

		crudFormElementCollection.afterBind(modelObject, crudOperation);

		UiHelper.addFields(layouts, crudFormElementCollection, isEditAllowed, isAddView);

		Button btn = null;

		if (!isReadView) {
			btn = UiHelper.buildCrudFormOpButton(modelObject, crudOperation, d -> {				
				crudFormElementCollection.preAction(modelObject, crudOperation);
			}, () -> binder.isValid(), d -> { 
				crudFormElementCollection.manualValidation(modelObject, crudOperation);
			}, crudAction, d -> {				
				crudFormElementCollection.afterAction(modelObject, crudOperation);
			}, notificationMap);
		}

		crudFormElementCollection.afterLayoutBuild(modelObject, crudOperation, btn, layouts);

		return Pair.of(btn, crudFormElementCollection);

	}

}