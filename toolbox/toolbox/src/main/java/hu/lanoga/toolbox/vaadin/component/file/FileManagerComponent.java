package hu.lanoga.toolbox.vaadin.component.file;

import java.io.File;
import java.text.Collator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.vaadin.dialogs.ConfirmDialog;

import com.teamunify.i18n.I;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.SerializableComparator;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PopupView;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.components.grid.HeaderRow;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.ToolboxSysKeys.FileDescriptorLocationType;
import hu.lanoga.toolbox.ToolboxSysKeys.FileDescriptorSecurityType;
import hu.lanoga.toolbox.ToolboxSysKeys.FileOperationAccessTypeIntent;
import hu.lanoga.toolbox.codestore.CodeStoreItem;
import hu.lanoga.toolbox.codestore.CodeStoreItemService;
import hu.lanoga.toolbox.exception.ManualValidationException;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreController;
import hu.lanoga.toolbox.file.FileStoreHelper;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.session.FileCartSessionBean;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.user.User;
import hu.lanoga.toolbox.user.UserService;
import hu.lanoga.toolbox.util.ToolboxAssert;
import hu.lanoga.toolbox.util.ToolboxMimeTypeHelper;
import hu.lanoga.toolbox.util.ToolboxStringUtil;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Fájlok kezelése...
 *
 * @see FileUploadComponent
 */
@Slf4j
@Getter
public class FileManagerComponent extends GridLayout implements FileUploadSuccessListener {
	
	@Getter
	public static class ServerFileCheckActionDescriptor {
		
		private final String exts;
		private final boolean extsIsBlackList;
		private final boolean emptyExtCountsAsOk;
		private final String mimeTypes;
		private final boolean mimeTypesIsBlackList;
		
		/**
		 * @param exts
		 * @param extsIsBlackList
		 * @param emptyExtCountsAsOk
		 * @param mimeTypes
		 * @param mimeTypesIsBlackList
		 * 
		 * @see ToolboxMimeTypeHelper
		 */
		public ServerFileCheckActionDescriptor(final String exts, final boolean extsIsBlackList, final boolean emptyExtCountsAsOk, final String mimeTypes, final boolean mimeTypesIsBlackList) {
			super();
			this.exts = exts;
			this.extsIsBlackList = extsIsBlackList;
			this.emptyExtCountsAsOk = emptyExtCountsAsOk;
			this.mimeTypes = mimeTypes;
			this.mimeTypesIsBlackList = mimeTypesIsBlackList;
		}
		
	}

	@Getter
	class FileItem extends HorizontalLayout {

		private FileDescriptor fileDescriptor;
		private final boolean isTemplateFile;
		private final boolean isInitialFile;
		private final String filenameForSearch;
		private final String downloadUrl;

		private CheckBox cbSelectedFile;

		public FileItem(final FileDescriptor fileDescriptor, final boolean isTemplateFile, final boolean isInitialFile) {
			super();

			// ---

			this.fileDescriptor = fileDescriptor;
			this.downloadUrl = FileStoreHelper.generateDownloadUrl(this.fileDescriptor.getId(), true);
			this.isTemplateFile = isTemplateFile;
			this.isInitialFile = isInitialFile;

			this.filenameForSearch = ToolboxStringUtil.convertToUltraSafe(fileDescriptor.getFilename(), "-");

			// ---

			this.setWidth("100%");
			this.setHeight("50px");

			this.setSpacing(true);
			this.setMargin(false);

			// ---

			this.initLayout(false);
		}

		void reInitLayout() {
			this.removeAllComponents();
			this.fileDescriptor = FileManagerComponent.this.fileStoreService.getFile2(this.fileDescriptor.getId(), false, null, FileOperationAccessTypeIntent.READ_ONLY_INTENT);
			this.initLayout(true);
		}

