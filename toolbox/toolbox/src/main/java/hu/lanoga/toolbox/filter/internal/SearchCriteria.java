package hu.lanoga.toolbox.filter.internal;

import hu.lanoga.toolbox.ToolboxSysKeys;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.Setter;

/**
 * hashCode/equals a fieldName
 */
@Builder
@Getter
@Setter
public class SearchCriteria {
	
	// TODO: a builder-t kiszedni (csak a lombok annotációt, legyen kézzel megírva a builder azonosra...)
	// (voltak bugok a lombok builder kapcsán már többször)
	// (ha itt megy, akkor gond nélkül, akkor ne bolygassuk)

	private String fieldName;
	private ToolboxSysKeys.SearchCriteriaOperation operation;

	private Class<?> criteriaType;

	private Object value;
	
	/**
	 * tól-ig esetén ez az "ig"...
	 */
	private Object secondValue;
	
	@Default
	private boolean enabled = true;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SearchCriteria other = (SearchCriteria) obj;
		if (fieldName == null) {
			if (other.fieldName != null)
				return false;
		} else if (!fieldName.equals(other.fieldName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SearchCriteria [fieldName=");
		builder.append(fieldName);
		builder.append(", operation=");
		builder.append(operation);
		builder.append(", criteriaType=");
		builder.append(criteriaType);
		builder.append(", value=");
		builder.append(value);
		builder.append(", secondValue=");
		builder.append(secondValue);
		builder.append("]");
		return builder.toString();
	}

}
