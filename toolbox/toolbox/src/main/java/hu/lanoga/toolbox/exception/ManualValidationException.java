package hu.lanoga.toolbox.exception;

import javax.validation.ValidationException;

import org.json.JSONObject;

import hu.lanoga.toolbox.i18n.I18nUtil;
import lombok.Getter;

@Getter
public class ManualValidationException extends ValidationException implements ToolboxException {

	/**
	 * JSON kell legyen
	 */
	private String uiMessageJson;

	public ManualValidationException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public ManualValidationException(final String message, final String uiMessage) {
		super(message);
		setUiMessage(uiMessage);
	}

	public ManualValidationException(final String message) {
		super(message);
	}

	public ManualValidationException() {
		super();
	}

	/**
	 * belépett user nyelve szerintinek tekinti...
	 * 
	 * @param uiMessageStr
	 */
	public void setUiMessage(final String uiMessageStr) {
		final String lang = I18nUtil.getLoggedInUserLocale().getLanguage().toLowerCase();
		setUiMessage(lang, uiMessageStr);
	}

	public void setUiMessage(final String lang, final String uiMessageStr) {
		this.uiMessageJson = new JSONObject().put(lang, uiMessageStr).toString();
	}

}
