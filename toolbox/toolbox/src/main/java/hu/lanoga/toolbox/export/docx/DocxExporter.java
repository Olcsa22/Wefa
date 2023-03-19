package hu.lanoga.toolbox.export.docx;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.HashBasedTable;
import com.google.common.util.concurrent.Striped;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.FileDescriptorLocationType;
import hu.lanoga.toolbox.ToolboxSysKeys.FileDescriptorSecurityType;
import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.export.SofficeHelper;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.util.ToolboxAssert;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;

/**
 * DOCX exporter, template behelyetesítés... 
 * mivel a generált fájlban csak azok a mezők fognak szerepelni, amelyekre a manuálisan létrehozott template fájl hivatkozik, 
 * ezért ez az exporter nem veszi figyelembe az {@link ExporterIgnore} annotációkat... 
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@Slf4j
public class DocxExporter {

	// TODO: időzónát tisztázni itt is (lásd többi Exporter)

	/**
	 * table template konstans
	 */
	public static final String TT_MAP_KEY_CONT_VAL = "<#>";

	/**
	 * table template konstans
	 */
	public static final String TT_MAP_KEY_CONTENT = "content";

	/**
	 * table template konstans
	 */
	public static final String TT_MAP_KEY_FOOTER = "footer";

	/**
	 * table template konstans
	 */
	public static final String TT_MAP_KEY_HEADER = "header";

	/**
	 * eredeti innen: https://stackoverflow.com/questions/22268898/replacing-a-text-in-apache-poi-xwpf
	 * (kisebb egyedi módosítások táblázat beszúrás stb.)
	 */
	private final class Replacer {
		
		// ne rakd field-eket ebbe az osztálya, mert lehet, hogy nem lesz thread-safe

		private Map<Integer, XWPFRun> getPosToRuns(final XWPFParagraph paragraph) {
			int pos = 0;
			final Map<Integer, XWPFRun> map = new HashMap<>(10);
			for (final XWPFRun run : paragraph.getRuns()) {
				final String runText = run.text();
				if (runText != null) {
					for (int i = 0; i < runText.length(); i++) {
						map.put(pos + i, run);
					}
					pos += runText.length();
				}
			}
			return (map);
		}

		private <V> void replace(final XWPFParagraph paragraph, final Map<String, V> map, final XWPFDocument document) {
			for (final Map.Entry<String, V> entry : map.entrySet()) {
				this.replace(paragraph, entry.getKey(), entry.getValue(), document);
			}
		}

		private <V> void replace(final XWPFParagraph paragraph, final String searchText, final V replacement, final XWPFDocument document) {

			boolean found = true;
			
			int loopSafetyStopCounter = 0;

			while (found && loopSafetyStopCounter < 1000) {

				loopSafetyStopCounter++;
				
				found = false;

				final int pos = paragraph.getText().indexOf(searchText);

				if ((replacement instanceof java.util.List) && ((List) replacement).isEmpty()) {
					final Map<Integer, XWPFRun> posToRuns = this.getPosToRuns(paragraph);
					final XWPFRun run = posToRuns.get(pos);
					final String text = "";
					final String[] texts = text.split("\n");
					if (pos >= 0) {
						run.setText(texts[0], 0);
					}
					break;
				}

				if ((replacement instanceof java.util.List) && (pos >= 0)) {

					found = true;

					final Map<Integer, XWPFRun> posToRuns = this.getPosToRuns(paragraph);
					final XWPFRun run = posToRuns.get(pos);
					final XWPFRun lastRun = posToRuns.get((pos + searchText.length()) - 1);
					final int runNum = paragraph.getRuns().indexOf(run);
					final int lastRunNum = paragraph.getRuns().indexOf(lastRun);

					for (final Object obj : (List) replacement) {

						// final String value = obj.toString();

						// ------------------------------ table/image

						String text;
						if (HashBasedTable.class.isInstance(obj)) {
							DocxExporter.this.insertTable(document, (HashBasedTable<Integer, Integer, String>) obj, ((HashBasedTable<Integer, Integer, String>) obj).rowKeySet().size(), ((HashBasedTable<Integer, Integer, String>) obj).columnKeySet().size(), paragraph);
							text = "";
						} else if (File.class.isInstance(obj)) {
							insertPicture((File) obj, run);
							text = "";
						} else {
							text = obj.toString();
						}

						// ------------------------------

						final String[] texts = text.split("\n");

						run.setText(texts[0], 0);
						XWPFRun newRun = run;
						for (int i = 1; i < texts.length; i++) {
							newRun.addCarriageReturn();
							newRun = paragraph.insertNewRun(runNum + i);
							/*
							 * We should copy all style attributes to the newRun from run also from background color, ... Here we duplicate only the simple attributes...
							 */
							newRun.setText(texts[i]);
							newRun.setBold(run.isBold());
							newRun.setCapitalized(run.isCapitalized());
							// newRun.setCharacterSpacing(run.getCharacterSpacing());
							newRun.setColor(run.getColor());
							newRun.setDoubleStrikethrough(run.isDoubleStrikeThrough());
							newRun.setEmbossed(run.isEmbossed());
							newRun.setFontFamily(run.getFontFamily());
							newRun.setFontSize(run.getFontSize());
							newRun.setImprinted(run.isImprinted());
							newRun.setItalic(run.isItalic());
							newRun.setKerning(run.getKerning());
							newRun.setShadow(run.isShadowed());
							newRun.setSmallCaps(run.isSmallCaps());
							newRun.setStrikeThrough(run.isStrikeThrough());
							newRun.setSubscript(run.getSubscript());
							newRun.setUnderline(run.getUnderline());
						}

						for (int i = (lastRunNum + texts.length) - 1; i > ((runNum + texts.length) - 1); i--) {
							paragraph.removeRun(i);
						}

					}

				} else if (pos >= 0) {

					found = true;
					final Map<Integer, XWPFRun> posToRuns = this.getPosToRuns(paragraph);
					final XWPFRun run = posToRuns.get(pos);
					final XWPFRun lastRun = posToRuns.get((pos + searchText.length()) - 1);
					final int runNum = paragraph.getRuns().indexOf(run);
					final int lastRunNum = paragraph.getRuns().indexOf(lastRun);

					// ------------------------------ table/image

					String text;
					if (HashBasedTable.class.isInstance(replacement)) {
						DocxExporter.this.insertTable(document, (HashBasedTable<Integer, Integer, String>) replacement, ((HashBasedTable<Integer, Integer, String>) replacement).rowKeySet().size(), ((HashBasedTable<Integer, Integer, String>) replacement).columnKeySet().size(), paragraph);
						text = "";
					} else if (File.class.isInstance(replacement)) {
						insertPicture((File) replacement, run);
						text = "";
					} else {
						text = replacement.toString();
					}

					// ------------------------------

					final String[] texts = text.split("\n");

					run.setText(texts[0], 0);
					XWPFRun newRun = run;
					for (int i = 1; i < texts.length; i++) {
						newRun.addCarriageReturn();
						newRun = paragraph.insertNewRun(runNum + i);
						/*
						 * We should copy all style attributes to the newRun from run also from background color, ... Here we duplicate only the simple attributes...
						 */
						newRun.setText(texts[i]);
						newRun.setBold(run.isBold());
						newRun.setCapitalized(run.isCapitalized());
						// newRun.setCharacterSpacing(run.getCharacterSpacing());
						newRun.setColor(run.getColor());
						newRun.setDoubleStrikethrough(run.isDoubleStrikeThrough());
						newRun.setEmbossed(run.isEmbossed());
						newRun.setFontFamily(run.getFontFamily());
						newRun.setFontSize(run.getFontSize());
						newRun.setImprinted(run.isImprinted());
						newRun.setItalic(run.isItalic());
						newRun.setKerning(run.getKerning());
						newRun.setShadow(run.isShadowed());
						newRun.setSmallCaps(run.isSmallCaps());
						newRun.setStrikeThrough(run.isStrikeThrough());
						newRun.setSubscript(run.getSubscript());
						newRun.setUnderline(run.getUnderline());
					}

					for (int i = (lastRunNum + texts.length) - 1; i > ((runNum + texts.length) - 1); i--) {
						paragraph.removeRun(i);
					}

				}

			}
		}

		<V> void replace(final XWPFDocument document, final Map<String, V> map) {

			java.util.List<XWPFParagraph> paragraphs = new ArrayList<>();

			paragraphs.addAll(document.getParagraphs());

			for (XWPFHeader h : document.getHeaderList()) {
				paragraphs.addAll(h.getParagraphs());
			}

			for (XWPFFooter f : document.getFooterList()) {
				paragraphs.addAll(f.getParagraphs());
			}

			for (final XWPFParagraph paragraph : paragraphs) {
				this.replace(paragraph, map, document);
			}

		}

	}

	/**
	 * A/4 odal, normáls margóval (pl.: táblázat szélesség erre lesz állítva)
	 */
	protected static final BigInteger DEFAULT_PAGE_WIDTH = BigInteger.valueOf(9072);

	/**
	 * A/4 odal, normáls margóval kb. jó
	 */
	protected static final Dimension DEFAULT_IMG_BOUNDS = new Dimension(200, 200);

	private static Integer determinePictureType(final File file) {

		if (FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("JPG")) {
			return Document.PICTURE_TYPE_JPEG;
		}

		if (FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("PNG")) {
			return Document.PICTURE_TYPE_PNG;
		}

		if (FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("GIF")) {
			return Document.PICTURE_TYPE_GIF;
		}

		return null;
	}

	private static void insertPicture(final File file, final XWPFRun run) {

		if (determinePictureType(file) != null) {

			try {

				byte[] ba = null;

				try (InputStream is = new BufferedInputStream(new FileInputStream(file), 64 * 1024); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

					Thumbnails.of(is).size(DEFAULT_IMG_BOUNDS.width, DEFAULT_IMG_BOUNDS.height).outputFormat("JPEG").toOutputStream(baos);

					ba = baos.toByteArray();

				}

				try (ByteArrayInputStream bais = new ByteArrayInputStream(ba)) {

					final BufferedImage bi = ImageIO.read(bais);
					final int width = bi.getWidth();
					final int height = bi.getHeight();

					bais.reset();

					run.addBreak();
					run.addPicture(bais, Document.PICTURE_TYPE_JPEG, file.getName(), Units.pixelToEMU(width), Units.pixelToEMU(height));

				}

			} catch (final Exception e) {
				throw new DocxExporterException("Picture insert error!", e);
			}

		}
	}

	private static void writeToDocx(final File targetFile, final XWPFDocument document) {

		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile), 64 * 1024)) {

			FileUtils.forceMkdirParent(targetFile);
			document.write(out);

		} catch (final Exception e) {
			throw new DocxExporterException("DOCX write error (docx4j)!", e);
		}

	}

	// /**
	// * fr.opensagres.xdocreport változat
	// *
	// * @param sourceDocxFile
	// * @param targetFile
	// */
	// private static void writeToPdf(final File sourceDocxFile, final File targetFile) {
	//
	// // throw new UnsupportedOperationException("PDF conversion is not avaliable currently (work in progress...)!");
	//
	// try (InputStream is = new BufferedInputStream(new FileInputStream(sourceDocxFile), 64 * 1024)) {
	//
	// FileUtils.forceMkdirParent(targetFile);
	//
	// XWPFDocument document = new XWPFDocument(is);
	// PdfOptions options = PdfOptions.create();
	//
	// try (OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile), 64 * 1024)) {
	// PdfConverter.getInstance().convert(document, out, options);
	// }
	//
	// } catch (final Exception e) {
	// throw new DocxExporterException("PDF write error (fr.opensagres.xdocreport)!", e);
	// }
	//
	// }

	// /**
	// * docx4j (3.2.1) változat
	// *
	// * @param sourceDocxFile
	// * @param targetFile
	// */
	// private static void writeToPdf(final File sourceDocxFile, final File targetFile) {
	//
	// try (InputStream is = new BufferedInputStream(new FileInputStream(sourceDocxFile), 64 * 1024)) {
	//
	// FileUtils.forceMkdirParent(targetFile);
	//
	// final WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(is);
	//
	// final PdfSettings pdfSettings = new PdfSettings();
	// final PdfConversion conversion = new Conversion(wordMLPackage);
	//
	// try (OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile), 64 * 1024)) {
	// conversion.output(out, pdfSettings);
	// }
	//
	// } catch (final Exception e) {
	// throw new DocxExporterException("PDF write error (docx4j)!", e);
	// }
	//
	// }

	protected static final void setParagraphParams(final XWPFParagraph paragraph, final int space, final ParagraphAlignment paragraphAlignment, final TextAlignment textAlignment) {
		paragraph.setAlignment(paragraphAlignment);
		paragraph.setVerticalAlignment(textAlignment);
		paragraph.setSpacingBeforeLines(space);
		paragraph.setSpacingAfterLines(space);
		paragraph.setSpacingBefore(space);
		paragraph.setSpacingAfter(space);
	}

	/**
	 * a "run" egy dokumentumon belüli egységet jelent itt...
	 *
	 * @param run
	 * @param text
	 * @param fontFamily
	 * @param fontSize
	 * @param colorRGB
	 * @param bold
	 * @param addBreak
	 */
	protected static final void setRunParams(final XWPFRun run, final String text, final String fontFamily, final int fontSize, final String colorRGB, final boolean bold, final boolean addBreak) {

		run.setFontFamily(fontFamily);
		run.setFontSize(fontSize);
		run.setColor(colorRGB);
		run.setText(text);
		run.setBold(bold);
		run.setTextPosition(1);

		if (addBreak) {
			run.addBreak();
		}

	}

	protected static final void setTableAlignment(final XWPFTable table, final STJc.Enum justification) {
		final CTTblPr tblPr = table.getCTTbl().getTblPr();
		final CTJc jc = (tblPr.isSetJc() ? tblPr.getJc() : tblPr.addNewJc());
		jc.setVal(justification);
	}

	private static final Striped<Lock> sourceResourceCacheLock = Striped.lock(5);
	private static final Cache<String, byte[]> sourceResourceCache = CacheBuilder.newBuilder().maximumSize(10).concurrencyLevel(4).expireAfterWrite(15, TimeUnit.MINUTES).build();

	private final Replacer replacer = new Replacer(); // elvileg thread-safe, mert nincs belső állapota

	/**
	 * DOCX template kitöltése, PDF-re konvertálása...
	 *
	 * @param sourceFileStr
	 *            template DOCX fájl helye;
	 *            bármi, amit a {@link PathMatchingResourcePatternResolver} kezelni tud,
	 *            pl.: "classpath:doc_templates/invoice_templates/inv1.docx",
	 *            lehet fájlrendszerre is mutatni "file:..." formában)
	 * @param targetDocx
	 *            ide lesz mentve az elkészült (kitöltött) DOCX fájl; targetDocx/targetPdf legalább egyike nem null kell legyen
	 * @param targetPdf
	 *            ide lesz mentve az elkészült (kitöltött és PDF-re konvertált) DOCX fájl; targetDocx/targetPdf legalább egyike nem null kell legyen
	 * @param templParamMap
	 *            <pre>
	 *            többféle mód is van:
	 *            1) egyszerű string érték: sima replace a key alapján (a template DOCX-ben <# kulcs #> helyére van behelyettítve, több helyen is lehet ugyanez, mindhez be lesz helyettesítve);
	 *            2) képfájl (File típus) érték: kép beszúráshoz, a key alapján;
	 *            3) com.google.common.collect.HashBasedTable<Integer, Integer, String> érték: generált táblázat beszúrásához (generálás egyes metódusai felülírhatók, egyedi stílus stb.);
	 *            Integer: i, Integer: j mező koordináták, String: a mező tartalma (ez is key alapján működik, oda fogja beszűrni a generált táblázatot);
	 *            4) TT (TableTemplate) esetén (= másik, nem generált táblázat mód) (a táblázat fejléc stb. megadása a template DOCX-ben történik):
	 *            A template DOCX fájlban az 1-3 módtól eltérően így kell megadni (tehát nem csak egy kulcs van): <#TT id = mintakulcs hrc=1 frc=1 #>ide jön a táblázat maga</#TT#>,
	 *            a hrc és az frc azt adja meg, hogy a táblázat hány sorát kell fejlécként, illetve láblécként használni (megj.: vertikálisan osztott header/footer cellák külön sornak számítanak TT tag atrribútumaiban: helyesen kell megadni!).
	 *            A templParamMap-ben az id (a példban "mintakulcs") kell legyen a key, az érték egy HashMap három elemmel:
	 *            a) MAP_KEY_HEADER -> header sor(ok)-hoz használt adat map (HashMap<String, String>) a kicserélendő String kulcsokkal/értékekkel (lásd fenti 1-es mód)
	 *            b) MAP_KEY_FOOTER -> footer sor(ok)-hoz használt adat map... (ugyanaz a "logika", mint a MAP_KEY_HEADER-nél)
	 *            c) MAP_KEY_CONTENT -> ez a "belső" részhez (ami nem a header, nem a footer), ez List<List<String>>; a belyettesítés a template DOCX-ben megadott <#> helyekre történik (ha egy body/content cellában nem ez van, akkor az a fejléchez/lábléchez hasonlóan fix cellaként lesz kezelve)
	 *            (lényeg, hogy itt a listákban haladva teszi be az értékeket sorban... itt nincs külön azonoító már a cellákban, ezt leszámítva hasonló mint a 1) mód, csak String lehet itt is a cellaérték)
	 *            (jelenleg nincs lehetőség többszörös content sorok használatára (csak egy content sor lehet), és egy cellában csak 1 kicserélendő adat lehet, minden mást töröl...)
	 *            </pre>
	 * @param docxExportEnginePdfMode
	 * 
	 * @see #TT_MAP_KEY_HEADER
	 * @see #TT_MAP_KEY_FOOTER
	 * @see #TT_MAP_KEY_CONTENT
	 * @see #TT_MAP_KEY_CONT_VAL
	 * 
	 * @see PathMatchingResourcePatternResolver
	 */
	public final void fillTemplate(final String sourceFileStr, final FileDescriptor targetDocx, final FileDescriptor targetPdf, final Map<String, Object> templParamMap, final ToolboxSysKeys.DocxExportEnginePdfMode docxExportEnginePdfMode) {

		// log.debug("fillTemplate started, sourceFile: " + sourceFile + ", targetDocx: " + targetDocx + ", targetPdf: " + targetPdf + ", docxExportEnginePdfMode: " + docxExportEnginePdfMode.toString());

		ToolboxAssert.notNull(sourceFileStr);
		ToolboxAssert.notNull(docxExportEnginePdfMode);
		ToolboxAssert.isTrue((targetDocx != null) || (targetPdf != null));
		ToolboxAssert.notNull(templParamMap);

		boolean isClasspathTemplate = sourceFileStr.contains("classpath:");

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

		try (InputStream in = new ByteArrayInputStream(ba)) {
			try (OPCPackage opcPackage = OPCPackage.open(in)) {
				try (XWPFDocument xwpfDocument = new XWPFDocument(opcPackage)) {

					// 1 - template mezők kitöltése

					if (!templParamMap.isEmpty()) {

						TableTemplateUtil.ttGen(xwpfDocument, templParamMap); // a - TT gen (ez egy külön dolog)

						this.replacer.replace(xwpfDocument, templParamMap); // b (sima param replace)
					}

					// 2 - mentés és/vagy konvertálás

					if (targetDocx != null) {
						writeToDocx(targetDocx.getFile(), xwpfDocument);
					}

					if (targetPdf != null) {

						FileDescriptor tmpTargetDocx = null;

						try {

							if (targetDocx == null) {
								tmpTargetDocx = ApplicationContextHelper.getBean(FileStoreService.class).createTmpFile2("tmp.docx", FileDescriptorLocationType.PROTECTED_FOLDER, FileDescriptorSecurityType.ADMIN_OR_CREATOR);
								writeToDocx(tmpTargetDocx.getFile(), xwpfDocument);
							}

							if (ToolboxSysKeys.DocxExportEnginePdfMode.WITH_SOFFICE.equals(docxExportEnginePdfMode)) {
								ApplicationContextHelper.getBean(SofficeHelper.class).convertToPdf(SofficeHelper.ConvertMode.PDF, targetDocx != null ? targetDocx : tmpTargetDocx, targetPdf);
							} else {
								final DocxExporterException ex = new DocxExporterException("Unimplemented engine type!");
								log.error("fillTemplate error", ex);
								throw ex;
							}

						} finally {
							if (tmpTargetDocx != null) {
								ApplicationContextHelper.getBean(FileStoreService.class).setToBeDeleted(tmpTargetDocx.getId()); // ilyenkor a docx-et csak tmp jelleggel hoztuk létre, ezért törölni kel (mj.: ha, van docx target is, akkor nem!)
							}
						}

					}

				}
			}

		} catch (final DocxExporterException e) {

			log.error("fillTemplate error", e);
			throw e;

		} catch (final Exception e) {

			final DocxExporterException ex = new DocxExporterException("Fill template general error!", e);
			log.error("fillTemplate error", ex);
			throw ex;

		}

		// log.debug("fillTemplate finished, sourceFile: " + sourceFile + ", targetDocx: " + targetDocx + ", targetPdf: " + targetPdf + ", docxExportEnginePdfMode: " + docxExportEnginePdfMode.toString());

	}

	private void insertTable(final XWPFDocument document, final HashBasedTable<Integer, Integer, String> hbt, final int rowNum, final int colNum, final XWPFParagraph paragraph) {

		final XWPFTable table = document.insertNewTbl(paragraph.getCTP().newCursor());
		this.buildTable(table, rowNum, colNum, hbt);
		this.setTableParams(table);

	}

	/**
	 * Jelenleg CV-het kellő táblázatra van kialakítva (beégetett formázások, igazítások stb.)
	 *
	 * @param table
	 * @param rowNum
	 * @param colNum
	 * @param hbt
	 */
	protected void buildTable(final XWPFTable table, final int rowNum, final int colNum, final HashBasedTable<Integer, Integer, String> hbt) {

		XWPFTableRow tableRow = null;
		for (int i = 0; i < rowNum; i++) {

			if (i == 0) {
				tableRow = table.getRow(0);
			} else {
				tableRow = table.createRow();
			}

			for (int j = 0; j < colNum; j++) {

				XWPFTableCell tableCell = null;

				if (i == 0) {

					if (j == 0) {
						tableCell = tableRow.getCell(0);
						tableCell.getCTTc().addNewTcPr().addNewTcW().setW(BigInteger.valueOf(4036));
					} else {
						tableCell = tableRow.addNewTableCell();

						tableCell.getCTTc().addNewTcPr().addNewTcW().setW(BigInteger.valueOf(4036));
					}

					final XWPFParagraph paragraph = tableCell.getParagraphs().get(0);

					setParagraphParams(paragraph, 40, ParagraphAlignment.LEFT, TextAlignment.CENTER);
					setTableAlignment(table, STJc.CENTER);

					setRunParams(paragraph.createRun(), hbt.get(i, j), "Arial", 12, "000000", true, false);

				} else {

					tableCell = tableRow.getCell(j);

					final XWPFParagraph paragraph = tableCell.getParagraphs().get(0);

					setParagraphParams(paragraph, 30, ParagraphAlignment.LEFT, TextAlignment.CENTER);

					setRunParams(paragraph.createRun(), hbt.get(i, j), "Arial", 12, "000000", false, false);
				}
			}
		}
	}

	protected void setTableParams(final XWPFTable table) {

		final CTTblWidth width = table.getCTTbl().addNewTblPr().addNewTblW();
		width.setType(STTblWidth.DXA);
		width.setW(DEFAULT_PAGE_WIDTH);

		final CTTblBorders borders = table.getCTTbl().addNewTblPr().addNewTblBorders();
		borders.addNewBottom().setVal(STBorder.BASIC_THIN_LINES);
		borders.addNewLeft().setVal(STBorder.BASIC_THIN_LINES);
		borders.addNewRight().setVal(STBorder.BASIC_THIN_LINES);
		borders.addNewTop().setVal(STBorder.BASIC_THIN_LINES);
		borders.addNewInsideH().setVal(STBorder.BASIC_THIN_LINES);
		borders.addNewInsideV().setVal(STBorder.BASIC_THIN_LINES);

	}

}
