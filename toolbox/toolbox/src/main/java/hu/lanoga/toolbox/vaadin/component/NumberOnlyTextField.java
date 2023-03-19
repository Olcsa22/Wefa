package hu.lanoga.toolbox.vaadin.component;

import java.text.DecimalFormatSymbols;

import org.apache.commons.lang.StringUtils;
import org.vaadin.textfieldformatter.NumeralFieldFormatter;

import com.vaadin.ui.TextField;

import hu.lanoga.toolbox.util.ToolboxNumberUtil;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.Getter;

/**
 * {@link TextField} + {@link NumeralFieldFormatter}
 */
@Getter
public class NumberOnlyTextField extends TextField {

	private final boolean negativeAllowed;
	private final int integerScale;
	private final int decimalScale;
	private final String thousandSep;
	private final String decimalMark;

	private boolean turnBackToInternationalFormatOnGetValue;

	public NumberOnlyTextField() {
		this(null, false, false);
	}

	public NumberOnlyTextField(final String caption) {
		this(caption, false, false);
	}

	/**
	 * @param caption
	 * @param negativeAllowed
	 * @param fractionAllowed 
	 * 		number of decimals = 5 (see other constructors too)
	 */
	public NumberOnlyTextField(final String caption, final boolean negativeAllowed, final boolean fractionAllowed) {
		this(caption, negativeAllowed, -1, fractionAllowed ? 5 : 0, null, null);
	}

	/**
	 * @param caption
	 * @param negativeAllowed
	 * @param integerScale 
	 * 		limit the scale of integer i.e. number of digits before decimal (-1 = unlimited)
	 * @param decimalScale 
	 * 		number of decimals
	 */
	public NumberOnlyTextField(final String caption, final boolean negativeAllowed, final int integerScale, final int decimalScale) {
		this(caption, negativeAllowed, integerScale, decimalScale, null, null);
	}

	/**
	 * @param caption
	 * @param negativeAllowed
	 * @param integerScale 
	 * 		limit the scale of integer i.e. number of digits before decimal (-1 = unlimited)
	 * @param decimalScale 
	 * 		number of decimals
	 * @param thousandSep
	 * 		null esetén UiHelper#getCurrentUiDecimalFormatSymbols() alapján
	 * @param decimalMark
	 * 		null esetén UiHelper#getCurrentUiDecimalFormatSymbols() alapján
	 * 		
	 * @see #setTurnBackToInternationalFormatOnGetValue(boolean)
	 */
	public NumberOnlyTextField(final String caption, final boolean negativeAllowed, final int integerScale, final int decimalScale, final String thousandSep, final String decimalMark) {
		super(caption);

		this.negativeAllowed = negativeAllowed;
		this.integerScale = integerScale;
		this.decimalScale = decimalScale;

		this.turnBackToInternationalFormatOnGetValue = false;

		DecimalFormatSymbols dfs = null;

		if (thousandSep == null || decimalMark == null) {
			dfs = UiHelper.getCurrentUiDecimalFormatSymbols();
		}

		this.thousandSep = thousandSep != null ? thousandSep : String.valueOf(dfs.getGroupingSeparator());
		this.decimalMark = decimalMark != null ? decimalMark : String.valueOf(dfs.getDecimalSeparator());

		this.extend();
	}

	private void extend() {

		// new NumeralFieldFormatter(this.thousandSep, this.decimalMark, this.integerScale, this.decimalScale, !this.negativeAllowed).extend(this);
	}

	@Override
	public String getValue() {

		String value = StringUtils.trimToNull(super.getValue());

		if (this.turnBackToInternationalFormatOnGetValue && StringUtils.isNotEmpty(value)) {

			// átalában:
			// nagyon észnél kell lenni, a "," lehet ezres és tizedes elválasztó is egyes országokban!
			// a szóköz általában simábban kiszedhető, mert az csak ezres elválasztó lehet, semmelyik országban sem tizedes elválaszó

			// ---
			
			value = ToolboxNumberUtil.stringToBigDecimalSafeString(value, this.thousandSep, this.decimalMark);

		}

		return value;
	}

	@Override
	protected boolean setValue(String value, boolean userOriginated) {
		
		value = StringUtils.replace(value, " ", "");
		value = StringUtils.replace(value, " ", ""); // ez egy másik fajta space
		// value = value.replaceAll("\\s+", ""); // nem jó
		
		return super.setValue(value, userOriginated);
	}

	public void setTurnBackToInternationalFormatOnGetValue(boolean turnBackToInternationalFormatOnGetValue) {
		this.turnBackToInternationalFormatOnGetValue = turnBackToInternationalFormatOnGetValue;
	}

}
