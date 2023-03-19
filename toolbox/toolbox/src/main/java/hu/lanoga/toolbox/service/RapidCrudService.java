package hu.lanoga.toolbox.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;

import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import hu.lanoga.toolbox.repository.DefaultInMemoryRepository;
import hu.lanoga.toolbox.repository.RapidRepository;
import hu.lanoga.toolbox.repository.ToolboxPersistable;

/**
 * nem feltételnül Spring bean (tehát a {@link Secured} és a a {@link Transactional} nem megy) 
 * (jellemzően {@link DefaultInMemoryRepository} van mögötte)
 * 
 * @param <T>
 * @param <U>
 */
public class RapidCrudService<T extends ToolboxPersistable, U extends RapidRepository<T>> implements ToolboxCrudService<T> {

	protected U repository;
	
	public RapidCrudService(U repository) {
		super();
		this.repository = repository;
	}

	public U getRepository() {
		return repository;
	}
	
	public void setRepository(U repository) {
		this.repository = repository;
	}

	@Override
	public void delete(final int id) {
		repository.delete(id);
	}

	@Override
	public T findOne(final int id) {
		return repository.findOne(id);
	}

	@Override
	public List<T> findAll() {
		return (List<T>) repository.findAll();
	}

	@Override
	public Page<T> findAll(final BasePageRequest<T> pageRequest) {
		return repository.findAll(pageRequest);
	}

	@Override
	public T save(final T t) {
		return repository.save(t);
	}
	
	@Override
	public long count() {
		return repository.count();
	}
	
}
