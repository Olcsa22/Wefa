//package hu.lanoga.toolbox.cert;
//
//import javax.annotation.PostConstruct;
//
//import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import hu.lanoga.toolbox.spring.StartManager;
//import lombok.NoArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
///**
// * Certification rendszeres ellenőrzése... (újraindítja új cert esetén a Java alkalmazást, ezért fontos, hogy este/csúcsidőn kivül fusson csak...)
// */
//@SuppressWarnings("static-method")
//@Slf4j
//@NoArgsConstructor
//@ConditionalOnMissingBean(name = "letsEncryptSchedulerOverrideBean")
//@ConditionalOnProperty({"tools.job-runner", "tools.cert.letsencrpyt.scheduler.enabled"})
//@Component
//public class LetsEncryptScheduler {
//	
//	@PostConstruct
//	private void init() {	
//		log.info("CertScheduler initialized.");
//	}
//		
//	@Scheduled(cron = "${tools.cert.letsencrpyt.scheduler.cronExpression:0 0 * * * *}")
//	private void checkCert(){
//				
//		try {
//
//			if (LetsEncryptUtil.isRenewRequired()) {			
//				log.info("Restart required!");
//				StartManager.restart();
//			}
//			
//		} catch (Exception e){
//			log.error("An error happened while checking the certification!", e);
//		}
//		
//	}
//
//
//}
