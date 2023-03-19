package hu.lanoga.toolbox.vaadin.binder;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.validation.constraints.Min;

import com.googlecode.gentyref.GenericTypeReflector;
import com.vaadin.data.BeanPropertySet;
import com.vaadin.data.Converter;
import com.vaadin.data.HasValue;
import com.vaadin.data.PropertyDefinition;
import com.vaadin.data.PropertySet;
import com.vaadin.data.RequiredFieldConfigurator;
import com.vaadin.data.ValueProvider;
import com.vaadin.server.Setter;
import com.vaadin.ui.CheckBox;

class ReflectionBinder<BEAN> extends BasicBinder<BEAN> {

	protected PropertySet<BEAN> propertySet;

	protected Map<String, EasyBinding<BEAN, ?, ?>> boundProperties = new HashMap<>();

	protected static final ConverterRegistry globalConverterRegistry = new ConverterRegistry();

	private static RequiredFieldConfigurator MIN = annotation -> annotation.annotationType().equals(Min.class) && (((Min) annotation).value() > 0);

	private RequiredFieldConfigurator requiredConfigurator = MIN.chain(RequiredFieldConfigurator.DEFAULT);

	protected ReflectionBinder(final Class<BEAN> clazz) {
		propertySet = BeanPropertySet.get(clazz);
	}

	protected <PRESENTATION, MODEL> EasyBinding<BEAN, PRESENTATION, MODEL> bind(final HasValue<PRESENTATION> field, final String propertyName) {

		Objects.requireNonNull(propertyName, "Property name cannot be null");
		// checkUnbound();

		final PropertyDefinition<BEAN, ?> definition = propertySet.getProperty(propertyName).orElseThrow(() -> new IllegalArgumentException("Could not resolve property name " + propertyName + " from " + propertySet));

		final Optional<Class<PRESENTATION>> fieldTypeClass = getFieldTypeForField(field);

		Class<?> modelTypeClass = definition.getType();

		// Hack as PropertyDefinition does not return primitive type
		final Optional<Field> modelField = getDeclaredFieldByName(definition.getPropertyHolderType(), definition.getName());
		if (modelField.isPresent()) {
			modelTypeClass = modelField.get().getType();
		}

		Converter<PRESENTATION, ?> converter = null;
		if (fieldTypeClass.isPresent()) {

			converter = globalConverterRegistry.getConverter(fieldTypeClass.get(), modelTypeClass);

			// if (converter != null) {
			// log.debug("c1 - converter for {} / {} -> {} found by lookup ({})", new Object[] { field.getClass().getName(), fieldTypeClass.get(), modelTypeClass, propertyName});
			// }

			if ((converter == null) && fieldTypeClass.get().equals(modelTypeClass)) {
				if (modelTypeClass.isPrimitive()) {
					converter = Converter.identity();
					// log.debug("c2 - converter for primitive {} / {} -> {} found by identity ({})", new Object[] { field.getClass().getName(), fieldTypeClass.get(), modelTypeClass, propertyName});
				} else {

					if (field instanceof CheckBox) {
						converter = new NullConverter<>(null);
					} else {
						converter = new NullConverter<>(field.getEmptyValue());
					}

					// log.debug("c3 - converter for non-primitive {} / {} -> {} found by identity ({})", new Object[] { field.getClass().getName(), fieldTypeClass.get(), modelTypeClass, propertyName});
				}
			}

		}

		if (converter == null) {
			// Uses definition.getType() to ensure that the object type and not primitive type is returned.
			converter = createConverter(definition.getType());
			// log.debug("Converter for {} generated ({})", new Object[] { modelTypeClass, propertyName });
		}

		return bind(field, propertyName, converter);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected <PRESENTATION, MODEL> EasyBinding<BEAN, PRESENTATION, MODEL> bind(final HasValue<PRESENTATION> field, final String propertyName, final Converter<PRESENTATION, ?> converter) {

		Objects.requireNonNull(converter);
		Objects.requireNonNull(propertyName, "Property name cannot be null");

		// checkUnbound();

		final PropertyDefinition<BEAN, ?> definition = propertySet.getProperty(propertyName).orElseThrow(() -> new IllegalArgumentException("Could not resolve property name " + propertyName + " from " + propertySet));

		final ValueProvider<BEAN, ?> getter = definition.getGetter();
		final Setter<BEAN, ?> setter = definition.getSetter().orElse((bean, value) -> {
			// Setter ignores value

			// log.debug("ignored value: " + bean + ", " + value);

		});

		// log.debug("reflection binder bind: " + propertyName + ", " + field.getClass().getName() + ", " + setter);

		final EasyBinding<BEAN, PRESENTATION, MODEL> binding = bind(field, (ValueProvider) getter, (Setter) setter, propertyName, (Converter) converter);

		boundProperties.put(propertyName, binding);

		final Optional<Field> modelField = getDeclaredFieldByName(definition.getPropertyHolderType(), definition.getName());
		if (modelField.isPresent()) {
			if (Arrays.asList(modelField.get().getAnnotations()).stream().anyMatch(requiredConfigurator)) {
				field.setRequiredIndicatorVisible(true);
			}
		}

		return binding;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected <PRESENTATION, MODEL> Converter<PRESENTATION, MODEL> createConverter(final Class<MODEL> propertyType) {

		return (Converter) Converter.from(fieldValue -> propertyType.cast(fieldValue), propertyValue -> propertyValue, exception -> {
			throw new RuntimeException(exception);
		});

	}

	@SuppressWarnings("unchecked")
	protected <PRESENTATION> Optional<Class<PRESENTATION>> getFieldTypeForField(final HasValue<PRESENTATION> field) {
		// Try to find the field type using reflection
		final Type valueType = GenericTypeReflector.getTypeParameter(field.getClass(), HasValue.class.getTypeParameters()[0]);

		return Optional.ofNullable((Class<PRESENTATION>) valueType);

	}

	protected Optional<Field> getDeclaredFieldByName(Class<?> searchClass, final String name) {

		while (searchClass != null) {
			try {
				return Optional.of(searchClass.getDeclaredField(name));
			} catch (NoSuchFieldException | SecurityException e) {
				// No such field, try superclass
				searchClass = searchClass.getSuperclass();
			}
		}

		return Optional.empty();

	}

	/**
	 * Sets a logic which allows to configure require indicator via
	 * {@link HasValue#setRequiredIndicatorVisible(boolean)} based on property
	 * descriptor.
	 * <p>
	 * Required indicator configuration will not be used at all if
	 * {@code configurator} is null.
	 * <p>
	 * By default the {@link RequiredFieldConfigurator#DEFAULT} configurator is
	 * used.
	 *
	 * @param configurator
	 *            required indicator configurator, may be {@code null}
	 */
	protected void setRequiredConfigurator(final RequiredFieldConfigurator configurator) {
		requiredConfigurator = configurator;
	}

	/**
	 * Gets field required indicator configuration logic.
	 *
	 * @see #setRequiredConfigurator(RequiredFieldConfigurator)
	 *
	 * @return required indicator configurator, may be {@code null}
	 */
	protected RequiredFieldConfigurator getRequiredConfigurator() {
		return requiredConfigurator;
	}

}
