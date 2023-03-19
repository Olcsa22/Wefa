package hu.lanoga.wefa.controller;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import hu.lanoga.toolbox.util.BrandUtil;
import hu.lanoga.wefa.exception.WefaGeneralException;

@RestController
public class PublicFormDemoController {

	@RequestMapping(value = "/public/public-form-demo-iframe", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String publicFormDemo(HttpServletRequest request, HttpServletResponse response) {

		final VelocityContext velocityContext = new VelocityContext();
		velocityContext.put("iframeUrl", BrandUtil.getRedirectUriHostFrontend() + "public/form?iframe=true#" + request.getParameter("t") + "/" + request.getParameter("p"));

		try (final StringWriter writer = new StringWriter()) {

			final VelocityEngine velocity = new VelocityEngine();
			velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
			velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
			velocity.init();

			velocity.getTemplate("html_templates/public-form-demo-iframe.vm", "UTF-8").merge(velocityContext, writer);
			return writer.toString();

		} catch (IOException e) {
			throw new WefaGeneralException(e);
		}

	}

	@Transactional
	@PostMapping(value = "/api/public/test-upload", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
	public ResponseEntity<String> testUpload(@RequestParam(name = "file") final MultipartFile request) {
		String name = request.getName();
		return new ResponseEntity<>("Name:" + name, HttpStatus.OK);
	}

	// @RequestMapping(value = "/api/public/test-upload", method = RequestMethod.POST)
	// public ResponseEntity<String> upload(final MultipartHttpServletRequest request) {
	// Iterator<String> fileNames = request.getFileNames();
	// return new ResponseEntity<>("File names:" + fileNames.toString(), HttpStatus.OK);
	// }

}
