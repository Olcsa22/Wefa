package hu.lanoga.toolbox.vaadin.component;

import com.google.common.collect.Sets;
import com.vaadin.ui.VerticalLayout;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.filter.internal.SearchCriteria;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.tenant.TenantKeyValueSettings;
import hu.lanoga.toolbox.tenant.TenantKeyValueSettingsService;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponent;
import hu.lanoga.toolbox.vaadin.component.crud.FormLayoutCrudFormComponent;

public class TenantKeyValueSettingsComponent extends VerticalLayout {

	public TenantKeyValueSettingsComponent() {

		this.setWidth("100%");

		final SearchCriteria fixedSearchCriteria = SearchCriteria.builder()
				.fieldName("tenantId")
				.criteriaType(Integer.class)
				.operation(ToolboxSysKeys.SearchCriteriaOperation.EQ)
				.value(SecurityUtil.getLoggedInUserTenantId())
				.build();

		final SearchCriteria fixedSearchCriteria2 = SearchCriteria.builder()
				.fieldName("manualEditAllowed")
				.criteriaType(Integer.class)
				.operation(ToolboxSysKeys.SearchCriteriaOperation.EQ)
				.value(true)
				.build();

		final CrudGridComponent<TenantKeyValueSettings> tenantKeyValueSettingsCrudGridComponent = new CrudGridComponent<>(
				TenantKeyValueSettings.class,
				ApplicationContextHelper.getBean(TenantKeyValueSettingsService.class),
				() -> new FormLayoutCrudFormComponent<>(() -> new TenantKeyValueSettings.VaadinForm()),
				true, Sets.newHashSet(fixedSearchCriteria, fixedSearchCriteria2));

		tenantKeyValueSettingsCrudGridComponent.toggleButtonVisibility(true, false, true, false, false, true, true, true);

		tenantKeyValueSettingsCrudGridComponent.getGrid().getColumn("kvValue").setMaximumWidth(2000d);

		this.addComponent(tenantKeyValueSettingsCrudGridComponent);

	}
}