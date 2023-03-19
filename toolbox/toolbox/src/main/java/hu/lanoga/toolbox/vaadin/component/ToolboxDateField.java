package hu.lanoga.toolbox.vaadin.component;

import java.time.LocalDate;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.data.Result;
import com.vaadin.ui.DateField;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ToolboxDateField extends DateField {

	@Override
	protected Result<LocalDate> handleUnparsableDateString(String dateString) {
		
		if (StringUtils.isBlank(dateString)) {
			return Result.error("Not a valid date!");
		}
		
		try {
			
			// TODO: tisztázni, időzőna stb.
			
			dateString = StringUtils.replaceAll(dateString, " ", "");
			dateString = StringUtils.replaceAll(dateString, "-", "");
			dateString = StringUtils.replaceAll(dateString, "\\.", "");

			if (dateString.length() == 6) {
				dateString += "20" + dateString;
			}
			
			String year = dateString.substring(0, 4);
			String month = dateString.substring(4, 6);
			String day = dateString.substring(6, 8);

			return Result.ok(LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day))); // TODO: tiszázni UiHelper.adjustToPageTimeZone(millis)
			
		} catch (Exception e) {
			log.warn("handleUnparsableDateString error", e);
			return Result.error("Not a valid date!");
		}

	}
	
	public ToolboxDateField() {
		super();
	}

	public ToolboxDateField(String caption, LocalDate value) {
		super(caption, value);
	}

	public ToolboxDateField(String caption) {
		super(caption);
	}

	public ToolboxDateField(ValueChangeListener<LocalDate> valueChangeListener) {
		super();
		addValueChangeListener(valueChangeListener);
	}

	public ToolboxDateField(String caption,
					 ValueChangeListener<LocalDate> valueChangeListener) {
		this(valueChangeListener);
		setCaption(caption);
	}

	public ToolboxDateField(String caption, LocalDate value,
					 ValueChangeListener<LocalDate> valueChangeListener) {
		this(caption, value);
		addValueChangeListener(valueChangeListener);
	}

}
