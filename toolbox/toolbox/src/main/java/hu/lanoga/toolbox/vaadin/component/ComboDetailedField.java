package hu.lanoga.toolbox.vaadin.component;

import com.teamunify.i18n.I;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import hu.lanoga.toolbox.filter.internal.SearchCriteria;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.service.ToolboxCrudService;
import hu.lanoga.toolbox.vaadin.component.crud.CrudFormComponent;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponent;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponentBuilder;
import hu.lanoga.toolbox.vaadin.util.UiHelper;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * experimental... lehet, hogy nem volt tesztelve még // TODO: tisztázni
 */
@SuppressWarnings("rawtypes")
public class ComboDetailedField<D extends ToolboxPersistable> extends CustomField<Integer> {

	private Window dialog;

	private D selected;

	private final Class<D> modelType;
	private final ToolboxCrudService<D> crudService;
	private final Supplier<CrudFormComponent<ToolboxPersistable>> crudFormComponentSupplier;
	private final Function<D, String> itemCaptionFunction;
	
	private final CrudGridComponentBuilder crudGridComponentBuilder;
	private CrudGridComponent crudGridComponent;

	private HorizontalLayout hlContent;
	private ComboBox<Integer> cmb = new ComboBox<>();
	private Integer value;

	@SuppressWarnings("unchecked")
	public ComboDetailedField(final String caption, final Class<D> modelType, final ToolboxCrudService<D> crudService, final Supplier<CrudFormComponent<ToolboxPersistable>> crudFormComponentSupplier, final Function<D, String> itemCaptionFunction, final Set<SearchCriteria> initialFixedSearchCriteriaSet) {
		super();

		this.modelType = modelType;
		this.crudService = crudService;
		this.crudFormComponentSupplier = crudFormComponentSupplier;
		this.itemCaptionFunction = itemCaptionFunction;

		// ---

		this.setCaption(caption);
		this.crudGridComponentBuilder = new CrudGridComponentBuilder<>()
				.setModelType((Class<ToolboxPersistable>) this.modelType)
				.setLeftRightPaging(false)
				.setCrudFormComponentSupplier(this.crudFormComponentSupplier)
				.setCrudService((ToolboxCrudService<ToolboxPersistable>) crudService);

		// ---

		if (initialFixedSearchCriteriaSet != null && !initialFixedSearchCriteriaSet.isEmpty()) {
			crudGridComponentBuilder.setInitialFixedSearchCriteriaSet(initialFixedSearchCriteriaSet);
		}
	}

	@SuppressWarnings({"unchecked"})
	@Override
	protected Component initContent() {

		this.hlContent = new HorizontalLayout();

		if (this.modelType == null || this.crudService == null) {
			return new ComboBox();
		}

		final List<D> list = this.crudService.findAll();
		list.sort(Comparator.comparing(D::getModifiedOn).reversed());

		this.cmb = UiHelper.buildCombo1(null,
				list,
				itemCaptionFunction);

		this.cmb.setEmptySelectionAllowed(false);
		this.cmb.addValueChangeListener(x -> {
			if (this.cmb.getValue() != null) {

				final Integer oldValue = this.value;
				this.selected = this.crudService.findOne(x.getValue());
				this.value = this.selected.getId();

				final Collection<?> listeners = this.getListeners(ValueChangeEvent.class);
				for (final Object listener : listeners) {

					// ez szükséges a CustomField implementációkban, ha nem triggerelünk egy ValueChangeEvent-et, akkor a getValue() sem fog meghívódni (a binder-ből)
					// (értsd: ezzel meg kell "jelölni", hogy változott és csak akkor mozdul rá a binder save-nél)

					((ValueChangeListener) listener).valueChange(new ValueChangeEvent<>(this, oldValue, true));
				}

			}
		});

		this.cmb.setValue(this.value);
		this.hlContent.addComponent(this.cmb);

		final Button btnOpen = new Button(VaadinIcons.SEARCH);

		btnOpen.addClickListener(event -> {
			this.initDialog();
		});

		btnOpen.setWidth("100%");
		this.hlContent.addComponent(btnOpen);

		this.hlContent.setComponentAlignment(this.cmb, Alignment.MIDDLE_CENTER);
		this.hlContent.setExpandRatio(this.cmb, 0.8f);

		this.hlContent.setComponentAlignment(btnOpen, Alignment.MIDDLE_CENTER);
		this.hlContent.setExpandRatio(btnOpen, 0.2f);

		this.hlContent.setWidth("100%");

		this.hlContent.addAttachListener(x -> {
			if (!this.isEnabled() || this.isReadOnly()) {
				this.hlContent.setEnabled(false);
			}
		});

		return this.hlContent;

	}

	@SuppressWarnings({"unchecked"})
	private void initDialog() {

		final VerticalLayout vlWrapper = new VerticalLayout();
		vlWrapper.setWidth("100%");

		this.dialog = new Window(I.trc("Button", "Select one"), vlWrapper);
		this.dialog.setWidth("1200px");
		this.dialog.setModal(true);
		this.dialog.setContent(vlWrapper);

		this.crudGridComponentBuilder.setSelectionMode(SelectionMode.SINGLE);

		this.crudGridComponent = this.crudGridComponentBuilder.createCrudGridComponent();

		// if (this.isReadOnly()) {
		this.crudGridComponent.toggleButtonVisibility(true, false, false, false, true, true, false, true);
		// }

		this.crudGridComponent.setSelectedItemWithId(this.value);

		final Button btnOk = new Button(I.trc("Button", "OK"));
		btnOk.setWidth("");
		btnOk.addStyleName("min-width-150px");
		btnOk.addStyleName("max-width-400px");
		btnOk.addStyleName(ValoTheme.BUTTON_FRIENDLY);

		btnOk.addClickListener(y -> {

			final Integer oldValue = this.value;

			if (!this.crudGridComponent.getSelectedItems().isEmpty()) {

				final Set<D> selectedItemIds = this.crudGridComponent.getSelectedItems();

				if (selectedItemIds.size() == 1) {
					this.selected = selectedItemIds.iterator().next();
					this.value = this.selected.getId();
					this.cmb.setValue(this.selected.getId());
				}

				final Collection<?> listeners = this.getListeners(ValueChangeEvent.class);
				for (final Object listener : listeners) {

					// ez szükséges a CustomField implementációkban, ha nem triggerelünk egy ValueChangeEvent-et, akkor a getValue() sem fog meghívódni (a binder-ből)
					// (értsd: ezzel meg kell "jelölni", hogy változott és csak akkor mozdul rá a binder save-nél)

					((ValueChangeListener) listener).valueChange(new ValueChangeEvent<>(this, oldValue, true));
				}

				this.dialog.close();
			} else {
				Notification.show(I.trc("Notification", "Please select one first"), Notification.Type.HUMANIZED_MESSAGE);
			}

		});

		vlWrapper.addComponents(this.crudGridComponent, btnOk);

		UI.getCurrent().addWindow(this.dialog);
		UiHelper.forceDialogFocus(this.dialog);
	}

	@Override
	public Integer getValue() {
		return this.value;
	}

	@Override
	protected void doSetValue(final Integer valueToBeSet) {
		this.value = valueToBeSet;
	}

}