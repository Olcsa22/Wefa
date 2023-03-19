package hu.lanoga.toolbox.repository.jdbc;

import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.repository.ToolboxRepository;

public interface JdbcRepository<T extends ToolboxPersistable> extends ToolboxRepository<T> {

	// noop (jelenleg nem ír elő extra művelet a ToolboxRepository-hoz képest)

}
