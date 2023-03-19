package hu.lanoga.toolbox.util;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import lombok.extern.slf4j.Slf4j;

/**
 * jelenleg Apache Tika alapú
 */
@Slf4j
public class ToolboxMimeTypeHelper {

	private static TikaConfig tika; // elvileg tread-safe // TODO: biztos?

	static {
		try {
			tika = new TikaConfig();
		} catch (final Exception e) {
			log.error("MimeTypeHelper init error!", e);
			throw new ToolboxGeneralException("MimeTypeHelper init error!", e);
		}
	}

	private ToolboxMimeTypeHelper() {
		//
	}

	/**
	 * Apache Tika alapú
	 * 
	 * @param is
	 * 		nem zárja le a stream-et
	 * 
	 * @return
	 * 		ha nem tudja érdemben megmondani, 
	 * 		akkor "application/octet-stream" lesz a visszadott érték
	 */
	public static String determineMimeType(final InputStream is) {

		try {

			ToolboxAssert.notNull(is);

			final MediaType m = tika.getDetector().detect(
					TikaInputStream.get(is), new Metadata());

			return m.toString();

		} catch (final Exception e) {
			throw new ToolboxGeneralException("MimeTypeHelper determineMimeType error!", e);
		}

	}

	/**
	 * Apache Tika alapú
	 * 
	 * @param f
	 * 
	 * @return
	 * 		ha nem tudja érdemben megmondani, 
	 * 		akkor "application/octet-stream" lesz a visszadott érték
	 */
	public static String determineMimeType(final File f) {

		try {

			ToolboxAssert.notNull(f);

			final Metadata metadata = new Metadata();
			metadata.set(Metadata.RESOURCE_NAME_KEY, f.toString());

			final MediaType m = tika.getDetector().detect(
					TikaInputStream.get(f.toPath()), metadata);

			return m.toString();

		} catch (final Exception e) {
			throw new ToolboxGeneralException("MimeTypeHelper determineMimeType error!", e);
		}

	}

	/**
	 * @param f
	 * 		fájl, amit ellenőrzünk
	 * 
	 * @param exts
	 * 		vesszővel elválaszott, pl.: "png,jpg,jpeg,gid"; 
	 * 		null vagy üres str esetén nincs ext check
	 * @param extsIsBlackList
	 * 		true esetén az exts-ben jelölt-ek számítanak hibásnak, 
	 * 		false esetén csak azok vannak engedve
	 * @param emptyExtCountsAsOk
	 * 		ha egyáltalán nincs ".valami" ext a fájlnévben, akkor az átengedhető-e
	 * 
	 * @param mimeTypes
	 * 		vesszővel elválaszott, pl.: "image/jpeg,application/pdf", 
	 * 		amennyibe exts is meg van adva, akkor előbb az lesz ellenőrizve; 
	 * 		null vagy üres str esetén nincs mime type check;
	 * 		ha application/octet-stream is a listában van, akkor minden engedve lesz
	 * @param mimeTypesIsBlackList
	 * 		true esetén az mimeTypes-ben jelölt-ek számítanak hibásnak, 
	 * 		false esetén csak azok vannak engedve
	 * 
	 * @return
	 * 		true = átment a teszten, "jó" fájl; 
	 * 		false = elbukott az ellenőrzésen
	 */
	public static boolean extAndMimeTypeCheck(final File f,
			final String exts, final boolean extsIsBlackList, final boolean emptyExtCountsAsOk,
			final String mimeTypes, final boolean mimeTypesIsBlackList) {

		final boolean doExtCheck = StringUtils.isNotBlank(exts);
		final boolean doMimeCheck = StringUtils.isNotBlank(mimeTypes);

		ToolboxAssert.isTrue(doExtCheck || doMimeCheck); // legalább egy dolog legyen nézve (ha már egyszer ráhívott a metódusra); lehet olyan is, hogy mindkettő

		// ---

		if (doExtCheck) { // level 1 check, filename alapján

			final String actualExt = FilenameUtils.getExtension(f.getName());

			if (StringUtils.isNotBlank(actualExt)) {

				final Set<String> extSet = new HashSet<>(Arrays.asList(StringUtils.split(exts, ",")));

				if (extsIsBlackList) {

					if (extSet.contains(actualExt)) {
						log.debug("ext check fail, extsIsBlackList, extSet: " + extSet + ", actualExt: " + actualExt);
						return false;
					}

				} else {

					if (!extSet.contains(actualExt)) {
						log.debug("ext check fail, extsIsWhiteList, extSet: " + extSet + ", actualExt: " + actualExt);
						return false;
					}

				}

			} else {

				if (!emptyExtCountsAsOk) {
					log.debug("ext check fail, empty ext and emptyExtCountsAsOk is false");
					return false;
				}

			}

		}

		// ---

		if (doMimeCheck) { // level 2 check, Apache Tika, bele "túr" a fájlba és az alapján próbál mimeType-ot megállapítani

			return mimeTypeCheck(f, mimeTypes, mimeTypesIsBlackList);

		} else {

			return true;

		}

	}

	/**
	 * @param f
	 * 		fájl, amit ellenőrzünk
	 * @param mimeTypes
	 * 		vesszővel elválaszott, pl.: "image/jpeg,application/pdf";
	 * 		ha application/octet-stream is a listában van, akkor minden engedve lesz
	 * @param mimeTypesIsBlackList
	 * 
	 * @return
	 */
	public static boolean mimeTypeCheck(final File f, final String mimeTypes, final boolean mimeTypesIsBlackList) {

		ToolboxAssert.notNull(f);
		ToolboxAssert.isTrue(StringUtils.isNotBlank(mimeTypes));

		// ---

		final String mimeTypeStr = determineMimeType(f);
		final Set<String> mimeTypeSet = new HashSet<>(Arrays.asList(StringUtils.split(mimeTypes, ",")));

		if (mimeTypesIsBlackList) {

			if (mimeTypeSet.contains(mimeTypeStr)) {
				log.debug("binary mimeTypeCheck failed, mimeTypesIsBlackList, mimeTypeSet: " + mimeTypeSet + ", actual mimeTypeStr: " + mimeTypeStr);
				return false;
			}

		} else {

			if (!mimeTypeSet.contains(mimeTypeStr)) {
				log.debug("binary mimeTypeCheck failed, mimeTypesIsWhiteList, mimeTypeSet: " + mimeTypeSet + ", actual mimeTypeStr: " + mimeTypeStr);
				return false;
			}

		}

		return true;

	}

	/**
	 * @param f
	 * 		fájl (InputStream), amit ellenőrzünk
	 * @param mimeTypes
	 * 		vesszővel elválaszott, pl.: "image/jpeg,application/pdf";
	 * 		ha application/octet-stream is a listában van, akkor minden engedve lesz
	 * @param mimeTypesIsBlackList
	 * 
	 * @return
	 */
	public static boolean mimeTypeCheck(final InputStream is, final String mimeTypes, final boolean mimeTypesIsBlackList) {

		ToolboxAssert.notNull(is);
		ToolboxAssert.isTrue(StringUtils.isNotBlank(mimeTypes));

		// ---

		final String mimeTypeStr = determineMimeType(is);
		final Set<String> mimeTypeSet = new HashSet<>(Arrays.asList(StringUtils.split(mimeTypes, ",")));

		if (mimeTypesIsBlackList) {

			if (mimeTypeSet.contains(mimeTypeStr)) {
				return false;
			}

		} else {

			if (!mimeTypeSet.contains(mimeTypeStr)) {
				return false;
			}

		}

		return true;

	}

}
