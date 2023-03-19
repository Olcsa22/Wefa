package hu.lanoga.toolbox.file;

import com.google.common.collect.Iterators;
import com.vaadin.ui.JavaScript;
import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.FileDescriptorStatus;
import hu.lanoga.toolbox.ToolboxSysKeys.FileOperationAccessTypeIntent;
import hu.lanoga.toolbox.controller.ToolboxHttpUtils;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.util.BrandUtil;
import hu.lanoga.toolbox.util.ToolboxAssert;
import hu.lanoga.toolbox.util.ToolboxStringUtil;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.File;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @see FileStoreService
 */
@Slf4j
public class FileStoreHelper {

	/**
	 * @param request
	 * @param allowedFileCount itt értsd: ebben a requestben mennyi fájl lehet maximum (null esetén nincs ellenőrzés)
	 * @param allowedMime      null esetén nincs ellenőrzés
	 */
	public static void checkUploadFiles(final MultipartHttpServletRequest request, final Integer allowedFileCount, final String allowedMime) {

		if ((allowedFileCount != null) && (Iterators.size(request.getFileNames()) > allowedFileCount)) {
			throw new FileStoreFileCountException("Upload error: too many files!");
		}

		if (StringUtils.isNotBlank(allowedMime)) {

			final Set<String> mimes = new HashSet<>();

			for (final String mime : allowedMime.split(",")) {

				ToolboxAssert.isTrue(StringUtils.isNotBlank(mime));
				mimes.add(mime.trim().toLowerCase());

			}

			for (final MultipartFile mf : request.getFileMap().values()) {

				if (!mimes.contains(mf.getContentType())) {
					throw new FileStoreMimeCheckException("Upload error: invalid mime type!");
				}

			}
		}

	}

	/**
	 * {@link FileDescriptorStatus#TEMPORARY} fájlt készít
	 */
	public static FileDescriptor createZip(final Collection<File> filesToAdd, final String zipPassword, final String zipFilename, final int fileDescriptorLocationType, final int fileDescriptorSecurityType) {

		try {

			ToolboxAssert.isTrue(StringUtils.isNotBlank(zipFilename));
			ToolboxAssert.notNull(filesToAdd);
			ToolboxAssert.isTrue(!filesToAdd.isEmpty());

			final FileDescriptor tmpFileFd = ApplicationContextHelper.getBean(FileStoreService.class).createTmpFile2(zipFilename, fileDescriptorLocationType, fileDescriptorSecurityType);

			tmpFileFd.getFile().delete(); // törölni kell itt, mert létrehozásnál touch volt ráhívva, viszont a zip libnek nem tetszik így...

			final ZipFile zipFile;

			if (zipPassword != null) {
				zipFile = new ZipFile(tmpFileFd.getFile(), zipPassword.toCharArray());
			} else {
				zipFile = new ZipFile(tmpFileFd.getFile());
			}

			final ZipParameters parameters = new ZipParameters();
			parameters.setCompressionMethod(CompressionMethod.DEFLATE);
			parameters.setCompressionLevel(CompressionLevel.NORMAL);

			if (zipPassword != null) {
				parameters.setEncryptFiles(true);
				parameters.setEncryptionMethod(EncryptionMethod.AES);
				parameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
			}

			zipFile.addFiles(new ArrayList<>(filesToAdd), parameters);

			return tmpFileFd;

		} catch (final Exception e) {
			throw new ToolboxGeneralException("ZIP creation failed.", e);
		}

	}

