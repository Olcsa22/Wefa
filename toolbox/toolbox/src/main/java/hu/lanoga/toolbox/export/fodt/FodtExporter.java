package hu.lanoga.toolbox.export.fodt;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Striped;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.FileDescriptorLocationType;
import hu.lanoga.toolbox.ToolboxSysKeys.FileDescriptorSecurityType;
import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.export.SofficeHelper;
import hu.lanoga.toolbox.export.docx.DocxExporterException;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.util.ToolboxAssert;
import lombok.extern.slf4j.Slf4j;

/**
 * FODT exporter/template behelyetesítés... 
 * mivel a generált fájlban csak azok a mezők fognak szerepelni, amelyekre a manuálisan létrehozott template fájl hivatkozik, 
 * ezért ez az exporter nem veszi figyelembe az {@link ExporterIgnore} annotációkat... 
 */
@Slf4j
public class FodtExporter {

	// fontos: amikor LibreOffice-ban szerkeszted a sablon, néha kerülnek be "szétörések" (span-nak nevezett bekezdésen belüli egységek), amiket nem kezel most még a kód
	// átmentileg megoldás: CTRL+X és CTRL+V, ez letisztázza ezeket és "kisimítja" az XML-t (a .FODT fájt), fontos még formázatlan/plain szövegként visszailleszteni (és inkább utána ráformázni az egészet kijelölve)

	private static final Striped<Lock> sourceResourceCacheLock = Striped.lock(5);
	private static final Cache<String, byte[]> sourceResourceCache = CacheBuilder.newBuilder().maximumSize(10).concurrencyLevel(4).expireAfterWrite(15, TimeUnit.MINUTES).build();

