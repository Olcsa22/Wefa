package hu.lanoga.toolbox.exception;

import java.io.IOException;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.validator.internal.engine.MessageInterpolatorContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MissingRequestHeaderException;

import hu.lanoga.toolbox.config.RequestFilterConfig;
import hu.lanoga.toolbox.email.EmailErrorReportManager;
import hu.lanoga.toolbox.exception.ErrorResult.ErrorResultBuilder;
import hu.lanoga.toolbox.i18n.I18nUtil;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.validation.ToolboxConstraintValidationException;
import hu.lanoga.toolbox.validation.UserSessionAwareMessageInterpolator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ConditionalOnMissingBean(name = "exceptionManagerOverrideBean")
@Component
public class ExceptionManager {

	public void logErrorResult(final ErrorResult errorResult, final Throwable e) {
		this.logErrorResult(errorResult, e, false);
	}

	public void logErrorResult(final ErrorResult errorResult, final Throwable e, final boolean skipLogEmail) {

		final String msg = "errorResult: " + errorResult + "; last HTTP request: " + RequestFilterConfig.getRequestLogMsg() + "; " + e.getMessage();

		if (!Boolean.TRUE.equals(errorResult.getMute())) {

			switch (errorResult.getHttpStatus()) {

			// case CONFLICT:
			// case FAILED_DEPENDENCY:
			// case FORBIDDEN:
			// case NOT_FOUND:
			// case BAD_REQUEST:
			// case UNAUTHORIZED:
			// log.debug(msg, e);
			// break;

			case INTERNAL_SERVER_ERROR:
				log.error(msg, e);
				break;

			default:
				log.warn(msg, e);

			}

			if (!skipLogEmail && HttpStatus.INTERNAL_SERVER_ERROR.equals(errorResult.getHttpStatus())) {

				try {
					ApplicationContextHelper.getBean(EmailErrorReportManager.class).addLogMail(msg, e);
					// TODO: mi van bg job hibák esetén (értsd: ami nem fut be a ControllerExHandler-be, így ide sem)? (úgy értve, hogy ott a exception util sem játszik)
				} catch (final org.springframework.beans.factory.NoSuchBeanDefinitionException e2) {
					// ha az EmailErrorReportManager nem elérhető, akkor nem küldünk semmit
					log.debug("EmailErrorReportManager missing (log email skipped...)!");
				}

			}

		//} else {
			//log.debug("muted error: " + errorResult);
		}
	}

