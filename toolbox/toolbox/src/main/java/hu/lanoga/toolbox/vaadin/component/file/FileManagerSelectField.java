package hu.lanoga.toolbox.vaadin.component.file;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.teamunify.i18n.I;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Grid.SelectionMode;
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
public class FileManagerSelectField extends CustomField<Set<Integer>> {

	private Window dialog;

	/**
	 * ebben lesznek a kiválaszott {@link FileDescriptor} id-k
	 */
	private Set<Integer> value;

	private final FileManagerComponentBuilder fileManagerComponentBuilder;
	private final Supplier<String> initialFilesStrSuplier;

	/**
	 * @param caption
	 * @param fileManagerComponentBuilder
	 * @param initialFilesStrSuplier
	 * 		ez egy olyan String-et ad vissza, amiben a szokásos JSON formátum van ({@link FileDescriptor} id-k tömbje) (ebből lesz az initialFiles, a builder-nél megadott nem játszik)
	 */
	public FileManagerSelectField(final String caption, final FileManagerComponentBuilder fileManagerComponentBuilder, final Supplier<String> initialFilesStrSuplier) {
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

		final VerticalLayout vlDialog = new VerticalLayout();

		this.dialog = new Window(strDialogCaption, vlDialog);
		this.dialog.setWidth("900px");
		this.dialog.setModal(true);
		// this.dialog.setIcon(VaadinIcons.FOLDER_OPEN); // többi dialogon sincs, egységesség végett itt sem lesz (plusz túl sok ikon zavaró)

		{

			final FileStoreService fileStoreService = ApplicationContextHelper.getBean(FileStoreService.class);

			final List<FileDescriptor> initialFiles = new ArrayList<>();

			final JSONArray jsonArray = new JSONArray(this.initialFilesStrSuplier.get());

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
		fmc.getFilesGrid().setHeaderVisible(false);
		fmc.setHeight("400px");
		vlDialog.addComponent(fmc);

		if (this.isEnabled() && !this.isReadOnly()) {

			final Button btnOk = new Button(I.trc("Button", "OK"));
			btnOk.setWidth("");
			btnOk.addStyleName("min-width-150px");
			btnOk.addStyleName("max-width-400px");
			btnOk.addStyleName(ValoTheme.BUTTON_FRIENDLY);
			// btnOk.addStyleName(ToolboxTheme.BUTTON_UPLOAD);

			btnOk.addClickListener(y -> {

				final Set<Integer> oldValue = this.value;

				{
					final Set<Integer> v = new HashSet<>();

					for (final FileItem fileItem : fmc.getFilesGrid().getSelectedItems()) {
						v.add(fileItem.getFileDescriptor().getId());
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
	public Set<Integer> getValue() {
		return this.value;
	}

	@SuppressWarnings("hiding")
	@Override
	protected void doSetValue(final Set<Integer> value) {
		this.value = value;
	}

}