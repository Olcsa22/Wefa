package org.vaadin.data.converter;

import com.vaadin.data.Converter;
import com.vaadin.data.Result;
import com.vaadin.data.ValueContext;

public class StringToCharacterConverter implements Converter<String, Character> {  // TODO: kell ez még?

    @Override
    public Result<Character> convertToModel(final String value, final ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }

        if (value.length()>1) {
            return Result.error("Could not convert '" + value);
        }

        return Result.ok(value.charAt(0));
    }

    @Override
    public String convertToPresentation(final Character value, final ValueContext context) {
        if (value == null) {
            return null;
        }

        return value.toString();
    }

}
