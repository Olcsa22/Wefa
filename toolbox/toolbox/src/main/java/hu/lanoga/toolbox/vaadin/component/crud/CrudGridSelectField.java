package hu.lanoga.toolbox.vaadin.component.crud;

import java.util.Collection;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.teamunify.i18n.I;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.vaadin.util.UiHelper;

public class CrudGridSelectField<T extends ToolboxPersistable> extends CustomField<Set<Integer>> {

	private Window dialog;
	private Set<Integer> value;

	private final CrudGridComponentBuilder<T> crudGridComponentBuilder;
	private CrudGridComponent<T> crudGridComponent;

	private String btnInitCaption = I.trc("Button", "Edit field (selection)");

	public CrudGridSelectField(final String caption, final String btnInitCaption, final CrudGridComponentBuilder<T> crudGridComponentBuilder) {
		super();
		this.setCaption(caption);
		this.btnInitCaption = btnInitCaption;
		this.crudGridComponentBuilder = crudGridComponentBuilder;
	}

	public CrudGridSelectField(final String caption, final CrudGridComponentBuilder<T> crudGridComponentBuilder) {
		super();
		this.setCaption(caption);
		this.crudGridComponentBuilder = crudGridComponentBuilder;
	}

	@Override
	protected Component initContent() {

		final Button btnInit = new Button(btnInitCaption);
		btnInit.setWidth("100%");
		btnInit.addClickListener(event -> {
			this.initDialog();
		});

		btnInit.addAttachListener(x -> {

			if (!this.isEnabled() || this.isReadOnly()) {

				btnInit.setCaption(I.trc("Button", "View field"));

				this.setEnabled(true);
				this.setReadOnly(true);

				this.setCaption("<span style=\"opacity:0.5\">" + this.getCaption() + "</span>");
				this.setCaptionAsHtml(true);
			}

		});

		return btnInit;

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void initDialog() {

		final VerticalLayout vlDialog = new VerticalLayout();
		vlDialog.setWidth("100%");

		String strDialogCaption;

		if (this.isEnabled() && !this.isReadOnly()) {
			strDialogCaption = I.trc("Title", "Select elements");
		} else {
			strDialogCaption = I.trc("Title", "View selection");
		}

		strDialogCaption += " (" + Jsoup.clean(this.getCaption(), Safelist.none()) + ")";

		this.dialog = new Window(strDialogCaption, vlDialog);
		this.dialog.setWidth("1000px");
		this.dialog.setModal(true);
		this.dialog.setContent(vlDialog);

		// ---

		this.crudGridComponentBuilder.setSelectionMode(SelectionMode.MULTI);

		this.crudGridComponent = this.crudGridComponentBuilder.createCrudGridComponent();
		this.crudGridComponent.setSelectedItemsWithIds(this.value);

		if (!this.isEnabled() || this.isReadOnly()) {
			this.crudGridComponent.toggleButtonVisibility(false, false, false, false, false, false, false, false);
			this.crudGridComponent.getGrid().setEnabled(false);
			// this.crudGridComponent.getGrid().setSelectionMode(SelectionMode.NONE);
		}

		vlDialog.addComponent(this.crudGridComponent);

		// ---

		if (this.isEnabled() && !this.isReadOnly()) {

			final Button btnOk = new Button(I.trc("Button", "OK"));
			btnOk.setWidth("");
			btnOk.addStyleName("min-width-150px");
			btnOk.addStyleName("max-width-400px");
			btnOk.addStyleName(ValoTheme.BUTTON_FRIENDLY);
			btnOk.addClickListener(y -> {

				final Set<Integer> oldValue = this.value;
				this.value = this.crudGridComponent.getSelectedItemIds();

				final Collection<?> listeners = this.getListeners(ValueChangeEvent.class);
				for (final Object listener : listeners) {

					// ez szükséges a CustomField implementációkban, ha nem triggerelünk egy ValueChangeEvent-et, akkor a getValue() sem fog meghívódni (a binder-ből)
					// (értsd: ezzel meg kell "jelölni", hogy változott és csak akkor mozdul rá a binder save-nél)

					((ValueChangeListener) listener).valueChange(new ValueChangeEvent<>(this, oldValue, true));
				}

				this.dialog.close();
			});

			vlDialog.addComponent(btnOk);

		}

		UI.getCurrent().addWindow(this.dialog);
		UiHelper.forceDialogFocus(this.dialog);

	}

	@Override
	public Set<Integer> getValue() {
		return this.value;
	}

	@Override
	protected void doSetValue(final Set<Integer> valueToBetSet) {
		this.value = valueToBetSet;
	}

}