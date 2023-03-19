package hu.lanoga.toolbox.vaadin.component.file;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.teamunify.i18n.I;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.FileOperationAccessTypeIntent;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreHelper;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.session.FileCartSessionBean;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.util.ToolboxAssert;
import hu.lanoga.toolbox.vaadin.AbstractToolboxUI;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.extern.slf4j.Slf4j;

/**
 * PDF és JPG/PNG/JPEG fájlokat fogad el,
 * paraméterben megkapott FileDescriptor.id listával működik (List, vagy JSONArray)
 * (alapja: https://github.com/alexwebgr/pdfSlider)
 */
@Slf4j
@StyleSheet({ "../../../default/assets/file-carousel/pdfSlider/pdfSlider.css", "../../../default/assets/file-carousel/carousel.css" })
@JavaScript({ "../../../webjars/jquery/1.12.4/jquery.min.js", "../../../default/assets/file-carousel/pdfSlider/pdfSlider.js" })
public class FileCarouselComponent extends VerticalLayout implements Consumer<String> {

	private final String fileCarouselUid;
	private final List<Integer> fileDescriptorIds = new ArrayList<>();

	private Integer lastShownFileDescriptorId = null;

	private Button btnOpenOriginal;
	private Button btnDownload;
	private Button btnAddToCart;

	private String jsLastShownFileDescriptorIdCallbackId;
	private Button btnInfo;

	public FileCarouselComponent(final List<Integer> fileDescriptorIds, final Integer forceAsFirst, final boolean buildDownloadButton, final boolean buildAddToCartButton) {

		ToolboxAssert.notNull(fileDescriptorIds);
		ToolboxAssert.isTrue(!fileDescriptorIds.isEmpty());

		this.fileDescriptorIds.addAll(fileDescriptorIds);

		if (forceAsFirst != null) {

			// log.debug("" + fileDescriptorIds);

			final int i = -1 * this.fileDescriptorIds.indexOf(forceAsFirst);

			// log.debug("forceAsFirst: " + forceAsFirst);
			// log.debug("i: " + i);

			Collections.rotate(this.fileDescriptorIds, i);

			// log.debug("" + fileDescriptorIds);
		}

		this.fileCarouselUid = UUID.randomUUID().toString();

		// ---

		this.btnOpenOriginal = new Button(VaadinIcons.EXPAND_FULL); this.btnOpenOriginal.setHeight("36px"); this.btnOpenOriginal.setCaption(I.trc("Button", "See original size"));
		this.btnInfo = new Button(VaadinIcons.INFO_CIRCLE); this.btnInfo.setHeight("36px"); this.btnInfo.setCaption(I.trc("Button", "File info"));

		if (buildDownloadButton) {
			this.btnDownload = new Button(VaadinIcons.DOWNLOAD); this.btnDownload.setWidth(null); this.btnDownload.setHeight("36px"); this.btnDownload.setCaption(I.trc("Button", "Download"));
		}

		if (buildAddToCartButton) {
			this.btnAddToCart = new Button(VaadinIcons.CART); this.btnAddToCart.setWidth(null); this.btnAddToCart.setHeight("36px"); this.btnAddToCart.setCaption(I.trc("Button", "Add to file cart"));
		}

	}

	/**
	 * @param fileDescriptorIds
	 * 		{@link FileDescriptor} azonosítók List-ben
	 * @param forceAsFirst
	 * 		úgy "forgatja" a listát, hogy ezzel kezdőjön (null esetén nincs forgatás)
	 */
	public FileCarouselComponent(final List<Integer> fileDescriptorIds, final Integer forceAsFirst) {
		this(fileDescriptorIds, forceAsFirst, false, false);
	}

	public Button getBtnOpenOriginal() {
		return btnOpenOriginal;
	}

	public Button getBtnInfo() {
		return btnInfo;
	}

	public Button getBtnDownload() {
		return btnDownload;
	}

	public Button getBtnAddToCart() {
		return btnAddToCart;
	}

	@Override
	public void attach() {
		super.attach();
		this.initLayout();
	}

