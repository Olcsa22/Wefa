package hu.lanoga.toolbox.controller; // TODO: áttenni hu.lanoga.util package-be (vigyázva)

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import lombok.extern.slf4j.Slf4j;

/**
 * request, response adatok, IP cím, header-ek stb.
 */
@Slf4j
public final class ToolboxHttpUtils {

	private ToolboxHttpUtils() {
		//
	}

	/**
	 * (minden még lehet, hogy ebben sincs benne! hash utáni rész nincs...)
	 * 
	 * @param request
	 * @return
	 */
	public static String getFullURL(HttpServletRequest request) {

		final StringBuffer requestURL = request.getRequestURL();
		final String queryString = request.getQueryString();

		final String result = queryString == null ? requestURL.toString()
				: requestURL.append('?')
						.append(queryString)
						.toString();

		return result;
	}

	/**
	 * @return
	 * @see RequestContextHolder
	 */
	public static HttpServletRequest getCurrentHttpServletRequest() {

		try {
			RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
			HttpServletRequest servletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
			return servletRequest;
		} catch (Exception e) {
			log.debug("Missing HttpServletRequest (migh be normal)");
		}

		return null;

	}

	/**
	 * A hívó (kliens IP-je)
	 * 
	 * @param request
	 * @return
	 */
	public static String determineIpAddress(final HttpServletRequest request) {

		String ip = request.getHeader("x-forwarded-for");

		if ((ip == null) || (ip.length() == 0) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}

		if ((ip == null) || (ip.length() == 0) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}

		if ((ip == null) || (ip.length() == 0) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}

		return ip;
	}

	public static String getHeadersAsString(final HttpServletRequest request) {

		final StringBuilder sb = new StringBuilder("\nHeaders:");
		final Enumeration<String> headerNames = request.getHeaderNames();

		while (headerNames.hasMoreElements()) {

			final String headerName = headerNames.nextElement();

			sb.append("\nHeader Name: ");
			sb.append(headerName);
			sb.append("\nHeader Value: ");
			sb.append(request.getHeader(headerName));

		}
		
		return StringUtils.abbreviate(sb.toString(), 10000);
	}

	public static void logHeaders(final HttpServletRequest request) {
		log.debug(getHeadersAsString(request));
	}

	/**
	 * http://stackoverflow.com/a/18030465/1845894
	 * 
	 * @param request
	 * @return
	 */
	public static String getClientOS(HttpServletRequest request) {

		final String browserDetails = request.getHeader("User-Agent");

		if (browserDetails == null) {
			return "?";
		}

		final String lowerCaseBrowser = browserDetails.toLowerCase();

		if (lowerCaseBrowser.contains("windows")) {
			return "Windows";
		} else if (lowerCaseBrowser.contains("mac")) {
			return "Mac";
		} else if (lowerCaseBrowser.contains("x11")) {
			return "Unix";
		} else if (lowerCaseBrowser.contains("android")) {
			return "Android";
		} else if (lowerCaseBrowser.contains("iphone")) {
			return "iOS";
		} else {
			return "?";
		}
	}

	/**
	 * http://stackoverflow.com/a/18030465/1845894
	 * 
	 * @param request
	 * @return
	 */
	public static String getClientBrowser(HttpServletRequest request) {

		final String browserDetails = request.getHeader("User-Agent");
		
		if (browserDetails == null) {
			return "?";
		}
		
		final String lowerCaseBrowser = browserDetails.toLowerCase();

		String browser = "?";

		try {

			if (lowerCaseBrowser.contains("msie")) {
				String substring = browserDetails.substring(browserDetails.indexOf("MSIE")).split(";")[0];
				browser = substring.split(" ")[0].replace("MSIE", "IE") + "-" + substring.split(" ")[1];
			} else if (lowerCaseBrowser.contains("safari") && lowerCaseBrowser.contains("version")) {
				browser = (browserDetails.substring(browserDetails.indexOf("Safari")).split(" ")[0]).split(
						"/")[0] + "-"
						+ (browserDetails.substring(
								browserDetails.indexOf("Version")).split(" ")[0]).split("/")[1];
			} else if (lowerCaseBrowser.contains("opr") || lowerCaseBrowser.contains("opera")) {
				if (lowerCaseBrowser.contains("opera"))
					browser = (browserDetails.substring(browserDetails.indexOf("Opera")).split(" ")[0]).split(
							"/")[0] + "-"
							+ (browserDetails.substring(
									browserDetails.indexOf("Version")).split(" ")[0]).split("/")[1];
				else if (lowerCaseBrowser.contains("opr"))
					browser = ((browserDetails.substring(browserDetails.indexOf("OPR")).split(" ")[0]).replace("/",
							"-")).replace(
									"OPR", "Opera");
			} else if (lowerCaseBrowser.contains("chrome")) {
				browser = (browserDetails.substring(browserDetails.indexOf("Chrome")).split(" ")[0]).replace("/", "-");
			} else if ((lowerCaseBrowser.indexOf("mozilla/7.0") > -1) || (lowerCaseBrowser.indexOf("netscape6") != -1) || (lowerCaseBrowser.indexOf(
					"mozilla/4.7") != -1) || (lowerCaseBrowser.indexOf("mozilla/4.78") != -1)
					|| (lowerCaseBrowser.indexOf(
							"mozilla/4.08") != -1)
					|| (lowerCaseBrowser.indexOf("mozilla/3") != -1)) {

				// browser=(userAgent.substring(userAgent.indexOf("MSIE")).split(" ")[0]).replace("/", "-");

				browser = "Netscape-?";

			} else if (lowerCaseBrowser.contains("firefox")) {
				browser = (browserDetails.substring(browserDetails.indexOf("Firefox")).split(" ")[0]).replace("/", "-");
			} else if (lowerCaseBrowser.contains("rv")) {
				browser = "IE";
			}

		} catch (Exception e) {
			log.error("getClientBrowser failed", e);
		}

		return browser;
	}

	public static String getUserAgent(HttpServletRequest request) {
		final String browserDetails = request.getHeader("User-Agent");
		if (browserDetails == null) {
			return "?";
		}
		return browserDetails;
	}

}
