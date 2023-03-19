package hu.lanoga.toolbox.vaadin.component;

import org.apache.commons.lang3.StringUtils;

import com.teamunify.i18n.I;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Notification;

public class CopyToClipboardButtonTextField extends ButtonTextField {

	public CopyToClipboardButtonTextField(final String caption) {

		super(caption, VaadinIcons.CLIPBOARD, null, I.trc("Button", "Copy to clipboard!"), null, true);

		this.setBtnClickListener(x -> {
			if (StringUtils.isNotBlank(this.getValue())) {
				JavaScript.eval("navigator.clipboard.writeText('" + this.getValue() + "');");
				Notification.show(I.trc("Button", "Copied to clipboard!"));
			}
		});

	}

}
