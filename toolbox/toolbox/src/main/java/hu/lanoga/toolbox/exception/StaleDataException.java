package hu.lanoga.toolbox.exception;

import hu.lanoga.toolbox.i18n.I18nUtil;
import lombok.Getter;

@Getter
public class StaleDataException extends org.springframework.dao.DataIntegrityViolationException implements ToolboxException {

	/**
	 * JSON kell legyen
	 */
	private String uiMessageJson;

	public StaleDataException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public StaleDataException(final String message, final String uiMessage) {
		super(message);
		setUiMessage(uiMessage);
	}

	public StaleDataException(final String message) {
		super(message);
	}
	
	public void setUiMessage(final String lang, final String uiMessageStr) {
		this.uiMessageJson = "{\"" + lang + "\":\"" + uiMessageStr + "\"}";
	}
	
	/**
	 * belépett user nyelve szerintinek tekinti...
	 * 
	 * @param uiMessageStr
	 */
	public void setUiMessage(final String uiMessageStr) {
		final String lang = I18nUtil.getLoggedInUserLocale().getLanguage().toLowerCase();
		this.uiMessageJson = "{\"" + lang + "\":\"" + uiMessageStr + "\"}";
	}
	
}