		private void initLayout(final boolean isReInit) {

			log.debug("FileItem: " + this.fileDescriptor.getFile() + ", " + this.downloadUrl);

			// ---

			final String filename = Jsoup.clean(this.fileDescriptor.getFilename(), Safelist.none());
			final long fileSize = this.fileDescriptor.getFile().length() / 1024;

			final Label lblFilename = new Label(FileManagerComponentUtil.getFileIcon(filename, fileSize).getHtml() + " " + StringUtils.abbreviateMiddle(filename, "...", 45));
			lblFilename.setContentMode(ContentMode.HTML);
			lblFilename.setDescription("<div><p>" + filename + "</p><p>" + fileSize + " KiB</p></div>", ContentMode.HTML);
			lblFilename.addStyleName("text-ellipsis");
			lblFilename.setWidth("95%");
			lblFilename.setEnabled(FileManagerComponent.this.isUploadEnabled && FileManagerComponent.this.isEnabled());

			if (this.isTemplateFile) {
				FileManagerComponent.this.templateFileItemMap.put(this.fileDescriptor.getId(), this);
				lblFilename.setValue(lblFilename.getValue() + " (" + I.trc("Caption", "template document") + ")");
				lblFilename.addStyleName(ValoTheme.LABEL_H4);
			} else {

				FileManagerComponent.this.fileItemMap.put(this.fileDescriptor.getId(), this);

				if (!this.isInitialFile) {
					lblFilename.setValue(lblFilename.getValue() + " (" + I.trc("Caption", "new") + ")");
					lblFilename.addStyleName(ValoTheme.LABEL_COLORED);
				}

			}

			// ---

			if (!isReInit) {
				FileManagerComponent.this.filesGridDataProvider.getItems().add(this);
				FileManagerComponent.this.filesGridDataProvider.refreshAll();
			}

			// ---

			if ((FileManagerComponent.this.isSelectionAllowed || FileManagerComponent.this.isAddToCartEnabled) && !FileManagerComponent.this.isUploadEnabled()) {

				this.cbSelectedFile = new CheckBox();
				this.cbSelectedFile.setValue(false);
				this.cbSelectedFile.addValueChangeListener(x -> {
					FileManagerComponent.this.refreshControls();
				});

				this.addComponent(this.cbSelectedFile);

			}

			// cmb3
			
			ComboBox<Integer> cmbMeta3 = null;

			if (!this.isTemplateFile && FileManagerComponent.this.codeStoreTypeIdForMeta3 != null) {

				cmbMeta3 = UiHelper.buildCombo1(null, ApplicationContextHelper.getBean(CodeStoreItemService.class).findAllByType(FileManagerComponent.this.codeStoreTypeIdForMeta3), CodeStoreItem::getCaptionCaption);
				cmbMeta3.addValueChangeListener(x -> {
					
					if (!x.isUserOriginated()) {
						return;
					}

					FileItem.this.fileDescriptor.setMeta3(x.getValue());

					// ---

					// itt kivételesen rögtön mentünk

					FileManagerComponent.this.fileStoreService.saveMeta3(this.fileDescriptor.getId(), FileManagerComponent.this.codeStoreTypeIdForMeta3, x.getValue());

				});

				cmbMeta3.setValue(this.fileDescriptor.getMeta3());
				cmbMeta3.setEnabled(FileManagerComponent.this.isUploadEnabled && FileManagerComponent.this.isEnabled());
				cmbMeta3.setWidth("240px");

				this.addComponent(cmbMeta3);
			}

			// view button

			{

				final String ext = FilenameUtils.getExtension(filename).trim().toLowerCase();

				final Button btnView = new Button();
				btnView.setWidth("50px");
				btnView.setIcon(VaadinIcons.PICTURE);
				btnView.addStyleName(ValoTheme.BUTTON_SMALL);
				this.addComponent(btnView);

				if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("pdf")) {

					// TODO: nem jó, 10000-szer letölti a képet, amíg mozog felette egy hangyányit is a cursor
					// if (ext.equals("jpg") || ext.equals("png")) {
					// final String tooltip = "<img src=\"" + viewUrl + "\" style=\"max-width: 400px; max-height: 110px;\"/>";
					// btnView.setDescription(tooltip, ContentMode.HTML);
					// }

					btnView.addClickListener(x -> {

						final Window carouselDialog = new Window(I.trc("Caption", "File carousel"));
						carouselDialog.setModal(true);
						carouselDialog.setResizable(false);
						carouselDialog.setWidth(null);

						final List<Integer> currentFileDescriptorIds = FileManagerComponent.this.getFileDescriptors().stream().flatMapToInt(fd -> IntStream.of(fd.getId())).boxed().collect(Collectors.toList());

						final VerticalLayout vlDialogContent = new VerticalLayout();
						vlDialogContent.setWidth("100%");
						vlDialogContent.setSpacing(false);

						final FileCarouselComponent fileCarouselComponent = new FileCarouselComponent(currentFileDescriptorIds, this.fileDescriptor.getId(), FileManagerComponent.this.isAddToCartEnabled, FileManagerComponent.this.isAddToCartEnabled);
						fileCarouselComponent.setWidth("912px");
						fileCarouselComponent.setHeight("735px");
						fileCarouselComponent.setMargin(false);

						vlDialogContent.addComponent(fileCarouselComponent);
						vlDialogContent.setComponentAlignment(fileCarouselComponent, Alignment.MIDDLE_CENTER);

						final HorizontalLayout hlButtonRow = new HorizontalLayout();
						hlButtonRow.setMargin(false);
						hlButtonRow.setSpacing(true);
						hlButtonRow.setHeight("36px");
						hlButtonRow.setWidth(null);

						vlDialogContent.addComponent(hlButtonRow);
						vlDialogContent.setComponentAlignment(hlButtonRow, Alignment.MIDDLE_CENTER);

						hlButtonRow.addComponent(fileCarouselComponent.getBtnInfo());
						hlButtonRow.addComponent(fileCarouselComponent.getBtnOpenOriginal());

						if (FileManagerComponent.this.isAddToCartEnabled) {
							hlButtonRow.addComponent(fileCarouselComponent.getBtnDownload()); // ez sem megy valamiért nem read-only ablakban.. (még tmp-k a fájlok? talén ez a gond?)
							hlButtonRow.addComponent(fileCarouselComponent.getBtnAddToCart());
						}

						carouselDialog.setHeight("850px");

						carouselDialog.setContent(vlDialogContent);

						FileManagerComponent.this.ui.addWindow(carouselDialog);

					});

				} else {
					btnView.setEnabled(false);
					btnView.setDescription(I.trc("Tooltip", "This file cannot be displayed in the file carousel dialog."));
				}

			}

			// download button

			Button btnDownload = null;
			Button btnEdit = null;
			if (this.isTemplateFile || (FileManagerComponent.this.isDownloadEnabled && StringUtils.isNotBlank(this.downloadUrl))) {

				btnDownload = new Button(VaadinIcons.DOWNLOAD); 
				btnDownload.setWidth("50px");
				btnDownload.addClickListener(e -> {
					FileManagerComponent.this.ui.getPage().open(this.downloadUrl, "_blank");
				});

				btnDownload.addStyleName(ValoTheme.BUTTON_SMALL);

				this.addComponent(btnDownload);

				// ---

				if (FileManagerComponent.this.isEditEnabled) {

					btnEdit = new Button(VaadinIcons.PENCIL); btnEdit.setWidth("50px");

					boolean isEditable = false;
					for (final String extension : FileManagerComponent.this.editableTypes.split(",")) {

						if (extension.equalsIgnoreCase(FilenameUtils.getExtension(this.fileDescriptor.getFilename()))) {
							isEditable = true;
							break;
						}

					}

					if (isEditable) {
						btnEdit.addClickListener(e -> {

							if (FileManagerComponent.this.downloadAltConsumer != null) {

								if (FileManagerComponent.this.lockWhenEditIsClicked) {
									FileManagerComponent.this.fileStoreService.lockFileDescriptor(this.fileDescriptor.getId());
									this.reInitLayout();
								}

								FileManagerComponent.this.downloadAltConsumer.accept(this.fileDescriptor);
							}

						});
					} else {
						btnEdit.setEnabled(false);
					}

					btnEdit.addStyleName(ValoTheme.BUTTON_SMALL);

					this.addComponent(btnEdit);
				}

			}

