package hu.lanoga.toolbox.vaadin.component.crud;

import java.util.function.Consumer;

import com.vaadin.ui.Button;
import com.vaadin.ui.Component;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.repository.ToolboxPersistable;

public interface CrudFormComponent<D extends ToolboxPersistable> extends Component {

	/**
	 * operation, domainObject, succesAction stb. setter után hívandó, 
	 * "nulláról" újraépíti a felületet (immár az új/aktuális domainObject stb. alapján)
	 */
	void init();

	ToolboxPersistable getDomainObject();

	void setDomainObject(D domainObject);

	ToolboxSysKeys.CrudOperation getCrudOperation();

	void setCrudOperation(ToolboxSysKeys.CrudOperation crudOperation);

	Consumer<D> getCrudAction();

	void setCrudAction(Consumer<D> crudAction);

	/**
	 * többnyire a save button
	 * 
	 * @return
	 */
	Button getMainButton();

}