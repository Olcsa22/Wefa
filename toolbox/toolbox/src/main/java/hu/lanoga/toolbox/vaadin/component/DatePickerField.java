package hu.lanoga.toolbox.vaadin.component;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.teamunify.i18n.I;
import com.vaadin.ui.AbstractSingleSelect;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.NativeSelect;

import hu.lanoga.toolbox.vaadin.util.UiHelper;

@SuppressWarnings("unchecked")
public class DatePickerField extends CustomField<java.sql.Date> {

	private java.sql.Date value;

	/**
	 * a mai dátumtól visszamenőleg engedélyezés (pl. 1975 kiválasztása), 
	 * ezt akkor kell kikapcsolni, ha csak jövőbeli dátumot akarunk mutatni
	 */
	private final boolean enableBackwardsSelection;

	/**
	 * ha leakarjuk korlátozni hány évre visszamenőleg akarjuk engedni a dátumokat, 
	 * ha ez null, akkor 1900-tól fog indulni 
	 * (másik példa: "100", ebben az esetben "2019-100=1919" -től fogja mutatni az éveket)
	 */
	private final Integer backwardsSelectionRange;

	/**
	 * a mai dátumtől előremenőleg enegedélyezés (pl. 2030), 
	 * ezt például születési dátum választásánál érdemes false-ra rakni
	 */
	private final boolean enableForwardSelection;

	/**
	 * hány évvel előre engedjük az éveket {@link #enableForwardSelection} esetén)
	 */
	private final int forwardSelectionRange;

	private final boolean enableDefaultValue;

	private HorizontalLayout hlContent;

	private AbstractSingleSelect<Integer> cmbYear;
	private AbstractSingleSelect<Integer> cmbMonth;
	private AbstractSingleSelect<Integer> cmbDay;

	private boolean useNativeSelects;

	public DatePickerField(final String caption) {
		this(caption, true, true, null, true, 5, false);
	}

	public DatePickerField(final String caption, final boolean enableDefaultValue, final boolean enableBackwardsSelection, final Integer backwardsSelectionRange, final boolean enableForwardSelection, final int forwardSelectionRange, final Boolean useNativeSelects) {
		this.setCaption(caption);
		this.enableDefaultValue = enableDefaultValue;
		this.enableBackwardsSelection = enableBackwardsSelection;
		this.backwardsSelectionRange = backwardsSelectionRange;
		this.enableForwardSelection = enableForwardSelection;
		this.forwardSelectionRange = forwardSelectionRange;

		if (useNativeSelects == null) {
			this.useNativeSelects = UiHelper.useTouchMode();
		} else {
			this.useNativeSelects = useNativeSelects;
		}

	}

	@Override
	protected Component initContent() {

		if (this.useNativeSelects) {

			this.cmbYear = new NativeSelect<>();
			this.cmbMonth = new NativeSelect<>();
			this.cmbDay = new NativeSelect<>();

			((NativeSelect<Integer>) this.cmbYear).setEmptySelectionAllowed(false);
			((NativeSelect<Integer>) this.cmbYear).setWidth("100%");

			((NativeSelect<Integer>) this.cmbYear).setEmptySelectionAllowed(false);
			((NativeSelect<Integer>) this.cmbYear).setWidth("100%");

			((NativeSelect<Integer>) this.cmbYear).setEmptySelectionAllowed(false);
			((NativeSelect<Integer>) this.cmbYear).setWidth("100%");

		} else {
			
			this.cmbYear = new ComboBox<>();
			this.cmbMonth = new ComboBox<>();
			this.cmbDay = new ComboBox<>();

			((ComboBox<Integer>) this.cmbYear).setEmptySelectionAllowed(false);
			((ComboBox<Integer>) this.cmbYear).setPlaceholder(I.trc("Caption", "year"));
			((ComboBox<Integer>) this.cmbYear).setWidth("100%");

			((ComboBox<Integer>) this.cmbMonth).setEmptySelectionAllowed(false);
			((ComboBox<Integer>) this.cmbMonth).setPlaceholder(I.trc("Caption", "month"));
			((ComboBox<Integer>) this.cmbMonth).setWidth("100%");

			((ComboBox<Integer>) this.cmbDay).setEmptySelectionAllowed(false);
			((ComboBox<Integer>) this.cmbDay).setPlaceholder(I.trc("Caption", "day"));
			((ComboBox<Integer>) this.cmbDay).setWidth("100%");
			
		}

		this.hlContent = new HorizontalLayout();
		this.hlContent.setWidth("100%");

		final Calendar currentCalendar = Calendar.getInstance(); // jelenlegi dátum, ezen nem módosítunk semmit

		// év

		final List<Integer> yearList = this.generateYearList();
		yearList.sort(Collections.reverseOrder());
		this.cmbYear.setItems(yearList);

		this.cmbYear.addValueChangeListener(x -> {
			if (this.cmbMonth.getValue() != null) {
				this.cmbDay.setItems(this.generateDayList(x.getValue() - 1));
			}

			this.changeCustomFieldValue();
		});

		// hónap

		final List<Integer> monthList = new ArrayList<>();
		for (int i = 1; i < 13; i++) {
			monthList.add(i);
		}

		this.cmbMonth.setItems(monthList);

		this.cmbMonth.addValueChangeListener(x -> {
			this.cmbDay.setItems(this.generateDayList(x.getValue() - 1));
			this.changeCustomFieldValue();
		});

		// nap
		
		this.cmbDay.setItems(this.generateDayList(currentCalendar.get(Calendar.MONTH)));

		this.cmbDay.addValueChangeListener(x -> {
			this.changeCustomFieldValue();
		});

		// alap értékek beállítása

		if (this.enableDefaultValue) {
			this.cmbYear.setValue(currentCalendar.get(Calendar.YEAR));
			this.cmbMonth.setValue(currentCalendar.get(Calendar.MONTH) + 1); // 0-tól indul a hónap
			this.cmbDay.setValue(currentCalendar.get(Calendar.DAY_OF_MONTH));
		}

		this.hlContent.addComponents(this.cmbYear, this.cmbMonth, this.cmbDay);

		// ---

		if (this.value != null) {
			final Calendar savedValue = Calendar.getInstance();
			savedValue.setTimeInMillis(this.value.getTime());

			this.cmbYear.setValue(savedValue.get(Calendar.YEAR));
			this.cmbMonth.setValue(savedValue.get(Calendar.MONTH) + 1); // 0-tól indul a hónap
			this.cmbDay.setValue(savedValue.get(Calendar.DAY_OF_MONTH));
		}

		return this.hlContent;

	}

