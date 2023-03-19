package hu.lanoga.toolbox.repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vaadin.v7.data.util.BeanContainer;
import com.vaadin.v7.data.util.BeanItem;
import com.vaadin.v7.data.util.filter.SimpleStringFilter;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import hu.lanoga.toolbox.filter.internal.SearchCriteria;
import hu.lanoga.toolbox.json.jackson.JacksonHelper;
import hu.lanoga.toolbox.repository.jdbc.View;

/**
 * experimental, egyelőre tenant-ot, createdBy/createdOn stb. értéket nem kezel... 
 * (Vaadin data alapú, de semmi köze a Vaadin UI-hoz, attól függetlenül is használható akár) 
 * (alapvetően úgy kezelhető, mint a DB, egy különbség van, a {@link View} mezők tartalmát megőrzi) 
 * (olyan model osztállyal, ahol a mezőkön {@link JsonIgnore} van lehetnek problémák; 
 * mert Jackson alapú deepCopy van használva itt)
 * 
 * 
 * @param <T>
 */
@SuppressWarnings({ "unchecked", "deprecation" })
public class DefaultInMemoryRepository<T extends ToolboxPersistable> implements RapidRepository<T> {

	final private Class<T> entityType;

	final private BeanContainer<Integer, T> bc;
	
	volatile int idHolder = 0;

	public DefaultInMemoryRepository(Class<T> entityType) {
		this.entityType = entityType; // (Class<T>) GenericTypeResolver.resolveTypeArguments(this.getClass(), RapidRepository.class)[0];
		this.bc = new BeanContainer<>(entityType);
	}

	@Override
	synchronized public <S extends T> void initWithData(final Iterable<S> entities) {

		deleteAll();
		
		if (entities != null) {
			saveAll(entities);
			// lastInitEntities = entities;
		}

		idHolder = (int) this.count();
	}

	@Override
	synchronized public Page<T> findAll(final Pageable pageable) {

		bc.removeAllContainerFilters();

		// ---

		final LinkedHashSet<SearchCriteria> searchCriteriaSet = ((BasePageRequest<T>) pageable).getSearchCriteriaSet();

		if ((searchCriteriaSet != null) && !searchCriteriaSet.isEmpty()) {
			
			for (final SearchCriteria searchCriteria : searchCriteriaSet) {
				
				if (ToolboxSysKeys.SearchCriteriaOperation.LIKE.equals(searchCriteria.getOperation()) || ToolboxSysKeys.SearchCriteriaOperation.EQ.equals(searchCriteria.getOperation())) {
					bc.addContainerFilter(new SimpleStringFilter(searchCriteria.getFieldName(), searchCriteria.getValue().toString(), true, false));
				}

			}
			
		}
		
		// ---

		if (pageable.getSort() != null) {

			for (final Order o : pageable.getSort()) {

				// TODO: megírni több szintű sort-ra is

				bc.sort(new String[] { o.getProperty() }, new boolean[] { o.getDirection().isAscending() });
				break;
			}

		}

		// ---

		final List<T> list = new ArrayList<>();

		for (final Integer id : bc.getItemIds((int) pageable.getOffset(), pageable.getPageSize())) {
		
			BeanItem<T> item = bc.getItem(id);

			if (item != null) {
				list.add((T) JacksonHelper.deepCopy(item.getBean(), entityType));
			}
			
		}

		// ---
		
		PageImpl<T> ret = new PageImpl<>(list, pageable, bc.getItemIds().size());

		bc.removeAllContainerFilters();

		return ret;
	}

	@Override
	synchronized public void delete(final Integer id) {
		// chm.remove(id);
		bc.removeItem(id);
	}

	@Override
	synchronized public T findOne(final Integer id) {

		final BeanItem<T> item = bc.getItem(id);
		if (item == null) {
			return null;
		}

		return (T) JacksonHelper.deepCopy(item.getBean(), entityType);

	}

	@Override
	synchronized public Iterable<T> findAll(final Sort sort) {
		throw new UnsupportedOperationException("Not implemented in the base repository!");
	}

	@Override
	synchronized public <S extends T> S save(final S entity) {

		final Class<? extends ToolboxPersistable> c = entity.getClass();

		final S s = (S) JacksonHelper.deepCopy(entity, c);

		if (s.getId() == null) {
			s.setId(idHolder);
			idHolder = idHolder + 1;
		}
		
		bc.removeItem(s.getId()); // ez is kell, mert az addItem csak akkor megy, ha nincs még olyan id...
		bc.addItem(s.getId(), s);

		return (S) JacksonHelper.deepCopy(s, c);
	}

	@Override
	synchronized public <S extends T> Iterable<S> saveAll(final Iterable<S> entities) {

		final List<S> retList = new ArrayList<>();

		for (final S s : entities) {
			retList.add(this.save(s));
		}

		return retList;
	}

	@Override
	synchronized public Optional<T> findById(final Integer id) {
		return Optional.ofNullable(findOne(id));
	}

	@Override
	synchronized public boolean existsById(final Integer id) {
		return bc.containsId(id);
	}

	@Override
	synchronized public Iterable<T> findAll() {

		final List<T> list = new ArrayList<>();

		for (final Integer id : bc.getItemIds()) {
			list.add((T) JacksonHelper.deepCopy(bc.getItem(id).getBean(), entityType));
		}

		return list;

	}

	@Override
	synchronized public Iterable<T> findAllById(final Iterable<Integer> ids) {
		throw new UnsupportedOperationException("Not implemented in the base repository!");
	}

	@Override
	synchronized public long count() {
		return bc.size();
	}

	@Override
	synchronized public void deleteById(final Integer id) {
		delete(id);
	}

	@Override
	synchronized public void delete(final T entity) {
		delete(entity.getId());
	}

	@Override
	synchronized public void deleteAll(final Iterable<? extends T> entities) {
		for (final T t : entities) {
			this.delete(t.getId());
		}
	}

	@Override
	synchronized public void deleteAll() {
		bc.removeAllItems();
	}

	@Override
	synchronized public void deleteAllById(Iterable<? extends Integer> ids) {
		for (Integer id : ids) {
			deleteById(id);
		}
	}

}
