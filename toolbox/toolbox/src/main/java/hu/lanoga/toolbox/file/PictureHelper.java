package hu.lanoga.toolbox.file;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.twelvemonkeys.image.ResampleOp;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.FileOperationAccessTypeIntent;
import hu.lanoga.toolbox.exception.ToolboxBadRequestException;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.vaadin.component.QrCodeSvgComponent;
import io.nayuki.qrcodegen.QrCode;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnailator;

@Slf4j
@Component
public class PictureHelper {

	public static final Dimension DIM_IMG_DEFAULT = new Dimension(4096, 4096); // TODO: prop fájlba a méretek (PT)
	public static final Dimension DIM_THUMB_DEFAULT = new Dimension(512, 512); // TODO: prop fájlba a méretek (PT)

	/**
	 * only if orig. img is too big (if already smaller, than no change)
	 *
	 * @param imgSize
	 * @param boundary
	 * @return
	 */
	private static Dimension getScaledDimension(final Dimension imgSize, final Dimension boundary) {

		// http://stackoverflow.com/questions/10245220/java-image-resize-maintain-aspect-ratio

		final int original_width = imgSize.width;
		final int original_height = imgSize.height;
		final int bound_width = boundary.width;
		final int bound_height = boundary.height;
		int new_width = original_width;
		int new_height = original_height;

		// first check if we need to scale width
		if (original_width > bound_width) {
			// scale width to fit
			new_width = bound_width;
			// scale height to maintain aspect ratio
			new_height = (new_width * original_height) / original_width;
		}

		// then check if we need to scale even with the new height
		if (new_height > bound_height) {
			// scale height to fit instead
			new_height = bound_height;
			// scale width to maintain aspect ratio
			new_width = (new_height * original_width) / original_height;
		}

		return new Dimension(new_width, new_height);
	}

	@Autowired
	private FileStoreService fileStoreService;

	private static final Semaphore magicConvertSemaphore = new Semaphore(100);

	/**
	 * (csak JPEG/PNG/GIF(non-anim.) input működik jól)
	 * 
	 * @param origFile 
	 * 		kötelező
	 * @param origMime
	 * 		kötelező
	 * @param targetMime
	 * 		kötelező
	 * @param dTargetImgSize
	 *  	cél kép mérete (null => default cél méret) (képarány megtartással minden esetben)
	 * @param createSmallImg
	 * 		legyen-e thumbnail is (képarány megtartással minden esetben)
	 * @param dTargetImgSmallSize
	 * 		thumbnail mérete (null => default thumnail méret)
	 *            
	 * @return (left) a mentett (tmp) fájl; (right) a thumbnail (ha a hívó nem kért, akkor null) tmp file
	 */
	public Pair<FileDescriptor, FileDescriptor> magicConvert(final FileDescriptor origFile, final String origMime, final String targetMime, final Dimension dTargetImgSize, final boolean createSmallImg, final Dimension dTargetImgSmallSize) {
		return this.magicConvert(origFile, origMime, targetMime, dTargetImgSize, createSmallImg, dTargetImgSmallSize, 0.8f);
	}

