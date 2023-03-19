package hu.lanoga.toolbox.vaadin.component.file;

import java.util.List;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.FileDescriptorSecurityType;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreController;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.session.FileCartSessionBean;
import hu.lanoga.toolbox.vaadin.component.file.FileManagerComponent.ServerFileCheckActionDescriptor;
import lombok.Getter;

/**
 * részletekért lásd még a konkrét (nem builder) osztályok kommentjeit is
 * 
 * @see FileManagerComponent
 * @see FileUploadComponent
 */
@Getter
public class FileManagerComponentBuilder {

	private boolean isUploadEnabled = true;
	private boolean isDownloadEnabled = true;

	private boolean isEditEnabled = false;
	private String editableTypes = "doc,docx,xls,xlsx,png,jpg";

	private boolean isDeleteEnabled = true;
	private boolean isSelectionAllowed = true;
	private boolean isPrettyNameEnabled = false;
	private boolean isAddToCartEnabled = false;

	private boolean isLockEnabled = false;
	private boolean lockWhenEditIsClicked = false;

	private String dropZoneDomId = null;
	private Integer maxFileCount = null;
	private String allowedMime;
	private List<FileDescriptor> initialFiles;
	private List<FileDescriptor> templateFiles;
	private boolean showTemplateFilled = false;
	private boolean realDeleteFiles = false;
	private boolean confirmDeleteFiles = false;
	private Integer fileDescriptorLocationType = ToolboxSysKeys.FileDescriptorLocationType.PROTECTED_FOLDER;
	private Integer fileDescriptorSecurityType = FileDescriptorSecurityType.ADMIN_OR_CREATOR;
	private String imgTargetMime = null;
	private Integer maxPxWidth;
	private Integer maxPxHeight;
	private String resizeMethod;
	private boolean allowSameFilenameMultipleTimes;
	private Integer codeStoreTypeIdForMeta3;
	private Integer defaultStatus;

	private String serverFilePathPrefix;
	private ServerFileCheckActionDescriptor serverFileCheckActionDescriptor;
	
	/**
	 * null/blank esetén a fájlok listája (Vaadin Panel) lesz a dropzone
	 * 
	 * @param dropZoneDomId
	 * @return
	 */
	public FileManagerComponentBuilder setDropZoneDomId(final String dropZoneDomId) {
		this.dropZoneDomId = dropZoneDomId;
		return this;
	}

	/**
	 * kötelező
	 * 
	 * @param maxFileCount
	 * @return
	 */
	public FileManagerComponentBuilder setMaxFileCount(final Integer maxFileCount) {
		this.maxFileCount = maxFileCount;
		return this;
	}

	/**
	 * kötelező, vesszővel elválaszott felsorolás 
	 * (üres string esetén bármilyen fájl engedett)
	 * 
	 * @param allowedMime
	 * @return
	 */
	public FileManagerComponentBuilder setAllowedMime(final String allowedMime) {
		this.allowedMime = allowedMime;
		return this;
	}

	/**
	 * korábban már feltöltött fájlok
	 * 
	 * @param initialFiles
	 * @return
	 */
	public FileManagerComponentBuilder setInitialFiles(final List<FileDescriptor> initialFiles) {
		this.initialFiles = initialFiles;
		return this;
	}

	/**
	 * speciálás, csak letölthető fájlok, sablon docx stb.
	 * 
	 * @param templateFiles
	 * @return
	 */
	public FileManagerComponentBuilder setTemplateFiles(final List<FileDescriptor> templateFiles) {
		this.templateFiles = templateFiles;
		return this;
	}

	/**
	 * a template elem mellett jelzi, hogy a user töltött fel azonos nevű fájlt (értsd letölti a template-et, kitölti pl. Word-ben és feltölti ezt)...
	 * 
	 * @param showTemplateFilled
	 * @return
	 */
	public FileManagerComponentBuilder setShowTemplateFilled(final boolean showTemplateFilled) {
		this.showTemplateFilled = showTemplateFilled;
		return this;
	}

