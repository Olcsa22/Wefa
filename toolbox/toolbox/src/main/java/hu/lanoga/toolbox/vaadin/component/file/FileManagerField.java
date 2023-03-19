package hu.lanoga.toolbox.vaadin.component.file;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.teamunify.i18n.I;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.ToolboxSysKeys.FileOperationAccessTypeIntent;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.vaadin.component.crud.CrudFormElementCollection;
import hu.lanoga.toolbox.vaadin.theme.ToolboxTheme;
import hu.lanoga.toolbox.vaadin.util.UiHelper;

/**
 * olyan {@link FileManagerComponent}, amit {@link CrudFormElementCollection}-ben lehet használni
 * 
 * @see FileManagerSelectField
 * @see FileManagerMeta3SeparatedField
 * @see FileManagerSelectMultiTabField
 */
public class FileManagerField extends CustomField<String> {

	private Window dialog;
	private String value;
	private final FileManagerComponentBuilder fileManagerComponentBuilder;

	private Button btn;
	private String strBtnCaptionBase;
	
	private String btnSize;

	public FileManagerField(final String caption, final FileManagerComponentBuilder fileManagerComponentBuilder) {
		super();
		this.setCaption(caption);
		this.fileManagerComponentBuilder = fileManagerComponentBuilder;
	}

	@Override
	protected Component initContent() {

		this.btn = new Button(I.trc("Button", "Manage"));
		this.btn.addStyleName(ToolboxTheme.BUTTON_UPLOAD);
		this.btn.setIcon(VaadinIcons.FOLDER_OPEN_O);
		this.btn.setWidth("100%");
		
		if (StringUtils.isNotBlank(this.btnSize)) {
			this.btn.setHeight(this.btnSize);
		}
		
		this.btn.addClickListener(event -> {
			this.initDialog();
		});

		this.btn.addAttachListener(x -> {

			if (!this.isEnabled() || this.isReadOnly()) {

				this.btn.setCaption(I.trc("Button", "View"));

				this.setEnabled(true);
				this.setReadOnly(true);

				this.setCaption("<span style=\"opacity:0.5\">" + this.getCaption() + "</span>");
				this.setCaptionAsHtml(true);
			}

			this.strBtnCaptionBase = this.btn.getCaption();

			this.refreshButtonCounter();

		});

		return this.btn;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void initDialog() {

		String strDialogCaption;

		if (this.isEnabled() && !this.isReadOnly()) {
			strDialogCaption = I.trc("Title", "Manage files");
		} else {
			strDialogCaption = I.trc("Title", "View files");
		}

		strDialogCaption += " (" + Jsoup.clean(this.getCaption(), Safelist.none()) + ")";

		final VerticalLayout vlDialog = new VerticalLayout();

		this.dialog = new Window(strDialogCaption, vlDialog);
		this.dialog.setWidth(950, Unit.PIXELS);
		this.dialog.setModal(true);
		this.dialog.setResizable(false);
		// this.dialog.setIcon(VaadinIcons.FOLDER_OPEN); // többi dialogon sincs, egységesség végett itt sem lesz (plusz túl sok ikon zavaró)

		final FileStoreService fileStoreService = ApplicationContextHelper.getBean(FileStoreService.class);

		if (this.value != null) {

			final List<FileDescriptor> initialFiles = new ArrayList<>();
			final JSONArray jsonArray = new JSONArray(this.value);

			for (int i = 0; i < jsonArray.length(); i++) {
				final int fileDescriptorId = jsonArray.getInt(i);
				final FileDescriptor fd = fileStoreService.getFile2(fileDescriptorId, false, null, FileOperationAccessTypeIntent.READ_ONLY_INTENT);
				initialFiles.add(fd);
			}

			this.fileManagerComponentBuilder.setInitialFiles(initialFiles);
		}

		this.fileManagerComponentBuilder.setUploadEnabled(this.isEnabled() && !this.isReadOnly());

		final FileManagerComponent fmc = this.fileManagerComponentBuilder.createFileManagerComponent();
		fmc.setMargin(false);
		fmc.setHeight(400, Unit.PIXELS);
		vlDialog.addComponent(fmc);

		if (this.isEnabled() && !this.isReadOnly()) {

			final Button btnOk = new Button(I.trc("Button", "OK"));
			btnOk.setWidth("");
			btnOk.addStyleName("min-width-150px");
			btnOk.addStyleName("max-width-400px");
			btnOk.addStyleName(ValoTheme.BUTTON_FRIENDLY);
			// btnOk.addStyleName(ToolboxTheme.BUTTON_UPLOAD);

			btnOk.addClickListener(y -> {

				final String oldValue = this.value;
				this.value = fmc.getFileIds();

				this.refreshButtonCounter();

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
	public String getValue() {
		return this.value;
	}

	@SuppressWarnings("hiding")
	@Override
	protected void doSetValue(final String value) {
		this.value = value;
	}

	public void refreshButtonCounter() {

		final int c;

		if (StringUtils.isNotBlank(this.value)) {
			c = new JSONArray(this.value).length();
		} else {
			c = 0;
		}

		this.btn.setCaption(this.strBtnCaptionBase + " (" + c + ")");

	}

	public void setBtnSize(String btnSize) {
		this.btnSize = btnSize;
	}

}