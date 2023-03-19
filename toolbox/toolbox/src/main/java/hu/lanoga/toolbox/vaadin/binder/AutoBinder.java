package hu.lanoga.toolbox.vaadin.binder;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import com.googlecode.gentyref.GenericTypeReflector;
import com.teamunify.i18n.I;
import com.vaadin.annotations.PropertyId;
import com.vaadin.data.HasValue;
import com.vaadin.data.PropertyDefinition;
import com.vaadin.data.converter.StringToBigDecimalConverter;
import com.vaadin.ui.Component;
import com.vaadin.util.ReflectTools;

import lombok.extern.slf4j.Slf4j;

/**
 * Eredeti library-hoz képest módosított/egyszerűsített...
 * (eredetit lásd: https://github.com/ljessendk/easybinder (Copyright 2017 Lars Sønderby Jessen Apache 2 Licence))
 *
 * @param <BEAN>
 */
@Slf4j
public class AutoBinder<BEAN> extends ReflectionBinder<BEAN> {

	public AutoBinder(final Class<BEAN> clazz) {
		super(clazz);
	}

	/**
	 * Binds member fields found in the given object.
	 * <p>
	 * This method processes all (Java) member fields whose type extends
	 * {@link HasValue} and that can be mapped to a property id. Property name
	 * mapping is done based on the field name or on a @{@link PropertyId}
	 * annotation on the field. All non-null unbound fields for which a property
	 * name can be determined are bound to the property name using
	 * {@link ReflectionBinder#bind(HasValue, String)}.
	 * <p>
	 * For example:
	 *
	 * <pre>
	 * public class DataForm extends VerticalLayout {
	 * private TextField firstName = new TextField("First name");
	 * &#64;PropertyId("last")
	 * private TextField lastName = new TextField("Last name");
	 *
	 * DataForm myForm = new DataForm();
	 * ...
	 * binder.bindMemberFields(myForm);
	 * </pre>
	 *
	 * This binds the firstName TextField to a "firstName" property in the item,
	 * lastName TextField to a "last" property.
	 * <p>
	 * It's always possible to do custom binding for any field: the
	 * {@link #bindInstanceFields(Object)} method doesn't override existing
	 * bindings.
	 *
	 * @param objectWithMemberFields
	 *            The object that contains (Java) member fields to bind
	 * @throws IllegalStateException
	 *             if there are incompatible HasValue&lt;T&gt; and property types
	 */
	public void bindInstanceFields(final Object objectWithMemberFields) { // !

		final Class<?> objectClass = objectWithMemberFields.getClass();

		final Integer numberOfBoundFields = getFieldsInDeclareOrder(objectClass).stream().filter(memberField -> HasValue.class.isAssignableFrom(memberField.getType())).filter(memberField -> !isFieldBound(memberField, objectWithMemberFields)).map(memberField -> handleProperty(memberField, objectWithMemberFields, (property, type) -> bindProperty(objectWithMemberFields, memberField, property, type))).reduce(0, AutoBinder::accumulate, Integer::sum);

		if ((numberOfBoundFields == 0) && bindings.isEmpty()) {
			// Throwing here for incomplete bindings would be wrong as they
			// may be completed after this call. If they are not, setBean and
			// other methods will throw for those cases
			throw new IllegalStateException("There are no instance fields found for automatic binding");
		}

	}

	/**
	 * Binds {@code property} with {@code propertyType} to the field in the
	 * {@code objectWithMemberFields} instance using {@code memberField} as a
	 * reference to a member.
	 *
	 * @param objectWithMemberFields
	 *            the object that contains (Java) member fields to build and bind
	 * @param memberField
	 *            reference to a member field to bind
	 * @param propertyName
	 *            property name to bind
	 * @param propertyType
	 *            type of the property
	 * @return {@code true} if property is successfully bound
	 */
	@SuppressWarnings("unchecked")
	protected boolean bindProperty(final Object objectWithMemberFields, final Field memberField, final String propertyName, final Class<?> propertyType) {

		final Type valueType = GenericTypeReflector.getTypeParameter(memberField.getGenericType(), HasValue.class.getTypeParameters()[0]);
		if (valueType == null) {
			throw new IllegalStateException(String.format("Unable to detect value type for the member '%s' in the class '%s'.", memberField.getName(), objectWithMemberFields.getClass().getName()));
		}

		HasValue<?> field;

		// Get the field from the object
		try {
			field = (HasValue<?>) ReflectTools.getJavaFieldValue(objectWithMemberFields, memberField, HasValue.class);
		} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			log.warn("Not able to determine type of field", e);
			// If we cannot determine the value, just skip the field
			return false;
		}

		if (BigDecimal.class.equals(propertyType)) {
			bind((HasValue<String>) field, propertyName, new StringToBigDecimalConverter(I.trc("ValidationError", "Not a number!")));
		} else {
			bind(field, propertyName);
		}