	/**
	 * (alkönytárakat is ki lehet alakítani, ha a {@link FileDescriptor} fileName-ben "/" van megadva,
	 * jelenleg csak egy könyvtárnyi szint lehet...)
	 * {@link FileDescriptorStatus#TEMPORARY} fájlt készít
	 *
	 * @param filesToAdd
	 * @param zipPassword
	 * @param zipFilename
	 * @param fileDescriptorLocationType
	 * @param fileDescriptorSecurityType
	 * @param tryToUsePrettyNameFromMeta1
	 * 		deprecated, nem csinál most semmit, használd false-zal
	 * @return
	 */
	public static FileDescriptor createZipFd(final Collection<FileDescriptor> filesToAdd, final String zipPassword, final String zipFilename, final int fileDescriptorLocationType, final int fileDescriptorSecurityType, @SuppressWarnings("unused") @Deprecated boolean tryToUsePrettyNameFromMeta1) {

		try {

			ToolboxAssert.isTrue(StringUtils.isNotBlank(zipFilename));
			ToolboxAssert.notNull(filesToAdd);
			ToolboxAssert.isTrue(!filesToAdd.isEmpty());

			final FileDescriptor tmpFileFd = ApplicationContextHelper.getBean(FileStoreService.class).createTmpFile2(zipFilename, fileDescriptorLocationType, fileDescriptorSecurityType);

			tmpFileFd.getFile().delete(); // törölni kell itt, mert létrehozásnál touch volt ráhívva, viszont a zip libnek nem tetszik így...

			final ZipFile zipFile;

			if (zipPassword != null) {
				zipFile = new ZipFile(tmpFileFd.getFile(), zipPassword.toCharArray());
			} else {
				zipFile = new ZipFile(tmpFileFd.getFile());
			}

			final ZipParameters parameters = new ZipParameters();
			parameters.setCompressionMethod(CompressionMethod.DEFLATE);
			parameters.setCompressionLevel(CompressionLevel.NORMAL);

			if (zipPassword != null) {
				parameters.setEncryptFiles(true);
				parameters.setEncryptionMethod(EncryptionMethod.AES);
				parameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
			}

			for (final FileDescriptor fd : filesToAdd) {

				// http://www.lingala.net/zip4j/forum/index.php?topic=336.0
				// egy bug miatt a parameters.setFileNameInZip() nem megy
				// azóta sem javították látszólag

				final String fn;
				// if (tryToUsePrettyNameForMeta1) {
				//
				// if (StringUtils.isNotBlank(fd.getMeta1())) {
				// fn = fd.getMeta1() + "." + fd.getFilePath().split("\\.")[1];
				// } else {
				// fn = fd.getFilename();
				// }
				//
				// } else {
				fn = fd.getFilename();
				// }

				if (fn.contains("/")) {

					// TODO: alaposabban tesztelni ezt az ágat

					final String[] split = fn.split("/");

					parameters.setFileNameInZip(ToolboxStringUtil.convertToUltraSafe(split[0], "-") +
							"/" +
							ToolboxStringUtil.convertToUltraSafe(FilenameUtils.removeExtension(split[1]), "-") +
							"." +
							ToolboxStringUtil.convertToUltraSafe(FilenameUtils.getExtension(split[1]), "-"));

				} else {

					parameters.setFileNameInZip(ToolboxStringUtil.convertToUltraSafe(FilenameUtils.removeExtension(fn), "-") +
							"." +
							ToolboxStringUtil.convertToUltraSafe(FilenameUtils.getExtension(fn), "-"));

				}

				zipFile.addFile(fd.getFile(), parameters);

			}

			return tmpFileFd;

		} catch (final Exception e) {
			throw new ToolboxGeneralException("ZIP creation failed.", e);
		}

	}

	/**
	 * {@link ToolboxSysKeys.UserAuth#COMMON_TENANT_ID} fájlok, 
	 * {@link ToolboxSysKeys.UserAuth#SYSTEM_USERNAME} beléptetéssel, 
	 * van {@link FileStoreService#checkAccessRight(FileDescriptor, FileOperationAccessTypeIntent) ellenőrzés is...
	 * 
	 * @param fileDescriptorId
	 * @param generateAbsoluteUrl
	 * 
	 * @return
	 * 
	 * @see #generateDownloadUrlPublicCdnFolderRelaxed(int, boolean)
	 * 		új, egyszerűbb változat
	 */
	public static String generateDownloadUrlPublicCdnFolder(final int fileDescriptorId, final boolean generateAbsoluteUrl) {

		final ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();

		final Integer currentTlTenantId = JdbcRepositoryManager.getTlTenantId();

		try {

			SecurityUtil.setSystemUser();

			// if (currentTlTenantId == null) { // TODO: elvileg csak 1-es tenant-ban lehet CDN fájl, de ezt lehet, hogy felül kell vizsgálni..
			JdbcRepositoryManager.setTlTenantId(ToolboxSysKeys.UserAuth.COMMON_TENANT_ID);
			// }

			FileDescriptor fd = ApplicationContextHelper.getBean(FileStoreService.class).getFile2(fileDescriptorId, false, null, FileOperationAccessTypeIntent.READ_ONLY_INTENT);

			return generateDownloadUrlPublicCdnFolderInner(fd, generateAbsoluteUrl);

		} finally {

			if (currentTlTenantId == null) {
				JdbcRepositoryManager.clearTlTenantId();
			} else {
				JdbcRepositoryManager.setTlTenantId(currentTlTenantId);
			}

			SecurityUtil.setUser(loggedInUser);
		}
	}

