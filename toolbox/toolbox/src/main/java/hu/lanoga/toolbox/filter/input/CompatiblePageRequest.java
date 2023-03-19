package hu.lanoga.toolbox.filter.input;

import hu.lanoga.toolbox.filter.internal.BasePageRequest;

/**
 * {@link hu.lanoga.toolbox.filter.internal.BasePageRequest} állítható elő belőle 
 * (lásd {@code getAsBasePageRequest} metódus)...
 */
public interface CompatiblePageRequest<T> {

	/**
	 * amennyiben user inputból, frontendről érkezik, akkor a későbbi felhasználásnál gondoskodni kell arról, hogy a mezőnevek helyesek-e (jellemzően egy whitelist-tal)
	 * (a {@link DefaultJdbcRepository} így működik)
	 * 
	 * @return
	 */
	public BasePageRequest<T> getAsBasePageRequest();

}
