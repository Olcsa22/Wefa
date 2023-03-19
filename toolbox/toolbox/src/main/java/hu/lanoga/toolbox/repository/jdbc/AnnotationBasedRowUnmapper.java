package hu.lanoga.toolbox.repository.jdbc;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.data.domain.Persistable;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-safe
 *
 * @param <T>
 */
@Slf4j
final class AnnotationBasedRowUnmapper<T extends Persistable<Integer>> implements RowUnmapper<T> {
	
	/**
	 * DB column nevek, amelyek soha nem szerepelnek SQL update-okban...
	 */
	private static final Set<String> globalOnlyOnInsertColumnNames = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("created_by", "created_on")));

	private final List<Field> insertFields; // TODO: bár ezek a listák nem lesznek módosítva... át kell gondolni a thread safety kérdését
	private final List<Field> updateFields;

	private final List<String> insertColumnNames;
	private final List<String> updateColumnNames;

	AnnotationBasedRowUnmapper(final Class<?> entityType, final Map<String, String> fieldAndColumnNames) {
		
		final List<Field> ifs = new ArrayList<>();
		final List<Field> ufs = new ArrayList<>();

		final List<String> icns = new ArrayList<>();
		final List<String> ucns = new ArrayList<>();

		final List<Field> allFields = FieldUtils.getAllFieldsList(entityType);

		for (final Field field : allFields) {

			if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isAbstract(field.getModifiers())) {

				field.setAccessible(true); // with setAccessible() you change the behavior of the AccessibleObject, i.e. the Field instance, but not the actual field of the class... (https://stackoverflow.com/questions/10638826/java-reflection-impact-of-setaccessibletrue)

				final boolean addToInsertList;
				final boolean addToUpdateList;
				final String columnName;

				final Column columnAnnotation1 = field.getAnnotation(Column.class);
				final View columnAnnotation2 = field.getAnnotation(View.class);

				if (columnAnnotation1 == null) {

					columnName = fieldAndColumnNames.get(field.getName());

					addToInsertList = (columnAnnotation2 == null);
					addToUpdateList = ((columnAnnotation2 == null) && (!AnnotationBasedRowUnmapper.globalOnlyOnInsertColumnNames.contains(columnName)));

				} else {

					columnName = columnAnnotation1.name().equals("") ? fieldAndColumnNames.get(field.getName()) : columnAnnotation1.name();

					addToInsertList = (columnAnnotation2 == null);
					addToUpdateList = ((columnAnnotation2 == null) && (!AnnotationBasedRowUnmapper.globalOnlyOnInsertColumnNames.contains(columnName)));

				}

				if (addToInsertList) {
					ifs.add(field);
					icns.add(columnName);
				}

				if (addToUpdateList) {
					ufs.add(field);
					ucns.add(columnName);
				}

			}

		}

		insertFields = Collections.unmodifiableList(ifs);
		updateFields = Collections.unmodifiableList(ufs);

		insertColumnNames = Collections.unmodifiableList(icns);
		updateColumnNames = Collections.unmodifiableList(ucns);

	}
	
	List<String> getInsertColumnNames() {
		return insertColumnNames;
	}

	List<String> getUpdateColumnNames() {
		return updateColumnNames;
	}

	@Override
	public LinkedHashMap<String, Object> mapColumns(final T t, final Set<String> leaveOutFields, final boolean isInsert) { // itt mindenképp LinkedHashMap legyen a return value, ne Map!

		try {

			final LinkedHashMap<String, Object> columnNameValueMap = new LinkedHashMap<>();

			final List<Field> fields;
			final List<String> columnNames;

			if (isInsert) {
				fields = insertFields;
				columnNames = insertColumnNames;
			} else {
				fields = updateFields;
				columnNames = updateColumnNames;
			}

			for (int i = 0; i < fields.size(); ++i) {

				final Field field = fields.get(i);
				final String fieldName = field.getName();
				
				if ((leaveOutFields != null) && leaveOutFields.contains(fieldName)) {
					continue;
				}
				
				final Object fieldValue = field.get(t);
				final String columnName = columnNames.get(i);

				columnNameValueMap.put(columnName, fieldValue);

			}
			
			return columnNameValueMap;

		} catch (final Exception e) {
			final JdbcRepositoryException ex = new JdbcRepositoryException("AbstractJdbcRepository reflection error!", e);
			log.warn("AbstractJdbcRepository error!", ex);
			throw ex;
		}

	}

}