package hu.lanoga.toolbox.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.FileOperationAccessTypeIntent;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.user.PublicTokenHolder;
import hu.lanoga.toolbox.vaadin.component.file.FileManagerComponent;
import hu.lanoga.toolbox.vaadin.component.file.FileUploadComponent;
import hu.lanoga.toolbox.vaadin.component.file.FileUploadInstanceHolder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ConditionalOnMissingBean(name = "fileControllerOverrideBean") // TODO: nincs benne a Store szó az override bean nevében... egyelőre hagyom így, át kell nézni minden rendszert
@ConditionalOnProperty(name = "tools.file.controller.enabled", matchIfMissing = true)
@RestController
public class FileStoreController {

	public static final Cache<String, Boolean> uploadVaadinInProgressUploadIds = CacheBuilder.newBuilder().concurrencyLevel(4).expireAfterWrite(18, TimeUnit.SECONDS).build(); // nem cache szó szerint, a timeout és a jó/beépített concurrency kezelés miatt hasznájuk (ConccurentHashMap stb. helyett)

	@Autowired
	private FileStoreService fileStoreService;
	
	@Autowired
	private PublicTokenHolder publicTokenHolder;

	/**
	 * projekt specifikusan további security limiteket lehet érvényesíteni itt 
	 * (dobj {@link AccessDeniedException}-t)... 
	 * 
	 * minden endpointnál meghívódik... 
	 * a token alapúaknál is (user kikeresés után)... 
	 */
	protected void fileStoreControllerRolePreCheck() throws AccessDeniedException {
		
		// alapban nincs semmi
		
	}
	
	@RequestMapping(value = "api/public/files/replace/{fileDescriptorId}", method = RequestMethod.POST)
	public void publicReplaceFileUpload(@PathVariable("fileDescriptorId") final int fileDescriptorId, final MultipartHttpServletRequest request) {

		// TODO: /action/ is legyen benne az URL-ben
		// (ez volt az eredeti konvenció custom műveletekre, de már késő átírni)

		try {

			this.publicTokenHolder.setUserBasedOnToken(request);
			
			fileStoreControllerRolePreCheck();

			this.replaceFileUpload(fileDescriptorId, request);

		} finally {
			SecurityUtil.setAnonymous();
		}
	}

	@RequestMapping(value = "api/files/replace/{fileDescriptorId}", method = RequestMethod.POST) // TODO: /action/ is legyen benne az URL-ben
	public void replaceFileUpload(@PathVariable("fileDescriptorId") final int fileDescriptorId, final MultipartHttpServletRequest request) {

		fileStoreControllerRolePreCheck();
		
		final String notifId = request.getParameter("notifId");
		this.fileStoreService.trickyReplaceFile(fileDescriptorId, request, notifId);

	}

	@RequestMapping(value = "api/files", method = RequestMethod.POST)
	public List<FileDescriptor> upload(final MultipartHttpServletRequest request) {
		
		fileStoreControllerRolePreCheck();
		
		return FileStoreHelper.saveAsTmpFiles(request, ToolboxSysKeys.FileDescriptorLocationType.PROTECTED_FOLDER,
				ToolboxSysKeys.FileDescriptorSecurityType.ADMIN_OR_CREATOR); // szándékosan van így később, amikor a model save van, akkor kell átállítani arra amire szeretnénk a jogosultságot
	}

	@RequestMapping(value = "api/files/cdn", method = RequestMethod.POST)
	public List<FileDescriptor> uploadCdn(final MultipartHttpServletRequest request) {
		
		fileStoreControllerRolePreCheck();
		
		return FileStoreHelper.saveAsTmpFiles(request, ToolboxSysKeys.FileDescriptorLocationType.PUBLIC_FOLDER_FOR_CDN, ToolboxSysKeys.FileDescriptorSecurityType.ADMIN_OR_CREATOR);
	}

	@RequestMapping(value = "api/files/vaadin/progress/{uploadId}", method = RequestMethod.GET)
	public void uploadVaadinInProgress(@PathVariable("uploadId") final String uploadId, @RequestParam(name = "finishedForNow", defaultValue = "false") final String finishedForNow) {

		fileStoreControllerRolePreCheck();
		
		log.debug("uploadVaadinInProgress, uploadId: " + uploadId + ", finshedForNow: " + finishedForNow);

		if (BooleanUtils.toBoolean(finishedForNow)) {
			uploadVaadinInProgressUploadIds.invalidate(uploadId);
		} else {
			uploadVaadinInProgressUploadIds.put(uploadId, Boolean.TRUE);
		}

	}

