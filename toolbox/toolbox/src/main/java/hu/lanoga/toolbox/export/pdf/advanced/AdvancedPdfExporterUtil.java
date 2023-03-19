package hu.lanoga.toolbox.export.pdf.advanced;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.export.ExporterException;
import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.util.ToolboxStringUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Java -> HTML (Velocity) -> PDF alapú, 
 * mivel a generált PDF-ben csak azok a mezők fognak szerepelni, amelyekre a manuálisan létrehozott template fájl hivatkozik, 
 * ezért ez az exporter nem veszi figyelembe az {@link ExporterIgnore} annotációkat... 
 * {@link I18nUtil#getLoggedInUserTimeZone()}-nal működik (minden {@link Timestamp} úgy kerül a PDF-be, {@link java.sql.Date}-ek viszont nincsenek állítva ilyen módon)
 */
@Slf4j
public class AdvancedPdfExporterUtil {

	public static final String TEMPLATE_VM_PATH = "html_templates/advanced_report_templates/";

	/**
	 * Spring {@link PathMatchingResourcePatternResolver}-nek "ehető" legyen
	 */
	public static final String TEMPLATE_FONT_PATH = "classpath:fonts/LiberationSans-Regular.ttf";

	/**
	 * fent a report title-je
	 */
	public static final String KEY_REPORT_NAME = "REPORT_NAME";

	/**
	 * TreeMap érték, ide mennek a beszúrandó értékek
	 */
	public static final String KEY_REPORT_VALUE = "REPORT_VALUE";

	/**
	 * használandó Velocity template fájl {@link #TEMPLATE_VM_PATH} könyvtáron belül
	 */
	public static final String KEY_TEMPLATE_FILE_NAME = "TEMPLATE_FILE_NAME";

	@SuppressWarnings("unchecked")
	public static <T> void generateReport(final File targetPdf, final String templateFileName, final List<T> entities, final Class<T> modelClass) {

		try {

			int rowIndex = 0;

			final Map<String, Object> recordMap = new TreeMap<>();

			final ObjectMapper objectMapper = new ObjectMapper();
			
			objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
			
			DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, I18nUtil.getLoggedInUserLocale());
			dateFormat.setTimeZone(I18nUtil.getLoggedInUserTimeZone());
			objectMapper.setDateFormat(dateFormat);

			for (final T record : entities) {

				final TreeMap<String, Object> t = objectMapper.convertValue(record, TreeMap.class);

				recordMap.put("R_" + rowIndex, t);
				rowIndex++;
			}

			final Map<String, Object> paramMap = new HashMap<>();
			paramMap.put(KEY_REPORT_VALUE, recordMap);
			paramMap.put(KEY_REPORT_NAME, ToolboxStringUtil.camelCaseToUnderscore(modelClass.getSimpleName()));
			paramMap.put(KEY_TEMPLATE_FILE_NAME, templateFileName);

			generateReport(targetPdf, paramMap);

		} catch (final Exception e) {
			throw new ExporterException("generateReport error", e);
		}
	}

	public static void generateReport(final File targetPdf, final Map<String, Object> paramMap) {

		Pair<File, File> tmpFiles = null;

		try {

			tmpFiles = createHtmlReport(paramMap);

			log.debug("generateReport - HTML created: " + tmpFiles.getLeft() + ", " + tmpFiles.getRight());

			createPdfReport(tmpFiles.getRight(), targetPdf);

			log.debug("generateReport - PDF created: " + targetPdf);

		} catch (final Exception e) {
			throw new ExporterException("generateReport error", e);
		} finally {

			if (tmpFiles != null) {
				FileUtils.deleteQuietly(tmpFiles.getLeft());
				FileUtils.deleteQuietly(tmpFiles.getRight());
			}

		}

	}

	private static Pair<File, File> createHtmlReport(final Map<String, Object> paramMap) throws IOException {

		final VelocityEngine velocity = new VelocityEngine();
		velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		velocity.init();

		// ---

		File tmpFontFile;

		try (InputStream is = new PathMatchingResourcePatternResolver().getResources(TEMPLATE_FONT_PATH)[0].getInputStream()) {

			tmpFontFile = File.createTempFile("LiberationSans-Regular", ".ttf"); 
			
			// TODO: a File.createTempFile() és/vagy Files.createTempDirectory() helyett kellene egy saját mechanizmus arra is, ha tmp könyvtárra van szükség (most a FileDescriptor megoldásunk ilyet nem tud)
			// TODO: van már, de be kell hozni ide RapidTmpHelper
			
			tmpFontFile.deleteOnExit();

			FileUtils.copyInputStreamToFile(is, tmpFontFile);

		} catch (final Exception e) {
			throw new ExporterException("Load font file (copy to temp) error!", e);
		}

		// ---

		final VelocityContext velocityContext = new VelocityContext();
		velocityContext.put("templateVmPath", TEMPLATE_VM_PATH);
		velocityContext.put("fontFileFilename", tmpFontFile.getName());

		for (final Map.Entry<String, Object> entry : paramMap.entrySet()) {
			velocityContext.put(entry.getKey(), entry.getValue());
		}

		// ---

		final String templateFileName = (String) paramMap.get(KEY_TEMPLATE_FILE_NAME);

		final File tmpHtmlFile = File.createTempFile("LiberationSans-Regular", ".ttf"); // TODO: ez miért LiberationSans-Regular.ttf? ez a html fájl nem? (talán csak így ment valamiért?)
		tmpHtmlFile.deleteOnExit();

		try (final FileWriter writer = new FileWriter(tmpHtmlFile)) {
			velocity.getTemplate(TEMPLATE_VM_PATH + templateFileName, "UTF-8").merge(velocityContext, writer);
			writer.flush();
		}

		return Pair.of(tmpFontFile, tmpHtmlFile);
	}

	private static void createPdfReport(final File generatedHtml, final File targetPdf) {

		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(targetPdf), 64 * 1024)) {

			final ITextRenderer renderer = new ITextRenderer();
			renderer.setDocument(generatedHtml);
			renderer.layout();
			renderer.createPDF(os);

		} catch (Exception e) {
			throw new ToolboxGeneralException(e);
		}

	}

	private AdvancedPdfExporterUtil() {
		//
	}

}
