package hu.lanoga.toolbox.filter.input.angular;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import hu.lanoga.toolbox.filter.input.CompatiblePageRequest;
import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * Angular frontendhez lapozás, filterelés, sort...
 */
@Getter
@Setter
public class AngularPageRequest<T> implements CompatiblePageRequest<T> {
	
	private AngularPageRequestConverter<T> converter = new AngularPageRequestConverter<>();

	@Getter
	@Setter
	public static class SortOption {
		private String key;
		private String direction;
		private String method;
	}

	@Getter
	@Setter
	public static class SearchCriteria {
		private String criteriaType;
		private String key;
		private String operation;
		private Object value;
		private Object secondValue; // tól-ig esetén ez az "ig"...
	}

	private Integer pageSize;
	private Integer pageNumber;

	private List<SearchCriteria> searchCriteriaList;
	private List<SortOption> sortOptionList;

	private String logicalOperation;

	@JsonIgnore
	@Override
	public BasePageRequest<T> getAsBasePageRequest() {
		
		// TODO: Spring Converter regisztráció jobb lenne, de nincs hozzá annotáció... tisztáni (lehet, hogy @Configuration vagy @Component is elég)
		
		return converter.convert(this); 
	}

}