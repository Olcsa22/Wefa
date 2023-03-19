package hu.lanoga.wefa.vaadin.procdef.dialog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.lang3.StringUtils;

import com.teamunify.i18n.I;
import com.vaadin.ui.Button;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.vaadin.component.codemirror.CodeMirrorComponent;
import hu.lanoga.wefa.exception.WefaGeneralException;

/**
 * Ez nem Groovy script alapú! Lásd Activiti dokumentáció a feltétel leíró formátumával kapcsolatban: 
 * https://www.activiti.org/userguide/#bpmnConditionalSequenceFlow
 */
public class ConditionEditorDialog extends Window {

	// https://www.activiti.org/userguide/#bpmnConditionalSequenceFlow
	// a <![CDATA[ nem kell elvileg (azt ráteszi a JavaScript editor komponens)

	// minta:
	// "${samplevarname == 'sample'}"

	public ConditionEditorDialog(final String processDefinitonId, final String elementId, final String condExprWhenOpened) {
		super();

		this.setCaption(I.trc("Caption", "Feltétel"));

		this.setWidth(80, Unit.PERCENTAGE);
		this.setHeight(null);
		this.setModal(true);

		final VerticalLayout vlContent = new VerticalLayout();
		vlContent.setWidth(100, Unit.PERCENTAGE);
		vlContent.setHeight(null);
		vlContent.setSpacing(true);
		vlContent.setMargin(true);
		this.setContent(vlContent);

		final CodeMirrorComponent codeMirrorComponent = new CodeMirrorComponent(CodeMirrorComponent.Mode.SHELL, CodeMirrorComponent.Theme.DARCULA); // TODO: ez nem Groovy, de nem tudom, hogy milyen szintax színezés kellene
		codeMirrorComponent.cmEnhance();
		codeMirrorComponent.setWidth("100%");
		vlContent.addComponent(codeMirrorComponent);

		if (StringUtils.isNotBlank(condExprWhenOpened)) {
			codeMirrorComponent.setValue(condExprWhenOpened);
		}

		final Button btnOk = new Button(I.trc("Button", "OK"));
		btnOk.setStyleName(ValoTheme.BUTTON_FRIENDLY);
		vlContent.addComponent(btnOk);

		btnOk.addClickListener(x -> {

			final String condExprToBeSaved = codeMirrorComponent.getValue();

			// default (else) flow esetén nem kell ide írni semmit 
			// jobb gomb és a kis wrench gombbal kell default flow-nak megjelölni a vonalat
			
			// if (StringUtils.isBlank(condExprToBeSaved)) {
			// throw new ManualValidationException("Condition expression cannot be null/empty!", I.trc("Error", "A feltétel nem lehet null/üres!"));
			// }

			// ---

			try {
				JavaScript.eval("wfeConditionalFlowBack('" + processDefinitonId + "', '" + elementId + "', '" + URLEncoder.encode(condExprToBeSaved, "UTF-8").replace("+", " ") + "')");
			} catch (final UnsupportedEncodingException e) {
				throw new WefaGeneralException(e);
			}

			ConditionEditorDialog.this.close();

		});

		// TODO: setRequiredIndicatorVisible + legalább manual validation ok nyomás előtt

	}

}
