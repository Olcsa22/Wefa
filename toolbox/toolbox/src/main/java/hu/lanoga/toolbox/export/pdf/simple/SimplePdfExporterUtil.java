package hu.lanoga.toolbox.export.pdf.simple;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.exception.ToolboxInterruptedException;
import hu.lanoga.toolbox.export.ExporterException;
import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.util.ToolboxStringUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link I18nUtil#getLoggedInUserTimeZone()}-nal működik (minden {@link Timestamp} úgy kerül a PDF-be, {@link java.sql.Date}-ek viszont nincsenek állítva ilyen módon)
 */
@Slf4j
public class SimplePdfExporterUtil {

	/**
	 * oldalszám beszúrása az oldalak fejlécébe...
	 */
	public static class PdfHeader extends PdfPageEventHelper {

		@Override
		public void onEndPage(final PdfWriter writer, final Document document) {
			try {
				final Rectangle pageSize = document.getPageSize();
				ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER, new Phrase(String.format("- %s -", String.valueOf(writer.getCurrentPageNumber()))),
						pageSize.getWidth() / 2, pageSize.getBottom(30), 0);

			} catch (final Exception e) {
				log.error("Simple PDF Report generate page number error!", e);
				throw new ExporterException("Simple PDF Report generate page number error!", e);
			}
		}

	}

	// --

	/**
	 * fent a report title-je
	 */
	public static final String KEY_REPORT_NAME = "REPORT_NAME";

	/**
	 * List<Map<String, Object>> érték, ide mennek a beszúrandó értékek
	 */
	public static final String KEY_REPORT_VALUE = "REPORT_VALUE";

	/**
	 * recordFieldAsTableHeader érték azonosítására szolgáló kulcs, tehát az egyes rekordokból képzett táblázatok header értéke
	 */
	public static final String KEY_TABLE_NAME = "TABLE_NAME";

	/**
	 * List<Map<String, Object>> table érték, tehát egy rekordból (egy entitás objektum), képzett táblázat értékei (minden Map a listában megfelel egy sornak a táblázatban)
	 */
	public static final String KEY_TABLE_VALUE = "TABLE_VALUE";

	/**
	 * boolean érték, szükséges-e kiemelni az első sort headerként
	 */
	public static final String KEY_TABLE_IS_HEADER_FIRST_ROW = "KEY_TABLE_IS_HEADER_FIRST_ROW";

	/**
	 * boolean érték, szükséges-e kiemelni az első oszlopot headerként
	 */
	public static final String KEY_TABLE_IS_HEADER_FIRST_COL = "KEY_TABLE_IS_HEADER_FIRST_COL";

	// --

	private SimplePdfExporterUtil() {
		//
	}

	/*
	 * public static void testReport(final File outputFile) {
	 * 
	 * final LoremIpsum loremIpsum = new LoremIpsum();
	 * 
	 * final Map<String, Object> map = new HashMap<>();
	 * map.put(KEY_REPORT_NAME, loremIpsum.getWords(2));
	 * 
	 * final List<Map<String, Object>> tables = new ArrayList<>();
	 * 
	 * for (int i = 0; i < 8; i++) {
	 * 
	 * final Map<String, Object> tableMap = new HashMap<>();
	 * tableMap.put(KEY_TABLE_NAME, loremIpsum.getWords(4));
	 * tableMap.put(KEY_TABLE_IS_HEADER_FIRST_ROW, true);
	 * tableMap.put(KEY_TABLE_IS_HEADER_FIRST_COL, true);
	 * 
	 * final List<Map<Integer, Object>> table = new ArrayList<>();
	 * 
	 * for (int j = 0; j < 6; j++) {
	 * 
	 * final Map<Integer, Object> recordMap = new HashMap<>();
	 * 
	 * for (int k = 0; k < 2; k++) {
	 * 
	 * // if (j == 0 && k == 0) {
	 * // recordMap.put(k, "");
	 * // } else {
	 * // recordMap.put(k, loremIpsum.getWords(1, 5));
	 * // }
	 * 
	 * if (k == 0) {
	 * recordMap.put(k, loremIpsum.getWords(1, 2));
	 * } else {
	 * recordMap.put(k, loremIpsum.getWords(1, 100));
	 * }
	 * }
	 * 
	 * table.add(recordMap);
	 * }
	 * 
	 * tableMap.put(KEY_TABLE_VALUE, table);
	 * 
	 * tables.add(tableMap);
	 * }
	 * 
	 * map.put(KEY_REPORT_VALUE, tables);
	 * 
	 * generateReport(outputFile, map);
	 * }
	 */

	/**
	 * @param outputFile
	 * @param recordList
	 * @param modelClass
	 * @param recordFieldAsTableHeader ez a field lesz a recordok (ami egy-egy táblázat) fejléce (null esetén default az "id")
	 * @param <T>
	 */
	@SuppressWarnings("unchecked")
	public static <T> void generateReport(final File outputFile, final List<T> recordList, final Class<T> modelClass, final String recordFieldAsTableHeader) {

		try {

			final Map<String, Object> map = new HashMap<>();

			map.put(KEY_REPORT_NAME, ToolboxStringUtil.camelCaseToUnderscore(modelClass.getSimpleName()));

			final List<Map<String, Object>> tables = new ArrayList<>();

			for (final T record : recordList) {
				
				if (Thread.currentThread().isInterrupted()) {
					
					// fontos, különben memory-leak lehet
					// a timeout (vagy gomb) future.cancel() csak akkor tud érvényesülni,
					
					throw new ToolboxInterruptedException("XLSX generation was interrupted (cancelled?)!");
				}

				final Map<String, Object> tableMap = new HashMap<>();
				tableMap.put(KEY_TABLE_IS_HEADER_FIRST_COL, true);

				final ObjectMapper objectMapper = new ObjectMapper();

				objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

				DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, I18nUtil.getLoggedInUserLocale());
				dateFormat.setTimeZone(I18nUtil.getLoggedInUserTimeZone());
				objectMapper.setDateFormat(dateFormat);

				final Map<String, Object> t1 = objectMapper.convertValue(record, Map.class);

				removeExporterIgnoreValues(record, t1);

				final List<Map<String, Object>> table = new ArrayList<>();

				for (final Map.Entry<String, Object> entry : t1.entrySet()) {

					final Map<String, Object> h = new HashMap<>();
					h.put("0", entry.getKey());
					h.put("1", entry.getValue());

					table.add(h);
				}

				generateTableName(tableMap, t1, recordFieldAsTableHeader);

				tableMap.put(KEY_TABLE_VALUE, table);

				tables.add(tableMap);
			}

			map.put(KEY_REPORT_VALUE, tables);

			// ---

			generateReport(outputFile, map);

		} catch (Exception e) {
			log.error("Simple PDF Report create error!", e);
			throw new ExporterException("Simple PDF Report create error!", e);
		}
	}

	@SuppressWarnings({ "resource", "unchecked" })
	public static void generateReport(final File outputFile, final Map<String, Object> map) {

		Document doc = null;
		OutputStream outputStream = null;
		PdfWriter writer = null;

		try {

			doc = new Document();
			outputStream = new BufferedOutputStream(new FileOutputStream(outputFile), 64 * 1024);
			writer = PdfWriter.getInstance(doc, outputStream);

			final PdfHeader event = new PdfHeader();
			writer.setPageEvent(event);

			doc.open();

			addReportName(doc, map);

			final List<Map<String, Object>> tables = (List<Map<String, Object>>) map.get(KEY_REPORT_VALUE);

			if (tables != null) {

				for (final Map<String, Object> tableMap : tables) {

					boolean isFirstRowHeader = false;
					if (tableMap.containsKey(KEY_TABLE_IS_HEADER_FIRST_ROW) && ((Boolean) tableMap.get(KEY_TABLE_IS_HEADER_FIRST_ROW))) {
						isFirstRowHeader = true;
					}

					boolean isFirstColHeader = false;
					if (tableMap.containsKey(KEY_TABLE_IS_HEADER_FIRST_COL) && ((Boolean) tableMap.get(KEY_TABLE_IS_HEADER_FIRST_COL))) {
						isFirstColHeader = true;
					}

					addTableName(doc, tableMap);

					final List<Map<String, Object>> table = (List<Map<String, Object>>) tableMap.get(KEY_TABLE_VALUE);

					int rowIndex = 0;
					for (final Map<String, Object> tableRecord : table) {

						final PdfPTable pdfPTable = new PdfPTable(tableRecord.size());
						pdfPTable.setWidthPercentage(100);

						int colIndex = 0;
						for (final Map.Entry<String, Object> entry : tableRecord.entrySet()) {

							addTableCell(entry, isNeededHeader(isFirstRowHeader, rowIndex, isFirstColHeader, colIndex), pdfPTable);

							colIndex++;
						}

						doc.add(pdfPTable);

						rowIndex++;
					}

				}

			}

			log.debug("Simple PDF Report success: " + outputFile);

		} catch (Exception e) {

			log.error("Simple PDF Report create error!", e);
			throw new ExporterException("Simple PDF Report create error!", e);

		} finally {

			if (doc != null) {
				try {
					doc.close();
				} catch (Exception e) {
					//
				}
			}

			if (writer != null) {
				try {
					writer.close();
				} catch (Exception e) {
					//
				}
			}

			IOUtils.closeQuietly(outputStream);

		}
	}

	private static <T> void removeExporterIgnoreValues(final T record, final Map<String, Object> m) {

		final List<Field> allFields = FieldUtils.getAllFieldsList(record.getClass());

		for (final Field field : allFields) {

			final java.lang.annotation.Annotation[] annotations = field.getAnnotations();
			for (final java.lang.annotation.Annotation annotation : annotations) {
				if (annotation.annotationType().equals(ExporterIgnore.class)) {
					m.remove(field.getName());
				}
			}
		}

	}

	private static void addReportName(final Document doc, final Map<String, Object> map) {

		try {

			if (map.containsKey(KEY_REPORT_NAME) && StringUtils.isNotBlank(map.get(KEY_REPORT_NAME).toString())) {

				final Chunk chunk = new Chunk(map.get(KEY_REPORT_NAME).toString(), getFont(30, Font.BOLD));

				final Paragraph paragraph = new Paragraph();
				paragraph.add(chunk);
				paragraph.setAlignment(Element.ALIGN_CENTER);
				paragraph.setSpacingAfter(42);

				doc.add(paragraph);
			}

			final Chunk chunk = new Chunk("EXPORT DATE: " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()), getFont(16, Font.NORMAL));

			final Paragraph paragraph = new Paragraph();
			paragraph.add(chunk);
			paragraph.setSpacingAfter(20);

			doc.add(paragraph);

		} catch (Exception e) {
			throw new ToolboxGeneralException(e);
		}
	}

	private static void generateTableName(final Map<String, Object> tableMap, final Map<String, Object> t1, final String recordFieldAsTableHeader) {
		if (t1 != null && t1.containsKey(recordFieldAsTableHeader) && StringUtils.isNotBlank(getValueString(t1.get(recordFieldAsTableHeader)))) {
			tableMap.put(KEY_TABLE_NAME, getValueString(recordFieldAsTableHeader.toUpperCase() + ": " + t1.get(recordFieldAsTableHeader)));
		} else if (t1 != null && t1.containsKey("id") && StringUtils.isNotBlank(getValueString(t1.get("id")))) {
			tableMap.put(KEY_TABLE_NAME, getValueString("ID: " + t1.get("id")));
		}
	}

	private static void addTableName(final Document doc, final Map<String, Object> tableMap) {
		try {
			final Paragraph paragraph = new Paragraph();

			if (tableMap.containsKey(KEY_TABLE_NAME) && StringUtils.isNotBlank(tableMap.get(KEY_TABLE_NAME).toString())) {
				final Chunk chunk = new Chunk(tableMap.get(KEY_TABLE_NAME).toString(), getFont(14, Font.NORMAL));
				paragraph.add(chunk);
			} else {
				paragraph.add(new Chunk(" "));
			}

			paragraph.setSpacingBefore(32);
			paragraph.setSpacingAfter(12);

			doc.add(paragraph);

		} catch (Exception e) {
			throw new ToolboxGeneralException(e);
		}
	}

	private static void addTableCell(final Map.Entry<String, Object> entry, final boolean isNeededHeader, final PdfPTable pdfPTable) {

		String text = getValueString(entry.getValue());

		Font font;

		if (isNeededHeader) {
			font = getFont(8, Font.BOLD);
			text = StringUtils.upperCase(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(text), StringUtils.SPACE));
		} else {
			font = getFont(8, Font.NORMAL);
		}

		final Chunk chunk = new Chunk(text, font);

		final Paragraph paragraph = new Paragraph();
		paragraph.add(chunk);
		paragraph.setAlignment(Element.ALIGN_TOP);
		paragraph.setSpacingAfter(8);

		final PdfPCell pdfPCell = new PdfPCell();
		pdfPCell.addElement(paragraph);
		pdfPCell.setPadding(8);

		if (isNeededHeader) {

			pdfPCell.setBackgroundColor(Color.LIGHT_GRAY);
		}

		pdfPTable.addCell(pdfPCell);

	}

	private static boolean isNeededHeader(final boolean isFirstRowHeader, final int rowIndex, final boolean isFirstColHeader, final int colIndex) {
		return ((isFirstRowHeader && rowIndex == 0) || (isFirstColHeader && colIndex == 0));
	}

	private static String getValueString(final Object value) {

		if (value == null) {
			return "-";
		}

		try {
			return value.toString();
		} catch (final Exception e) {
			//
		}

		return "";

	}

	private static Font getFont(final int fontSize, final int fontStyle) {
		try {
			return FontFactory.getFont("/fonts/LiberationSans-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, fontSize, fontStyle);
		} catch (final Exception e) {
			log.error("Simple PDF Report create error (missing LiberationSans-Regular font)!", e);
			throw new ExporterException("Simple PDF Report create error (missing LiberationSans-Regular font)!", e);

		}
	}
}