	/**
	 * {@link FileStoreService#setToBeDeleted(int)} hívás is legyen-e a törlés gombra nyomva (false esetén az külső komponensnek kell gondoskodnia erről)
	 * 
	 * @param realDeleteFiles
	 * @return
	 */
	public FileManagerComponentBuilder setRealDeleteFiles(final boolean realDeleteFiles) {
		this.realDeleteFiles = realDeleteFiles;
		return this;
	}

	/**
	 * törlés hívása esetén jelenjen-e meg egy confirmDialog törlés előtt
	 *
	 * @param confirmDeleteFiles
	 * @return
	 */
	public FileManagerComponentBuilder setConfirmDeleteFiles(final boolean confirmDeleteFiles) {
		this.confirmDeleteFiles = confirmDeleteFiles;
		return this;
	}

	/**
	 * null esetén default a {@link ToolboxSysKeys.FileDescriptorLocationType#PROTECTED_FOLDER},
	 * feltöltött fájlok ilyenek lesznek, 
	 * lásd {@link FileStoreController#uploadVaadin(String, org.springframework.web.multipart.MultipartHttpServletRequest, javax.servlet.http.HttpServletResponse)}, 
	 * null esetén default a {@link ToolboxSysKeys.FileDescriptorLocationType#PROTECTED_FOLDER}
	 * 
	 * @param fileDescriptorLocationType
	 * @return
	 */
	public FileManagerComponentBuilder setFileDescriptorLocationType(final Integer fileDescriptorLocationType) {
		this.fileDescriptorLocationType = fileDescriptorLocationType;
		return this;
	}

	/**
	 * null esetén default a {@link ToolboxSysKeys.FileDescriptorSecurityType#ADMIN_OR_CREATOR},
	 * feltöltött fájlok ilyenek lesznek, lásd {@link FileStoreController#uploadVaadin(String, org.springframework.web.multipart.MultipartHttpServletRequest, javax.servlet.http.HttpServletResponse)}, 
	 * null esetén default a {@link FileDescriptorSecurityType#ADMIN_OR_CREATOR}
	 * 
	 * @param fileDescriptorSecurityType
	 * @return
	 */
	public FileManagerComponentBuilder setFileDescriptorSecurityType(final Integer fileDescriptorSecurityType) {
		this.fileDescriptorSecurityType = fileDescriptorSecurityType;
		return this;
	}

	/**
	 * konvert rögtön (bitképek) (JavaScript-ben), 
	 * null esetén nincs konvert, 
	 * csak akkor működik, ha max méret is meg van adva
	 * 
	 * @param imgTargetMime
	 * @return
	 */
	public FileManagerComponentBuilder setImgTargetMime(final String imgTargetMime) {
		this.imgTargetMime = imgTargetMime;
		return this;
	}

	/**
	 * átméretezés rögtön (bitképek) (JavaScript-ben), null esetén nincs méretezés (maxPxHeight + maxPxWidth is kitöltendő, külön nem lehet)
	 * 
	 * @param maxPxWidth
	 * @return
	 */
	public FileManagerComponentBuilder setMaxPxWidth(final Integer maxPxWidth) {
		this.maxPxWidth = maxPxWidth;
		return this;
	}

	/**
	 * átméretezés rögtön (bitképek) (JavaScript-ben), null esetén nincs méretezés (maxPxHeight + maxPxWidth is kitöltendő, külön nem lehet)
	 * 
	 * @param maxPxHeight
	 * @return
	 */
	public FileManagerComponentBuilder setMaxPxHeight(final Integer maxPxHeight) {
		this.maxPxHeight = maxPxHeight;
		return this;
	}

	/**
	 * {@link #RESIZE_METHOD_CONTAIN} vagy {@link FileUploadComponent#RESIZE_METHOD_CROP} (JavaScript átm. vonatkozásában),
	 * null esetén a {@link FileUploadComponent#RESIZE_METHOD_CONTAIN} a default
	 * 
	 * @param resizeMethod
	 * @return
	 */
	public FileManagerComponentBuilder setResizeMethod(final String resizeMethod) {
		this.resizeMethod = resizeMethod;
		return this;
	}
	
