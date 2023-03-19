package hu.lanoga.toolbox.repository.jdbc;

import java.util.Set;

import hu.lanoga.toolbox.ToolboxSysKeys.JdbcInsertConflictMode;
import hu.lanoga.toolbox.repository.ToolboxPersistable;

public class ReadOnlyJdbcRepository<T extends ToolboxPersistable> extends DefaultJdbcRepository<T> {

	@Override
	public void delete(Integer id) {
		throw new UnsupportedOperationException("Read-only repository!");
	}

	@Override
	public void deleteById(Integer id) {
		throw new UnsupportedOperationException("Read-only repository!");
	}

	@Override
	public void delete(Iterable<? extends T> entities) {
		throw new UnsupportedOperationException("Read-only repository!");
	}

	@Override
	public void delete(T entity) {
		throw new UnsupportedOperationException("Read-only repository!");
	}

	@Override
	public void deleteAll(Iterable<? extends T> entities) {
		throw new UnsupportedOperationException("Read-only repository!");
	}

	@Override
	public void deleteAll() {
		throw new UnsupportedOperationException("Read-only repository!");
	}

	@Override
	public void disable(Integer id) {
		throw new UnsupportedOperationException("Read-only repository!");
	}

	@Override
	public void enable(Integer id) {
		throw new UnsupportedOperationException("Read-only repository!");
	}

	@Override
	public <S extends T> Iterable<S> save(Iterable<S> entities) {
		throw new UnsupportedOperationException("Read-only repository!");
	}

	@Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
		throw new UnsupportedOperationException("Read-only repository!");
	}

	@Override
	public <S extends T> S save(S entity) {
		throw new UnsupportedOperationException("Read-only repository!");
	}

	@Override
	public <S extends T> S save(S entity, Set<String> leaveOutFields, boolean findAfter) {
		throw new UnsupportedOperationException("Read-only repository!");
	}

	@Override
	public <S extends T> Integer insertWithConflictMode(S entity, Set<String> leaveOutFields, JdbcInsertConflictMode jdbcInsertConflictMode) {
		throw new UnsupportedOperationException("Read-only repository!");
	}

	@Override
	public <S extends T> Number insert(S entity, Set<String> leaveOutFields, JdbcInsertConflictMode jdbcInsertConflictMode) {
		throw new UnsupportedOperationException("Read-only repository!");
	}

	@Override
	public <S extends T> Number insert(S entity, Set<String> leaveOutFields, JdbcInsertConflictMode jdbcInsertConflictMode, String conflictModeColumnName, String conflictModeConstraintName) {
		throw new UnsupportedOperationException("Read-only repository!");
	}

	@Override
	public void lockTable() {
		throw new UnsupportedOperationException("Read-only repository!");
	}

	@Override
	public String txIdCurrent() {
		throw new UnsupportedOperationException("Read-only repository!");
	}
	
}
