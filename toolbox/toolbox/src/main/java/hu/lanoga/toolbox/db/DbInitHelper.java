package hu.lanoga.toolbox.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.util.ToolboxAssert;
import lombok.extern.slf4j.Slf4j;

/**
 * Jelenleg a CREATE és a DROP funkció csak PostgreSQL-lel működnek (pg_database táblához kell hozzáférés...).
 * A {@link WAIT_FOR_TARGET_DATABASE} mód működik más DBMS-sel is.
 */
@Slf4j
@ConditionalOnMissingBean(name = "dbInitHelperOverrideBean")
@Component
public class DbInitHelper {

	private static final String SIMPLE_CONNECTION_VALIDATION = "SELECT 1";

	private static final String DATABASE_EXISTS = "SELECT 1 FROM pg_database WHERE datname = ?";
	private static final String CREATE_DATABASE = "CREATE DATABASE %s";
	private static final String DROP_DATABASE = "DROP DATABASE %s";

	private static void checkConnection(final String url, final String user, final String password, final int retrySleep, final int maxTryCount) throws InterruptedException {

		boolean isSuccessful = false;
		int tryCount = 0;

		do {

			if (tryCount > 0) {
				Thread.sleep(retrySleep);
			}

			++tryCount;

			try {

				try (Connection conn = DriverManager.getConnection(url, user, password)) {

					try (PreparedStatement ps = conn.prepareStatement(SIMPLE_CONNECTION_VALIDATION)) {
						isSuccessful = ps.execute();
					}

				}

			} catch (final Exception e) {
				log.debug("DbInitHelper, checkConnection failed (attempt: " + tryCount + "/" + maxTryCount + ")!", e);
			}

		} while (!isSuccessful && (tryCount <= maxTryCount));

		if (isSuccessful) {
			log.info("DbInitHelper, checkConnection (" + url + ") success!");
		} else {
			log.error("DbInitHelper, checkConnection (" + url + ") failed!");
			throw new ToolboxGeneralException("DbInitHelper, checkConnection (" + url + ") failed!!");
		}

	}

	private static void createIfNotExists(final String url, final String user, final String password, final String targetDatabase) {

		try (Connection conn = DriverManager.getConnection(url, user, password);) {

			boolean targetDatabaseExists = false;

			targetDatabaseExists = databaseExists(targetDatabase, conn);

			if (!targetDatabaseExists) {
				try (Statement st = conn.createStatement()) {

					// Postgresql complains "CREATE DATABASE cannot run inside a transaction block"
					// http://stackoverflow.com/q/26482777/14731

					final boolean transactionsEnabled = !conn.getAutoCommit();

					if (transactionsEnabled) {
						conn.setAutoCommit(true);
					}

					st.executeUpdate(String.format(CREATE_DATABASE, targetDatabase));

					if (transactionsEnabled) {
						conn.setAutoCommit(false);
					}

					log.info("DbInitHelper, database created: " + targetDatabase);
				}
			}

		} catch (final Exception e) {
			log.error("DbInitHelper, createIfNotExists failed!", e);
			throw new ToolboxGeneralException("DbInitHelper, createIfNotExists failed!", e);
		}
	}

	private static void dropIfExists(final String url, final String user, final String password, final String targetDatabase) {

		try (Connection conn = DriverManager.getConnection(url, user, password);) {

			boolean targetDatabaseExists = false;

			targetDatabaseExists = databaseExists(targetDatabase, conn);

			if (targetDatabaseExists) {
				try (Statement st = conn.createStatement()) {

					// Postgresql complains "CREATE DATABASE cannot run inside a transaction block"
					// http://stackoverflow.com/q/26482777/14731
					// (gondolom a DROP is hasonló... és csak autocommit-tal megy...)

					final boolean transactionsEnabled = !conn.getAutoCommit();

					if (transactionsEnabled) {
						conn.setAutoCommit(true);
					}

					st.executeUpdate(String.format(DROP_DATABASE, targetDatabase));

					if (transactionsEnabled) {
						conn.setAutoCommit(false);
					}

					log.info("DbInitHelper, database dropped: " + targetDatabase);
				}
			}

		} catch (final Exception e) {
			log.error("DbInitHelper, dropIfExists failed!", e);
			throw new ToolboxGeneralException("DbInitHelper, dropIfExists failed!", e);
		}
	}

