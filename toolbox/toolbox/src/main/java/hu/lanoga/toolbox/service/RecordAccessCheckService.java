package hu.lanoga.toolbox.service;

/**
 * régi toolbox dolog
 *
 * @param <L>
 * @param <R>
 * 
 * @see SecurityUtil#hasRecordAccess(Class, Object, Object)
 * 
 * @deprecated
 */
@Deprecated
public interface RecordAccessCheckService<L, R> {

	/**
	 * sorrendnek igazodik a {@link #hasAccessByIds(int, int)} sorrendhez (az implementálónak is ügyelnie kell a sorrendre)
	 * 
	 * @param left
	 * @param right
	 * @return
	 */
	boolean hasAccess(L left, R right);
	
	/**
	 * objecktumok sorrendjéhez lásd {@link #hasAccess(Object, Object)} (az implementálónak is ügyelnie kell a sorrendre)
	 * 
	 * @param leftId
	 * @param rightId
	 * @return
	 */
	boolean hasAccessByIds(int leftId, int rightId);

}
