package hu.lanoga.toolbox.vaadin.component.crud;

import java.util.Collection;
import java.util.List;

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

import hu.lanoga.toolbox.repository.DefaultInMemoryRepository;
import hu.lanoga.toolbox.repository.RapidRepository;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.service.RapidCrudService;
import hu.lanoga.toolbox.service.ToolboxCrudService;
import hu.lanoga.toolbox.util.ToolboxAssert;
import hu.lanoga.toolbox.vaadin.util.UiHelper;

/**
 * olyan {@link CrudGridComponent}, amit {@link CrudFormElementCollection}-ben lehet használni (tehát az egy második szint, egy dialogban egy grid...)
 * 
 * @param <T>
 */
public class CrudGridField<T extends ToolboxPersistable> extends CustomField<List<T>> {

	private Window dialog;
	private List<T> value;

	private final CrudGridComponentBuilder<T> crudGridComponentBuilder;
	private CrudGridComponent<T> crudGridComponent;

	public CrudGridField(final String caption, final CrudGridComponentBuilder<T> crudGridComponentBuilder) {
		super();
		this.setCaption(caption);
		this.crudGridComponentBuilder = crudGridComponentBuilder;
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initDialog() {

		final VerticalLayout vlDialog = new VerticalLayout();
		vlDialog.setWidth("100%");

		String strDialogCaption;

		if (this.isEnabled() && !this.isReadOnly()) {
			strDialogCaption = I.trc("Title", "Edit materials");
		} else {
			strDialogCaption = I.trc("Title", "View materials");
		}

		strDialogCaption += " (" + Jsoup.clean(this.getCaption(), Safelist.none()) + ")";

		this.dialog = new Window(strDialogCaption, vlDialog);
		this.dialog.setWidth("1000px");
		this.dialog.setModal(true);
		this.dialog.setContent(vlDialog);

		// ---

		this.crudGridComponentBuilder.setCrudService(new RapidCrudService<>(new DefaultInMemoryRepository<>(this.crudGridComponentBuilder.getModelType())));

		this.crudGridComponent = this.crudGridComponentBuilder.createCrudGridComponent();

		if (!this.isEnabled() || this.isReadOnly()) {
			this.crudGridComponent.toggleButtonVisibility(true, false, false, false, false, true, true, false);
			this.crudGridComponent.getGrid().setEnabled(false);
			this.crudGridComponent.getGrid().setSelectionMode(SelectionMode.NONE);
		}

		vlDialog.addComponent(this.crudGridComponent);

		// ---

		final ToolboxCrudService<T> service = this.crudGridComponent.getCrudService();
		ToolboxAssert.isTrue(service instanceof RapidCrudService); // felesleges, mert jelenleg pár sorral feljebb itt állítjuk be... setCrudService...

		final RapidRepository<T> repository = ((RapidCrudService<T, RapidRepository<T>>) service).getRepository();
		repository.initWithData(this.value);

		// ---

		if (this.isEnabled() && !this.isReadOnly()) {

			final Button btnOk = new Button(I.trc("Button", "OK"));
			btnOk.setWidth("");
			btnOk.addStyleName("min-width-150px");
			btnOk.addStyleName("max-width-400px");
			btnOk.addStyleName(ValoTheme.BUTTON_FRIENDLY);
			btnOk.addClickListener(y -> {

				final List<T> oldValue = this.value;
				this.value = (List<T>) repository.findAll(); // TODO: tisztázni

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
	public List<T> getValue() {
		return this.value;
	}

	@Override
	protected void doSetValue(final List<T> valueToBeSet) {
		this.value = valueToBeSet;
	}

}