package hu.lanoga.toolbox.vaadin.component;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.server.Resource;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ButtonNumberField extends CustomField<String> {

	private NumberOnlyTextField txt;
	private Button btn;

	private String buttonCaptionStr;
	private String buttonTooltipStr;
	private Resource buttonIcon;
	private Button.ClickListener btnClickListener;
	private boolean allowClickWhenDisabled;

	public ButtonNumberField(final String caption, final Resource buttonIcon, final String buttonCaptionStr, final String buttonTooltipStr, final Button.ClickListener clickListener, final boolean allowClickWhenDisabled, final boolean negativeAllowed, final boolean fractionAllowed) {
		super();

		this.setCaption(caption);

		this.buttonCaptionStr = buttonCaptionStr;
		this.buttonTooltipStr = buttonTooltipStr;
		this.buttonIcon = buttonIcon;
		this.btnClickListener = clickListener;
		this.allowClickWhenDisabled = allowClickWhenDisabled;

		this.txt = new NumberOnlyTextField(null, negativeAllowed, -1, fractionAllowed ? 5 : 0);
	}

	public ButtonNumberField(final String caption, final Resource buttonIcon, final String buttonCaptionStr, final String buttonTooltipStr, final Button.ClickListener clickListener, final boolean allowClickWhenDisabled) {
		super();

		this.setCaption(caption);

		this.buttonCaptionStr = buttonCaptionStr;
		this.buttonTooltipStr = buttonTooltipStr;
		this.buttonIcon = buttonIcon;
		this.btnClickListener = clickListener;
		this.allowClickWhenDisabled = allowClickWhenDisabled;

		this.txt = new NumberOnlyTextField();
	}

	@Override
	public String getValue() {
		return this.txt.getValue();
	}

	@Override
	protected Component initContent() {

		this.btn = new Button(this.buttonCaptionStr, this.buttonIcon);
		this.btn.setDescription(this.buttonTooltipStr);
		this.btn.addClickListener(this.btnClickListener);

		this.txt.setWidth("100%");

		this.btn.addAttachListener(x -> {

			if (this.allowClickWhenDisabled) {
				if (!this.isEnabled() || this.isReadOnly()) {

					this.setEnabled(true);
					btn.setEnabled(true);

					this.txt.setEnabled(false);

					this.setCaption("<span style=\"opacity:0.5\">" + this.getCaption() + "</span>");
					this.setCaptionAsHtml(true);
				}
			}

		});

		this.txt.addValueChangeListener(v -> {
			this.btn.setEnabled(StringUtils.isNotBlank(v.getValue()));
		});

		final HorizontalLayout hl = new HorizontalLayout();
		hl.setWidth("100%");
		hl.addComponents(this.txt, btn);
		hl.setExpandRatio(this.txt, 1f);

		return hl;
	}

	@Override
	protected void doSetValue(final String value) {
		this.txt.setValue(value);
	}

	@Override
	public Registration addValueChangeListener(final ValueChangeListener<String> listener) {
		return this.txt.addValueChangeListener(listener);
	}

}
