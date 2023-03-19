package hu.lanoga.toolbox.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.data.domain.Page;

import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.service.LazyEnhanceCrudService;

/**
 * mögöttes {@link LazyEnhanceCrudService}-ra rögtön (minden műveletnél, a visszatérés előtt) 
 * ráhívja az {@link LazyEnhanceCrudService#enhance(ToolboxPersistable)} műveletet
 * 
 * @param <T>
 * @param <U>
 */
public class AutoLazyEnhanceCrudController<T extends ToolboxPersistable, U extends LazyEnhanceCrudService<T>> extends DefaultCrudController<T, U> {

	@Override
	protected void handleExport(List<T> list, String exportType) {
		super.handleExport(service.enhance(list), exportType);
	}

	@Override
	public T findOne(int id, String exportType) {
		return service.enhance(super.findOne(id, exportType));
	}

	@Override
	public List<T> findAll(String exportType) {
		return service.enhance(super.findAll(exportType));
	}

	@Override
	public Page<T> findAllPaged(String angularPageRequestAsJsonString, String exportType) {
		return service.enhance(super.findAllPaged(angularPageRequestAsJsonString, exportType));
	}

	@Override
	public T save(@Valid T t) {
		return service.enhance(super.save(t));
	}

}
