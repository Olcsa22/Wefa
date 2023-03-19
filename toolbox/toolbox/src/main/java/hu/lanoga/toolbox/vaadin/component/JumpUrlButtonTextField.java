package hu.lanoga.toolbox.vaadin.component;

import org.apache.commons.lang3.StringUtils;

import com.teamunify.i18n.I;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.UI;

import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.Getter;
import lombok.Setter;

/**
 * @see UiHelper#buildJumpUrlStr(String, String, String, boolean, boolean)
 */
@Getter
@Setter
public class JumpUrlButtonTextField extends ButtonTextField {

	private String viewNameStr;
	private String paramName;
	boolean makeAbsoluteUrl;
	boolean openViewDialog;
	boolean isBackOffice;

	public JumpUrlButtonTextField(final String caption, final String viewNameStr) {
		this(caption, viewNameStr, "id", false, true);
	}

	public JumpUrlButtonTextField(final String caption, final String viewNameStr, final String paramName, final Boolean makeAbsoluteUrl, final Boolean openViewDialog) {
		this(caption, viewNameStr, paramName, makeAbsoluteUrl, openViewDialog, true);
	}

	public JumpUrlButtonTextField(final String caption, final String viewNameStr, final String paramName, final Boolean makeAbsoluteUrl, final Boolean openViewDialog, boolean isBackOffice) {

		super(caption, VaadinIcons.CLIPBOARD, null, I.trc("Button", "Jump to record URL!"), null, true);

		this.viewNameStr = viewNameStr;
		this.paramName = paramName;
		this.makeAbsoluteUrl = makeAbsoluteUrl;
		this.openViewDialog = openViewDialog;
		this.isBackOffice = isBackOffice;

		this.setBtnClickListener(x -> {
			if (StringUtils.isNotBlank(this.getValue())) {
				final String jumpUrlStr = UiHelper.buildJumpUrlStr(this.viewNameStr, this.paramName, this.getValue(), this.makeAbsoluteUrl, this.openViewDialog, this.isBackOffice);
				UI.getCurrent().getPage().open(jumpUrlStr, "_blank");
			}
		});

	}

}
