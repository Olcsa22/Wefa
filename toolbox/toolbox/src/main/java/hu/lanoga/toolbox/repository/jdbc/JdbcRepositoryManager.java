package hu.lanoga.toolbox.repository.jdbc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.simpleflatmapper.jdbc.spring.JdbcTemplateMapperFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Order;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.RepositoryTenantMode;
import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import hu.lanoga.toolbox.filter.internal.SearchCriteria;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.util.ToolboxAssert;
import hu.lanoga.toolbox.util.ToolboxStringUtil;
import lombok.Getter;

/**
 * JDBC, Spring JdbcTemplate kezelés... (Spring context kell egyes metódusokhoz!)... 
 * (a {@link DefaultJdbcRepository} egy konkrét használó, de függetlenül is működik, használható részleteiben is pár dologra)
 * 
 * @see JdbcRepositoryAuditManager
 * @see JdbcRepositoryTenantManager
 * @see JdbcRepositoryVariableManager
 */
@SuppressWarnings("rawtypes")
@Getter
public class JdbcRepositoryManager<T extends ToolboxPersistable> {

	private static final String DEFAULT_ID_COLUMN_NAME = "id";

	/**
	 * @see ToolboxSysKeys.JdbcDisableMode
	 */
	private static final String DEFAULT_DISABLE_MODE_STR = "ENABLED_COLUMN";

	/**
	 * @see ToolboxSysKeys.JdbcDisableMode
	 */
	private static final String DEFAULT_DISABLE_COLUMN_NAME = "enabled";

	// ========================================================================
	
	/**
	 * a {@link BeanPropertyRowMapper} helyett
	 */
	private static JdbcTemplateMapperFactory jdbcTemplateMapperFactory = JdbcTemplateMapperFactory.newInstance(); // TODO: is this really thread-safe?
	
	/**
	 * rowMapper-t ad vissza (válaszott model osztályához, jellemző extended model-el oszályokhoz jó)
	 *
	 * @return
	 */
	public static <X> RowMapper<X> newRowMapperInstance(final Class<X> entityType) {
		return jdbcTemplateMapperFactory.newRowMapper(entityType);
	}
	
	// ========================================================================
	
	/**
	 * @param expected
	 * @param actual
	 * @throws EmptyResultDataAccessException
	 * @throws IncorrectResultSizeDataAccessException
	 */
	public static void checkChangedRowCount(final int expected, final int actual) throws EmptyResultDataAccessException, IncorrectResultSizeDataAccessException {

		if (actual == 0) {
			throw new EmptyResultDataAccessException(expected);
		}

		if (actual != expected) {
			throw new IncorrectResultSizeDataAccessException(expected, actual);
		}

	}

	/**
	 * Java mezőnév -> DB tábla oszlopnév...
	 * 
	 * a visszaadott map a szokványos map-ektől eltérően get() hivás és hiányzó value esetén exception-t dob (nem null-t ad vissza)...
	 * a contains művelet sztenderd HashMap-nek megfelelő
	 *
	 * @param clazz
	 * @return
	 * @see AbstractJdbcPersistable
	 * @see Column
	 */
	public static Map<String, String> collectFieldAndColumnNames(final Class<?> clazz) {

		final Map<String, String> hm = new HashMap<String, String>() {

			@Override
			public String get(final Object key) {
				final String value = super.get(key);

				if (StringUtils.isBlank(value)) {
					throw new JdbcRepositoryException("collectFieldAndColumnNames error (blank)! clazz: " + clazz.getName() + ", " + key + ", " + value);
				}

				return value;
			}

		};

		final List<Field> allFields = FieldUtils.getAllFieldsList(clazz);

		for (final Field field : allFields) {

			if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isAbstract(field.getModifiers())) {

				final Column columnAnnotation = field.getAnnotation(Column.class);

				final String columnName;

				if (columnAnnotation == null) {
					columnName = ToolboxStringUtil.camelCaseToUnderscore(field.getName());
				} else {
					columnName = columnAnnotation.name().equals("") ? ToolboxStringUtil.camelCaseToUnderscore(field.getName()) : columnAnnotation.name();
				}

				hm.put(field.getName(), columnName);
			}
		}

