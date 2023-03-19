package hu.lanoga.toolbox.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyShell;
import hu.lanoga.toolbox.exception.ToolboxException;
import lombok.extern.slf4j.Slf4j;

/**
 * experimental Groovy scriptezés... 
 */
@Slf4j
public final class ToolboxGroovyHelper {

	public static class ToolboxGroovyHelperException extends RuntimeException implements ToolboxException {

		public ToolboxGroovyHelperException() {
			super();
		}

		public ToolboxGroovyHelperException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}

		public ToolboxGroovyHelperException(final String message, final Throwable cause) {
			super(message, cause);
		}

		public ToolboxGroovyHelperException(final String message) {
			super(message);
		}

		public ToolboxGroovyHelperException(final Throwable cause) {
			super(cause);
		}

	}

	private ToolboxGroovyHelper() {
		//
	}

	@SuppressWarnings("unchecked")
	public static <V> Class<V> buildClass(final String groovyScriptStr) {
		GroovyClassLoader groovyClassLoader = null;

		try {

			groovyClassLoader = new GroovyClassLoader();

			return groovyClassLoader.parseClass(groovyScriptStr);

		} catch (final Exception e) {
			throw new ToolboxGroovyHelperException("buildClass error!", e);
		} finally {
			if (groovyClassLoader != null) {
				try {
					groovyClassLoader.close();
				} catch (final IOException e) {
					log.error("groovyClassLoader close failed!", e);
				}
			}

		}

	}

	@SuppressWarnings("unchecked")
	public static <V> V buildClassInstance(final String groovyScriptStr) {

		GroovyClassLoader groovyClassLoader = null;

		try {

			groovyClassLoader = new GroovyClassLoader();

			final Class<V> groovyClass = groovyClassLoader.parseClass(groovyScriptStr);
			final GroovyObject groovyObj = (GroovyObject) groovyClass.newInstance();

			// System.out.println(groovyObj.getClass());

			return (V) groovyObj;

		} catch (final Exception e) {
			throw new ToolboxGroovyHelperException("buildClassInstance error!", e);
		} finally {
			if (groovyClassLoader != null) {
				try {
					groovyClassLoader.close();
				} catch (final IOException e) {
					log.error("groovyClassLoader close failed!", e);
				}
			}

		}

	}

	// public static List experiment2(String codeBodyStr) {
	//
	// try {
	//
	// final StringBuilder sbGroovyScript = new StringBuilder();
	// sbGroovyScript.append("import hu.lanoga.*;");
	// sbGroovyScript.append("class C {");
	// sbGroovyScript.append(" List f() {");
	// sbGroovyScript.append(codeBodyStr);
	// sbGroovyScript.append(" }");
	// sbGroovyScript.append("}");
	//
	// // ---
	//
	// Class groovyClass = new GroovyClassLoader().parseClass(sbGroovyScript.toString());
	// GroovyObject groovyObj = (GroovyObject) groovyClass.newInstance();
	//
	// Object result = groovyObj.invokeMethod("f", new Object[] {});
	//
	// // ---
	//
	// return (List) result;
	//
	// } catch (Exception e) {
	// throw new ToolboxGroovyHelperException("getListData1 error!", e);
	// }
	//
	// }

	/**
	 * experimental
	 * 
	 * @param scriptText
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static List getListData(final String scriptText) {

		// minta hívásra:
		// long currentMillis = System.currentTimeMillis(); // csak mérés
		//
		// List listData = ToolboxGroovyHelper.getListData2("hu.lanoga.toolbox.spring.ApplicationContextHelper.getBean(hu.lanoga.waste.service.DealService.class).findAll();");
		//
		// for (Object o : listData) {
		// System.out.println(o.toString());
		// }
		//
		// System.out.println("---> " + (System.currentTimeMillis() - currentMillis));

		// ---

		try {

			return (List) new GroovyShell().evaluate(scriptText);

		} catch (final Exception e) {
			throw new ToolboxGroovyHelperException("getListData error!", e);
		}

	}

	/**
	 * @param scriptText
	 * @param logDesc
	 * @return
	 * 
	 * @see #evaluate(String, String, Map, String)
	 */
	public static Object evaluate(final String scriptText, final String logDesc) {
		return evaluate(null, scriptText, null, logDesc);
	}
	
	/**
	 * @param scriptText
	 * @return
	 * 
	 * @see #evaluate(String, String, Map, String)
	 */
	public static Object evaluate(final String scriptText) {
		return evaluate(null, scriptText, null, null);
	}

	/**
	 * @param scriptResource
	 * 		{@link PathMatchingResourcePatternResolver}... empty/null esetén skip
	 * @param scriptText
	 * 		empty/null esetén skip (ha scriptResource és scriptText is van, akkor a scriptText a végére lesz konkatenálva)
	 * @param valueMap
	 * 		empty/null esetén skip
	 * @param logDesc
	 * 		error esetén hozzá lesz fűzve a log.error üzenethez (későbbi visszaazonoítás végett)
	 * @return
	 */
	public static Object evaluate(final String scriptResource, final String scriptText, final Map<String, Object> valueMap, String logDesc) {

		try {
			
			if (logDesc == null) {
				logDesc = "-";
			}
			
			log.debug("Groovy evaluate... logDesc: " + logDesc);

			String scriptStr = "";

			if (StringUtils.isNotBlank(scriptResource)) {

				final Resource sourceResource;

				try {
					sourceResource = new PathMatchingResourcePatternResolver().getResources(scriptResource)[0];
				} catch (final Exception e) {
					throw new ToolboxGroovyHelperException("Missing sourceFile: " + scriptResource, e);
				}

				try (InputStream is = sourceResource.getInputStream()) {
					scriptStr += IOUtils.toString(is, "UTF-8");
				}

			}

			if (StringUtils.isNotBlank(scriptText)) {
				scriptStr += scriptText;
			}

			// TODO: GroovyShell: mi különbség a setProperty és a setVariable között?

			return new GroovyShell(new Binding(valueMap)).evaluate(scriptStr);

		} catch (final Exception e) {
			throw new ToolboxGroovyHelperException("Groovy evaluate error! logDesc: " + logDesc, e);
		}

	}

}
