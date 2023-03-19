package hu.lanoga.toolbox.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.jdbc.core.JdbcTemplate;

import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Actuator, {@link Runtime#getRuntime()} stb.
 */
@Slf4j
public class DiagnosticsHelper {

	private DiagnosticsHelper() {
		//
	}

	/**
	 * MiB
	 * 
	 * @return
	 */
	public static long getMaxMemory() {

		// long allocatedMemory = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
		// long presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;

		return Runtime.getRuntime().maxMemory() / 1024l / 1024l;
	}

	/**
	 * MiB
	 * 
	 * @return
	 */
	public static long getPresumableFreeMemory() {

		final long allocatedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
		final long presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;

		return presumableFreeMemory / 1024l / 1024l;
	}

	/**
	 * @return ha nem sikerül (pl.: nem elérhető az infó, vagy a Spring verzió változott és a kód már nem kompatibilis), akkor üres String
	 */
	@SuppressWarnings("rawtypes")
	public static String getMavenBuildInfo() {

		final StringBuilder sb = new StringBuilder();

		try {

			final Map<String, Object> infoMap = ApplicationContextHelper.getBean(InfoEndpoint.class).info();

			final Object build = infoMap.get("build");

			final Object group = ((Map) build).get("group");
			final Object artifact = ((Map) build).get("artifact");
			final Object version = ((Map) build).get("version");
			final Object time = ((Map) build).get("time");

			sb.append(group);
			sb.append(" ");
			sb.append(artifact);
			sb.append(" ");
			sb.append(version);
			sb.append(" (");
			sb.append(time);
			sb.append(")");

		} catch (final Exception e) {
			log.warn("Maven build info query failed!");
		}

		return sb.toString();

	}

	/**
	 * @return ha nem sikerül (pl.: nem elérhető az infó, vagy a Spring verzió változott és a kód már nem kompatibilis), akkor üres String
	 */
	@SuppressWarnings("rawtypes")
	public static String getGitCommitInfo() {

		final StringBuilder sb = new StringBuilder();

		try {

			final Map<String, Object> infoMap = ApplicationContextHelper.getBean(InfoEndpoint.class).info();

			final Object git = infoMap.get("git");

			final Object commit = ((Map) git).get("commit");
			final Object id = ((Map) commit).get("id");
			final Object dirty = ((Map) git).get("dirty");
			final Object time = ((Map) commit).get("time");

			sb.append(id);

			if (Boolean.parseBoolean(dirty.toString())) {
				sb.append("d");
			}

			sb.append(" (");
			sb.append(time);
			sb.append(")");

		} catch (final Exception e) {
			log.debug("Commit info query failed!");
		}

		return sb.toString();

	}

	/**
	 * @return ha nem sikerül (pl.: nem elérhető az infó, vagy a Spring verzió változott és a kód már nem kompatibilis), akkor üres String
	 */
	@SuppressWarnings("rawtypes")
	public static String getShortBuildAndGitCommitInfo() {

		final StringBuilder sb = new StringBuilder();

		try {

			final Map<String, Object> infoMap = ApplicationContextHelper.getBean(InfoEndpoint.class).info();

			final Object build = infoMap.get("build");

			final Object version = ((Map) build).get("version");

			sb.append(version);

		} catch (final Exception e) {
			log.warn("Maven build info query failed!");
		}

		try {

			final Map<String, Object> infoMap = ApplicationContextHelper.getBean(InfoEndpoint.class).info();

			final Object git = infoMap.get("git");

			final Object commit = ((Map) git).get("commit");
			final Object id = ((Map) commit).get("id");
			final Object describeShort = ((Map) id).get("describe-short");

			final Object dirty = ((Map) git).get("dirty");

			sb.append(" (");
			sb.append(describeShort);

			if (Boolean.parseBoolean(dirty.toString())) {
				sb.append("d");
			}

			sb.append(")");

		} catch (final Exception e) {
			log.debug("Commit info query failed!");
		}

		return sb.toString();

	}

	/**
	 * Java verzió, Spring verzió, PostgreSQL verzió; 
	 * csak ott megy, ahol Spring context már felépült
	 * 
	 * @return
	 * 		bal: Java, Spring, PostgreSQL verizók listaként;
	 * 		jobb: ugyanez összefűzve 
	 * 
	 * @see ApplicationContextHelper
	 */
	public static Pair<List<String>, String> getEnvVersionInfoAlpha() {

		final List<String> list = new ArrayList<>();
		final StringBuilder sb = new StringBuilder();

		// ---

		sb.append("Java: ");
		try {

			// Java 9+ eetén Runtime.Version version = Runtime.version(); is jó lehet majd

			final String str = System.getProperty("java.version");

			list.add(str);
			sb.append(str);

		} catch (final Exception e) {
			list.add("?");
			sb.append("?");
		}

		// ---

		sb.append("; Spring: ");
		try {

			sb.append(org.springframework.core.SpringVersion.getVersion());

		} catch (final Exception e) {
			list.add("?");
			sb.append("?");
		}

		// ---

		sb.append("; PostgreSQL: ");
		try {

			final JdbcTemplate jdbcTemplate = ApplicationContextHelper.getBean(JdbcTemplate.class);
			final String str = jdbcTemplate.queryForObject("SELECT version();", String.class);

			list.add(str);
			sb.append(str);

		} catch (final Exception e) {
			list.add("?");
			sb.append("?");
		}

		// ---

		// sb.append("; Vaadin 8: ");
		// try {
		//
		// String str = ...
		// list.add(str);
		// sb.append(str);
		//
		// } catch (Exception e) {
		// list.add("?");
		// sb.append("?");
		// }
		
		// ---
		
		return Pair.of(list, sb.toString());

	}

}
