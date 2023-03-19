package hu.lanoga.toolbox.vaadin.component;

import org.apache.commons.lang3.StringUtils;

import com.teamunify.i18n.I;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Notification;

import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.Getter;
import lombok.Setter;

/**
 * @see UiHelper#buildJumpUrlStr(String, String, String, boolean, boolean)
 */
@Getter
@Setter
public class JumpUrlCopyToClipboardButtonTextField extends ButtonTextField {

	private String viewNameStr;
	private String paramName;
	boolean makeAbsoluteUrl;
	boolean openViewDialog;
	boolean isBackOffice;

	public JumpUrlCopyToClipboardButtonTextField(final String caption, final String viewNameStr) {
		this(caption, viewNameStr, "id", true, true);
	}

	public JumpUrlCopyToClipboardButtonTextField(final String caption, final String viewNameStr, final String paramName, final boolean makeAbsoluteUrl, final boolean openViewDialog) {
		this(caption, viewNameStr, paramName, makeAbsoluteUrl, openViewDialog, true);
	}

	public JumpUrlCopyToClipboardButtonTextField(final String caption, final String viewNameStr, final String paramName, final boolean makeAbsoluteUrl, final boolean openViewDialog, boolean isBackOffice) {

		super(caption, VaadinIcons.CLIPBOARD, null, I.trc("Button", "Copy jump URL to clipboard!"), null, true);

		this.viewNameStr = viewNameStr;
		this.paramName = paramName;
		this.makeAbsoluteUrl = makeAbsoluteUrl;
		this.openViewDialog = openViewDialog;
		this.isBackOffice = isBackOffice;

		this.setBtnClickListener(x -> {
			if (StringUtils.isNotBlank(this.getValue())) {
				final String jumpUrlStr = UiHelper.buildJumpUrlStr(this.viewNameStr, this.paramName, this.getValue(), this.makeAbsoluteUrl, this.openViewDialog, this.isBackOffice);
				JavaScript.eval("navigator.clipboard.writeText('" + jumpUrlStr + "');");
				Notification.show(I.trc("Button", "Jump URL copied to clipboard!"));
			}
		});

	}

}
