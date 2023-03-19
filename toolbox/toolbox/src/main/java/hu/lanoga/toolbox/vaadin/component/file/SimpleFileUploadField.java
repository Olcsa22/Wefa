package hu.lanoga.toolbox.vaadin.component.file;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.teamunify.i18n.I;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.VerticalLayout;

import hu.lanoga.toolbox.exception.ManualValidationException;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.util.ToolboxAssert;
import hu.lanoga.toolbox.util.ToolboxMimeTypeHelper;
import hu.lanoga.toolbox.vaadin.component.file.FileManagerComponent.ServerFileCheckActionDescriptor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link Upload} alapú egyszerűbb, alternatív file komponens...
 */
@Slf4j
public class SimpleFileUploadField extends CustomField<String> {

	private String value;

	private final FileStoreService fileStoreService;

	private final TreeSet<Integer> fileIds;

	private FileManagerComponentBuilder fileManagerComponentBuilder;
	private ServerFileCheckActionDescriptor serverFileCheckActionDescriptor;

	private int activeUploadComponents = 1;
	private Button btnAdd;

	private Button btnRemove;

	/**
	 * @param caption
	 * @param fileManagerComponentBuilder 
	 * 	    nincs direkt köze a {@link FileManagerComponent}-hez,
	 *      de felhasználjuk a builder pár paraméterét itt is  (fileDescriptorSecurityType, fileDescriptorLocationType, max fájl szám)...
	 *      viszont a {@link FileManagerComponentBuilder#setServerFilePathPrefix(String)} megoldással nem kompatiblis jelenleg!
	 */
	public SimpleFileUploadField(final String caption, final FileManagerComponentBuilder fileManagerComponentBuilder) {
		super();

		this.fileManagerComponentBuilder = fileManagerComponentBuilder;
		this.serverFileCheckActionDescriptor = this.fileManagerComponentBuilder.getServerFileCheckActionDescriptor();

		ToolboxAssert.isTrue(fileManagerComponentBuilder.getMaxFileCount() > 0);

		this.fileIds = new TreeSet<>(); // TreeSet, mert az rögtön sorrendbe rendezett

		this.fileStoreService = ApplicationContextHelper.getBean(FileStoreService.class);

		this.setCaption(caption);
	}

	@Override
	protected Component initContent() {

		ToolboxAssert.isTrue(this.isEnabled() && !this.isReadOnly());
		ToolboxAssert.isTrue(StringUtils.isBlank(this.value));

		// jelenleg csak ADD crud művelet kapcsán kell, nincs előző érték, nincs read-only állapot stb. (ez jó is így)

		// if (this.value != null) {
		//
		// final JSONArray jsonArray = new JSONArray(this.value);
		//
		// for (int i = 0; i < jsonArray.length(); i++) {
		// final int fileDescriptorId = jsonArray.getInt(i);
		// fileStoreService.getFile2(fileDescriptorId); // getFile2 hívás, hogy legyen egy checkAccessRight check
		// fileIds.add(fileDescriptorId);
		// }
		//
		// }

		// ---

		// TODO: több fájl lehetőség: legyen egy plusz gomb legalul, amivel hozzá lehet adni újabb upload component sorokat
		// lehessen vissza csökkenteni is (pl.: legyen egy-egy minus vagy x gomb minden sorban)
		// vagy
		// az is jó, ha egy upload component van csak, és már feltöltött fájlok mellett van törlés gomb
		// (ez esetben nincs slot-on belüli "csere" lehetőség, de nem baj)

		final VerticalLayout vlList = new VerticalLayout();
		vlList.setMargin(false);
		vlList.setWidth("100%");

		addUploadComponentRowToList(vlList);

		btnAdd = new Button(I.trc("Button", "New file"));
		btnAdd.setIcon(VaadinIcons.PLUS);

		btnAdd.addClickListener(x -> {
			activeUploadComponents++;
			addUploadComponentRowToList(vlList);
		});

		vlList.addComponent(btnAdd);

		// TODO: max fájlméret jobb megadása?

		return vlList;

	}

