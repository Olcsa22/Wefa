package hu.lanoga.toolbox.repository.jdbc;

import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.GenericTypeResolver;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.JdbcInsertConflictMode;
import hu.lanoga.toolbox.ToolboxSysKeys.RepositoryTenantMode;
import hu.lanoga.toolbox.exception.StaleDataException;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import hu.lanoga.toolbox.repository.ToolboxAdvancedRepository;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.util.ToolboxAssert;
import hu.lanoga.toolbox.util.ToolboxStringUtil;

/**
 * Egy fő típushoz kötődő repository (alsó layer) osztályok őse. Nagyrészt DB független, szabványos SQL (külön jelezve van a metódusnál).
 * A tranzakciókezelésről ez a réteg nem gondoskodik (a service réteg dolga)!
 *
 * @param <T>
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@NoRepositoryBean
public class DefaultJdbcRepository<T extends ToolboxPersistable> implements ToolboxAdvancedRepository<T>, JdbcRepository<T> {

	/**
	 * thread-safe...
	 */
	@Autowired
	protected JdbcTemplate jdbcTemplate;

	/**
	 * thread-safe
	 */
	@Autowired
	protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Value("${tools.enable.stale-data-exception:false}")
	private boolean enableStaleDataException;

	protected final RepositoryTenantMode tenantMode;
	protected final Class<T> entityType;

	protected final JdbcRepositoryManager jdbcRepositoryManager;

	protected final RowUnmapper<T> rowUnmapper;
	protected final Map<String, String> fieldAndColumnNames;

	protected final String tableName;
	protected String idColumnName;
	protected String tenantIdColumnName;

	protected final ToolboxSysKeys.JdbcDisableMode jdbcDisableMode;
	protected final String disableModeColumnName;

	protected final String selectQuery;
	protected final String selectByIdQuery;
	protected final String existsQuery;
	protected final String existsByIdQuery;
	protected final String countQuery;
	protected final String deleteByIdQuery;
	protected final String deleteQuery;
	protected final String insertQuery;
	protected final String updateQuery;

	public DefaultJdbcRepository() {
		this(RepositoryTenantMode.DEFAULT);
	}

	public DefaultJdbcRepository(final RepositoryTenantMode tenantMode) {

		this.tenantMode = tenantMode;

		this.entityType = (Class<T>) GenericTypeResolver.resolveTypeArguments(this.getClass(), DefaultJdbcRepository.class)[0]; // elvileg csak Spring bean-ekre működik (a resolveTypeArguments hívás)

		// ---

		this.jdbcRepositoryManager = new JdbcRepositoryManager(this.entityType, tenantMode);

		this.fieldAndColumnNames = this.jdbcRepositoryManager.getFieldAndColumnNames();
		this.rowUnmapper = this.jdbcRepositoryManager.getRowUnmapper();

		this.tableName = this.jdbcRepositoryManager.getTableName();
		this.idColumnName = this.jdbcRepositoryManager.getIdColumnName();
		this.tenantIdColumnName = this.jdbcRepositoryManager.getTenantIdColumnName();

		this.disableModeColumnName = this.jdbcRepositoryManager.getDisableModeColumnName();
		this.jdbcDisableMode = this.jdbcRepositoryManager.getJdbcDisableMode();

		// ---

		ToolboxAssert.notNull(this.tableName);
		ToolboxAssert.hasLength(this.tableName);
		ToolboxAssert.notNull(this.idColumnName);
		ToolboxAssert.hasLength(this.idColumnName);
		ToolboxAssert.notNull(this.tenantIdColumnName);
		ToolboxAssert.hasLength(this.tenantIdColumnName);

		// ---

		final String innerSelect = this.getInnerSelect();
			
		final String selectViewName = this.getSelectViewName();

		if (StringUtils.isNotBlank(innerSelect)) {

			ToolboxAssert.isTrue(StringUtils.isBlank(selectViewName));

			this.selectQuery = String.format("SELECT * FROM (%s) AS insel WHERE #tenantCondition ", innerSelect);
			this.selectByIdQuery = String.format("SELECT * FROM (%s) AS insel WHERE #tenantCondition AND %s = ? ", innerSelect, this.idColumnName);
			this.existsQuery = String.format("SELECT 1 FROM (%s) AS insel WHERE #tenantCondition ", innerSelect);
			this.existsByIdQuery = String.format("SELECT 1 FROM (%s) AS insel WHERE #tenantCondition AND %s = ? ", innerSelect, this.idColumnName);
			this.countQuery = String.format("SELECT COUNT(%s) FROM (%s) insel WHERE #tenantCondition ", this.idColumnName, innerSelect);

		} else if (StringUtils.isNotBlank(selectViewName)) {

			this.selectQuery = String.format("SELECT * FROM %s WHERE #tenantCondition ", selectViewName);
			this.selectByIdQuery = String.format("SELECT * FROM %s WHERE #tenantCondition AND %s = ? ", selectViewName, this.idColumnName);
			this.existsQuery = String.format("SELECT 1 FROM %s WHERE #tenantCondition ", selectViewName);
			this.existsByIdQuery = String.format("SELECT 1 FROM %s WHERE #tenantCondition AND %s = ? ", selectViewName, this.idColumnName);
			this.countQuery = String.format("SELECT COUNT(%s) FROM %s WHERE #tenantCondition ", this.idColumnName, selectViewName);

		} else {

			this.selectQuery = String.format("SELECT * FROM %s WHERE #tenantCondition ", this.tableName);
			this.selectByIdQuery = String.format("SELECT * FROM %s WHERE #tenantCondition AND %s = ? ", this.tableName, this.idColumnName);
			this.existsQuery = String.format("SELECT 1 FROM %s WHERE #tenantCondition ", this.tableName);
			this.existsByIdQuery = String.format("SELECT 1 FROM %s WHERE #tenantCondition AND %s = ? ", this.tableName, this.idColumnName);
			this.countQuery = String.format("SELECT COUNT(%s) FROM %s WHERE #tenantCondition ", this.idColumnName, this.tableName);

		}

		this.deleteQuery = String.format("DELETE FROM %s WHERE #tenantCondition ", this.tableName);
		this.deleteByIdQuery = String.format("DELETE FROM %s WHERE #tenantCondition AND %s = ? ", this.tableName, this.idColumnName);

		this.insertQuery = String.format("INSERT INTO %s ", this.tableName);
		this.updateQuery = String.format("UPDATE %s SET ", this.tableName);

	}

	/**
	 * figyelni arra, hogy user inputból származó érték ne legyen string-ként belefűzve...
	 * egyszer hívódik meg a repo példányosításkor...
	 *
	 * @return
	 */
	public String getInnerSelect() { // public kell legyen Spring interface-ek/cglib proxy-k miatt (nem elég a protected)
		return null;
	}

	// public String getInnerSelect2() { // public kell legyen Spring interface-ek/cglib proxy-k miatt (nem elég a protected)
	// return null;
	// }
	//
	// public String getInnerSelect2Count() { // public kell legyen Spring interface-ek/cglib proxy-k miatt (nem elég a protected)
	// return null;
	// }

	/**
	 * {@link #getInnerSelect()} testvére,
	 * csak az egyik lehet egyszerre
	 *
	 * @return
	 */
	public String getSelectViewName() { // public kell legyen Spring interface-ek/cglib proxy-k miatt (nem elég a protected)
		return null;
	}

	@Override
	public long count() {
		return this.jdbcTemplate.queryForObject(this.fillVariables(this.countQuery), Long.class);
	}

	@Override
	public void delete(final Integer id) {

		ToolboxAssert.notNull(id);

		this.jdbcTemplate.update(this.fillVariables(this.deleteByIdQuery), id);
	}

	/**
	 * (új Spring 5 metódus)
	 *
	 * @see #delete(Integer)
	 */
	@Override
	public void deleteById(final Integer id) {
		this.delete(id);
	}
	
	@Override
	public void deleteAllById(Iterable<? extends Integer> ids) {
		for (Integer id : ids) {
			deleteById(id);
		}
	}

	/**
	 * A jelenlegi implementáció nem hatékony, pár elemere jó, több estén tisztázni kell.
	 * A tranzakciókezelésről ez a réteg nem gondoskodik (a service réteg dolga)!
	 */
	public void delete(final Iterable<? extends T> entities) {

		// TODO: nem hatékony implementáció, optimalizálni a jövőben szükség esetén (batch esetleg?)

		for (final T t : entities) {
			this.delete(t);
		}
	}

	@Override
	public void delete(final T entity) {

		ToolboxAssert.notNull(entity);
		ToolboxAssert.notNull(entity.getId());

		this.jdbcTemplate.update(this.fillVariables(this.deleteByIdQuery), entity.getId());
	}

	/**
	 * (új Spring 5 metódus)
	 *
	 * @see #delete(Iterable)
	 */
	@Override
	public void deleteAll(final Iterable<? extends T> entities) {
		this.delete(entities);
	}

	/**
	 * Üríti a táblát (truncate).
	 * Csak kivételes esetekben van erre szükség!
	 */
	@Override
	public void deleteAll() {
		this.jdbcTemplate.update(this.fillVariables(this.deleteQuery));
	}

	@Override
	public void disable(final Integer id) {
		this.disableOrEnable(id, true);
	}

	@Override
	public void enable(final Integer id) {
		this.disableOrEnable(id, false);
	}

	private void disableOrEnable(final Integer id, final boolean disable) {

		if (!this.fieldAndColumnNames.values().contains(this.disableModeColumnName)) {
			throw new UnsupportedOperationException("Not allowed in the base repository!");
		}

		ToolboxAssert.notNull(id);

		// ---

		final int changed;

		if (ToolboxSysKeys.JdbcDisableMode.ENABLED_COLUMN.equals(this.jdbcDisableMode)) {

			changed = this.namedParameterJdbcTemplate.update(this.fillVariables(this.updateQuery + this.disableModeColumnName + " = " + (disable ? "FALSE" : "TRUE") + " WHERE #tenantCondition AND id = :id"), new MapSqlParameterSource().addValue("id", id));

		} else if (ToolboxSysKeys.JdbcDisableMode.DISABLED_COLUMN.equals(this.jdbcDisableMode)) {

			changed = this.namedParameterJdbcTemplate.update(this.fillVariables(this.updateQuery + this.disableModeColumnName + " = " + (disable ? "TRUE" : "FALSE") + " WHERE #tenantCondition AND id = :id"), new MapSqlParameterSource().addValue("id", id));

		} else {
			throw new UnsupportedOperationException("Not allowed in the base repository (missing mode)!");
		}

		// ---

		JdbcRepositoryManager.checkChangedRowCount(1, changed);
	}

	/**
	 * Ez nem alkalmas a unique constraint helyettesítésre!
	 * "Amennyiben nem létezik, akkor szúrjuk be" általában nem oldható meg így!
	 */
	public boolean exists(final Integer id) {

		ToolboxAssert.notNull(id);

		try {
			this.jdbcTemplate.queryForObject(this.fillVariables(this.existsByIdQuery), new Object[] { id }, Long.class);
			return true;
		} catch (final IncorrectResultSizeDataAccessException e) {
			//
		}

		return false;
	}

	/**
	 * (új Spring 5 metódus)
	 *
	 * @see #existsById(Integer)
	 */
	@Override
	public boolean existsById(final Integer id) {
		return this.exists(id);
	}

	/**
	 * Java mezőnév (camelCase) kell!
	 */
	@Override
	public boolean existsBy(final String fieldName, final Object value) {

		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("p", value);

		final StringBuilder sbQuery = new StringBuilder();
		sbQuery.append(this.existsQuery);
		sbQuery.append(" AND ");
		sbQuery.append(this.fieldAndColumnNames.get(fieldName));
		sbQuery.append(" = :p");

		try {
			this.namedParameterJdbcTemplate.queryForObject(this.fillVariables(sbQuery.toString()), namedParameters, Long.class);
			return true;
		} catch (final IncorrectResultSizeDataAccessException e) {
			//
		}

		return false;

	}

	/**
	 * Ez nem alkalmas a unique constraint helyettesítésre!
	 * "Amennyiben nem létezik, akkor szúrjuk be" általában nem oldható meg így!
	 * Java mező név (camelCase) kell a fieldName paraméterekhez!
	 */
	@Override
	public boolean existsBy(final String firstFieldName, final Object firstValue, final String secondFieldName, final Object secondValue) {

		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("p1", firstValue).addValue("p2", secondValue);

		final StringBuilder sbQuery = new StringBuilder();
		sbQuery.append(this.existsQuery);
		sbQuery.append(" AND ");
		sbQuery.append(this.fieldAndColumnNames.get(firstFieldName));
		sbQuery.append(" = :p1 AND ");
		sbQuery.append(this.fieldAndColumnNames.get(secondFieldName));
		sbQuery.append(" = :p2");

		try {
			this.namedParameterJdbcTemplate.queryForObject(this.fillVariables(sbQuery.toString()), namedParameters, Long.class);
			return true;
		} catch (final IncorrectResultSizeDataAccessException e) {
			//
		}

		return false;
	}

	/**
	 * rendezés ID (lásd idColumnName) szerint növekvő
	 */
	@Override
	public List<T> findAll() {
		return this.jdbcTemplate.query(this.fillVariables(this.selectQuery + " ORDER BY " + this.idColumnName), this.newRowMapperInstance());
	}

	/**
	 * (új Spring 5 metódus)
	 * nincs implementálva
	 *
	 * @see #findAll(Iterable)
	 */
	@Override
	public Iterable<T> findAllById(final Iterable<Integer> ids) {
		throw new UnsupportedOperationException("Not implemented in the base repository!");
	}

	/**
	 * nincs implementálva
	 */
	@SuppressWarnings("unused")
	public Iterable<T> findAll(final Iterable<Integer> ids) {
		throw new UnsupportedOperationException("Not implemented in the base repository!");
	}

	/**
	 * nincs implementálva
	 *
	 * @see #findAll(Pageable)
	 */
	@Override
	public Iterable<T> findAll(final Sort sort) {
		throw new UnsupportedOperationException("Not implemented in the base repository!");
	}

	/**
	 * csak "LIMIT"/"OFFSET"-et támogató DB-vel megy...
	 * default (ha mást nem adott meg) rendezés ID szerint növekvő
	 */
	@Override
	public Page<T> findAll(final Pageable pageable) {

		final boolean doQuery = (pageable instanceof BasePageRequest) ? ((BasePageRequest) pageable).isDoQuery() : true;
		final boolean doQueryLastModifiedOnly = (pageable instanceof BasePageRequest) ? ((BasePageRequest) pageable).isDoQueryLastModifiedOnly() : false;
		final boolean doCount = (pageable instanceof BasePageRequest) ? ((BasePageRequest) pageable).isDoCount() : true;

		final boolean doAggregate = (pageable instanceof BasePageRequest) ? StringUtils.isNoneBlank(((BasePageRequest) pageable).getAggregateFieldName(), ((BasePageRequest) pageable).getAggregateSqlFunctionName()) : false;

		// ---

		final StringBuilder sbQuery = new StringBuilder();
		final StringBuilder sbCountQuery = new StringBuilder(this.countQuery);

		if (doAggregate) {

			final String aggregateFieldName = ((BasePageRequest) pageable).getAggregateFieldName(); // amit aggregálunk
			final String aggregateSqlFunctionName = ((BasePageRequest) pageable).getAggregateSqlFunctionName(); // SUM, AVG stb.
			final String aggregateViewFieldName = aggregateFieldName + ToolboxStringUtil.underscoreToCamelCaseBig(aggregateSqlFunctionName); // ahová fog kerülni az érték (egy @View mező a modelben)

			sbQuery.append("SELECT ");
			sbQuery.append(aggregateSqlFunctionName);
			sbQuery.append("(outsel.");
			sbQuery.append(fieldAndColumnNames.get(aggregateFieldName));
			sbQuery.append(") as ");
			sbQuery.append(fieldAndColumnNames.get(aggregateViewFieldName));
			sbQuery.append(" FROM (");
			sbQuery.append(this.selectQuery);
			sbQuery.append(") outsel");

		} else {
			sbQuery.append(this.selectQuery);
		}


		final List<Object> queryArgList = new ArrayList<>();

		// ---

		{

			final Pair<String, List<Object>> sqlWherePart = this.jdbcRepositoryManager.buildSqlWherePart(pageable, false);

			if (StringUtils.isNotBlank(sqlWherePart.getLeft())) {

				final String strWherePart = " AND " + sqlWherePart.getLeft();

				sbQuery.append(strWherePart);
				sbCountQuery.append(strWherePart);

			}

			final List<Object> whereColumnParamValues = sqlWherePart.getRight();
			queryArgList.addAll(whereColumnParamValues);

		}

		// ---

		{

			String sqlOrderPart = null;

			if (doQueryLastModifiedOnly) {

				if (this.jdbcRepositoryManager.getFieldAndColumnNames().containsValue("modified_on")) {
					sqlOrderPart = " ORDER BY modified_on DESC";
				} else {
					sqlOrderPart = " ORDER BY " + this.idColumnName + " DESC";
				}

			} else if (!doAggregate) {

				sqlOrderPart = this.jdbcRepositoryManager.buildSqlOrderPart(pageable, true);

				if (StringUtils.isBlank(sqlOrderPart)) {
					sqlOrderPart = " ORDER BY " + this.idColumnName;
				}

			}

			if (sqlOrderPart != null) {
				sbQuery.append(sqlOrderPart);
			}

		}

		// ---

		{

			final String sqlLimitPart;

			if (doQueryLastModifiedOnly) {
				sqlLimitPart = " LIMIT 1 ";
			} else {
				sqlLimitPart = this.jdbcRepositoryManager.buildSqlLimitPart(pageable);
			}

			sbQuery.append(sqlLimitPart);

		}

		// ---

		final Object[] queryArgArray = queryArgList.toArray();

		final String strQuery = this.fillVariables(sbQuery.toString());
		final String strCountQuery = this.fillVariables(sbCountQuery.toString());

		final List<T> resultList = doQuery ? this.jdbcTemplate.query(strQuery, queryArgArray, this.newRowMapperInstance()) : new ArrayList<>();

		// a filterek figyelembe vételével, ez nem a totál-totál! (viszont a lap/oldal mérettől független)

		final long count;

		if (doCount) {

			if (doQueryLastModifiedOnly && doQuery) {
				count = resultList.size(); // 0 vagy 1 ilyenkor... nincs igazi jelentősége (értsd: doQueryLastModifiedOnly=true esetén nem érdekeli a hívót)
			} else {
				count = this.jdbcTemplate.queryForObject(strCountQuery, queryArgArray, Long.class);
			}

		} else {
			count = -1L; // nincs igazi jelentősége (értsd: ha doCount=false, akkor nem érdekeli a hívót)
		}

		return new PageImpl<>(resultList, pageable, count);
	}

	/**
	 * ha nincs megfelelő rekord, akkor null-t ad vissza...
	 */
	@Override
	public T findOne(final Integer id) {

		ToolboxAssert.notNull(id);

		try {
			return this.jdbcTemplate.queryForObject(this.fillVariables(this.selectByIdQuery), new Object[] { id }, this.newRowMapperInstance());
		} catch (final IncorrectResultSizeDataAccessException e) {
			return null;
		}
	}

	/**
	 * (új Spring 5 metódus)
	 *
	 * @see #findOne(Integer)
	 */
	@Override
	public Optional<T> findById(final Integer id) {
		return Optional.ofNullable(this.findOne(id));
	}

	/**
	 * A tranzakciókezelésről ez a réteg nem gondoskodik (a service réteg dolga)!
	 */
	public <S extends T> Iterable<S> save(final Iterable<S> entities) {

		// TODO: batch lehetőségek? optimalizáció a jövőben...

		final List<S> retList = new ArrayList<>();
		for (final S s : entities) {
			retList.add(this.save(s));
		}

		return retList;
	}

	/**
	 * (új Spring 5 metódus)
	 * A tranzakciókezelésről ez a réteg nem gondoskodik (a service réteg dolga)!
	 *
	 * @see #save(Iterable)
	 */
	@Override
	public <S extends T> Iterable<S> saveAll(final Iterable<S> entities) {
		return this.save(entities);
	}

	@Override
	public <S extends T> S save(final S entity) {
		return this.save(entity, null, true);
	}

	/**
	 * @param entity
	 * @param leaveOutFields Java osztályon belüli mező nevét kell megadni (camelCase)... (null vagy üres esetén nincs alkalmazva)
	 * @param findAfter deprecated, false esetén nem tölti újra a rekordot db-ből, csak model mapperrel mappel (figyelem ez nem deep copy)
	 * @return
	 */
	public <S extends T> S save(final S entity, final Set<String> leaveOutFields, @Deprecated final boolean findAfter) {

		ToolboxAssert.notNull(entity);

		if (entity.isNew()) {

			// insert

			final int generatedId = this.insert(entity, leaveOutFields, JdbcInsertConflictMode.DEFAULT).intValue(); // mivel JdbcInsertConflictMode.DEFAULT, ezért itt biztosan van már id (ha nincs, akkor exception-nel elszállt már "korábban")

			if (findAfter) {
				return (S) this.findOne(generatedId); // TODO: repository.save, mi van akkor, ha subclass-t ad be és findAfter = true
			} else {
				try {

					final S retEntity = (S) entity.getClass().newInstance();

					BeanUtils.copyProperties(retEntity, entity);

					retEntity.setId(generatedId);

					return retEntity;

				} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
					throw new ToolboxGeneralException("BeanUtils error!", e);
				}

			}

		} else {

			// update

			if (this.enableStaleDataException) {

				final S currentlyPersistedFromDb = (S) this.findOne(entity.getId());
				if (currentlyPersistedFromDb.getModifiedOn().after(entity.getModifiedOn())) { // fontos, hogy ez még a setAuditFields hívás előtt legyen

					// ha az épp mentett (update) model objektum modifiedOn date-je régebbi, mint ami most a rekordnál a db-ben van
					// akkorl lehet csak ilyen, ha felhozta a GUI-t (CRUD ablak stb.) ott sokat várt és közben egy másik user már rámentett a rekordra
					// erre mondjuk azt, hogy elavult/stale az adatod, és el akarjuk kerülni az ilyen kaotikus felülírásokat
					// (mj.: nem biztos, hogy minden szoftvernél kell ez)

					throw new StaleDataException("Stale data!");

				}

			}

			this.update(entity, leaveOutFields);

			if (findAfter) {
				return (S) this.findOne(entity.getId()); // TODO: repository.save, mi van akkor, ha subclass-t ad be és findAfter = true
			} else {
				try {

					final S retEntity = (S) entity.getClass().newInstance();

					BeanUtils.copyProperties(retEntity, entity);

					return retEntity;

				} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
					throw new ToolboxGeneralException("BeanUtils error!", e);
				}

			}

		}

	}

	private <S extends T> void update(final S entity, final Set<String> leaveOutFields) {

		final Map<String, Object> columns = this.rowUnmapper.mapColumns(entity, leaveOutFields, false);
		final Object idValue = columns.remove(this.idColumnName);

		columns.remove(this.tenantIdColumnName); // csak a feltételbe tesszük be, értéket nem állítunk át (tehát marad az insert-nél állított tenant örökre)

		ToolboxAssert.notEmpty(columns);
		ToolboxAssert.notNull(idValue);

		JdbcRepositoryAuditManager.setAuditFields(columns, false, true);

		// ---

		final String strQuery;

		{

			final StringBuilder sbQuery = new StringBuilder(this.updateQuery);

			boolean isNotFirst = false;

			for (final String columnName : columns.keySet()) {

				if (isNotFirst) {
					sbQuery.append(", ");
				} else {
					isNotFirst = true;
				}

				sbQuery.append(columnName); // elvileg a columnName értéke a kód alapján áll elő (rowUnmapper), nem user inputból közvetlenül jön, ezért nincs SQL injection veszély
				sbQuery.append(" = ? ");

			}

			columns.put(this.idColumnName, idValue); // így a legvégén lesz az id (az update-hoz így jó)

			sbQuery.append(" WHERE ");
			sbQuery.append(this.idColumnName);
			sbQuery.append(" = ?");
			sbQuery.append(" AND #tenantCondition "); // csak a feltételbe tesszük be, értéket nem állítunk át (tehát marad az insert-nél állított tenant örökre)

			strQuery = this.fillVariables(sbQuery.toString());

		}

		// ---

		final PreparedStatementCreator psc = con -> {

			final PreparedStatement ps = con.prepareStatement(strQuery); // itt nem kell close(try... finally), a hívó (Spring JdbcTemplate) lezárja

			int i = 1;
			for (final Object o : columns.values()) {
				ps.setObject(i, o);
				++i;
			}

			return ps;

		};

		final int rowsAffected = this.jdbcTemplate.update(psc);

		if (rowsAffected < 1) {
			throw new org.springframework.dao.IncorrectUpdateSemanticsDataAccessException("update error (no record for the given id), table: " + this.tableName);
		}

		if (rowsAffected > 1) {
			throw new org.springframework.dao.IncorrectUpdateSemanticsDataAccessException("update error (more than one record for the given id), table: " + this.tableName);
		}

	}

	/**
	 * @param entity
	 * @param leaveOutFields
	 * @param jdbcInsertConflictMode
	 * @return
	 *
	 * @deprecated
	 *
	 * @see #insert(ToolboxPersistable, Set, JdbcInsertConflictMode, String, String)
	 */
	@Deprecated
	public <S extends T> Integer insertWithConflictMode(final S entity, final Set<String> leaveOutFields, final JdbcInsertConflictMode jdbcInsertConflictMode) {
		return (Integer) this.insert(entity, leaveOutFields, jdbcInsertConflictMode, null, null);
	}

	/**
	 * @param entity
	 * 		(nincs köze a JPA/Hibernate-hez, csak ez a param is "enity"-nek van nevezve)
	 * @param leaveOutFields
	 * 		Java osztály mezőnevét kell megadni (camelCase)... (null esetén nincs alkalmazva)
	 * @param jdbcInsertConflictMode
	 * 		{@link JdbcInsertConflictMode#ON_CONFLICT_DO_UPDATE} opcionális, esetén lásd inkább az {@link #insert(ToolboxPersistable, Set, JdbcInsertConflictMode, String, String)} változatot...
	 *
	 * @return az id (DB generált)
	 *
	 * @see #insert(ToolboxPersistable, Set, JdbcInsertConflictMode, String, String)
	 */
	public <S extends T> Number insert(final S entity, final Set<String> leaveOutFields, final JdbcInsertConflictMode jdbcInsertConflictMode) {
		return this.insert(entity, leaveOutFields, jdbcInsertConflictMode, null, null);
	}

	/**
	 * @param entity
	 * 		nincs köze a JPA/Hibernate-hez, csak ez a param is "enity"-nek van nevezve)
	 * @param leaveOutFields
	 * 		Java osztály mezőnevét kell megadni (camelCase)... (null esetén nincs alkalmazva)
	 * @param jdbcInsertConflictMode
	 * 		DB-ben látható unique constraint (key) neve (kézzel add meg, ne legyen user ui inputból direktben, mert nincs semmilyen sanitize)
	 * @param conflictModeColumnName
	 * 		{@link JdbcInsertConflictMode}-dal összefüggésben (vagy az egyik, vagy a másik)
	 * @param conflictModeConstraintName
	 * 		{@link JdbcInsertConflictMode}-dal összefüggésben (vagy az egyik, vagy a másik)
	 *
	 * @return az id (DB generált)
	 */
	public <S extends T> Number insert(final S entity, final Set<String> leaveOutFields, final JdbcInsertConflictMode jdbcInsertConflictMode, final String conflictModeColumnName, final String conflictModeConstraintName) {

		final Map<String, Object> columns = this.rowUnmapper.mapColumns(entity, leaveOutFields, true);
		columns.remove(this.idColumnName);
		columns.remove(this.tenantIdColumnName); // itt levesszük, később hozzátesszük (ha nincs nem tenant mód van, akkor sincs gond, nem itt vesz le semmit, lentebb nem is tesz vissza semmit)

		ToolboxAssert.notEmpty(columns);

		JdbcRepositoryAuditManager.setAuditFields(columns, true, true);

		// ---

		final String strQuery;

		{

			final StringBuilder sbQuery = new StringBuilder(this.insertQuery);

			final StringBuilder sbQueryPart1 = new StringBuilder("(");
			final StringBuilder sbQueryPart2 = new StringBuilder("(");

			boolean isNotFirst = false;

			for (final String columnName : columns.keySet()) {

				if (isNotFirst) {
					sbQueryPart1.append(", ");
					sbQueryPart2.append(", ");
				} else {
					isNotFirst = true;
				}

				sbQueryPart1.append(columnName); // ez a kódból jön (rowUnmapper), így itt nincs SQL injection veszély
				sbQueryPart2.append("?");

			}

			if (RepositoryTenantMode.DEFAULT.equals(this.tenantMode)) {
				sbQueryPart1.append(", " + this.tenantIdColumnName);
				sbQueryPart2.append(", #tenantParam ");
			}

			sbQueryPart1.append(") ");
			sbQueryPart2.append(")");

			sbQuery.append(sbQueryPart1);
			sbQuery.append(" VALUES ");
			sbQuery.append(sbQueryPart2);

			if (JdbcInsertConflictMode.ON_CONFLICT_DO_NOTHING.equals(jdbcInsertConflictMode)) {
				sbQuery.append(" ON CONFLICT DO NOTHING");
			} else if (JdbcInsertConflictMode.ON_CONFLICT_DO_UPDATE.equals(jdbcInsertConflictMode)) {
				// throw new JdbcRepositoryException("Automatic ON CONFLICT DO UPDATE handling is not avaliable! Please write fully custom SQL!");

				final boolean conflictModeColumnNameWithColumnSyntax = StringUtils.isNotBlank(conflictModeColumnName);
				final boolean conflictModeColumnNameWithContraintSyntax = StringUtils.isNotBlank(conflictModeConstraintName);

				ToolboxAssert.isTrue(BooleanUtils.xor(new boolean[] { conflictModeColumnNameWithColumnSyntax, conflictModeColumnNameWithContraintSyntax }));

				if (conflictModeColumnNameWithColumnSyntax) {

					// úgy néz ki unique index (nem contraint) esetén is ez a syntax kell...

					// "The syntax you use is not valid for a unique index because a unique index does not create a constraint.
					// You need to remove the ON CONSTRAINT and use the index expression instead."
					// lásd még: https://dba.stackexchange.com/questions/139756/on-conflict-on-constraint-fails-saying-constraint-doesnt-exist

					// példa: "ON CONFLICT (user_id, lower(kv_key::text))"
					// másik példa: "ON CONFLICT (deal_id, type_id)"
					// lényegében meg kell adni az index definicióját (pgadminnal megnézni stb.)

					sbQuery.append(" ON CONFLICT (");
					sbQuery.append(conflictModeColumnName);
					sbQuery.append(") DO UPDATE SET ");
				} else if (conflictModeColumnNameWithContraintSyntax) {
					sbQuery.append(" ON CONFLICT ON CONSTRAINT ");
					sbQuery.append(conflictModeConstraintName);
					sbQuery.append(" DO UPDATE SET ");
				}

				// ilyenkor ide kell tenni mégegyszer az össze column/érték párt

				boolean isNotFirst2 = false;

				final Map<String, Object> additonalColumns = new LinkedHashMap<>();

				for (final Entry<String, Object> column : columns.entrySet()) {

					if (isNotFirst2) {
						sbQuery.append(", ");
					} else {
						isNotFirst2 = true;
					}

					sbQuery.append(column.getKey());
					sbQuery.append(" = ?");

					additonalColumns.put(column.getKey() + "_second", column.getValue()); // "_second" azért kell, hogy lehessen a Map-ben duplán ugyanaz a value, lenetebb a key már nem számít csak a Map.values()

				}

				columns.putAll(additonalColumns);
			}

			strQuery = this.fillVariables(sbQuery.toString());

		}

		// ---

		final GeneratedKeyHolder key = new GeneratedKeyHolder();

		final PreparedStatementCreator psc = con -> {

			// log.debug("insert SQL: " + strQuery);

			final PreparedStatement ps = con.prepareStatement(strQuery, new String[] { this.idColumnName }); // itt nem kell close(try... finally), a hívó (Spring JdbcTemplate) lezárja

			int i = 1;
			for (final Object o : columns.values()) {
				ps.setObject(i, o);

				// log.debug("insert SQL: " + o);
				++i;
			}

			return ps;

		};

		this.jdbcTemplate.update(psc, key);

		return key.getKey();
	}

	/**
	 * @param fieldName
	 * 		Java mező név (camelCase)
	 * @param value
	 * 		mező kívánt értéke
	 * @return
	 * 		ha nincs megfelelő rekord, akkor null...
	 */
	@Override
	public T findOneBy(final String fieldName, final Object value) {

		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("p", value);

		final StringBuilder sbQuery = new StringBuilder();
		sbQuery.append(this.selectQuery);
		sbQuery.append(" AND ");
		sbQuery.append(this.fieldAndColumnNames.get(fieldName));
		sbQuery.append(" = :p");

		try {
			return this.namedParameterJdbcTemplate.queryForObject(this.fillVariables(sbQuery.toString()), namedParameters, this.newRowMapperInstance());
		} catch (final IncorrectResultSizeDataAccessException e) {
			return null;
		}
	}

	/**
	 * szűrendő mezők között AND kapcsolat...
	 *
	 * @param firstFieldName
	 * 		Java mező név (camelCase), szűrendő mező
	 * @param firstValue
	 * 		kívánt értéke (firstFieldName-hez)
	 * @param secondFieldName
	 * 		Java mező név (camelCase), második szűrendő mező
	 * @param secondValue
	 * 		szűrő értéke (secondFieldName kívánt értéke)
	 * @return ha nincs megfelelő rekord, akkor null
	 */
	@Override
	public T findOneBy(final String firstFieldName, final Object firstValue, final String secondFieldName, final Object secondValue) {

		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("p1", firstValue).addValue("p2", secondValue);

		final StringBuilder sbQuery = new StringBuilder();
		sbQuery.append(this.selectQuery);
		sbQuery.append(" AND ");
		sbQuery.append(this.fieldAndColumnNames.get(firstFieldName));
		sbQuery.append(" = :p1 AND ");
		sbQuery.append(this.fieldAndColumnNames.get(secondFieldName));
		sbQuery.append(" = :p2");

		try {
			return this.namedParameterJdbcTemplate.queryForObject(this.fillVariables(sbQuery.toString()), namedParameters, this.newRowMapperInstance());
		} catch (final IncorrectResultSizeDataAccessException e) {
			return null;
		}
	}

	/**
	 * Rendezés ID szerint növekvő.
	 * Java mező név (camelCase) kell!
	 */
	@Override
	public List<T> findAllBy(final String fieldName, final Object value) {
		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("p", value);

		final StringBuilder sbQuery = new StringBuilder();
		sbQuery.append(this.selectQuery);
		sbQuery.append(" AND ");
		sbQuery.append(this.fieldAndColumnNames.get(fieldName));
		sbQuery.append(" = :p");
		sbQuery.append(" ORDER BY ");
		sbQuery.append(this.idColumnName);

		return this.namedParameterJdbcTemplate.query(this.fillVariables(sbQuery.toString()), namedParameters, this.newRowMapperInstance());
	}

	/**
	 * szűrendő mezők között AND kapcsolat... rendezés ID szerint növekvő...
	 *
	 * @param firstFieldName
	 * 		Java mező név (camelCase), szűrendő mező
	 * @param firstValue
	 * 		szűrő értéke (firstFieldName-hez, ennek kívánt/feltétel értéke)
	 * @param secondFieldName
	 * 		Java mező név (camelCase) alapján, második szűrendő mező
	 * @param secondValue
	 * 		szűrő értéke (secondFieldName kívánt/szűrt értéke)
	 *
	 * @return
	 */
	@Override
	public List<T> findAllBy(final String firstFieldName, final Object firstValue, final String secondFieldName, final Object secondValue) {

		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("p1", firstValue).addValue("p2", secondValue);

		final StringBuilder sbQuery = new StringBuilder();
		sbQuery.append(this.selectQuery);
		sbQuery.append(" AND ");
		sbQuery.append(this.fieldAndColumnNames.get(firstFieldName));
		sbQuery.append(" = :p1 AND ");
		sbQuery.append(this.fieldAndColumnNames.get(secondFieldName));
		sbQuery.append(" = :p2");
		sbQuery.append(" ORDER BY ");
		sbQuery.append(this.idColumnName);

		return this.namedParameterJdbcTemplate.query(this.fillVariables(sbQuery.toString()), namedParameters, this.newRowMapperInstance());
	}

	/**
	 * fontos:
	 *
	 * 1) hogy tranzakcióban legyen (különben nem is érvényesül postgres esetén)
	 * 2) ez mindig tenant mentes (egész táblára vonatkozik), tehát mértékkel használjuk!
	 * 3) hol kellhet: egyes háttér job-ok kapcsán (clean-up jellegű müveletek stb.), esetleg kritikus bug-ok kapcsán quick fix...
	 * 4) tábla neve nem SQL inject védett, soha ne jöjjön user inputból (most nincs ilyen engedve, de a jövőben kód változtatásoknál is figyelni kell)
	 *
	 * (bővebben lásd: https://www.postgresql.org/docs/9.4/sql-lock.html)
	 */
	public void lockTable(/* boolean isParalelReadAllowed */) {

		// if (isParalelReadAllowed) {
		// this.jdbcTemplate.execute("LOCK TABLE " + tableName + " IN SHARE MODE;");
		// } else {

		this.jdbcTemplate.execute("LOCK TABLE " + this.tableName + " IN ACCESS EXCLUSIVE MODE;");

		// postgres dokumentáció: "if no lock mode is specified, then ACCESS EXCLUSIVE, the most restrictive mode, is used."
		// (biztos, ami biztos kiírtam, ha netán válotozik a defaul újabb postgres verziókban)

		// }

	}

	/**
	 * debug célokra... csak postgres-re jó (vagy ahol a SHOW TRANSACTION ISOLATION LEVEL működik)
	 *
	 * @return
	 */
	public String showTransactionIsolationLevel() {
		return this.jdbcTemplate.queryForObject("SHOW TRANSACTION ISOLATION LEVEL", String.class);
	}

	/**
	 * debug célokra...
	 *
	 * stackoverflow-ról:
	 * "Note that calling txid_current() will assign a transaction ID
	 * if the session didn't already have one.
	 * Read-only sessions only have a virtual transaction ID."
	 *
	 * @return
	 */
	public String txIdCurrent() {
		return this.jdbcTemplate.queryForObject("SELECT txid_current()", String.class);
	}

	/**
	 * @see JdbcRepositoryManager#fillVariables(String, boolean)
	 */
	protected String fillVariables(final String sql) {
		return this.jdbcRepositoryManager.fillVariables(sql, true);
	}

	/**
	 * @see JdbcRepositoryManager#fillVariables(String, boolean)
	 */
	protected String fillVariables(final String sql, final boolean doFillTenantVariables) {
		return this.jdbcRepositoryManager.fillVariables(sql, doFillTenantVariables);
	}

	/**
	 * @see JdbcRepositoryManager#newRowMapperInstance()
	 */
	protected RowMapper<T> newRowMapperInstance() {
		return this.jdbcRepositoryManager.newRowMapperInstance();
	}
	
}