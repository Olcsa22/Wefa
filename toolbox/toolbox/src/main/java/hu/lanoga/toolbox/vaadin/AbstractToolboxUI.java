package hu.lanoga.toolbox.vaadin;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.vaadin.leif.headertags.Meta;
import org.vaadin.leif.headertags.MetaTags;

import com.google.common.util.concurrent.Futures;
import com.teamunify.i18n.I;
import com.vaadin.annotations.Viewport;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.CustomizedSystemMessages;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.DefaultSystemMessagesProvider;
import com.vaadin.server.Page;
import com.vaadin.server.SystemMessages;
import com.vaadin.server.SystemMessagesInfo;
import com.vaadin.server.SystemMessagesProvider;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import hu.lanoga.toolbox.exception.ErrorResult;
import hu.lanoga.toolbox.exception.ExceptionManager;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.util.BrandUtil;
import hu.lanoga.toolbox.util.SpecialLogUtil;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.extern.slf4j.Slf4j;

@MetaTags({ @Meta(name = "robots", content = "noindex, nofollow") })
// @org.vaadin.leif.headertags.Link(rel = "manifest", href = "/b/assets/manifest.json") // RedirectController-ben van brand specifikusra állítva, // TODO: ha nem root a contextPath akkor nem megy
@Viewport(value = "width=device-width, initial-scale=1, minimum-scale=0.5")
@Widgetset("AppWidgetset")
@Slf4j
public abstract class AbstractToolboxUI extends UI {

	private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
	private static final ScheduledExecutorService secondaryScheduledExecutorService = Executors.newScheduledThreadPool(1);

	public static <V> ScheduledFuture<V> scheduleRunnable(final Callable<V> command, final long delay, final TimeUnit timeUnit) {
		((ScheduledThreadPoolExecutor) scheduledExecutorService).setRemoveOnCancelPolicy(true); // e nélkül memory leak lehet!
		return scheduledExecutorService.schedule(command, delay, timeUnit);
	}

	public static void purgeScheduledExecutorService() {
		((ScheduledThreadPoolExecutor) scheduledExecutorService).purge();
	}

	public static <V> ScheduledFuture<V> scheduleSecondaryRunnable(final Callable<V> command, final long delay, final TimeUnit timeUnit) {
		((ScheduledThreadPoolExecutor) secondaryScheduledExecutorService).setRemoveOnCancelPolicy(true); // e nélkül memory leak lehet!
		return secondaryScheduledExecutorService.schedule(command, delay, timeUnit);
	}

	public static void purgeSecondaryScheduledExecutorService() {
		((ScheduledThreadPoolExecutor) secondaryScheduledExecutorService).purge();
	}

	public static class ToolboxUIErrorHandler extends DefaultErrorHandler {

		protected final WeakReference<AbstractToolboxUI> wrUI; // WeakReference... "just in case" + optimalizáció hogy könnyebb dolga legyen a garbage collectornak

		protected ToolboxUIErrorHandler(final AbstractToolboxUI ui) {
			this.wrUI = new WeakReference<>(ui);
		}

		protected ToolboxUIErrorHandler() {
			this.wrUI = null;
		}

		@Override
		public void error(final com.vaadin.server.ErrorEvent event) {

			try {

				final Throwable throwable = DefaultErrorHandler.findRelevantThrowable(event.getThrowable());

				if (throwable instanceof java.net.URISyntaxException || throwable.getCause() instanceof java.net.URISyntaxException) { // itt azért van megnézve a getCause is, mert a java.net.URISyntaxException egy RuntimeException-be van belecsomagolva
					// TODO: itt még pontosítani lehetne, hogy ne a főoldalra irányítson át
					Page.getCurrent().setLocation(VaadinService.getCurrentRequest().getContextPath() + "/"); // vissza írányít a főoldalra (ez akkor következik be, ha a uri fragment duplikálódik)
				} else if (throwable instanceof java.lang.IllegalArgumentException) {
					Page.getCurrent().setLocation(VaadinService.getCurrentRequest().getContextPath() + "/"); // vissza írányít a főoldalra, ha illegal argument exception-be ütközik, a ToolboxAssert is ilyen exception-t dob (pl. ez Homefood-nál előfordul)
				}

				final AbstractToolboxUI abstractToolboxUI = this.wrUI.get();

				if (abstractToolboxUI != null) {
					abstractToolboxUI.showErrorMsg(throwable);
				}

			} catch (final Exception e) {
				log.error("ErrorHandler failed 1!", e);
				log.error("ErrorHandler failed 2!", event.getThrowable());
			}

			// ---

			// doDefault(event); // itt direkt nincs meghívva
		}

	}

	private final String uiUuid;

