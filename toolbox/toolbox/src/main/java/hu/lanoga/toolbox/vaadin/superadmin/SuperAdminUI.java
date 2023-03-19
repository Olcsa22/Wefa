package hu.lanoga.toolbox.vaadin.superadmin;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.google.common.util.concurrent.AtomicLongMap;
import com.teamunify.i18n.I;
import com.vaadin.annotations.Theme;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.tenant.Tenant;
import hu.lanoga.toolbox.tenant.TenantService;
import hu.lanoga.toolbox.util.BrandUtil;
import hu.lanoga.toolbox.util.DiagnosticsHelper;
import hu.lanoga.toolbox.vaadin.AbstractToolboxUI;
import hu.lanoga.toolbox.vaadin.component.CodeStoreComponent;
import hu.lanoga.toolbox.vaadin.component.EmailSenderDiagnosticsComponent;
import hu.lanoga.toolbox.vaadin.component.EmailTemplateComponent;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponent;
import hu.lanoga.toolbox.vaadin.component.crud.MultiFormLayoutCrudFormComponent;
import hu.lanoga.toolbox.vaadin.util.UiHelper;

// @PreserveOnRefresh
@Theme("super-admin-ui")
//@Push(value = PushMode.AUTOMATIC, transport = Transport.LONG_POLLING)
@ConditionalOnProperty(name = "tools.super-admin.ui.enabled", matchIfMissing = true)
@SpringUI(path = "/super-admin/v/main") // Vaadin 1X kompatibilitás miatt már nem használjuk toolbox alap UI-ok esetén ezt az annotációt, lásd még kapcsolodó Servlet class és hu.lanoga.toolbox.config.VaadinServletRegistrationConfig
public class SuperAdminUI extends AbstractToolboxUI {

	protected VerticalLayout vlContent;
	private VerticalLayout vlDiagnostics;

	public SuperAdminUI() {

		super();

		// ---

		SecurityUtil.limitAccess(ToolboxSysKeys.UserAuth.ROLE_SUPER_ADMIN_STR);

		// ---

		this.getPage().setTitle(BrandUtil.getAppTitle(true) + " " + I.trc("Title", "SUPER ADMIN UI"));

		// ---

		final VerticalLayout vlWrapper1 = new VerticalLayout();
		vlWrapper1.setWidth("100%");
		vlWrapper1.setHeight(null);
		vlWrapper1.setId("view-wrapper-1");

		this.setContent(vlWrapper1);

		final VerticalLayout vlWrapper2 = new VerticalLayout();
		vlWrapper2.setWidth("100%");
		vlWrapper2.setHeight(null);
		vlWrapper2.setId("view-wrapper-2");
		vlWrapper2.setMargin(true);
		vlWrapper2.setSpacing(true);

		vlWrapper1.addComponent(vlWrapper2);

		this.vlContent = new VerticalLayout();
		this.vlContent.setWidth("100%");
		this.vlContent.setHeight(null);

		vlWrapper2.addComponent(this.vlContent);
		vlWrapper2.setComponentAlignment(this.vlContent, Alignment.TOP_CENTER);
		vlWrapper2.setExpandRatio(this.vlContent, 1f);

	}

