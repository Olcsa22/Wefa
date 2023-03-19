package hu.lanoga.wefa.vaadin.procdef.dialog;

import java.util.Base64;

import org.apache.commons.lang3.StringUtils;

import com.teamunify.i18n.I;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.exception.ManualValidationException;
import hu.lanoga.toolbox.util.ToolboxGroovyHelper;
import hu.lanoga.toolbox.util.ToolboxGroovyHelper.ToolboxGroovyHelperException;
import hu.lanoga.toolbox.vaadin.component.codemirror.CodeMirrorComponent;
import hu.lanoga.toolbox.vaadin.util.UiHelper;

/**
 * Ez az Activiti "gyári" script task-ja. Ez nem interaktív (UI) task, ez jó lehet pl. külső API hívásokra, export, import műveletek,
 * e-mail/értesítés küldése (a szokásos értesítéseken felül; akár külső partnernek)...
 *
 * @see UserTaskEditorDialog
 */
public class ScriptTaskEditorDialog extends Window {

	// minta (lényeg, hogy a execution változó automaikusan ott lesz, ebből lehet kinyerni adatokat):
	// "hu.lanoga.wefa.vaadin.util.DemoEmailUtil.sendMail(execution.getProcessInstanceId(), \"tivadar.bocz@gmail.com\");";

	public ScriptTaskEditorDialog(final String processDefinitonId, final String elementId, final String scriptStrWhenOpened) {
		super();

		this.setCaption(I.trc("Caption", "Automata (script) lépés"));

		this.setWidth(800, Unit.PIXELS);
		this.setHeight(null);
		this.setModal(true);

		final VerticalLayout vlContent = new VerticalLayout();
		vlContent.setWidth(100, Unit.PERCENTAGE);
		vlContent.setHeight(null);
		vlContent.setSpacing(true);
		vlContent.setMargin(true);
		this.setContent(vlContent);

		final CodeMirrorComponent codeMirrorComponent = new CodeMirrorComponent(CodeMirrorComponent.Mode.GROOVY, CodeMirrorComponent.Theme.DARCULA); // TODO: látszólag nem színez semmit
		codeMirrorComponent.cmEnhance();

		codeMirrorComponent.setWidth("100%");
		vlContent.addComponent(codeMirrorComponent);

		if (StringUtils.isNotBlank(scriptStrWhenOpened)) {
			codeMirrorComponent.setValue(/*new String(Base64.getDecoder().decode(*/scriptStrWhenOpened/*))*/);
		}

		addBtnRow(processDefinitonId, elementId, vlContent, codeMirrorComponent);

		// TODO: setRequiredIndicatorVisible + legalább manual validation ok nyomás előtt

	}

	private void addBtnRow(String processDefinitonId, String elementId, VerticalLayout vlContent, CodeMirrorComponent codeMirrorComponent) {

		final HorizontalLayout hlButtonRow = new HorizontalLayout();
		hlButtonRow.setWidth("100%");

		final Button btnOk = new Button(I.trc("Button", "OK"));
		btnOk.setWidth("100px");
		btnOk.setStyleName(ValoTheme.BUTTON_FRIENDLY);
		hlButtonRow.addComponent(btnOk);
		hlButtonRow.setComponentAlignment(btnOk, Alignment.MIDDLE_LEFT);

		btnOk.addClickListener(x -> {

			final String scriptStrToBeSaved = codeMirrorComponent.getValue();

			if (StringUtils.isBlank(scriptStrToBeSaved)) {
				throw new ManualValidationException("Script cannot be null/empty!", I.trc("Error", "A script nem lehet null/üres!"));
			}

			try {
				ToolboxGroovyHelper.buildClass(scriptStrToBeSaved);
			} catch (final ToolboxGroovyHelperException e) {
				throw new ManualValidationException("Syntax error!", I.trc("Error", "Hibás szintaxis!"));
			}

			// ---

			JavaScript.eval("wfeScriptTaskNodeBack('" + processDefinitonId + "', '" + elementId + "', '" + Base64.getEncoder().encodeToString(scriptStrToBeSaved.getBytes()) + "')");

			ScriptTaskEditorDialog.this.close();

		});

		final Button btnHelp = new Button(I.trc("Button", "Help"));
		btnHelp.setWidth("100px");

		btnHelp.addClickListener(y -> {

			Window infoBoxDialog = buildScriptInfoBoxDialog();
			UI.getCurrent().addWindow(infoBoxDialog);
			UiHelper.forceDialogFocus(infoBoxDialog);

		});

		hlButtonRow.addComponent(btnHelp);
		hlButtonRow.setExpandRatio(btnHelp, 1f);

		vlContent.addComponent(hlButtonRow);
	}
	