			// delete button

			Button btnDelete = null;
			if (!this.isTemplateFile &&
					((FileManagerComponent.this.isDeleteEnabled && FileManagerComponent.this.isUploadEnabled) || (!this.isInitialFile && FileManagerComponent.this.isUploadEnabled))) {

				btnDelete = new Button(VaadinIcons.FILE_REMOVE);
				btnDelete.setWidth("50px");
				btnDelete.addStyleName(ValoTheme.BUTTON_SMALL);

				btnDelete.addClickListener(x -> {

					if (FileManagerComponent.this.realDeleteFiles) {
						FileManagerComponent.this.fileStoreService.setToBeDeleted(this.fileDescriptor.getId());
					}

					if (FileManagerComponent.this.confirmDeleteFiles) {
						ConfirmDialog.show(UI.getCurrent(), I.trc("Caption", "Confirm"),
								I.trc("Caption", "Would you like to delete this file?"),
								I.trc("Button", "Confirm"), I.trc("Button", "Deny"), new ConfirmDialog.Listener() {

									@Override
									public void onClose(final ConfirmDialog dialog) {

										if (dialog.isConfirmed()) {
											FileManagerComponent.this.fileStoreService.checkFileLock(FileItem.this.fileDescriptor.getId());

											FileManagerComponent.this.fileItemMap.remove(FileItem.this.fileDescriptor.getId());
											FileManagerComponent.this.filesGridDataProvider.getItems().remove(FileItem.this);
											FileManagerComponent.this.filesGridDataProvider.refreshAll();
											FileManagerComponent.this.fileUploadComponent.decrementCurrentFileCount();

											FileManagerComponent.this.refreshControls();
										}

									}

								});
					} else {
						FileManagerComponent.this.fileItemMap.remove(this.fileDescriptor.getId());
						FileManagerComponent.this.filesGridDataProvider.getItems().remove(FileItem.this);
						FileManagerComponent.this.filesGridDataProvider.refreshAll();
						FileManagerComponent.this.fileUploadComponent.decrementCurrentFileCount();

						FileManagerComponent.this.refreshControls();
					}
				});

				this.addComponent(btnDelete);

			}

			// lock button

			Button btnLockUnlock = null;

			if (FileManagerComponent.this.isLockEnabled) {

				btnLockUnlock = new Button(VaadinIcons.UNLOCK); btnLockUnlock.setWidth("50px");
				btnLockUnlock.addStyleName(ValoTheme.BUTTON_SMALL);
				this.addComponent(btnLockUnlock);

				if (this.fileDescriptor.getLockedBy() != null) {

					btnLockUnlock.setIcon(VaadinIcons.LOCK);
					btnLockUnlock.setData("unlock");

					final User lockedByUser = ApplicationContextHelper.getBean(UserService.class).findOne(this.fileDescriptor.getLockedBy());

					final String lockedByStr = SecurityUtil.getLoggedInUser().getId().equals(lockedByUser.getId()) ? I.trc("Tooltip", "me") : lockedByUser.getUsername();

					btnLockUnlock.setDescription(I.trc("Tooltip", "Try to unlock") + " (" + I.trc("Tooltip", "currently locked by") + ": " + lockedByStr + ")");

					// ---

					if (!SecurityUtil.getLoggedInUser().getId().equals(lockedByUser.getId())) {
						if (btnDelete != null) {
							btnDelete.setEnabled(false);
						}

						if (btnEdit != null) {
							btnEdit.setEnabled(false);
						}
					}

				} else {
					btnLockUnlock.setIcon(VaadinIcons.UNLOCK);
					btnLockUnlock.setDescription(I.trc("Tooltip", "Make locked (currently not locked)"));
					btnLockUnlock.setData("lock");
				}

				btnLockUnlock.addClickListener(x -> {

					if ("unlock".equals(x.getButton().getData())) {
						FileManagerComponent.this.fileStoreService.unlockFileDescriptor(this.fileDescriptor.getId());
					} else if ("lock".equals(x.getButton().getData())) {
						FileManagerComponent.this.fileStoreService.lockFileDescriptor(this.fileDescriptor.getId());
					}

					this.reInitLayout();

				});

			}

			// ---

			final Button btnInfo = new Button();
			btnInfo.addStyleName(ValoTheme.BUTTON_SMALL);
			btnInfo.setWidth("50px");
			btnInfo.setDescription(I.trc("Caption", "File info"));
			btnInfo.setIcon(VaadinIcons.INFO_CIRCLE);

			final PopupView pvFileInfo = new PopupView(null, UiHelper.buildFileDescriptInfoBox(FileItem.this.fileDescriptor));
			pvFileInfo.setWidth("0px");

			pvFileInfo.setPrimaryStyleName("not-exists-dummy");

			// van egy CSS megoldásunk, amit a rendes Grid-ek fejléc sorában lévő szűrőkre kitesz egy kis filter ikont (CSS before-ral)...
			// mivel a FileManagerComp is igazából egy Grid, ezért erre is rákerül és ott éktelenkedik... ezzel ki lehet "ütni"
			// látszólag a popupview felugró részét nem rontja el ez a setPrimaryStyleName() hívás itt

			// TODO: tisztázni jobban... cactus-ban nem is volt gond, miért?

			pvFileInfo.addStyleName("hide-with-visibility");
			pvFileInfo.setHideOnMouseOut(false);