	/**
	 * Vaadin {@link FileManagerComponent}-hez
	 *
	 * @param request
	 * @param uploadId
	 * @return
	 */
	@RequestMapping(value = "api/files/vaadin/{uploadId}", method = RequestMethod.POST)
	public List<FileDescriptor> uploadVaadin(@PathVariable("uploadId") final String uploadId, final MultipartHttpServletRequest request, final HttpServletResponse response) {

		fileStoreControllerRolePreCheck();
		
		final FileUploadComponent fileUploadComponent = FileUploadInstanceHolder.findInstance(uploadId);

		List<FileDescriptor> uploadedFds = null;
		try {

			FileStoreHelper.checkUploadFiles(request, fileUploadComponent.getRemainingFileCount(), fileUploadComponent.getAllowedMime());

			try {

				final String filePathPrefix = fileUploadComponent.getServerFilePathPrefix();
				if (filePathPrefix != null) {
					FileStoreService.tlFilePathPrefix.set(filePathPrefix); // TODO: experimental, tisztázni
				}

				uploadedFds = FileStoreHelper.saveAsTmpFiles(request, fileUploadComponent.getFileDescriptorLocationType(), fileUploadComponent.getFileDescriptorSecurityType(), fileUploadComponent.getDefaultStatus());

			} finally {
				FileStoreService.tlFilePathPrefix.remove();
			}

			for (final FileDescriptor uploadedFile : uploadedFds) {
				fileUploadComponent.dispatchSuccessEvent(uploadedFile);
			}

			return uploadedFds;

		} catch (final Exception e) {

			fileUploadComponent.showErrorMsg(e);

			response.setStatus(HttpStatus.BAD_REQUEST.value()); // TODO: megnézni, hogy működik-e
			return null;

		}

	}

	@RequestMapping(value = "api/public/files/{fileDescriptorId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public void publicDownload(@PathVariable("fileDescriptorId") final int fileDescriptorId,
			@RequestParam(value = "fd", defaultValue = "false") final boolean fd,
			@RequestParam(value = "childType", required = false) final Integer childTypeCsiId,
			@RequestParam(value = "ct", required = false) final String cacheTs,
			final HttpServletRequest request, final HttpServletResponse response) {

		// TODO: tisztázni mihez készült, milyen kapcsolatban van a Mapfre-vel, egodoc-kal

		try {

			this.publicTokenHolder.setUserBasedOnToken(request);
			
			fileStoreControllerRolePreCheck();

			this.download(fileDescriptorId, fd, childTypeCsiId, cacheTs, request, response);

		} finally {
			SecurityUtil.setAnonymous();
		}

	}

	/**
	 * @param fileDescriptorId
	 * @param fd
	 * 		(force) download true/false (fontos, false esetén most a fájlnév sem lesz bent a header-ben)
	 * @param response
	 * 
	 * @throws IOException
	 */
	@RequestMapping(value = "api/files/{fileDescriptorId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public void download(@PathVariable("fileDescriptorId") final int fileDescriptorId,
			@RequestParam(value = "fd", defaultValue = "false") final boolean fd,
			@RequestParam(value = "childType", required = false) final Integer childTypeCsiId,
			@RequestParam(value = "ct", required = false) final String cacheTs,
			final HttpServletRequest request, final HttpServletResponse response) {
		
		fileStoreControllerRolePreCheck();

		FileDescriptor fileDescriptor;

		if (childTypeCsiId != null) {
			fileDescriptor = this.fileStoreService.getFile2(fileDescriptorId, true, childTypeCsiId, FileOperationAccessTypeIntent.READ_ONLY_INTENT);
		} else {
			fileDescriptor = this.fileStoreService.getFile2(fileDescriptorId, true, null, FileOperationAccessTypeIntent.READ_ONLY_INTENT);
		}

		// ---

		if (StringUtils.isBlank(cacheTs)) {

			final StringBuilder sb = new StringBuilder(request.getRequestURL().toString());

			if (request.getQueryString() != null) {
				sb.append("?");
				sb.append(request.getQueryString());
				sb.append("&");
			} else {
				sb.append("?");
			}

			sb.append("ct=");
			sb.append(fileDescriptor.getModifiedOn().getTime());

			try {
				response.sendRedirect(sb.toString());
			} catch (final IOException e) {
				log.warn("sendRedirect failed", e);
			}
		}

		// ---

		final File file = fileDescriptor.getFile();

		// if (fd || (contentType == null)) { response.setHeader("Content-Type", "application/octet-stream"); } else { response.setHeader("Content-Type", contentType); } // TODO: tisztázni

		// ---

		if (StringUtils.isNotBlank((fileDescriptor.getMimeType()))) {
			response.setHeader("Content-Type", fileDescriptor.getMimeType());
		} else {
			// minden egyéb bináris fájlformátum
			response.setHeader("Content-Type", "application/octet-stream");
		}
		response.setHeader("Content-Length", Long.toString(file.length()));
		response.addHeader("Cache-Control", CacheControl.maxAge(14, TimeUnit.DAYS).cachePrivate().getHeaderValue());

		// ---

		try {

			if (fd) {

				// így szabványos/szabályos a fájlnév (régi böngészők viszont nem ismerik ezt a formátumot!)

				response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(fileDescriptor.getFilename(), "UTF-8").replace("+", "%20"));
			} else {

				response.setHeader("Content-Disposition", "inline");
			}

			FileStoreController.log.info("File download, fileDescriptor: " + fileDescriptor);

			try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file), 64 * 1024); OutputStream outputStream = response.getOutputStream()) {
				IOUtils.copyLarge(inputStream, outputStream);
			}

		} catch (org.apache.catalina.connector.ClientAbortException e) {
			log.warn("Download error, client abort: " + e.getMessage());
		} catch (final Exception e) {
			throw new ToolboxGeneralException(e);
		}

	}

}