	// lehet, hogy ez nem jó, a meglévő ronda, de megy
	// /**
	// * direktben a {@link FileDescriptorJdbcRepository} révén keresi ki a {@link FileDescriptor} rekordot és ezáltal tudja összeállítani az URL-t,
	// * user, tenant stb. állítás nincs benne (tehát nem léptet be itt system user-t stb.),
	// * azt azért ellenőrzi, hogy valóban {@link ToolboxSysKeys.FileDescriptorLocationType#PUBLIC_FOLDER_FOR_CDN} típusú-e...
	// *
	// * @param fileDescriptorId
	// * @param generateAbsoluteUrl
	// * @return
	// *
	// * @see FileDescriptorJdbcRepository#setTlAllowCommontTenantFallbackLookup(boolean)
	// * szükség esetén ez kellhet pluszban (try finally blokk!)
	// */
	// public static String generateDownloadUrlPublicCdnFolder(final int fileDescriptorId, final boolean generateAbsoluteUrl) {
	//
	// FileDescriptor fd = ApplicationContextHelper.getBean(FileDescriptorJdbcRepository.class).findOne(fileDescriptorId);
	// return generateDownloadUrlPublicCdnFolderInner(fd, generateAbsoluteUrl);
	//
	// }

	private static String generateDownloadUrlPublicCdnFolderInner(FileDescriptor fd, final boolean generateAbsoluteUrl) {

		ToolboxAssert.isTrue(ToolboxSysKeys.FileDescriptorLocationType.PUBLIC_FOLDER_FOR_CDN == fd.getLocationType().intValue());

		if (generateAbsoluteUrl) {
			return ApplicationContextHelper.getConfigProperty("tools.redirect-uri-host-frontend") + ApplicationContextHelper.getConfigProperty("tools.cdnrooturl") + "/" + fd.getFilePath();
		} else {
			return ToolboxHttpUtils.getCurrentHttpServletRequest().getContextPath() + ApplicationContextHelper.getConfigProperty("tools.cdnrooturl") + "/" + fd.getFilePath();
		}

	}

	public static String generateDownloadUrl(final int fileDescriptorId, final boolean forceDownload, final Integer childTypeCsiId) {

		final StringBuilder sb = new StringBuilder();

		sb.append(ToolboxHttpUtils.getCurrentHttpServletRequest().getContextPath());
		sb.append("/api/files/");
		sb.append(fileDescriptorId);

		if (forceDownload || childTypeCsiId != null) {

			sb.append("?");

			if (forceDownload) {
				sb.append("fd=true");
			}

			if (forceDownload && childTypeCsiId != null) {
				sb.append("&");
			}

			if (childTypeCsiId != null) {
				sb.append("childType=");
				sb.append(childTypeCsiId);
			}

			final FileDescriptor fileDescriptor = ApplicationContextHelper.getBean(FileStoreService.class).getFile2(fileDescriptorId, false, null, FileOperationAccessTypeIntent.READ_ONLY_INTENT);
			sb.append("&ct=");
			sb.append(fileDescriptor.getModifiedOn().getTime());
		}

		return sb.toString();
	}

	public static String generateDownloadUrl(final int fileDescriptorId, final boolean forceDownload) {
		return BrandUtil.getRedirectUriHostFrontend() + "api/files/" + fileDescriptorId + (forceDownload ? "?fd=true" : ""); // fd = force download
	}

	/**
	 * elsősorban Vaadin-hoz... ez a változat absolute URL alapú
	 *
	 * @param fileDescriptorId
	 * @return
	 * 
	 * @see JavaScript#eval(String)
	 */
	public static String generateJsDownloadScript(final int fileDescriptorId) {
		return generateJsDownloadScript(fileDescriptorId, true);
	}

	/**
	 * elsősorban Vaadin-hoz...
	 *
	 * @param fileDescriptorId
	 * @param generateAbsoluteUrl
	 * 
	 * @return
	 * 
	 * @see JavaScript#eval(String)
	 */
	public static String generateJsDownloadScript(final int fileDescriptorId, boolean generateAbsoluteUrl) {
		return "filePath = '" + FileStoreHelper.generateDownloadUrl(fileDescriptorId, generateAbsoluteUrl) + "'; var link=document.createElement('a'); link.href = filePath; link.download = filePath.substr(filePath.lastIndexOf('/') + 1); link.click();";
	}

	/**
	 * elsősorban Vaadin-hoz...
	 *
	 * @param fileDescriptorId
	 * @param generateAbsoluteUrl
	 * 
	 * @return
	 * @see JavaScript#eval(String)
	 */
	public static String generateJsDownloadScriptPubliCdnFolder(final int fileDescriptorId, boolean generateAbsoluteUrl) {
		return "filePath = '" + FileStoreHelper.generateDownloadUrlPublicCdnFolder(fileDescriptorId, generateAbsoluteUrl) + "'; var link=document.createElement('a'); link.href = filePath; link.download = filePath.substr(filePath.lastIndexOf('/') + 1); link.click();";
	}

