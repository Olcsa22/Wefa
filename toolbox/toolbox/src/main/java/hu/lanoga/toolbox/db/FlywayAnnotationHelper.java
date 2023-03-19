//package hu.lanoga.toolbox.db;
//
//import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
//import com.healthmarketscience.sqlbuilder.AlterTableQuery;
//import com.healthmarketscience.sqlbuilder.CreateIndexQuery;
//import com.healthmarketscience.sqlbuilder.CreateTableQuery;
//import com.healthmarketscience.sqlbuilder.dbspec.Constraint.Type;
//import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
//import com.healthmarketscience.sqlbuilder.dbspec.basic.DbConstraint;
//import com.healthmarketscience.sqlbuilder.dbspec.basic.DbForeignKeyConstraint;
//import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
//import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec;
//import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
//import hu.lanoga.toolbox.db.annotation.FwCreateColumn;
//import hu.lanoga.toolbox.db.annotation.FwCreateFk;
//import hu.lanoga.toolbox.db.annotation.FwCreateIndex;
//import hu.lanoga.toolbox.db.annotation.FwCreateTable;
//import hu.lanoga.toolbox.db.annotation.FwCreateUniqueIndex;
//import hu.lanoga.toolbox.exception.ToolboxGeneralException;
//import hu.lanoga.toolbox.repository.ToolboxPersistable;
//import hu.lanoga.toolbox.repository.jdbc.View;
//import hu.lanoga.toolbox.util.ToolboxStringUtil;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.io.FileUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.commons.lang3.tuple.Pair;
//import org.hibernate.validator.constraints.Length;
//import org.reflections.Reflections;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import javax.validation.constraints.Max;
//import java.io.File;
//import java.io.IOException;
//import java.lang.reflect.Field;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.Set;
//
///**
// * experimental, kézi Flyway SQL-ek helyett egyszerűbb esetekben annotiációkkkal is lehet dolgozni 
// * (ezek alapján induláskor generálódik Flyway SQL fájl egy könyvtárba (default-ban user.home alatt))
// */
//@Slf4j
//@Component
//public class FlywayAnnotationHelper {
//
//	@Value("${tools.dbinit.flyway.disk-dir}")
//	private String dbinitFlywayDiskDirStr;
//
//	public void deleteAllGenerated() {
//
//		try {
//
//			final File dir = new File(this.dbinitFlywayDiskDirStr);
//
//			if (dir.exists() && dir.isDirectory()) {
//				FileUtils.deleteDirectory(dir);
//				log.info("FlywayAnnotationHelper deleteAllGenerated");
//			} else {
//				log.info("FlywayAnnotationHelper deleteAllGenerated (skip, folder does not exists yet)");
//			}
//
//		} catch (final IOException e) {
//			log.error("FlywayAnnotationHelper deleteAllGenerated failed", e);
//			throw new ToolboxGeneralException(e);
//		}
//
//	}
//
//	public void scanAndGenerate() {
//
//		// TODO: ötlet: lehetne egy egyszerűbb dolog a SysKeys beli code store elemekhez, @DevFwInsert... 
//		// lényeg, hogy ugyanide generáljon egy insert-eklet tartalmazó fájlt, ne legyen .sql a kiterjesztés, lényeg, csak az, hogy ne teljesen kézzel kelljen megírni
//		// a nyelvi feliratokhoz tegye be a Java field/változó nevet átmenetinek (hu és en is)
//		// mindez csak Spring dev módban
//
//		// TODO: más (nem ide tartozó) ötlet: az EmailTemplate részt intézzük diskről mindig, csak legyen egy olyan mód, ahol a lemezen lévő fájlt lehet szerkeszteni is (akár per tenant)
//		// értsd ugyanaz, mint a DB-s csak lemezen van
//		// megelőzőleg lehet egy migráció, ami kimeneti ide a fájlokat
//
//		try {
//
//			final Map<Integer, StringBuilder> sbMap = new HashMap<>();
//			final Map<Integer, StringBuilder> sbBottomMap = new HashMap<>();
//
//			final Reflections reflections = new Reflections("hu.lanoga");
//			final Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(FwCreateTable.class);
//
//			// ezek igazából nem módosítanak semmit a DB-n ez gyakorlatilag egy stringBuilder munkáját végzi
//			final DbSpec spec = new DbSpec();
//			final DbSchema schema = spec.addDefaultSchema();
//
//			for (final Class<?> clazz : annotated) {
//
//				if (!clazz.isAssignableFrom(ToolboxPersistable.class)) {
//					log.warn("scanAndGenerate, class is not a ToolboxPersistable: " + clazz.getCanonicalName());
//				}
//
//				final FwCreateTable fwCreateTableAnnotation = clazz.getAnnotation(FwCreateTable.class);
//
//				final String tableName = StringUtils.isNotBlank(fwCreateTableAnnotation.tableName()) ? fwCreateTableAnnotation.tableName() : ToolboxStringUtil.camelCaseToUnderscoreBig(clazz.getSimpleName());
//				final int tableVersionNumber = fwCreateTableAnnotation.versionNumber();
//
//				// ---
//
//				// CREATE TABLE
//
//				DbTable dbt = schema.findTable(tableName);
//
//				if (dbt == null) {
//					dbt = schema.addTable(tableName);
//				}
//
//				// ha már létezett idegen kulcs miatt, akkor eltávolítjuk és felveszzük újra
//				if (dbt.findColumn("ID") != null) {
//					dbt.getColumns().remove(dbt.findColumn("ID"));
//				}
//
//				final DbColumn idColumn = new DbColumn(dbt, "ID", "SERIAL");
//				idColumn.addConstraint(new DbConstraint(idColumn, null, Type.PRIMARY_KEY));
//
//				final String str = new CreateTableQuery(dbt, true).addColumns(idColumn).validate().toString();
//				addString(sbMap, tableVersionNumber, str);
//
//				// ---
//
//				final boolean autoColumnMode = fwCreateTableAnnotation.autoColumnMode();
//
//				addCreateColumns(schema, dbt, sbMap, sbBottomMap, clazz, tableVersionNumber, autoColumnMode);
//
//				if (clazz.isAnnotationPresent(FwCreateIndex.class)) {
//					FwCreateIndex indexAnnotation = clazz.getAnnotation(FwCreateIndex.class);
//					String[] arrayOfIndexes = indexAnnotation.indexFields();
//
//					for (String indexFields : arrayOfIndexes) {
//
//						final Pair<List<DbColumn>, StringBuilder> colIndexPair = buildColumnListAndIndexName(dbt, indexFields);
//						final List<DbColumn> columnList = colIndexPair.getLeft();
//						final StringBuilder indexNameBuilder = colIndexPair.getRight();
//
//						final CreateIndexQuery indexQuery = new CreateIndexQuery(dbt, indexNameBuilder.toString()).addColumns(columnList.toArray(new DbColumn[columnList.size()]));
//
//						addString(sbBottomMap, tableVersionNumber, indexQuery.toString());
//					}
//
//				} else if (clazz.isAnnotationPresent(FwCreateUniqueIndex.class)) {
//					FwCreateUniqueIndex indexAnnotation = clazz.getAnnotation(FwCreateUniqueIndex.class);
//					String[] arrayOfIndexes = indexAnnotation.indexFields();
//
//					for (String indexFields : arrayOfIndexes) {
//
//						final Pair<List<DbColumn>, StringBuilder> colIndexPair = buildColumnListAndIndexName(dbt, indexFields);
//						final List<DbColumn> columnList = colIndexPair.getLeft();
//						final StringBuilder indexNameBuilder = colIndexPair.getRight();
//
//						final CreateIndexQuery indexQuery = new CreateIndexQuery(dbt, indexNameBuilder.toString()).setIndexType(CreateIndexQuery.IndexType.UNIQUE).addColumns(columnList.toArray(new DbColumn[columnList.size()]));
//
//						addString(sbBottomMap, tableVersionNumber, indexQuery.toString());
//					}
//
//				}
//
//			}
//
//			// ---
//
//			for (final Entry<Integer, StringBuilder> entry : sbMap.entrySet()) {
//
//				final String filename = this.dbinitFlywayDiskDirStr + "/V" + StringUtils.leftPad(Integer.toString(entry.getKey()), 4, "0") + "__ann_gen.sql";
//				final File file = new File(filename);
//
//				if (file.exists()) {
//					log.info("FlywayAnnotationHelper scanAndGenerate, SQL file already exists (skip): " + file.getAbsolutePath());
//					continue;
//				}
//
//				final StringBuilder sqlBuilder = entry.getValue();
//				StringBuilder bottomBuilder = sbBottomMap.get(entry.getKey());
//				if (bottomBuilder != null) {
//					sqlBuilder.append(bottomBuilder);
//				}
//
//				FileUtils.writeStringToFile(file, sqlBuilder.toString(), "UTF-8");
//			}
//
//			log.info("FlywayAnnotationHelper scanAndGenerate");
//		} catch (final Exception e) {
//			log.error("FlywayAnnotationHelper scanAndGenerate failed", e);
//			throw new ToolboxGeneralException(e);
//		}
//
//	}
//
//	private static Pair<List<DbColumn>, StringBuilder> buildColumnListAndIndexName(final DbTable dbt, String indexFields) {
//
//		final StringBuilder indexNameBuilder = new StringBuilder();
//		indexNameBuilder.append(dbt.getName());
//
//		List<DbColumn> columnList = new ArrayList<>();
//
//		indexFields = indexFields.replaceAll("\\s+", "");
//		for (String indexField : indexFields.split(",")) {
//			indexNameBuilder.append("_");
//
//			// ha nem találja, akkor hozza létre
//			String refColName = ToolboxStringUtil.camelCaseToUnderscoreBig(indexField);
//			indexNameBuilder.append(refColName);
//
//			DbColumn dbColumn = dbt.findColumn(refColName);
//
//			if (dbColumn == null) {
//				dbColumn = dbt.addColumn(refColName);
//			}
//
//			columnList.add(dbColumn);
//
//		}
//
//		indexNameBuilder.append("_IDX");
//
//		return Pair.of(columnList, indexNameBuilder);
//
//	}
//
//	/**
//	 * CREATE COLUMN
//	 */
//	private static void addCreateColumns(final DbSchema schema, final DbTable dbt, final Map<Integer, StringBuilder> sbMap, final Map<Integer, StringBuilder> sbBottomMap, final Class<?> clazz, final int tableVersionNumber, final boolean autoColumnMode) {
//
//		final Field[] fields = clazz.getDeclaredFields();
//
//		for (final Field field : fields) {
//
//			if (field.isAnnotationPresent(View.class)) {
//				continue;
//			}
//
//			final FwCreateColumn fwCreateColumn = field.getAnnotation(FwCreateColumn.class);
//
//			if (!autoColumnMode && fwCreateColumn == null) {
//				continue;
//			}
//
//			final String columnName = (fwCreateColumn != null && StringUtils.isNotBlank(fwCreateColumn.columnName()))
//					? fwCreateColumn.columnName()
//					: ToolboxStringUtil.camelCaseToUnderscoreBig(field.getName());
//
//			if ("ID".equalsIgnoreCase(columnName)) {
//				// ezt már a CREATE TABLE-nél hozzáadtuk
//				continue;
//			}
//
//			final int versionNumber = (fwCreateColumn != null && fwCreateColumn.versionNumber() > -1)
//					? fwCreateColumn.versionNumber()
//					: tableVersionNumber;
//
//			final String columnType = (fwCreateColumn != null && StringUtils.isNotBlank(fwCreateColumn.columnType()))
//					? fwCreateColumn.columnType()
//					: getSqlType(field);
//
//			final boolean notNull = (fwCreateColumn != null && fwCreateColumn.notNull());
//
//			// ---
//
//			final DbColumn dbColumn;
//
//			// ha van max hossz validáció, akkor itt megkapjuk a hosszát
//			final Integer customMaxLength = getCustomMaxLength(field);
//			if (customMaxLength != null) {
//				dbColumn = new DbColumn(dbt, columnName, columnType, customMaxLength);
//			} else {
//				dbColumn = new DbColumn(dbt, columnName, columnType);
//			}
//
//			if (notNull) {
//				dbColumn.addConstraint(new DbConstraint(dbColumn, null, Type.NOT_NULL));
//			}
//
//			addString(sbMap, versionNumber, new AlterTableQuery(dbt).setAddColumn(dbColumn).toString());
//
//			// foreign key-ek
//			if (field.isAnnotationPresent(FwCreateFk.class)) {
//				FwCreateFk fkAnnotation = field.getAnnotation(FwCreateFk.class);
//
//				final String fkName = dbt.getName() + "_" + columnName + "_fkey";
//
//				DbTable referredTable = schema.findTable(fkAnnotation.referencedTable().toUpperCase());
//
//				// ha nem találja, akkor hozza létra
//				if (referredTable == null) {
//					referredTable = schema.addTable(fkAnnotation.referencedTable().toUpperCase());
//				}
//
//				// ha nem találja, akkor hozza létre
//				String refColName = fkAnnotation.referencedColumn().toUpperCase();
//
//				if (referredTable.findColumn(refColName) == null) {
//					referredTable.addColumn(refColName);
//				}
//
//
//				addString(sbBottomMap, versionNumber, new AlterTableQuery(dbt).setAddConstraint(new DbForeignKeyConstraint(dbColumn, fkName, referredTable, refColName)).toString());
//			}
//
//		}
//
//	}
//
//	private static String getSqlType(final Field field) {
//
//		final String javaType = field.getType().getCanonicalName();
//
//		final String sqlType;
//
//		switch (javaType) {
//			case "java.lang.String":
//				if (field.isAnnotationPresent(JsonDeserialize.class)) {
//					sqlType = "JSONB";
//				} else if (getCustomMaxLength(field) != null) {
//					sqlType = "VARCHAR";
//				} else { // csak akkor adjuk be a default 250-es varchar hosszat, ha nincs meghatározva a max karakterek száma az annotációban
//					sqlType = "VARCHAR(250)";
//				}
//
//				break;
//			case "java.lang.Integer":
//				sqlType = "INT";
//				break;
//			case "java.lang.Long":
//				sqlType = "BIGINT";
//				break;
//			case "java.lang.Boolean":
//				sqlType = "BOOLEAN";
//				break;
//			case "java.sql.Timestamp":
//				sqlType = "TIMESTAMP";
//				break;
//			case "java.sql.Date":
//				sqlType = "DATE";
//				break;
//			default:
//				sqlType = "VARCHAR";
//				log.warn("Missing precise type mapping for Java type: " + javaType);
//				break;
//		}
//
//		return sqlType;
//
//	}
//
//	/**
//	 * a Length és Max validáció értéke alapján elkéri a maximum limitet
//	 *
//	 * @param field
//	 * @return a maximum értéket adja vissza,
//	 * NULL-t abban az esetben, ha nincs megfelelő annotáció
//	 */
//	private static Integer getCustomMaxLength(final Field field) {
//
//		// TODO: megnézni, még milyen validációk esetén kéne ellenőrizni
//		if (field.isAnnotationPresent(Length.class) && field.getAnnotation(Length.class).max() != Integer.MAX_VALUE) {
//			return field.getAnnotation(Length.class).max();
//
//		} else if (field.isAnnotationPresent(Max.class)) {
//			return Math.toIntExact(field.getAnnotation(Max.class).value());
//		}
//
//		return null;
//	}
//
//	private static void addString(final Map<Integer, StringBuilder> m, final int versionNumber, final String str) {
//
//		StringBuilder sb = m.get(versionNumber);
//
//		if (sb == null) {
//			sb = new StringBuilder();
//			m.put(versionNumber, sb);
//		}
//
//		sb.append(str);
//		sb.append(";\r\n");
//	}
//
//}