	private final ConcurrentHashMap<String, WeakReference<Consumer<String>>> atuiCallbackMap;

	public AbstractToolboxUI() {

		super();

		this.uiUuid = UUID.randomUUID().toString();
		this.atuiCallbackMap = new ConcurrentHashMap<>();

		// ---

		String titleStr = BrandUtil.getAppTitle(true);

		final ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();
		if (loggedInUser != null) {
			titleStr += " (t" + loggedInUser.getTenantId() + "u" + loggedInUser.getId() + ")";
		}

		this.getPage().setTitle(titleStr);

	}

	/**
	 * extra isAttached() check...
	 */
	@Override
	public Future<Void> access(final Runnable runnable) {

		if (this.isAttached()) {
			return super.access(runnable);
		}

		log.debug("Detached UI!");

		return Futures.immediateCancelledFuture();
	}

	@Override
	public void attach() {

		super.attach();

		log.debug("ToolboxUI (" + this.getClass().getSimpleName() + ") attach: " + this.getUIId() + ", " + this.getUiUuid());
	}

	@Override
	public void detach() {

		log.debug("ToolboxUI (" + this.getClass().getSimpleName() + ") detach: " + this.getUIId() + ", " + this.getUiUuid());

		uiWrMap.remove(this.uiUuid);

		super.detach();
	}

	@Override
	protected void finalize() throws Throwable {

		try {
			uiWrMap.remove(this.uiUuid);
		} catch (Exception e) {
			//
		}

		super.finalize();
	}

	/**
	 * hasonló, mint a {@link #getUIId()}, de {@link UUID} alapú
	 *
	 * @return
	 */
	public String getUiUuid() {
		return this.uiUuid;
	}

	@Override
	public void push() {
		if (this.isAttached()) {
			super.push();
		}
	}

	/**
	 * hibaüzenenet mutatás a {@link ExceptionManager} alapján
	 *
	 * @param throwable
	 */
	@SuppressWarnings("deprecation")
	public void showErrorMsg(Throwable throwable) {

		final ExceptionManager exceptionManager = ApplicationContextHelper.getBean(ExceptionManager.class);

		if (throwable instanceof java.util.concurrent.ExecutionException && throwable.getCause() != null) {
			throwable = throwable.getCause();
		}

		final ErrorResult errorResult = exceptionManager.exceptionToErrorResult(throwable);
		exceptionManager.logErrorResult(errorResult, throwable);

		final String errorMessage = ExceptionManager.errorMessageExtractor(errorResult);

		// ---

		Notification.Type notifType = null;

		switch (errorResult.getHttpStatus()) {

		case CONFLICT:
		case FAILED_DEPENDENCY:
		case FORBIDDEN:
		case NOT_FOUND:
		case BAD_REQUEST:
		case UNAUTHORIZED:
			notifType = Notification.Type.WARNING_MESSAGE;
			break;

		case INTERNAL_SERVER_ERROR:
			this.showUnexpectedBugErrorDialog(errorMessage); // spec. eset, itt inkább nem a piros Vaadin notif. van, hanem egy szebb ablak, több szöveggel
			break;
		default:
			notifType = Notification.Type.WARNING_MESSAGE;

		}

		// ---

		if (notifType != null) {
			AbstractToolboxUI.this.showNotification(errorMessage, notifType);
		}

	}

	private void showUnexpectedBugErrorDialog(final String errorMessage) {

		final Window ed = new Window(I.trc("Caption", "Error"));
		ed.setModal(true);
		ed.setWidth("500px");
		ed.setHeight("");

		final VerticalLayout vlContent = new VerticalLayout();
		vlContent.setWidth("100%");

		boolean emailErrorReport = false;

		try {
			emailErrorReport = ApplicationContextHelper.getConfigProperty("tools.mail.error-report.enabled", boolean.class);
		} catch (final Exception e) {
			// TODO: handle exception
		}

		String strInfo = getUnexpectedErrorMsg();
		if (emailErrorReport) {
			if (!strInfo.endsWith(" ")) {
				strInfo += " ";
			}
			strInfo += getUnexpectedErrorAdminsWereNotifiedMsg();
		}

		final Label lblInfo = new Label(strInfo);
		lblInfo.setWidth("100%");
		vlContent.addComponents(lblInfo);

		if (allowDetailsAfterUnexpectedErrorMsg()) {
			final Label lblInfo3 = new Label(I.trc("Caption", "Details") + ": " + errorMessage);
			lblInfo3.addStyleName(ValoTheme.LABEL_LIGHT);
			lblInfo3.setWidth("100%");
			vlContent.addComponents(lblInfo3);
		}

		final Button btnClose = new Button(I.trc("Button", "Close"));
		btnClose.setWidth("100%");
		btnClose.addClickListener(x -> {
			UiHelper.closeParentWindow(btnClose);
		});
		vlContent.addComponent(btnClose);
		vlContent.setComponentAlignment(btnClose, Alignment.BOTTOM_LEFT);

		ed.setContent(vlContent);
		this.addWindow(ed);
	}