	/**
	 * FODT template kitöltése, PDF-re konvertálása...
	 *
	 * @param sourceFileStr
	 *            template FODT fájl helye;
	 *            bármi, amit a {@link PathMatchingResourcePatternResolver} kezelni tud,
	 *            pl.: "classpath:doc_templates/invoice_templates/inv1.docx",
	 *            lehet fájlrendszerre is mutatni "file:..." formában)
	 * @param targetFodt
	 *            ide lesz mentve az elkészült (kitöltött) FODT fájl; targetFodt/targetPdf legalább egyike nem null kell legyen
	 * @param targetPdf
	 *            ide lesz mentve az elkészült (kitöltött és PDF-re konvertált) FODT fájl; targetFodt/targetPdf legalább egyike nem null kell legyen
	 * @param templParamMap
	 *            <pre>
	 *            többféle mód is van:
	 *            1) egyszerű érték: 
	 *            		sima replace a key alapján (a template FODT-ben <# kulcs #> helyére van behelyettesítve, több helyen is lehet ugyanez, mindhez be lesz helyettesítve), 
	 *            		(.toString() van ráhívva, ahol ez nem jó, pl. BigDecimal, ott már a Map-be rakásnál csinálj belőle alkalmas/formázott String-et)
	 *            2) lista (value instance of List) megoldás:
	 *            		példa: "<#R_NAME_*#>" ez a bekezdés (LibreOffice értelemben bekezdés) lesz kiszedve, feltölve annyiszor, 
	 *            		ahány elem van a listában és végül ezzel (a hosszú konkatenált értékkel) lesz az eredeti *-os elem lecserélve, 
	 *            		amennyiben egyes sorszámok külön is szereplenek, akkor azok oda lesznek beírva 
	 *            		(tehát pl.: "<#R_NAME_22#>", ekkor ez soron kívül nem a listába kerül, a számozás 1-ről indul), 
	 *            		több szintű listák stb. nem támogatottak
	 *            </pre>
	 * @param docxExportEnginePdfMode
	 * 
	 * @see PathMatchingResourcePatternResolver
	 */
	public final void fillTemplate(final String sourceFileStr, final FileDescriptor targetFodt, final FileDescriptor targetPdf, final Map<String, Object> templParamMap, final ToolboxSysKeys.DocxExportEnginePdfMode docxExportEnginePdfMode) {

		ToolboxAssert.notNull(sourceFileStr);
		ToolboxAssert.notNull(docxExportEnginePdfMode);
		ToolboxAssert.isTrue((targetFodt != null) || (targetPdf != null));
		ToolboxAssert.notNull(templParamMap);

		final boolean isClasspathTemplate = sourceFileStr.contains("classpath:");

		// ---

		byte[] ba = null;

		// ---

		if (isClasspathTemplate) {
			ba = sourceResourceCache.getIfPresent(sourceFileStr);
		}

		if (ba == null) {

			try {

				if (isClasspathTemplate) {
					sourceResourceCacheLock.get(sourceFileStr).lock();
					ba = sourceResourceCache.getIfPresent(sourceFileStr);
				}

				if (ba == null) {

					final Resource sourceResource = new PathMatchingResourcePatternResolver().getResources(sourceFileStr)[0];

					try (InputStream in = new BufferedInputStream(sourceResource.getInputStream(), 131072)) {
						ba = IOUtils.toByteArray(in);
					}

					if (isClasspathTemplate) {
						sourceResourceCache.put(sourceFileStr, ba);
					}

				}

			} catch (final Exception e) {
				final DocxExporterException ex = new DocxExporterException("fillTemplate error!", e);
				log.error("fillTemplate error", ex);
				throw ex;

			} finally {
				if (isClasspathTemplate) {
					sourceResourceCacheLock.get(sourceFileStr).unlock();
				}
			}

		}

		// ---

		try {

			final String fodtTemplateStr = new String(ba, StandardCharsets.UTF_8);

			// ---

			if (targetFodt != null) {
				this.writeToFodt(fodtTemplateStr, targetFodt.getFile(), templParamMap);
			}

			if (targetPdf != null) {

				FileDescriptor tmpTargetFodt = null;

				try {

					if (targetFodt == null) {
						tmpTargetFodt = ApplicationContextHelper.getBean(FileStoreService.class).createTmpFile2("tmp.fodt", FileDescriptorLocationType.PROTECTED_FOLDER, FileDescriptorSecurityType.ADMIN_OR_CREATOR);
						this.writeToFodt(fodtTemplateStr, tmpTargetFodt.getFile(), templParamMap);
					}

					if (ToolboxSysKeys.DocxExportEnginePdfMode.WITH_SOFFICE.equals(docxExportEnginePdfMode)) {
						ApplicationContextHelper.getBean(SofficeHelper.class).convertToPdf(SofficeHelper.ConvertMode.PDF, targetFodt != null ? targetFodt : tmpTargetFodt, targetPdf);
					} else {
						final DocxExporterException ex = new DocxExporterException("Unimplemented engine type!");
						log.error("fillTemplate error", ex);
						throw ex;
					}

				} finally {
					if (tmpTargetFodt != null) {
						// ilyenkor a .fodt fájlt csak tmp jelleggel hoztuk létre,
						// ezért törölni kell (mj.: ha, van rendes, nem tmp fodt target, akkor azt nem!)
						ApplicationContextHelper.getBean(FileStoreService.class).setToBeDeleted(tmpTargetFodt.getId());
					}
				}

			}

		} catch (final FodtExporterException e) {

			log.error("fillTemplate error", e);
			throw e;

		} catch (final Exception e) {

			final FodtExporterException ex = new FodtExporterException("Fill template general error!", e);
			log.error("fillTemplate error", ex);
			throw ex;

		}

		// log.debug("fillTemplate finished, sourceFile: " + sourceFile + ", targetFodt: " + targetFodt + ", targetPdf: " + targetPdf);

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void writeToFodt(String fodtTemplateStr, final File targetFodt, final Map<String, Object> templParamMap) {

		for (final Entry<String, Object> entry : templParamMap.entrySet()) {

			String key = entry.getKey();

			Object value = entry.getValue();

			// ---

			if (value == null) {
				value = "";
			}

			// ---

			if (value instanceof List) {

				key = key.replace("<", "&lt;");
				key = key.replace("#>", "");

				final Pattern pattern = Pattern.compile("<text:p.*" + Pattern.quote(key) + ".*</text:p>");
				final Matcher matcher = pattern.matcher(fodtTemplateStr);

				StringBuilder sbMatcherReplace = null;
				String wildCardTemplateParagraphBlock = null;

				if (matcher.find()) {

					// ha megvan a szövegben a *-os tag

					wildCardTemplateParagraphBlock = matcher.group();
					sbMatcherReplace = new StringBuilder();
				}

				// ---

				int i = 1;

				for (final Object o : (List) value) {

					ToolboxAssert.isTrue(o instanceof Map);

					// boolean wasIterationNumberTypeFillActuallyExecutedAtLeastOnce = false;
					boolean wasWildCardTypeFillActuallyExecutedAtLeastOnce = false;

					String wildCardTemplateParagraphBlockForThisElement = wildCardTemplateParagraphBlock;
					final boolean doesTheWildCardMainKeyEvenAppear = (wildCardTemplateParagraphBlockForThisElement != null);

					for (final Map.Entry e : (Set<Map.Entry>) ((Map) o).entrySet()) {

						final String subKey = e.getKey().toString(); // kulcs
						final String keyWithSubKeyAndAsteriskWildCard = key + subKey + "#&gt;"; // kulcs, *-os változat
						final String keyWithSubKeyAndIterationNumber = key.replace("*", Integer.toString(i)) + subKey + "#&gt;"; // kulcs, számlálós változat

						final String subValueAsStr = e.getValue() == null ? "" : StringEscapeUtils.escapeXml10(e.getValue().toString()); // behelyetessítendő érték

						if (fodtTemplateStr.contains(keyWithSubKeyAndIterationNumber)) {

							// megnézzük, hogy kiírtan szerepel-e ezzel a sorszámmal

							fodtTemplateStr = this.simpleReplace(fodtTemplateStr, keyWithSubKeyAndIterationNumber, subValueAsStr);
							// wasIterationNumberTypeFillActuallyExecutedAtLeastOnce = true;

						} else if (doesTheWildCardMainKeyEvenAppear) {

							// ha nem és a sima * helyzet van

							ToolboxAssert.isTrue(ObjectUtils.allNotNull(wildCardTemplateParagraphBlock, sbMatcherReplace));
							wildCardTemplateParagraphBlockForThisElement = wildCardTemplateParagraphBlockForThisElement.replace(keyWithSubKeyAndAsteriskWildCard, subValueAsStr);
							wasWildCardTypeFillActuallyExecutedAtLeastOnce = true;
						}

					}

					if (wasWildCardTypeFillActuallyExecutedAtLeastOnce && sbMatcherReplace != null) {
						sbMatcherReplace.append(wildCardTemplateParagraphBlockForThisElement);
						sbMatcherReplace.append("\n"); // ez csak, hogy az XML-en belül szebb legyen (az XML "kód")
					}

					i++;
				}

				if (sbMatcherReplace != null) {

					final Matcher matcher2 = pattern.matcher(fodtTemplateStr); // újra meg kell keresni, mert közben már lehet, hogy módosítottuk a fájlt
					fodtTemplateStr = matcher2.replaceFirst(sbMatcherReplace.toString());

				}

			} else {
				key = key.replace("<", "&lt;");
				key = key.replace(">", "&gt;");
				fodtTemplateStr = this.simpleReplace(fodtTemplateStr, key, value.toString()); // TODO: a value-ra az escapelést ide áthozni (mapfre-ben meg van csinálva kintebb, de itt kellene, vagy nem?)
			}

		}

		try {
			FileUtils.writeStringToFile(targetFodt, fodtTemplateStr, StandardCharsets.UTF_8);
		} catch (final IOException e) {
			throw new FodtExporterException("Output write error!", e);
		}

	}

	private String simpleReplace(final String str, final String key, final String value) {
		return str.replace(key, StringEscapeUtils.escapeXml10(value));
	}

}
