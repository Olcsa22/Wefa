package hu.lanoga.toolbox.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.vaadin.server.Page;
import com.vaadin.server.WebBrowser;

import hu.lanoga.toolbox.controller.ToolboxHttpUtils;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpecialLogUtil {

	private SpecialLogUtil() {
		//
	}

	/**
	 * @param request
	 * @param levelInfo
	 * 		false esetén debug szint lesz
	 */
	public static void logL1(final HttpServletRequest request, final String description, final boolean levelInfo) {

		try {

			ToolboxAssert.notNull(description);

			final StringBuilder sb = new StringBuilder("logL1, ");
			sb.append(description);
			sb.append("\n");
			sb.append("\n");
			
			{

				final HttpSession session = request.getSession(false);

				if (session != null) {

					sb.append("request.getSession().getId(): ");
					sb.append(request.getSession().getId());
					sb.append("\n");

					sb.append("request.getSession().getCreationTime(): ");
					sb.append(request.getSession().getCreationTime());
					sb.append("\n");

					sb.append("request.getSession().getLastAccessedTime(): ");
					sb.append(request.getSession().getLastAccessedTime());
					sb.append("\n");

					sb.append("request.getSession().getMaxInactiveInterval(): ");
					sb.append(request.getSession().getMaxInactiveInterval());
					sb.append("\n");

				} else {
					sb.append("request.getSession(): ");
					sb.append("no session (null)");
					sb.append("\n");
				}

			}

			ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();

			if (loggedInUser != null) {

				sb.append("loggedInUser.getId(): ");
				sb.append(loggedInUser.getId());
				sb.append("\n");

				sb.append("loggedInUser.getUsername(): ");
				sb.append(loggedInUser.getUsername());
				sb.append("\n");

				sb.append("loggedInUser.getTenantId(): ");
				sb.append(loggedInUser.getTenantId());
				sb.append("\n");

			}

			sb.append("ToolboxHttpUtils.getFullURL(): ");
			sb.append(ToolboxHttpUtils.getFullURL(request));
			sb.append("\n");
			
			// kiszedtem, mert sokra nem jó, de kérdéses GDPR szempontból
			// sb.append("ToolboxHttpUtils.determineIpAddress(): ");
			// sb.append(ToolboxHttpUtils.determineIpAddress(request));
			// sb.append("\n");

			sb.append("ToolboxHttpUtils.getUserAgent(): ");
			sb.append(ToolboxHttpUtils.getUserAgent(request));
			sb.append("\n");

			sb.append("ToolboxHttpUtils.getClientBrowser(): ");
			sb.append(ToolboxHttpUtils.getClientBrowser(request));
			sb.append("\n");

			sb.append("ToolboxHttpUtils.getClientOS(): ");
			sb.append(ToolboxHttpUtils.getClientOS(request));
			sb.append("\n");

			// Vaadin

			try {

				final Page page = Page.getCurrent();
				if (page != null) {

					final WebBrowser webBrowser = page.getWebBrowser();

					sb.append("(vaadin) getAddress(): ");
					sb.append(webBrowser.getAddress());
					sb.append("\n");
					sb.append("(vaadin) getBrowserApplication(): ");
					sb.append(webBrowser.getBrowserApplication());
					sb.append("\n");
					sb.append("(vaadin) getBrowserMajorVersion(): ");
					sb.append(webBrowser.getBrowserMajorVersion());
					sb.append("\n");
					sb.append("(vaadin) getBrowserMinorVersion(): ");
					sb.append(webBrowser.getBrowserMinorVersion());
					sb.append("\n");
					sb.append("(vaadin) getBrowserVersion(): ");
					sb.append(webBrowser.getBrowserVersion());
					sb.append("\n");
					sb.append("(vaadin) getDSTSavings(): ");
					sb.append(webBrowser.getDSTSavings());
					sb.append("\n");
					sb.append("(vaadin) getRawTimezoneOffset(): ");
					sb.append(webBrowser.getRawTimezoneOffset());
					sb.append("\n");
					sb.append("(vaadin) getScreenHeight(): ");
					sb.append(webBrowser.getScreenHeight());
					sb.append("\n");
					sb.append("(vaadin) getScreenWidth(): ");
					sb.append(webBrowser.getScreenWidth());
					sb.append("\n");
					sb.append("(vaadin) getTimeZoneId(): ");
					sb.append(webBrowser.getTimeZoneId());
					sb.append("\n");
					sb.append("(vaadin) getTimezoneOffset(): ");
					sb.append(webBrowser.getTimezoneOffset());
					sb.append("\n");
					sb.append("(vaadin) getCurrentDate(): ");
					sb.append(webBrowser.getCurrentDate());
					sb.append("\n");
					sb.append("(vaadin) getLocale(): ");
					sb.append(webBrowser.getLocale());
					sb.append("\n");

				}

			} catch (final Exception e) {
				log.error("SpecialLogUtil L1 failed (Vaadin parts)", e);
			}

			sb.append("ToolboxHttpUtils.getHeadersAsString(): ");
			sb.append(ToolboxHttpUtils.getHeadersAsString(request));
			sb.append("\n");

			if (levelInfo) {
				log.info(sb.toString());
			} else {
				log.debug(sb.toString());
			}

		} catch (final Exception e) {
			log.error("SpecialLogUtil L1 failed", e);
		}

	}

}
