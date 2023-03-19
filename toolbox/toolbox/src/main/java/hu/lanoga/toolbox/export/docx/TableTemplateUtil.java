package hu.lanoga.toolbox.export.docx;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings({ "unused", "unchecked", "rawtypes"})
public class TableTemplateUtil {

	private TableTemplateUtil() {
		//
	}

	private static final String TABLE_TAG = "TT";
	private static final String TABLE_ATTR_ID = "id";
	private static final String TABLE_ATTR_HRC = "hrc";
	private static final String TABLE_ATTR_FRC = "frc";

	/**
	 * megkeresi TT tageket, ha nincs hiba a sémában ill. az adatforrásban, elkészíti TableTemp inform. listát; 
	 * ezek alapján kigenerálja az új táblázatot, és törli a régit
	 *
	 * @param document
	 * @param map
	 * @param <V>
	 */
	static <V> void ttGen(final XWPFDocument document, final Map<String, V> map) {
		final List<XWPFParagraph> paragraphs = document.getParagraphs();

		final List<TableTemplate> tableTemplates = new ArrayList<>();

		for (int i = 0; i < paragraphs.size(); i++) {
			final XWPFParagraph p = paragraphs.get(i);
			String pT = p.getText();
			
			pT = pT.trim().replaceAll(" +", " ");

			pT = pT.replace("<# ", "<#");

			pT = pT.replace(" =", "=");
			pT = pT.replace("= ", "=");

			final String[] datas = pT.split(" ");

			if (pT.matches("(.*)<#" + "(?i)(" + TABLE_TAG + ")" + " (.*)" + "(?i)(" + TABLE_ATTR_ID + ")" + "=(.*)+" + "(.*)#>(.*)")) {
				log.debug("--------------------- header tag ok, id: " + i);
				final String idS = findId(datas, map);

				if (idS != null) {
					final int hrc = findAttr(datas, TABLE_ATTR_HRC);
					final int frc = findAttr(datas, TABLE_ATTR_FRC);

					// Ha megtalálta a TableTemplate blokk zárótag-jét az i.-ik paragraph-ban, akkor létrehozza az adott
                    // blokkot reprezentáló TableTemplate objektumot, és a zárótag utáni paragraph-ra ugrik, ahonnan
                    // folytatja tovább az újabb TableTemplate blokkok keresését.
					final Integer endPid = findEndPid(i, paragraphs);
					if (endPid != null) {
						final TableTemplate tt = new TableTemplate();
						tt.setStartP(i);
						tt.setEndP(endPid);
						tt.setMapKey(idS);
						tt.setHrc(hrc);
						tt.setFrc(frc);
						tableTemplates.add(tt);

						i = endPid;
					} else {
						throw new DocxExporterException("NO table end tag!");
					}

				} else {
					throw new DocxExporterException("BAD tableId value!");
				}
			}
		}

		getTables(document, map, tableTemplates);

		removeTableTags(document);
	}

	/**
	 *
	 * törli a régi táblázat sablont
	 *
	 * @param document
	 */
	private static void removeTableTags(final XWPFDocument document) {
		final List<TableTemplate> remTTs = new ArrayList<>();

		final List<XWPFParagraph> paragraphs = document.getParagraphs();

		for (int i = 0; i < paragraphs.size(); i++) {
			final XWPFParagraph p = paragraphs.get(i);
			String pT = p.getText();

			pT = pT.trim().replaceAll(" +", " ");

			pT = pT.replace("<# ", "<#");

			pT = pT.replace(" =", "=");
			pT = pT.replace("= ", "=");

			if (pT.matches("(.*)<#" + "(?i)(" + TABLE_TAG + ")" + " (.*)" + "(?i)(" + TABLE_ATTR_ID + ")" + "=(.*)+" + "(.*)#>(.*)")) {
				log.debug("--------------------- REM header tag ok, id: " + i);

				final Integer endPid = findEndPid(i, paragraphs);
				if (endPid != null) {
					final TableTemplate tt = new TableTemplate();
					tt.setStartP(i);
					tt.setEndP(endPid);
					remTTs.add(tt);

					// ha megtalálta at adott TT blokk zárótag-jét, a rákövetkező paragrafustól folytatódik a következő TT keresése (nincs egymásba ágyazás)
					i = endPid;
				} else {
					throw new DocxExporterException("NO table end tag!");
				}
			}
		}

		for (int i = 0; i < remTTs.size(); i++) {
			final TableTemplate tt = remTTs.get(i);

			if (document.getParagraphs().get(tt.getStartP()) != null) {
				document.removeBodyElement(document.getPosOfParagraph(document.getParagraphs().get(tt.getStartP())));
			}
			if (document.getParagraphs().get(tt.getEndP() - 1) != null) {
				document.removeBodyElement(document.getPosOfParagraph(document.getParagraphs().get(tt.getEndP() - 1)));
			}
		}
	}

