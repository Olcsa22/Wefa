package hu.lanoga.toolbox.repository;

import java.util.List;

public interface ToolboxAdvancedRepository<T extends ToolboxPersistable> extends ToolboxRepository<T> {

	void enable(final Integer id);

	void disable(final Integer id);

	List<T> findAllBy(String fieldName, Object value);

	List<T> findAllBy(String firstFiedName, Object firstValue, String secondFieldName, Object secondValue);

	T findOneBy(String fieldName, Object value);

	T findOneBy(String firstFieldName, Object firstValue, String secondFieldName, Object secondValue);

	boolean existsBy(String fieldName, Object value);

	boolean existsBy(String firstFieldName, Object firstValue, String secondFieldName, Object secondValue);

}
