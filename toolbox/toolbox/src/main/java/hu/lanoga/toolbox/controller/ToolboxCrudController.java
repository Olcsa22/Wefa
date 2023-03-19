package hu.lanoga.toolbox.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Persistable;

public interface ToolboxCrudController<T extends Persistable<Integer>> {

	void delete(int id);

	T findOne(int id, String exportType);

	List<T> findAll(String exportType);
	
	Page<T> findAllPaged(String angularPageRequestAsJsonString, String exportType);
	
	T save(T t);

}
