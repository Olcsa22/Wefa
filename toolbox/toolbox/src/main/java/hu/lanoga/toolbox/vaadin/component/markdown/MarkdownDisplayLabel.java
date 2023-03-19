package hu.lanoga.toolbox.vaadin.component.markdown;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;

import hu.lanoga.toolbox.util.MarkdownHtmlConverterUtil;

public class MarkdownDisplayLabel extends Label {

	public MarkdownDisplayLabel() {
		this.setContentMode(ContentMode.HTML);
	}

	@Override
	public void setValue(final String value) {
		super.setValue(MarkdownHtmlConverterUtil.convertMarkdownToHtmlString(value));
	}

}
