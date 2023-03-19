package hu.lanoga.toolbox.config;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.apache.tomcat.util.http.SameSiteCookies;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * csak embedded Tomcat esetén 
 * (standalone Tomcat, embedded Jetty stb. esetén nem működik, 
 * illetve ki is kell kapcsolni)
 *
 */
@Slf4j
@ConditionalOnProperty("tools.tomcat.two-port.enabled")
@Configuration
public class TomcatTwoPortConfig {

	@Value("${tools.tomcat.two-port.http.port}")
	private int httpPort;

	@Value("${tools.tomcat.two-port.https.redirect.port}")
	private int httpsRedirectPort;

	@Value("${tools.tomcat.two-port.https.redirect.force-pattern}")
	private String httpsForcePattern;

	@Value("${tools.tomcat.two-port.https.redirect.re-allow-http-pattern}")
	private String reAllowHttpPattern;
	
	@Value("${tools.security.same-site-cookie-policy}")
	private String sameSiteCookiePolicy;
	@Value("${tools.security.same-site-cookie-policy-url-path}")
	private String sameSiteCookiePolicyUrlPath;

	@Bean
	public ServletWebServerFactory servletContainer() {
		
		final TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {

			@Override
			protected void postProcessContext(final Context context) {

				// több ilyen SecurityConstraint elemet is hozzá lehet adni
				// egy Java list-ben vagy Array-ben (lényeg, hogy sorrend értelmezett) tárolja őket a Tomcat (jelen állás szerint, Tomcat 9.0.30)
				// ez (úgy néz ki) lehetővé teszi, hogy egy ilyen force HTTPS, de egy "másik pattern-re mégsem force" config felállást ki tudjunk alakítani

				// maguk a pattern String-ek nem tudni, hogy milyen wildcard stb. megoldásokat tudnak
				// szerintem semmit, simán .startsWith String check van (tehát a "/valami/minta" pattern csillag nélkül is ráillik a "/valami/minta/xy/z" URL-re is)
				
				if (StringUtils.isNotBlank(TomcatTwoPortConfig.this.httpsForcePattern)) {

					{
						final SecurityConstraint securityConstraint = new SecurityConstraint();
						securityConstraint.setUserConstraint("CONFIDENTIAL");
						final SecurityCollection collection = new SecurityCollection();

						// ennél patternnél force-olva lesz a HTTPS    
						
						// ez a kód a "HSTS" néven ismert header-t adja hozzá a response-okhoz
						// lásd: https://en.wikipedia.org/wiki/HTTP_Strict_Transport_Security
						// lásd: https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Upgrade-Insecure-Requests
						
						// hiányossága, hogy nem default (ha a HTTPS port nem 443) nem működik rendesen... 
						// lásd: https://bugzilla.mozilla.org/show_bug.cgi?id=613645
						
						// ez a böngésző miatt van, a második körben ezt a header-t (Upgrade-Insecure-Requests) megjegyzi és már nem is a szerver révén irányít át
						// a lentebb állított connector.setRedirectPort() (8443 stb.) nem érvényesül (a legelső "kör"-nél még igen, de később nem)
						
						// (tehát a második és későbbi esetén, pl. ez történik: http://valami:8080 -> https://valami:8080)
						// (legalőször még jó, akkor http://valami:8080 -> https://valami:8443)
						
						// (sztenderd 443/80 "páros" esetén nincs gond)
						
						// TODO: megpróbálni inkább valamilyen WebFilter-rel vagy Spring interceptor-ral? (az viszont kevésbé secure, mint a HSTS)
						
						if (TomcatTwoPortConfig.this.httpsForcePattern.contains(",")) {

							final String[] split = TomcatTwoPortConfig.this.httpsForcePattern.split(",");

							for (final String s : split) {
								collection.addPattern(s);
							}

						} else {
							collection.addPattern(TomcatTwoPortConfig.this.httpsForcePattern);
						}

						securityConstraint.addCollection(collection);
						context.addConstraint(securityConstraint);
						
					}

					if (StringUtils.isNotBlank(TomcatTwoPortConfig.this.reAllowHttpPattern)) {

						final SecurityConstraint securityConstraint = new SecurityConstraint();
						securityConstraint.setUserConstraint("NONE");
						final SecurityCollection collection = new SecurityCollection();

						// ennél patternnél mégis (kivételként) vissza lesz engedve a HTTP is
						// engedi a HTTP-t, de nem erőlteti vissza (tehát ezeken az URL-eken megy a HTTP és a HTTPS is)

						// mj.: a Spring Security-nek is van egy hasonló megoldása, de ott mindenképp erőlteti az egyiket (a próbáim alapján)

						if (TomcatTwoPortConfig.this.reAllowHttpPattern.contains(",")) {
							final String[] split = TomcatTwoPortConfig.this.reAllowHttpPattern.split(",");

							for (final String s : split) {
								collection.addPattern(s);
							}
						} else {
							collection.addPattern(TomcatTwoPortConfig.this.reAllowHttpPattern);
						}

						securityConstraint.addCollection(collection);
						context.addConstraint(securityConstraint);
					}

				}
				
				// ---
				
				if (StringUtils.isNotBlank(sameSiteCookiePolicy)) {
					
					if (StringUtils.isNotBlank(sameSiteCookiePolicyUrlPath)) {

				        final Rfc6265CookieProcessor cookieProcessor = new Rfc6265CookieProcessor() {
				        	
				        	public String generateHeader(javax.servlet.http.Cookie cookie, HttpServletRequest request) {
				        		
				        		String header = super.generateHeader(cookie, request);
				        		
				        		if (request != null && request.getRequestURI().startsWith(sameSiteCookiePolicyUrlPath)) {
				        			header += "; SameSite="; 
				        			header += SameSiteCookies.fromString(sameSiteCookiePolicy).getValue();
				        		} 
				        		
								return header;
				        		
				        	};
				        };
				        cookieProcessor.setSameSiteCookies("Unset");
				        context.setCookieProcessor(cookieProcessor);
				        
				        log.info("Customized Rfc6265CookieProcessor, sameSiteCookiePolicy: " + sameSiteCookiePolicy + ", sameSiteCookiePolicyUrlPath: " + sameSiteCookiePolicyUrlPath);
						
					} else {
				        final Rfc6265CookieProcessor cookieProcessor = new Rfc6265CookieProcessor();
				        cookieProcessor.setSameSiteCookies(sameSiteCookiePolicy);
				        context.setCookieProcessor(cookieProcessor);
				        
				        log.info("Rfc6265CookieProcessor, sameSiteCookiePolicy: " + sameSiteCookiePolicy);
					}

				}
				
			}

			@Override
			protected void prepareContext(final Host host, final ServletContextInitializer[] initializers) {

				super.prepareContext(host, initializers);

				// root (üres) context átirányítása a mi context-ünkre (ha az nem root)
				// tehát pl.: http://localhost/valamiApp ahol bejön a web app, akko a http://localhost -> http://localhost/valamiApp átiránytás

				final String contextPath = this.getContextPath();

				if (StringUtils.isNotBlank(contextPath)) {
					final StandardContext child = new StandardContext();
					child.addLifecycleListener(new Tomcat.FixContextListener());
					child.setPath("");
					final ServletContainerInitializer initializer = TomcatTwoPortConfig.this.getServletContextInitializer(contextPath);
					child.addServletContainerInitializer(initializer, Collections.emptySet());
					child.setCrossContext(true);
					host.addChild(child);
				}

			}

		};
		tomcat.addAdditionalTomcatConnectors(this.buildAdditionalTomcatConnector());
		return tomcat;
	}

	private ServletContainerInitializer getServletContextInitializer(final String contextPath) {

		final String contextPathNormalized;

		if (!contextPath.endsWith("/")) { // ezzel egy extra 302 redirect-et meg tudunk spórolni
			contextPathNormalized = contextPath + "/";
		} else {
			contextPathNormalized = contextPath;
		}

		// ---

		return (c, context) -> {
			final Servlet servlet = new HttpServlet() {

				@Override
				protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
					resp.sendRedirect(contextPathNormalized);
				}
			};
			context.addServlet("root", servlet).addMapping("/*");
		};
	}

	private Connector buildAdditionalTomcatConnector() {

		final Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		connector.setScheme("http");
		connector.setPort(this.httpPort);
		connector.setSecure(false);

		if (this.httpsRedirectPort > 0) {
			connector.setRedirectPort(this.httpsRedirectPort);
		}

		return connector;
	}
}