			btnInfo.addClickListener(x -> {
				pvFileInfo.setPopupVisible(true);
			});

			this.addComponent(btnInfo);

			// ---

			if (!this.isTemplateFile && !FileManagerComponent.this.isUploadEnabled && FileManagerComponent.this.isPrettyNameEnabled) {

				final TextField txtPrettyFileName = new TextField();
				txtPrettyFileName.setPlaceholder(I.trc("Caption", "(optional pretty name)"));
				txtPrettyFileName.setDescription(I.trc("Tooltip", "Optional pretty name that will be used in exports, email attachments etc."));
				txtPrettyFileName.setValueChangeMode(ValueChangeMode.BLUR);

				if (StringUtils.isNotBlank(this.fileDescriptor.getMeta1())) {
					txtPrettyFileName.setValue(this.fileDescriptor.getMeta1());
				}

				txtPrettyFileName.addValueChangeListener(x -> {

					if (x.isUserOriginated()) {
						String safeFileName;

						if (StringUtils.isNotBlank(x.getValue())) {
							safeFileName = ToolboxStringUtil.convertToUltraSafe(x.getValue().toLowerCase(), "-");
							this.fileDescriptor.setMeta1(safeFileName);
						} else {
							safeFileName = null;
							this.fileDescriptor.setMeta1(null);
						}

						if (safeFileName != null) {
							txtPrettyFileName.setValue(safeFileName);
						}

						FileManagerComponent.this.fileStoreService.saveMeta1(this.fileDescriptor.getId(), this.fileDescriptor.getMeta1());

					}
				});

				this.addComponent(txtPrettyFileName);

			}

			// ---

			this.addComponent(pvFileInfo);

			this.addComponent(lblFilename);
			this.setExpandRatio(lblFilename, 1f);

			UiHelper.alignAll(this, Alignment.MIDDLE_LEFT);

			// ---

			final boolean preCheckFailed = !FileManagerComponent.this.fileStoreService.preCheckAccessRight(this.fileDescriptor, FileOperationAccessTypeIntent.CHANGE_INTENT);

