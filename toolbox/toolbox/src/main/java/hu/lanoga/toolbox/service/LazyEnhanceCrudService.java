package hu.lanoga.toolbox.service;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.repository.jdbc.View;

/**
 * {@link View} mezők kézi feltöltésére használható (amikor a {@link DefaultJdbcRepository#getInnerSelect()} nem alkalmazható):
 * one-to-many lekérések, generált értékek, külső API-ból lekért adatok... 
 * 
 * (az {@link AutoLazyEnhanceCrudController} és a {@link CrudGridComponent} automatikusan megcsinálja az {@link #enhance(ToolboxPersistable)} műveletet) 
 * 
 * ne hívjuk meg duplán feleslegesen, ne hívjuk meg ott, ahol az extra értékek nem kellenek éppen... 
 * többnyire csak a controller/UI réteg számára kellenek (esetleg export fájl kapcsán) 
 * 
 * (mj.: van több collection-re vonatkozó default impl. metódus is, szükség esetén ezek felülírhatóak (pl. teljesítmény optimalizáció miatt))
 * 
 * @param <T>
 */
public interface LazyEnhanceCrudService<T extends ToolboxPersistable> extends ToolboxCrudService<T> {

	/**
	 * nem csinál másolatot (a paraméter page-et (annak elemeit) módosítja, return value csak a kényelmes láncolhatóság miatt van)
	 * 
	 * @param set
	 * @return
	 */
	default org.springframework.data.domain.Page<T> enhance(final org.springframework.data.domain.Page<T> page) {

		enhance(page.getContent());

		return page;
	}
	
	/**
	 * nem csinál másolatot (a paraméter page-et (annak elemeit) módosítja, return value csak a kényelmes láncolhatóság miatt van)
	 * 
	 * @param iterable
	 * @return
	 */
	default Iterable<T> enhance(final Iterable<T> iterable) {
		
		if (iterable == null) {
			return null;
		}

		final Iterator<T> itr = iterable.iterator();

		while (itr.hasNext()) {
			final T t = itr.next();
			enhance(t);
		}

		return iterable;

	}

	/**
	 * nem csinál másolatot (a paraméter page-et (annak elemeit) módosítja, return value csak a kényelmes láncolhatóság miatt van)
	 * 
	 * @param set
	 * @return
	 */
	default Set<T> enhance(final Set<T> set) {
		
		if (set == null) {
			return null;
		}
		
		for (final T t : set) {
			enhance(t);
		}
		return set;
	}

	/**
	 * nem csinál másolatot (a paraméter page-et (annak elemeit) módosítja, return value csak a kényelmes láncolhatóság miatt van)
	 * 
	 * @param list
	 * @return
	 */
	default List<T> enhance(final List<T> list) {
		
		if (list == null) {
			return null;
		}
		
		for (final T t : list) {
			enhance(t);
		}
		return list;
	}

	/**
	 * nem csinál másolatot (a paraméter page-et (annak elemeit) módosítja, return value csak a kényelmes láncolhatóság miatt van)
	 * 
	 * @param collection
	 * @return
	 */
	default Collection<T> enhance(final Collection<T> collection) {
		
		if (collection == null) {
			return null;
		}
		
		for (final T t : collection) {
			enhance(t);
		}
		return collection;
	}

	/**
	 * nem csinál másolatot (a paraméter page-et (annak elemeit) módosítja, return value csak a kényelmes láncolhatóság miatt van)
	 * 
	 * @param t
	 * @return
	 */
	T enhance(final T t);

}