	@Override
	public void accept(final String t) {

		if (StringUtils.isNotBlank(t)) {

			String[] split = t.split("<op>");
			String op = split[0];
			String value = split[1];

			this.lastShownFileDescriptorId = Integer.parseInt(value);

			// mj.: itt a művelet nevek azok, amikekkel lentebb meghívjuk a callback-et (tehát csak ezen a Java fájlon belül vannak használva)

			if ("showInfo".equals(op)) {

				final Window dialogFileInfo = new Window(I.trc("Caption", "File info"));
				dialogFileInfo.setModal(true);
				dialogFileInfo.setWidth(null);
				dialogFileInfo.setContent(UiHelper.buildFileDescriptInfoBox(this.lastShownFileDescriptorId));

				UI.getCurrent().addWindow(dialogFileInfo);

			} else if ("openOriginal".equals(op)) {
				UI.getCurrent().getPage().open(FileStoreHelper.generateDownloadUrl(this.lastShownFileDescriptorId, false), "_blank");
			} else if ("download".equals(op)) {
				UI.getCurrent().getPage().open(FileStoreHelper.generateDownloadUrl(this.lastShownFileDescriptorId, true), "_blank");
			} else if ("addToCart".equals(op)) {
				ApplicationContextHelper.getBean(FileCartSessionBean.class).add(this.lastShownFileDescriptorId);
				Notification.show(I.trc("Notification", "File(s) successfully added to cart."));

				FileCartComponent.sendFileCartChangeJmsMsg(SecurityUtil.getLoggedInUser().getId());
			}

		}

	}

