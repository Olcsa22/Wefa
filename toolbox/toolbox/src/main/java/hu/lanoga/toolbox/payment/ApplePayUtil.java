package hu.lanoga.toolbox.payment;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.google.common.collect.ImmutableSet;

import hu.lanoga.toolbox.payment.exception.ToolboxPaymentGeneralException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApplePayUtil {

	/**
	 * https://developer.apple.com/documentation/apple_pay_on_the_web/setting_up_your_server 
	 * (A kínai változatok nincsenek itt!
	 */
	private static final ImmutableSet<String> validAppleUrlSet = ImmutableSet.of(
			"apple-pay-gateway.apple.com",
			"apple-pay-gateway-nc-pod1.apple.com",
			"apple-pay-gateway-nc-pod2.apple.com",
			"apple-pay-gateway-nc-pod3.apple.com",
			"apple-pay-gateway-nc-pod4.apple.com",
			"apple-pay-gateway-nc-pod5.apple.com",
			"apple-pay-gateway-pr-pod1.apple.com",
			"apple-pay-gateway-pr-pod2.apple.com",
			"apple-pay-gateway-pr-pod3.apple.com",
			"apple-pay-gateway-pr-pod4.apple.com",
			"apple-pay-gateway-pr-pod5.apple.com",
			"apple-pay-gateway-nc-pod1-dr.apple.com",
			"apple-pay-gateway-nc-pod2-dr.apple.com",
			"apple-pay-gateway-nc-pod3-dr.apple.com",
			"apple-pay-gateway-nc-pod4-dr.apple.com",
			"apple-pay-gateway-nc-pod5-dr.apple.com",
			"apple-pay-gateway-pr-pod1-dr.apple.com",
			"apple-pay-gateway-pr-pod2-dr.apple.com",
			"apple-pay-gateway-pr-pod3-dr.apple.com",
			"apple-pay-gateway-pr-pod4-dr.apple.com",
			"apple-pay-gateway-pr-pod5-dr.apple.com",
			"apple-pay-gateway-cert.apple.com"); // last one is the sandbox

	@SuppressWarnings("resource")
	private static SSLContext buildSessionCreateSslContext() {

		try {

			final String keyStorePassword = "ab";
			final String keyPassword = "ab";

			// apple-merch-identity.jks = Apple Pay Merchant Identity Certificate

			final Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath:apple_pay/2/apple-merch-identity.jks");

			// ---

			final KeyStore clientStore = KeyStore.getInstance("JKS"); // vagy PKCS12
			clientStore.load(resources[0].getInputStream(), keyStorePassword.toCharArray());

			final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(clientStore, keyPassword.toCharArray());
			final KeyManager[] kms = kmf.getKeyManagers();

			SSLContext sslContext = null;
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kms, null, new SecureRandom()); // TODO: itt második param nem kell?

			return sslContext;

		} catch (final Exception e) {
			throw new ToolboxPaymentGeneralException("KeyStore load error!", e);
		}
	}

	/**
	 * @param appleValidationUrlFromJs
	 * @param forceSandbox
	 * @return
	 */
	public static String createSession(final String appleValidationUrlFromJs, boolean forceSandbox) {

		try {

			{

				// Ensure that your server accesses only the validation URLs provided by Apple in Setting Up Your Server, and fails for other URLs.
				// Using a strict whitelist for the validation URLs is recommended.
				// The payment session request must be sent from your server;
				// never request the session from the client.

				String s = StringUtils.removeStart(appleValidationUrlFromJs, "https://");
				s = StringUtils.removeEnd(s, "/");
				s = StringUtils.removeEnd(s, "/paymentservices/startSession");

				if (!validAppleUrlSet.contains(s)) {
					throw new ToolboxPaymentGeneralException("Invalid Apple Pay session init URL!"); // az appleValidationUrlFromJs azért nem logoljuk, mert unsafe lehet (már a logolás is)
				}

			}

			String validationURL = appleValidationUrlFromJs;

			if (forceSandbox) {
				validationURL = "https://apple-pay-gateway-cert.apple.com/paymentservices/startSession";
			}

			log.debug("createSession, validationURL: " + validationURL);

			final URL url = new URL(validationURL);

			final HttpsURLConnection urlConn = (HttpsURLConnection) url.openConnection();
			urlConn.setSSLSocketFactory(buildSessionCreateSslContext().getSocketFactory()); // TODO: lehet, hogy lehet tárolni az SSLContext objektumot (thread-safe?)... viszont a HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory()); meg neccess, az már túl globális...
			urlConn.setDoOutput(true);
			urlConn.setDoInput(true);
			urlConn.setRequestProperty("Content-Type", "application/json");
			urlConn.setRequestProperty("Accept", "application/json");
			urlConn.setRequestMethod("POST");

			final JSONObject requestBodyJsonObject = new JSONObject();
			// TODO: https://developer.apple.com/documentation/apple_pay_on_the_web/apple_pay_js_api/requesting_an_apple_pay_payment_session
			requestBodyJsonObject.put("merchantIdentifier", "merchant.hu.lanoga");
			requestBodyJsonObject.put("initiativeContext", "lngtest.eu.ngrok.io");
			requestBodyJsonObject.put("domainName", "lngtest.eu.ngrok.io"); // TODO: ez elvileg nem kell, e helyett van a initiativeContext nevű (eggyfel fentebb)
			requestBodyJsonObject.put("initiative", "web");
			requestBodyJsonObject.put("displayName", "Test Merchant 2020 Jan");

			final String requestBodyStr = requestBodyJsonObject.toString();

			try (OutputStream os = urlConn.getOutputStream()) {
				IOUtils.write(requestBodyStr, os, "UTF-8");
			}

			final int responseCode = urlConn.getResponseCode();

			String reponseBodyStr;

			try (InputStream is = (responseCode == 200 ? urlConn.getInputStream() : urlConn.getErrorStream())) {
				reponseBodyStr = IOUtils.toString(is, "UTF-8");
			}

			return reponseBodyStr;

		} catch (final Exception e) {
			throw new ToolboxPaymentGeneralException("Apple Pay generateSession / merchant validation failed!", e);
		}
	}

}
