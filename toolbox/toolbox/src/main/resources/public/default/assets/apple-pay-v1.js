
function showApplePayIfViable() {
	
	const t = '<div class="apple-pay-button apple-pay-button-black" onclick="applePayButtonClicked()"></div>';

	var viable = false;
	
    if (typeof ApplePaySession !== 'undefined') {
        if (ApplePaySession.canMakePayments()) {
        	viable = true;
        }
    }
        
    var box = document.getElementsByClassName('apple-pay-box')[0];
    
    if (viable) {
    	
    	box.firstChild.innerHTML = t;
    	
    } else {
    	
    	var x = box.parentElement.parentElement.parentElement.parentElement;
    	
    	x.style.display = 'none';
    	    	
    	var spacer = x.parentElement.nextElementSibling;
    	    	
    	if (spacer.classList.contains('v-spacing')) { // ha utolsó elem, akkor nincs
    		spacer.style.display = 'none';
    	}
    	
    }
        
}

function applePayButtonClicked() {
		        
    apSession = new ApplePaySession(4, apPaymentRequest);
    
    apSession.onvalidatemerchant = (event) => {  
    	hu.lanoga.toolbox.vaadin.component.payment.PaymentCustomerStartComponent.applePayJsCallback("step1", event.validationURL);
    };
    
    apSession.onpaymentauthorized  = (event) => {
    	hu.lanoga.toolbox.vaadin.component.payment.PaymentCustomerStartComponent.applePayJsCallback("step2", event.payment);
    }
    
    // TODO: onpayment... not authorized... timeout... listeners (to show some message)
    
    apSession.begin();
   
}