	/**
	 * experimental ... 
	 * 	új, az egodoc igény hívta életre... lemegy egészen a {@link FileStoreService} szintjéig; minden feltöltött fájl valódi file system fájlneve kap ilyen prefixet 
	 * 	(lehet benne "/" jel is, ezzel alkönyvtárak is létrehozhatóak)
	 * 
	 * @param serverFilePathPrefix
	 * @return
	 */
	public FileManagerComponentBuilder setServerFilePathPrefix(final String serverFilePathPrefix) {
		this.serverFilePathPrefix = serverFilePathPrefix;
		return this;
	}

	/**
	 * régebben: ez egyben lehetővé teszi (engedélyezi) a korábban feltöltött fájlok (initialFiles) törlését is...
	 * a delete-re külön param van már!
	 * 
	 * @param isUploadEnabled
	 * @return
	 */
	public FileManagerComponentBuilder setUploadEnabled(final boolean isUploadEnabled) {
		this.isUploadEnabled = isUploadEnabled;
		return this;
	}
	
	/**
	 * lehetővé teszi a korábban feltöltött fájlok (initialFiles) törlését is... 
	 * (mj.: az amit most tölt fel frissen az, még törölhető alapban is (mentésig))
	 * 
	 * @param isDeleteEnabled
	 * @return
	 */
	public FileManagerComponentBuilder setDeleteEnabled(final boolean isDeleteEnabled) {
		this.isDeleteEnabled = isDeleteEnabled;
		return this;
	}

	/**
	 * letöltés is lehet-e
	 * 
	 * @param isDownloadEnabled
	 * @return
	 */
	public FileManagerComponentBuilder setDownloadEnabled(final boolean isDownloadEnabled) {
		this.isDownloadEnabled = isDownloadEnabled;
		return this;
	}

	/**
	 * szerkesztés is lehet-e (ha maga a letöltés nincs engedélyezve, akkor ez sem fog megjelenni)
	 *
	 * @param isEditEnabled
	 * @return
	 */
	public FileManagerComponentBuilder setEditEnabled(final boolean isEditEnabled) {
		this.isEditEnabled = isEditEnabled;
		return this;
	}

	/**
	 * engedélyezzük-e a lock funkciót
	 *
	 * @param isLockEnabled
	 * @return
	 */
	public FileManagerComponentBuilder setLockEnabled(final boolean isLockEnabled) {
		this.isLockEnabled = isLockEnabled;
		return this;
	}

	/**
	 *
	 * lockolja-e (értsd automatikusan az edittel együtt) a fileDescriptor-t, ha az edit gombra nyom a user
	 *
	 * @param lockWhenEditIsClicked
	 * @return
	 */
	public FileManagerComponentBuilder setLockWhenEditIsClicked(final boolean lockWhenEditIsClicked) {
		this.lockWhenEditIsClicked = lockWhenEditIsClicked;
		return this;
	}

	/**
	 * a típusok (ext), amiknél a szerkesztés gomb engedélyezve van
	 * az értékre példa: "doc,docx"
	 *
	 * @param editableTypes
	 * @return
	 */
	public FileManagerComponentBuilder setEditableTypes(final String editableTypes) {
		this.editableTypes = editableTypes;
		return this;
	}

	/**
	 * engedélyezi a fájlok kijelölését, és letöltését zip-ként
	 *
	 * @param isSelectionAllowed
	 * @return
	 */
	public FileManagerComponentBuilder setIsSelectionAllowed(final boolean isSelectionAllowed) {
		this.isSelectionAllowed = isSelectionAllowed;
		return this;
	}

	/**
	 * lehet-e használni a {@link FileCartSessionBean}-t 
	 * (ezzel későbbi használathoz lehet összeszedni fájlokat a teljes rendszerből)
	 *  
	 * @param addToCartEnabled
	 * @return
	 */
	public FileManagerComponentBuilder setAddToCartEnabled(final boolean addToCartEnabled) {
		this.isAddToCartEnabled = addToCartEnabled;
		return this;
	}