	/**
	 *
	 * megkeresi a dokumentum táblázatait, a hozzájuk tartozó TableTemp-et, és ezek alapján kigenerálja az új táblázatokat
	 *
	 * @param document
	 * @param map
	 * @param tableTemplates
	 * @param <V>
	 */
	private static <V> void getTables(final XWPFDocument document, final Map<String, V> map, final List<TableTemplate> tableTemplates) {
		final List<XWPFTable> tables = document.getTables();

		for (int i = 0; i < tables.size(); i++) {
			final XWPFTable table = tables.get(i);

			final TableTemplate tt = getTtInfoForTable(document, table, tableTemplates);
			if (tt != null) {
				genNewTable(document, table, tt, map);
			} else {
				// throw new RuntimeException("BAD SCHEMA!");
			}
		}

	}

	/**
	 *
	 * map adataiból kigenerálja tamplate alapján az új táblázatot
	 *
	 * @param document
	 * @param table
	 * @param tableTemplate
	 * @param map
	 * @param <V>
	 */
	private static <V> void genNewTable(final XWPFDocument document, final XWPFTable table, final TableTemplate tableTemplate, final Map<String, V> map) {
		if (table.getRows().size() < (tableTemplate.getHrc() + tableTemplate.getFrc() + 1)) {
			throw new DocxExporterException("BAD hrc/frc ATTRIBUTES or template row SIZE!");
		}

		final XWPFTable newTable = document.insertNewTbl(document.getParagraphs().get(tableTemplate.getStartP()).getCTP().newCursor());
		final CTTblWidth width = newTable.getCTTbl().addNewTblPr().addNewTblW();
		width.setType(STTblWidth.DXA);
		width.setW(BigInteger.valueOf(9072));

		final List<XWPFTableRow> headerRows = new ArrayList<>();
		for (int i = 0; i < tableTemplate.getHrc(); i++) {
			headerRows.add(table.getRows().get(i));
		}
		final List<XWPFTableRow> footerRows = new ArrayList<>();
		for (int i = table.getRows().size() - tableTemplate.getFrc(); i < table.getRows().size(); i++) {
			footerRows.add(table.getRows().get(i));
		}

		// CREATE NEW TABLE:

		// header
		for (int i = 0; i < headerRows.size(); i++) {

			final XWPFTableRow hr = headerRows.get(i);
			replaceHeader(hr, map, tableTemplate);

			newTable.addRow(hr);
		}

		// content
		
		List<List<String>> contList;
		try {
			contList = ((List<List<String>>) ((HashMap<String, Object>) map.get(tableTemplate.getMapKey())).get(DocxExporter.TT_MAP_KEY_CONTENT));
		} catch (final Exception e) {
			throw new DocxExporterException("BAD contList for actual mapKey!", e);
		}
		final Integer contSize = contList.size();

		for (int i = 0; i < contSize; i++) {

			final CTRow ctRow = CTRow.Factory.newInstance();
			final XWPFTableRow contentRow = table.getRows().get(tableTemplate.getHrc());
			ctRow.set(contentRow.getCtRow());
			final XWPFTableRow newContRow = new XWPFTableRow(ctRow, table);

			replaceContent(newContRow, contList.get(i));

			newTable.addRow(newContRow);
		}

		// footer
		for (int i = 0; i < footerRows.size(); i++) {

			final XWPFTableRow fr = footerRows.get(i);
			replaceFooter(fr, map, tableTemplate);

			newTable.addRow(fr);
		}

		newTable.removeRow(0);
		document.removeBodyElement(document.getPosOfTable(table));
	}

