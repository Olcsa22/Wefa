package hu.lanoga.toolbox.vaadin.component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.teamunify.i18n.I;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.json.jackson.JacksonHelper;
import hu.lanoga.toolbox.vaadin.util.UiHelper;

/**
 * Több számot lehet megadni, JSON stringként lesz mentve... 
 * minden szám mellé egy megjegyzést is kell írni...
 * 
 * experimental, untested
 */
public class MultiNumberOnlyTextField extends CustomField<String> {

	private GridLayout gl;
	private Window dialog;

	private List<Pair<TextField, NumberOnlyTextField>> fieldPairList;

	private String value;

	private final boolean negativeAllowed;
	private final int integerScale;
	private final int decimalScale;
	private final String thousandSep;
	private final String decimalMark;
	private final int validationMaxLength;

	public MultiNumberOnlyTextField(final String caption, final boolean negativeAllowed, final int integerScale, final int decimalScale, final String thousandSep, final String decimalMark, final int validationMaxLength) {
		this.setCaption(caption);

		this.negativeAllowed = negativeAllowed;
		this.integerScale = integerScale;
		this.decimalScale = decimalScale;
		this.thousandSep = thousandSep;
		this.decimalMark = decimalMark;
		this.validationMaxLength = validationMaxLength;
	}

	public MultiNumberOnlyTextField(final String caption) {
		this.setCaption(caption);

		this.negativeAllowed = false;
		this.integerScale = -1;
		this.decimalScale = 0;
		this.thousandSep = null;
		this.decimalMark = null;
		this.validationMaxLength = 15;
	}

	@Override
	protected Component initContent() {

		final Button btn = new Button(I.trc("Button", "Edit amounts"));
		btn.setWidth("100%");

		btn.addClickListener(event -> {
			this.initDialog();
		});

		btn.addAttachListener(x -> {

			if (!this.isEnabled() || this.isReadOnly()) {

				btn.setCaption(I.trc("Button", "View amounts"));

				this.setEnabled(true);
				this.setReadOnly(true);

				this.setCaption("<span style=\"opacity:0.5\">" + this.getCaption() + "</span>");
				this.setCaptionAsHtml(true);
			}

		});

		return btn;
	}

