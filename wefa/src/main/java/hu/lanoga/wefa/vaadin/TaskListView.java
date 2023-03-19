package hu.lanoga.wefa.vaadin;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Function;

import org.activiti.engine.task.Task;
import org.apache.commons.beanutils.BeanUtils;
import org.vaadin.alump.materialicons.MaterialIcons;

import com.teamunify.i18n.I;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import hu.lanoga.toolbox.repository.DefaultInMemoryRepository;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.service.RapidLazyEnhanceCrudService;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.util.ServiceUtil;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridColumn;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponent;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponentBuilder;
import hu.lanoga.wefa.exception.WefaGeneralException;
import hu.lanoga.wefa.service.ActivitiHelperService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskListView extends VerticalLayout implements View {

	private ActivitiHelperService activitiHelperService;

	private TaskViewModel selectedTask;

	@Getter
	@Setter
	public static class TaskViewModel implements ToolboxPersistable {

		@CrudGridColumn(translationMsg = "Feladat azonosítója", allowHide = false)
		private String activitiId;

		@CrudGridColumn(translationMsg = "Feladat neve", allowHide = false)
		private String name;

		@CrudGridColumn(translationMsg = "Munkafolyamat def. azon.")
		private String processDefinitionId;
		
		@CrudGridColumn(translationMsg = "Munkafolyamat def. neve")
		private String processDefinitionName;
		
		@CrudGridColumn(translationMsg = "Munkaf. példány azon.", allowHide = false)
		private String processInstanceId;
				
		@CrudGridColumn(translationMsg = "Csoportra osztott", allowHide = false)
		private Boolean isGroup;
		
		@CrudGridColumn(translationMsg = "Létrehozva", allowHide = false)
		private java.util.Date createTime;
		


		@Override
		public Integer getId() {
			return this.activitiId.hashCode();
		}

		@Override
		public void setId(final Integer id) {
			//
		}

	}

	public TaskListView() {
		activitiHelperService = ApplicationContextHelper.getBean(ActivitiHelperService.class);
	}

	@Override
	public void enter(final ViewChangeListener.ViewChangeEvent event) {

		this.removeAllComponents();

		// csak a saját taskjaim
		// mindenkinek, hu.lanoga.wefa.service.ActivitiHelperService.findTasks(String)

		final List<Task> tasks = this.activitiHelperService.findTasks(SecurityUtil.getLoggedInUser().getId());

		final RapidLazyEnhanceCrudService<TaskViewModel, DefaultInMemoryRepository<TaskViewModel>> inMemoryService = ServiceUtil.createInMemoryService(tasks, TaskViewModel.class, new Function<Task, TaskViewModel>() {

			@Override
			public TaskViewModel apply(Task t) {

				try {
					TaskViewModel viewModel = TaskViewModel.class.newInstance();
					BeanUtils.copyProperties(viewModel, t);

					viewModel.setActivitiId(t.getId());
					viewModel.setProcessDefinitionName(activitiHelperService.getProcessDefinition(viewModel.getProcessDefinitionId()).getName());

					boolean b = t.getAssignee().contains(":");
					viewModel.setIsGroup(b);
					
					return viewModel;
				} catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
					throw new WefaGeneralException(e);
				}
			}

		}, null);

		final CrudGridComponentBuilder<TaskViewModel> crudGridComponentBuilder = new CrudGridComponentBuilder<>();
		crudGridComponentBuilder.setModelType(TaskViewModel.class);
		crudGridComponentBuilder.setCrudService(inMemoryService);

		final CrudGridComponent<TaskViewModel> crudGridComponent = crudGridComponentBuilder.createCrudGridComponent();
		crudGridComponent.toggleButtonVisibility(false, false, false, false, false, true, true, false);
		crudGridComponent.setMarkRefreshButtonCheckSeconds(-1);
		
		crudGridComponent.getBtnRefresh().removeListener(ClickEvent.class, crudGridComponent.getBtnRefresh());
		crudGridComponent.getBtnRefresh().addClickListener(x -> {
			this.enter(null);
		});
		
		crudGridComponent.getGrid().sort("createTime", SortDirection.DESCENDING);

		final HorizontalLayout hlHeader = new HorizontalLayout();

		final Button btnDoTask = new Button(I.trc("Button", "Űrlap kitöltése"));
		btnDoTask.setDisableOnClick(true);
		btnDoTask.addClickListener(x -> {
			btnDoTask.setEnabled(true);
			UI.getCurrent().addWindow(new GroovyTaskFormDialog(this.selectedTask.getActivitiId()));
		});
		btnDoTask.setIcon(MaterialIcons.DEVELOPER_BOARD);
		btnDoTask.setEnabled(false);

		hlHeader.addComponents(btnDoTask);

		crudGridComponent.addAdditionalHeaderToolbar(hlHeader);

		crudGridComponent.setSelectionConsumer(x -> {

			this.selectedTask = x;

			if (x != null) {
				btnDoTask.setEnabled(true);
			} else {
				btnDoTask.setEnabled(false);
			}

		});

		this.addComponent(crudGridComponent);
		
	}

}