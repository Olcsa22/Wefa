/**
 * based on https://vaadin.com/directory/component/pagedtable (Apache License 2.0, http://www.apache.org/licenses/LICENSE-2.0.html)
 */
package hu.lanoga.toolbox.vaadin.component.crud;

import org.apache.commons.lang3.StringUtils;
import org.vaadin.teemusa.gridextensions.paging.PagedDataProvider;
import org.vaadin.teemusa.gridextensions.paging.PagingControls;

import com.teamunify.i18n.I;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PopupView;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.vaadin.component.NumberOnlyTextField;

/**
 * @param <T>
 * @see PagedDataProvider
 * @see PagingControls
 */
class CrudGridPageControlComponent<T extends ToolboxPersistable> extends HorizontalLayout {

	private final Button btnPageIndicator = new Button();

	private final Button btnFirst = new Button(VaadinIcons.ANGLE_DOUBLE_LEFT);
	private final Button btnPrevious = new Button(VaadinIcons.ANGLE_LEFT);
	private final Button btnNext = new Button(VaadinIcons.ANGLE_RIGHT);
	private final Button btnLast = new Button(VaadinIcons.ANGLE_DOUBLE_RIGHT);

	private final PagingControls<T> pagingControls;

	/**
	 * oldalválasztó popupban...
	 */
	private Label lblNotif;

	public CrudGridPageControlComponent(final PagingControls<T> pagingControls) {

		this.pagingControls = pagingControls;

		// ---

		this.setWidth(null);

		// ---

		final Label lblSpacer = new Label();
		lblSpacer.setWidth("100%");
		this.addComponent(lblSpacer);
		this.setExpandRatio(lblSpacer, 1f);

		final HorizontalLayout hlPageManagement = new HorizontalLayout();
		hlPageManagement.setWidth("100%");
		this.addComponent(hlPageManagement);
		this.setComponentAlignment(hlPageManagement, Alignment.MIDDLE_CENTER);

		// ---

		this.btnFirst.addClickListener(x -> {
			pagingControls.setPageNumber(0);
			this.refreshControlsAndLabels();
		});

		this.btnPrevious.addClickListener(x -> {
			pagingControls.previousPage();
			this.refreshControlsAndLabels();
		});

		this.btnNext.addClickListener(x -> {
			pagingControls.nextPage();
			this.refreshControlsAndLabels();
		});

		this.btnLast.addClickListener(x -> {
			pagingControls.setPageNumber(pagingControls.getPageCount() - 1);
			this.refreshControlsAndLabels();
		});

		final PopupView pvPageSelect;
		final NumberOnlyTextField txtPopupPageSelect;

		{

			final VerticalLayout vlPopup = new VerticalLayout();
			vlPopup.setWidth("300px");

			pvPageSelect = new PopupView(null, vlPopup);
			pvPageSelect.setHideOnMouseOut(false);

			this.lblNotif = new Label();
			this.lblNotif.setWidth("100%");
			vlPopup.addComponent(this.lblNotif);

			txtPopupPageSelect = new NumberOnlyTextField();
			txtPopupPageSelect.setWidth("100%");
			vlPopup.addComponent(txtPopupPageSelect);

			final Button btnPopupOk = new Button(I.trc("Button", "OK"));
			btnPopupOk.setWidth("100%");
			btnPopupOk.addClickListener(z -> {

				final String value = txtPopupPageSelect.getValue();

				if (StringUtils.isBlank(value) || Integer.parseInt(value) < 1 || Integer.parseInt(value) > pagingControls.getPageCount()) {
					Notification.show(this.lblNotif.getValue(), Notification.TYPE_WARNING_MESSAGE);
					return;
				}

				pagingControls.setPageNumber(Integer.parseInt(value) - 1);
				this.refreshControlsAndLabels();

				pvPageSelect.setPopupVisible(false);

			});
			vlPopup.addComponent(btnPopupOk);

			final Button btnPopupClose = new Button(I.trc("Button", "Cancel"));
			btnPopupClose.setWidth("100%");
			btnPopupClose.addClickListener(z -> {
				pvPageSelect.setPopupVisible(false);

			});

			vlPopup.addComponent(btnPopupClose);
		}

		this.btnPageIndicator.addStyleName(ValoTheme.BUTTON_QUIET);
		this.btnPageIndicator.setWidth("");
		this.btnPageIndicator.addClickListener(y -> {

			txtPopupPageSelect.setValue(Integer.toString(pagingControls.getPageNumber() + 1));

			pvPageSelect.setPopupVisible(true);

		});

		this.btnFirst.setWidth("60px");
		this.btnPrevious.setWidth("60px");
		this.btnNext.setWidth("60px");
		this.btnLast.setWidth("60px");

		// ---

		hlPageManagement.addComponent(this.btnPageIndicator);
		hlPageManagement.addComponent(pvPageSelect);
		hlPageManagement.addComponent(this.btnFirst);
		hlPageManagement.addComponent(this.btnPrevious);
		hlPageManagement.addComponent(this.btnNext);
		hlPageManagement.addComponent(this.btnLast);
		hlPageManagement.setComponentAlignment(this.btnFirst, Alignment.MIDDLE_CENTER);
		hlPageManagement.setComponentAlignment(this.btnPrevious, Alignment.MIDDLE_CENTER);
		hlPageManagement.setComponentAlignment(this.btnPageIndicator, Alignment.MIDDLE_CENTER);
		hlPageManagement.setComponentAlignment(this.btnNext, Alignment.MIDDLE_CENTER);
		hlPageManagement.setComponentAlignment(this.btnLast, Alignment.MIDDLE_CENTER);
		hlPageManagement.setWidth(null);
		hlPageManagement.setSpacing(true);

		// ---

		this.refreshControlsAndLabels();
	}

	public int getPageNumber() {
		return this.pagingControls.getPageNumber();
	}

	public int getPageCount() {
		return this.pagingControls.getPageCount();
	}

	public int getPageLength() {
		return this.pagingControls.getPageLength();
	}

	public void refreshControlsAndLabels() {
		
		final int pageCount = this.pagingControls.getPageCount();
		final int pageNumber = this.pagingControls.getPageNumber();

		if (pageCount == 0) {

			this.btnPageIndicator.setCaption("");
			this.btnPageIndicator.setEnabled(false);
			this.btnPageIndicator.addStyleName(ValoTheme.BUTTON_BORDERLESS);

		} else {

			this.btnPageIndicator.setCaption((pageNumber + 1) + "/" + pageCount);
			this.lblNotif.setValue(I.trc("Notification", "Please choose between 1 and") + " " + pageCount);

			if (pageCount == 1) {
				this.btnPageIndicator.setEnabled(false);
				this.btnPageIndicator.addStyleName(ValoTheme.BUTTON_BORDERLESS);
			} else {
				this.btnPageIndicator.setEnabled(true);
				this.btnPageIndicator.removeStyleName(ValoTheme.BUTTON_BORDERLESS);
			}

		}

		this.btnFirst.setEnabled(false);
		this.btnPrevious.setEnabled(false);
		this.btnNext.setEnabled(false);
		this.btnLast.setEnabled(false);

		if (pageNumber > 0) {
			this.btnFirst.setEnabled(true);
			this.btnPrevious.setEnabled(true);
		}

		if (pageNumber < (pageCount - 1)) {
			this.btnNext.setEnabled(true);
			this.btnLast.setEnabled(true);
		}

	}

}