	private void initDialog() {

		final VerticalLayout vlDialog = new VerticalLayout();

		this.gl = new GridLayout();
		this.gl.setWidth("100%");
		this.gl.setMargin(false);
		this.gl.setSpacing(true);
		this.gl.setColumns(3);
		this.gl.setColumnExpandRatio(0, 1f);
		this.gl.setColumnExpandRatio(1, 1f);
		this.gl.setColumnExpandRatio(2, 0.25f);

		vlDialog.addComponent(this.gl);

		// ---

		if (this.isEnabled() && !this.isReadOnly()) {
			vlDialog.addComponent(this.buildBtnLayout());
		}

		// ---

		String strDialogCaption;

		if (this.isEnabled() && !this.isReadOnly()) {
			strDialogCaption = I.trc("Title", "Edit amount field");
		} else {
			strDialogCaption = I.trc("Title", "View amount field");
		}

		strDialogCaption += " (" + Jsoup.clean(this.getCaption(), Safelist.none()) + ")";

		this.dialog = new Window(strDialogCaption, vlDialog);
		this.dialog.setWidth("700px");

		this.dialog.setModal(true);

		UI.getCurrent().addWindow(this.dialog);
		UiHelper.forceDialogFocus(this.dialog);

		// ---

		this.fieldPairList = new ArrayList<>();

		if (StringUtils.isNotBlank(this.value)) {

			final Map<String, String> valueMap = this.valueToMap(this.value);

			for (final Entry<String, String> entry : valueMap.entrySet()) {

				this.addFieldPair();

				final Pair<TextField, NumberOnlyTextField> pair = this.fieldPairList.get(this.fieldPairList.size() - 1);
				pair.getKey().setValue(entry.getKey());
				pair.getValue().setValue(entry.getValue());

			}

		} else if (this.isEnabled() && !this.isReadOnly()) {
			this.addFieldPair();
		}

		// ---

		if (!this.isEnabled() || this.isReadOnly()) {
			UiHelper.setEverytingDisabled(vlDialog);

			// TODO: eltüntetni az ilyenkor fölösleges 3. oszlopot (a "-" gombok)
		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<String, String> valueToMap(final String value) {
		final Map map = JacksonHelper.fromJsonToMap(value);
		return map;
	}

	private String mapToValue(final Map<String, String> map) {
		return JacksonHelper.toJson(map);
	}

	public BigDecimal valueSum() {
		final Map<String, String> map = this.valueToMap(this.value);
		BigDecimal sum = new BigDecimal(0);
		for (final String t : map.values()) {
			sum = sum.add(new BigDecimal(t));
		}
		return sum;
	}

	private void addFieldPair() {

		final TextField txtNote = new TextField();
		txtNote.setRequiredIndicatorVisible(true);
		txtNote.setWidth("100%");

		// ---

		final NumberOnlyTextField txtValue = new NumberOnlyTextField(null, this.negativeAllowed, this.integerScale, this.decimalScale, this.thousandSep, this.decimalMark);
		txtValue.setWidth("100%");
		txtValue.setRequiredIndicatorVisible(true);

		final Button btnRemove = new Button("", VaadinIcons.MINUS);
		btnRemove.setWidth("100%");
		btnRemove.addClickListener(y -> {
			this.gl.removeComponent(txtNote);
			this.gl.removeComponent(txtValue);
			this.gl.removeComponent(btnRemove);
			this.fieldPairList.remove(Pair.of(txtNote, txtValue));
		});

		this.gl.addComponents(txtNote, txtValue, btnRemove);

		this.fieldPairList.add(Pair.of(txtNote, txtValue));

	}

	@SuppressWarnings("unchecked")
	private HorizontalLayout buildBtnLayout() {

		final HorizontalLayout hlBtnLayout = new HorizontalLayout();
		hlBtnLayout.setSpacing(true);
		hlBtnLayout.setMargin(new MarginInfo(true, false, false, false));
		hlBtnLayout.setWidth("100%");

		// ---

		final Button btnAdd = new Button(I.trc("Button", "Add new line"));
		btnAdd.setWidth(null);
		btnAdd.addClickListener(y -> {
			this.addFieldPair();
			this.dialog.center();
		});

		// ---

		final Button btnOk = new Button(I.trc("Button", "OK"));
		btnOk.setWidth("100px");
		btnOk.addStyleName("min-width-150px");
		btnOk.addStyleName("max-width-400px");
		btnOk.addStyleName(ValoTheme.BUTTON_FRIENDLY);
		btnOk.addClickListener(y -> {

			final Map<String, String> valueMap = new HashMap<>();

			boolean hasError = false;

			for (final Pair<TextField, NumberOnlyTextField> tfp : this.fieldPairList) {

				final TextField txtNote = tfp.getKey();
				final NumberOnlyTextField txtValue = tfp.getValue();

				hasError = this.validateFields(txtNote, txtValue);

				txtValue.setComponentError(null);
				valueMap.put(txtNote.getValue(), txtValue.getValue());

			}

			if (!hasError) {

				if (!valueMap.isEmpty()) {

					final String oldValue = this.value;

					this.value = this.mapToValue(valueMap);
					
					this.valueSum();

					final Collection<?> listeners = this.getListeners(ValueChangeEvent.class);
					for (final Object listener : listeners) {
						((ValueChangeListener<String>) listener).valueChange(new ValueChangeEvent<>(this, oldValue, true));
					}

				}

				this.dialog.close();

			}

		});

		// ---

		hlBtnLayout.addComponent(btnOk);
		hlBtnLayout.addComponent(btnAdd);
		hlBtnLayout.setExpandRatio(btnAdd, 1f);

		return hlBtnLayout;
	}

	private boolean validateFields(final TextField txtNote, final NumberOnlyTextField txtValue) {

		if (StringUtils.isBlank(txtNote.getValue())) {

			txtNote.setComponentError(new UserError(""));
			Notification.show(I.trc("Notification", "Cannot be empty"), Notification.Type.WARNING_MESSAGE);

			return true;
		}

		if (StringUtils.isBlank(txtValue.getValue())) {

			txtValue.setComponentError(new UserError(""));
			Notification.show(I.trc("Notification", "Cannot be empty"), Notification.Type.WARNING_MESSAGE);

			return true;
		}

		try {

			new BigDecimal(txtValue.getValue());

		} catch (NumberFormatException e) {

			txtValue.setComponentError(new UserError(""));
			Notification.show(I.trc("Notification", "Not a number"), Notification.Type.WARNING_MESSAGE);

			return true;

		}

		if (txtNote.getValue() != null && txtNote.getValue().length() > this.validationMaxLength) {

			txtNote.setComponentError(new UserError(""));
			Notification.show(I.trc("Notification", "The field exceeds the maximum character count") + ":" + this.validationMaxLength, Notification.Type.WARNING_MESSAGE);

			return true;

		}

		if (txtValue.getValue() != null && txtValue.getValue().length() > this.validationMaxLength) {

			txtValue.setComponentError(new UserError(""));
			Notification.show(I.trc("Notification", "The field exceeds the maximum character count") + ":" + this.validationMaxLength, Notification.Type.WARNING_MESSAGE);

			return true;

		}

		return false;
	}

	@Override
	public String getValue() {
		return this.value;
	}

	@SuppressWarnings("hiding")
	@Override
	protected void doSetValue(final String value) {
		this.value = value;
	}

}