	private void initLayout() {

		ToolboxAssert.isTrue(!this.fileDescriptorIds.isEmpty());

		this.removeAllComponents();

		final FileStoreService fileStoreService = ApplicationContextHelper.getBean(FileStoreService.class);

		final Label lblContent = new Label();
		lblContent.setSizeFull();
		lblContent.setContentMode(ContentMode.HTML);
		this.addComponent(lblContent);

		// ---

		// TODO: jsLastShownFileDescriptorIdCallbackId
		this.jsLastShownFileDescriptorIdCallbackId = ((AbstractToolboxUI) UI.getCurrent()).registerAtuiCallbackConsumer(new WeakReference<>(this));

		this.btnInfo.addClickListener(x -> {
			final String script = "function () { return 'showInfo<op>' + document.getElementsByClassName(\"activeSlide\")[0].querySelectorAll(\"img, object\")[0].getAttribute('data-file-desc-id'); }";
			((AbstractToolboxUI) UI.getCurrent()).callJsViaAtuiCallback(this.jsLastShownFileDescriptorIdCallbackId, script);
		});

		this.btnOpenOriginal.addClickListener(x -> {
			final String script = "function () { return 'openOriginal<op>' + document.getElementsByClassName(\"activeSlide\")[0].querySelectorAll(\"img, object\")[0].getAttribute('data-file-desc-id'); }";
			((AbstractToolboxUI) UI.getCurrent()).callJsViaAtuiCallback(this.jsLastShownFileDescriptorIdCallbackId, script);
		});

		if (this.btnDownload != null && this.btnAddToCart != null) {

			this.btnDownload.addClickListener(x -> {
				final String script = "function () { return 'download<op>' + document.getElementsByClassName(\"activeSlide\")[0].querySelectorAll(\"img, object\")[0].getAttribute('data-file-desc-id'); }";
				((AbstractToolboxUI) UI.getCurrent()).callJsViaAtuiCallback(this.jsLastShownFileDescriptorIdCallbackId, script);
			});

			this.btnAddToCart.addClickListener(x -> {
				final String script = "function () { return 'addToCart<op>' + document.getElementsByClassName(\"activeSlide\")[0].querySelectorAll(\"img, object\")[0].getAttribute('data-file-desc-id'); }";
				((AbstractToolboxUI) UI.getCurrent()).callJsViaAtuiCallback(this.jsLastShownFileDescriptorIdCallbackId, script);
			});
		}

		// ---

		final StringBuilder sbHtmlCode = new StringBuilder();
		sbHtmlCode.append("</div>\n");
		sbHtmlCode.append("<!-- The Modal -->\n");
		sbHtmlCode.append("<div id=\"fileCarouselModal-").append(this.fileCarouselUid).append("\" class=\"modal\">\n");
		sbHtmlCode.append("\t<span class=\"close\">&times;</span>\n");
		sbHtmlCode.append("\t<img class=\"modal-content\" id=\"img01-").append(this.fileCarouselUid).append("\">\n");
		sbHtmlCode.append("\t<div class=\"caption-text\" id=\"caption-").append(this.fileCarouselUid).append("\"></div>\n");
		sbHtmlCode.append("</div>\n");

		sbHtmlCode.append("<div id=\"carousel-").append(this.fileCarouselUid).append("\">\n");

		int imgDomId = 1;
		for (final Integer fileDescriptorId : this.fileDescriptorIds) {

			ToolboxAssert.notNull(fileDescriptorId);

			final FileDescriptor fileDescriptor = fileStoreService.getFile2(fileDescriptorId, false, null, FileOperationAccessTypeIntent.READ_ONLY_INTENT);
			String filename = StringUtils.abbreviateMiddle(fileDescriptor.getFilename(), "...", 45);

			if (fileDescriptor.getMeta3Caption() != null) {
				filename += "(" + fileDescriptor.getMeta3Caption() + ")";
			}

			final String fileExt = FilenameUtils.getExtension(filename);

			if (FileStoreHelper.isJpgOrPng(fileExt)) {

				final String url = FileStoreHelper.generateDownloadUrl(fileDescriptorId, false, ToolboxSysKeys.FileDescriptorChildType.VARIANT_2048_2048_PROGR_JPG);

				sbHtmlCode.append("\t<img id=\"img").append(imgDomId).append("\" class=\"slide\" data-src=\"").append(url).append("\" data-file-desc-id=\"").append(fileDescriptorId).append("\" alt=\"").append(filename).append("\" data-caption=\"").append(filename).append("\"/> \n");
				imgDomId++;

			} else if (FileStoreHelper.isPdf(fileExt)) {

				final String url = FileStoreHelper.generateDownloadUrl(fileDescriptorId, false, null);

				sbHtmlCode.append("\t<object class=\"slide\" data-data=\"").append(url).append("\" type=\"application/pdf\" data-caption=\"").append(filename).append("\" data-file-desc-id=\"").append(fileDescriptorId).append("\"></object>\n");

			} else {
				log.debug("Unable to display file (ext: " + fileExt + ")!");
			}

		}

		lblContent.setValue(sbHtmlCode.toString());

		// ---

		StringBuilder sbScript = new StringBuilder();

		sbScript.append("var myInterval = setInterval(function(){\n");
		sbScript.append("\t// Get the modal\n");
		sbScript.append("\tvar modal = document.getElementById('fileCarouselModal-").append(this.fileCarouselUid).append("');\n");
		sbScript.append("\t\n");
		sbScript.append("\tif (modal) {\n");
		sbScript.append("\t\t// Get the image and insert it inside the modal - use its \"alt\" text as a caption\n");
		sbScript.append("\t\tvar modalImg = document.getElementById(\"img01-").append(this.fileCarouselUid).append("\");\n");
		sbScript.append("\t\tvar captionText = document.getElementById(\"caption-").append(this.fileCarouselUid).append("\");\n");
		sbScript.append("\n");
		sbScript.append("\t\tfunction onImgClick() {\n");
		sbScript.append("\t\t\tmodal.style.display = \"block\";\n");
		sbScript.append("\t\t\tmodalImg.src = this.src;\n");
		sbScript.append("\t\t\tcaptionText.innerHTML = this.alt;\n");
		sbScript.append("\t\t}\n");
		sbScript.append("\t\t//$('img.slide').on(\"click\", onImgClick);\n");
		sbScript.append("\n");
		sbScript.append("\t\t// Get the <span> element that closes the modal\n");
		sbScript.append("\t\tvar span = document.getElementsByClassName(\"close\")[0];\n");
		sbScript.append("\t\t// When the user clicks on <span> (x), close the modal\n");
		sbScript.append("\t\tmodalImg.onclick = function() {\n");
		sbScript.append("\t\t\tmodal.style.display = \"none\";\n");
		sbScript.append("\t\t}\n");
		sbScript.append("\t\t\n");
		sbScript.append("\t\tclearInterval(myInterval);\n");
		sbScript.append("\t}\n");
		sbScript.append("\t\n");
		sbScript.append("},10);");

		com.vaadin.ui.JavaScript.getCurrent().execute(sbScript.toString());

		// ---

		sbScript = new StringBuilder();

		sbScript.append("var carouselInterval = setInterval(function(){\n");
		sbScript.append("\n");
		sbScript.append("console.log('Start carouselInterval...');\n");
		sbScript.append("\tvar modal = document.getElementById('carousel-").append(this.fileCarouselUid).append("');\n");
		sbScript.append("\t\n");
		sbScript.append("\tif (modal) {\n");
		sbScript.append("console.log('modal exists');\n");
		sbScript.append("\t\t$(function($) {\n");
		sbScript.append("\t\t\t$(\"#carousel-").append(this.fileCarouselUid).append("\").pdfSlider(\n");
		sbScript.append("\t\t\t\t{\n");
		sbScript.append("\t\t\t\t\tcontainer: '#carousel-").append(this.fileCarouselUid).append("',\n");
		sbScript.append("\t\t\t\t\titem : '.slide',\n");
		sbScript.append("\t\t\t\t\titemWidth : 912,\n");
		sbScript.append("\t\t\t\t\titemHeight : 684\n");
		sbScript.append("\t\t\t\t}\n");
		sbScript.append("\t\t\t);\n");
		sbScript.append("\t\t});\n");
		sbScript.append("\t\t\n");
		sbScript.append("\t\tclearInterval(carouselInterval);\n");
		sbScript.append("\t}\n");
		sbScript.append("\t\n");
		sbScript.append("},10);");

		com.vaadin.ui.JavaScript.getCurrent().execute(sbScript.toString());

	}

}