	final static String SCRIPT_INFO_BOX_HELP_STR = 
			
			"<p style=\"font-weight: bold\">Fájlok feltöltése egy másik rendszerbe url alapján: </p>" +
			"hu.lanoga.wefa.util.ActivitiHelperUtil.uploadFileIntoExternalSystem(\"https://pelda.hu/feltoltes\", \"fileIdsFieldName\", execution); <br />" +
			"<p style=\"margin-left: 30px; color:green;\">Paraméterek: </p>" +
			"<p style=\"margin-left: 30px;\">1. Feltöltés helye (ezt null vagy üres \"\" érték estén az alapértelmezett lesz használva)<br />" +
			"2. A mező neve amelyből a fájlokat felakarjuk tölteni<br />" +
			"3. Mindig meg kell adni, a jelenlegi folyamat adatait adja át</p><br />" +

			"<p style=\"font-weight: bold\">Belső használatú email küldése a jelenlegi munkafolyamatról és adatairól: </p>" +
			"hu.lanoga.wefa.util.ActivitiHelperUtil.sendInternalNotifEmail(\"example@example.com\", execution);" +
			"<p style=\"margin-left: 30px; color:green;\">Paraméterek: </p>" +
			"<p style=\"margin-left: 30px;\">1. Címzett email címének megadása<br />" +
			"2. Mindig meg kell adni, a jelenlegi folyamat adatait adja át</p><br />" +

			"<p style=\"font-weight: bold\">Belső használatú email küldése a jelenlegi munkafolyamatról és adatairól (haladó): </p>" +
			"hu.lanoga.wefa.util.ActivitiHelperUtil.sendInternalNotifEmail(\"example@example.com\", \"fileIdsFieldName\", true, execution);" +
			"<p style=\"margin-left: 30px; color:green;\">Paraméterek: </p>" +
			"<p style=\"margin-left: 30px;\">1. Címzett email címének megadása<br />" +
			"2. A mező neve amelyből a fájlokat elakarjuk küldeni csatolmányként az emailben<br />" +
			"3. A folyamatban lévő adatok JSON fájl csatolmányként pluszban (true és false értékekkel használható (true=igaz, false=hamis))<br />" +
			"4. Mindig meg kell adni, a jelenlegi folyamat adatait adja át</p><br />" +
			
			"<p style=\"font-weight: bold\">Email küldése az ügyfélnek: </p>" +
			"hu.lanoga.wefa.util.ActivitiHelperUtil.sendExternalNotifEmail(\"clientEmail\", 550, [\"clientAccountType\", \"monthlyFee\"], \"Számláját megnyitottuk\", execution);" +
			"<p style=\"margin-left: 30px; color:green;\">Paraméterek: ...</p>";
	
			// TODO: befejezni
			/*
			"<p style=\"margin-left: 30px;\">1. Címzett email címének megadása<br />" +
			"2. A mező neve amelyből a fájlokat elakarjuk küldeni csatolmányként az emailben<br />" +
			"3. A folyamatban lévő adatok JSON fájl csatolmányként pluszban (true és false értékekkel használható (true=igaz, false=hamis))<br />" +
			"4. Mindig meg kell adni, a jelenlegi folyamat adatait adja át</p><br />"
			*/

	public static Window buildScriptInfoBoxDialog() {
		
		final VerticalLayout vlDialog = new VerticalLayout();
		vlDialog.setWidth("100%");
		vlDialog.setHeight(null);
		vlDialog.setMargin(true);

		final Label lbl = new Label();
		lbl.setWidth("100%");
		lbl.setContentMode(ContentMode.HTML);
		lbl.setValue(SCRIPT_INFO_BOX_HELP_STR);

		vlDialog.addComponent(lbl);

		final Window dialog = new Window(I.trc("Caption", "Help (and samples)"), vlDialog);
		dialog.setWidth("850px");
		dialog.setHeight(null);
		dialog.setModal(true);

		return dialog;

	}

}