	public ErrorResult exceptionToErrorResult(Throwable e) {

		if (e instanceof com.vaadin.event.ListenerMethod.MethodException && e.getCause() != null) {

			// Vaadin van, hogy beburkolja (pl.: btnValami.click(); hívás esetén) com.vaadin.event.ListenerMethod.MethodException és ezért nem látszik a érdemi ex. (nem lenne jó a mapping/elágazás lent)

			e = e.getCause();
		}

		// ---

		final ErrorResultBuilder errorResultBuilder = ErrorResult.builder();

		if (e instanceof org.springframework.security.access.AccessDeniedException) {

			errorResultBuilder.description1("Access denied.");
			errorResultBuilder.description3("{ \"hu\":\"Hozzáférés megtagadva.\", \"en\":\"Access denied.\" }");
			errorResultBuilder.httpStatus(HttpStatus.FORBIDDEN);

		} else if ((e instanceof javax.validation.ValidationException) || (e instanceof org.springframework.web.bind.MethodArgumentNotValidException)) { //

			errorResultBuilder.description1("Invalid value(s).").description2(null);
			errorResultBuilder.httpStatus(HttpStatus.BAD_REQUEST);

			if (e instanceof FileLockException) {

				errorResultBuilder.description1("File lock exception.");
				String d3 = "{ \"hu\":\"A fájl zárolt.\", \"en\":\"The file is locked.\" }";

				String uiMessage = ((ManualValidationException) e).getUiMessageJson();
				if (StringUtils.isNotBlank(uiMessage)) {
					d3 = uiMessage;
				}

				errorResultBuilder.description3(d3);

			} else if (e instanceof ToolboxConstraintValidationException) {
				
				errorResultBuilder.description3(tcveConvertToJsonString((ToolboxConstraintValidationException) e));

			// } else if (e instanceof org.springframework.web.bind.MethodArgumentNotValidException) {
				
				// errorResultBuilder.description3(tcveConvertToJsonString((ToolboxConstraintValidationException) e)); // TODO: implementálni (valahogy MethodArgumentNotValidException-ből is ki lehet túrni a hibák helyét) (PT) (nem égető)

			} else {

				String d3 = "{ \"hu\":\"Felvitt adat(ok) nem megfelelő(ek) (formátum, hossz, hiányzó mező...).\", \"en\":\"The entered data is incorrect (format, length, missing field...).\" }";

				if (e instanceof ManualValidationException) {

					String uiMessage = ((ManualValidationException) e).getUiMessageJson();
					if (StringUtils.isNotBlank(uiMessage)) {
						d3 = uiMessage;
					}

				}

				errorResultBuilder.description3(d3);
			}

		} else if (e instanceof org.springframework.security.core.AuthenticationException) {

			errorResultBuilder.description1("Auth error.");
			errorResultBuilder.description3("{ \"hu\":\"Bejelentkezési hiba.\", \"en\":\"Authentication error.\" }");
			errorResultBuilder.httpStatus(HttpStatus.UNAUTHORIZED);

		} else if (e instanceof org.springframework.dao.DuplicateKeyException) {

			errorResultBuilder.description1("Conflict.");
			errorResultBuilder.description3("{ \"hu\":\"Ütköző rekord.\", \"en\":\"Conflicting record.\" }");
			errorResultBuilder.httpStatus(HttpStatus.CONFLICT);

			// TODO: lehet, de dke.getCause() is legyen egy belső instacene of-fal megnézve, ha postgre, akkor logoljon, ha nem akkor nem (értsd akkor csak desc1 és dec3 lesz, desc2 nem) // PT

			// try {
			// final org.springframework.dao.DuplicateKeyException dke = (DuplicateKeyException) e;
			// final org.postgresql.util.PSQLException psqle = (org.postgresql.util.PSQLException) dke.getCause();
			//
			// final String d2 = psqle.getServerErrorMessage().getConstraint();
			// errorResultBuilder.description2(d2);
			//
			// } catch (final Exception e2) {
			// log.warn("ToolboxExceptionUtil internal: DuplicateKeyException processing error (relevant key could not be determined)!", e2);
			// }

		} else if (e instanceof org.springframework.dao.EmptyResultDataAccessException || e instanceof ToolboxNotFoundException) {

			errorResultBuilder.description1("Not found.");
			errorResultBuilder.description3("{ \"hu\":\"Nem található.\", \"en\":\"Not found.\" }");
			errorResultBuilder.httpStatus(HttpStatus.NOT_FOUND);

		} else if (e instanceof org.springframework.web.HttpRequestMethodNotSupportedException || e instanceof org.springframework.web.bind.MissingServletRequestParameterException) {

			errorResultBuilder.description1("Bad call.");
			errorResultBuilder.description3("{ \"hu\":\"Hibás kérés.\", \"en\":\"Bad call.\" }");
			errorResultBuilder.httpStatus(HttpStatus.BAD_REQUEST);

			if (e.getMessage() != null && e.getMessage().equalsIgnoreCase("Request method 'POST' not supported")) {
				errorResultBuilder.mute(Boolean.TRUE);
			}

		} else if (e instanceof org.springframework.web.multipart.MultipartException) {

			errorResultBuilder.description1("Failed upload (multipart error).");
			errorResultBuilder.description3("{ \"hu\":\"Megszakadt feltöltés.\", \"en\":\"Failed upload.\" }");
			errorResultBuilder.httpStatus(HttpStatus.BAD_REQUEST);

		} else if (e instanceof java.lang.IllegalArgumentException) {

			errorResultBuilder.description1("Bad call.");
			errorResultBuilder.description3("{ \"hu\":\"Hibás hívás.\", \"en\":\"Bad call.\" }");
			errorResultBuilder.httpStatus(HttpStatus.INTERNAL_SERVER_ERROR);

			try {
				final String stackTrace = ExceptionUtils.getStackTrace(e);
				if (stackTrace.contains(".controller")) {
					errorResultBuilder.description1("Bad call.");
					errorResultBuilder.description3("{ \"hu\":\"Hibás kérés.\", \"en\":\"Bad call.\" }");
					errorResultBuilder.httpStatus(HttpStatus.BAD_REQUEST);
				}
			} catch (final Exception e2) {
				log.warn("ToolboxExceptionUtil internal: IllegalArgumentException processing error!", e2);
			}

		} else if (e instanceof org.springframework.dao.IncorrectUpdateSemanticsDataAccessException) {
	
			errorResultBuilder.description1("Not found.");
			errorResultBuilder.description3("{ \"hu\":\"Nem található.\", \"en\":\"Not found.\" }");
			errorResultBuilder.httpStatus(HttpStatus.FAILED_DEPENDENCY); // TODO: NOT_FOUND inkább?
	
		} else if (e instanceof java.lang.UnsupportedOperationException) {

			errorResultBuilder.description1("Unsupported operation.");
			errorResultBuilder.description3("{ \"hu\":\"Nincs megvalósítva.\", \"en\":\"Unsupported operation.\" }");
			errorResultBuilder.httpStatus(HttpStatus.INTERNAL_SERVER_ERROR);

			try {
				final String stackTrace = ExceptionUtils.getStackTrace(e);
				if (stackTrace.contains(".controller") || stackTrace.contains("(client error / bad request)")) {
					errorResultBuilder.description1("Bad call.");
					errorResultBuilder.description3("{ \"hu\":\"Hibás kérés.\", \"en\":\"Bad call.\" }");
					errorResultBuilder.httpStatus(HttpStatus.BAD_REQUEST);
				}
			} catch (final Exception e2) {
				log.warn("ToolboxExceptionUtil internal: UnsupportedOperationException processing error!", e2);
			}

		} else if (e instanceof StaleDataException) {

			errorResultBuilder.description1("Stale data.");
			errorResultBuilder.description3("{ \"hu\":\"Elavult adat.\", \"en\":\"Stale data.\" }");
			errorResultBuilder.httpStatus(HttpStatus.BAD_REQUEST);

			String uiMessage = ((StaleDataException) e).getUiMessageJson();
			if (StringUtils.isNotBlank(uiMessage)) {
				errorResultBuilder.description3(uiMessage);
			}

		} else if (e instanceof ToolboxInterruptedException) {

			errorResultBuilder.description1("Interrupted (canceled?).");
			errorResultBuilder.description3("{ \"hu\":\"Megszakítva.\", \"en\":\"Interrupted.\" }");
			errorResultBuilder.httpStatus(HttpStatus.NOT_FOUND);

		} else if (e instanceof org.springframework.dao.DataIntegrityViolationException) {

			errorResultBuilder.description1("Data integrity error.");
			errorResultBuilder.description3("{ \"hu\":\"Adat integritási hiba.\", \"en\":\"Data integrity error.\" }");
			errorResultBuilder.httpStatus(HttpStatus.BAD_REQUEST);

			try {
				final String stackTrace = ExceptionUtils.getStackTrace(e);
				if (stackTrace.contains("violates not-null constraint")) {
					errorResultBuilder.description2("Not-null constraint violation.");
					errorResultBuilder.description3("{ \"hu\":\"Nem lehet üres.\", \"en\":\"Can not be empty.\" }");
				}
			} catch (final Exception e2) {
				log.warn("ToolboxExceptionUtil internal: DataIntegrityViolationException processing error!", e2);
			}

		} else if (e instanceof org.springframework.http.converter.HttpMessageNotReadableException) {
	
			errorResultBuilder.description1("JSON deserialization error.");
			errorResultBuilder.description3("{ \"hu\":\"JSON feldolgozási hiba.\", \"en\":\"JSON processing error.\" }");
			errorResultBuilder.httpStatus(HttpStatus.BAD_REQUEST);
	
		} else if (e instanceof hu.lanoga.toolbox.file.FileStoreMimeCheckException) {
	
			errorResultBuilder.description1("Bad call.");
			errorResultBuilder.description3("{ \"hu\":\"Nem megfelelő fájl típus.\", \"en\":\"Invalid file type.\" }");
			errorResultBuilder.httpStatus(HttpStatus.BAD_REQUEST);
	
		} else if (e instanceof hu.lanoga.toolbox.file.FileStoreFileCountException) {
	
			errorResultBuilder.description1("Bad call.");
			errorResultBuilder.description3("{ \"hu\":\"Túllépte a fájl szám limitet.\", \"en\":\"Exceeded file count limit.\" }");
			errorResultBuilder.httpStatus(HttpStatus.BAD_REQUEST);
			
		// } else if (e instanceof hu.lanoga.toolbox.export.ExporterException) { // TODO: tisztázni
			
			// errorResultBuilder.description1("ExporterOptions error.");
			// errorResultBuilder.description3("{ \"hu\":\"ExporterOptions hiba!\", \"en\":\"ExporterOptions error!\" }");
			// errorResultBuilder.httpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			//
			// try {
			// final String stackTrace = ExceptionUtils.getStackTrace(e);
			// if (stackTrace.contains("java.util.concurrent.CancellationException")) {
			// errorResultBuilder.description1("ExporterOptions cancelled.");
			// errorResultBuilder.description3("{ \"hu\":\"ExporterOptions megszakítva.\", \"en\":\"ExporterOptions cancelled.\" }");
			// errorResultBuilder.httpStatus(HttpStatus.NOT_FOUND); // TODO: ide mit?
			// }
			// } catch (final Exception e2) {
			// log.warn("ToolboxExceptionUtil internal: ExporterException processing error!", e2);
			// }
			
		} else if (e instanceof IOException && "org.apache.catalina.connector.ClientAbortException".equalsIgnoreCase(e.getClass().getName())) {

			// azért nem sima instanceof, mert a Tomcat lehet, hogy nincs a CLASSPATH-ban (időként embedded Jetty-t használunk)

			errorResultBuilder.description1("Client abort.");
			errorResultBuilder.description3("{ \"hu\":\"Kliens megszakítás.\", \"en\":\"Client abort.\" }");
			errorResultBuilder.httpStatus(HttpStatus.BAD_REQUEST);
			
		} else if (e instanceof com.vaadin.server.UploadException) { 
			
			// ahol gyári Vaadin upload van, lásd hu.lanoga.toolbox.vaadin.component.file.SimpleFileUploadField
			
			if (e.getCause() != null && e.getCause().getCause() != null && e.getCause().getCause() instanceof hu.lanoga.toolbox.exception.ManualValidationException) {
				return exceptionToErrorResult(e.getCause().getCause());
			}
			
			errorResultBuilder.description1("Upload error.");
			errorResultBuilder.description3("{ \"hu\":\"Hiba feltöltés közben.\", \"en\":\"Upload error.\" }");
			errorResultBuilder.httpStatus(HttpStatus.BAD_REQUEST);
			
		} else if (e instanceof HttpMediaTypeNotAcceptableException) {
			
			errorResultBuilder.description1("Other error.");
			errorResultBuilder.description3("{ \"hu\":\"Hiba történt.\", \"en\":\"An error has occurred.\" }");
			errorResultBuilder.httpStatus(HttpStatus.BAD_REQUEST);
			
		} else if (e instanceof MissingRequestHeaderException) {
			
			errorResultBuilder.description1("Other error.");
			errorResultBuilder.description3("{ \"hu\":\"Hiba történt.\", \"en\":\"An error has occurred.\" }");
			errorResultBuilder.httpStatus(HttpStatus.BAD_REQUEST);

		} else {
	
			errorResultBuilder.description1("Other error.");
			errorResultBuilder.description3("{ \"hu\":\"Hiba történt.\", \"en\":\"An error has occurred.\" }");
			errorResultBuilder.httpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
	
		}

		return errorResultBuilder.build();

	}

