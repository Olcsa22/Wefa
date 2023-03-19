package hu.lanoga.toolbox.vaadin.component.crud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import lombok.Getter;
import lombok.Setter;

/**
 * @param <D>
 * @param <C>
 * 
 * @see MultiFormLayoutCrudFormComponent
 */
@SuppressWarnings("rawtypes")
@Getter
@Setter
public class FormLayoutCrudFormComponent<D extends ToolboxPersistable, C extends CrudFormElementCollection> extends FormLayout implements CrudFormElementCollectionCrudComponent<D, C> {

	private D domainObject;
	private ToolboxSysKeys.CrudOperation crudOperation;
	private Consumer<D> crudAction;

	private Supplier<C> crudFormElementCollectionSupplier;
	private Map<ToolboxSysKeys.CrudOperation, String> notifMap = new HashMap<>();

	private Button btnMain;

	public FormLayoutCrudFormComponent() {
		super();
		this.setMargin(true);
	}

	public FormLayoutCrudFormComponent(final Supplier<C> crudFormElementCollectionSupplier) {
		this();

		this.crudFormElementCollectionSupplier = crudFormElementCollectionSupplier;
	}

	public FormLayoutCrudFormComponent(final Supplier<C> crudFormElementCollectionSupplier, final Map<ToolboxSysKeys.CrudOperation, String> notifMap) {
		this();

		this.crudFormElementCollectionSupplier = crudFormElementCollectionSupplier;

		this.notifMap = notifMap;
	}

	@Override
	public void init() {
		
		List<AbstractLayout> list = new ArrayList<>();
		list.add(this);
		
		this.btnMain = this.initInner(this.crudOperation, list, this.crudFormElementCollectionSupplier, this.domainObject, this.crudAction, this.notifMap).getLeft();
		if (this.btnMain != null) {
			this.addComponent(this.btnMain);
		}
	}

	/**
	 * lehet null is (ha nincs gomb)
	 */
	@Override
	public Button getMainButton() {
		return this.btnMain;
	}

}
