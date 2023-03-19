///*
// * acme4j - Java ACME client
// *
// * Copyright (C) 2015 Richard "Shred" Körber
// *   http://acme4j.shredzone.org
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
// */
//package hu.lanoga.toolbox.cert;
//
//import java.io.File;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.Writer;
//import java.net.URI;
//import java.net.URL;
//import java.security.KeyPair;
//import java.security.cert.X509Certificate;
//import java.util.Collection;
//
//import org.apache.commons.lang3.StringUtils;
//import org.shredzone.acme4j.Authorization;
//import org.shredzone.acme4j.Certificate;
//import org.shredzone.acme4j.Registration;
//import org.shredzone.acme4j.RegistrationBuilder;
//import org.shredzone.acme4j.Session;
//import org.shredzone.acme4j.Status;
//import org.shredzone.acme4j.challenge.Challenge;
//import org.shredzone.acme4j.challenge.Http01Challenge;
//import org.shredzone.acme4j.exception.AcmeConflictException;
//import org.shredzone.acme4j.exception.AcmeException;
//import org.shredzone.acme4j.util.CSRBuilder;
//import org.shredzone.acme4j.util.CertificateUtils;
//import org.shredzone.acme4j.util.KeyPairUtils;
//
//import com.google.common.io.Files;
//
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//class AcmeClient {
//
//	private AcmeClient() {
//		//
//	}
//
//	// File name of the User Key Pair
//	static final File USER_KEY_FILE;
//
//	// File name of the Domain Key Pair
//	static final File DOMAIN_KEY_FILE;
//
//	// File name of the CSR
//	static final File DOMAIN_CSR_FILE;
//
//	// File name of the signed certificate
//	static final File DOMAIN_CHAIN_FILE;
//
//	static final File KEYSTORE_FILE;
//	
//	/**
//	 * ha ennél kevesebb hátralévő érvényességi ideje van a tanúsítványnak, akkor újat kérünk már...
//	 */
//	static final long MILLIS_LEFT_BEFORE_RENEW;
//
//	// RSA key size of generated key pairs
//	static final int KEY_SIZE = 2048;
//
//	static {
//		final String certDir = StringUtils.defaultIfBlank(System.getProperty("certDir"), System.getProperty("user.home") + "/.tools/cert");
//
//		USER_KEY_FILE = new File(certDir + "/user.key");
//		DOMAIN_KEY_FILE = new File(certDir + "/domain.key");
//		DOMAIN_CSR_FILE = new File(certDir + "/domain.csr");
//		DOMAIN_CHAIN_FILE = new File(certDir + "/domain-chain.crt");
//		KEYSTORE_FILE = new File(certDir + "/ks.jks");
//		
//		String leMillisLeftBeforeRenewStr = System.getProperty("leMillisLeftBeforeRenew");
//		
//		// default: 90 nap - 10 nap (értsd: a letsencrypt 90 nap-ra ad tanúsítványt, ha már kevesebb mint 10 nap van vissza, akkor újat kérünk; következésképp max 10 naponta ellenőrzni kell, legtisztább hetente egy fix munkaidőn kövüli időpontban)
//		// teszteléshez jó lehet: 7775820000 (90 nap - 3 perc)
//		MILLIS_LEFT_BEFORE_RENEW = StringUtils.isNotBlank(leMillisLeftBeforeRenewStr) ? Long.parseLong(leMillisLeftBeforeRenewStr) : 1000L * 60L * 60L * 24L * 90L - 1000L * 60L * 60L * 24L * 10L; 
//	}
//
//	static Http01Challenge lastChallange;
//
//	/**
//	 * Presents the user a link to the Terms of Service, and asks for confirmation. If the user denies confirmation, an exception is thrown.
//	 *
//	 * @param reg
//	 *            {@link Registration} User's registration
//	 * @param agreement
//	 *            {@link URI} of the Terms of Service
//	 */
//	private static void acceptAgreement(final Registration reg, final URI agreement) throws AcmeException {
//		// Modify the Registration and accept the agreement
//		reg.modify().setAgreement(agreement).commit();
//		log.info("Updated user's ToS");
//	}
//
//	/**
//	 * Generates a certificate for the given domains. Also takes care for the registration process.
//	 *
//	 * @param domains
//	 *            Domains to get a common certificate for
//	 *
//	 */
//	static void fetchCertificate(final Collection<String> domains, final boolean isStaging) throws IOException, AcmeException {
//
//		// Load the user key file. If there is no key file, create a new one.
//		// Keep this key pair in a safe place! In a production environment, you will not be
//		// able to access your account again if you should lose the key pair.
//
//		final KeyPair userKeyPair = loadOrCreateKeyPair(USER_KEY_FILE);
//
//		// Create a session for Let's Encrypt.
//		// Use "acme://letsencrypt.org" for production server
//
//		final Session session = new Session(isStaging ? "acme://letsencrypt.org/staging" : "acme://letsencrypt.org", userKeyPair);
//
//		// Get the Registration to the account.
//		// If there is no account yet, create a new one.
//		final Registration reg = findOrRegisterAccount(session);
//
//		// Separately authorize every requested domain.
//		for (final String domain : domains) {
//			authorize(reg, domain);
//		}
//
//		// Load or create a key pair for the domains. This should not be the userKeyPair!
//		final KeyPair domainKeyPair = loadOrCreateKeyPair(DOMAIN_KEY_FILE);
//
//		// Generate a CSR for all of the domains, and sign it with the domain key pair.
//		final CSRBuilder csrb = new CSRBuilder();
//		csrb.addDomains(domains);
//		csrb.sign(domainKeyPair);
//
//		// Write the CSR to a file, for later use.
//		try (Writer out = new FileWriter(DOMAIN_CSR_FILE)) {
//			csrb.write(out);
//		}
//
//		// ------------------
//
//		if (LetsEncryptUtil.isRenewRequired()) {
//
//			// Now request a signed certificate if it doesn't exist.
//			final Certificate certificate = reg.requestCertificate(csrb.getEncoded());
//
//			log.info("Success! The certificate for domains " + domains + " has been generated!");
//			log.info("Certificate URL: " + certificate.getLocation());
//
//			// Download the leaf certificate and certificate chain.
//			final X509Certificate cert = certificate.download();
//			final X509Certificate[] chain = certificate.downloadChain();
//
//			// Write a combined file containing the certificate and chain.
//			try (FileWriter fw = new FileWriter(DOMAIN_CHAIN_FILE)) {
//				CertificateUtils.writeX509CertificateChain(fw, cert, chain);
//				log.info("Certification generated successfully");
//			}
//		}
//
//		// That's all! Configure your web server to use the DOMAIN_KEY_FILE and
//		// DOMAIN_CHAIN_FILE for the requested domans.
//	}
//
//	/**
//	 * Prepares a HTTP challenge.
//	 * The verification of this challenge expects a file with a certain content to be reachable at a given path under the domain to be tested.
//	 * This example outputs instructions that need to be executed manually. In a production environment, you would rather generate this file automatically, or maybe use a servlet that returns {@link Http01Challenge#getAuthorization()}.
//	 *
//	 * @param auth
//	 *            {@link Authorization} to find the challenge in
//	 * @return {@link Challenge} to verify
//	 */
//	private static Challenge httpChallenge(final Authorization auth) throws AcmeException {
//
//		// Find a single http-01 challenge
//
//		final Http01Challenge challenge = auth.findChallenge(Http01Challenge.TYPE);
//		if (challenge == null) {
//			throw new AcmeException("Found no " + Http01Challenge.TYPE + " challenge, don't know what to do...");
//		}
//
//		lastChallange = challenge; // nem a legszebb megoldás így static változót használni, de itt elmegy, mert ebből biztosan egy példány van csak...
//
//		return challenge;
//	}
//
//	/**
//	 * Authorize a domain. It will be associated with your account, so you will be able to retrieve a signed certificate for the domain later.
//	 * You need separate authorizations for subdomains (e.g. "www" subdomain). Wildcard certificates are not currently supported.
//	 *
//	 * @param reg
//	 *            {@link Registration} of your account
//	 * @param domain
//	 *            Name of the domain to authorize
//	 */
//	private static void authorize(final Registration reg, final String domain) throws AcmeException {
//
//		// Authorize the domain.
//		final Authorization auth = reg.authorizeDomain(domain);
//		log.info("Authorization for domain " + domain);
//
//		final Challenge challenge = httpChallenge(auth);
//
//		// If the challenge is already verified, there's no need to execute it again.
//		if (challenge.getStatus() == Status.VALID) {
//			return;
//		}
//
//		// Now trigger the challenge.
//		challenge.trigger();
//
//		// Poll for the challenge to complete.
//		try {
//			int attempts = 10;
//			while ((challenge.getStatus() != Status.VALID) && (attempts-- > 0)) {
//				// Did the authorization fail?
//				if (challenge.getStatus() == Status.INVALID) {
//					throw new AcmeException("Challenge failed... Giving up.");
//				}
//
//				// Wait for a few seconds
//				Thread.sleep(3000L);
//
//				// Then update the status
//				challenge.update();
//			}
//		} catch (final InterruptedException ex) {
//			log.error("interrupted", ex);
//			Thread.currentThread().interrupt();
//		}
//
//		// All reattempts are used up and there is still no valid authorization?
//		if (challenge.getStatus() != Status.VALID) {
//			throw new AcmeException("Failed to pass the challenge for domain " + domain + ", ... Giving up.");
//		}
//	}
//
//	/**
//	 * Finds your {@link Registration} at the ACME server. It will be found by your user's public key. If your key is not known to the server yet, a new registration will be created.
//	 * <p>
//	 * This is a simple way of finding your {@link Registration}. A better way is to get the URL of your new registration with {@link Registration#getLocation()} and store it somewhere. If you need to get access to your account later, reconnect to it via {@link Registration#bind(Session, URL)} by using the stored location.
//	 *
//	 * @param session
//	 *            {@link Session} to bind with
//	 * @return {@link Registration} connected to your account
//	 */
//	private static Registration findOrRegisterAccount(final Session session) throws AcmeException {
//		Registration reg;
//
//		try {
//			// Try to create a new Registration.
//			reg = new RegistrationBuilder().create(session);
//			log.info("Registered a new user, URL: " + reg.getLocation());
//
//			// This is a new account. Let the user accept the Terms of Service.
//			// We won't be able to authorize domains until the ToS is accepted.
//			final URI agreement = reg.getAgreement();
//			log.info("Terms of Service: " + agreement);
//			acceptAgreement(reg, agreement);
//
//		} catch (final AcmeConflictException ex) {
//			// The Key Pair is already registered. getLocation() contains the
//			// URL of the existing registration's location. Bind it to the session.
//			reg = Registration.bind(session, ex.getLocation());
//			log.info("Account does already exist, URL: " + reg.getLocation(), ex);
//		}
//
//		return reg;
//	}
//
//	/**
//	 * Loads a key pair from specified file. If the file does not exist, a new key pair is generated and saved.
//	 *
//	 * @return {@link KeyPair}.
//	 */
//	private static KeyPair loadOrCreateKeyPair(final File file) throws IOException {
//
//		if (file.exists()) {
//			try (FileReader fr = new FileReader(file)) {
//				return KeyPairUtils.readKeyPair(fr);
//			}
//		}
//
//		Files.createParentDirs(file);
//
//		final KeyPair domainKeyPair = KeyPairUtils.createKeyPair(KEY_SIZE);
//		try (FileWriter fw = new FileWriter(file)) {
//			KeyPairUtils.writeKeyPair(domainKeyPair, fw);
//		}
//
//		return domainKeyPair;
//	}
//
//}