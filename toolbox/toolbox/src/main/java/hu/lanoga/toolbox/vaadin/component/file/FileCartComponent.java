package hu.lanoga.toolbox.vaadin.component.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.teamunify.i18n.I;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.FileOperationAccessTypeIntent;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.session.FileCartSessionBean;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.JmsManager;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.vaadin.util.UiHelper;

public class FileCartComponent extends VerticalLayout {

	public static void sendFileCartChangeJmsMsg(final int userId) {
		final HashMap<String, Object> hm = new HashMap<>();
		hm.put("type", ToolboxSysKeys.JmsMsgType.FILE_CART_MSG.name());
		JmsManager.send(JmsManager.buildDestStr(ToolboxSysKeys.JmsDestinationMode.USER, Integer.toString(userId), "notification"), hm);
	}

	private FileCartSessionBean fileCartSessionBean;

	private FileManagerComponent fileManagerComponent;

	public FileCartComponent() {
		this.init();
	}

	public void init() {

		this.removeAllComponents();

		// ---

		this.fileCartSessionBean = ApplicationContextHelper.getBean(FileCartSessionBean.class);

		final List<FileDescriptor> initialFileDescriptors = new ArrayList<>();
		for (final Integer fileId : this.fileCartSessionBean.getAll()) {
			initialFileDescriptors.add(ApplicationContextHelper.getBean(FileStoreService.class).getFile2(fileId, FileOperationAccessTypeIntent.READ_ONLY_INTENT));
		}

		final Label lblInfo = new Label(VaadinIcons.INFO_CIRCLE.getHtml() + " " + I.trc("Caption", "Information about this component: <br>You can use the file cart to gather various files from the system, group them together, <br>and use them for email attachments, exports, reports, etc."), ContentMode.HTML);
		lblInfo.setWidth(null);
		// lblInfo.addStyleName("wrap-text"); // inkább ez legyen <br> helyett
		this.addComponent(lblInfo);

		this.setComponentAlignment(lblInfo, Alignment.MIDDLE_CENTER);

		if (initialFileDescriptors.isEmpty()) {
	
			final Label lblEmpty = new Label(I.trc("Caption", "File cart is empty!"));
			this.addComponent(new VerticalLayout(lblEmpty));
	
		} else {
			FileManagerComponentBuilder fileManagerComponentBuilder = new FileManagerComponentBuilder()
					.setMaxFileCount(999)
					.setFileDescriptorSecurityType(ToolboxSysKeys.FileDescriptorSecurityType.AUTHENTICATED_USER)
					.setAllowedMime("image/png,image/jpeg,application/pdf")
					.setFileDescriptorSecurityType(ToolboxSysKeys.FileDescriptorSecurityType.AUTHENTICATED_USER)
					.setIsSelectionAllowed(true)
					.setAddToCartEnabled(false)
					.setUploadEnabled(false)
					.setIsPrettyNameEnabled(true);

			fileManagerComponentBuilder.setInitialFiles(initialFileDescriptors);

			this.fileManagerComponent = fileManagerComponentBuilder.createFileManagerComponent();
			this.fileManagerComponent.getFilesGrid().getHeaderRow(0).getCell("fi").setText(I.trc("Caption", "Currently in the file cart"));

			this.addComponent(fileManagerComponent);

		}

		final HorizontalLayout hlButtonRow = new HorizontalLayout();
		hlButtonRow.setMargin(new MarginInfo(false, false, false, true));

		final Button btnRemove = new Button(I.trc("Button", "Remove selected file(s) from cart"));
		btnRemove.setDescription(I.trc("Caption", "Removes the file(s) from the cart (note: the files will be removed only from the cart)"));
		btnRemove.setStyleName(ValoTheme.BUTTON_FRIENDLY);
		btnRemove.setIcon(VaadinIcons.MINUS_CIRCLE_O);
		btnRemove.addClickListener(x -> {

			if (this.fileManagerComponent == null) {
				Notification.show(I.trc("Notification", "The cart is empty"));
			} else {

				final Map<Integer, FileManagerComponent.FileItem> fileItemMap = this.fileManagerComponent.getFileItemMap();

				boolean hasSelectedFile = false;

				for (Map.Entry<Integer, FileManagerComponent.FileItem> integerFileItemEntry : fileItemMap.entrySet()) {
					if (Boolean.TRUE.equals(integerFileItemEntry.getValue().getCbSelectedFile().getValue())) {
						this.fileCartSessionBean.remove(integerFileItemEntry.getValue().getFileDescriptor().getId());
						hasSelectedFile = true;
					}
				}

				if (!hasSelectedFile) {
					Notification.show(I.trc("Notification", "No file(s) selected"));
				}

				sendFileCartChangeJmsMsg(SecurityUtil.getLoggedInUser().getId());
				this.init();

			}
		});

		hlButtonRow.addComponent(btnRemove);

		final Button btnEmpty = new Button(I.trc("Button", "Empty cart"));
		btnEmpty.setDescription(I.trc("Caption", "Removes all files from the cart (note: the files will be removed only from the cart)"));
		btnEmpty.setStyleName(ValoTheme.BUTTON_FRIENDLY);
		btnEmpty.setIcon(VaadinIcons.TRASH);
		btnEmpty.addClickListener(x -> {
			
			this.fileCartSessionBean.clear();

			if (this.fileManagerComponent == null) {
			
				Notification.show(I.trc("Notification", "The cart is empty"));
		
			} else {

				if (this.fileManagerComponent != null && this.fileManagerComponent.getFileItemMap() != null) {
					this.fileManagerComponent.getFileItemMap().clear();
				}

				this.init();

				sendFileCartChangeJmsMsg(SecurityUtil.getLoggedInUser().getId());

				UiHelper.closeParentWindow(this);
			}
		});

		hlButtonRow.addComponent(btnEmpty);

		this.addComponent(hlButtonRow);

	}

	public int getFileCartSize() {
		return this.fileCartSessionBean.getFileIds().size();
	}

}