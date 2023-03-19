package hu.lanoga.toolbox.service;

import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;

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
public abstract class RapidLazyEnhanceCrudService<T extends ToolboxPersistable, U extends RapidRepository<T>> extends RapidCrudService<T, U> implements LazyEnhanceCrudService<T> {

	public RapidLazyEnhanceCrudService(U repository) {
		super(repository);
	}

	@Override
	public abstract T enhance(T t);
	
}
