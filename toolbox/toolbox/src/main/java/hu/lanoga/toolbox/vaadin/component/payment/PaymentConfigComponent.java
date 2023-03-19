package hu.lanoga.toolbox.vaadin.component.payment;

import com.vaadin.ui.VerticalLayout;

import hu.lanoga.toolbox.payment.PaymentConfig;
import hu.lanoga.toolbox.payment.PaymentConfigService;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponent;
import hu.lanoga.toolbox.vaadin.component.crud.MultiFormLayoutCrudFormComponent;

class PaymentConfigComponent extends VerticalLayout {

    final PaymentConfigService paymentConfigService;

    public PaymentConfigComponent() {
        this.paymentConfigService = ApplicationContextHelper.getBean(PaymentConfigService.class);
    }

    public void initLayout() {
    	
		// ---
		
		SecurityUtil.limitAccessAdmin();
		SecurityUtil.limitAccessDisabled(true);
    	
    	// ---

        this.removeAllComponents();

        // ---

        final CrudGridComponent<PaymentConfig> paymentConfigCrud = new CrudGridComponent<>(
				PaymentConfig.class,
                this.paymentConfigService,
                () -> new MultiFormLayoutCrudFormComponent<>(() -> new PaymentConfig.VaadinForm(), null),
                true);

        paymentConfigCrud.toggleButtonVisibility(true, true, true, true, false, true, true, false);

        paymentConfigCrud.setSelectionConsumer(x -> {

        	if (x == null) {
				return;
			}

			paymentConfigCrud.toggleEnableButton(true, true, true, true);
			

		});

        this.addComponent(paymentConfigCrud);

    }

}
