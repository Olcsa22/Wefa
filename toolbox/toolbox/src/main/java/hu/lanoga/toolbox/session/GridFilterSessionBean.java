package hu.lanoga.toolbox.session;

import com.google.common.collect.Sets;
import hu.lanoga.toolbox.filter.internal.SearchCriteria;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.util.Set;

@Getter
@Setter
@Component
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS, value = "session")
public class GridFilterSessionBean {

	private Set<SearchCriteria> initialFixedSearchCriteriaSet;

	public void add(final SearchCriteria searchCriteria) {
		if (this.initialFixedSearchCriteriaSet == null) {
			this.initialFixedSearchCriteriaSet = Sets.newHashSet();
		}

		this.initialFixedSearchCriteriaSet.add(searchCriteria);
	}

	public void remove(final SearchCriteria searchCriteria) {
		this.initialFixedSearchCriteriaSet.removeIf(x -> {
			if (x.equals(searchCriteria)) {
				return true;
			}

			return false;
		});
	}
	
	public void clear() {
		this.initialFixedSearchCriteriaSet.clear();
	}
	
	public Set<SearchCriteria> getAll() {
		if (this.initialFixedSearchCriteriaSet == null) {
			this.initialFixedSearchCriteriaSet = Sets.newHashSet();
		}

		return this.initialFixedSearchCriteriaSet;
	}
	
	public Set<SearchCriteria> getAllAndClear() {
		final Set<SearchCriteria> currentAll = this.getAll();
		this.initialFixedSearchCriteriaSet.removeAll(currentAll);
		return currentAll;
	}

}
