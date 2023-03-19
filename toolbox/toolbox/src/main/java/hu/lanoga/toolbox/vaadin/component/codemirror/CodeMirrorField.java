package hu.lanoga.toolbox.vaadin.component.codemirror;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.teamunify.i18n.I;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.vaadin.component.codemirror.CodeMirrorComponent.Mode;
import hu.lanoga.toolbox.vaadin.component.codemirror.CodeMirrorComponent.Theme;
import hu.lanoga.toolbox.vaadin.util.UiHelper;

@StyleSheet({
		"../../../webjars/codemirror/5.62.2/lib/codemirror.css", //
		"../../../webjars/codemirror/5.62.2/theme/eclipse.css", //
		"../../../webjars/codemirror/5.62.2/theme/elegant.css", //
		"../../../webjars/codemirror/5.62.2/theme/darcula.css", //
		"../../../webjars/codemirror/5.62.2/theme/nord.css" //
})
@JavaScript({
		"../../../webjars/codemirror/5.62.2/lib/codemirror.js", //
		"../../../webjars/codemirror/5.62.2/mode/groovy/groovy.js", //
		"../../../webjars/codemirror/5.62.2/mode/xml/xml.js", //
		"../../../webjars/codemirror/5.62.2/mode/yaml/yaml.js", //
		"../../../webjars/codemirror/5.62.2/mode/javascript/javascript.js", //
		"../../../webjars/codemirror/5.62.2/mode/css/css.js", //
		"../../../webjars/codemirror/5.62.2/mode/sql/sql.js", //
		"../../../webjars/codemirror/5.62.2/mode/shell/shell.js", //
		"../../../webjars/codemirror/5.62.2/mode/velocity/velocity.js", //
		// "../../../webjars/codemirror/5.62.2/mode/dockerfile/dockerfile.js", //
		"../../../webjars/codemirror/5.62.2/mode/markdown/markdown.js", //
		"../../../webjars/codemirror/5.62.2/mode/properties/properties.js", //
		"../../../webjars/codemirror/5.62.2/mode/spreadsheet/spreadsheet.js" //
})
public class CodeMirrorField extends CustomField<String> {

	private final CodeMirrorComponent codeMirrorComponent;
	private Window dialog;

	private final String helpStrHtml;

	public CodeMirrorField(final String caption, final Mode mode, final Theme theme) {
		this(caption, mode, theme, null);
	}

	/**
	 * @param caption
	 * @param mode
	 * @param theme
	 * @param helpStrHtml
	 * 		HTML-ként van értelmezve, szükség esetén sanitize kell rá
	 */
	public CodeMirrorField(final String caption, final Mode mode, final Theme theme, final String helpStrHtml) {

		this.setCaption(caption);

		this.helpStrHtml = helpStrHtml;

		this.codeMirrorComponent = new CodeMirrorComponent(mode, theme);
		this.codeMirrorComponent.setId(UUID.randomUUID().toString());
	}

	public void initDialog() {

		final VerticalLayout vlDialog = new VerticalLayout();
		vlDialog.setWidth("100%");
		vlDialog.setHeight(null);
		vlDialog.setMargin(true);
		vlDialog.setSpacing(true);

		vlDialog.addComponent(this.codeMirrorComponent);

		this.codeMirrorComponent.cmEnhance();

		// ---

		if (this.isEnabled() && !this.isReadOnly()) {

			HorizontalLayout hlButtons = new HorizontalLayout();

			{

				final Button btnOk = new Button(I.trc("Button", "OK"));
				btnOk.setWidth("");
				btnOk.addStyleName("min-width-150px");
				btnOk.addStyleName("max-width-400px");
				btnOk.addStyleName(ValoTheme.BUTTON_FRIENDLY);
				// btnOk.addStyleName(ToolboxTheme.BUTTON_UPLOAD);

				btnOk.addClickListener(y -> {
					this.dialog.close();
				});

				hlButtons.addComponent(btnOk);

			}

			if (StringUtils.isNotBlank(this.helpStrHtml)) {

				final Button btnHelpStr = new Button(I.trc("Button", "Help"));
				btnHelpStr.setWidth("");
				btnHelpStr.addStyleName("min-width-150px");
				btnHelpStr.addStyleName("max-width-400px");

				btnHelpStr.addClickListener(y -> {

					Window infoBoxDialog = UiHelper.buildInfoBoxDialog(btnHelpStr.getCaption(), helpStrHtml, ContentMode.HTML);
					UI.getCurrent().addWindow(infoBoxDialog);
					UiHelper.forceDialogFocus(infoBoxDialog);

				});

				hlButtons.addComponent(btnHelpStr);

			}

			vlDialog.addComponent(hlButtons);

		}

		// ---

		String strDialogCaption;

		if (this.isEnabled() && !this.isReadOnly()) {
			strDialogCaption = I.trc("Title", "Edit language field");
		} else {
			strDialogCaption = I.trc("Title", "View language field");
		}

		strDialogCaption += " (" + Jsoup.clean(this.getCaption(), Safelist.none()) + ")";

		this.dialog = new Window(strDialogCaption, vlDialog);
		this.dialog.setWidth("80%");
		this.dialog.setHeight(null);
		this.dialog.setModal(true);

		UI.getCurrent().addWindow(this.dialog);
		UiHelper.forceDialogFocus(this.dialog);

	}

	@Override
	public String getValue() {
		return this.codeMirrorComponent.getValue();
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
		this.codeMirrorComponent.setValue(value);
	}

	@Override
	public Registration addValueChangeListener(final ValueChangeListener<String> listener) {
		return this.codeMirrorComponent.addValueChangeListener(listener);
	}

}
