package hu.lanoga.toolbox.service;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;

public class TransactionHelper {

	private TransactionHelper() {
		//
	}

	/**
	 * tranz.-ban vagyunk-e épp (true/false = igen/nem)  
	 * 
	 * @return
	 */
	public static boolean isInActiveTransaction() {
		return TransactionSynchronizationManager.isActualTransactionActive();
	}
	
	/**
	 * tranz.-ban vagyunk-e épp (true/false = igen/nem), exception dob, ha nem
	 * 
	 * @return
	 */
	public static void limitToActiveTransaction() {
		
		if (!isInActiveTransaction()) {
			throw new ToolboxGeneralException("Not in a transaction!");
		}

	}

	/**
	 * @return null, ha nem vagyunk tranz.-ban épp
	 */
	public static TransactionStatus getCurrentTransactionStatus() {

		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			return TransactionAspectSupport.currentTransactionStatus();
		}

		return null;

	}
		
	/**
	 * (lehet, hogy nem megbízható, ha fontos valahol, akkor tesztelni kell)
	 * 
	 * @param propagationBehavior
	 * 		null esetén {@link TransactionDefinition#PROPAGATION_REQUIRED}
	 * @param runnables
	 */
	//@Deprecated
	public static void doInTransaction(final Integer propagationBehavior, final Runnable... runnables) {
		
		final PlatformTransactionManager platformTransactionManager = ApplicationContextHelper.getBean(PlatformTransactionManager.class);
		
		final TransactionTemplate txTemplate = new TransactionTemplate(platformTransactionManager);                
		txTemplate.setPropagationBehavior(propagationBehavior == null ? TransactionDefinition.PROPAGATION_REQUIRED : propagationBehavior);
		txTemplate.execute(new TransactionCallback<Object>() {
		
			@Override
			public Object doInTransaction(final TransactionStatus status) {
				
				limitToActiveTransaction();
				
				for (Runnable runnable : runnables) {
					runnable.run();
				}
		        		    	
		    	return null;
		    }
		});
		
	}

}
