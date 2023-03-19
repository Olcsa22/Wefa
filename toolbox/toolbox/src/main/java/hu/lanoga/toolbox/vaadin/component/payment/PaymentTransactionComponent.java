package hu.lanoga.toolbox.vaadin.component.payment;

import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import org.apache.commons.compress.utils.Sets;
import org.vaadin.dialogs.ConfirmDialog;

import com.teamunify.i18n.I;
import com.vaadin.data.provider.GridSortOrderBuilder;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.filter.internal.SearchCriteria;
import hu.lanoga.toolbox.payment.PaymentManager;
import hu.lanoga.toolbox.payment.PaymentTransaction;
import hu.lanoga.toolbox.payment.PaymentTransactionRawLog;
import hu.lanoga.toolbox.payment.PaymentTransactionRawLogService;
import hu.lanoga.toolbox.payment.PaymentTransactionService;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponent;
import hu.lanoga.toolbox.vaadin.component.crud.MultiFormLayoutCrudFormComponent;
import hu.lanoga.toolbox.vaadin.util.UiHelper;

/**
 * back-office komponens
 */
public class PaymentTransactionComponent extends VerticalLayout {

	final PaymentTransactionService paymentTransactionService;
	private PaymentTransaction selectedPaymentTransaction;
	private CrudGridComponent<PaymentTransaction> paymentTransactionCrud;

	private boolean isTransactionsCrudEnabled = true;
	private boolean isPaymentProviderConfigsCrudEnabled = true;

	public PaymentTransactionComponent(final boolean isTransactionsCrudEnabled, final boolean isPaymentProviderConfigsCrudEnabled) {
		this.paymentTransactionService = ApplicationContextHelper.getBean(PaymentTransactionService.class);

		this.isTransactionsCrudEnabled = isTransactionsCrudEnabled;
		this.isPaymentProviderConfigsCrudEnabled = isPaymentProviderConfigsCrudEnabled;
	}

	public PaymentTransactionComponent() {
		this.paymentTransactionService = ApplicationContextHelper.getBean(PaymentTransactionService.class);
	}

