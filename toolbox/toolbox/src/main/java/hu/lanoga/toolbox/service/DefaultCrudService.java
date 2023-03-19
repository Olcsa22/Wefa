package hu.lanoga.toolbox.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.repository.ToolboxRepository;

/**
 * @see AdminOnlyCrudService
 */
@Retryable(
	      value = { org.springframework.dao.CannotSerializeTransactionException.class },
	      maxAttempts = 3,
	      backoff = @Backoff(delay = 1000, multiplier = 4, random = true)) // csak akkor megy, ha van @EnableRetry egy @Configuration class-on (tehát be van kapcsolva a "főkapcsolóval"
@Transactional 
@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
public abstract class DefaultCrudService<T extends ToolboxPersistable, U extends ToolboxRepository<T>> implements ToolboxCrudService<T> {

	// újabban fontos már, hogy minden transaction-ban legyen (ezért került az osztályra, nem a metódusokra): 
	// triggerek, izolációs szintek stb. okán
	
	// egyes metódusokon readOnly-ra van téve pluszban
	// (a readOnly opció postgres esetén a legtöbb írási jellegű műveletet (de nem abszolút mindent) tiltja, 
	// plusz némi optimalizációt is jelent sebességben (főként magasabb tranz. izoláció szintek esetén van jelentősége))
	
	@Autowired
	protected U repository;
	
	/**
	 * nagyon ritkán kell csak spec. esetekben
	 * 
	 * @return
	 */
	public U getRepository() {
		return repository;
	}

	@Override
	public void delete(final int id) {
		repository.delete(id);
	}

	@Override
	@Transactional(readOnly = true)
	public T findOne(final int id) {
		return repository.findOne(id);
	}

	@Override
	@Transactional(readOnly = true)
	public List<T> findAll() {
		return (List<T>) repository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public Page<T> findAll(final BasePageRequest<T> pageRequest) {
		return repository.findAll(pageRequest);
	}

	@Override
	public T save(final T t) {
		return repository.save(t);
	}

	@Override
	@Transactional(readOnly = true)
	public long count() {
		return repository.count();
	}
	
}