		return Collections.unmodifiableMap(hm);

	}

	/**
	 * System.currentTimeMillis() alapján
	 *
	 * @return
	 */
	public static Timestamp getNowTs() {
		return new Timestamp(System.currentTimeMillis());
	}

	// ========================================================================

	/**
	 * @deprecated
	 * @see JdbcRepositoryTenantManager
	 */
	@Deprecated
	public static void setTlTenantId(final int tenantId) {
		JdbcRepositoryTenantManager.setTlTenantId(tenantId);
	}

	/**
	 * @deprecated
	 * @see JdbcRepositoryTenantManager
	 */
	@Deprecated
	public static void clearTlTenantId() {
		JdbcRepositoryTenantManager.clearTlTenantId();
	}

	/**
	 * @deprecated
	 * @see JdbcRepositoryTenantManager
	 */
	@Deprecated
	public static Integer getTlTenantId() {
		return JdbcRepositoryTenantManager.getTlTenantId();
	}
	
	// ========================================================================

	private final Class<T> entityType;
	private final RepositoryTenantMode tenantMode;
	private final Map<String, String> fieldAndColumnNames;
	private final RowUnmapper<T> rowUnmapper;

	private String tableName;
	private String idColumnName;
	private String tenantIdColumnName;

	private final ToolboxSysKeys.JdbcDisableMode jdbcDisableMode;
	private final String disableModeColumnName;

	private JdbcRepositoryVariableManager jdbcRepositoryVariableManager;

	private JdbcRepositoryAuditManager jdbcRepositoryAuditManager;

	private JdbcRepositoryTenantManager jdbcRepositoryTenantManager;

	@SuppressWarnings("unchecked")
	public JdbcRepositoryManager(final Class<T> entityType, final RepositoryTenantMode tenantMode) {

		ToolboxAssert.notNull(entityType);
		ToolboxAssert.notNull(tenantMode);

		this.entityType = entityType;
		this.fieldAndColumnNames = JdbcRepositoryManager.collectFieldAndColumnNames(this.entityType);
		this.tenantMode = tenantMode;
		this.rowUnmapper = new AnnotationBasedRowUnmapper(this.entityType, this.fieldAndColumnNames); // AnnotationBasedRowUnmapper thread-safe (de ez nem biztos, hogy igaz más implmentációkra, illetve a fordított irányú RowMapper-ekre sem!)

		// ---

		// reflection/annotáció feldolgozás rész, táblanév stb. meghatározása...

		try {

			determineTableName();
			determineIdAndTenantIdColumnNames();

			// ---

			this.jdbcDisableMode = ToolboxSysKeys.JdbcDisableMode.valueOf(DEFAULT_DISABLE_MODE_STR); // TODO: ehhez is kell annotáció
			this.disableModeColumnName = DEFAULT_DISABLE_COLUMN_NAME;
			
			// ---
			
			jdbcRepositoryTenantManager = new JdbcRepositoryTenantManager(tenantMode, tenantIdColumnName);
			jdbcRepositoryVariableManager = new JdbcRepositoryVariableManager(entityType, jdbcRepositoryTenantManager);
			jdbcRepositoryAuditManager = new JdbcRepositoryAuditManager();

		} catch (final Exception e) {
			throw new JdbcRepositoryException("JdbcRepositoryManager init exception (reflection error etc.)!", e);
		}

	}

	private void determineIdAndTenantIdColumnNames() {
		
		final Field[] fields = this.entityType.getDeclaredFields();
		final List<Field> allFields = new ArrayList<>(Arrays.asList(fields));

		for (final Field field : allFields) {
			
			if (field.getDeclaredAnnotations().length > 0) {

				final Annotation[] annotations = field.getDeclaredAnnotations();

				for (final Annotation annotation : annotations) {

					if (annotation.annotationType().equals(Column.class)) {

						final Column columnAnnotation = ((Column) annotation);

						if (columnAnnotation.isId()) {

							if (StringUtils.isNotBlank(this.idColumnName)) {
								throw new JdbcRepositoryException("Multiple id coulmns!");
							}

							this.idColumnName = columnAnnotation.name();

							if (StringUtils.isBlank(this.idColumnName)) {
								this.idColumnName = ToolboxStringUtil.camelCaseToUnderscore(field.getName());
							}

						}

						if (columnAnnotation.isTenantId()) {

							if (StringUtils.isNotBlank(tenantIdColumnName)) {
								throw new JdbcRepositoryException("Multiple tenant id columns!");
							}

							tenantIdColumnName = columnAnnotation.name();

							if (StringUtils.isBlank(tenantIdColumnName)) {
								tenantIdColumnName = ToolboxStringUtil.camelCaseToUnderscore(field.getName());
							}

						}

					}
				}
			}
		}
		
		// ---

		if (StringUtils.isBlank(this.idColumnName)) {
			this.idColumnName = DEFAULT_ID_COLUMN_NAME;
		}
		
		if (StringUtils.isBlank(tenantIdColumnName)) {
			tenantIdColumnName = JdbcRepositoryTenantManager.DEFAULT_TENANT_ID_COLUMN_NAME;
		}
	}

	private void determineTableName() {
		
		String tn;

		final Table tableAnnotation = this.entityType.getAnnotation(Table.class);

		if ((tableAnnotation != null) && StringUtils.isNotBlank(tableAnnotation.name())) {

			tn = tableAnnotation.name();

			if (tn.contains("#random")) {
				tn = StringUtils.replace(tn, "#random", StringUtils.replace(UUID.randomUUID().toString(), "-", "_"));
			}

		} else {
			tn = ToolboxStringUtil.camelCaseToUnderscore(this.entityType.getSimpleName());
		}
		
		this.tableName = tn;
	}

	/**
	 * {@link BasePageRequest} / {@link Pageable} / {@link SearchCriteria}... alapon SQL query-k összállítása...
	 * 
	 * @param pageable
	 * @param prependWhereKeyword
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Pair<String, List<Object>> buildSqlWherePart(final Pageable pageable, final boolean prependWhereKeyword) {

		final List<Object> columnValueParams = new ArrayList<>(); // JdbcTemlate behelyettesítéshez...
		final StringBuilder sbWherePart = new StringBuilder();

		if (pageable instanceof BasePageRequest) {

			final BasePageRequest basePageRequest = (BasePageRequest) pageable;

			final LinkedHashSet<SearchCriteria> searchCriteriaSet = basePageRequest.getSearchCriteriaSet();

			if ((searchCriteriaSet != null) && !searchCriteriaSet.isEmpty()) {

				int i = 0;

				for (final SearchCriteria searchCriteria : searchCriteriaSet) {

					if (i == 0) {

						if (prependWhereKeyword) {
							sbWherePart.append(" WHERE ");
						}

						sbWherePart.append(" (");
					}

					if (i > 0) {
						sbWherePart.append(") ");
						sbWherePart.append(basePageRequest.getSearchCriteriaLogicalOperation().name());
						sbWherePart.append(" (");
					}

					if (!searchCriteria.isEnabled()) {

						sbWherePart.append("2 = 2");

					} else if (ToolboxSysKeys.SearchCriteriaOperation.BETWEEN.equals(searchCriteria.getOperation())) {

						sbWherePart.append(this.fieldAndColumnNames.get(searchCriteria.getFieldName()));
						sbWherePart.append(" BETWEEN ?");

						columnValueParams.add(searchCriteria.getValue());

						sbWherePart.append(" AND ?");

						columnValueParams.add(searchCriteria.getSecondValue());

					} else if (ToolboxSysKeys.SearchCriteriaOperation.LIKE.equals(searchCriteria.getOperation())) {

						sbWherePart.append("LOWER("); 
						sbWherePart.append(this.fieldAndColumnNames.get(searchCriteria.getFieldName()));
						sbWherePart.append("::text) LIKE ?");

						columnValueParams.add("%" + searchCriteria.getValue().toString().trim().toLowerCase() + "%");

					} else if (ToolboxSysKeys.SearchCriteriaOperation.LIKE_END.equals(searchCriteria.getOperation())) {

						sbWherePart.append("LOWER(");
						sbWherePart.append(this.fieldAndColumnNames.get(searchCriteria.getFieldName()));
						sbWherePart.append("::text) LIKE ?");

						columnValueParams.add(searchCriteria.getValue().toString().trim().toLowerCase() + "%");

					} else if (ToolboxSysKeys.SearchCriteriaOperation.NOT_LIKE.equals(searchCriteria.getOperation())) {

						sbWherePart.append("LOWER(");
						sbWherePart.append(this.fieldAndColumnNames.get(searchCriteria.getFieldName()));
						sbWherePart.append("::text) NOT LIKE ?");

						columnValueParams.add("%" + searchCriteria.getValue().toString().trim().toLowerCase() + "%");

					} else if (ToolboxSysKeys.SearchCriteriaOperation.NOT_LIKE_END.equals(searchCriteria.getOperation())) {

						sbWherePart.append("LOWER(");
						sbWherePart.append(this.fieldAndColumnNames.get(searchCriteria.getFieldName()));
						sbWherePart.append("::text) NOT LIKE ?");

						columnValueParams.add(searchCriteria.getValue().toString().trim().toLowerCase() + "%");

					} else if (ToolboxSysKeys.SearchCriteriaOperation.IS_NULL.equals(searchCriteria.getOperation())) {

						sbWherePart.append(this.fieldAndColumnNames.get(searchCriteria.getFieldName()));
						sbWherePart.append(" IS NULL");

					} else if (ToolboxSysKeys.SearchCriteriaOperation.IS_NOT_NULL.equals(searchCriteria.getOperation())) {

						sbWherePart.append(this.fieldAndColumnNames.get(searchCriteria.getFieldName()));
						sbWherePart.append(" IS NOT NULL");

					} else if (ToolboxSysKeys.SearchCriteriaOperation.JSON_CONTAINS.equals(searchCriteria.getOperation())) {

						if (searchCriteria.getValue() instanceof Number) {
							sbWherePart.append(this.fieldAndColumnNames.get(searchCriteria.getFieldName()));
							sbWherePart.append(" @> '" + searchCriteria.getValue() + "'"); // mivel szám, ezért ok
						} else {

							// nem számmal nem biztoságos, SQL injection veszély lenne!

							throw new JdbcRepositoryException(ToolboxSysKeys.SearchCriteriaOperation.JSON_CONTAINS + " only supports numbers!");
						}

					} else if (ToolboxSysKeys.SearchCriteriaOperation.SMALLER_THAN.equals(searchCriteria.getOperation())) {

						sbWherePart.append(this.fieldAndColumnNames.get(searchCriteria.getFieldName()));
						sbWherePart.append(" < ?");

						columnValueParams.add(searchCriteria.getValue());
					} else if (ToolboxSysKeys.SearchCriteriaOperation.BIGGER_THAN.equals(searchCriteria.getOperation())) {

						sbWherePart.append(this.fieldAndColumnNames.get(searchCriteria.getFieldName()));
						sbWherePart.append(" > ?");

						columnValueParams.add(searchCriteria.getValue());

					} else if (ToolboxSysKeys.SearchCriteriaOperation.SMALLER_THAN_EQUALS.equals(searchCriteria.getOperation())) {

						sbWherePart.append(this.fieldAndColumnNames.get(searchCriteria.getFieldName()));
						sbWherePart.append(" <= ?");

						columnValueParams.add(searchCriteria.getValue());

					} else if (ToolboxSysKeys.SearchCriteriaOperation.BIGGER_THAN_EQUALS.equals(searchCriteria.getOperation())) {

						sbWherePart.append(this.fieldAndColumnNames.get(searchCriteria.getFieldName()));
						sbWherePart.append(" >= ?");

						columnValueParams.add(searchCriteria.getValue());

					} else if (ToolboxSysKeys.SearchCriteriaOperation.IN.equals(searchCriteria.getOperation())) {

						sbWherePart.append(this.fieldAndColumnNames.get(searchCriteria.getFieldName()));
						sbWherePart.append(" IN (");

						if (searchCriteria.getValue() != null) {

							// searchCriteria.getCriteriaType()

							Set set = (Set) searchCriteria.getValue();

							boolean isFirstIn = true;

							for (Object o : set) {

								// postgres: max 32767 elem lehet az IN-ben
								// https://stackoverflow.com/questions/1009706/postgresql-max-number-of-parameters-in-in-clause

								if (!isFirstIn) {
									sbWherePart.append(",");
								}

								sbWherePart.append("?");
								columnValueParams.add(o);

								isFirstIn = false;
							}

						} else {
							// valószínűleg ez nem kell érdemben, csak azért van itt, hogy az SQL string összeálljon és debugnál lehessen egyben látni
							// értsd: ne itt haljon le, hanem később

							sbWherePart.append("?");
							columnValueParams.add(null);
						}

						sbWherePart.append(")");

					} else if (ToolboxSysKeys.SearchCriteriaOperation.EQ.equals(searchCriteria.getOperation())) {

						if (String.class.equals(searchCriteria.getCriteriaType())) {

							sbWherePart.append("LOWER(");
							sbWherePart.append(this.fieldAndColumnNames.get(searchCriteria.getFieldName()));
							sbWherePart.append(") = ?");

							columnValueParams.add(searchCriteria.getValue().toString().trim().toLowerCase());

						} else {

							sbWherePart.append(this.fieldAndColumnNames.get(searchCriteria.getFieldName()));
							sbWherePart.append(" = ?");

							columnValueParams.add(searchCriteria.getValue());

						}

					} else if (ToolboxSysKeys.SearchCriteriaOperation.NE.equals(searchCriteria.getOperation())) {

						if (String.class.equals(searchCriteria.getCriteriaType())) {

							sbWherePart.append("LOWER(");
							sbWherePart.append(this.fieldAndColumnNames.get(searchCriteria.getFieldName()));
							sbWherePart.append(") <> ?");

							columnValueParams.add(searchCriteria.getValue().toString().trim().toLowerCase());

						} else {

							sbWherePart.append(this.fieldAndColumnNames.get(searchCriteria.getFieldName()));
							sbWherePart.append(" <> ?");

							columnValueParams.add(searchCriteria.getValue());

						}

					} else {

						throw new JdbcRepositoryException("Unknown SearchCriteria operation!");

					}

					++i;
				}

				sbWherePart.append(") ");

			}

		}

		return Pair.of(sbWherePart.toString(), columnValueParams);

	}

	/**
	 * {@link BasePageRequest} / {@link Pageable} / {@link SearchCriteria}... alapon SQL query-k összállítása...
	 * 
	 * @param pageable
	 * @param prependOrderByKeyword
	 * @return
	 */
	public String buildSqlOrderPart(final Pageable pageable, final boolean prependOrderByKeyword) {

		final StringBuilder sbOrderPart = new StringBuilder();

		if (pageable.getSort() != null) {

			int j = 0;

			for (final Order o : pageable.getSort()) {

				if (j == 0) {

					if (prependOrderByKeyword) {
						sbOrderPart.append(" ORDER BY");
					}

					sbOrderPart.append(" ");
				}

				if (j > 0) {
					sbOrderPart.append(", ");
				}

				sbOrderPart.append(this.fieldAndColumnNames.get(o.getProperty()));

				sbOrderPart.append(" ");
				sbOrderPart.append(o.getDirection().name());

				j++;
			}

			if (j > 0) {
				sbOrderPart.append(" ");
			}

		}

		return sbOrderPart.toString();

	}

	/**
	 * csak "LIMIT"/"OFFSET"-et támogató DB-vel megy
	 */
	public String buildSqlLimitPart(final Pageable pageable) {

		final StringBuilder sbLimitPart = new StringBuilder();

		sbLimitPart.append(" LIMIT ");
		sbLimitPart.append(pageable.getPageSize());
		sbLimitPart.append(" OFFSET "); // TODO: elvileg az offset lassú lehet (nagy számok esetén), megnézni, talán jobb row_number() megoldással? (nem égetően sürgős)
		sbLimitPart.append(pageable.getOffset());

		return sbLimitPart.toString();

	}

	/**
	 * @deprecated 
	 * @see JdbcRepositoryVariableManager
	 */
	@Deprecated
	public String fillVariables(final String sql, final boolean doFillTenantVariables) {
		return jdbcRepositoryVariableManager.fillVariables(sql, doFillTenantVariables);
	}

	/**
	 * rowMapper-t ad vissza (válaszott model osztályához, jellemző extended model-el oszályokhoz jó)
	 *
	 * @return
	 */
	public RowMapper<T> newRowMapperInstance() {
		return jdbcTemplateMapperFactory.newRowMapper(this.entityType);
	}

	/**
	 * @param auditModel
	 * 
	 * @see JdbcRepositoryAuditManager
	 */
	@Deprecated
	public static void setTlAuditModel(AuditModel auditModel) {
		JdbcRepositoryAuditManager.setTlAuditModel(auditModel);
	}

	/**
	 * @see JdbcRepositoryAuditManager
	 */
	@Deprecated
	public static void clearTlAuditModel() {
		JdbcRepositoryAuditManager.clearTlAuditModel();
	}

}
