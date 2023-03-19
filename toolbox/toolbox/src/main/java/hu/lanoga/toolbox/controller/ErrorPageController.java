package hu.lanoga.toolbox.controller;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import hu.lanoga.toolbox.exception.ErrorPageManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ConditionalOnMissingBean(name = "errorPageControllerOverrideBean")
@RestController
public class ErrorPageController {
	
	@Autowired
	private ErrorPageManager errorPageManager;
	
	private ResponseEntity<String> inner(final HttpServletRequest request, final int errorCode) {
		final String fromHeader = request.getHeader("From");

		if (StringUtils.isNotBlank(fromHeader) && fromHeader.contains("client-software=")) {
			return new ResponseEntity<>(HttpStatus.valueOf(errorCode));
		}

		try {

			final HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.TEXT_HTML); // TODO: charset UTF-8 nem kell? (lehet, hogy default-ban is az mindig)
			return new ResponseEntity<>(this.errorPageManager.generateErrorPage(errorCode), headers, HttpStatus.valueOf(errorCode));

		} catch (final Exception e) {

			log.error("ErrorPageController generateErrorPage error!", e);
			return new ResponseEntity<>(HttpStatus.valueOf(errorCode));

		}
	}

	@RequestMapping(value = "public/error/{errorCode}", method = RequestMethod.GET)
	public ResponseEntity<String> errorPage(final HttpServletRequest request, @PathVariable("errorCode") final int errorCode) {
		return inner(request, errorCode);
	}

	@RequestMapping(value = "public/no-permission", method = RequestMethod.GET)
	public ResponseEntity<String> noPermissionPage(final HttpServletRequest request) {
		return inner(request, 403);
	}

}
