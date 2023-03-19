package hu.lanoga.toolbox.service;

import java.util.List;

import org.springframework.data.domain.Page;

import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import hu.lanoga.toolbox.repository.ToolboxPersistable;

public interface ToolboxCrudService<T extends ToolboxPersistable> {

	void delete(int id);

	T findOne(int id);

	List<T> findAll();
	
	Page<T> findAll(BasePageRequest<T> pageRequest);

	T save(T t);
	
	long count();


}
