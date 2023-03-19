package hu.lanoga.toolbox.vaadin.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.teamunify.i18n.I;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.ItemCaptionGenerator;
import com.vaadin.ui.Notification;
import com.vaadin.ui.RichTextArea;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.vaadin.util.UiHelper;

/**
 * Toolbox szokásos JSON nyelvesített mezőkhöz szerkesztő
 */
public class LangEditField extends CustomField<String> {

	private static final List<Locale> listOfCountries = new UnmodifiableList<>(initializeListOfCountries());

	private static List<Locale> initializeListOfCountries() {

		final List<Locale> list = new ArrayList<>();

		final String[] langCodes = {"en", "hu", "de"}; // TODO: hosszú volt a Locale.getISOLanguages()... tisztázni (vegyünk fel kézzel még pár (össz max 10-15 szokásos nyelvet aztán kész)

		for (final String langCode : langCodes) {
			list.add(new Locale(langCode));
		}

		return list;

	}

	private GridLayout gl;
	private Window dialog;

	private List<Pair<ComboBox<Locale>, TextField>> fieldPairList;
	private List<Pair<ComboBox<Locale>, TextArea>> fieldPairAreaList;
	private List<Pair<ComboBox<Locale>, RichTextArea>> fieldPairRichTextList;

	private boolean isTextArea = false;
	private boolean isRichTextArea = false;
	private final int validationMaxLength;

	private String value;

	/**
	 *
	 * @param caption
	 * @param validationMaxLength
	 * 		nyelvenként ennyi
	 */
	public LangEditField(final String caption, final int validationMaxLength) {
		this.validationMaxLength = validationMaxLength;
		this.setCaption(caption);
	}

	public LangEditField(final String caption, final int validationMaxLength, final boolean isTextArea) {
		this.validationMaxLength = validationMaxLength;
		this.setCaption(caption);
		this.isTextArea = isTextArea;
	}

	public LangEditField(final String caption, final int validationMaxLength, final boolean isTextArea, final boolean isRichTextArea) {
		this.validationMaxLength = validationMaxLength;
		this.setCaption(caption);
		this.isTextArea = isTextArea;
		this.isRichTextArea = isRichTextArea;

		if (this.isTextArea && this.isRichTextArea) {
			throw new ToolboxGeneralException("can't be both TextArea and RichTextArea");
		}

	}

