package hu.lanoga.toolbox.export.xlsx;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import hu.lanoga.toolbox.exception.ToolboxInterruptedException;
import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.util.DateTimeUtil;
import hu.lanoga.toolbox.util.ToolboxAssert;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link I18nUtil#getLoggedInUserTimeZone()}-nal működik (minden {@link Timestamp} úgy kerül a PDF-be, 
 * {@link java.sql.Date}-ek viszont nincsenek állítva ilyen módon)
 */
@SuppressWarnings({ "unused", "deprecation", "rawtypes" })
@Slf4j
public class XlsxExporterUtil {

	private XlsxExporterUtil() {
		//
	}

	/**
	 * üres xlsx
	 */
	private static final String SOURCE_FILE = "classpath:doc_templates/xlsx_export/empty_template.xlsx";

	/**
	 * @param targetFile
	 * @param list
	 * @param dataClass
	 * @param dateCellTimeZone
	 *            null esetén {@link I18nUtil#getLoggedInUserTimeZone()}
	 */

	public static void generateXlsxViaReflection(final File targetFile, final List list, final Class dataClass, TimeZone dateCellTimeZone) {

		log.debug("fillTemplate started, sourceFile: " + SOURCE_FILE + ", targetFile: " + targetFile);

		ToolboxAssert.notNull(targetFile);
		ToolboxAssert.notNull(list);

		if (dateCellTimeZone == null) {
			dateCellTimeZone = I18nUtil.getLoggedInUserTimeZone();
		}

		// ---

		final Resource sourceResource;

		try {

			sourceResource = new PathMatchingResourcePatternResolver().getResources(SOURCE_FILE)[0];

		} catch (final Exception e) {

			final XlsxExporterException ex = new XlsxExporterException("Missing sourceFile (template): " + SOURCE_FILE, e);
			log.error("fillTemplate error", ex);
			throw ex;

		}

		// ---

		try (InputStream in = new BufferedInputStream(sourceResource.getInputStream(), 131072)) {

			try (XSSFWorkbook workbook = new XSSFWorkbook(OPCPackage.open(in))) {

				fillWithData(workbook, list, dataClass, dateCellTimeZone);
				writeToXlsx(targetFile, workbook);

			}

		} catch (final ToolboxInterruptedException e) {
			
			log.error("fillTemplate interrupted", e);
			throw e;
			
		} catch (final XlsxExporterException e) {

			log.error("fillTemplate error", e);
			throw e;

		} catch (final Exception e) {

			final XlsxExporterException ex = new XlsxExporterException("Fill template general error!", e);
			log.error("fillTemplate error", ex);
			throw ex;

		}

		log.debug("fillTemplate finished, sourceFile: " + SOURCE_FILE + ", targetFile: " + targetFile);

	}

	@SuppressWarnings("cast")
	private static void fillWithData(final XSSFWorkbook workbook, final List list, final Class dataClass, final TimeZone dateCellTimeZone) throws IllegalAccessException {
		
		// final Map<String, XSSFCellStyle> cellStyleCacheMap = new HashMap<>(); // TODO: itt is legyen ilyen cache dolog, mint Mapfre2-ben van
			
		// ---
		
		final Map<String, List<Object>> data = new TreeMap<>();

		int rowId = 0;

		final int sizeWithHeader = list.size() + 1;

		final int[] rowNum = new int[sizeWithHeader];

		final List<Field> allFields = FieldUtils.getAllFieldsList(dataClass);

		ArrayList<Object> fields = new ArrayList<>();

		// HEADER
		for (final Field field : allFields) {

			final String name = StringUtils.upperCase(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(field.getName()), StringUtils.SPACE));

			boolean skipField = false;

			final Annotation[] annotations = field.getAnnotations();
			for (final Annotation annotation : annotations) {
				if (annotation.annotationType().equals(ExporterIgnore.class)) {
					skipField = true;
				}
			}
			if (!skipField) {
				fields.add(name);
			}
		}

		data.put(String.format("%09d", rowId), fields);
		rowId++;

		// TODO: itt a ciklusokon lehetne még optimalizálni, de egyelőre elmegy így is

		// DATA
		for (int i = 0; i < list.size(); i++) {
			fields = new ArrayList<>();

			for (final Field field : allFields) {

				field.setAccessible(true);

				final Object value = field.get(list.get(i));

				boolean skipField = false;

				final Annotation[] annotations = field.getAnnotations();
				for (final Annotation annotation : annotations) {
					if (annotation.annotationType().equals(ExporterIgnore.class)) {
						skipField = true;
						break;
					}
				}

				if (!skipField) {

					final Type type = field.getAnnotatedType().getType();
					final String typeString = type.toString();

					if (typeString.equals("class java.sql.Date")) {
						if (value != null) {

							// itt direkt nincs időzóna kezelés (úgy csináljuk, hogy a nap az mindig az a nap)

							fields.add((java.sql.Date) value); // kell a cast itt
						} else {
							fields.add(value);
						}
					} else if (typeString.equals("class java.sql.Timestamp")) {
						if (value != null) {
							final Timestamp date = DateTimeUtil.adjustTimeZone((Timestamp) value, TimeZone.getDefault().toZoneId(), dateCellTimeZone.toZoneId());
							fields.add(date);
						} else {
							fields.add(value);
						}
					} else {
						fields.add(value);
					}
				}

			}

			data.put(String.format("%09d", rowId), fields);
			rowId++;
			rowNum[i + 1] = rowId;
		}

		// XLSX
		final XSSFSheet mySheet = workbook.getSheetAt(0);
		int rownum = mySheet.getLastRowNum();

		final Set<String> newRows = data.keySet();

