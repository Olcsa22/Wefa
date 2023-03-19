package hu.lanoga.toolbox.vaadin.component;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.UUID;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.StringUtils;

import com.vaadin.server.StreamResource;
import com.vaadin.ui.Embedded;

import hu.lanoga.toolbox.file.PictureHelper;
import hu.lanoga.toolbox.util.ToolboxAssert;
import lombok.Getter;

/**
 * @see PictureHelper#generateQrCodeSvgStr(String)
 */
@Getter
public class QrCodeSvgComponent extends Embedded {
	
	// minta használat:
	// QrCodeSvgComponent qrCodeSvg = new QrCodeSvgComponent("Árvíztűrő tükörfúrógép");
	// qrCodeSvg.setWidth("250px");
	// qrCodeSvg.setHeight("250px");
	// this.addComponent(qrCodeSvg);

	public class SvgStreamSource extends StreamResource {

		public SvgStreamSource(final String svgText, final String svgFilename) {
			super(new StreamSource() {

				@Override
				public InputStream getStream() {
					final InputStream s = new BufferedInputStream(new ReaderInputStream(new StringReader(svgText)));
					return s;
				}
			}, svgFilename);
		}

		@Override
		public String getMIMEType() {
			return "image/svg+xml";
		}

	}

	public QrCodeSvgComponent(final String content) {

		ToolboxAssert.isTrue(StringUtils.isNotBlank(content));

		final String svgText = PictureHelper.generateQrCodeSvgStr(content);

		super.setSource(new SvgStreamSource(svgText, UUID.randomUUID().toString() + ".svg"));
		
	}

}
