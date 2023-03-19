package hu.lanoga.toolbox.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;

public class ReflectionUtils {

	private ReflectionUtils() {
		//
	}
	
	/**
	 * az osztály összes nem static és nem abstract mezőjének neve 
	 * (az ősosztályokból is minden, nem csak a protected)...
	 * 
	 * @param clazz
	 * @return
	 */
	public static Set<String> collectTypeFieldNames(final Class<?> clazz) {

		final HashSet<String> hs = new HashSet<>();

		final List<Field> allFields = FieldUtils.getAllFieldsList(clazz);

		for (final Field field : allFields) {

			if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isAbstract(field.getModifiers())) {
				hs.add(field.getName());
			}
		}

		return Collections.unmodifiableSet(hs);
		
	}
	
}
