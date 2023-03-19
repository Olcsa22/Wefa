package hu.lanoga.toolbox.vaadin.binder;

import com.vaadin.data.Converter;
import com.vaadin.data.Result;
import com.vaadin.data.ValueContext;

@SuppressWarnings("serial")
class NullConverter<T> implements Converter<T, T> {

	protected T nullRepresentation;

	public NullConverter(final T nullRepresentation) {
		this.nullRepresentation = nullRepresentation;
	}

	@Override
	public Result<T> convertToModel(final T value, final ValueContext context) {

		if (((this.nullRepresentation == null) && (value == null)) || ((this.nullRepresentation != null) && this.nullRepresentation.equals(value))) {
			return Result.ok(null);
		}

		return Result.ok(value);

	}

	@Override
	public T convertToPresentation(final T value, final ValueContext context) {
		return value == null ? this.nullRepresentation : value;
	}
}