package hu.lanoga.toolbox.vaadin.component;

import org.apache.commons.lang3.StringUtils;

import com.teamunify.i18n.I;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.UI;

public class UrlButtonTextField extends ButtonTextField {

	public UrlButtonTextField(final String caption) {

		super(caption, VaadinIcons.ARROW_CIRCLE_RIGHT_O, null, I.trc("Button", "Jump to URL!"), null, true);

		this.setBtnClickListener(x -> {
			if (StringUtils.isNotBlank(this.getValue())) {

				if (this.getValue() != null && StringUtils.isNotBlank(this.getValue()) && StringUtils.contains(this.getValue(), ".") && !StringUtils.containsIgnoreCase(this.getValue(), "http")) { // a "."-ot azért nézzük meg, mert azokban, amikben még egy "." sincs, azokat relatív URL-nek vesszük itt és nem módosítunk rá
					UI.getCurrent().getPage().open("http://" + this.getValue(), "_blank");
				} else {
					UI.getCurrent().getPage().open(this.getValue(), "_blank");
				}

			}
		});

	}

}
