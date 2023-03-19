package hu.lanoga.toolbox.vaadin.component.markdown;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.teamunify.i18n.I;
import com.vaadin.data.HasValue;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;

import hu.lanoga.toolbox.util.MarkdownHtmlConverterUtil;
import hu.lanoga.toolbox.vaadin.component.codemirror.CodeMirrorComponent;

public class MarkdownComponent extends TabSheet {

	private final CodeMirrorComponent codeMirrorComponent;
	private final Label lblPreview;

	public MarkdownComponent(final CodeMirrorComponent.Theme theme) {

		this.setWidth("100%");
		this.setHeight(null);

		this.codeMirrorComponent = new CodeMirrorComponent(CodeMirrorComponent.Mode.MARKDOWN, theme);
		this.codeMirrorComponent.setWidth("100%");
		this.addTab(codeMirrorComponent, I.trc("Caption", "Editor"));

		this.lblPreview = new Label();
		this.lblPreview.setWidth("100%");
		this.lblPreview.setContentMode(ContentMode.HTML);
		this.addTab(lblPreview, I.trc("Caption", "Preview"));

		this.addSelectedTabChangeListener(x -> {
			this.lblPreview.setValue(MarkdownHtmlConverterUtil.convertMarkdownToHtmlString(this.codeMirrorComponent.getValue()));
		});

	}

	public String getValue() {
		String v = this.codeMirrorComponent.getValue();
		if (v == null) {
			return null;
		}
		return Jsoup.clean(v, Safelist.none()); // TODO: tesztelni, hogy a Jsoup.clean nem teszi-e tönkre
	}

	public void setValue(final String value) {
		this.codeMirrorComponent.setValue(value);
	}

	public Registration addValueChangeListener(final HasValue.ValueChangeListener<String> listener) { // erre a metódusra a field miatt van szükség, különben nem érzékeli az érték változást
		return this.codeMirrorComponent.addValueChangeListener(listener);
	}

}
