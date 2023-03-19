package hu.lanoga.toolbox.vaadin.component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.TextField;

public class MobileFriendlyDateField extends CustomField<LocalDate> {

	DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	// DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("HH:mm");
	// DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private TextField wrappedField;

	public MobileFriendlyDateField() {
		super();
	}

	@Override
	public LocalDate getValue() {

		if (StringUtils.isBlank(this.wrappedField.getValue())) {
			return null;
		}

		return LocalDate.parse(this.wrappedField.getValue(), this.formatter1);

		// ha dátum és idő is lenne, akkor két TextField kellene egy HorizontalLayout-on...
		// return LocalDateTime.parse(d + " " + t, formatter3);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Component initContent() {

		this.wrappedField = new TextField();
		this.wrappedField.setWidth("100%");
		this.wrappedField.setId(UUID.randomUUID().toString());

		JavaScript.eval("setTimeout(function(){ document.getElementById('" + this.wrappedField.getId() + "').type='date'; }, 100); setTimeout(function(){ document.getElementById('" + this.wrappedField.getId() + "').type='date'; }, 3000);");
		// ha óra/perc is lenne: document.getElementById('" + ... + "').type='time';

		this.wrappedField.addValueChangeListener(x -> {

			final LocalDate oldValue = StringUtils.isNotBlank(x.getOldValue()) ? LocalDate.parse(x.getOldValue(), this.formatter1) : null;

			final Collection<?> listeners = this.getListeners(ValueChangeEvent.class);
			for (final Object listener : listeners) {
				((ValueChangeListener<LocalDate>) listener).valueChange(new ValueChangeEvent<>(this, oldValue, true));
			}

		});

		return this.wrappedField;
	}

	@Override
	protected void doSetValue(final LocalDate value) {
		this.wrappedField.setValue(this.formatter1.format(value));

	}

	public TextField getWrappedField() {
		return this.wrappedField;
	}

}
