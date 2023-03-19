package hu.lanoga.wefa.vaadin;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.teamunify.i18n.I;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import hu.lanoga.toolbox.json.jackson.JacksonHelper;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponent;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponentBuilder;
import hu.lanoga.toolbox.vaadin.component.crud.FormLayoutCrudFormComponent;
import hu.lanoga.wefa.model.WefaProcessInstance;
import hu.lanoga.wefa.service.WefaProcessInstanceService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessInstanceListView extends VerticalLayout implements View {

	private final WefaProcessInstanceService wefaProcessInstanceService;

	private WefaProcessInstance selecttedWefaProcessInstance;

	public ProcessInstanceListView() {

		this.wefaProcessInstanceService = ApplicationContextHelper.getBean(WefaProcessInstanceService.class);

		final CrudGridComponentBuilder<WefaProcessInstance> processInstanceGridBuilder = new CrudGridComponentBuilder<>();
		processInstanceGridBuilder.setModelType(WefaProcessInstance.class);
		processInstanceGridBuilder.setCrudFormComponentSupplier(() -> new FormLayoutCrudFormComponent<>(() -> new WefaProcessInstance.VaadinForm()));
		processInstanceGridBuilder.setCrudService(this.wefaProcessInstanceService);
		processInstanceGridBuilder.setBooleanNullChar("");
		
		final CrudGridComponent<WefaProcessInstance> crudGridComponent = processInstanceGridBuilder.createCrudGridComponent();
		crudGridComponent.setAsyncExport(true);

		crudGridComponent.toggleButtonVisibility(true, false, false, false, false, true, true, true);
		crudGridComponent.getGrid().sort("modifiedOn", SortDirection.DESCENDING);

		final Button btnViewProcVars = new Button(I.trc("Button", "Adatok áttekintése (nyers)"));
		btnViewProcVars.setIcon(VaadinIcons.BOOK);
		btnViewProcVars.setEnabled(false);
		btnViewProcVars.setDisableOnClick(true);
		btnViewProcVars.addClickListener(x -> {
			btnViewProcVars.setEnabled(true);
			// this.selectedHistoricViewModel;

			// ---

			final Window dialog = new Window(I.trc("Caption", "Adatok áttekintése (nyers)"));
			dialog.setWidth("600px");
			dialog.setHeight("");
			dialog.setModal(true);

			final FormLayout fl = new FormLayout();
			fl.setMargin(true);
			dialog.setContent(fl);
			UI.getCurrent().addWindow(dialog);

			// log.debug("getProcessVariables(): " + this.wefaProcessInstance.getProcessVariables());

			if (StringUtils.isNotBlank(this.selecttedWefaProcessInstance.getProcessVariables())) {

				final Map<String, Object> m = JacksonHelper.fromJsonToMap(this.selecttedWefaProcessInstance.getProcessVariables());

				for (final Entry<String, Object> entry : m.entrySet()) {
					if (!"staleDataCheckTs".equalsIgnoreCase(entry.getKey()) && entry.getValue() != null) {
						final TextField txt = new TextField(entry.getKey());
						txt.setValue(entry.getValue().toString());
						txt.setWidth("100%");
						txt.setReadOnly(true);
						fl.addComponent(txt);
					}
				}
			}

		});

		final Button btnViewFormDetails = new Button(I.trc("Button", "Adatok áttekintése"));
		btnViewFormDetails.setIcon(VaadinIcons.FORM);
		btnViewFormDetails.setEnabled(false);
		btnViewFormDetails.setDisableOnClick(true);
		btnViewFormDetails.addClickListener(x -> {
			btnViewFormDetails.setEnabled(true);
			UI.getCurrent().addWindow(new GroovyProcessInstanceViewFormDialog(this.selecttedWefaProcessInstance.getProcessInstanceId()));
		});

		final HorizontalLayout hlHeader = new HorizontalLayout();
		hlHeader.addComponents(btnViewProcVars, btnViewFormDetails);

		crudGridComponent.addAdditionalHeaderToolbar(hlHeader);
		// historicCrudGridComponent.setMarkRefreshButtonCheckSeconds(-1);

		crudGridComponent.setSelectionConsumer(x -> {

			this.selecttedWefaProcessInstance = x;

			boolean b = ((x != null) /* && (StringUtils.isNotBlank(x.getProcessVariables())) */);
			btnViewProcVars.setEnabled(b);
			btnViewFormDetails.setEnabled(b);

		});

		this.addComponent(crudGridComponent);

	}

}