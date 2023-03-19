package hu.lanoga.toolbox.repository;

public interface RapidRepository<T extends ToolboxPersistable> extends ToolboxRepository<T> {

	<S extends T> void initWithData(Iterable<S> entities);

}