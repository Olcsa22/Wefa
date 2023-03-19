package hu.lanoga.toolbox.config;

import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.AbstractRequestLoggingFilter;

import com.teamunify.i18n.I;

import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryAuditManager;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryTenantManager;
import hu.lanoga.toolbox.session.LcuHelperSessionBean;

/**
 * controller request-ek vizsgálata, logolása (paraméterek, belépett user stb.), ThreadLocal clear...
 */
@Configuration
public class RequestFilterConfig {

	// login/logout hívás nem kerül benne
	// security megfontolások, mi kerül a logba, jelszavak? (mivel a login nincs benne, ezért nem biztos, hogy ez felmerül)

	private static ThreadLocal<String> requestLogMsg = new ThreadLocal<>();
	private static ThreadLocal<Locale> requestLocale = new ThreadLocal<>();

	public static String getRequestLogMsg() {
		return requestLogMsg.get();
	}

	public static Locale getRequestLocale() {
		return requestLocale.get();
	}

	public static class CustomLoggingFilter extends AbstractRequestLoggingFilter {

		// Apache Commons Logging-gal megy, de a Logback kezeli, "elfogja" ezeket a log üzeneteket is, a Logback config vonatkozik rá stb.

		@Override
		protected boolean shouldLog(final HttpServletRequest request) {
			return this.logger.isErrorEnabled();
		}

		@Override
		protected void beforeRequest(final HttpServletRequest request, final String message) {

			Locale localeFromBrowserRequeset = null;

			{

				final Enumeration<Locale> locales = request.getLocales(); // ez a többes számos getLocales() a jobb elvileg
				while (locales.hasMoreElements()) {
					localeFromBrowserRequeset = locales.nextElement();
					break;
				}

				// if (localeFromBrowserRequeset == null) {
				// localeFromBrowserRequeset = request.getLocale(); // az a régebbi/egyszerűbb megoldás, de nem tűnik megbízhatónak... inkább építünk további (saját, server szintű fallback-re)
				// }

			}

			requestLocale.set(localeFromBrowserRequeset);
			requestLogMsg.set(message);

			// ---

			JdbcRepositoryManager.clearTlTenantId();
			I.clearLocale();
		}

		@Override
		protected void afterRequest(final HttpServletRequest request, final String message) {

			requestLocale.remove();
			requestLogMsg.remove();

			// ---

			LcuHelperSessionBean.clearTlLcuHelperBeanReference();
			JdbcRepositoryAuditManager.clearTlAuditModel();
			JdbcRepositoryTenantManager.clearTlTenantId();
			
			I.clearLocale();

		}

	}

	@Bean
	public AbstractRequestLoggingFilter logFilter() {

		final CustomLoggingFilter filter = new CustomLoggingFilter();

		filter.setIncludeQueryString(true);
		filter.setIncludeHeaders(true);
		filter.setIncludeClientInfo(true);
		filter.setIncludePayload(true);
		filter.setMaxPayloadLength(500);

		return filter;

	}
}
