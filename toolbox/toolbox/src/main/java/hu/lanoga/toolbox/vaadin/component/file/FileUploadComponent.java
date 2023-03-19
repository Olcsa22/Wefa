package hu.lanoga.toolbox.vaadin.component.file;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.annotations.JavaScript;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.UI;

import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.vaadin.AbstractToolboxUI;

/**
 * Fájl feltöltés (DropzoneJS-re alapul)
 *
 * @see FileManagerComponent
 */
@JavaScript({ "../../../webjars/jquery/1.12.4/jquery.min.js", "../../../webjars/dropzone/5.2.0/dist/dropzone.js", "../../../default/assets/fileupload-v1.js" })
public class FileUploadComponent extends com.vaadin.ui.AbstractJavaScriptComponent {

	public static final String RESIZE_METHOD_CONTAIN = "contain";
	public static final String RESIZE_METHOD_CROP = "crop";

	/**
	 * háttérszálas eléréshez
	 */
	private final UI ui;

	private final Set<FileUploadSuccessListener> successListeners = Collections.newSetFromMap(new ConcurrentHashMap<FileUploadSuccessListener, Boolean>());

	private final AtomicInteger currentFileCount;

	private final int fileDescriptorLocationType;
	private final int fileDescriptorSecurityType;
	private String serverFilePathPrefix;
	private Integer defaultStatus;

	/**
	 * @param areaId
	 *            drop area DOM id
	 * @param browseId
	 *            gomb DOM id
	 * @param progDispId
	 *            feltöltő animáció stb. itt legyen (DOM id)
	 * @param isMultiFileAllowed
	 *            annak beállítás, lehet-e egyszerre több fájlt (kijelölni/feltölteni)
	 * @param allowedMime
	 *            csak ilyen fájltípusok tölthetőek fel (JavaScript ellenőrzés)
	 * @param fileCountInit
	 *            a maxFileCount limitet innen "indul" (értsd olyan, mintha ennyi fájlt már feltöltött volna a user, általában 0)
	 * @param maxFileCount
	 *            max. fájl szám, ami feltölthető (1-nél kisebb esetén isMultiFileAllowed mindenképp false lesz)
	 * @param fileDescriptorLocationType
	 *            a feltöltött fájlok ilyenek leszenek (pl. CDN fájl stb.), lásd {@link FileStoreService}
	 * @param fileDescriptorSecurityType
	 *            a feltöltött fájlok ilyenek leszenek, lásd {@link FileStoreService}
	 * @param imgTargetMime
	 *            konvert rögtön (bitképek) (JavaScript-ben), null esetén nincs konvert
	 * @param maxPxWidth
	 *            átméretezés rögtön (bitképek) (JavaScript-ben), null esetén nincs méretezés (maxPxHeight + maxPxWidth is kitöltendő, külön nem lehet)
	 * @param maxPxHeight
	 *            átméretezés rögtön (bitképek) (JavaScript-ben), null esetén nincs méretezés (maxPxHeight + maxPxWidth is kitöltendő, külön nem lehet)
	 * @param resizeMethod
	 *            {@link #RESIZE_METHOD_CONTAIN} vagy {@link #RESIZE_METHOD_CROP} (JavaScript átm. vonatkozásában), null esetén a {@link #RESIZE_METHOD_CONTAIN} a default
	 * @param serverFilePathPrefix
	 * 
	 * @param defaultStatus
	 * 
	 */
	FileUploadComponent(final String areaId, final String browseId, final String progDispId, final boolean isMultiFileAllowed, final String allowedMime, final int fileCountInit, final int maxFileCount, final int fileDescriptorLocationType, final int fileDescriptorSecurityType, final String imgTargetMime, final Integer maxPxWidth, final Integer maxPxHeight, final String resizeMethod, String serverFilePathPrefix, final Integer defaultStatus) {

		super();

		this.ui = UI.getCurrent();

		this.currentFileCount = new AtomicInteger(fileCountInit);

		this.fileDescriptorLocationType = fileDescriptorLocationType;
		this.fileDescriptorSecurityType = fileDescriptorSecurityType;
		this.defaultStatus = defaultStatus;

		this.getState().uploadId = "uplcomp-" + UUID.randomUUID();
		this.setId(this.getState().uploadId);

		this.getState().contextPath = VaadinService.getCurrentRequest().getContextPath();

		this.getState().areaId = areaId;
		this.getState().browseId = browseId;
		this.getState().progDispId = progDispId;

		this.getState().maxFileCount = maxFileCount;

		this.getState().isMultiFileAllowed = isMultiFileAllowed && (maxFileCount > 1);

		this.getState().allowedMime = allowedMime;

		this.getState().imgTargetMime = imgTargetMime;
		this.getState().maxPxWidth = maxPxWidth;
		this.getState().maxPxHeight = maxPxHeight;
		this.getState().resizeMethod = resizeMethod == null ? RESIZE_METHOD_CONTAIN : resizeMethod;

		this.serverFilePathPrefix = serverFilePathPrefix;
	}

	public void addFileUploadSuccessListener(final FileUploadSuccessListener fileUploadSuccessListener) {
		this.successListeners.add(fileUploadSuccessListener);
	}

	public void removeFileUploadSuccessListener(final FileUploadSuccessListener fileUploadSuccessListener) {
		this.successListeners.remove(fileUploadSuccessListener);
	}

	@Override
	public JsFileUploadComponentState getState() {
		return (JsFileUploadComponentState) super.getState();
	}

	@Override
	public void attach() {
		super.attach();
		FileUploadInstanceHolder.addInstance(this);
	}

	/**
	 * Jelzés küldése a listener(ek)nek...
	 *
	 * @param fileDescriptor
	 */
	public void dispatchSuccessEvent(final FileDescriptor fileDescriptor) {

		final FileUploadSuccessEvent event = new FileUploadSuccessEvent(fileDescriptor);

		this.ui.access(() -> {

			FileUploadComponent.this.incrementCurrentFileCount();

			for (final FileUploadSuccessListener listener : FileUploadComponent.this.successListeners) {
				listener.onFinish(event);
			}

		});

	}

	public String getUploadId() {
		return this.getState().uploadId;
	}

	/**
	 * jelen állás szerint hány fájlt lehet még feltölteni ehhez a komponenshez
	 *
	 * @return
	 */
	public int getRemainingFileCount() {
		return this.getState().maxFileCount - this.currentFileCount.get();
	}

	public String getAllowedMime() {
		return this.getState().allowedMime;
	}

	public int incrementCurrentFileCount() {
		return this.currentFileCount.incrementAndGet();
	}

	public int decrementCurrentFileCount() {
		return this.currentFileCount.decrementAndGet();
	}

	public void showErrorMsg(final Throwable throwable) {
		this.ui.access(() -> {
			((AbstractToolboxUI) this.ui).showErrorMsg(throwable);
		});
	}

	public int getFileDescriptorLocationType() {
		return this.fileDescriptorLocationType;
	}

	public int getFileDescriptorSecurityType() {
		return this.fileDescriptorSecurityType;
	}

	public String getServerFilePathPrefix() {
		return serverFilePathPrefix;
	}

	public Integer getDefaultStatus() {
		return defaultStatus;
	}

}