	private List<Integer> generateYearList() {

		final Calendar counterCalendar = Calendar.getInstance(); // ez is a jelenlegi dátumtól indul, de ez módosítva lesz
	
		if (this.enableBackwardsSelection) {

			if (this.backwardsSelectionRange != null) {
				counterCalendar.add(Calendar.YEAR, -1 * this.backwardsSelectionRange);
			} else {
				counterCalendar.set(Calendar.YEAR, 1900);
			}

		}

		final Calendar endCalendar = Calendar.getInstance(); // ez a végső dátum, hogy meddig jelenjenek meg az évszámok

		if (this.enableForwardSelection) {
			endCalendar.add(Calendar.YEAR, this.forwardSelectionRange);
		}

		final List<Integer> yearList = new ArrayList<>();
		for (; (counterCalendar.before(endCalendar) || counterCalendar.equals(endCalendar)); counterCalendar.add(Calendar.YEAR, 1)) {
			yearList.add(counterCalendar.get(Calendar.YEAR));
		}

		return yearList;
	}

	/**
	 * az adott hónaphoz kigenerálni a nap listát 
	 * (figyelem az nsYear értékét is figyelembe veszi!)
	 *
	 * @param month
	 *
	 * @return
	 */
	private List<Integer> generateDayList(final int month) {
		final Calendar cal = Calendar.getInstance();

		if (this.cmbYear.getValue() != null) {
			cal.set(Calendar.YEAR, this.cmbYear.getValue());
		}

		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH, 1);

		final int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

		final List<Integer> dayList = new ArrayList<>();
		for (int i = 0; i < maxDay; i++) {
			dayList.add(i + 1);
		}

		return dayList;
	}

	/**
	 * előállít a 3 ComboBox-ból egy Date értéket
	 *
	 * @return
	 */
	private java.sql.Date generateValue() {

		// itt le van ellenőrizve minden null értékre is a biztonság kedvéért, de minden NativeSelect-nek van default értéke emiatt nem lehet(ne) null
		final int currentYear = Calendar.getInstance().get(Calendar.YEAR);

		final int year = this.cmbYear.getValue() != null ? this.cmbYear.getValue() : currentYear;
		final int month = this.cmbMonth.getValue() != null ? this.cmbMonth.getValue() : 1;
		final int day = this.cmbDay.getValue() != null ? this.cmbDay.getValue() : 1;

		// kikell vonni belőle 1900-at mert feleselegesen adja hozzá
		// kikell vonni 1-et a hónapból, mert alapból hozzá ad 1-et
		return new java.sql.Date(year - 1900, month - 1, day);
	}

	/**
	 * field értékének frissítése
	 */
	private void changeCustomFieldValue() {

		final java.sql.Date oldValue = this.value;
		this.value = this.generateValue();

		final Collection<?> listeners = this.getListeners(ValueChangeEvent.class);
		for (final Object listener : listeners) {
			((ValueChangeListener<java.sql.Date>) listener).valueChange(new ValueChangeEvent<>(this, oldValue, true));
		}

	}

	@Override
	public java.sql.Date getValue() {
		return this.value;
	}

	@Override
	protected void doSetValue(final java.sql.Date valueToBeSet) {

		final Calendar savedValue = Calendar.getInstance();
		savedValue.setTimeInMillis(valueToBeSet.getTime());

		this.cmbYear.setValue(savedValue.get(Calendar.YEAR));
		this.cmbMonth.setValue(savedValue.get(Calendar.MONTH) + 1); // 0-tól indul a hónap
		this.cmbDay.setValue(savedValue.get(Calendar.DAY_OF_MONTH));
	}

}