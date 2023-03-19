package hu.lanoga.toolbox.vaadin.component.file;

import com.vaadin.shared.ui.JavaScriptComponentState;

/**
 * @see FileUploadComponent
 */
public class JsFileUploadComponentState extends JavaScriptComponentState {

	/**
	 * UUID, feltöltési folyamat azonosítója...
	 */
	public String uploadId;
	
	public String contextPath;

	/**
	 * Tallózás gomb id (DOM)
	 */
	public String browseId;

	/**
	 * Drop terület id (DOM)
	 */
	public String areaId;

	/**
	 * JS lib ide írja ki a feltöltés állapotát (szöveges és/vagy progressbar) (DOM)
	 */
	public String progDispId;

	/**
	 * Feltölthető fájlok maximális száma
	 */
	public Integer maxFileCount;

	/**
	 * lehet-e több fájlt egyszerre feltölteni
	 * (akár drag and drop, akár gombra kattintva választással)
	 */
	public boolean isMultiFileAllowed;

	/**
	 * ha null, akkor bármi lehet
	 */
	public String allowedMime;

	/**
	 * átméretezés utáni img mime
	 */
	public String imgTargetMime;

	/**
	 * átméretezés négyzet max px szélessége
	 */
	public Integer maxPxWidth;

	/**
	 * átméretezés négyzet max px magassága
	 */
	public Integer maxPxHeight;

	/**
	 * átméretezés módja (lásd Dropzone.js dokumentáció, "contain" vagy "crop" jelenleg)
	 */
	public String resizeMethod;
}