			if (preCheckFailed) {
				if (btnDelete != null) {
					btnDelete.setEnabled(false);
				}

				if (btnEdit != null) {
					btnEdit.setEnabled(false);
				}

				if (btnLockUnlock != null) {
					btnLockUnlock.setEnabled(false);
				}

				if (this.cbSelectedFile != null) {
					this.cbSelectedFile.setEnabled(false);
				}
				
				if (cmbMeta3 != null) {
					cmbMeta3.setEnabled(false);
				}
			}

		}

	}

	/**
	 * {@link FileDescriptor} id -> {@link FileItem} komponens
	 */
	private final Map<Integer, FileItem> fileItemMap;

	/**
	 * {@link FileDescriptor} id -> {@link FileItem} komponens
	 */
	private final Map<Integer, FileItem> templateFileItemMap;

	private final boolean isUploadEnabled;
	private final boolean isDeleteEnabled;
	private final boolean isDownloadEnabled;
	private final boolean isEditEnabled;
	private final String editableTypes;
	private final boolean isSelectionAllowed;
	private final boolean isPrettyNameEnabled;
	private final boolean isAddToCartEnabled;

	private final boolean isLockEnabled;
	private final boolean lockWhenEditIsClicked;

	private final Integer defaultStatus;

	private final boolean showTemplateFilled;

	private final boolean realDeleteFiles;
	private final boolean confirmDeleteFiles;

	public final int fileDescriptorLocationType;
	public final int fileDescriptorSecurityType;

	private final FileUploadComponent fileUploadComponent;

	private HorizontalLayout hl;
	private final Button btnUpload;

	private Button btnDownloadZip;
	private Button btnAddSelectedFilesToCart;
	private Button btnViewCart;
	private Button btnSelectAll;
	private Button btnDeselectAll;

	private final UI ui;

	private final FileStoreService fileStoreService;

	private final boolean allowSameFilenameMultipleTimes;

	private final Integer codeStoreTypeIdForMeta3;

	private final Grid<FileItem> filesGrid;
	private final ListDataProvider<FileItem> filesGridDataProvider;

	private Consumer<String> fileListChangeConsumer;
	private Consumer<FileDescriptor> downloadAltConsumer;

	private final TextField txtHeaderSearch;

	private final Collator collator;

	private ServerFileCheckActionDescriptor serverFileCheckActionDescriptor;

	/**
	 * @see FileManagerComponentBuilder
	 * 		itt van leírva, hogy melyik param mi
	 */
	@SuppressWarnings("unused")
	FileManagerComponent(
			final boolean isUploadEnabled, final boolean isDeleteEnabled, final boolean isDownloadEnabled, final boolean isEditEnabled, //
			final String editableTypes, //
			final String dropZoneDomId, //
			Integer maxFileCount, final String allowedMime, //
			final List<FileDescriptor> initialFiles, //
			@Deprecated final List<FileDescriptor> templateFiles, final boolean showTemplateFilled, //
			final boolean realDeleteFiles, final boolean confirmDeleteFiles, //
			final Integer fileDescriptorLocationType, final Integer fileDescriptorSecurityType, //
			final String imgTargetMime, final Integer maxPxWidth, final Integer maxPxHeight, final String resizeMethod, //
			final boolean allowSameFilenameMultipleTimes, //
			final Integer codeStoreTypeIdForMeta3, //
			final boolean isAddToCartEnabled, final boolean isSelectionAllowed, final boolean isPrettyNameEnabled, //
			final String serverFilePathPrefix, final boolean isLockEnabled, final boolean lockWhenEditIsClicked, //
			final Integer defaultStatus, //
			final ServerFileCheckActionDescriptor serverFileCheckActionDescriptor) {
		
		super();

		// ToolboxAssert.notNull(allowedMime);

		this.realDeleteFiles = realDeleteFiles;
		this.confirmDeleteFiles = confirmDeleteFiles;

		this.isUploadEnabled = isUploadEnabled;
		this.isDeleteEnabled = isDeleteEnabled;
		this.isDownloadEnabled = isDownloadEnabled;
		this.isEditEnabled = isEditEnabled;
		this.editableTypes = editableTypes;
		this.isSelectionAllowed = isSelectionAllowed;
		this.isPrettyNameEnabled = isPrettyNameEnabled;
		this.isAddToCartEnabled = (!isUploadEnabled) && isAddToCartEnabled;

		this.isLockEnabled = isLockEnabled;
		this.lockWhenEditIsClicked = lockWhenEditIsClicked;

		this.defaultStatus = defaultStatus;
		
		this.serverFileCheckActionDescriptor = serverFileCheckActionDescriptor;

		ToolboxAssert.isTrue(!(lockWhenEditIsClicked && !isLockEnabled)); // invalid kombináció

		this.showTemplateFilled = showTemplateFilled;

		this.allowSameFilenameMultipleTimes = allowSameFilenameMultipleTimes;
		this.codeStoreTypeIdForMeta3 = codeStoreTypeIdForMeta3;

		this.fileDescriptorLocationType = fileDescriptorLocationType != null ? fileDescriptorLocationType : FileDescriptorLocationType.PROTECTED_FOLDER;
		this.fileDescriptorSecurityType = fileDescriptorSecurityType != null ? fileDescriptorSecurityType : FileDescriptorSecurityType.ADMIN_OR_CREATOR;

		this.fileStoreService = ApplicationContextHelper.getBean(FileStoreService.class);
		this.ui = UI.getCurrent();

		this.fileItemMap = Collections.synchronizedMap(new LinkedHashMap<>());
		this.templateFileItemMap = Collections.synchronizedMap(new LinkedHashMap<>());

		// ---
		
		this.setSpacing(true);
		this.setMargin(true);

		this.setColumns(2);
		this.setRows(1);
		this.setSizeFull();
		this.setRowExpandRatio(0, 1f);
		this.setColumnExpandRatio(0, 1f);

		this.addStyleName("file-manager");

		// ---

		this.collator = Collator.getInstance(I18nUtil.getLoggedInUserLocale());

		// ---

		this.filesGridDataProvider = new ListDataProvider<>(new ArrayList<>());

		this.filesGrid = new Grid<>();
		this.filesGrid.setDataProvider(this.filesGridDataProvider);
		this.filesGrid.setSizeFull();
		this.filesGrid.setId("filemancomp-" + UUID.randomUUID());
		this.filesGrid.setHeaderVisible(true);
		this.filesGrid.setBodyRowHeight(50);
		this.filesGrid.setSelectionMode(SelectionMode.NONE);
		this.filesGrid.addStyleName("grid-no-outline-select");
		this.addComponent(this.filesGrid, 0, 0, 1, 0);

		this.filesGrid.addComponentColumn(c -> c).setId("fi").setComparator(new SerializableComparator<FileItem>() {

			@Override
			public int compare(final FileItem o1, final FileItem o2) {

				if (o1.isTemplateFile() && !o2.isTemplateFile()) {
					return -1;
				}

				if (!o1.isTemplateFile() && o2.isTemplateFile()) {
					return 1;
				}

				// ---

				if (o1.isInitialFile() && !o2.isInitialFile()) {
					return -1;
				}

				if (!o1.isInitialFile() && o2.isInitialFile()) {
					return 1;
				}

				// ---

				final String s1 = o1.getFileDescriptor().getFilename();
				final String s2 = o2.getFileDescriptor().getFilename();

				if (StringUtils.isAllBlank(s1, s2)) {
					return 0;
				}

				if (StringUtils.isBlank(s2)) {
					return -1;
				}

				if (StringUtils.isBlank(s1)) {
					return 1;
				}

				// ---

				final int b = FileManagerComponent.this.collator.compare(s1, s2);

				log.debug("FileItem compare: " + s1 + ", " + s2 + ", " + b);

				return b;

			}
		});

		// ---

		{

			String strHeaderText = I.trc("Caption", "Files");

			if (maxFileCount != null && isUploadEnabled) {
				strHeaderText += " (" + I.trc("Caption", "max. file count") + ": " + maxFileCount + ")";
			} else {
				maxFileCount = 99999;
			}

			final Label lblHeaderText = new Label(strHeaderText);

			this.txtHeaderSearch = new TextField();
			this.txtHeaderSearch.setWidth("200px");
			this.txtHeaderSearch.addStyleName(ValoTheme.TEXTFIELD_TINY);
			this.txtHeaderSearch.setPlaceholder(I.trc("Prompt", "Search filename"));
			this.txtHeaderSearch.setMaxLength(200);
			this.txtHeaderSearch.setValueChangeMode(ValueChangeMode.LAZY);
			this.txtHeaderSearch.setValueChangeTimeout(250);

			this.txtHeaderSearch.addValueChangeListener(x -> {
				if (x.isUserOriginated()) {

					if (StringUtils.isNotBlank(x.getValue())) {

						final String strSearch = ToolboxStringUtil.convertToUltraSafe(x.getValue(), "-");

						this.filesGridDataProvider.setFilter(new SerializablePredicate<FileManagerComponent.FileItem>() {

							@Override
							public boolean test(final FileItem t) {
								return t.getFilenameForSearch().startsWith(strSearch);
							}

						});
					} else {
						this.filesGridDataProvider.setFilter(null);
					}

					this.txtHeaderSearch.focus(); // amúgy valmiért elveszti néha a focus-t filter rárakás után...

				}
			});

			final HorizontalLayout hlHeaderRow = new HorizontalLayout();
			hlHeaderRow.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
			hlHeaderRow.addComponents(lblHeaderText, this.txtHeaderSearch);

			final HeaderRow headerRow = this.filesGrid.getHeaderRow(0);
			headerRow.getCell("fi").setComponent(hlHeaderRow);

		}

		// ---

		this.setRows(2);

		final String progDispDomId = UUID.randomUUID().toString();
		final Label lblProgress = new Label("<span id=\"" + progDispDomId + "\"></span>", ContentMode.HTML);
		lblProgress.setWidth("100%");
		lblProgress.addStyleName("text-ellipsis");
		this.addComponent(lblProgress);

		this.btnUpload = new Button(I.trc("Button", "Upload")); this.btnUpload.setIcon(VaadinIcons.UPLOAD); this.btnUpload.setId("uplbutton-" + UUID.randomUUID());
		this.btnUpload.setWidth("200px");

		this.fileUploadComponent = new FileUploadComponent((StringUtils.isBlank(dropZoneDomId) ? this.filesGrid.getId() : dropZoneDomId), this.btnUpload.getId(), progDispDomId, true, allowedMime, 0, maxFileCount, this.fileDescriptorLocationType, this.fileDescriptorSecurityType, imgTargetMime, maxPxWidth, maxPxHeight, resizeMethod, serverFilePathPrefix, defaultStatus);
		this.fileUploadComponent.addFileUploadSuccessListener(this);

		// template fájlok (ha vannak)

		if (templateFiles != null) {
			for (final FileDescriptor fileDescriptor : templateFiles) {
				new FileItem(fileDescriptor, true, false);
			}
		}

		// induló (a "rendes" / nem template) fájlok (ha vannak)

		if (isDownloadEnabled && (initialFiles != null)) {
			for (final FileDescriptor fileDescriptor : initialFiles) {
				new FileItem(fileDescriptor, false, true);
				FileManagerComponent.this.fileUploadComponent.incrementCurrentFileCount();
			}
		}

		// ---

		this.filesGrid.sort("fi", SortDirection.ASCENDING);

		// ---

		if (this.isUploadEnabled) {

			this.hl = new HorizontalLayout(this.btnUpload, this.fileUploadComponent);
			this.hl.setSpacing(false);
			this.hl.setMargin(false);
			this.addComponent(this.hl);

		} else if (this.isSelectionAllowed && this.isAddToCartEnabled) {

			// ZIP letöltés gomb
			this.btnDownloadZip = new Button(I.trc("Button", "Download as ZIP")); this.btnDownloadZip.setIcon(VaadinIcons.FILE_ZIP); this.btnDownloadZip.setWidth("250px");
			this.btnDownloadZip.addClickListener(x -> {

				final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				final FileDescriptor fiZip = FileStoreHelper.createZipFd(this.getSelectedFileDescriptors(), null, "files-" + LocalDateTime.now().format(formatter) + ".zip", FileDescriptorLocationType.PROTECTED_FOLDER, FileDescriptorSecurityType.ADMIN_OR_CREATOR, true);
				FileManagerComponent.this.ui.getPage().open(FileStoreHelper.generateDownloadUrl(fiZip.getId(), true), "_blank");

			});

			// kijelölt fájlok hozzáadása "kosárhoz"

			this.btnAddSelectedFilesToCart = new Button(I.trc("Button", "Add to file cart")); this.btnAddSelectedFilesToCart.setIcon(VaadinIcons.CART); this.btnAddSelectedFilesToCart.setWidth("250px");
			this.btnAddSelectedFilesToCart.setDescription(I.trc("Tooltip", "Add selected files to file cart"), ContentMode.TEXT);
			this.btnAddSelectedFilesToCart.addClickListener(x -> {
				for (final FileItem fileItem : this.fileItemMap.values()) {
					if (Boolean.TRUE.equals(fileItem.cbSelectedFile.getValue())) {
						ApplicationContextHelper.getBean(FileCartSessionBean.class).add(fileItem.getFileDescriptor().getId());
						Notification.show(I.trc("Notification", "File(s) successfully added to cart."));
					}
				}

				FileCartComponent.sendFileCartChangeJmsMsg(SecurityUtil.getLoggedInUser().getId());
			});

			this.btnViewCart = new Button(I.trc("Button", "View file cart")); this.btnViewCart.setIcon(VaadinIcons.CART); this.btnViewCart.setWidth("150px");
			this.btnViewCart.setDescription(I.trc("Tooltip", "View the added files to file cart"), ContentMode.TEXT);
			this.btnViewCart.addClickListener(x -> {
				final Window fileCartDialog = new Window(I.trc("Caption", "View files"));

				fileCartDialog.setWidth("650px");
				fileCartDialog.setModal(true);
				fileCartDialog.setContent(new FileCartComponent());

				UI.getCurrent().addWindow(fileCartDialog);
			});

			// összes kijelölése és kijelölés leszedése

			this.btnSelectAll = new Button(); this.btnSelectAll.setIcon(VaadinIcons.CHECK_SQUARE_O);
			this.btnSelectAll.setDescription(I.trc("Tooltip", "Select all"), ContentMode.TEXT);
			this.btnSelectAll.addClickListener(x -> {
				for (final FileItem fileItem : this.fileItemMap.values()) {
					fileItem.cbSelectedFile.setValue(true);
				}
			});

			this.btnDeselectAll = new Button(); this.btnDeselectAll.setIcon(VaadinIcons.THIN_SQUARE);
			this.btnDeselectAll.setDescription(I.trc("Tooltip", "Deselect all"), ContentMode.TEXT);
			this.btnDeselectAll.addClickListener(x -> {
				for (final FileItem fileItem : this.fileItemMap.values()) {
					fileItem.cbSelectedFile.setValue(false);
				}
			});

			this.hl = new HorizontalLayout(this.btnDownloadZip, this.btnAddSelectedFilesToCart, this.btnViewCart, this.btnSelectAll, this.btnDeselectAll);
			this.hl.setSpacing(true);
			this.hl.setMargin(false);

			this.addComponent(this.hl);

		} else if (this.isSelectionAllowed) {

			// ZIP letöltés gomb
			this.btnDownloadZip = new Button(I.trc("Button", "Download as ZIP")); this.btnDownloadZip.setIcon(VaadinIcons.FILE_ZIP); this.btnDownloadZip.setWidth("250px");
			this.btnDownloadZip.addClickListener(x -> {

				final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				final FileDescriptor fiZip = FileStoreHelper.createZipFd(this.getSelectedFileDescriptors(), null, "files-" + LocalDateTime.now().format(formatter) + ".zip", FileDescriptorLocationType.PROTECTED_FOLDER, FileDescriptorSecurityType.ADMIN_OR_CREATOR, true);
				FileManagerComponent.this.ui.getPage().open(FileStoreHelper.generateDownloadUrl(fiZip.getId(), true), "_blank");

			});

			// összes kijelölése és kijelölés leszedése
			this.btnSelectAll = new Button(); this.btnSelectAll.setIcon(VaadinIcons.CHECK_SQUARE_O);
			this.btnSelectAll.setDescription(I.trc("Tooltip", "Select all"), ContentMode.TEXT);
			this.btnSelectAll.addClickListener(x -> {
				for (final FileItem fileItem : this.fileItemMap.values()) {
					fileItem.cbSelectedFile.setValue(true);
				}
			});

			this.btnDeselectAll = new Button(); this.btnDeselectAll.setIcon(VaadinIcons.THIN_SQUARE);
			this.btnDeselectAll.setDescription(I.trc("Tooltip", "Deselect all"), ContentMode.TEXT);
			this.btnDeselectAll.addClickListener(x -> {
				for (final FileItem fileItem : this.fileItemMap.values()) {
					fileItem.cbSelectedFile.setValue(false);
				}
			});

			this.hl = new HorizontalLayout(this.btnDownloadZip, this.btnSelectAll, this.btnDeselectAll);
			this.hl.setSpacing(true);
			this.hl.setMargin(false);

			this.addComponent(this.hl);
		}

		// ---

		FileManagerComponent.this.refreshControls();
	}

	public boolean hasOngoingUpload() {

		if (this.fileUploadComponent != null && this.fileUploadComponent.getUploadId() != null) {
			if (FileStoreController.uploadVaadinInProgressUploadIds.getIfPresent(this.fileUploadComponent.getUploadId()) != null) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @param fileListChangeConsumer
	 * 		refreshControls()-kor lesz meghívva, a {@link #getFileIds()}-t fogja megkapni {@code String}-ként...
	 */
	public void setFileListChangeConsumer(final Consumer<String> fileListChangeConsumer) {
		this.fileListChangeConsumer = fileListChangeConsumer;
	}

	private void refreshControls() {

		// feltöltés gomb (enabled, disabled stb.)

		if (this.isUploadEnabled) {

			if (FileManagerComponent.this.fileUploadComponent.getRemainingFileCount() < 1) {

				FileManagerComponent.this.btnUpload.setEnabled(false);
				FileManagerComponent.this.btnUpload.setCaption(I.trc("Button", "Upload") + " (" + I.trc("Caption", "full") + ")");
				FileManagerComponent.this.btnUpload.addStyleNames("avoid-clicks");

			} else {

				FileManagerComponent.this.btnUpload.setEnabled(true);
				FileManagerComponent.this.btnUpload.setCaption(I.trc("Button", "Upload"));
				FileManagerComponent.this.btnUpload.removeStyleName("avoid-clicks");

			}
		}

		// ZIP-ben letöltés gomb

		if (this.btnDownloadZip != null) {
			FileManagerComponent.this.btnDownloadZip.setVisible(!this.isUploadEnabled && this.isDownloadEnabled && this.isAnyFileSelected());
		}

		// check all checkbox

		if (this.btnSelectAll != null && this.btnDeselectAll != null) {

			final boolean b = !this.isUploadEnabled && (this.fileItemMap.size() > 0);

			FileManagerComponent.this.btnSelectAll.setVisible(b);
			FileManagerComponent.this.btnDeselectAll.setVisible(b);

		}

		// add to cart gomb

		if (this.btnAddSelectedFilesToCart != null) {
			FileManagerComponent.this.btnAddSelectedFilesToCart.setVisible(!this.isUploadEnabled && this.isAddToCartEnabled && this.isAnyFileSelected());
		}

		// template fájlok, számláló (van-e a template-tel azonos nevű feltöltött fájl, ha van hány darab) stb.

		if (this.showTemplateFilled && (FileManagerComponent.this.templateFileItemMap != null)) {

			for (final FileItem templateFileItem : FileManagerComponent.this.templateFileItemMap.values()) {

				final String cssClass = "lbl-check-template-" + templateFileItem.fileDescriptor.getId(); // ez csak azért kell, hogy tudjuk melyik label az, ami a számlálót tartalmazza (változásnál ez alapján tudjuk a régit levenni)

				int filledTemplateCount = 0;

				for (final FileItem fileItem : FileManagerComponent.this.fileItemMap.values()) {
					if (fileItem.fileDescriptor.getFilename().equals(templateFileItem.fileDescriptor.getFilename())) {
						filledTemplateCount++;
					}
				}

				final Iterator<Component> iterator = templateFileItem.iterator();

				while (iterator.hasNext()) {

					final Component component = iterator.next();

					if (component.getStyleName().contains(cssClass)) {
						templateFileItem.removeComponent(component);
						break;
					}

				}

				if (filledTemplateCount > 0) {

					final Button lblTemplateCheckCounter = new Button();
					lblTemplateCheckCounter.setWidth("");
					lblTemplateCheckCounter.setStyleName(ValoTheme.BUTTON_BORDERLESS);
					lblTemplateCheckCounter.setEnabled(false);
					lblTemplateCheckCounter.addStyleName(cssClass);
					lblTemplateCheckCounter.setIcon(VaadinIcons.CHECK);

					if (filledTemplateCount > 1) {
						lblTemplateCheckCounter.setCaption("" + filledTemplateCount);
					}

					templateFileItem.addComponent(lblTemplateCheckCounter, 1);
					templateFileItem.setComponentAlignment(lblTemplateCheckCounter, Alignment.MIDDLE_CENTER);
				}
			}
		}

		// ---

		if (this.fileListChangeConsumer != null) {
			this.fileListChangeConsumer.accept(this.getFileIds());
		}
	}

	/**
	 * van-e (legalább egy) kijelölt (checkbox) fájl
	 *
	 * @return
	 */
	private boolean isAnyFileSelected() {

		for (final FileItem fileItem : this.fileItemMap.values()) {
			if (fileItem.cbSelectedFile != null && Boolean.TRUE.equals(fileItem.cbSelectedFile.getValue())) {
				return true;
			}
		}

		return false;
	}

	private List<FileDescriptor> getSelectedFileDescriptors() {

		final List<FileDescriptor> fileDescriptors = new ArrayList<>();

		for (final FileItem fileItem : this.fileItemMap.values()) {
			if (fileItem.cbSelectedFile != null && Boolean.TRUE.equals(fileItem.cbSelectedFile.getValue())) {
				fileDescriptors.add(fileItem.fileDescriptor);
			}
		}

		return fileDescriptors;
	}

	@SuppressWarnings("unused")
	@Override
	public void onFinish(final FileUploadSuccessEvent event) {

		this.ui.access(() -> {

			log.debug("File upload finished (FileUploadSuccessEvent listener): " + event.tmpFileDescriptor);

			// ---
			
			if (!this.allowSameFilenameMultipleTimes && this.hasFilename(event.tmpFileDescriptor.getFilename())) {

				this.fileUploadComponent.decrementCurrentFileCount();
				
				throw new ManualValidationException("Duplicate filename!", 
						I.trc("Notification", "The file was not added, because it's filename was a duplicate") + ": " + event.tmpFileDescriptor.getFilename());

			}
			
			// ---
			
			if (this.serverFileCheckActionDescriptor != null) {
				
				this.fileUploadComponent.decrementCurrentFileCount();
				
				final boolean extAndMimeTypeCheck = ToolboxMimeTypeHelper.extAndMimeTypeCheck(event.tmpFileDescriptor.getFile(), 
						serverFileCheckActionDescriptor.exts, 
						serverFileCheckActionDescriptor.extsIsBlackList, 
						serverFileCheckActionDescriptor.emptyExtCountsAsOk, 
						serverFileCheckActionDescriptor.mimeTypes, 
						serverFileCheckActionDescriptor.mimeTypesIsBlackList);
				
				if (!extAndMimeTypeCheck) {
					throw new ManualValidationException("ExtAndMimeTypeCheck failed!", I.trc("Error", "File type is not allowed!"));
				}
				
			}
			
			// ---

			new FileItem(event.tmpFileDescriptor, false, false);

			FileManagerComponent.this.refreshControls();

			// ---

		});

	}

	public boolean hasFilename(final String filenameToCheck) {

		for (final FileItem fi : this.fileItemMap.values()) {

			if (filenameToCheck.equalsIgnoreCase(fi.fileDescriptor.getFilename())) {
				return true;
			}

		}

		return false;
	}

	/**
	 * aktuális állapot szerinti lista
	 *
	 * @return
	 */
	public List<FileDescriptor> getFileDescriptors() {

		final List<FileDescriptor> fileDescriptors = new ArrayList<>();

		for (final FileItem fi : this.fileItemMap.values()) {
			fileDescriptors.add(fi.fileDescriptor);
		}

		return fileDescriptors;
	}

	public void reInitFileItem(final int fileDescriptorId) {

		final FileItem fi = this.fileItemMap.get(fileDescriptorId);

		if (fi != null) {
			fi.reInitLayout();
		}

	}

	/**
	 * ugyanaz, mint a {@link #getFileDescriptors()}, csak JSON az output...
	 *
	 * @return
	 */
	public String getFileIds() {

		final List<FileDescriptor> files = this.getFileDescriptors();

		final JSONArray jsonArray = new JSONArray();
		for (final FileDescriptor file : files) {
			jsonArray.put(file.getId());
		}

		return jsonArray.toString();

	}

	/**
	 * aktuális állapot szerinti lista
	 *
	 * @return
	 */
	public List<File> getFiles() {

		final List<File> files = new ArrayList<>();

		for (final FileItem fi : this.fileItemMap.values()) {
			files.add(fi.fileDescriptor.getFile());
		}

		return files;
	}

	/**
	 * minden template-hez van-e legalább egy felöltött azonos nevű fájl elem
	 * (amennyiben nincsenek tempalte fájlok, akkor mindig true-t ad vissza)
	 *
	 * @return
	 */
	public boolean isAllTemplateFilled() {

		if (FileManagerComponent.this.templateFileItemMap != null) {

			for (final FileItem templateFileItem : FileManagerComponent.this.templateFileItemMap.values()) {

				int uploadedTemplateFileCount = 0;

				for (final FileItem fileItem : FileManagerComponent.this.fileItemMap.values()) {
					if (fileItem.fileDescriptor.getFilename().equals(templateFileItem.fileDescriptor.getFilename())) {
						uploadedTemplateFileCount++;
					}
				}

				if (uploadedTemplateFileCount < 1) {
					return false;
				}

			}
		}

		return true;
	}

	public void setDownloadAltConsumer(final Consumer<FileDescriptor> downloadAltConsumer) {
		this.downloadAltConsumer = downloadAltConsumer;
	}

}
