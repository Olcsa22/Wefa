package hu.lanoga.toolbox.vaadin.component.file;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;

import com.teamunify.i18n.I;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.FormLayout;

import hu.lanoga.toolbox.ToolboxSysKeys.FileOperationAccessTypeIntent;
import hu.lanoga.toolbox.codestore.CodeStoreItemService;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.vaadin.component.crud.CrudFormElementCollection;

/**
 * olyan {@link FileManagerComponent}, amit {@link CrudFormElementCollection}-ben lehet használni
 * 
 * @see FileManagerSelectField
 * @see FileManagerField
 * @see FileManagerSelectMultiTabField
 */
public class FileManagerMeta3SeparatedField extends CustomField<String> {

	private String value;
	private final FileManagerComponentBuilder fileManagerComponentBuilder;

	private final CodeStoreItemService codeStoreItemService;
	private final FileStoreService fileStoreService;

	private final LinkedHashSet<Integer> codeStoreIdsForMeta3;

	private FormLayout flInner;
	private final List<FileManagerField> fmfList;
	
	private final ValueChangeListener<String> transitiveValueChangeListener;
	
	private final Integer codeStoreTypeIdForMeta3;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public FileManagerMeta3SeparatedField(final String caption, final FileManagerComponentBuilder fileManagerComponentBuilder, final LinkedHashSet<Integer> codeStoreIdsForMeta3) {

		super();

		this.setCaption(caption);

		this.fileManagerComponentBuilder = fileManagerComponentBuilder;
		this.codeStoreTypeIdForMeta3 = this.fileManagerComponentBuilder.getCodeStoreTypeIdForMeta3();
		this.fileManagerComponentBuilder.setCodeStoreTypeIdForMeta3(null); // azért kell null-ra tenni, mert egyébként a FileManagerComponent-ben megjelenne a "másik fajta" meta3 kiválaztó

		this.codeStoreIdsForMeta3 = codeStoreIdsForMeta3;

		this.codeStoreItemService = ApplicationContextHelper.getBean(CodeStoreItemService.class);
		this.fileStoreService = ApplicationContextHelper.getBean(FileStoreService.class);

		this.fmfList = new ArrayList<>();

		this.transitiveValueChangeListener = y -> {

			final String oldValue = this.value;

			{
				final JSONArray sumJsonArray = new JSONArray();

				for (final FileManagerField fmf : this.fmfList) {

					final String v = fmf.getValue();

					if (StringUtils.isNotBlank(v)) {

						final JSONArray meta3PartJsonArray = new JSONArray(v);

						for (int i = 0; i < meta3PartJsonArray.length(); ++i) {

							final int fileDescId = meta3PartJsonArray.getInt(i);

							sumJsonArray.put(fileDescId);

							if (fmf.getData() != null) {
								this.fileStoreService.saveMeta3(fileDescId, this.codeStoreTypeIdForMeta3, (Integer) fmf.getData());
							}

						}

					}
				}

				this.value = sumJsonArray.toString();
			}

			final Collection<?> listeners = this.getListeners(ValueChangeEvent.class);
			for (final Object listener : listeners) {

				// ez szükséges a CustomField implementációkban, ha nem triggerelünk egy ValueChangeEvent-et, akkor a getValue() sem fog meghívódni (a binder-ből)
				// (értsd: ezzel meg kell "jelölni", hogy változott és csak akkor mozdul rá a binder save-nél)

				((ValueChangeListener) listener).valueChange(new ValueChangeEvent<>(this, oldValue, true));
			}

		};
	}

	@Override
	protected Component initContent() {

		this.flInner = new FormLayout();
		this.flInner.setMargin(false);

		// ---

		this.fmfList.clear();

		// ---

		List<FileDescriptor> fullListOfInitialFiles;

		if (this.value != null) {

			final List<FileDescriptor> list = new ArrayList<>();
			final JSONArray jsonArray = new JSONArray(this.value);

			for (int i = 0; i < jsonArray.length(); i++) {
				final int fileDescriptorId = jsonArray.getInt(i);
				final FileDescriptor fd = this.fileStoreService.getFile2(fileDescriptorId, FileOperationAccessTypeIntent.READ_ONLY_INTENT); // itt még az (READ_ONLY_INTENT), amikor a FileManager komponenst tartalmazó ablakok látszanak ott már nem mindig
				list.add(fd);
			}

			fullListOfInitialFiles = list;
		} else {
			fullListOfInitialFiles = new ArrayList<>();
		}

		// ---

		for (final Integer codeStoreIdForMeta3 : this.codeStoreIdsForMeta3) {
			this.addFmf(fullListOfInitialFiles, codeStoreIdForMeta3);
		}

		this.addFmf(fullListOfInitialFiles, null);

		// ---

		this.flInner.addAttachListener(x -> {

			if (!this.isEnabled() || this.isReadOnly()) {

				this.setEnabled(true);
				this.setReadOnly(true);

				this.setCaption("<span style=\"opacity:0.5\">" + this.getCaption() + "</span>");
				this.setCaptionAsHtml(true);
			}

		});

		return this.flInner;
	}

	private void addFmf(final List<FileDescriptor> fullListOfInitialFiles, final Integer codeStoreIdForMeta3) {

		final JSONArray valueForTheFmfItem = new JSONArray();

		for (final FileDescriptor fd : fullListOfInitialFiles) {
		
			if (codeStoreIdForMeta3 != null) {
				
				if (codeStoreIdForMeta3.equals(fd.getMeta3())) { 
					valueForTheFmfItem.put(fd.getId());
				}
				
			} else {
				
				if (fd.getMeta3() == null || !this.codeStoreIdsForMeta3.contains(fd.getMeta3())) {
					valueForTheFmfItem.put(fd.getId());
				}
			}
			
		}

		final String strCodeStoreTypeIdForMeta3CaptionCaption = (codeStoreIdForMeta3 != null) ? this.codeStoreItemService.findOne(codeStoreIdForMeta3).getCaptionCaption() : "(" + I.trc("Caption", "not categorized") + ")";
		final String strCodeStoreTypeIdForMeta3CaptionCaptionShortend = StringUtils.abbreviate(strCodeStoreTypeIdForMeta3CaptionCaption, 25);

		final FileManagerField fmf = new FileManagerField(strCodeStoreTypeIdForMeta3CaptionCaptionShortend, this.fileManagerComponentBuilder);
		fmf.setValue(valueForTheFmfItem.toString());

		fmf.addValueChangeListener(this.transitiveValueChangeListener);
		fmf.setData(codeStoreIdForMeta3);

		fmf.setEnabled(this.isEnabled());
		fmf.setReadOnly(this.isReadOnly());
		fmf.setVisible(this.isVisible());

		fmf.setDescription(strCodeStoreTypeIdForMeta3CaptionCaption);

		this.flInner.addComponent(fmf);
		this.fmfList.add(fmf);
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