	protected String getUnexpectedErrorMsg() {
		return I.trc("Caption", "An unexpected error has occurred in our system. ");
	}

	protected String getUnexpectedErrorAdminsWereNotifiedMsg() {
		return I.trc("Caption", "The administrators/developers have been automatically notified about the error/bug. "); // itt volt egy szóköz a végén, nem szedtem ki, hátha a fordításba már így került be...
	}

	protected boolean allowDetailsAfterUnexpectedErrorMsg() {
		return true;
	}

	private static ConcurrentHashMap<String, WeakReference<AbstractToolboxUI>> uiWrMap = new ConcurrentHashMap<>();

	public static Collection<WeakReference<AbstractToolboxUI>> getUiWrSet() {

		SecurityUtil.limitAccessSuperAdmin();

		Collection<WeakReference<AbstractToolboxUI>> values = uiWrMap.values();
		for (WeakReference<AbstractToolboxUI> value : values) {
			if (value.get() == null) { // ha már nem létezik az objektum, amire a WeakReference
				values.remove(value); // mivel itt mögötte ConcurrentHashMap van, ezért iterator nélkül is lehet a collection-ből kiszedni
			}
		}

		return values;

	}

	@Override
	protected void init(final VaadinRequest vaadinRequest) {

		log.debug("ToolboxUI (" + this.getClass().getSimpleName() + ") init: " + this.getUIId() + ", " + this.getUiUuid());

		try {
			if (VaadinServletRequest.getCurrent() != null) {
				SpecialLogUtil.logL1(VaadinServletRequest.getCurrent().getHttpServletRequest(), this.getClass().getSimpleName() + ", init()", true);
			}
		} catch (Exception e) {
			//
		}

		// ---

		uiWrMap.put(this.getUiUuid(), new WeakReference<>(this));

		// ---

		this.setLocale(Locale.US); // a feliratokat már nem ez befolyásolja, ez csak a Vaadin komponensek (pl. DateTimeField) számára fontos

		// ---

		this.setResizeLazy(true);

		// ---

		this.setErrorHandler(new ToolboxUIErrorHandler(this));

		// ---

		// if (this.getPushConfiguration().getPushMode().equals(PushMode.DISABLED)) {
		// log.info("setPollInterval() 60 sec (because PushMode is DISABLED)");
		// this.setPollInterval(1000 * 60);
		// }

		// ---

		final VaadinService vaadinService = this.getSession().getService();

		if (vaadinService.getSystemMessagesProvider() instanceof DefaultSystemMessagesProvider) {

			// lazy, ha már nem a DefaultSystemMessagesProvider van a VaadinService-nál beállítva, akkor az instaceof miatt nem jön be mégeyszer ide

			vaadinService.setSystemMessagesProvider(new SystemMessagesProvider() {

				@Override
				public SystemMessages getSystemMessages(final SystemMessagesInfo systemMessagesInfo) {

					final CustomizedSystemMessages systemMessages = new CustomizedSystemMessages();

					systemMessages.setCookiesDisabledCaption(I.trc("Notification", "The cookies are disabled"));
					systemMessages.setCookiesDisabledMessage(I.trc("Notification", "Enable the cookies in your browser then try again."));

					systemMessages.setAuthenticationErrorCaption(I.trc("Notification", "Wrong username or password"));
					systemMessages.setAuthenticationErrorMessage(I.trc("Notification", "Login failed, please try again."));

					systemMessages.setCommunicationErrorCaption(I.trc("Notification", "Communication error"));
					systemMessages.setCommunicationErrorMessage(I.trc("Notification", "The connection with the server has been terminated. Reconnection is in progress."));

					systemMessages.setSessionExpiredCaption(I.trc("Notification", "Session expired"));
					systemMessages.setSessionExpiredMessage(I.trc("Notification", "Take note of any unsaved data, and <u>click here</u> or press ESC to continue."));

					systemMessages.setInternalErrorCaption(I.trc("Notification", "Internal error"));
					systemMessages.setInternalErrorMessage(I.trc("Notification", "Please notify the administrator.<br/>Take note of any unsaved data, and <u>click here</u> or press ESC to continue."));

					return systemMessages;

				}
			});

			log.debug("Custom SystemMessagesProvider was set");
		}

		// ---

		this.getPage().getJavaScript().addFunction("atuiCallback", x -> {

			final String key = x.get(0).asString();

			final WeakReference<Consumer<String>> value = this.atuiCallbackMap.get(key);

			if (value != null && value.get() != null) {
				value.get().accept(x.getString(1));
			} else {
				this.atuiCallbackMap.remove(key);
			}

		});

		// ---

		// IOS esetén visszalépésnél az oldalt újra is kell tölteni. Lásd: https://stackoverflow.com/questions/8788802/prevent-safari-loading-from-cache-when-back-button-is-clicked
		this.getPage().getJavaScript().execute("window.onpageshow = function(event) { if (event.persisted) { window.location.reload(); }};");

	}