	/**
	 * {@link FileCartComponent} / {@link FileCartSessionBean} témakör kapcsán
	 * 
	 * @param isPrettyNameEnabled
	 * @return
	 */
	public FileManagerComponentBuilder setIsPrettyNameEnabled(final boolean isPrettyNameEnabled) {
		this.isPrettyNameEnabled = isPrettyNameEnabled;
		return this;
	}

	/**
	 * feltöltésnél legyen-e ellenőrzés (false esetén van)
	 * 
	 * @param allowSameFilenameMultipleTimes
	 * @return
	 */
	public FileManagerComponentBuilder setAllowSameFilenameMultipleTimes(final boolean allowSameFilenameMultipleTimes) {
		this.allowSameFilenameMultipleTimes = allowSameFilenameMultipleTimes;
		return this;
	}

	/**
	 * amennyiben meg van adva (nem null), akkor az egyes elemekhez feltöltésnél fel lehet vinni metaadatként ennek a codostore type-nak az elemeit
	 * 
	 * @param codeStoreTypeIdForMeta3
	 * @return
	 */
	public FileManagerComponentBuilder setCodeStoreTypeIdForMeta3(final Integer codeStoreTypeIdForMeta3) {
		this.codeStoreTypeIdForMeta3 = codeStoreTypeIdForMeta3;
		return this;
	}

	/**
	 * az a status, amiben mentésre fog kerülni a feltöltött fájl
	 *
	 * @param defaultStatus
	 * @return
	 */
	public FileManagerComponentBuilder setDefaultStatus(final Integer defaultStatus) {
		this.defaultStatus = defaultStatus;
		return this;
	}

	public Integer getCodeStoreTypeIdForMeta3() {
		return this.codeStoreTypeIdForMeta3;
	}
	

	/**
	 * null esetén nincs szerveroldali check
	 * 
	 * @param serverFileCheckActionDescriptor
	 * @return
	 */
	public FileManagerComponentBuilder setServerFileCheckActionDescriptor(final ServerFileCheckActionDescriptor serverFileCheckActionDescriptor) {
		this.serverFileCheckActionDescriptor = serverFileCheckActionDescriptor;
		return this;
	}
	
	public FileManagerComponentBuilder setUsualServerFileCheckActionDescriptor() {
		
		this.serverFileCheckActionDescriptor = new FileManagerComponent.ServerFileCheckActionDescriptor("png,jpeg,jpg,pdf,rtf,txt,doc,docx,xls,xlsx,odt,ods,odf,zip", false, false, "image/jpeg,image/png,image/bmp,audio/mpeg,audio/wav,video/mpeg,video/x-msvideo,application/pdf,application/rtf,"
				+ "text/plain,application/msword,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.wordprocessingml.document,"
				+ "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.oasis.opendocument.text,"
				+ "application/vnd.oasis.opendocument.spreadsheet,application/zip", false);
		
		return this;
		
	}

	public ServerFileCheckActionDescriptor getServerFileCheckActionDescriptor() {
		return this.serverFileCheckActionDescriptor;
	}
	
	public FileManagerComponent createFileManagerComponent() {
		return new FileManagerComponent(
				this.isUploadEnabled, this.isDeleteEnabled, this.isDownloadEnabled, this.isEditEnabled, //
				this.editableTypes, //
				this.dropZoneDomId, //
				this.maxFileCount, this.allowedMime, //
				this.initialFiles, //
				this.templateFiles, this.showTemplateFilled, // 
				this.realDeleteFiles, this.confirmDeleteFiles, //
				this.fileDescriptorLocationType, this.fileDescriptorSecurityType, // 
				this.imgTargetMime, this.maxPxWidth, this.maxPxHeight, this.resizeMethod, // 
				this.allowSameFilenameMultipleTimes, //
				this.codeStoreTypeIdForMeta3, //
				this.isAddToCartEnabled, this.isSelectionAllowed, this.isPrettyNameEnabled, // 
				this.serverFilePathPrefix, this.isLockEnabled, this.lockWhenEditIsClicked, //
				this.defaultStatus, //
				this.serverFileCheckActionDescriptor);
	}

	
}