package hu.lanoga.toolbox.vaadin.binder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import com.vaadin.data.Converter;
import com.vaadin.data.HasValue;
import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.Result;
import com.vaadin.data.ValueContext;
import com.vaadin.data.ValueProvider;
import com.vaadin.event.EventRouter;
import com.vaadin.server.ErrorMessage;
import com.vaadin.server.Setter;
import com.vaadin.server.UserError;
import com.vaadin.shared.Registration;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

import hu.lanoga.toolbox.i18n.I18nUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class BasicBinder<BEAN> {

	static class EasyBinding<BEAN, FIELDVALUE, TARGET> {

		private final HasValue<FIELDVALUE> field;
		private final ValueProvider<BEAN, TARGET> getter;
		private final Setter<BEAN, TARGET> setter;
		private final String property;

		private final Converter<FIELDVALUE, TARGET> converterValidatorChain;
		private final BasicBinder<BEAN> binder;
		private final Registration registration;

		EasyBinding(final BasicBinder<BEAN> binder, final HasValue<FIELDVALUE> field, final ValueProvider<BEAN, TARGET> getter, final Setter<BEAN, TARGET> setter, final String property, final Converter<FIELDVALUE, TARGET> converterValidatorChain) {

			this.binder = binder;
			this.field = field;
			this.getter = getter;
			this.setter = setter;
			this.property = property;
			this.converterValidatorChain = converterValidatorChain;

			registration = field.addValueChangeListener(e -> {
				if (binder.getBean() != null) {
					if (fieldToBean(binder.getBean())) {
						binder.validate();
						binder.fireValueChangeEvent(e);
					}
				}
			});
		}

		void remove() {
			registration.remove();
		}

		void beanToField(final BEAN bean) {

			final TARGET a = getter.apply(bean);
			final ValueContext b = createValueContext();

			if (a != null) { // saját változtatás !
				field.setValue(converterValidatorChain.convertToPresentation(a, b));
			}

		}

		boolean fieldToBean(final BEAN bean) {
			final Result<TARGET> result = converterValidatorChain.convertToModel(field.getValue(), createValueContext());
			result.ifError(e -> binder.setConversionError(field, e));
			result.ifOk(e -> {
				binder.clearConversionError(field);

				// System.out.println("1: " + bean);

				setter.accept(bean, e);

				// System.out.println("2: " + bean);

				binder.hasChanges = true;
			});
			return !result.isError();
		}

		HasValue<FIELDVALUE> getField() {
			return field;
		}

		/**
		 * Creates a value context from the current state of the binding and its field.
		 *
		 * @return the value context
		 */
		ValueContext createValueContext() {
			if (field instanceof Component) {
				return new ValueContext((Component) field, field);
			}
			return new ValueContext(null, field, findLocale());
		}

		/**
		 * Finds an appropriate locale to be used in conversion and validation.
		 *
		 * @return the found locale, not null
		 */
		Locale findLocale() {
			Locale l = null;
			if (field instanceof Component) {
				l = ((Component) field).getLocale();
			}
			if ((l == null) && (UI.getCurrent() != null)) {
				l = UI.getCurrent().getLocale();
			}
			if (l == null) {
				// l = Locale.getDefault();
				l = I18nUtil.getLoggedInUserLocale();
			}
			return l;
		}

		String getProperty() {
			return property;
		}

	}

	private BEAN bean;

	private Label statusLabel;

	private final Map<String, HasValue<?>> validationErrorMap = new HashMap<>();

	protected List<EasyBinding<BEAN, ?, ?>> bindings = new LinkedList<>();

	private Set<ConstraintViolation<BEAN>> constraintViolations;

	private final Map<HasValue<?>, String> conversionViolations = new HashMap<>();

	private boolean hasChanges = false;

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	private Class<?>[] groups = new Class<?>[0];

	private EventRouter eventRouter;

	protected BasicBinder() {
		validate();
	}

	public void setBean(final BEAN bean) {

		this.bean = bean;

		if (bean == null) {
			//
		} else {
			bindings.forEach(e -> e.beanToField(bean));
		}

		validate();
		hasChanges = false;
	}

	public BEAN getBean() {
		return bean;
	}

	protected void removeBean() {
		setBean(null);
	}

	protected void setValidationGroups(final Class<?>... groups) {
		this.groups = groups;
		validate();
	}

	protected Class<?>[] getValidationGroups() {
		return groups;
	}

	protected void clearValidationGroups() {
		groups = new Class<?>[0];
		validate();
	}

	public boolean isValid() {
		
		validate();
		
		boolean v = constraintViolations.isEmpty();

		if (!v) {
			log.debug("constraintViolations: " + constraintViolations);
		}

		return v;
	}

	protected <FIELDVALUE> EasyBinding<BEAN, FIELDVALUE, ?> bind(final HasValue<FIELDVALUE> field, final ValueProvider<BEAN, FIELDVALUE> getter, final Setter<BEAN, FIELDVALUE> setter, final String property) {
		return bind(field, getter, setter, property, Converter.identity());
	}

	protected <FIELDVALUE, TARGET> EasyBinding<BEAN, FIELDVALUE, TARGET> bind(final HasValue<FIELDVALUE> field, final ValueProvider<BEAN, TARGET> getter, final Setter<BEAN, TARGET> setter, final String property, final Converter<FIELDVALUE, TARGET> converter) {

		Objects.requireNonNull(field);
		Objects.requireNonNull(getter);
		Objects.requireNonNull(converter);

		// Register as binding
		final EasyBinding<BEAN, FIELDVALUE, TARGET> binding = new EasyBinding<>(this, field, getter, setter, property, converter);
		bindings.add(binding);

		// Add property to validation error map
		if (property != null) {
			validationErrorMap.put(property, field);
		}

		if (getBean() != null) {
			if (binding.fieldToBean(getBean())) {
				validate();
			}
		}

		return binding;
	}

	protected void unbind() {
		while (!bindings.isEmpty()) {
			final EasyBinding<BEAN, ?, ?> binding = bindings.remove(0);
			binding.remove();
			validationErrorMap.remove(binding.getProperty());
		}
	}

	protected void unbind(final HasValue<?> field) {
		bindings.stream().filter(e -> e.getField().equals(field)).findFirst().ifPresent(e -> unbind(e));
		validate();
	}

	protected <FIELDVALUE, TARGET> void unbind(final EasyBinding<BEAN, FIELDVALUE, TARGET> binding) {
		if (bindings.remove(binding)) {
			binding.remove();
		}

		if (binding.getProperty() != null) {
			validationErrorMap.remove(binding.getProperty());
		}
	}

	public Stream<HasValue<?>> getFields() {
		return bindings.stream().map(e -> e.getField());
	}

	protected void handleConstraintViolations(final ConstraintViolation<BEAN> v, final Function<ConstraintViolation<BEAN>, String> f) {
		final String property = v.getPropertyPath().toString();
		if (property.isEmpty()) {
			// Bean level validation error
			if (statusLabel != null) {
				statusLabel.setValue(f.apply(v));
			}
		} else {
			// Field validation error
			final HasValue<?> g = validationErrorMap.get(property);
			if (g != null) {
				handleError(g, f.apply(v));
			}
		}
	}

	protected void validate() {
		// Clear validation errors
		getStatusLabel().ifPresent(e -> e.setValue(""));
		validationErrorMap.values().stream().forEach(e -> clearError(e));

		// Validate and set validation errors

		BEAN b = getBean();

		if (b != null) {
			constraintViolations = validator.validate(b, groups);
			constraintViolations.stream().forEach(e -> handleConstraintViolations(e, f -> f.getMessage()));
		} else {
			constraintViolations = new HashSet<>();
		}

		conversionViolations.entrySet().stream().forEach(e -> handleError(e.getKey(), e.getValue()));

	}

	protected void setConversionError(final HasValue<?> field, final String message) {
		conversionViolations.put(field, message);
		handleError(field, message);
	}

	protected void clearConversionError(final HasValue<?> field) {
		conversionViolations.remove(field);
		clearError(field);
	}

	/**
	 * Clears the error condition of the given field, if any. The default
	 * implementation clears the
	 * {@link AbstractComponent#setComponentError(ErrorMessage) component error} of
	 * the field if it is a Component, otherwise does nothing.
	 *
	 * @param field
	 *            the field with an invalid value
	 */
	protected void clearError(final HasValue<?> field) {
		if (field instanceof AbstractComponent) {
			((AbstractComponent) field).setComponentError(null);
		}
	}

	/**
	 * Gets the status label or an empty optional if none has been set.
	 *
	 * @return the optional status label
	 * @see #setStatusLabel(Label)
	 */
	protected Optional<Label> getStatusLabel() {
		return Optional.ofNullable(statusLabel);
	}

	/**
	 * Handles a validation error emitted when trying to write the value of the
	 * given field. The default implementation sets the
	 * {@link AbstractComponent#setComponentError(ErrorMessage) component error} of
	 * the field if it is a Component, otherwise does nothing.
	 *
	 * @param field
	 *            the field with the invalid value
	 * @param error
	 *            the error message to set
	 */
	protected void handleError(final HasValue<?> field, final String error) {
		if (field instanceof AbstractComponent) {
			((AbstractComponent) field).setComponentError(new UserError(error));
		}
	}

	protected void setStatusLabel(final Label statusLabel) {
		this.statusLabel = statusLabel;
	}

	protected Optional<HasValue<?>> getFieldForProperty(final String propertyName) {
		return Optional.ofNullable(validationErrorMap.get(propertyName));
	}

	/**
	 * Returns the event router for this binder.
	 *
	 * @return the event router, not null
	 */
	protected EventRouter getEventRouter() {
		if (eventRouter == null) {
			eventRouter = new EventRouter();
		}
		return eventRouter;
	}

	protected boolean getHasChanges() {
		return hasChanges;
	}

	protected <V> void fireValueChangeEvent(final ValueChangeEvent<V> event) {
		getEventRouter().fireEvent(event);
	}

}