	public Pair<FileDescriptor, FileDescriptor> magicConvert(final FileDescriptor origFile, String origMime, String targetMime, Dimension dTargetImgSize, final boolean createSmallImg, Dimension dTargetImgSmallSize, final float jpgQuality) {

		final long startT = System.currentTimeMillis();

		// ---

		if (dTargetImgSize == null) {
			dTargetImgSize = DIM_IMG_DEFAULT; // default
		}

		if (dTargetImgSmallSize == null) {
			dTargetImgSmallSize = DIM_THUMB_DEFAULT; // default
		}

		// ---

		int resampleOpType = ResampleOp.FILTER_LANCZOS;
		int resampleOpTypeForSmallImg = ResampleOp.FILTER_LANCZOS;

		// ---

		File thumbnailFile = null;
		File outFile = null;

		InputStream is = null;

		Integer magicConvertSemaphorePermitCount = null;

		try {

			final File origFileFile = this.fileStoreService.getFile2(origFile.getId(), FileOperationAccessTypeIntent.READ_ONLY_INTENT).getFile();

			final long fileSize = origFileFile.length(); // mj.: origFile.getFileSize() nem mindig érhető el (ha TMP még az origFile is)

			if (fileSize > 5L * 1024L * 1024L) { // 5 MB <
				magicConvertSemaphorePermitCount = 80;

				// http://forum.doom9.org/archive/index.php/t-139656.html
				// from soft to sharp (according to my experience):
				// Bilinear
				// Bicubic(b=1./3, c=1./3)
				// Spline16
				// Bicubic(b=0, c=0.75)
				// Spline36
				// Blackman(taps=4)
				// Spline64
				// Lanczos3
				// Lanczos4

				resampleOpType = ResampleOp.FILTER_BLACKMAN; // valamivel gyorsabb, de kevésbé jó resample algoritmus
				resampleOpTypeForSmallImg = ResampleOp.FILTER_BLACKMAN;

			} else if (fileSize > 2L * 1024L * 1024L) { // 1 - 5 MB között

				resampleOpType = ResampleOp.FILTER_MITCHELL; // valamivel gyorsabb, de kevésbé jó resample algoritmus
				resampleOpTypeForSmallImg = ResampleOp.FILTER_MITCHELL;

				magicConvertSemaphorePermitCount = 40;
			} else { // 1 MB alatti input fájl
				magicConvertSemaphorePermitCount = 20;
			}

			magicConvertSemaphore.acquire(magicConvertSemaphorePermitCount); // ezzel a megoldással limitáljuk, hogy egyszerre hány fájl konvertálás mehet

			// ---

			is = new BufferedInputStream(new FileInputStream(origFileFile), 32 * 1024);

			if (origMime == null) {
				origMime = targetMime;
			}

			targetMime = targetMime.toLowerCase();
			origMime = origMime.toLowerCase();

			final boolean makeJpeg = "image/jpeg".equals(targetMime);
			final boolean makePng = "image/png".equals(targetMime);
			final boolean makeGif = "image/gif".equals(targetMime);

			String outExt;

			if (makeJpeg) {
				outExt = "jpg";
			} else if (makePng) {
				outExt = "png";
			} else if (makeGif) {
				outExt = "gif";
			} else {
				outExt = "dat";
			}

			FileDescriptor pThumbnail = null;
			if (createSmallImg) {
				pThumbnail = this.fileStoreService.createTmpFile2(FilenameUtils.getBaseName(origFile.getFilename()) + "-phth." + outExt, origFile.getLocationType(), origFile.getSecurityType());
				thumbnailFile = pThumbnail.getFile();
			}

			final FileDescriptor pOutFile = this.fileStoreService.createTmpFile2(FilenameUtils.getBaseName(origFile.getFilename()) + "-ph." + outExt, origFile.getLocationType(), origFile.getSecurityType());

			outFile = pOutFile.getFile();

			// ---

			if (makeJpeg) {

				// if jpeg then limit the size and (optionally) create thumbnail

				ImageInputStream iis = null;
				ImageReader ir = null;

				ImageWriter iw1 = null;
				ImageWriter iw2 = null;

				BufferedOutputStream bos1 = null;
				BufferedOutputStream bos2 = null;

				ImageOutputStream ios1 = null;
				ImageOutputStream ios2 = null;

				try {

					iis = ImageIO.createImageInputStream(is);
					ir = ImageIO.getImageReaders(iis).next();

					ir.setInput(iis);

					BufferedImage biOrig = ir.read(0);

					// ---

					boolean isCmyk = false;

					final Iterator<ImageTypeSpecifier> imageTypes = ir.getImageTypes(0);
					while (imageTypes.hasNext()) {
						final ImageTypeSpecifier t = imageTypes.next();
						if (t.getBufferedImageType() == 0) { // http://stackoverflow.com/questions/26716215/java-read-a-cmyk-image-to-bufferdimage-and-keep-the-right-color-model
							isCmyk = true;
							break;
						}
					}

					// ---

					// author etc. (for some reason doesn't work with CMYK JPG-s)
					// TODO: possibly faulty in some cases... disable if it causes problems (but otherwise leave it, I think some relavant laws make it mandatory to keep the original author!)

					final IIOMetadata iiometaOrig = (!isCmyk) ? (ir.getImageMetadata(0)) : (null);

					// ---

					if (!("image/jpeg".equals(origMime))) {

						// alpha chanel have to be removed, CMYK problems etc.

						final BufferedImage biTmp = new BufferedImage(biOrig.getWidth(), biOrig.getHeight(), BufferedImage.TYPE_INT_RGB);
						biTmp.createGraphics().drawImage(biOrig, 0, 0, Color.WHITE, null);

						biOrig = biTmp;

					}

					final Dimension dOrig = new Dimension(biOrig.getWidth(), biOrig.getHeight());

					// ---

					iw1 = ImageIO.getImageWritersByFormatName("jpg").next();
					bos1 = new BufferedOutputStream(new FileOutputStream(outFile), 32 * 1024);
					ios1 = ImageIO.createImageOutputStream(bos1);

					final Dimension d1 = getScaledDimension(dOrig, dTargetImgSize);
					final BufferedImage biOutput1;

					if ((d1.height < dOrig.height) || (d1.width < dOrig.width)) {
						biOutput1 = new ResampleOp(d1.width, d1.height, resampleOpType).filter(biOrig, null);
					} else {
						biOutput1 = biOrig; // if the input is already small engough
					}

					biOrig = null; // garbage collector optimization

					final ImageWriteParam imageWriterParam1 = iw1.getDefaultWriteParam();
					imageWriterParam1.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					imageWriterParam1.setCompressionQuality(jpgQuality);
					imageWriterParam1.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);

					final IIOImage iio1 = new IIOImage(biOutput1, null, iiometaOrig);
					iw1.setOutput(ios1);
					iw1.write(null, iio1, imageWriterParam1);

					// ---

					if (createSmallImg) {

						iw2 = ImageIO.getImageWritersByFormatName("jpg").next();
						bos2 = new BufferedOutputStream(new FileOutputStream(thumbnailFile), 64 * 1024);
						ios2 = ImageIO.createImageOutputStream(bos2);

						final Dimension d2 = getScaledDimension(dOrig, dTargetImgSmallSize);

						final BufferedImage biOutput2;
						if ((d2.height < d1.height) || (d2.width < d1.width)) {
							biOutput2 = new ResampleOp(d2.width, d2.height, resampleOpTypeForSmallImg).filter(biOutput1, null);
						} else {
							biOutput2 = biOutput1; // if it is already small engough
						}

						// TODO: use (force) 4:2:0 subsampling for the thumbnail? (possibly -10-20% file size) (compressionQuality handles this? separate option?)

						final ImageWriteParam imageWriterParam2 = iw1.getDefaultWriteParam();
						imageWriterParam2.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
						imageWriterParam2.setCompressionQuality(jpgQuality);
						imageWriterParam2.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);

						final IIOImage iio2 = new IIOImage(biOutput2, null, iiometaOrig);
						iw1.setOutput(ios2);
						iw1.write(null, iio2, imageWriterParam2);

						return Pair.of(pOutFile, pThumbnail);

					} else {
						return Pair.of(pOutFile, null);
					}

				} finally {

					try {
						if (iw1 != null) {
							iw1.dispose();
						}
					} catch (final Exception e) {
						//
					}

					try {
						if (iw2 != null) {
							iw2.dispose();
						}
					} catch (final Exception e) {
						//
					}

					IOUtils.closeQuietly(ios1);
					IOUtils.closeQuietly(ios2);

					IOUtils.closeQuietly(bos1);
					IOUtils.closeQuietly(bos2);

					try {
						if (ir != null) {
							ir.dispose();
						}
					} catch (final Exception e) {
						//
					}

					IOUtils.closeQuietly(iis);
				}

			} else if (makePng || makeGif) {

				if (makeGif) {

					// anim gif resize is not supported

					boolean isAnimatedGif = false;

					final InputStream is2 = new ByteArrayInputStream(IOUtils.toByteArray(is));
					IOUtils.closeQuietly(is);
					is = is2;

					try {

						final ImageReader ir = ImageIO.getImageReadersBySuffix("GIF").next();

						try (ImageInputStream iis = ImageIO.createImageInputStream(is)) {

							ir.setInput(iis);

							try {
								ir.read(1);
								isAnimatedGif = true;
								// log.debug("Animated GIF");
							} catch (final IndexOutOfBoundsException ex) {
								// log.debug("Simple GIF");
							}

						}

					} catch (final Exception e) {
						log.debug("Unable to detect gif img count (isAnimated)!");
					}

					is.reset(); // !!!

					if (isAnimatedGif) {

						// simply write it to disk

						try (OutputStream os = new BufferedOutputStream(new FileOutputStream(outFile), 64 * 1024)) {
							IOUtils.copyLarge(is, os);
						}

						return Pair.of(pOutFile, null);

					}

				}

				final BufferedImage biA = ImageIO.read(is);

				final Dimension dOrig = new Dimension(biA.getWidth(), biA.getHeight());

				final Dimension d1 = getScaledDimension(dOrig, dTargetImgSize);

				final BufferedImage biB;

				if ((d1.height < dOrig.height) || (d1.width < dOrig.width)) {
					biB = Thumbnailator.createThumbnail(biA, d1.width, d1.height);
				} else {
					biB = biA; // if it is already small engough
				}

				try (OutputStream os = new BufferedOutputStream(new FileOutputStream(outFile), 64 * 1024)) {
					ImageIO.write(biB, outExt, outFile);
				}

				if (createSmallImg) {

					final Dimension d2 = getScaledDimension(new Dimension(biA.getWidth(), biA.getHeight()), dTargetImgSmallSize);

					final BufferedImage biC;
					if ((d2.height < d1.height) || (d2.width < d1.width)) {
						biC = Thumbnailator.createThumbnail(biB, d2.width, d2.height);
					} else {
						biC = biB; // if it is already small engough
					}

					try (OutputStream os = new BufferedOutputStream(new FileOutputStream(thumbnailFile), 64 * 1024)) {
						ImageIO.write(biC, outExt, thumbnailFile);
					}

					return Pair.of(pOutFile, pThumbnail);

				} else {

					return Pair.of(pOutFile, null);
				}

			} else {

				// simply write it to disk

				try (OutputStream os = new BufferedOutputStream(new FileOutputStream(outFile), 64 * 1024)) {
					IOUtils.copyLarge(is, os);
				}

				return Pair.of(pOutFile, null);

			}

		} catch (javax.imageio.IIOException e) {
			
			if ("Invalid argument to native writeImage".equalsIgnoreCase(e.getMessage())) {
				final String error = "magicConvert error (probably broken, or exotic format image): " + e.getMessage();
				log.warn(error);
				throw new ToolboxBadRequestException(error);
			} else {
				log.error("magicConvert error!", e);
				throw new ToolboxGeneralException(e);
			}
			
		} catch (final Exception e) {
			
			log.error("magicConvert error!", e);
			throw new ToolboxGeneralException(e);

		} finally {

			IOUtils.closeQuietly(is);

			if (magicConvertSemaphorePermitCount != null) {
				magicConvertSemaphore.release(magicConvertSemaphorePermitCount);
			}

			log.debug("magicConvert run time (millis): " + (System.currentTimeMillis() - startT));

		}

	}

	/**
	 * @param content
	 * @return
	 * 
	 * @see QrCodeSvgComponent
	 */
	public static String generateQrCodeSvgStr(final String content) {

		final QrCode qr0 = QrCode.encodeText(content, QrCode.Ecc.MEDIUM);
		return qr0.toSvgString(1);

	}

	/**
	 * @return
	 * 		{@link ToolboxSysKeys.FileDescriptorStatus#TEMPORARY} status
	 */
	public static FileDescriptor generateDummyPngPict() {

		try {

			final FileDescriptor tmpFd = ApplicationContextHelper.getBean(FileStoreService.class).createTmpFile2("dummy.png");

			final BufferedImage bufferedImage = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);

			for (int x = 0; x < 500; x = x + 50) {
				for (int y = 0; y < 500; y = y + 50) {

					final int r = RandomUtils.nextInt(0, 256);
					final int g = RandomUtils.nextInt(0, 256);
					final int b = RandomUtils.nextInt(0, 256);
					final int color = (r << 16) | (g << 8) | b;

					for (int i = 0; i < 50; i++) {
						for (int j = 0; j < 50; j++) {
							bufferedImage.setRGB(x + i, y + j, color);
						}
					}

				}
			}

			ImageIO.write(bufferedImage, "jpg", tmpFd.getFile());

			return tmpFd;

		} catch (final IOException e) {
			throw new ToolboxGeneralException("generateDummyPngPict() failed!", e);
		}
	}

}
