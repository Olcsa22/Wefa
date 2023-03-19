package hu.lanoga.toolbox.vaadin.component.codemirror;

import java.util.UUID;

import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.server.Page;
import com.vaadin.ui.TextArea;

@StyleSheet({
		"../../../../webjars/codemirror/5.62.2/lib/codemirror.css", //
		"../../../../webjars/codemirror/5.62.2/theme/eclipse.css", //
		"../../../../webjars/codemirror/5.62.2/theme/elegant.css", //
		"../../../../webjars/codemirror/5.62.2/theme/darcula.css", //
		"../../../../webjars/codemirror/5.62.2/theme/nord.css" //
})
@JavaScript({
		"../../../../webjars/codemirror/5.62.2/lib/codemirror.js", //
		"../../../../webjars/codemirror/5.62.2/mode/groovy/groovy.js", //
		"../../../../webjars/codemirror/5.62.2/mode/xml/xml.js", //
		"../../../../webjars/codemirror/5.62.2/mode/yaml/yaml.js", //
		"../../../../webjars/codemirror/5.62.2/mode/javascript/javascript.js", //
		"../../../../webjars/codemirror/5.62.2/mode/css/css.js", //
		"../../../../webjars/codemirror/5.62.2/mode/sql/sql.js", //
		"../../../../webjars/codemirror/5.62.2/mode/shell/shell.js", //
		"../../../../webjars/codemirror/5.62.2/mode/velocity/velocity.js", //
		// "../../../../webjars/codemirror/5.62.2/mode/dockerfile/dockerfile.js", //
		"../../../../webjars/codemirror/5.62.2/mode/markdown/markdown.js", //
		"../../../../webjars/codemirror/5.62.2/mode/properties/properties.js", //
		"../../../../webjars/codemirror/5.62.2/mode/spreadsheet/spreadsheet.js" //
})
public class CodeMirrorComponent extends TextArea {

	public enum Theme {

		// https://codemirror.net/demo/theme.html

		ECLIPSE, ELEGANT, DARCULA, NORD
	}

	public enum Mode {

		// https://codemirror.net/mode/

		GROOVY, XML, YAML, 
		
		/**
		 * JSON-höz is jó lehet...
		 */
		JAVASCRIPT, 
		
		CSS, SQL, SHELL, VELOCITY, /* DOCKERFILE, */ MARKDOWN, PROPERTIES, SPREADSHEET
	}
	
	private final Mode mode;
	private final Theme theme;

	public CodeMirrorComponent(final Mode mode, final Theme theme) {
		
		this.setId(UUID.randomUUID().toString());		
		this.setWidth("100%");
		resizeHeight();
		
		// ---

		this.mode = mode;
		this.theme = theme;
				
	}
	
	public void cmEnhance() {
		
		int height = resizeHeight();
	
		final StringBuilder sbScript = new StringBuilder();
		sbScript.append("var myInterval = setInterval(function(){ ");
		sbScript.append("var x = document.getElementById('" + this.getId() + "');");
		sbScript.append("if (x) {");

		sbScript.append("var myCodeMirror = CodeMirror.fromTextArea(x, { content: x.value, inputStyle: 'textarea', autofocus: true, indentWithTabs: true, smartIndent: false, spellcheck: false, lineNumbers: true, mode: '" + mode.name().toLowerCase() + "', theme: '" + theme.name().toLowerCase() + "' });");
		sbScript.append("myCodeMirror.setSize(null, " + height + ");");
		sbScript.append("myCodeMirror.on('change', function(){ console.log('CodeMirror save'); myCodeMirror.save(); x.dispatchEvent(new Event('change')); });");
		sbScript.append("clearInterval(myInterval);");

		sbScript.append("}}, 30);");

		com.vaadin.ui.JavaScript.getCurrent().execute(sbScript.toString());
		
	}
	
	private int resizeHeight() {
		int height = (int) (0.8d * Page.getCurrent().getBrowserWindowHeight());
		this.setHeight(height, Unit.PIXELS);
		return height;
	}

}
