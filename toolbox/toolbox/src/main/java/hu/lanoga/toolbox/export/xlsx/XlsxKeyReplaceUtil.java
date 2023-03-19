package hu.lanoga.toolbox.export.xlsx;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.common.annotations.VisibleForTesting;

import hu.lanoga.toolbox.util.DateTimeUtil;

public class XlsxKeyReplaceUtil {

	public static final String KEY_OPEN_TAG = "<@";
	public static final String KEY_CLOSE_TAG = "@>";

	private XlsxKeyReplaceUtil() {
		//
	}

	/**
	 * minta/teszt
	 * 
	 * @param workbook
	 * @param dateCellTimeZone
	 */
	@VisibleForTesting
	public static void fillWithDataSimpleTest(final XSSFWorkbook workbook, final TimeZone dateCellTimeZone) {

		Map<String, Object> map = new HashMap<>();

		map.put("title", "Árvíztűrő");

		map.put("a", new java.sql.Timestamp(System.currentTimeMillis()));
		map.put("b", RandomUtils.nextInt(0, 1000) - 500);
		map.put("c", System.currentTimeMillis());
		map.put("d", RandomUtils.nextDouble(0d, 1000d) - 500d);
		map.put("e", RandomUtils.nextFloat(0f, 1000f) - 500f);
		map.put("f", new BigDecimal("1234.56"));
		map.put("g", Boolean.TRUE);

		map.put("k1", RandomUtils.nextInt(1, 10));
		map.put("k2", RandomUtils.nextInt(1, 10));

		insertValuesIntoTemplate(map, workbook.getSheetAt(0), new CellReference("A1"), new CellReference("F100"), dateCellTimeZone, null);
	}

	/**
	 * @param map
	 * @param sheet
	 * @param cellFrom
	 * @param cellTo
	 * @param dateCellTimeZone
	 * 
	 * @deprecated
	 * @see #insertValuesIntoTemplate(Map, XSSFSheet, CellReference, CellReference, TimeZone, String)
	 */
	@Deprecated
	public static void insertValuesIntoTemplate(final Map<String, Object> map, XSSFSheet sheet, final CellReference cellFrom, final CellReference cellTo, final TimeZone dateCellTimeZone) {
		insertValuesIntoTemplate(map, sheet, cellFrom, cellTo, dateCellTimeZone, null);
	}

