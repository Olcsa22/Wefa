package hu.lanoga.toolbox.filter.input.angular;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.SearchCriteriaOperation;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.filter.input.angular.AngularPageRequest.SearchCriteria;
import hu.lanoga.toolbox.filter.input.angular.AngularPageRequest.SortOption;
import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import hu.lanoga.toolbox.filter.internal.SearchCriteria.SearchCriteriaBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * AngularPageRequest -> BasePageRequest 
 * 
 * (nincs függősége, thread-safe...)
 */
@Slf4j
public class AngularPageRequestConverter<T> implements Converter<AngularPageRequest<T>, BasePageRequest<T>> {
	
	// mj.: ha módosítod, akkor figyelj a hasonló nevű (esetleg azonos, de más package) osztályokra, ne keveredj össze...

	@Override
	public BasePageRequest<T> convert(AngularPageRequest<T> source) {

		log.info("Paging from frontend... pageNumber: " + source.getPageNumber() + "; pageSize: " + source.getPageSize());

		int page = ObjectUtils.defaultIfNull(source.getPageNumber(), 0);
		int size = ObjectUtils.defaultIfNull(source.getPageSize(), 10);

		Sort sort = Sort.unsorted();
		LinkedHashSet<hu.lanoga.toolbox.filter.internal.SearchCriteria> searchCriteriaSet = null;
		ToolboxSysKeys.SearchCriteriaLogicalOperation searchCriteriaLogicalOperation = null;

		// ---

		log.info("Sort from frontend... sortOptionList: " + source.getSortOptionList());

		if (source.getSortOptionList() != null && !source.getSortOptionList().isEmpty()) {

			List<Order> orders = new ArrayList<>();

			for (SortOption sortOption : source.getSortOptionList()) {

				orders.add(new Order("DESC".equalsIgnoreCase(sortOption.getDirection()) ? Direction.DESC : Direction.ASC, sortOption.getKey()));
			}

			sort = Sort.by(orders);
		}

		// ---

		log.info("SearchCriteria list from frontend... searchCriteriaList: " + source.getSearchCriteriaList());

		if (source.getSearchCriteriaList() != null && !source.getSearchCriteriaList().isEmpty()) {

			searchCriteriaSet = new LinkedHashSet<>();

			for (SearchCriteria searchCriteria : source.getSearchCriteriaList()) {

				SearchCriteriaBuilder builder = hu.lanoga.toolbox.filter.internal.SearchCriteria.builder();

				builder.fieldName(searchCriteria.getKey());
				
				final SearchCriteriaOperation searchCriteriaOperation;

				if (StringUtils.isNotBlank(searchCriteria.getOperation())) {
					searchCriteriaOperation = ToolboxSysKeys.SearchCriteriaOperation.valueOf(searchCriteria.getOperation().toUpperCase());
				} else {
					searchCriteriaOperation = ToolboxSysKeys.SearchCriteriaOperation.EQ;
				}
				
				builder.operation(searchCriteriaOperation);

				// ---

				String criteriaType = searchCriteria.getCriteriaType();

				log.info("SearchCriteria from frontend... criteriaType: " + criteriaType + "; intial deserialized type (Jackson auto): " + searchCriteria.getValue().getClass().getName() + "; value: " + searchCriteria.getValue() + "; secondValue: " + searchCriteria.getSecondValue());

				if (StringUtils.isBlank(criteriaType)) {

					// "okosban" segít kitalálni az ágat/típust

					if (searchCriteria.getValue() instanceof Number) {
						criteriaType = searchCriteria.getValue().getClass().getSimpleName().toUpperCase();
					} else if (searchCriteria.getValue() instanceof Boolean) {
						criteriaType = "BOOLEAN";
					} else if (ObjectUtils.allNotNull(searchCriteria.getValue(), searchCriteria.getSecondValue()) && searchCriteria.getValue() instanceof String && searchCriteria.getValue().toString().contains("Z")) {
						criteriaType = "TIMESTAMP";
					} else if (ObjectUtils.allNotNull(searchCriteria.getValue(), searchCriteria.getSecondValue()) && searchCriteria.getValue() instanceof String) {
						criteriaType = "DATE";
					}

				}
				
				if (ToolboxSysKeys.SearchCriteriaOperation.IN.equals(searchCriteriaOperation) && ("INT".equalsIgnoreCase(criteriaType) || "INTEGER".equalsIgnoreCase(criteriaType))) {
					
					builder.criteriaType(Integer.class);
					
					if (searchCriteria.getValue() instanceof Set) {
						builder.value(searchCriteria.getValue());
					} else {
						
						Set<Integer> set = new HashSet<>();
						String[] split = searchCriteria.getValue().toString().split(",");
						
						for (String s : split) {
							set.add(Integer.parseInt(s.trim()));
						}
						
						builder.value(set);
						
					}

				} else if ("INT".equalsIgnoreCase(criteriaType) || "INTEGER".equalsIgnoreCase(criteriaType)) {

					builder.criteriaType(Integer.class);

					if (searchCriteria.getValue() instanceof Integer) {
						builder.value(searchCriteria.getValue());
					} else {
						builder.value(Integer.parseInt(searchCriteria.getValue().toString()));
					}

					if (searchCriteria.getSecondValue() != null) {
						if (searchCriteria.getSecondValue() instanceof Integer) {
							builder.secondValue(searchCriteria.getValue());
						} else {
							builder.secondValue(Integer.parseInt(searchCriteria.getSecondValue().toString()));
						}
					}

				} else if ("LONG".equalsIgnoreCase(criteriaType) || "LONGINT".equalsIgnoreCase(criteriaType)) {

					builder.criteriaType(Long.class);

					if (searchCriteria.getValue() instanceof Long) {
						builder.value(searchCriteria.getValue());
					} else {
						builder.value(Long.parseLong(searchCriteria.getValue().toString()));
					}

					if (searchCriteria.getSecondValue() != null) {
						if (searchCriteria.getSecondValue() instanceof Long) {
							builder.secondValue(searchCriteria.getValue());
						} else {
							builder.secondValue(Long.parseLong(searchCriteria.getSecondValue().toString()));
						}
					}

				} else if ("BIGDECIMAL".equalsIgnoreCase(criteriaType) || "DECIMAL".equalsIgnoreCase(criteriaType) || "NUMBER".equalsIgnoreCase(criteriaType)) {

					builder.criteriaType(BigDecimal.class);

					if (searchCriteria.getValue() instanceof BigDecimal) {
						builder.value(searchCriteria.getValue());
					} else {
						builder.value(new BigDecimal(searchCriteria.getValue().toString()));
					}

					if (searchCriteria.getSecondValue() != null) {
						if (searchCriteria.getSecondValue() instanceof BigDecimal) {
							builder.secondValue(searchCriteria.getValue());
						} else {
							builder.secondValue(new BigDecimal(searchCriteria.getSecondValue().toString()));
						}
					}

				} else if ("FLOAT".equalsIgnoreCase(criteriaType) || "REAL".equalsIgnoreCase(criteriaType)) {

					builder.criteriaType(Float.class);

					if (searchCriteria.getValue() instanceof Float) {
						builder.value(searchCriteria.getValue());
					} else {
						builder.value(Float.parseFloat(searchCriteria.getValue().toString()));
					}

					if (searchCriteria.getSecondValue() != null) {
						if (searchCriteria.getSecondValue() instanceof Float) {
							builder.secondValue(searchCriteria.getValue());
						} else {
							builder.secondValue(Float.parseFloat(searchCriteria.getSecondValue().toString()));
						}
					}

				} else if ("DOUBLE".equalsIgnoreCase(criteriaType)) {

					builder.criteriaType(Double.class);

					if (searchCriteria.getValue() instanceof Double) {
						builder.value(searchCriteria.getValue());
					} else {
						builder.value(Double.parseDouble(searchCriteria.getValue().toString()));
					}

					if (searchCriteria.getSecondValue() != null) {
						if (searchCriteria.getSecondValue() instanceof Double) {
							builder.secondValue(searchCriteria.getValue());
						} else {
							builder.secondValue(Double.parseDouble(searchCriteria.getSecondValue().toString()));
						}
					}

				} else if ("BOOL".equalsIgnoreCase(criteriaType) || "BOOLEAN".equalsIgnoreCase(criteriaType)) {

					builder.criteriaType(Boolean.class);

					if (searchCriteria.getValue() instanceof Boolean) {
						builder.value(searchCriteria.getValue());
					} else {
						builder.value(Boolean.parseBoolean(searchCriteria.getValue().toString()));
					}

					if (searchCriteria.getSecondValue() != null) {
						if (searchCriteria.getSecondValue() instanceof Boolean) {
							builder.secondValue(searchCriteria.getValue());
						} else {
							builder.secondValue(Boolean.parseBoolean(searchCriteria.getSecondValue().toString()));
						}
					}

				} else if ("DATE".equalsIgnoreCase(criteriaType)) {

					builder.criteriaType(java.sql.Date.class);

					boolean a = searchCriteria.getValue() instanceof java.sql.Date;
					boolean b = searchCriteria.getSecondValue() instanceof java.sql.Date;

					if (a) {
						builder.value(searchCriteria.getValue());
					}

					if (searchCriteria.getSecondValue() != null && b) {
						builder.secondValue(searchCriteria.getSecondValue());
					}

					SimpleDateFormat format = null;

					if (!(a && b)) {

						format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

						try {

							if (!a) {
								String valueAsString = searchCriteria.getValue().toString();
								builder.value(new java.sql.Date(format.parse(valueAsString).getTime()));
							}

							if (!b && searchCriteria.getSecondValue() != null && StringUtils.isNotBlank(searchCriteria.getSecondValue().toString())) {
								builder.secondValue(new java.sql.Date(format.parse(searchCriteria.getSecondValue().toString()).getTime()));
							}

						} catch (ParseException e) {
							throw new ToolboxGeneralException(e); // TODO: tisztázni (legyen saját toolbox-os ex class) (mármint a filterekhez egy, nem kell túlvariálni) (PT)
						}

					}

				} else if ("DATETIME".equalsIgnoreCase(criteriaType) || "TIMESTAMP".equalsIgnoreCase(criteriaType) || "TIMESTAMPTZ".equalsIgnoreCase(criteriaType)) {

					builder.criteriaType(java.sql.Timestamp.class);

					boolean a = searchCriteria.getValue() instanceof java.sql.Timestamp;
					boolean b = searchCriteria.getSecondValue() instanceof java.sql.Timestamp;

					if (a) {
						builder.value(searchCriteria.getValue());
					}

					if (searchCriteria.getSecondValue() != null && b) {
						builder.secondValue(searchCriteria.getSecondValue());
					}

					SimpleDateFormat format = null;

					if (!(a && b)) {

						format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

						try {

							if (!a) {
								String valueAsString = searchCriteria.getValue().toString();
								builder.value(format.parse(valueAsString));
							}

							if (!b && searchCriteria.getSecondValue() != null && StringUtils.isNotBlank(searchCriteria.getSecondValue().toString())) {
								builder.secondValue(new java.sql.Timestamp(format.parse(searchCriteria.getSecondValue().toString()).getTime()));
							}

						} catch (ParseException e) {
							throw new ToolboxGeneralException("AngularPageRequestConverter error!", e);
						}

					}

				} else {

					builder.criteriaType(String.class);

					if (searchCriteria.getValue() instanceof String) {
						builder.value(searchCriteria.getValue());
					} else {
						builder.value(searchCriteria.getValue().toString());
					}

					if (searchCriteria.getSecondValue() != null) {
						if (searchCriteria.getSecondValue() instanceof String) {
							builder.secondValue(searchCriteria.getValue());
						} else {
							builder.secondValue(searchCriteria.getSecondValue().toString());
						}
					}

				}

				searchCriteriaSet.add(builder.build());
			}

		}

		// ---

		log.info("logicalOperation from frontend: " + source.getLogicalOperation());

		if ("OR".equalsIgnoreCase(source.getLogicalOperation())) {
			searchCriteriaLogicalOperation = ToolboxSysKeys.SearchCriteriaLogicalOperation.OR;
		} else {
			searchCriteriaLogicalOperation = ToolboxSysKeys.SearchCriteriaLogicalOperation.AND;
		}

		// ---

		BasePageRequest<T> basePageRequest = new BasePageRequest<>(page, size, sort, searchCriteriaSet, searchCriteriaLogicalOperation);

		log.info("Converted request (BasePageRequest): " + basePageRequest);

		return basePageRequest;

	}

}
