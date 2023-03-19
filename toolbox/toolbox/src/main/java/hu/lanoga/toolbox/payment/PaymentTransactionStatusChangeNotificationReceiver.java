package hu.lanoga.toolbox.payment;

public interface PaymentTransactionStatusChangeNotificationReceiver {

	/**
	 * @param paymentTransaction
	 * 		figyelj arra, hogy a model idővel változahat, egyes értékek lehetnek null-ok...   
	 * 		friss (copyProperties..) objektum, bármit módosítasz rajta, az nem hat "vissza";  
	 * 
	 * @param isSuccessful
	 * 		true = sikeres fizetés, false = sikertelen, null = még nem érte el a végső állapotát
	 */
	@SuppressWarnings("unused")
	default void transactionStatusChanged(final PaymentTransaction paymentTransaction, final Boolean isSuccessful) {
		//
	}

	// TODO: elvileg refund esetén is megy, de tesztelni kell

	// TODO: most nem így van, mert csak egy tranz.-ban megy a PaymentTransaction, a raw log tábla mentés és az app sepcifikus mentés rendesen...
	// DB tranzakció kezelés kapcsán itt ajánlott {@link Transactional} {@link Propagation#REQUIRES_NEW} beiktatása
	// (az implementáló service ezen metódusán) (de nem mindig! alkalmazás függő!)

}