		for (final String key : newRows) {
			
			if (Thread.currentThread().isInterrupted()) {
				
				// fontos, különben memory-leak lehet
				// a timeout (vagy gomb) future.cancel() csak akkor tud érvényesülni,
				
				throw new ToolboxInterruptedException("XLSX generation was interrupted (cancelled?)!");
			}
			
			final Row row = mySheet.createRow(rownum++);

			final ArrayList<Object> objArr = (ArrayList<Object>) data.get(key);
			int cellnum = 0;
			for (final Object obj : objArr) {
								
				final Cell cell = row.createCell(cellnum++);

				final XSSFCellStyle cellStyle = setActualCellStyle(rownum, rowNum, cellnum, workbook, obj);
				if (cellStyle != null) {
					cell.setCellStyle(cellStyle);
				}

				if (obj instanceof String) {
					cell.setCellValue((String) obj);
				} else if (obj instanceof Double) {
					cell.setCellValue((Double) obj);
				} else if (obj instanceof Integer) {
					cell.setCellValue((Integer) obj);
				} else if (obj instanceof Long) {
					cell.setCellValue((Long) obj);
				} else if (obj instanceof Boolean) {
					cell.setCellValue((Boolean) obj);
				} else if (obj instanceof Date) {
					cell.setCellValue((Date) obj);
				} else if (obj instanceof BigDecimal) {
					cell.setCellValue(((BigDecimal) obj).doubleValue());
				}
			}
		}

	}

	private static XSSFCellStyle setActualCellStyle(final int rownum, final int[] rns, final int cellnum, final XSSFWorkbook workbook, final Object obj) {

		String content = "";
		if ((obj != null) && !String.valueOf(obj).equals("")) {
			content = String.valueOf(obj);
		}

		final XSSFCellStyle cellStyle = workbook.createCellStyle();

		if (obj instanceof Integer || obj instanceof Long || obj instanceof BigInteger) {
			cellStyle.setDataFormat(BuiltinFormats.getBuiltinFormat("0"));
		} else if (obj instanceof BigDecimal || obj instanceof Float || obj instanceof Double) {
			cellStyle.setDataFormat(BuiltinFormats.getBuiltinFormat("0.00"));
		} else if (obj instanceof Timestamp) {
			cellStyle.setDataFormat(BuiltinFormats.getBuiltinFormat("m/d/yy h:mm")); // nincs olyan POI lehetőség, ahol a nap és a másdodperc is benne lenne...
		} else if (obj instanceof Date) {
			cellStyle.setDataFormat(BuiltinFormats.getBuiltinFormat("m/d/yy"));
		}

		setBasicCS(cellStyle);

		// header
		if (rownum == 1) {
			setHeaderStyle(cellStyle, workbook);
		}

		if (rownum > 1) {
			if (!content.equals("") && (cellnum > 1)) {
				// van tartalom
				if (containsRowNum(rownum, rns)) {
					setUserContentStyle(cellStyle, workbook);
					setBasicCS(cellStyle);
				} else {
					setProjectContentStyle(cellStyle);
					setBasicCS(cellStyle);
				}
			} else {
				if (containsRowNum(rownum, rns)) {
					setUserStyle(cellStyle);
				} else {
					setProjectStyle(cellStyle);
				}
			}
		}

		return cellStyle;
	}

	private static boolean containsRowNum(final int actowNum, final int[] rns) {

		for (int i = 0; i < rns.length; i++) {
			if (rns[i] == actowNum) {
				return true;
			}
		}

		return false;
	}

	private static void setHeaderStyle(final XSSFCellStyle cellStyle, final XSSFWorkbook workbook) {
		cellStyle.setBorderTop(BorderStyle.THIN);
		cellStyle.setBorderBottom(BorderStyle.THIN);

		cellStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
		cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);

		final Font headerFont = workbook.createFont();
		headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
		headerFont.setColor(IndexedColors.WHITE.getIndex());
		cellStyle.setFont(headerFont);
	}

	private static void setBoldStyle(final XSSFCellStyle cellStyle, final XSSFWorkbook workbook) {
		final Font headerFont = workbook.createFont();
		headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
		cellStyle.setFont(headerFont);
	}

	private static void setUserStyle(final XSSFCellStyle cellStyle) {
		cellStyle.setBorderTop(BorderStyle.THIN);
		cellStyle.setBorderBottom(BorderStyle.THIN);

		cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
		cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
	}

	private static void setProjectStyle(final XSSFCellStyle cellStyle) {
		cellStyle.setBorderBottom(BorderStyle.THIN);
	}

	private static void setUserContentStyle(final XSSFCellStyle cellStyle, final XSSFWorkbook workbook) {
		cellStyle.setBorderTop(BorderStyle.THIN);
		cellStyle.setBorderBottom(BorderStyle.THIN);

		cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
		cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);

		final Font headerFont = workbook.createFont();
		// headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
		headerFont.setColor(IndexedColors.BLACK.getIndex());
		cellStyle.setFont(headerFont);

		// setBoldStyle(cellStyle, workbook);
	}

	private static void setProjectContentStyle(final XSSFCellStyle cellStyle) {
		cellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
		cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);

		cellStyle.setBorderBottom(BorderStyle.THIN);
	}

	private static void setBasicCS(final XSSFCellStyle cellStyle) {
		cellStyle.setBorderLeft(BorderStyle.THIN);
		cellStyle.setBorderRight(BorderStyle.THIN);

		cellStyle.setAlignment(HorizontalAlignment.CENTER);
	}

	private static void writeToXlsx(final File targetFile, final XSSFWorkbook workbook) {

		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile), 64 * 1024)) {

			FileUtils.forceMkdirParent(targetFile);
			workbook.write(out);

		} catch (final IOException e) {
			throw new XlsxExporterException("XLSX write error (POI)!", e);
		}

	}
}
