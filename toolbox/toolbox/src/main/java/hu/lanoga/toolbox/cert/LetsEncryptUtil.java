//package hu.lanoga.toolbox.cert;
//
//import java.io.BufferedInputStream;
//import java.io.ByteArrayInputStream;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.security.KeyPair;
//import java.security.KeyStore;
//import java.security.Security;
//import java.security.cert.Certificate;
//import java.security.cert.CertificateFactory;
//import java.security.cert.X509Certificate;
//import java.util.Arrays;
//import java.util.Date;
//
//import javax.servlet.http.HttpServletResponse;
//
//import org.apache.commons.io.FileUtils;
//import org.apache.commons.io.IOUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.bouncycastle.jce.provider.BouncyCastleProvider;
//import org.shredzone.acme4j.util.KeyPairUtils;
//
//import lombok.extern.slf4j.Slf4j;
//import spark.Request;
//import spark.Response;
//import spark.Route;
//import spark.Spark;
//
///**
// * teszt ngrok-kal, extraként meghagyott sima http port (így az embedded Tomcat 443/https és 8080/http connectorral fut majd): 
// * -DdomainForLe=fc5b2644.ngrok.io -DleStaging=true -DportForAfterLeSecondaryHttp=8080 
// * (a portForAfterLeSecondaryHttp nem megy jelenleg, Spring Boot 2 óta)
// * 
// * staging/prod letsencryt mód váltásnál ki kell kézzel törölni a cert fájlt... 
// * (auto megújítás teszthez kell még ez (90 nap - 3 perc): -DleMillisLeftBeforeRenew=7775820000) (időzóna bekavarhat, lehet, pár órát hagyni rá)
// */
//@Slf4j
//public final class LetsEncryptUtil {
//
//	private LetsEncryptUtil() {
//		//
//	}
//
//	// TODO: itt a jelszavak lehetnek beégetve, viszont akkor legyen ez is a ENC(-es enkódolással, használja azt a dekód jelszót, amit a Spring részek is (viszont itt kézzel kell megírni, mert ez Spring-en kivül van)
//
//	private static final String KEYSTORE_PASSWD = "Cap4DEbRup3u";
//	private static final String KEY_PASSWD = "fRepuC58egAs";
//
//	/**
//	 * Let’s Encrypt tanúsítvány beszerzése és Spring Boot beállítás...
//	 * (SpringApplication.run() előtt kell hívni!)
//	 * 
//	 * Lásd még
//	 * -DdomainForLe (domain ami alatt elérhető az oldal, ha nincs megadva, akkor nem kér tanúsítványt)
//	 * -DleStaging paraméter (true esetén staging/test letsencrpyt mód)!
//	 *
//	 * @return true = sikerült, false = nem (exception nem dob soha) (akkor is false, ha a domainForLe property hiányzik)
//	 */
//	public static synchronized boolean aquireAndSetCert() {
//
//		try {
//
//			final String domainForLe = System.getProperty("domainForLe");
//
//			if (StringUtils.isBlank(domainForLe)) {
//				// log.info("Property \"domainForLe\" is missing or empty (cert req. skipped)!");
//				return false;
//			}
//
//			final String portForLePre = StringUtils.defaultIfBlank(System.getProperty("portForLePre"), "80"); // port a letsencrpyt challange-hez, jelenleg csak 80-as porton tudja csinálni a http challange-et (ezért nincs is értelme átállítani)
//			final String leStaging = StringUtils.defaultIfBlank(System.getProperty("leStaging"), "false"); // staging=true -> test/dev üzemmód
//
//			final String portForHttps = StringUtils.defaultIfBlank(System.getProperty("portForLeHttps"), "443"); // ez már a Spring-es Tomcat-hez lesz a port (jelenleg csak a 443 támogatott letsencrpyt-ék által, nincs értelme átállítani)
//			// final String portForAfterLeSecondaryHttp = System.getProperty("portForAfterLeSecondaryHttp"); // ez már a Spring-es Tomcat-hez lesz a port
//
//			// ---
//
//			BouncyCastleProvider bouncyCastleProvider = null;
//
//			try {
//
//				bouncyCastleProvider = new BouncyCastleProvider();
//				Security.addProvider(bouncyCastleProvider);
//
//				Spark.port(Integer.parseInt(portForLePre));
//				Spark.get("/.well-known/acme-challenge/*", new Route() {
//
//					@Override
//					public Object handle(Request req, Response resp) throws Exception {
//
//						HttpServletResponse response = resp.raw();
//
//						response.setHeader("Content-Type", "application/octet-stream");
//
//						log.info("CertUtil 1...");
//
//						if (AcmeClient.lastChallange != null && AcmeClient.lastChallange.getAuthorization() != null && AcmeClient.lastChallange.getToken() != null) {
//
//							response.setHeader("Content-Length", Long.toString(AcmeClient.lastChallange.getAuthorization().getBytes().length));
//							response.setHeader("Content-Disposition", "Content-Disposition: attachment; filename=\"" + AcmeClient.lastChallange.getToken() + "\"");
//
//							IOUtils.write(AcmeClient.lastChallange.getAuthorization().getBytes(), response.getOutputStream());
//
//							log.info("CertUtil 2a: " + AcmeClient.lastChallange.getToken());
//
//						} else {
//
//							response.setHeader("Content-Length", "0");
//							response.setHeader("Content-Disposition", "Content-Disposition: attachment; filename=\"404.txt\"");
//
//							log.warn("CertUtil 2b (missing)");
//						}
//
//						return null;
//
//					}
//				});
//
//				Spark.awaitInitialization();
//
//				boolean isLeStaging = leStaging != null && Boolean.parseBoolean(leStaging);
//
//				AcmeClient.fetchCertificate(Arrays.asList(domainForLe), isLeStaging);
//
//			} finally {
//
//				try {
//
//					Spark.stop();
//
//					if (bouncyCastleProvider != null) {
//						Security.removeProvider(bouncyCastleProvider.getName());
//					}
//
//				} catch (Exception e) {
//					log.debug("Spark and/or BouncyCastleProvider stop/remove failed!", e);
//				}
//
//			}
//
//			// ---
//
//			final KeyStore ks = KeyStore.getInstance("JKS");
//			ks.load(null, null);
//
//			final CertificateFactory cf = CertificateFactory.getInstance("X.509");
//			final Certificate certs = cf.generateCertificate(new ByteArrayInputStream(FileUtils.readFileToByteArray(AcmeClient.DOMAIN_CHAIN_FILE)));
//
//			KeyPair kp = null;
//
//			try (FileReader fr = new FileReader(AcmeClient.DOMAIN_KEY_FILE)) {
//				kp = KeyPairUtils.readKeyPair(fr);
//			}
//
//			ks.setKeyEntry("k1", kp.getPrivate(), KEY_PASSWD.toCharArray(), new Certificate[] { certs });
//
//			if (AcmeClient.KEYSTORE_FILE.exists()) {
//				final boolean deletedQuietly = FileUtils.deleteQuietly(AcmeClient.KEYSTORE_FILE);
//				if (!deletedQuietly) {
//					throw new IOException("JKS file could not be deleted!");
//				}
//			}
//
//			try (OutputStream os = new FileOutputStream(AcmeClient.KEYSTORE_FILE)) {
//				ks.store(os, KEYSTORE_PASSWD.toCharArray());
//			}
//
//			// ---
//
//			log.debug("Certificate for domain: " + domainForLe + " is valid.");
//
//			// ---
//
//			System.setProperty("server.port", portForHttps);
//			System.setProperty("server.ssl.key-store", AcmeClient.KEYSTORE_FILE.getAbsolutePath());
//			System.setProperty("server.ssl.key-store-password", KEYSTORE_PASSWD);
//			System.setProperty("server.ssl.key-password", KEY_PASSWD);
//			System.setProperty("server.ssl.key-alias", "k1");
//
//			// TODO: már nincs ilyen property, Spring 1.X-nél használtuk... viszont van más másik hasonló, tisztáni, hogy itt miért is kellett ez (lehet, hogy az újatt is át kell itt "dobni" ugyanígy...)
//			// if (StringUtils.isNotBlank(portForAfterLeSecondaryHttp)) {
//			// System.setProperty("server.additional-http-ports", portForAfterLeSecondaryHttp);
//			// }
//
//			return true;
//
//		} catch (final Exception ex) {
//			throw new LetsEncryptException("Failed to get a certificate for domain", ex);
//		}
//
//	}
//
//	/**
//	 * veszi a mai dátumot és a lejárati dátumot, kiszámítja a visszamaradó napok számát...
//	 * 
//	 * @return amennyiben a fájl egyáltalán nem létezik (még), akkor is true-t ad vissza
//	 */
//	public static synchronized boolean isRenewRequired() {
//
//		try {
//
//			final String domainForLe = System.getProperty("domainForLe");
//			if (StringUtils.isBlank(domainForLe)) {
//				log.info("Property \"domainForLe\" is missing or empty!");
//				return false;
//			}
//
//			if (!AcmeClient.DOMAIN_CHAIN_FILE.exists()) {
//				log.info("The certification file does not exists yet!");
//				return true;
//			}
//
//			final CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
//
//			final X509Certificate oldCert;
//
//			try (final InputStream inputStream = new BufferedInputStream(new FileInputStream(AcmeClient.DOMAIN_CHAIN_FILE), 32 * 1024)) {
//				oldCert = (X509Certificate) certFactory.generateCertificate(inputStream);
//			}
//
//			final Date expirationDate = oldCert.getNotAfter();
//			final long expirationDateMillis = expirationDate.getTime();
//
//			long millisLeft = (expirationDateMillis - System.currentTimeMillis());
//			String millisLeftStr = String.format("%.3f", (millisLeft / (float) (1000L * 60L * 60L * 24L)));
//
//			boolean isRenewRequired = millisLeft < AcmeClient.MILLIS_LEFT_BEFORE_RENEW;
//
//			if (isRenewRequired) {
//				log.info("There's only " + millisLeftStr + " days remaining from the certification (renew required).");
//			} else {
//				log.info("There's still " + millisLeftStr + " days remaining from the certification (no renew required).");
//			}
//
//			return isRenewRequired;
//
//		} catch (Exception e) {
//			throw new LetsEncryptException("Remaining day count check error!", e);
//		}
//
//	}
//
//}