	protected static String tcveConvertToJsonString(final ToolboxConstraintValidationException customValidationException) {

		try {
			final Set<ConstraintViolation<Object>> constraintViolationSet = customValidationException.getConstraintViolationSet();

			final JSONArray ja = new JSONArray();

			for (final ConstraintViolation<Object> constraintViolation : constraintViolationSet) {

				final JSONObject jo = new JSONObject();

				jo.put("propertyPath", constraintViolation.getPropertyPath().toString());

				String strErrorType = constraintViolation.getMessageTemplate();
				strErrorType = StringUtils.replace(strErrorType, "javax.validation.constraints.", "");
				strErrorType = StringUtils.replace(strErrorType, "org.hibernate.validator.constraints.", "");
				strErrorType = StringUtils.replace(strErrorType, ".message", "");
				strErrorType = StringUtils.replace(strErrorType, "{", "");
				strErrorType = StringUtils.replace(strErrorType, "}", "");

				String strErrorMessage = constraintViolation.getMessage(); // régi, nem a belépett user alapján nyelvesített

				try {

					final UserSessionAwareMessageInterpolator userSessionAwareMessageInterpolator = ApplicationContextHelper.getBean(UserSessionAwareMessageInterpolator.class);

					strErrorMessage = userSessionAwareMessageInterpolator.interpolate(constraintViolation.getMessageTemplate(),
							new MessageInterpolatorContext(constraintViolation.getConstraintDescriptor(), constraintViolation.getInvalidValue(), constraintViolation.getRootBean().getClass(), null, null, null, null, false));

				} catch (Exception e) {
					log.warn("UserSessionAwareMessageInterpolator failed... using simpler strErrorMessage...", e);
				}

				jo.put("errorType", strErrorType);
				jo.put("errorMessage", strErrorMessage);

				ja.put(jo);

			}

			return ja.toString();

		} catch (final Exception e) {
			log.error("EgoConstraintValidationException convertToJsonString failed", e);
		}

		return null;

	}

	/**
	 * I18nUtil segitsegevel kinyeri a description3-bol a nyelvesitett hiba uzenetet
	 * A hibauzenet nyelve a bejelentkezett user locale-ja alapjan dol el
	 *
	 * @param error
	 * @return
	 */
	public static String errorMessageExtractor(ErrorResult error) {
		if (StringUtils.isNotBlank(error.getDescription3())) {
			try {
				return I18nUtil.extractMsgFromMultiLang(error.getDescription3(), I18nUtil.getLoggedInUserLocale().getLanguage(), null);
			} catch (Exception e) {
				// throw new ToolboxGeneralException("errorMessageExtractor error ", e);
				log.warn("errorMessageExtractor failed: " + e.getMessage());
			}
		}

		return I18nUtil.extractMsgFromMultiLang("{ \"hu\":\"Hiba történt.\", \"en\":\"An error has occurred.\" }", I18nUtil.getLoggedInUserLocale().getLanguage(), null);
	}

}
