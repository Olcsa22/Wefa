package hu.lanoga.toolbox.vaadin.component;

import com.google.common.collect.Sets;
import com.teamunify.i18n.I;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.codestore.CodeStoreItem;
import hu.lanoga.toolbox.codestore.CodeStoreItemService;
import hu.lanoga.toolbox.codestore.CodeStoreType;
import hu.lanoga.toolbox.codestore.CodeStoreTypeService;
import hu.lanoga.toolbox.filter.internal.SearchCriteria;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponent;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponentBuilder;
import hu.lanoga.toolbox.vaadin.component.crud.FormLayoutCrudFormComponent;
import hu.lanoga.toolbox.vaadin.util.UiHelper;


/**
 * super admin célokra szolgál
 */
public class CodeStoreComponent extends VerticalLayout {

	private CodeStoreType selectedCodeStoreType;

	protected Integer leftRightPagingPageSize;

	public void initLayout() {

		this.removeAllComponents();

		// ---

		final CodeStoreTypeService codeStoreTypeService = ApplicationContextHelper.getBean(CodeStoreTypeService.class);

		final CrudGridComponentBuilder<CodeStoreType> codeStoreTypeCrudBuilder = new CrudGridComponentBuilder<CodeStoreType>()
				.setModelType(CodeStoreType.class)
				.setCrudFormComponentSupplier(() -> new FormLayoutCrudFormComponent<>(() -> new CodeStoreType.VaadinForm()))
				.setCrudService(codeStoreTypeService)
				.setLeftRightPaging(true);

		if (this.leftRightPagingPageSize != null) {
			codeStoreTypeCrudBuilder.setLeftRightPagingPageSize(this.leftRightPagingPageSize);
		}

		final CrudGridComponent<CodeStoreType> codeStoreTypeCrud = codeStoreTypeCrudBuilder.createCrudGridComponent();
		codeStoreTypeCrud.toggleButtonVisibility(true, false, false, false, false, true, true, true);

		final HorizontalLayout hlFooter = new HorizontalLayout();

		final Button btnSeeCodeStoreItems = new Button(I.trc("Button", "Items"));
		btnSeeCodeStoreItems.setIcon(VaadinIcons.LIST);
		btnSeeCodeStoreItems.setEnabled(false);
		btnSeeCodeStoreItems.addClickListener(x -> {

			final Window dialog = new Window(I.trc("Caption", "Items for") + ": " + this.selectedCodeStoreType.getCaptionCaption());
			dialog.setModal(true);
			dialog.setWidth("1000px");

			final VerticalLayout vlContent = new VerticalLayout();
			vlContent.setWidth("100%");

			final SearchCriteria fixedSearchCriteria = SearchCriteria.builder()
					.fieldName("codeStoreTypeId")
					.criteriaType(Integer.class)
					.operation(ToolboxSysKeys.SearchCriteriaOperation.EQ)
					.value(selectedCodeStoreType.getId())
					.build();

			final CodeStoreItemService codeStoreItemService = ApplicationContextHelper.getBean(CodeStoreItemService.class);

			final CrudGridComponent<CodeStoreItem> codeStoreItemCrud = new CrudGridComponent<>(
					CodeStoreItem.class,
					codeStoreItemService,
					() -> new FormLayoutCrudFormComponent<>(() -> new CodeStoreItem.VaadinForm() {

						@Override
						public Void afterBind(CodeStoreItem modelObject, ToolboxSysKeys.CrudOperation crudOperation) {
							this.codeStoreTypeId.setValue(selectedCodeStoreType.getId());

							return null;
						}
					}),
					true, Sets.newHashSet(fixedSearchCriteria));

			// ha nem bővíthető, akkor csak megnézhetjük és szerkeszthetjük a meglévőt
			if (!this.selectedCodeStoreType.getExpandable()) {
				codeStoreItemCrud.toggleButtonVisibility(true, false, true, false, false, true, true, true);
			}

			vlContent.addComponent(codeStoreItemCrud);

			final HorizontalLayout hlButtons = new HorizontalLayout();


			final Button btnClose = new Button(I.trc("Button", "Close"));
			btnClose.addStyleName(ValoTheme.BUTTON_PRIMARY);
			btnClose.setWidth("150px");

			btnClose.addClickListener(y -> {
				UiHelper.closeParentWindow(btnClose);
			});

			hlButtons.addComponent(btnClose);
			vlContent.addComponent(hlButtons);

			dialog.setContent(vlContent);
			UI.getCurrent().addWindow(dialog);

		});

		hlFooter.addComponent(btnSeeCodeStoreItems);

		codeStoreTypeCrud.addAdditionalFooterToolbar(hlFooter);

		codeStoreTypeCrud.setSelectionConsumer(x -> {
			if (x != null) {
				this.selectedCodeStoreType = x;

				btnSeeCodeStoreItems.setEnabled(true);
			} else {
				btnSeeCodeStoreItems.setEnabled(false);
			}
		});

		this.addComponent(codeStoreTypeCrud);

	}

}