	/**
	 * val lista elemeinek száma egyezzen oszlopok számával (va nincs az adott oszlopban TAG, akkor lehet pl. üres string a listában)!
	 *
	 * @param row
	 * @param val
	 */
	private static void replaceContent(final XWPFTableRow row, final List<String> val) {

		// CONTENT:

		if (val != null) {
			for (int i = 0; i < row.getTableCells().size(); i++) {
				
				final XWPFTableCell cell = row.getTableCells().get(i);
				
				for (int j = 0; j < cell.getParagraphs().size(); j++) {
					
					final XWPFParagraph p = cell.getParagraphs().get(j);
					replace(p, DocxExporter.TT_MAP_KEY_CONT_VAL, val.get(i));
					
				}
			}
		}
		
	}

	private static <V> void replaceHeader(final XWPFTableRow row, final Map<String, V> map, final TableTemplate tableTemplate) {
	
		final HashMap<String, Object> dataContainer = (HashMap<String, Object>) map.get(tableTemplate.getMapKey());

		// HEADER:
		
		if (tableTemplate.getHrc() > 0) {
			final HashMap<String, String> headHm = (HashMap<String, String>) dataContainer.get(DocxExporter.TT_MAP_KEY_HEADER);
			if (headHm != null) {
				for (int i = 0; i < row.getTableCells().size(); i++) {
					final XWPFTableCell cell = row.getTableCells().get(i);
					for (int j = 0; j < cell.getParagraphs().size(); j++) {
						final XWPFParagraph p = cell.getParagraphs().get(j);
						for (final Entry<String, String> entry : headHm.entrySet()) {
							replace(p, entry.getKey(), entry.getValue());
						}
					}
				}
			}
		}
		
	}

	private static <V> void replaceFooter(final XWPFTableRow row, final Map<String, V> map, final TableTemplate tableTemplate) {
		
		final HashMap<String, Object> dataContainer = (HashMap<String, Object>) map.get(tableTemplate.getMapKey());

		// FOOTER:
		
		if (tableTemplate.getFrc() > 0) {
			final HashMap<String, String> footHm = (HashMap<String, String>) dataContainer.get(DocxExporter.TT_MAP_KEY_FOOTER);
			if (footHm != null) {
				for (int i = 0; i < row.getTableCells().size(); i++) {
					final XWPFTableCell cell = row.getTableCells().get(i);
					for (int j = 0; j < cell.getParagraphs().size(); j++) {
						final XWPFParagraph p = cell.getParagraphs().get(j);
						for (final Entry<String, String> entry : footHm.entrySet()) {
							replace(p, entry.getKey(), entry.getValue());
						}
					}
				}
			}
		}
		
	}

	// alternatív félkész replace implementáció... 
	// private static String replaceByMap(String content, HashMap<String, String> hm) {
	// String s = content;
	//
	// if (s.matches("(.*)<#(.*)#>(.*)")) {
	//
	// String prepS = s.replace("#>", "#>" + TAG_SPLIT_STRING);
	// String[] datas = prepS.split(TAG_SPLIT_STRING);
	//
	// for (int i = 0; i < datas.length; i++) {
	//
	// if (datas[i].matches("(.*)<#(.*)#>(.*)")) {
	//
	// String key = datas[i];
	// key = key.replaceAll("(.*)<#", "");
	// key = key.replaceAll("#>(.*)", "");
	// key.trim();
	//
	// String val = hm.get(key);
	// if (val != null) {
	// datas[i] = datas[i].replaceAll("<#(.*)#>", val);
	// } else {
	// //
	// }
	// }
	// }
	//
	// StringBuilder sb = new StringBuilder();
	// for (String str : datas) {
	// sb.append(str);
	// }
	// return sb.toString();
	//
	// } else {
	// return content;
	// }
	// }

