package hu.lanoga.toolbox.vaadin.component;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.vaadin.hene.popupbutton.PopupButton;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public class SuggestionExtendedTextField1 extends CustomField<String> {

	private final AbstractTextField txt;
	private final List<String> options;
	private PopupButton pbNote;
	private AbstractOrderedLayout layout;

	@SuppressWarnings("unchecked")
	public SuggestionExtendedTextField1(final AbstractTextField txt, final List<String> options) {
		super();

		this.txt = txt;
		this.options = options;

		if (StringUtils.isNotBlank(txt.getCaption())) {
			this.setCaption(txt.getCaption());
			this.txt.setCaption(null);
		}

		final boolean useHorizontalLayout = this.txt instanceof TextField;

		this.layout = useHorizontalLayout ? new HorizontalLayout() : new VerticalLayout();

		this.layout.setWidth("100%");
		this.layout.setSpacing(true);
		this.layout.setMargin(false);

		this.pbNote = new PopupButton();
		this.pbNote.setWidth("100px");
		this.pbNote.setIcon(VaadinIcons.TOUCH);

		{

			final VerticalLayout vl = new VerticalLayout();
			vl.setWidth("350px");
			this.pbNote.setContent(vl);

			final ListSelect<String> ls = new ListSelect<>("", this.options);
			ls.setWidth("100%");

			final Button btnSelect = new Button();
			btnSelect.setIcon(useHorizontalLayout ? VaadinIcons.ARROW_CIRCLE_LEFT_O : VaadinIcons.ARROW_CIRCLE_UP_O);
			btnSelect.setEnabled(false);
			btnSelect.setWidth("100%");

			vl.addComponent(ls);
			vl.addComponent(btnSelect);

			ls.addValueChangeListener(x -> {

				final Set<String> value = x.getValue();

				if (value != null && !value.isEmpty()) {
					btnSelect.setEnabled(true);
					btnSelect.setData(value);
				} else {
					btnSelect.setEnabled(false);
				}

			});

			btnSelect.addClickListener(x -> {

				final Object data = btnSelect.getData();
				if (data != null) {
					this.txt.setValue((StringUtils.stripToEmpty(this.txt.getValue()) + " " + String.join(" ", (Set<String>) data)).trim());
					this.pbNote.setPopupVisible(false);
				}

			});

		}

		this.txt.setWidth("100%");
		this.txt.addValueChangeListener(x -> {

			final String oldValue = x.getOldValue();

			final Collection<?> listeners = this.getListeners(ValueChangeEvent.class);
			for (final Object listener : listeners) {
				((ValueChangeListener<String>) listener).valueChange(new ValueChangeEvent<>(this, oldValue, true));
			}

		});

		this.layout.addComponent(this.txt);
		this.layout.addComponent(this.pbNote);

		if (useHorizontalLayout) {
			this.layout.setExpandRatio(this.txt, 1f);
		} else {
			this.layout.setComponentAlignment(this.pbNote, Alignment.MIDDLE_RIGHT);
		}

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
		this.txt.setValue(value);
	}

	@Override
	public void setEnabled(final boolean enabled) {

		super.setEnabled(enabled);

		this.pbNote.setEnabled(enabled);

		this.txt.setEnabled(enabled);

		this.layout.setEnabled(enabled);

	}

}
