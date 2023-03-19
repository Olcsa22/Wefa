package hu.lanoga.toolbox.vaadin.component.crud;

import java.util.function.Supplier;

import com.vaadin.ui.VerticalLayout;

import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.service.ToolboxCrudService;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrudGridNextGenComponent<D extends ToolboxPersistable> extends VerticalLayout {

	/**
	 * fontos, hogy {@link Class#newInstance()} módon példányosítható kell legyen (pl. az add művelet miatt)
	 */
	private final Class<D> modelType;
	private final ToolboxCrudService<D> crudService;
	private final Supplier<CrudFormComponent<D>> crudFormComponentSupplier;
	
	public CrudGridNextGenComponent(Class<D> modelType, ToolboxCrudService<D> crudService, Supplier<CrudFormComponent<D>> crudFormComponentSupplier) {
		super();
		this.modelType = modelType;
		this.crudService = crudService;
		this.crudFormComponentSupplier = crudFormComponentSupplier;
	}


}