	private static boolean databaseExists(final String targetDatabase, final Connection conn) throws SQLException {

		boolean targetDatabaseExists = false;

		try (PreparedStatement ps = conn.prepareStatement(DATABASE_EXISTS)) {

			ps.setString(1, targetDatabase);

			try (ResultSet resultSet = ps.executeQuery()) {
				targetDatabaseExists = resultSet.next();
			}
		}

		return targetDatabaseExists;
	}

	/**
	 * Exception-t dob, ha nem sikerül...
	 * 
	 * @param dbInitMode
	 * @param rootUrl
	 *            DB root URL (adatbázis nélkül)
	 * @param username
	 * @param password
	 * @param targetDatabase
	 * @param retrySleep
	 * @param maxTryCountParam
	 */
	public void initDb1(final ToolboxSysKeys.DbInitMode dbInitMode, String rootUrl, final String username, final String password, final String targetDatabase, final int retrySleep, final int maxTryCountParam) {
		
		try {

			if (ToolboxSysKeys.DbInitMode.SKIP.equals(dbInitMode)) {
				return;
			}

			// ---

			rootUrl = StringUtils.appendIfMissing(rootUrl, "/"); // ha nem '/'-re végződik, akkor JDBC exception-t kapunk

			if (ToolboxSysKeys.DbInitMode.WAIT_FOR_TARGET_DATABASE.equals(dbInitMode)) {
				rootUrl += targetDatabase;
			}

			// ---

			checkConnection(rootUrl, username, password, retrySleep, maxTryCountParam);

			// ---

			if (ToolboxSysKeys.DbInitMode.WAIT_FOR_ROOT_AND_CREATE_TARGET_IF_NOT_EXISTS.equals(dbInitMode)) {

				createIfNotExists(rootUrl, username, password, targetDatabase);

			} else if (ToolboxSysKeys.DbInitMode.WAIT_FOR_ROOT_AND_DROP_CREATE_TARGET.equals(dbInitMode)) {

				// flywayAnnotationHelper.deleteAllGenerated(); // nincs használva semelyik projektben, ezért kiszedve dependency-k (de menne)
				
				dropIfExists(rootUrl, username, password, targetDatabase);
				createIfNotExists(rootUrl, username, password, targetDatabase);

			}

		} catch (final Exception e) {
			log.error("DbInitHelper error!", e);
			throw new ToolboxGeneralException("DbInitHelper error!", e);
		}
	}

	@Value("${tools.dbinit.mode:}")
	private String initModeStr;
	
	@Value("${spring.datasource.url}")
	private String url;
	
	@Value("${spring.datasource.username}")
	private String username1;
	
	@Value("${spring.datasource.password}")
	private String password1;
	
	@Value("${tools.dbinit.root-username:}")
	private String username2;
	
	@Value("${tools.dbinit.root-password:}")
	private String password2;
	
	@Value("${tools.dbinit.connection-wait.max-try-count}")
	private int maxTryCount;
	
	@Value("${tools.dbinit.connection-wait.max-wait}")
	private int maxWait;
	
//	@Autowired
//	private FlywayAnnotationHelper flywayAnnotationHelper;

	/**
	 * Spring properties alapján (lásd: https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html).
	 * Exception-t dob, ha nem sikerül... 
	 */
	public void initDb2() {

		try {

			ToolboxAssert.isTrue(StringUtils.isNotBlank(url));

			ToolboxSysKeys.DbInitMode dbInitMode;

			try {
				dbInitMode = ToolboxSysKeys.DbInitMode.valueOf(initModeStr);
			} catch (final IllegalArgumentException e) {
				return; // ha nincs megadva property, akkor semmit sem csinál...
			}

			final String username = (StringUtils.isNotBlank(username2)) ? username2 : username1;
			final String password = (StringUtils.isNotBlank(password2)) ? password2 : password1;

			ToolboxAssert.isTrue(StringUtils.isNotBlank(username));
			ToolboxAssert.isTrue(StringUtils.isNotBlank(password));

			final String urlWithoutOptions = StringUtils.substringBefore(url, "?");

			final String rootUrl = StringUtils.substringBeforeLast(urlWithoutOptions, "/");
			final String targetDatabase = StringUtils.substringAfterLast(urlWithoutOptions, "/");

			ToolboxAssert.isTrue(StringUtils.isNotBlank(rootUrl));
			ToolboxAssert.isTrue(StringUtils.isNotBlank(targetDatabase));

			initDb1(dbInitMode, rootUrl, username, password, targetDatabase, maxWait / maxTryCount, maxTryCount);

		} catch (final Exception e) {
			log.error("DbInitHelper (initDbViaProps) error!", e);
			throw e;
		}

	}
}