	public void initLayout() {

		this.removeAllComponents();
		
		// ---
		
		SecurityUtil.limitAccessRoleUser();
		SecurityUtil.limitAccessDisabled(true);

		// ---

		final TabSheet ts = new TabSheet();

		if (this.isTransactionsCrudEnabled) {

			final List<String> tabNames = new ArrayList<>();
			tabNames.add(I.trc("Caption", "Basic data"));
			tabNames.add(I.trc("Caption", "Notes, files"));
			tabNames.add(I.trc("Caption", "Provider specifc"));
			tabNames.add(I.trc("Caption", "Others"));

			this.paymentTransactionCrud = new CrudGridComponent<>(
					PaymentTransaction.class,
					this.paymentTransactionService,
					() -> new MultiFormLayoutCrudFormComponent<>(() -> new PaymentTransaction.VaadinForm(), tabNames),
					true);

			this.paymentTransactionCrud.setAsyncExport(true);

			this.paymentTransactionCrud.toggleButtonVisibility(true, false, false, false, false, true, true, true);
			this.paymentTransactionCrud.setMargin(new MarginInfo(true, false, false, false));
			final Grid<PaymentTransaction> grid = this.paymentTransactionCrud.getGrid();
			grid.setSortOrder(new GridSortOrderBuilder<PaymentTransaction>().thenDesc(grid.getColumn("id")));

			// ---

			this.addPaymentTransactionCmbFilter();

			// ---

			final Button btnRefund = new Button(I.trc("Button", "Refund"));
			btnRefund.setIcon(VaadinIcons.MONEY_WITHDRAW);
			btnRefund.setEnabled(false);
			btnRefund.addClickListener(x -> {

				final PaymentManager paymentManager = ApplicationContextHelper.getBean(PaymentManager.class);

				ConfirmDialog.show(UI.getCurrent(), I.trc("Caption", "Confirmation"),

						I.trc("Caption", "Refund transaction? (" + this.selectedPaymentTransaction.getOriginalAmount().setScale(2, RoundingMode.HALF_UP) + " " + this.selectedPaymentTransaction.getOriginalCurrency() + ")"),
						I.trc("Button", "Refund"), I.trc("Button", "Cancel"), new ConfirmDialog.Listener() {

							@Override
							public void onClose(final ConfirmDialog dialog) {

								if (dialog.isConfirmed()) {

									final PaymentTransaction refundTransaction = paymentManager.buildChildTransaction(PaymentTransactionComponent.this.selectedPaymentTransaction, ToolboxSysKeys.PaymentOperationType.REFUND);
									paymentManager.doTransaction(refundTransaction);

									PaymentTransactionComponent.this.paymentTransactionCrud.refreshGridWithAfterSelectAuto();

									if (refundTransaction.getStatus().equals(ToolboxSysKeys.PaymentTransactionStatus.SUCCESS)) {
										Notification.show(I.trc("Notification", "Refund successful"));
									}
								}

							}

						});

			});
			btnRefund.setVisible(Boolean.TRUE.equals(ApplicationContextHelper.getConfigProperty("tools.payment.refund.enabled", Boolean.class)));

			final Button btnSeeRawLog = new Button(I.trc("Button", "See raw log"));
			btnSeeRawLog.setIcon(VaadinIcons.FILE_TEXT);
			btnSeeRawLog.setVisible(SecurityUtil.hasAdminRole());
			btnSeeRawLog.setEnabled(false);
			btnSeeRawLog.addClickListener(x -> {

				final Window transactionDialog = new Window(I.trc("Title", "Payment transaction identifier") + ": #" + this.selectedPaymentTransaction.getId());
				transactionDialog.setModal(true);
				transactionDialog.setWidth("75%");

				UI.getCurrent().addWindow(transactionDialog);

				final VerticalLayout vl = new VerticalLayout();
				vl.setWidth("100%");

				final SearchCriteria fixedSearchCriteria = SearchCriteria.builder()
						.fieldName("paymentTransactionId")
						.criteriaType(Integer.class)
						.operation(ToolboxSysKeys.SearchCriteriaOperation.EQ)
						.value(this.selectedPaymentTransaction.getId())
						.build();

				final CrudGridComponent<PaymentTransactionRawLog> paymentRawLogGrid = new CrudGridComponent<>(
						PaymentTransactionRawLog.class,
						ApplicationContextHelper.getBean(PaymentTransactionRawLogService.class),
						() -> new MultiFormLayoutCrudFormComponent<>(() -> new PaymentTransactionRawLog.VaadinForm(), null),
						true, Sets.newHashSet(fixedSearchCriteria));

				paymentRawLogGrid.setMargin(false);
				paymentRawLogGrid.toggleButtonVisibility(true, false, false, false, false, false, true, false);
				vl.addComponent(paymentRawLogGrid);

				transactionDialog.setContent(vl);

			});
			
			final Button btnCheckStatus = new Button(I.trc("Button", "Force status check (manual)"));
			btnCheckStatus.setIcon(VaadinIcons.REFRESH);
			btnCheckStatus.setVisible(SecurityUtil.hasSuperAdminRole());
			btnCheckStatus.setEnabled(false);
			btnCheckStatus.addClickListener(x -> {

				final PaymentManager paymentManager = ApplicationContextHelper.getBean(PaymentManager.class);
				paymentManager.checkTransactionStatus(this.paymentTransactionService.findOne(this.selectedPaymentTransaction.getId()), true, true);
				
				PaymentTransactionComponent.this.paymentTransactionCrud.refreshGrid();
				Page.getCurrent().reload();
				
			});

			final HorizontalLayout hlFooter = new HorizontalLayout();
			hlFooter.addComponent(btnSeeRawLog);
			hlFooter.addComponent(btnCheckStatus);
			hlFooter.addComponent(btnRefund);

			this.paymentTransactionCrud.addAdditionalFooterToolbar(hlFooter);
			this.paymentTransactionCrud.setSelectionConsumer(x -> {

				if (x != null) {
					
					this.selectedPaymentTransaction = x;

					btnSeeRawLog.setEnabled(true);

					if (ApplicationContextHelper.getBean(PaymentManager.class).isViableForRefund(x)) {
						btnRefund.setEnabled(true);
					} else {
						btnRefund.setEnabled(false);
					}

					if (x.getManualStatusChange()) {
						this.paymentTransactionCrud.toggleButtonVisibility(true, false, true, false, false, true, true, true);
					} else {
						this.paymentTransactionCrud.toggleButtonVisibility(true, false, false, false, false, true, true, true);
					}
					
					btnCheckStatus.setEnabled(true);

				} else {
					
					this.selectedPaymentTransaction = null;
					
					btnRefund.setVisible(false);
					btnSeeRawLog.setEnabled(false);
					btnCheckStatus.setEnabled(false);

					this.paymentTransactionCrud.toggleButtonVisibility(true, false, false, false, false, true, true, true);

				}
			});

			ts.addTab(this.paymentTransactionCrud, I.trc("Caption", "Transactions"));

		}

		if (this.isPaymentProviderConfigsCrudEnabled) {

			final PaymentConfigComponent paymentConfigComponent = new PaymentConfigComponent();
			paymentConfigComponent.initLayout();

			final TabSheet.Tab configTab = ts.addTab(paymentConfigComponent, I.trc("Caption", "Payment provider configs"));

			final boolean hasAdminRole = SecurityUtil.hasRole(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR);
			configTab.setEnabled(hasAdminRole);

			if (!hasAdminRole) {
				configTab.setCaption(I.trc("Caption", "Payment provider configs (only admin can modify it)"));
			}

		}

		this.addComponent(ts);

	}