	@Override
	protected void init(final VaadinRequest vaadinRequest) {

		super.init(vaadinRequest);

		// ---

		final Button btnLogout = new Button(I.trc("Button", "Logout"));
		btnLogout.setIcon(VaadinIcons.EXIT);
		btnLogout.addClickListener(x -> UiHelper.logout(this));
		this.vlContent.addComponent(btnLogout);

		// ---

		final TabSheet ts = new TabSheet();
		ts.setWidth("100%");
		this.vlContent.addComponent(ts);

		final List<String> tabNames = new ArrayList<>();
		tabNames.add(I.trc("Caption", "Basic data"));
		tabNames.add(I.trc("Caption", "Company details"));
		tabNames.add(I.trc("Caption", "Notes, extra"));
		tabNames.add(I.trc("Caption", "Additional info"));

		final CrudGridComponent<Tenant> tenantCrud = new CrudGridComponent<>(
				Tenant.class,
				ApplicationContextHelper.getBean(TenantService.class),
				() -> new MultiFormLayoutCrudFormComponent<Tenant, Tenant.VaadinForm>(() -> new Tenant.VaadinForm(), tabNames) {

					@Override
					public void init() {
						super.init();

						if (this.getDomainObject().getId() == null) {
							this.getTs().getTab(1).setVisible(false);
							this.getTs().getTab(2).setVisible(false);
						}

					}

				},
				true);

		tenantCrud.toggleButtonVisibility(true, true, true, false, false, true, true, true);
		tenantCrud.setMargin(new MarginInfo(true, false, false, false));

		// gridCrud.setSelectionConsumer(t -> {log.debug("select consumer: " + t);});
		// gridCrud.setReadOnlyPredicate(t -> {log.debug("is read only predicate: " + t); return false;});
		// gridCrud.setBeforeOpenOperator(t -> {log.debug("before open op.: " + t); return t;});
		// gridCrud.setBeforeSaveOperator(t -> {log.debug("before save op.: " + t); return t;});

		ts.addTab(tenantCrud, I.trc("Caption", "Manage tenants"));

		// ---

		final CodeStoreComponent codeStoreComponent = new CodeStoreComponent();
		codeStoreComponent.initLayout();
		ts.addComponent(codeStoreComponent);
		ts.getTab(codeStoreComponent).setCaption(I.trc("Caption", "Codestore"));

		// ---

		final EmailSenderDiagnosticsComponent emailComponent = new EmailSenderDiagnosticsComponent();
		emailComponent.initLayout();
		ts.addComponent(emailComponent);
		ts.getTab(emailComponent).setCaption(I.trc("Caption", "Email sender diagnostics"));

		// ---

		final EmailTemplateComponent emailTemplateComponent = new EmailTemplateComponent();
		emailTemplateComponent.initLayout();
		ts.addComponent(emailTemplateComponent);
		ts.getTab(emailTemplateComponent).setCaption(I.trc("Caption", "Email templates"));

		// ---

		this.vlDiagnostics = new VerticalLayout();
		this.vlDiagnostics.setWidth("100%");
		ts.addTab(this.vlDiagnostics, I.trc("Caption", "Diagnostics"));
		this.refreshDiagnostics();

	}

	private void refreshDiagnostics() {

		this.vlDiagnostics.removeAllComponents();

		// ---

		this.vlDiagnostics.addComponent(new Label("")); // for spacing

		{

			final Button btnRefresh = new Button("Refresh", e -> this.refreshDiagnostics());
			btnRefresh.setIcon(VaadinIcons.REFRESH);
			btnRefresh.setWidth(null);
			this.vlDiagnostics.addComponent(btnRefresh);

		}

		this.vlDiagnostics.addComponent(new Label("")); // for spacing

		{
			final Pair<Integer, AtomicLongMap<String>> pair = UiHelper.getActiveUiCount();

			final Label lbl = new Label("active session count: " + SecurityUtil.getSessionPrincipalCount() + " (" + pair.getLeft() + ")");
			// lbl.setDescription(SecurityUtil.getSessionPrincipalListStr() + " | " + pair.getRight().toString());
			lbl.setDescription(pair.getRight().toString());
			this.vlDiagnostics.addComponent(lbl);
		}

		this.vlDiagnostics.addComponent(new Label("")); // for spacing

		{

			this.vlDiagnostics.addComponent(new Label("build (mvn) info: " + DiagnosticsHelper.getMavenBuildInfo()));
			this.vlDiagnostics.addComponent(new Label("env. info: " + DiagnosticsHelper.getEnvVersionInfoAlpha().getRight()));
			this.vlDiagnostics.addComponent(new Label("commit info: " + DiagnosticsHelper.getGitCommitInfo()));
			this.vlDiagnostics.addComponent(new Label("mac address: " + SecurityUtil.getPhysicalAddressHex()));
			this.vlDiagnostics.addComponent(UiHelper.buildMemDiagnosticAndManualGcComponent());
			this.vlDiagnostics.addComponent(UiHelper.buildActiveSessionCountAndVaadinUiCountDiagnosticLabel());
			this.vlDiagnostics.addComponent(UiHelper.buildShutdownMessageWarningTriggerBtn());

		}

		this.vlDiagnostics.addComponent(new Label("")); // for spacing

		{

			final Link link = new Link(I.trc("Button", "LOG"), new ExternalResource("/actuator/logfile"));
			link.setIcon(VaadinIcons.LIST_UL);
			this.vlDiagnostics.addComponent(link);

		}

		{

			final Link link = new Link(I.trc("Button", "Health"), new ExternalResource("/actuator/health"));
			link.setIcon(VaadinIcons.HEART);
			this.vlDiagnostics.addComponent(link);

		}

		{

			final Link link = new Link(I.trc("Button", "Info (version etc.)"), new ExternalResource("/actuator/info"));
			link.setIcon(VaadinIcons.INFO_CIRCLE);
			this.vlDiagnostics.addComponent(link);

		}

		{

			final Link link = new Link(I.trc("Button", "Metrics"), new ExternalResource("/actuator/metrics"));
			link.setIcon(VaadinIcons.MAGIC);
			this.vlDiagnostics.addComponent(link);

		}

	}

}