	@Override
	protected Component initContent() {

		final Button btn = new Button(I.trc("Button", "Edit language field"));
		btn.setWidth("100%");

		btn.addClickListener(event -> {
			this.initDialog();
		});

		btn.addAttachListener(x -> {

			if (!this.isEnabled() || this.isReadOnly()) {

				btn.setCaption(I.trc("Button", "View language field"));

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
			strDialogCaption = I.trc("Title", "Edit language field");
		} else {
			strDialogCaption = I.trc("Title", "View language field");
		}

		strDialogCaption += " (" + Jsoup.clean(this.getCaption(), Safelist.none()) + ")";

		this.dialog = new Window(strDialogCaption, vlDialog);
		this.dialog.setWidth("700px");

		if (this.isRichTextArea) {
			this.dialog.setWidth("1300px");
			this.gl.setColumnExpandRatio(0, 0.25f);
			this.gl.setColumnExpandRatio(1, 1f);
			this.gl.setColumnExpandRatio(2, 0.10f);
		}

		this.dialog.setModal(true);

		UI.getCurrent().addWindow(this.dialog);
		UiHelper.forceDialogFocus(this.dialog);

		// ---

		this.fieldPairList = new ArrayList<>();
		this.fieldPairAreaList = new ArrayList<>();
		this.fieldPairRichTextList = new ArrayList<>();

		if (StringUtils.isNotBlank(this.value)) {

			final Map<Locale, String> valueMap = I18nUtil.multiLangToMap(this.value);

			for (final Entry<Locale, String> entry : valueMap.entrySet()) {

				this.addFieldPair();
				if (this.isTextArea) {
					final Pair<ComboBox<Locale>, TextArea> pair = this.fieldPairAreaList.get(this.fieldPairAreaList.size() - 1);
					pair.getKey().setValue(entry.getKey());
					pair.getValue().setValue(entry.getValue());
				} else if (this.isRichTextArea) {
					final Pair<ComboBox<Locale>, RichTextArea> pair = this.fieldPairRichTextList.get(this.fieldPairRichTextList.size() - 1);
					pair.getKey().setValue(entry.getKey());
					pair.getValue().setValue(entry.getValue());
				} else {
					final Pair<ComboBox<Locale>, TextField> pair = this.fieldPairList.get(this.fieldPairList.size() - 1);
					pair.getKey().setValue(entry.getKey());
					pair.getValue().setValue(entry.getValue());
				}
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

	private void addFieldPair() {

		final ComboBox<Locale> cmbLangCode = new ComboBox<>();
		cmbLangCode.setItems(LangEditField.listOfCountries);
		cmbLangCode.setEmptySelectionAllowed(false);
		cmbLangCode.setRequiredIndicatorVisible(true);
		cmbLangCode.setWidth("100%");
		cmbLangCode.setItemCaptionGenerator(new ItemCaptionGenerator<Locale>() {

			@Override
			public String apply(final Locale locale) {
				return locale.getDisplayName(I18nUtil.getLoggedInUserLocale()) + " (" + locale.toString() + ")";
			}
		});

		// ---

		if (this.isTextArea) {

			final TextArea taLangValue = new TextArea();
			taLangValue.setWidth("100%");
			taLangValue.setRequiredIndicatorVisible(true);

			final Button btnRemove = new Button("", VaadinIcons.MINUS);
			btnRemove.setWidth("100%");
			btnRemove.addClickListener(y -> {
				this.gl.removeComponent(cmbLangCode);
				this.gl.removeComponent(taLangValue);
				this.gl.removeComponent(btnRemove);
				this.fieldPairAreaList.remove(Pair.of(cmbLangCode, taLangValue));
			});

			this.gl.addComponents(cmbLangCode, taLangValue, btnRemove);

			this.fieldPairAreaList.add(Pair.of(cmbLangCode, taLangValue));

		} else if (this.isRichTextArea) {

			final RichTextArea rtaLangValue = new RichTextArea();
			rtaLangValue.setWidth("100%");
			rtaLangValue.setHeight("500px");
			rtaLangValue.setRequiredIndicatorVisible(true);

			final Button btnRemove = new Button("", VaadinIcons.MINUS);
			btnRemove.setWidth("100%");
			btnRemove.addClickListener(y -> {
				this.gl.removeComponent(cmbLangCode);
				this.gl.removeComponent(rtaLangValue);
				this.gl.removeComponent(btnRemove);
				this.fieldPairRichTextList.remove(Pair.of(cmbLangCode, rtaLangValue));
			});

			this.gl.addComponents(cmbLangCode, rtaLangValue, btnRemove);

			this.fieldPairRichTextList.add(Pair.of(cmbLangCode, rtaLangValue));

		} else {

			final TextField txtLangValue = new TextField();
			txtLangValue.setWidth("100%");
			txtLangValue.setRequiredIndicatorVisible(true);

			final Button btnRemove = new Button("", VaadinIcons.MINUS);
			btnRemove.setWidth("100%");
			btnRemove.addClickListener(y -> {
				this.gl.removeComponent(cmbLangCode);
				this.gl.removeComponent(txtLangValue);
				this.gl.removeComponent(btnRemove);
				this.fieldPairList.remove(Pair.of(cmbLangCode, txtLangValue));
			});

			this.gl.addComponents(cmbLangCode, txtLangValue, btnRemove);

			this.fieldPairList.add(Pair.of(cmbLangCode, txtLangValue));
		}

	}

	@SuppressWarnings("unchecked")
	private HorizontalLayout buildBtnLayout() {

		final HorizontalLayout hlBtnLayout = new HorizontalLayout();
		hlBtnLayout.setSpacing(true);
		hlBtnLayout.setMargin(new MarginInfo(true, false, false, false));
		hlBtnLayout.setWidth("100%");

		// ---

		final Button btnAdd = new Button(I.trc("Button", "Add new language"));
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

			final Map<Locale, String> valueMap = new HashMap<>();

			boolean hasError = false;

			if (this.isTextArea) {

				for (final Pair<ComboBox<Locale>, TextArea> tfp : this.fieldPairAreaList) {
					final ComboBox<Locale> cmbLangCode = tfp.getKey();
					final TextArea txtaLangValue = tfp.getValue();
					final String txtaValue = txtaLangValue.getValue();

					hasError = validateFields(hasError, cmbLangCode, txtaLangValue, txtaValue);

					txtaLangValue.setComponentError(null);
					valueMap.put(cmbLangCode.getValue(), txtaLangValue.getValue().trim());

				}

			} else if (this.isRichTextArea) {

				for (final Pair<ComboBox<Locale>, RichTextArea> tfp : this.fieldPairRichTextList) {
					final ComboBox<Locale> cmbLangCode = tfp.getKey();
					final RichTextArea txtaLangValue = tfp.getValue();
					final String txtaValue = txtaLangValue.getValue();

					hasError = validateFields(hasError, cmbLangCode, txtaLangValue, txtaValue);

					txtaLangValue.setComponentError(null);
					valueMap.put(cmbLangCode.getValue(), txtaLangValue.getValue().trim());

				}

			} else {

				for (final Pair<ComboBox<Locale>, TextField> tfp : this.fieldPairList) {

					final ComboBox<Locale> cmbLangCode = tfp.getKey();
					final TextField txtLangValue = tfp.getValue();
					final String txtValue = txtLangValue.getValue();

					hasError = validateFields(hasError, cmbLangCode, txtLangValue, txtValue);

					txtLangValue.setComponentError(null);
					valueMap.put(cmbLangCode.getValue(), txtLangValue.getValue().trim());

				}

			}

			if (!hasError) {

				if (!valueMap.isEmpty()) {

					final String oldValue = this.value;

					this.value = I18nUtil.packageIntoMultiLang(valueMap);

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

	@SuppressWarnings("rawtypes")
	private boolean validateFields(boolean hasError, ComboBox<Locale> cmbLangCode, AbstractField field, String strToValidate) {
	
		// TODO: strToValidate... ez nem a field.getValue()? miért kell két paraméter
		
		if (cmbLangCode.getValue() == null) {
			
			cmbLangCode.setComponentError(new UserError(""));
			hasError = true; // TODO: hasError parameternek nincs értelme, lehetne helyi változó is (amit a return-nel visszadunk a végén)
			Notification.show(I.trc("Notification", "Cannot be empty"), Notification.Type.WARNING_MESSAGE);

		} else if (StringUtils.isBlank(strToValidate)) {
		
			field.setComponentError(new UserError(""));
			hasError = true;
			Notification.show(I.trc("Notification", "Cannot be empty"), Notification.Type.WARNING_MESSAGE);

		} else if (strToValidate.length() > this.validationMaxLength) {
			
			field.setComponentError(new UserError(""));
			hasError = true;
			Notification.show(I.trc("Notification", "The field exceeds the maximum character count") + ":" + this.validationMaxLength, Notification.Type.WARNING_MESSAGE);
			
		}
		
		return hasError;
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