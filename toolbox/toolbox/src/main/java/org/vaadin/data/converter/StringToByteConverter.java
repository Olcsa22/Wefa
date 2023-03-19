package org.vaadin.data.converter;

import java.text.NumberFormat;
import java.util.Locale;

import com.vaadin.data.Result;
import com.vaadin.data.ValueContext;
import com.vaadin.data.converter.AbstractStringToNumberConverter;

public class StringToByteConverter extends AbstractStringToNumberConverter<Byte> { // TODO: kell ez még?

    /**
     * Creates a new converter instance with the given error message. Empty
     * strings are converted to <code>null</code>.
     *
     * @param errorMessage
     *            the error message to use if conversion fails
     */
    public StringToByteConverter(final String errorMessage) {
        this(null, errorMessage);
    }

    /**
     * Creates a new converter instance with the given empty string value and
     * error message.
     *
     * @param emptyValue
     *            the presentation value to return when converting an empty
     *            string, may be <code>null</code>
     * @param errorMessage
     *            the error message to use if conversion fails
     */
    public StringToByteConverter(final Byte emptyValue, final String errorMessage) {
        super(emptyValue, errorMessage);
    }

    /**
     * Returns the format used by
     * {@link #convertToPresentation(Object, ValueContext)} and
     * {@link #convertToModel(String, ValueContext)}.
     *
     * @param locale
     *            The locale to use
     * @return A NumberFormat instance
     */
    @Override
    protected NumberFormat getFormat(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return NumberFormat.getIntegerInstance(locale);
    }

    @Override
    public Result<Byte> convertToModel(final String value, final ValueContext context) {
        final Result<Number> n = convertToNumber(value,context);
        return n.flatMap(number -> {
            if (number == null) {
                return Result.ok(null);
            } else {
                final byte intValue = number.byteValue();
                if (intValue == number.longValue()) {
                    // If the value of n is outside the range of long, the
                    // return value of longValue() is either Long.MIN_VALUE or
                    // Long.MAX_VALUE. The/ above comparison promotes int to
                    // long and thus does not need to consider wrap-around.
                    return Result.ok(intValue);
                } else {
                    return Result.error(getErrorMessage(context));
                }
            }
        });
    }

}
