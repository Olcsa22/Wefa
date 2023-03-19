package hu.lanoga.wefa.vaadin.procdef;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Function;

import org.activiti.engine.repository.ProcessDefinition;
import org.apache.commons.beanutils.BeanUtils;

import com.teamunify.i18n.I;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import hu.lanoga.toolbox.exception.ManualValidationException;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreHelper;
import hu.lanoga.toolbox.repository.DefaultInMemoryRepository;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.service.RapidLazyEnhanceCrudService;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.util.BrandUtil;
import hu.lanoga.toolbox.util.ServiceUtil;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridColumn;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponent;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponentBuilder;
import hu.lanoga.toolbox.vaadin.component.file.FileManagerComponent;
import hu.lanoga.toolbox.vaadin.component.file.FileManagerComponentBuilder;
import hu.lanoga.wefa.SysKeys;
import hu.lanoga.wefa.exception.WefaGeneralException;
import hu.lanoga.wefa.service.ActivitiHelperService;
import hu.lanoga.wefa.vaadin.MainUI;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessDefinitionListView extends VerticalLayout implements View {

	private ProcessDefinitionViewModel selectedProcDef;

	@Getter
	@Setter
	public static class ProcessDefinitionViewModel implements ToolboxPersistable {

		@CrudGridColumn(translationMsg = "Munkafolyamat def. azonosítója", allowHide = false)
		private String procDefId;

		@CrudGridColumn(translationMsg = "Munkafolyamat def. kulcs", allowHide = false)
		private String key;

		@CrudGridColumn(translationMsg = "Munkafolyamat def. neve", allowHide = false)
		private String name;

		@Override
		public Integer getId() {
			return this.procDefId.hashCode();
		}

		@Override
		public void setId(final Integer id) {
			//
		}

	}

	private final ActivitiHelperService activitiHelperService = ApplicationContextHelper.getBean(ActivitiHelperService.class);

	@Override
	public void enter(ViewChangeEvent event) {

		this.removeAllComponents();

		// ---

		final List<ProcessDefinition> list = this.activitiHelperService.listProcessDefinitions(true);

		final RapidLazyEnhanceCrudService<ProcessDefinitionViewModel, DefaultInMemoryRepository<ProcessDefinitionViewModel>> inMemoryService = ServiceUtil.createInMemoryService(list, ProcessDefinitionViewModel.class, new Function<ProcessDefinition, ProcessDefinitionViewModel>() {

			@Override
			public ProcessDefinitionViewModel apply(final ProcessDefinition t) {
				try {
					final ProcessDefinitionViewModel viewModel = ProcessDefinitionViewModel.class.newInstance();
					BeanUtils.copyProperties(viewModel, t);

					viewModel.setProcDefId(t.getId());

					return viewModel;
				} catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
					throw new WefaGeneralException(e);
				}
			}
		}, null);

		final CrudGridComponentBuilder<ProcessDefinitionViewModel> crudGridComponentBuilder = new CrudGridComponentBuilder<>();
		crudGridComponentBuilder.setModelType(ProcessDefinitionViewModel.class);
		crudGridComponentBuilder.setCrudService(inMemoryService);

		final CrudGridComponent<ProcessDefinitionViewModel> crudGridComponent = crudGridComponentBuilder.createCrudGridComponent();
		crudGridComponent.toggleButtonVisibility(false, false, false, false, false, true, true, false);
		crudGridComponent.setMarkRefreshButtonCheckSeconds(-1);

		crudGridComponent.getBtnRefresh().removeListener(ClickEvent.class, crudGridComponent.getBtnRefresh());
		crudGridComponent.getBtnRefresh().addClickListener(x -> {
			this.enter(null);
		});

		this.addComponent(crudGridComponent);

		// ---

		final HorizontalLayout hlHeader = new HorizontalLayout();
		crudGridComponent.addAdditionalHeaderToolbar(hlHeader);

		final Button btnAdd = new Button();
		btnAdd.setIcon(VaadinIcons.PLUS);
		btnAdd.addClickListener(x -> {
			UI.getCurrent().getNavigator().navigateTo(MainUI.NavConst.PROCESS_DEF_EDITOR + "/new");
		});

		final Button btnEdit = new Button();
		btnEdit.setIcon(VaadinIcons.PENCIL);
		btnEdit.setEnabled(false);
		btnEdit.addClickListener(x -> {
			UI.getCurrent().getNavigator().navigateTo(MainUI.NavConst.PROCESS_DEF_EDITOR + "/" + this.selectedProcDef.getProcDefId());
		});

		final Button btnViewDiagram = new Button(I.trc("Button", "Diagram"));
		btnViewDiagram.setIcon(VaadinIcons.SPLIT);
		btnViewDiagram.setEnabled(false);
		btnViewDiagram.setDisableOnClick(true);
		btnViewDiagram.addClickListener(x -> {

			final byte[] ba = this.activitiHelperService.loadBackDeployedProcDefDiagramImg(this.selectedProcDef.getProcDefId());

			final Window diagramDialog = new Window(I.trc("Caption", "Diagram a következő munkafolyamathoz") + ": " + this.selectedProcDef.getProcDefId());
			diagramDialog.setWidth("");
			diagramDialog.setHeight("");
			diagramDialog.setModal(true);

			final Image img = new Image(this.selectedProcDef.getProcDefId(), new StreamResource(new StreamResource.StreamSource() {

				@Override
				public InputStream getStream() {
					return new ByteArrayInputStream(ba);
				}

			}, "diagram-" + this.selectedProcDef.getProcDefId() + ".png"));
			img.setSizeUndefined();

			diagramDialog.setContent(img);

			UI.getCurrent().addWindow(diagramDialog);

			btnViewDiagram.setEnabled(true);

		});

		final Button btnStart = new Button(I.trc("Button", "Indít"));
		btnStart.setIcon(VaadinIcons.PLAY);
		btnStart.setEnabled(false);
		btnStart.setDisableOnClick(true);
		btnStart.addClickListener(x -> {

			this.activitiHelperService.startProcess(this.selectedProcDef.getProcDefId(), null);
			btnStart.setEnabled(true);

			Notification.show(I.trc("Notification", "Munkafolyamat elindítva!")); // TODO: I.trc plusz részletesebb szöveg (key, name, id stb.)

		});

		final Button btnDemoPublicForm = new Button(I.trc("Button", "Publ. form demo"));
		btnDemoPublicForm.setIcon(VaadinIcons.DESKTOP);
		btnDemoPublicForm.setEnabled(false);
		btnDemoPublicForm.addClickListener(x -> {

			final Window dialog = new Window(I.trc("Caption", "Publikus form demo") + ": " + this.selectedProcDef.getProcDefId());
			dialog.setWidth("300px");
			dialog.setHeight("");
			dialog.setModal(true);

			VerticalLayout vl = new VerticalLayout();
			vl.setWidth("100%");
			dialog.setContent(vl);

			Button btn1 = new Button(I.trc("Caption", "Demo 1 (iframe)"));
			btn1.setWidth("100%");
			Button btn2 = new Button(I.trc("Caption", "Demo 2 (full)"));
			btn2.setWidth("100%");
			vl.addComponents(btn1, btn2);

			btn1.addClickListener(y -> {
				
				final String publicFormUrl = BrandUtil.getRedirectUriHostFrontend() + "public/public-form-demo-iframe?t=" + SecurityUtil.getLoggedInUserTenantId() + "&p=" + this.selectedProcDef.getKey();

				JavaScript.eval("navigator.clipboard.writeText('" + publicFormUrl + "');");

				Notification n = new Notification(I.trc("Button", "A publikus indító form (iframe demo) URL-je a vágólapra lett másolva!<br>Ez a publikus form külön HTTP session-nel működik (kilépteti az admin user-t),<br>tehát inkognitó módban vagy másik böngészőben lehet kényelmesen tesztelni."));
				n.setHtmlContentAllowed(true);
				n.show(Page.getCurrent());

			});

			btn2.addClickListener(y -> {
				
				String publicFormUrl = BrandUtil.getRedirectUriHostFrontend() + "public/form#" + SecurityUtil.getLoggedInUserTenantId() + "/" + this.selectedProcDef.getKey();
				
				JavaScript.eval("navigator.clipboard.writeText('" + publicFormUrl + "');");

				Notification n = new Notification(I.trc("Button", "A publikus indító form (ügyfeleknek szóló) URL-je a vágólapra lett másolva!<br>Ez a publikus form külön HTTP session-nel működik (kilépteti az admin user-t),<br>tehát inkognitó módban vagy másik böngészőben lehet kényelmesen tesztelni."));
				n.setHtmlContentAllowed(true);
				n.show(Page.getCurrent());

			});
			
			UI.getCurrent().addWindow(dialog);

		});

		final Button btnImport = new Button(I.trc("Button", "Imp."));
		btnImport.setIcon(VaadinIcons.PLUS_CIRCLE);
		btnImport.addClickListener(x -> {

			// import

			final Window importDialog = new Window(I.trc("Caption", "Import"));
			importDialog.setWidth("600px");
			importDialog.setHeight("");
			importDialog.setModal(true);

			final VerticalLayout vlImport = new VerticalLayout();
			importDialog.setContent(vlImport);
			UI.getCurrent().addWindow(importDialog);

			final FileManagerComponentBuilder fmcb = new FileManagerComponentBuilder();
			fmcb.setMaxFileCount(1);
			final FileManagerComponent fmc = fmcb.createFileManagerComponent();
			fmc.setWidth("100%");
			vlImport.addComponent(fmc);

			final Button btn = new Button(I.trc("Caption", "Import"));
			btn.setWidth("100%");
			btn.setDisableOnClick(true);
			vlImport.addComponent(btn);

			btn.addClickListener(y -> {
				final List<FileDescriptor> fileDescriptors = fmc.getFileDescriptors();
				if (fileDescriptors.isEmpty()) {
					btn.setEnabled(true);
					throw new ManualValidationException("Missing import file!", I.trc("Notification", "Nincs feltöltött fájl!"));
				}
				String procDefId = this.activitiHelperService.importProcessDefinition(fileDescriptors.get(0));

				UI.getCurrent().showNotification(I.trc("Notification", "Import sikerült, procDefId") + ": " + procDefId, Notification.TYPE_HUMANIZED_MESSAGE);

				importDialog.close();

				this.enter(null);
			});

		});

		final Button btnExport = new Button(I.trc("Button", "Exp."));
		btnExport.setIcon(VaadinIcons.DOWNLOAD);
		btnExport.setEnabled(false);
		btnExport.addClickListener(x -> {

			final FileDescriptor fd = this.activitiHelperService.exportProcessDefinition(this.selectedProcDef.getProcDefId());
			JavaScript.eval(FileStoreHelper.generateJsDownloadScript(fd.getId()));

		});

		if (SecurityUtil.hasRole(SysKeys.UserAuth.ROLE_ADMIN_STR)) {
			hlHeader.addComponent(btnAdd);
			hlHeader.addComponent(btnEdit);
			hlHeader.addComponent(btnDemoPublicForm);
			hlHeader.addComponent(btnImport);
			hlHeader.addComponent(btnExport);
		}

		if (SecurityUtil.hasAnyRole(SysKeys.UserAuth.ROLE_ADMIN_STR, SysKeys.UserAuth.ROLE_CLERK_STR)) {
			hlHeader.addComponent(btnStart);
		}
		
		if (SecurityUtil.hasUserRole()) {
			hlHeader.addComponent(btnViewDiagram);
		}

		crudGridComponent.setSelectionConsumer(x -> {

			this.selectedProcDef = x;

			final boolean b = (x != null);
			btnEdit.setEnabled(b);
			btnViewDiagram.setEnabled(b);
			btnStart.setEnabled(b);
			btnDemoPublicForm.setEnabled(b);
			btnExport.setEnabled(b);

		});

	}

}