	private void addUploadComponentRowToList(final VerticalLayout vlList) {

		final HorizontalLayout hlRow = new HorizontalLayout();
		hlRow.setDefaultComponentAlignment(Alignment.BOTTOM_LEFT);
		hlRow.setMargin(false);
		hlRow.setWidth("100%");

		final List<Integer> uploadedFiles = new ArrayList<>();

		final Upload uploadComponent = new Upload(I.trc("Caption", "Upload file"), null); // a receiver azért van utána beállítva nem itt, mert különben nem tudunk módosítani a komponenesen, mert még nincs inicializálva
		uploadComponent.setReceiver(new Receiver() {

			@Override
			public OutputStream receiveUpload(final String filename, final String mimeType) {

				log.debug("receiveUpload: " + filename);

				FileDescriptor tmpFile = fileStoreService.createTmpFile2(filename, fileManagerComponentBuilder.getFileDescriptorLocationType(), fileManagerComponentBuilder.getFileDescriptorSecurityType());

				uploadComponent.setData(tmpFile);

				fileIds.add(tmpFile.getId());

				// ha nem üres a lista, az azt jelenti hogy töltöttek már fel fájlt ebben a komponensben
				// ebben az esetben kitöröljük a korábbi fájlokat
				if (!uploadedFiles.isEmpty()) {
					fileIds.removeAll(uploadedFiles);
					for (Integer k : uploadedFiles) {
						fileStoreService.setToBeDeletedInner(k, false);
					}
				}

				uploadedFiles.add(tmpFile.getId());
				uploadComponent.setButtonCaption(filename);

				doValueChangeEventCall();

				try {
					return new BufferedOutputStream(new FileOutputStream(tmpFile.getFile()));
				} catch (FileNotFoundException e) {
					throw new ToolboxGeneralException(e);
				}
			}

		});

		uploadComponent.setImmediateMode(true);
		uploadComponent.setButtonCaption(I.trc("Caption", "Select file")); // TODO: finomítani még a kinézetet
		uploadComponent.setIcon(VaadinIcons.UPLOAD);
		uploadComponent.setWidth("100%");
		uploadComponent.addStartedListener(x -> {

			log.debug("addStartedListener: " + x.getContentLength());

			if (x.getContentLength() > 200L * 1024L * 1024L) {

				UI.getCurrent().showNotification(I.trc("Notification", "File is too big!"), Notification.TYPE_WARNING_MESSAGE);
				btnRemove.click();

				throw new ManualValidationException("File is too big!", I.trc("Notification", "File is too big!"));
			}

		});

		uploadComponent.addSucceededListener(x -> {

			if (this.serverFileCheckActionDescriptor != null) {

				final boolean extAndMimeTypeCheck = ToolboxMimeTypeHelper.extAndMimeTypeCheck(
						((FileDescriptor) uploadComponent.getData()).getFile(),
						serverFileCheckActionDescriptor.getExts(),
						serverFileCheckActionDescriptor.isExtsIsBlackList(),
						serverFileCheckActionDescriptor.isEmptyExtCountsAsOk(),
						serverFileCheckActionDescriptor.getMimeTypes(),
						serverFileCheckActionDescriptor.isMimeTypesIsBlackList());

				if (!extAndMimeTypeCheck) {

					UI.getCurrent().showNotification(I.trc("Error", "File type is not allowed!"), Notification.TYPE_WARNING_MESSAGE);
					btnRemove.click();
					
					throw new ManualValidationException("ExtAndMimeTypeCheck failed!", I.trc("Error", "File type is not allowed!"));
				}

			}

			log.debug("addSucceededListener: " + x.getFilename());

			UI.getCurrent().showNotification(I.trc("Notification", "Upload finished") + ": " + x.getFilename(), Notification.TYPE_TRAY_NOTIFICATION);
		});

		hlRow.addComponent(uploadComponent);

		btnRemove = new Button();
		btnRemove.setIcon(VaadinIcons.MINUS);
		btnRemove.addClickListener(y -> {
			
			for (Integer k : uploadedFiles) {
				fileStoreService.setToBeDeletedInner(k, false);
			}
			fileIds.removeAll(uploadedFiles);

			activeUploadComponents--;

			vlList.removeComponent(hlRow);

			if (activeUploadComponents < fileManagerComponentBuilder.getMaxFileCount()) {
				vlList.addComponent(btnAdd);
			}

		});

		hlRow.addComponent(btnRemove);

		hlRow.setExpandRatio(uploadComponent, 0.9f);
		hlRow.setExpandRatio(btnRemove, 0.1f);

		vlList.addComponent(hlRow);

		// ---

		// kitörli a régi btnAdd-ot és újra hozzá adja a lista aljához
		// abban az esetben, ha elértük a maximális fájl számot nem engedünk többet hozzá adni
		if (btnAdd != null) {
			vlList.removeComponent(btnAdd);
			if (activeUploadComponents < fileManagerComponentBuilder.getMaxFileCount()) {
				vlList.addComponent(btnAdd);
			}
		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void doValueChangeEventCall() {

		final String oldValue = this.value;

		JsonArray ja = new JsonArray();
		for (Integer f : fileIds) {
			ja.add(f);
		}

		this.value = ja.toString();

		final Collection<?> listeners = this.getListeners(ValueChangeEvent.class);
		for (final Object listener : listeners) {

			// ez szükséges a CustomField implementációkban, ha nem triggerelünk egy ValueChangeEvent-et, akkor a getValue() sem fog meghívódni (a binder-ből)
			// (értsd: ezzel meg kell "jelölni", hogy változott és csak akkor mozdul rá a binder save-nél)

			((ValueChangeListener) listener).valueChange(new ValueChangeEvent<>(this, oldValue, true));
		}

	}

	@Override
	public String getValue() {
		return this.value;
	}

	@SuppressWarnings("hiding")
	@Override
	protected void doSetValue(final String value) {
		this.value = value;
	}

}