	/**
	 * újabb generációs megoldás... 
	 * a sheetbe (munkalap) behelyettesítés a map alapján  
	 * 
	 * @param map
	 * @param sheet
	 * @param cellFrom
	 * @param cellTo
	 * @param dateCellTimeZone
	 * @param stringForUnknownKeys
	 * 		ha van olyan key a template fájlban (ebben a cell tartományban), 
	 * 		ami a map-ben nincs, akkor oda mit tegyünk (pl.: "", "-"), 
	 * 		null esetén nincs replace (tehát a key/tag fog látszani)
	 * @see #KEY_OPEN_TAG
	 * @see #KEY_CLOSE_TAG
	 */
	public static void insertValuesIntoTemplate(final Map<String, Object> map, XSSFSheet sheet, final CellReference cellFrom, final CellReference cellTo, final TimeZone dateCellTimeZone, String stringForUnknownKeys) {

		for (int i = cellFrom.getRow(); i <= cellTo.getRow(); i++) {
			for (int j = cellFrom.getCol(); j <= cellTo.getCol(); j++) {

				final XSSFRow actualRow = sheet.getRow(i);

				if (actualRow == null) {
					continue;
				}

				final Cell actualCell = actualRow.getCell(j);

				if (actualCell == null || actualCell.getCellTypeEnum() != CellType.STRING || StringUtils.isBlank(actualCell.getStringCellValue())) {
					continue;
				}

				final String[] keys = StringUtils.substringsBetween(actualCell.getStringCellValue(), KEY_OPEN_TAG, KEY_CLOSE_TAG);

				if (keys == null || keys.length == 0) {
					continue;
				}

				final String keyStr = keys[0];

				// ---

				if (keyStr.startsWith("formula:")) {

					// mj.: mindig az angol függvénynév kell (ha magyar az office, akkor lehet, hogy kézi próba esetén hibásnak is írja!)
					// értsd: SUM vs SZUM.. formula:SUM(... a helyes

					String formulaStr = StringUtils.removeStart(keyStr, "formula:");
					actualCell.setCellType(CellType.FORMULA);
					actualCell.setCellFormula(formulaStr);
					continue;

				}

				// ---

				if (!map.containsKey(keyStr)) {

					if (stringForUnknownKeys != null) {
						actualCell.setCellType(CellType.STRING);
						actualCell.setCellValue(stringForUnknownKeys);
					}

					continue;
				}

				final Object val = map.get(keys[0]);

				if (val == null) {
					actualCell.setCellType(CellType.STRING);
					actualCell.setCellValue("");
					continue;
				}

				// ---

				// TODO: dátum típusokat tisztázni kell még jobban

				if (val instanceof Boolean) {

					actualCell.setCellType(CellType.BOOLEAN); // TODO: furán működik
					actualCell.setCellValue(((Boolean) val));

				} else if (val instanceof Integer) {

					actualCell.setCellType(CellType.NUMERIC);
					actualCell.setCellValue(((Integer) val));

				} else if (val instanceof Long) {

					actualCell.setCellType(CellType.NUMERIC);
					actualCell.setCellValue(((Long) val));

				} else if (val instanceof Float) {

					actualCell.setCellType(CellType.NUMERIC);
					actualCell.setCellValue(((Float) val));

				} else if (val instanceof Double) {

					// TODO: szükség esetén... style.setDataFormat(workbook.createDataFormat().getFormat("0.00"));

					actualCell.setCellType(CellType.NUMERIC);
					actualCell.setCellValue(((Double) val));

				} else if (val instanceof BigDecimal) {

					actualCell.setCellType(CellType.NUMERIC);
					actualCell.setCellValue(((BigDecimal) val).doubleValue());

				} else if (val instanceof java.sql.Date) {

					actualCell.setCellType(CellType.STRING);
					actualCell.setCellValue(((java.sql.Date) val).toString());

				} else if (val instanceof java.sql.Timestamp) {

					actualCell.setCellType(CellType.STRING);

					// hu.lanoga.toolbox.export.xlsx.XlsxExporterUtil.timestampConverterWithHoursAndMinutes((java.sql.Timestamp) val, dateCellTimeZone);

					actualCell.setCellValue(DateTimeUtil.dateConverterWithHoursAndMinutes((java.sql.Timestamp) val, dateCellTimeZone));

				} else if (val instanceof java.util.Date) {

					actualCell.setCellType(CellType.STRING);
					actualCell.setCellValue(((java.util.Date) val).toString());

				} else if (val instanceof java.util.Date) {

					actualCell.setCellType(CellType.STRING);
					actualCell.setCellValue(((java.util.Date) val).toString());

				} else if (val instanceof String) {

					actualCell.setCellType(CellType.STRING);
					actualCell.setCellValue((String) val);

				} else {

					actualCell.setCellType(CellType.STRING);
					actualCell.setCellValue("");

				}

			}
		}
	}

	/**
	 * @param map
	 * @param key
	 * @param rowNum
	 * 		ez is a key-hez kerül hozzáfűzésre (akkor, jó, ha sok lényegében azonos key van), null esetén nincs append/concat
	 * @param value
	 */
	public static void putEntryIntoMap(final Map<String, Object> map, final String key, final Integer rowNum, final Object value) {

		final StringBuilder sbFormattedKey = new StringBuilder();
		sbFormattedKey.append(key);

		if (rowNum != null) {
			sbFormattedKey.append("#"); // a sorszámosak is csak sima paraméterek, csak több van belőlük...
			sbFormattedKey.append(rowNum);
		}

		map.put(sbFormattedKey.toString(), value);
	}

}