	private void addPaymentTransactionCmbFilter() {
		final ArrayList<String> views = new ArrayList<>();

		final String successView = I.trc("Caption", "success");
		final String failedView = I.trc("Caption", "failed");
		final String pendingView = I.trc("Caption", "pending");
		final String preparingView = I.trc("Caption", "preparing");
		final String currentView = I.trc("Caption", "last 7 days");
		final String allView = I.trc("Caption", "no main filter");

		views.add(successView);
		views.add(failedView);
		views.add(pendingView);
		views.add(currentView);
		views.add(allView);

		final ComboBox<String> cmbView = new ComboBox<>();
		cmbView.setItems(views);
		cmbView.setWidth("250px");
		cmbView.setTextInputAllowed(false);
		cmbView.setEmptySelectionAllowed(false);

		if (UiHelper.interpreteSearchUriFragment() != null) { // addValueChangeListener előtt legyen
			cmbView.setValue(allView);
		}

		cmbView.addValueChangeListener(x -> {

			this.paymentTransactionCrud.getBtnClearFilters().click();
			this.paymentTransactionCrud.getToolboxBackEndDataProvider().getFixedSearchCriteriaSet().clear();

			final Optional<String> selectedItem = cmbView.getSelectedItem();

			if (selectedItem.get().equalsIgnoreCase(successView)) {

				final SearchCriteria.SearchCriteriaBuilder searchCriteriaBuilder = SearchCriteria.builder()
						.fieldName("status")
						.criteriaType(Integer.class)
						.operation(ToolboxSysKeys.SearchCriteriaOperation.EQ)
						.value(ToolboxSysKeys.PaymentTransactionStatus.SUCCESS);

				this.paymentTransactionCrud.getToolboxBackEndDataProvider().getFixedSearchCriteriaSet().add(searchCriteriaBuilder.build());

			} else if (selectedItem.get().equalsIgnoreCase(failedView)) {

				final SearchCriteria.SearchCriteriaBuilder searchCriteriaBuilder = SearchCriteria.builder()
						.fieldName("status")
						.criteriaType(Integer.class)
						.operation(ToolboxSysKeys.SearchCriteriaOperation.EQ)
						.value(ToolboxSysKeys.PaymentTransactionStatus.FAILED);

				this.paymentTransactionCrud.getToolboxBackEndDataProvider().getFixedSearchCriteriaSet().add(searchCriteriaBuilder.build());

			} else if (selectedItem.get().equalsIgnoreCase(pendingView)) {

				final SearchCriteria.SearchCriteriaBuilder searchCriteriaBuilder = SearchCriteria.builder()
						.fieldName("status")
						.criteriaType(Integer.class)
						.operation(ToolboxSysKeys.SearchCriteriaOperation.EQ)
						.value(ToolboxSysKeys.PaymentTransactionStatus.PENDING);

				this.paymentTransactionCrud.getToolboxBackEndDataProvider().getFixedSearchCriteriaSet().add(searchCriteriaBuilder.build());

			} else if (selectedItem.get().equalsIgnoreCase(preparingView)) {

				final SearchCriteria.SearchCriteriaBuilder searchCriteriaBuilder = SearchCriteria.builder()
						.fieldName("status")
						.criteriaType(Integer.class)
						.operation(ToolboxSysKeys.SearchCriteriaOperation.EQ)
						.value(ToolboxSysKeys.PaymentTransactionStatus.GATEWAY_INIT);

				this.paymentTransactionCrud.getToolboxBackEndDataProvider().getFixedSearchCriteriaSet().add(searchCriteriaBuilder.build());

			} else if (selectedItem.get().equalsIgnoreCase(currentView)) {

				final Calendar c = Calendar.getInstance();
				c.setTimeInMillis(System.currentTimeMillis());
				c.add(Calendar.HOUR, -168); // 1 hét

				final Timestamp oneWeekAgo = new Timestamp(c.getTimeInMillis());

				final SearchCriteria.SearchCriteriaBuilder searchCriteriaBuilder = SearchCriteria.builder()
						.fieldName("createdOn")
						.criteriaType(Timestamp.class)
						.operation(ToolboxSysKeys.SearchCriteriaOperation.BIGGER_THAN)
						.value(oneWeekAgo);

				this.paymentTransactionCrud.getToolboxBackEndDataProvider().getFixedSearchCriteriaSet().add(searchCriteriaBuilder.build());

			}

			this.paymentTransactionCrud.refreshGrid();
		});

		if (cmbView.getValue() == null) { // addValueChangeListener után legyen
			if (this.paymentTransactionCrud.getSelectedItemIds().isEmpty()) {
				cmbView.setValue(currentView);
			} else {
				cmbView.setValue(allView);
			}
		}

		if (cmbView.getValue().equals(currentView)) {
			cmbView.setValue(UiHelper.interpreteSearchUriFragment() != null ? allView : currentView);
		}

		this.paymentTransactionCrud.addAdditionalHeaderToolbar(new HorizontalLayout(cmbView));
	}

	public CrudGridComponent<PaymentTransaction> getPaymentTransactionCrud() {
		return this.paymentTransactionCrud;
	}

}
