package hu.lanoga.toolbox.vaadin.component.markdown;

import java.util.UUID;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.teamunify.i18n.I;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.vaadin.component.codemirror.CodeMirrorComponent;
import hu.lanoga.toolbox.vaadin.util.UiHelper;

public class MarkdownField extends CustomField<String> {

	private final MarkdownComponent markdownComponent;
	private Window dialog;

	public MarkdownField(final String caption, final CodeMirrorComponent.Theme theme) {

		this.setCaption(caption);

		this.markdownComponent = new MarkdownComponent(theme);
		this.markdownComponent.setId(UUID.randomUUID().toString());
	}

	private void initDialog() {

		final VerticalLayout vlDialog = new VerticalLayout();
		vlDialog.setWidth("100%");
		vlDialog.setHeight(null);
		vlDialog.setMargin(true);
		vlDialog.setSpacing(true);
		vlDialog.addComponent(this.markdownComponent);

		// ---

		if (this.isEnabled() && !this.isReadOnly()) {

			final Button btnOk = new Button(I.trc("Button", "OK"));
			btnOk.setWidth("");
			btnOk.addStyleName("min-width-150px");
			btnOk.addStyleName("max-width-400px");
			btnOk.addStyleName(ValoTheme.BUTTON_FRIENDLY);

			btnOk.addClickListener(y -> {
				this.dialog.close();
			});

			vlDialog.addComponent(btnOk);

		}

		// ---

		String strDialogCaption;

		if (this.isEnabled() && !this.isReadOnly()) {
			strDialogCaption = I.trc("Title", "Edit field");
		} else {
			strDialogCaption = I.trc("Title", "View field");
		}

		strDialogCaption += " (" + Jsoup.clean(this.getCaption(), Safelist.none()) + ")";

		this.dialog = new Window(strDialogCaption, vlDialog);
		this.dialog.setWidth("1000px");
		this.dialog.setHeight(null);
		this.dialog.setModal(true);

		UI.getCurrent().addWindow(this.dialog);
		UiHelper.forceDialogFocus(this.dialog);

	}

	@Override
	public String getValue() {
		return this.markdownComponent.getValue();
	}

	@Override
	protected Component initContent() {

		final Button btn = new Button(I.trc("Button", "Edit field"));
		btn.setWidth("100%");
		btn.addClickListener(event -> {
			this.initDialog();
		});
		btn.addAttachListener(x -> {

			if (!this.isEnabled() || this.isReadOnly()) {

				btn.setCaption(I.trc("Button", "View field"));

				this.setEnabled(true);
				this.setReadOnly(true);

				this.setCaption("<span style=\"opacity:0.5\">" + this.getCaption() + "</span>");
				this.setCaptionAsHtml(true);
			}

		});

		return btn;

	}

	@Override
	protected void doSetValue(final String value) {
		this.markdownComponent.setValue(value);
	}

	@Override
	public Registration addValueChangeListener(final ValueChangeListener<String> listener) {
		return this.markdownComponent.addValueChangeListener(listener);
	}

}