	/**
	 * Az "atuiCallback()" JavaScript hívásokhoz callback...
	 * <p>
	 * A visszaadott értékkel lehet {@link #unregisterAtuiCallbackConsumer(String)}-t hívni, de a {@link WeakReference} jelleg okán akkor sincs leak,
	 * ha az {@link #unregisterAtuiCallbackConsumer(String)} hívás elmarad.
	 * <p>
	 * Fontos, hogy a {@link Consumer} a másik oldalon field legyen (különben a {@link WeakReference} miatt senki nem "fogná").
	 *
	 * @param callbackConsumer
	 * @return
	 */
	public String registerAtuiCallbackConsumer(final WeakReference<Consumer<String>> callbackConsumer) {

		final String key = UUID.randomUUID().toString();

		this.atuiCallbackMap.put(key, callbackConsumer);

		return key;
	}

	/**
	 * @param key {@link #registerAtuiCallbackConsumer(WeakReference)} hívásnál kapott {@code String}
	 */
	public void unregisterAtuiCallbackConsumer(final String key) {
		this.atuiCallbackMap.remove(key);
	}

	/**
	 * @param key    
	 * 		{@link #registerAtuiCallbackConsumer(WeakReference)} hívásnál kapott {@code String}
	 * @param script 
	 * 		egy JS function String visszatérési értékkel (pl.: "function() { x = 1; y = 2; return '' + x + y; }"), ennek visszatéri értékét kapja meg a callback {@link Consumer} paraméterként
	 *
	 * @see #registerAtuiCallbackConsumer(WeakReference)
	 * @see JavaScript#eval(String)
	 */
	public void callJsViaAtuiCallback(final String key, final String script) {
		this.getPage().getJavaScript().execute("f = " + script + "; v = f(); atuiCallback('" + key + "', v);");
	}

	/**
	 * @param localStorageKey
	 * @param callbackConsumer
	 * @see #registerAtuiCallbackConsumer(WeakReference)
	 * @see #callJsViaAtuiCallback(String, String)
	 */
	public void retrieveLocalStorageValueViaAtuiCallback(final String localStorageKey, final WeakReference<Consumer<String>> callbackConsumer) {

		final String callbackKey = this.registerAtuiCallbackConsumer(callbackConsumer);

		this.callJsViaAtuiCallback(callbackKey, "function() { return String(window.localStorage.getItem('" + localStorageKey + "')); }");

	}

	/**
	 * a UI glitch-ek kezelésére lehet jó (ilyenkor tesz egy "extra" "kört" a böngésző és a szerver között...)
	 *
	 * @param callbackConsumer 
	 * 		érdemi értéket nem kap, csak a JS callback lefutását jelzi (ide lehet setVisible() hívásokat tenni)
	 * @see #registerAtuiCallbackConsumer(WeakReference)
	 * @see #callJsViaAtuiCallback(String, String)
	 */
	public void emptyCallbackViaAtuiCallback(final WeakReference<Consumer<String>> callbackConsumer) {

		final String callbackKey = this.registerAtuiCallbackConsumer(callbackConsumer);

		this.callJsViaAtuiCallback(callbackKey, "function() { return 'empty'; };");

	}

	/**
	 * @param localStorageKey
	 * @param localStorageValue
	 * @see JavaScript#eval(String)
	 */
	public void setLocalStorageValue(final String localStorageKey, final String localStorageValue) {
		this.getPage().getJavaScript().execute("window.localStorage.setItem('" + localStorageKey + "', '" + localStorageValue + "')");
	}

	/**
	 * @param localStorageKey
	 * @see JavaScript#eval(String)
	 */
	public void removeLocalStorageValue(final String localStorageKey) {
		this.getPage().getJavaScript().execute("window.localStorage.removeItem('" + localStorageKey + "');");
	}

	public void showShutdownWarning() {
		this.showNotification(I.trc("Caption", "The system will be updated/restarted in 1-2 minutes. Please save your work, and continue after the restart (~10 min)! Sorry for the inconvinience."), Notification.Type.ERROR_MESSAGE);
	}

}