		return true;

	}

	/**
	 * Returns an array containing {@link Field} objects reflecting all the fields
	 * of the class or interface represented by this Class object. The elements in
	 * the array returned are sorted in declare order from sub class to super class.
	 *
	 * @param searchClass
	 *            class to introspect
	 * @return list of all fields in the class considering hierarchy
	 */
	private static List<Field> getFieldsInDeclareOrder(Class<?> searchClass) {
		final ArrayList<Field> memberFieldInOrder = new ArrayList<>();

		while (searchClass != null) {
			memberFieldInOrder.addAll(Arrays.asList(searchClass.getDeclaredFields()));
			searchClass = searchClass.getSuperclass();
		}
		return memberFieldInOrder;
	}

	private boolean isFieldBound(final Field memberField, final Object objectWithMemberFields) {
		try {
			final HasValue<?> field = (HasValue<?>) getMemberFieldValue(memberField, objectWithMemberFields);
			return bindings.stream().anyMatch(binding -> binding.getField() == field);
		} catch (final Exception e) {
			return false;
		}
	}

	private static Object getMemberFieldValue(final Field memberField, final Object objectWithMemberFields) {
		memberField.setAccessible(true);
		try {
			return memberField.get(objectWithMemberFields);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		} finally {
			memberField.setAccessible(false);
		}
	}

	@SuppressWarnings("unused")
	private boolean handleProperty(final Field field, final Object objectWithMemberFields, final BiFunction<String, Class<?>, Boolean> propertyHandler) {
		final Optional<PropertyDefinition<BEAN, ?>> descriptor = getPropertyDescriptor(field);

		if (!descriptor.isPresent()) {
			log.warn("No property descriptor found for field={}", field.getName());
			return false;
		}

		final PropertyId propertyIdAnnotation = field.getAnnotation(PropertyId.class);
		String propertyName;
		if (propertyIdAnnotation != null) {
			// @PropertyId(propertyId) always overrides property id
			propertyName = propertyIdAnnotation.value();
		} else {
			propertyName = field.getName();
		}

		if (boundProperties.containsKey(propertyName)) {
			return false;
		}

		final Boolean isPropertyBound = propertyHandler.apply(propertyName, descriptor.get().getType());
		assert boundProperties.containsKey(propertyName);
		return isPropertyBound;
	}

	private static int accumulate(final int count, final boolean value) {
		return value ? count + 1 : count;
	}

	private Optional<PropertyDefinition<BEAN, ?>> getPropertyDescriptor(final Field field) {

		final PropertyId propertyIdAnnotation = field.getAnnotation(PropertyId.class);

		String propertyId;
		if (propertyIdAnnotation != null) {
			// @PropertyId(propertyId) always overrides property id
			propertyId = propertyIdAnnotation.value();
		} else {
			propertyId = field.getName();
		}

		return propertySet.getProperty(propertyId);

	}

	// private Component createAndBind(final Field f, final String path) {
	//
	// final Optional<Component> c = ComponentFactoryRegistry.getInstance().createComponent(f);
	//
	// if (!c.isPresent()) {
	// throw new RuntimeException("No Component factory matches field, field=<" + f + ">");
	// }
	//
	// if (c.get() instanceof HasValue<?>) {
	// final HasValue<?> h = (HasValue<?>) c.get();
	// bind(h, path + f.getName());
	// }
	// return c.get();
	//
	// }

	@SuppressWarnings("unused")
	private <T> void buildAndBind(final Class<?> currentClazz, final String path, final List<Component> components, final String... nestedProperties) {

		final List<Field> fields = getFieldsInDeclareOrder(currentClazz);

		final Map<String, List<String>> nestedPropertyMap = new HashMap<>();

		for (final String p : nestedProperties) {

			final int index = p.indexOf('.', 0);
			final String current = index == -1 ? p : p.substring(0, index);
			List<String> next = nestedPropertyMap.get(current);

			if (next == null) {
				next = new LinkedList<>();
				nestedPropertyMap.put(current, next);
			}
			if (index != -1) {
				next.add(p.substring(index + 1, p.length()));
			}

		}

		for (final Field field : fields) {

			if ((field.getModifiers() & Modifier.STATIC) != 0) {
				continue;
			}

			if (boundProperties.containsKey(path + field.getName())) {
				// property already bound, skip
				continue;
			}

			if (nestedPropertyMap.containsKey(field.getName())) {
				buildAndBind(field.getType(), path + field.getName() + ".", components, nestedPropertyMap.get(field.getName()).stream().toArray(String[]::new));

			} else {
				
				// try {
				// components.add(createAndBind(field, path));
				// } catch (final Exception e) {
				
				log.warn("Could not add field fielaName={}", field.getName());
				
				//
				
			}

		}

	}

}