	/**
	 *
	 * megkeresi az adott táblázat sablonhoz tartozó tableTemp-et
	 *
	 * @param document
	 * @param table
	 * @param tableTemplates
	 * @return
	 */
	private static TableTemplate getTtInfoForTable(final XWPFDocument document, final XWPFTable table, final List<TableTemplate> tableTemplates) {

		for (int i = 0; i < tableTemplates.size(); i++) {
			final TableTemplate tt = tableTemplates.get(i);
			final int tablePos = document.getPosOfTable(table);

			if ((tt.getStartP() < tablePos) && (tt.getEndP() >= tablePos)) {
				return tt;
			}
		}

		return null;
	}

	/**
	 *
	 * megkeresi az adott TT zárótag-jét
	 *
	 * @param startInd
	 * @param paragraphs
	 * @return paragraph sorszáma
	 */
	private static Integer findEndPid(final int startInd, final List<XWPFParagraph> paragraphs) {
		for (int i = startInd; i < paragraphs.size(); i++) {
			String pT = paragraphs.get(i).getText();
			pT = pT.replace(" ", "");
			if (pT.matches("(.*)</#" + "(?i)(" + TABLE_TAG + ")" + "#>(.*)")) {
				return i;
			}
		}

		return null;
	}

	/**
	 *
	 * megkeresi az adatforrás attribútum ID-ját
	 *
	 * @param datas
	 * @param map
	 * @param <V>
	 * @return
	 */
	private static <V> String findId(final String[] datas, final Map<String, V> map) {
		
		for (int j = 0; j < datas.length; j++) {
			if (datas[j].matches("(?i)(" + TABLE_ATTR_ID + ")" + "=(.*)+")) {
				final String idS = datas[j].replaceAll("(?i)(" + TABLE_ATTR_ID + ")" + "=", "").trim();
				if (map.keySet().contains(idS)) {
					return idS;
				}
			}
		}

		return null;
	}

	/**
	 * megkeresi az attribútum értéket (hrc(frc)
	 *
	 * @param datas
	 * @param attrName
	 * @return
	 */
	private static int findAttr(final String[] datas, final String attrName) {
	
		int attr = 0;
		
		for (int j = 0; j < datas.length; j++) {
			if (datas[j].matches(attrName + "=(.*)+")) {
				
				final String attrS = datas[j].replace(attrName + "=", "").trim();

				try {
					attr = Integer.parseInt(attrS);
					return attr;
				} catch (final Exception e) {
					attr = 0;
				}
				
			}
		}

		return attr;
		
	}

	/**
	 * @param paragraph
	 * @param searchText
	 * @param replacement
	 */
	static <V> void replace(final XWPFParagraph paragraph, final String searchText, final V replacement) {

		boolean found = true;
		
		while (found) {
			
			found = false;
			final int pos = paragraph.getText().indexOf(searchText);

			if (replacement instanceof List) { // TODO: elágazások egybe, ahol lehet (PT)
				if (pos >= 0) {
					
					found = true;
				
					final Map<Integer, XWPFRun> posToRuns = getPosToRuns(paragraph);
					final XWPFRun run = posToRuns.get(pos);
					final XWPFRun lastRun = posToRuns.get((pos + searchText.length()) - 1);
					final int runNum = paragraph.getRuns().indexOf(run);
					final int lastRunNum = paragraph.getRuns().indexOf(lastRun);
				
					for (final Object obj : (List) replacement) {

						// final String value = obj.toString();
						final String text = obj.toString();

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

			} else {
				
				if (pos >= 0) {
					
					found = true;
					
					final Map<Integer, XWPFRun> posToRuns = getPosToRuns(paragraph);
					final XWPFRun run = posToRuns.get(pos);
					final XWPFRun lastRun = posToRuns.get((pos + searchText.length()) - 1);
					final int runNum = paragraph.getRuns().indexOf(run);
					final int lastRunNum = paragraph.getRuns().indexOf(lastRun);

					final String text = replacement.toString();

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
	}

	private static Map<Integer, XWPFRun> getPosToRuns(final XWPFParagraph paragraph) {
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
}