	public static String getMimeType(final String filename) {

		String contentType = URLConnection.getFileNameMap().getContentTypeFor("file://" + filename); // Az URLConnection.getFileNameMap() csak string-ként megnézi a kiterjesztést

		if (contentType == null) {

			// pár extra (ami az URLConnection.getFileNameMap()-ben nincs meg)

			final String extension = FilenameUtils.getExtension(filename);

			if ("css".equalsIgnoreCase(extension)) {
				contentType = "text/css";
			} else if ("scss".equalsIgnoreCase(extension)) {
				contentType = "text/x-scss";
			} else if ("sql".equalsIgnoreCase(extension)) {
				contentType = "text/sql";
			}
		}

		return contentType;

	}

	public static List<FileDescriptor> saveAsTmpFiles(final MultipartHttpServletRequest request, final int fileDescriptorLocationType, final int fileDescriptorSecurityType) {
		return saveAsTmpFiles(request, fileDescriptorLocationType, fileDescriptorSecurityType, null);
	}

	public static List<FileDescriptor> saveAsTmpFiles(final MultipartHttpServletRequest request, final int fileDescriptorLocationType, final int fileDescriptorSecurityType, final Integer defaultStatus) {

		try {

			final FileStoreService fileStoreService = ApplicationContextHelper.getBean(FileStoreService.class);

			final List<FileDescriptor> retList = new ArrayList<>();

			final Iterator<String> itr = request.getFileNames();

			while (itr.hasNext()) {

				FileDescriptor tmpFile = null;

				try {

					final String uploadedFile = itr.next();
					final MultipartFile mpFile = request.getFile(uploadedFile);

					// final String mimeType = mpFile.getContentType(); // TODO: tisztázni (nem ment...)
					final String origFilename = mpFile.getOriginalFilename();

					tmpFile = fileStoreService.createTmpFile2(origFilename, fileDescriptorLocationType, fileDescriptorSecurityType, defaultStatus);

					FileStoreHelper.log.info("File upload, fileDescriptor (now created): " + tmpFile);

					// mpFile.transferTo(tmpFile.getFile()); // embedded Jetty-vel nem jó, az csak a saját TMP könyvtárába enged menteni/másolni valamiért
					FileCopyUtils.copy(mpFile.getInputStream(), Files.newOutputStream(tmpFile.getFile().toPath()));

					retList.add(tmpFile);

					FileStoreService.addToPreResizeQueueIfPossible(tmpFile);

				} catch (final Exception e) {
					if (tmpFile != null) {
						fileStoreService.setToBeDeleted(tmpFile.getId());
					}
					throw e;
				}

			}

			return retList;

		} catch (final Exception e) {

			FileStoreHelper.log.error("File upload error!", e);
			throw new ToolboxGeneralException("Upload error!", e);

		}

	}

	private FileStoreHelper() {
		//
	}

	public static boolean isJpgOrPng(final String origFileExt) {
		return "jpg".equalsIgnoreCase(origFileExt) || "jpeg".equalsIgnoreCase(origFileExt) || "png".equalsIgnoreCase(origFileExt);
	}

	public static boolean isPdf(final String origFileExt) {
		return "pdf".equalsIgnoreCase(origFileExt);
	}

	/**
	 * @param fileIdsList 
	 * 		egy vagy több String, ahol egy-egy String egy-egy JsonArray (ahogyan a DB is tárolni szoktuk) 
	 * 		(értsd: több model objektum fájljait együtt is meg lehet jeleníteni)
	 * @return
	 */
	public static List<FileDescriptor> toFileDescriptorList(FileOperationAccessTypeIntent fileOperationAccessTypeIntent, final String... fileIdsList) {

		final List<Integer> fileIdList = new ArrayList<>();

		for (final String fileIds : fileIdsList) {

			final JSONArray jsonArray = new JSONArray(fileIds);

			for (int i = 0; i < jsonArray.length(); i++) {
				final int fileDescriptorId = jsonArray.getInt(i);

				fileIdList.add(fileDescriptorId);

			}
		}

		// ---

		final FileStoreService fileStoreService = ApplicationContextHelper.getBean(FileStoreService.class);
		final List<FileDescriptor> fileDescriptorList = new ArrayList<>();

		for (final Integer fileDescriptorId : fileIdList) {

			// TODO: sok elemre nem hatékony így

			final FileDescriptor fd = fileStoreService.getFile2(fileDescriptorId, fileOperationAccessTypeIntent);
			fileDescriptorList.add(fd);
		}

		return fileDescriptorList;
	}

}
