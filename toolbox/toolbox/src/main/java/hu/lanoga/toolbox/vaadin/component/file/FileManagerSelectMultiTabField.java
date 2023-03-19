package hu.lanoga.toolbox.vaadin.component.file;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.teamunify.i18n.I;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.ToolboxSysKeys.FileOperationAccessTypeIntent;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.vaadin.component.crud.CrudFormElementCollection;
import hu.lanoga.toolbox.vaadin.component.file.FileManagerComponent.FileItem;
import hu.lanoga.toolbox.vaadin.theme.ToolboxTheme;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.extern.slf4j.Slf4j;

/**
 * olyan {@link FileManagerComponent}, amit {@link CrudFormElementCollection}-ben lehet használni (fájlok kiválasztására)... 
 * eredetileg cactus-hoz készült, ha kell máshol, akkor előbb át kell nézni alaposan... 
 * 
 * @see FileManagerField
 */
@Slf4j
@Deprecated
public class FileManagerSelectMultiTabField extends CustomField<List<String>> {

	// minta:
	// final FileManagerSelectMultiTabField fmsmtf = new FileManagerSelectMultiTabField("XY", WasteUIHelper.buildFileManagerComponentBuilder(), () -> {
	// return Lists.newArrayList(Pair.of("First tab", "[1, 2]"), Pair.of("Second tab", "[5, 6]"));
	// });
	// this.addComponent(fmsmtf);

	private Window dialog;

	/**
	 * ebben lesznek a kiválaszott {@link FileDescriptor} id-k (JSON formátum van ({@link FileDescriptor} id-k tömbjeinek list-je)
	 */
	private List<String> value;

	private final FileManagerComponentBuilder fileManagerComponentBuilder;
	private final Supplier<List<Pair<String, String>>> initialFilesStrSuplier;

	/**
	 * @param caption
	 * @param fileManagerComponentBuilder
	 * @param initialFilesStrSuplier
	 * 		olyan String-et ad vissza, amiben a szokásos JSON formátum van ({@link FileDescriptor} id-k tömbje) (ebből lesz az initialFiles, a builder-nél megadott nem játszik)... 
	 * 		azért list mert több ilyet is lehet, azért {@link Pair}, mert a bal String lesz a fül cimke... 
	 */
	public FileManagerSelectMultiTabField(final String caption, final FileManagerComponentBuilder fileManagerComponentBuilder, final Supplier<List<Pair<String, String>>> initialFilesStrSuplier) {
		super();
		this.setCaption(caption);
		this.fileManagerComponentBuilder = fileManagerComponentBuilder;
		this.initialFilesStrSuplier = initialFilesStrSuplier;
	}

	@Override
	protected Component initContent() {

		final Button btn = new Button(I.trc("Button", "Select files"));
		btn.addStyleName(ToolboxTheme.BUTTON_UPLOAD);
		btn.setIcon(VaadinIcons.FOLDER_OPEN_O);
		btn.setWidth("100%");
		btn.addClickListener(event -> {
			this.initDialog();
		});

		btn.addAttachListener(x -> {

			if (!this.isEnabled() || this.isReadOnly()) {

				btn.setCaption(I.trc("Button", "View file selection"));

				this.setEnabled(true);
				this.setReadOnly(true);

				this.setCaption("<span style=\"opacity:0.5\">" + this.getCaption() + "</span>");
				this.setCaptionAsHtml(true);
			}

		});

		return btn;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void initDialog() {

		String strDialogCaption;

		if (this.isEnabled() && !this.isReadOnly()) {
			strDialogCaption = I.trc("Title", "Select files");
		} else {
			strDialogCaption = I.trc("Title", "View selected files");
		}

		strDialogCaption += " (" + Jsoup.clean(this.getCaption(), Safelist.none()) + ")";

		final TabSheet tsDialog = new TabSheet();

		final VerticalLayout vlDialog = new VerticalLayout();
		vlDialog.addComponent(tsDialog);

		this.dialog = new Window(strDialogCaption, vlDialog);
		this.dialog.setWidth("900px");
		this.dialog.setModal(true);

		final List<FileManagerComponent> fmcList = new ArrayList<>();

		for (final Pair<String, String> initialFilesPair : this.initialFilesStrSuplier.get()) {

			{

				final FileStoreService fileStoreService = ApplicationContextHelper.getBean(FileStoreService.class);

				final List<FileDescriptor> initialFiles = new ArrayList<>();

				final JSONArray jsonArray = new JSONArray(initialFilesPair.getRight());

				for (int i = 0; i < jsonArray.length(); i++) {
					final int fileDescriptorId = jsonArray.getInt(i);
					final FileDescriptor fd = fileStoreService.getFile2(fileDescriptorId, FileOperationAccessTypeIntent.READ_ONLY_INTENT);
					initialFiles.add(fd);
				}

				this.fileManagerComponentBuilder.setInitialFiles(initialFiles);

			}

			this.fileManagerComponentBuilder.setUploadEnabled(false);

			final FileManagerComponent fmc = this.fileManagerComponentBuilder.createFileManagerComponent();
			fmc.getFilesGrid().setSelectionMode(SelectionMode.MULTI);
			fmc.setHeight("400px");

			tsDialog.addTab(fmc, initialFilesPair.getLeft());

			fmcList.add(fmc);
		}

		if (this.isEnabled() && !this.isReadOnly()) {

			final Button btnOk = new Button(I.trc("Button", "OK"));
			btnOk.setWidth("");
			btnOk.addStyleName("min-width-150px");
			btnOk.addStyleName("max-width-400px");
			btnOk.addStyleName(ValoTheme.BUTTON_FRIENDLY);
			// btnOk.addStyleName(ToolboxTheme.BUTTON_UPLOAD);

			btnOk.addClickListener(y -> {

				final List<String> oldValue = this.value;

				{
					final List<String> v = new ArrayList<>();

					for (final FileManagerComponent fmc : fmcList) {

						final List<Integer> t = new ArrayList<>();

						for (final FileItem fileItem : fmc.getFilesGrid().getSelectedItems()) {
							t.add(fileItem.getFileDescriptor().getId());
						}

						v.add(new JSONArray(t).toString());

					}

					this.value = v;

					log.debug("selected files: " + this.value);
				}

				final Collection<?> listeners = this.getListeners(ValueChangeEvent.class);
				for (final Object listener : listeners) {

					// ez szükséges a CustomField implementációkban, ha nem triggerelünk egy ValueChangeEvent-et, akkor a getValue() sem fog meghívódni (a binder-ből)
					// (értsd: ezzel meg kell "jelölni", hogy változott és csak akkor mozdul rá a binder save-nél)

					((ValueChangeListener) listener).valueChange(new ValueChangeEvent<>(this, oldValue, true));
				}

				this.dialog.close();
			});

			vlDialog.addComponent(btnOk);

		}

		UI.getCurrent().addWindow(this.dialog);
		UiHelper.forceDialogFocus(this.dialog);
	}

	@Override
	public List<String> getValue() {
		return this.value;
	}

	@SuppressWarnings("hiding")
	@Override
	protected void doSetValue(final List<String> value) {
		this.value = value;
	}

}