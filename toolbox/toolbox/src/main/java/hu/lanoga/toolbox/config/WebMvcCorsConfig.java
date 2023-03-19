package hu.lanoga.toolbox.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import lombok.extern.slf4j.Slf4j;

/**
 * CorsConfigurationSource is játszik... lásd {@link SecurityConfig} oszály...
 * ez az itteni config vonatkozik a static fájlokra is (értsd a {@link WebSecurity} hatálya alá tartozókra), 
 * a {@link CorsConfigurationSource} nem (az csak azokra, amik már a Spring Security hatáskörébe tartoznak, tehát a {@link HttpSecurity} hatálya alá)
 */
@Slf4j
@Configuration
public class WebMvcCorsConfig extends WebMvcConfigurerAdapter {

	@Value("${tools.security.cors-config.allowed-origins}")
	private List<String> corsConfigAllowedOrigins;

	@Value("${tools.security.cors-config.allow-credentials}")
	private boolean corsConfigAllowCredentials;

	@Override
	public void addCorsMappings(final CorsRegistry registry) {

		if (this.corsConfigAllowedOrigins != null && !this.corsConfigAllowedOrigins.isEmpty()) {

			log.debug("SEC WebMvcCorsConfig addCorsMappings: " + this.corsConfigAllowedOrigins);

			final String[] corsConfigAllowCredentialsArray = this.corsConfigAllowedOrigins.toArray(new String[this.corsConfigAllowedOrigins.size()]);

			registry.addMapping("/**")
					.allowedOrigins(corsConfigAllowCredentialsArray)
					.allowCredentials(this.corsConfigAllowCredentials)
					.allowedMethods("GET", "POST", "PUT", "DELETE", "CONNECT", "HEAD", "OPTIONS", "PATCH")
					.maxAge(3600L);
		}

	}
}