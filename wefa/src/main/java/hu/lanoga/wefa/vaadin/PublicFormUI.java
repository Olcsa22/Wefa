package hu.lanoga.wefa.vaadin;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.vaadin.cssinject.CSSInject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamunify.i18n.I;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Viewport;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.ToolboxSysKeys.CrudOperation;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.util.ToolboxAssert;
import hu.lanoga.toolbox.util.ToolboxGroovyHelper;
import hu.lanoga.toolbox.vaadin.AbstractToolboxUI;
import hu.lanoga.toolbox.vaadin.component.crud.FormLayoutCrudFormComponent;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import hu.lanoga.wefa.SysKeys;
import hu.lanoga.wefa.exception.WefaGeneralException;
import hu.lanoga.wefa.service.ActivitiHelperService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Theme("wefa")
// @PreserveOnRefresh // TODO: megfontolandó
@Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET_XHR)
@SpringUI(path = "/public/form")
@JavaScript(value = "https://cdnjs.cloudflare.com/ajax/libs/iframe-resizer/4.2.9/iframeResizer.contentWindow.min.js")
// @JavaScript(value = "https://cdnjs.cloudflare.com/ajax/libs/iframe-resizer/3.6.6/iframeResizer.contentWindow.min.js")
@Viewport("width=600")
public class PublicFormUI extends AbstractToolboxUI {

	public PublicFormUI() {
		super();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void init(final VaadinRequest vaadinRequest) {

		SecurityUtil.setAnonymous();

		// FIXME: hiba (pl. az Assert-ek) esetén átvisz a login oldalra, ez nem ideális public iframe esetén

		// próba: #11/Vzj5Cp8Lnc

		super.init(vaadinRequest);
		this.setLocale(new Locale("hu"));

		final String uriFragment = Page.getCurrent().getUriFragment();

		ToolboxAssert.isTrue(StringUtils.isNotBlank(uriFragment) && uriFragment.contains("/"));

		final String[] split = uriFragment.split("/");

		ToolboxAssert.isTrue(split.length == 2);

		final ActivitiHelperService activitiHelperService = ApplicationContextHelper.getBean(ActivitiHelperService.class);

		final int tenantId = Integer.parseInt(split[0].trim());
		final String procDefKey = split[1].trim();
		final String procDefId;

		try {
			SecurityUtil.setSystemUser();

			procDefId = activitiHelperService.getProcessDefinitionByKey(procDefKey, Integer.toString(tenantId)).getId();

			final LinkedHashMap<String, String> procDefGroovyScriptStrings = activitiHelperService.loadBackDeployedProcDefGroovyScriptStrings(procDefId);

			final String procDefPublicStartDataModelGroovyScriptStr = procDefGroovyScriptStrings.get(SysKeys.ProccessJsonObjectTypes.PROC_DEF_PUBLIC_START_DATA_MODEL);
			final String procDefPublicStartFormModelGroovyScriptStr = procDefGroovyScriptStrings.get(SysKeys.ProccessJsonObjectTypes.PROC_DEF_PUBLIC_START_FORM_MODEL);

			final Class domainClass = ToolboxGroovyHelper.buildClass(procDefPublicStartDataModelGroovyScriptStr);
			final Object domainObject = domainClass.newInstance();

			// TODO: ide tabnamek is kellenének, ha ezt akarjuk használni, mert különben nem látszik a tab
			// final MultiFormLayoutCrudFormComponent crudFormComponent = new MultiFormLayoutCrudFormComponent(() -> {
			final FormLayoutCrudFormComponent crudFormComponent = new FormLayoutCrudFormComponent(() -> {
				return ToolboxGroovyHelper.buildClassInstance(procDefPublicStartFormModelGroovyScriptStr);
			});
			crudFormComponent.setDomainObject((ToolboxPersistable) domainObject);
			crudFormComponent.setCrudOperation(CrudOperation.ADD); // itt ez nem az, mint normálisan lenne egy normál CRUD-ban... egyelőre az UPDATE és a READ kell csak

			final boolean isIframe = StringUtils.isNotBlank(vaadinRequest.getParameter("iframe"));

			crudFormComponent.setCrudAction(v -> {

				crudFormComponent.removeAllComponents();
				SecurityUtil.setAnonymous();

				final Map<String, Object> initialProcVarMap = new ObjectMapper().convertValue(domainObject, Map.class);

				activitiHelperService.startProcessFromPublicForm(tenantId, procDefId, initialProcVarMap);

				crudFormComponent.addComponent(new Label(I.trc("Label", "Űrlap leadás sikeres!"))); // TODO: ez még szebbre, illetve a szöveg is lehet, hogy a procdef editorban legyen megadható

				if (isIframe) {
					addIframeHeightMarker(crudFormComponent);
				}

				SecurityUtil.setAnonymous();

			});

			crudFormComponent.setId("form-component-publ");

			this.setContent(crudFormComponent);
			crudFormComponent.init();

			crudFormComponent.getBtnMain().setCaption(I.trc("Label", "Beküldés"));
			crudFormComponent.getBtnMain().setWidth("226px");
			crudFormComponent.getBtnMain().setIcon(VaadinIcons.INSERT);

			crudFormComponent.addComponent(new Label(), crudFormComponent.getComponentIndex(crudFormComponent.getBtnMain()));

			crudFormComponent.setWidth("100%");
			crudFormComponent.setMargin(new MarginInfo(true, true, false, true));

			if (isIframe) {
				addIframeHeightMarker(crudFormComponent);
			}
			
			// ---

			CSSInject css = new CSSInject(this);

			if (!UiHelper.useTouchMode() && !isIframe) {
				css.setStyles("#form-component-publ { min-width: 600px; max-width: 950px; }");
			} else if (isIframe) {
				css.setStyles("body { overflow: hidden !important; } .v-ui.v-scrollable { overflow: hidden !important; }");
			}

		} catch (final Exception e) {
			throw new WefaGeneralException(e);
		} finally {
			SecurityUtil.setAnonymous();
		}

	}

	private void addIframeHeightMarker(@SuppressWarnings("rawtypes") FormLayoutCrudFormComponent f) {

		final Label lblIframeSizer = new Label("<div data-iframe-height=\"true\" style=\"visibility: hidden;\">---</div>", ContentMode.HTML);
		lblIframeSizer.setStyleName(ValoTheme.LABEL_TINY);
		f.addComponent(lblIframeSizer);

	}

}
