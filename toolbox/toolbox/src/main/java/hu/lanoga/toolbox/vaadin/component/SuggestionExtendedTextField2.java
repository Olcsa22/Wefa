package hu.lanoga.toolbox.vaadin.component;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;

public class SuggestionExtendedTextField2 extends CustomField<String> {

	private final List<String> options;
	private final String showTxtFieldOption;

	private AbstractOrderedLayout layout;
	private final AbstractTextField txt;
	private ComboBox<String> cb;

	/**
	 * @param txt
	 * @param options
	 * @param showTxtFieldOptionű
	 * 		"egyéb" lehetőség (kézi beírásra), amikor a TextField-et kell mutatni
	 */
	@SuppressWarnings("unchecked")
	public SuggestionExtendedTextField2(final AbstractTextField txt, final List<String> options, final String showTxtFieldOption) {
		super();

		this.txt = txt;
		this.options = options;
		this.showTxtFieldOption = showTxtFieldOption;

		if (StringUtils.isNotBlank(txt.getCaption())) {
			this.setCaption(txt.getCaption());
			this.txt.setCaption(null);
		}

		this.layout = new HorizontalLayout();
		this.layout.setWidth("100%");
		this.layout.setSpacing(false);
		this.layout.setMargin(false);

		this.txt.setVisible(false);

		this.cb = new ComboBox<>(null, this.options);
		this.cb.setWidth("100%");

		final Button btnSelect = new Button();
		btnSelect.setIcon(VaadinIcons.ARROW_CIRCLE_LEFT_O);
		btnSelect.setEnabled(false);
		btnSelect.setWidth("100%");

		this.cb.addValueChangeListener(x -> {

			final String value = x.getValue();

			if (StringUtils.isNotBlank(value)) {
				if (this.showTxtFieldOption.equals(value)) {
					this.txt.setVisible(true);
					this.txt.setValue("");
					this.layout.setSpacing(true);
				} else {
					this.txt.setValue(value);
					this.txt.setVisible(false);
					this.layout.setSpacing(false);
				}

			}
		});

		this.txt.setWidth("100%");
		this.txt.addValueChangeListener(x -> {

			final String oldValue = x.getOldValue();

			final Collection<?> listeners = this.getListeners(ValueChangeEvent.class);
			for (final Object listener : listeners) {
				((ValueChangeListener<String>) listener).valueChange(new ValueChangeEvent<>(this, oldValue, true));
			}

		});

		this.layout.addComponent(this.cb);
		this.layout.addComponent(this.txt);

	}

	@Override
	protected Component initContent() {
		return this.layout;
	}

	@Override
	public String getValue() {
		return this.txt.getValue();
	}

	@Override
	protected void doSetValue(final String value) {
		
		if (options.contains(value)) {
			this.cb.setValue(value);
		} else {
			this.cb.setValue(showTxtFieldOption);
		}

		this.txt.setValue(value);

	}

	@Override
	public void setEnabled(final boolean enabled) {

		super.setEnabled(enabled);

		this.cb.setEnabled(enabled);

		this.txt.setEnabled(enabled);

		this.layout.setEnabled(enabled);

	}

	public AbstractTextField getTxt() {
		return txt;
	}

	
}
