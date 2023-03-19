package hu.lanoga.toolbox.repository;

import java.util.Optional;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface ToolboxRepository<T extends ToolboxPersistable> extends PagingAndSortingRepository<T, Integer>  {

	/**
	 * régi elnevezés (Spring 5 előtt), ugyanaz, mint a {@link #deleteById(Object)}
	 * 
	 * @param id
	 */
	void delete(Integer id);

	/**
	 * régi elnevezés (Spring 5 előtt), érdemben ugyanaz, mint a {@link #findById(Object)} 
	 * (viszont nem {@link Optional}-t ad vissza, hanem object/null-t)
	 * 
	 * @param id
	 * @return
	 */
	T findOne(Integer id);

}
