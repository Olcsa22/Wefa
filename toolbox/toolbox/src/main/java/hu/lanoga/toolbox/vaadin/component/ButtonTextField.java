package hu.lanoga.toolbox.vaadin.component;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.server.Resource;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ButtonTextField extends CustomField<String> {

	private TextField txt;
	private Button btn;

	private String buttonCaptionStr;
	private String buttonTooltipStr;
	private Resource buttonIcon;
	private Button.ClickListener btnClickListener;
	private boolean allowClickWhenDisabled;
	private boolean allowClickWhenEmpty;

	public ButtonTextField(final String caption, final Resource buttonIcon, final String buttonCaptionStr, final String buttonTooltipStr, final Button.ClickListener clickListener, final boolean allowClickWhenDisabled, final boolean allowClickWhenEmpty) {
		super();

		this.setCaption(caption);

		this.buttonCaptionStr = buttonCaptionStr;
		this.buttonTooltipStr = buttonTooltipStr;
		this.buttonIcon = buttonIcon;
		this.btnClickListener = clickListener;
		this.allowClickWhenDisabled = allowClickWhenDisabled;
		this.allowClickWhenEmpty = allowClickWhenEmpty;

		this.txt = new TextField();
		this.btn = new Button(this.buttonCaptionStr, this.buttonIcon);
	}

	public ButtonTextField(final String caption, final Resource buttonIcon, final String buttonCaptionStr, final String buttonTooltipStr, final Button.ClickListener clickListener, final boolean allowClickWhenDisabled) {
		super();

		this.setCaption(caption);

		this.buttonCaptionStr = buttonCaptionStr;
		this.buttonTooltipStr = buttonTooltipStr;
		this.buttonIcon = buttonIcon;
		this.btnClickListener = clickListener;
		this.allowClickWhenDisabled = allowClickWhenDisabled;
		this.allowClickWhenEmpty = false;

		this.txt = new TextField();
		this.btn = new Button(this.buttonCaptionStr, this.buttonIcon);
	}

	@Override
	public String getValue() {
		return this.txt.getValue();
	}

	@Override
	protected Component initContent() {

		this.btn.setDescription(this.buttonTooltipStr);

		if (this.btnClickListener != null) {
			this.btn.addClickListener(this.btnClickListener);
		}

		this.txt.setWidth("100%");
		this.btn.setWidth("100%");

		this.btn.addAttachListener(x -> {

			if (this.allowClickWhenDisabled) {
				if (!this.isEnabled() || this.isReadOnly()) {

					this.setEnabled(true);
					this.btn.setEnabled(true);

					this.txt.setEnabled(false);

					this.setCaption("<span style=\"opacity:0.5\">" + this.getCaption() + "</span>");
					this.setCaptionAsHtml(true);
				}
			}

			if (!this.allowClickWhenEmpty) {
				this.btn.setEnabled(StringUtils.isNotBlank(this.txt.getValue()));
			}

		});

		if (!this.allowClickWhenEmpty) {
			this.txt.addValueChangeListener(v -> {
				this.btn.setEnabled(StringUtils.isNotBlank(v.getValue()));
			});
		}

		final HorizontalLayout hl = new HorizontalLayout();
		hl.setWidth("100%");
		hl.addComponents(this.txt, this.btn);
		hl.setExpandRatio(this.txt, 0.8f);
		hl.setExpandRatio(this.btn, 0.2f);

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

	public Button getButton() {
		return this.btn;
	}

}
