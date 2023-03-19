package hu.lanoga.toolbox.file;

import com.google.common.util.concurrent.Striped;
import com.teamunify.i18n.I;
import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.FileOperationAccessTypeIntent;
import hu.lanoga.toolbox.ToolboxSysKeys.JdbcInsertConflictMode;
import hu.lanoga.toolbox.ToolboxSysKeys.JmsDestinationMode;
import hu.lanoga.toolbox.amazon.s3.AmazonS3Manager;
import hu.lanoga.toolbox.exception.FileLockException;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.file.remote.RemoteFile;
import hu.lanoga.toolbox.file.remote.RemoteFileJdbcRepository;
import hu.lanoga.toolbox.file.remote.RemoteFileService;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.JmsManager;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.util.ToolboxAssert;
import hu.lanoga.toolbox.util.ToolboxStringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;

/**
 * @see FileStoreHelper
 */
@Slf4j
@ConditionalOnMissingBean(name = "fileStoreServiceOverrideBean")
@Transactional
@Service
public class FileStoreService {

	private static final ConcurrentLinkedQueue<FileDescriptor> preResizeQueue2048ProgrJpg = new ConcurrentLinkedQueue<>();

	/**
	 * @return
	 * 		null, ha üres (nincs mit feldolgozni most épp)
	 */
	static FileDescriptor pollPreResizeQueue() {
		return preResizeQueue2048ProgrJpg.poll();
	}

	static void addToPreResizeQueueIfPossible(final FileDescriptor fileDescriptor) {

		final boolean isJpgOrPng = FileStoreHelper.isJpgOrPng(FilenameUtils.getExtension(fileDescriptor.getFilename()));

		if (isJpgOrPng) {
			preResizeQueue2048ProgrJpg.add(fileDescriptor);
		}

	}

	private static final Striped<Lock> createChildVariantStripedLock = Striped.lazyWeakLock(50);

	protected void checkAccessRight(final FileDescriptor fileDescriptor) {

		// ez a FileOperationAccessTypeIntent később került be, visszafele komp. miatt az egy paramos checkAccessRight() is megmaradt
		// itt fontos, hogy a legszűkebb ("legkomolyabb") FileOperationAccessTypeIntent legyen!

		checkAccessRight(fileDescriptor, FileOperationAccessTypeIntent.CHANGE_INTENT);
	}

	protected void checkAccessRight(final FileDescriptor fileDescriptor, final FileOperationAccessTypeIntent fileOperationAccessTypeIntent) {

		ToolboxAssert.notNull(fileOperationAccessTypeIntent);

		if (ToolboxSysKeys.FileDescriptorSecurityType.ADMIN_OR_CREATOR == fileDescriptor.getSecurityType()) {
			SecurityUtil.limitAccessAdminOrOwner(fileDescriptor.getCreatedBy());
		} else if (ToolboxSysKeys.FileDescriptorSecurityType.AUTHENTICATED_USER == fileDescriptor.getSecurityType()) {
			SecurityUtil.limitAccessRoleUserOrOwner(fileDescriptor.getCreatedBy());
		} else if (ToolboxSysKeys.FileDescriptorSecurityType.READ_AUTHENTICATED_USER_MODIFY_ADMIN_OR_CREATOR == fileDescriptor.getSecurityType()) {

			if (FileOperationAccessTypeIntent.READ_ONLY_INTENT.equals(fileOperationAccessTypeIntent)
					|| fileDescriptor.getParentId() != null // a child fájlokra lazább szabályozás vontakozik
			) {
				SecurityUtil.limitAccess(ToolboxSysKeys.UserAuth.ROLE_USER_STR);
			} else {
				SecurityUtil.limitAccessAdminOrOwner(fileDescriptor.getCreatedBy());
			}

		} else if (ToolboxSysKeys.FileDescriptorSecurityType.SUPER_ADMIN_ONLY == fileDescriptor.getSecurityType()) {
			SecurityUtil.limitAccessSuperAdmin();
		} else if (ToolboxSysKeys.FileDescriptorSecurityType.SYSTEM_ONLY == fileDescriptor.getSecurityType()) {
			SecurityUtil.limitAccessSystem();
		} else {
			throw new AccessDeniedException("Unsupported FileDescriptorSecurityType!");
		}

	}
	
	/**
	 * @param fileDescriptor
	 * @param fileOperationAccessTypeIntent
	 * @return
	 * 		true, ha engedett
	 */
	public boolean preCheckAccessRight(final FileDescriptor fileDescriptor, final FileOperationAccessTypeIntent fileOperationAccessTypeIntent) {

		try {
			
			this.checkAccessRight(fileDescriptor, fileOperationAccessTypeIntent);
			return true;
			
		} catch (AccessDeniedException e) {
			//
		}
		
		return false;
		

	}

	protected void checkFileLock(final FileDescriptor fileDescriptor) throws FileLockException {
		if (this.hasFileLock(fileDescriptor)) {
			throw new FileLockException("Locked!");
		}
	}

	/**
	 * ha a lock aktív, akkor ezzel a metódussal letudjuk ellenőrizni, hogy mi a helyzet
	 *
	 * @param fileDescriptor
	 * @return
	 * 		false, ha szerkeszthető a fájl (nincs lock, lejárt a lock, vagy a belépett user a lock "tulajdonosa", vagy ROLE_ADMIN-nak minden)
	 */
	protected boolean hasFileLock(final FileDescriptor fileDescriptor) {

		{

			if (fileDescriptor.getLockedBy() == null) {
				return false;
			}

			final ToolboxUserDetails loggedInUser = SecurityUtil.getLoggedInUser();

			if (loggedInUser == null) {
				return true;
			}

			final boolean isLockedByTheLoggedInUser = fileDescriptor.getLockedBy().equals(loggedInUser.getId());
			final boolean isAdmin = SecurityUtil.hasAdminRole();

			if (isLockedByTheLoggedInUser || isAdmin) {
				return false;
			}

		}

		// ---

		ToolboxAssert.notNull(fileDescriptor.getLockedOn());

		final Timestamp startTime;

		if (fileDescriptor.getModifiedOn().after(fileDescriptor.getLockedOn())) {
			startTime = fileDescriptor.getModifiedOn();
		} else {
			startTime = fileDescriptor.getLockedOn();
		}

		// ---

		final Calendar c = Calendar.getInstance();
		c.setTimeInMillis(startTime.getTime());
		c.add(Calendar.MINUTE, this.lockDuration);

		final Timestamp currentTime = new Timestamp(System.currentTimeMillis());

		final Timestamp oneHourLaterFromModifiedOn = new Timestamp(c.getTimeInMillis());

		return !oneHourLaterFromModifiedOn.before(currentTime);

	}

	@Value("${tools.remote.remote-provider-type:-1}")
	private int remoteProviderType;

	@Value("${tools.file.remote.download-missing-files.enabled:false}")
	private boolean downloadMissingFileProperty;

	@Value("${tools.file.lock-duration}")
	private int lockDuration;

	@Autowired
	private FileDescriptorJdbcRepository fileDescriptorJdbcRepository;

	@Autowired
	private DiscFileStoreRepository discFileStoreRepository;

	@Autowired
	private RemoteFileJdbcRepository remoteFileJdbcRepository;

	@Autowired
	private RemoteFileService remoteFileService;

	@Autowired
	private AmazonS3Manager amazonS3Manager;

	@Autowired
	private PictureHelper pictureHelper;

	@Value("${tools.file.disk.storedir.limit}")
	private Integer fileDiskStoredirLimit;

	@Value("${tools.file.disk.cdndir.limit}")
	private Integer fileDiskCdndirLimit;
	
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	public void cleanUp() {
		cleanUp(7);
	}

	/**
	 * @see FileDescriptorJdbcRepository#findToBeDeleted()
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	public void cleanUp(int daysSinceMod) {

		FileStoreService.log.debug("FileStoreService cleanUp...");

		// this.fileDescriptorJdbcRepository.lockTable(); // nem lényeges itt, mert a TMP fájlokat is csak x nap után töröljük, elenyésző az esélye, hogy pont most (a findToBeDeleted()) hívás után legyenek NORMAL status-ra állítva

		final List<FileDescriptor> fileDescriptors = this.fileDescriptorJdbcRepository.findToBeDeleted(daysSinceMod);

		for (final FileDescriptor fileDescriptor : fileDescriptors) {

			try {

				FileStoreService.log.debug("FileService delete file, url: " + fileDescriptor.getFilePath() + ", locationType:" + fileDescriptor.getLocationType());
				this.discFileStoreRepository.deleteFile(fileDescriptor.getFilePath(), fileDescriptor.getLocationType());

				FileStoreService.log.debug("FileService delete record: " + fileDescriptor.getId());

				fileDescriptor.setStatus(ToolboxSysKeys.FileDescriptorStatus.DELETED);
				this.fileDescriptorJdbcRepository.save(fileDescriptor);

			} catch (final Exception e) {
				FileStoreService.log.warn("File delete error!", e);
			}

		}

		FileStoreService.log.debug("FileStoreService cleanUp finished.");

	}

	/**
	 * összes tenantra egyszerre...
	 */
	public void manageArchive() {

		FileStoreService.log.debug("FileStoreService manageArchive...");

		SecurityUtil.limitAccessSystem();

		// ---

		{

			final BigDecimal totalFileSize = this.fileDescriptorJdbcRepository.findTotalFileSize2();
			final BigDecimal totalFileSizeInMb = totalFileSize.divide(BigDecimal.valueOf(1048576), BigDecimal.ROUND_HALF_UP);

			if (this.fileDiskCdndirLimit < totalFileSizeInMb.intValue()) {
				log.warn("File disk tools.file.disk.cdndir.limit exceeded!");
			}

		}

		// ---

		{

			BigDecimal totalFileSize = this.fileDescriptorJdbcRepository.findTotalFileSize1();
			BigDecimal totalFileSizeInMb = totalFileSize.divide(BigDecimal.valueOf(1048576), BigDecimal.ROUND_CEILING);

			// először csak le ellenőrizzük, hogy kell-e lefutnia a törlésnek, mert túlléptük a megengedett limit-et

			if (this.fileDiskStoredirLimit < totalFileSizeInMb.intValue()) {

				final int thresholdSiteInMb = (int) (this.fileDiskStoredirLimit * 0.8d);

				externalWhileLabel: while (true) {

					final List<FileDescriptor> list = this.fileDescriptorJdbcRepository.findForManageArchive();

					if (!list.isEmpty()) {

						for (final FileDescriptor fileDescriptor : list) {

							// ha túllépi a fájlok mérete a threshold limit-et, akkor addig töröljük a legrégebbi fájlokat, amíg alá nem megyünk méretben

							if (thresholdSiteInMb < totalFileSizeInMb.intValue()) {

								try {

									// FileStoreService.log.debug("FileService manageArchive delete file: " + fileDescriptor.getFilePath() + ", locationType:" + fileDescriptor.getLocationType());

									this.discFileStoreRepository.deleteFile(fileDescriptor.getFilePath(), fileDescriptor.getLocationType());

									FileStoreService.log.debug("FileService manageArchive record archived: " + fileDescriptor.getId());

									fileDescriptor.setRemoteOnly(Boolean.TRUE);

									try {
										JdbcRepositoryManager.setTlTenantId(fileDescriptor.getTenantId());
										this.fileDescriptorJdbcRepository.save(fileDescriptor);
									} finally {
										JdbcRepositoryManager.clearTlTenantId();
									}

									totalFileSize = totalFileSize.subtract(BigDecimal.valueOf(fileDescriptor.getFileSize()));
									totalFileSizeInMb = totalFileSize.divide(BigDecimal.valueOf(1048576), BigDecimal.ROUND_CEILING);

								} catch (final Exception e) {
									FileStoreService.log.warn("File delete error!", e);
								}

							} else {
								FileStoreService.log.info("FileStoreService manageArchive disk space freeing successful.");
								break externalWhileLabel;
							}

						}

					} else {
						FileStoreService.log.info("FileStoreService manageArchive there's no more file to archive");
						break;
					}

				}

			}

		}

		FileStoreService.log.debug("FileStoreService manageArchive finished.");

	}

	/**
	 * arra használható, hogy a disk-en is (ahova érdemben a bináris kerül) legyen némi alkönyvtár struktúra
	 */
	public static ThreadLocal<String> tlFilePathPrefix = new ThreadLocal<>();

	protected String getFilePathPrefix() {

		final String s = tlFilePathPrefix.get();
		return (s != null) ? s : "";

	}

	/**
	 * tesztekhez stb.
	 * 
	 * @param locationPattern
	 * 		a source, a {@link PathMatchingResourcePatternResolver} paraméterea
	 * @param filename
	 * 		a TMP {@link FileDescriptor}-hoz
	 * @param fileDescriptorLocationType
	 * 		a TMP {@link FileDescriptor}-hoz
	 * @param fileDescriptorSecurityType
	 * 		a TMP {@link FileDescriptor}-hoz
	 * @return
	 */
	public FileDescriptor pathMatchingResourcePatternResolverFileToTmpFileDescriptor(final String locationPattern, final String filename, final int fileDescriptorLocationType, final int fileDescriptorSecurityType) {

		try {

			final FileDescriptor fileDescriptor = this.createTmpFile2(filename, fileDescriptorLocationType, fileDescriptorSecurityType);

			final Resource sourceResource = new PathMatchingResourcePatternResolver().getResources(locationPattern)[0];

			try (InputStream is = sourceResource.getInputStream()) {
				FileUtils.copyInputStreamToFile(is, fileDescriptor.getFile());
			}

			return fileDescriptor;

		} catch (Exception e) {
			throw new ToolboxGeneralException("pathMatchingResourcePatternResolverFileToTmpFileDescriptor error!", e);
		}

	}
	
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR)
	public FileDescriptor createTmpFile2() {
		return createTmpFile2(UUID.randomUUID().toString() + ".dat", ToolboxSysKeys.FileDescriptorLocationType.PROTECTED_FOLDER, ToolboxSysKeys.FileDescriptorSecurityType.ADMIN_OR_CREATOR, null);
	}

	/**
	 * @param filename
	 * 		{@link FileDescriptor#setFilename(String)}-hez
	 * @return
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR)
	public FileDescriptor createTmpFile2(final String filename) {
		return createTmpFile2(filename, ToolboxSysKeys.FileDescriptorLocationType.PROTECTED_FOLDER, ToolboxSysKeys.FileDescriptorSecurityType.ADMIN_OR_CREATOR, null);
	}
	
	/**
	 * @param filename
	 * 		{@link FileDescriptor#setFilename(String)}-hez, 
	 * 		ez az eredeti (pl. feltöltésnél lévő) fájlnév (ez Java kódban megnyitásnál nem jó, nem ilyen néven van tárolva a lemezen!)
	 * @param fileDescriptorLocationType
	 * 		{@link ToolboxSysKeys.FileDescriptorLocationType}
	 * @param fileDescriptorSecurityType
	 * 		{@link ToolboxSysKeys.FileDescriptorSecurityType}
	 * 
	 * @return
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR)
	public FileDescriptor createTmpFile2(final String filename, final int fileDescriptorLocationType, final int fileDescriptorSecurityType) {
		return createTmpFile2(filename, fileDescriptorLocationType, fileDescriptorSecurityType, null);
	}

	/**
	 * @param filename
	 * 		{@link FileDescriptor#setFilename(String)}-hez, 
	 * 		ez az eredeti (pl. feltöltésnél lévő) fájlnév (ez Java kódban megnyitásnál nem jó, nem ilyen néven van tárolva a lemezen!)
	 * @param fileDescriptorLocationType
	 * 		{@link ToolboxSysKeys.FileDescriptorLocationType}
	 * @param fileDescriptorSecurityType
	 * 		{@link ToolboxSysKeys.FileDescriptorSecurityType}
	 * @param defaultStatus
	 * 		{@link ToolboxSysKeys.FileDescriptorStatus}
	 * 
	 * @return
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR)
	public FileDescriptor createTmpFile2(final String filename, final int fileDescriptorLocationType, final int fileDescriptorSecurityType, final Integer defaultStatus) {

		if (ToolboxSysKeys.FileDescriptorLocationType.PUBLIC_FOLDER_FOR_CDN == fileDescriptorLocationType) {
			SecurityUtil.limitAccessAdmin();
		}

		final UUID uuid = UUID.randomUUID();

		FileDescriptor fileDescriptor = new FileDescriptor();

		fileDescriptor.setGid(uuid);
		fileDescriptor.setLocationType(fileDescriptorLocationType);
		fileDescriptor.setSecurityType(fileDescriptorSecurityType);

		if (defaultStatus == null) {
			fileDescriptor.setStatus(ToolboxSysKeys.FileDescriptorStatus.TEMPORARY);
		} else {
			fileDescriptor.setStatus(defaultStatus);
		}

		final StringBuilder sbFilePath = new StringBuilder(this.getFilePathPrefix());

		String ext = null;

		if (StringUtils.isNotBlank(filename)) {

			fileDescriptor.setFilename(StringUtils.abbreviate(filename, 80));
			fileDescriptor.setMimeType(FileStoreHelper.getMimeType(filename));

			sbFilePath.append(ToolboxStringUtil.convertToUltraSafe(FilenameUtils.getBaseName(fileDescriptor.getFilename()), "-"));

			ext = ToolboxStringUtil.convertToUltraSafe(FilenameUtils.getExtension(filename), "");

		}

		if (StringUtils.isBlank(filename)) {
			sbFilePath.append("f");
		}

		if (StringUtils.isBlank(ext)) {
			ext = "dat";
		}

		sbFilePath.append("-");
		sbFilePath.append(uuid.toString());
		sbFilePath.append(".");
		sbFilePath.append(ext);
		
		fileDescriptor.setFilePath(sbFilePath.toString());

		fileDescriptor = this.fileDescriptorJdbcRepository.save(fileDescriptor);

		final File file = this.discFileStoreRepository.getFile(fileDescriptor.getFilePath(), fileDescriptorLocationType);

		// ---

		this.discFileStoreRepository.makeParentDirAndTouch(file);

		// ---

		fileDescriptor.setFile(file);

		return fileDescriptor;
	}
	
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR)
	public FileDescriptor getFile2(final int fileDescriptorId, FileOperationAccessTypeIntent fileOperationAccessTypeIntent) {
		return this.getFile2(fileDescriptorId, true, null, fileOperationAccessTypeIntent);
	}

	/**
	 * @param fileDescriptorId
	 * @return
	 * 
	 * @see #getFile2(int, boolean, Integer, FileOperationAccessTypeIntent)
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR)
	public FileDescriptor getFile2(final int fileDescriptorId) {
		return this.getFile2(fileDescriptorId, true, null, FileOperationAccessTypeIntent.CHANGE_INTENT); // fontos, hogy a default a CHANGE_INTENT legyen
	}

	/**
	 * @param fileDescriptorId
	 * @param downloadMissingOrArchivedFile
	 * @return
	 * 
	 * @see #getFile2(int, boolean, Integer, FileOperationAccessTypeIntent)
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR)
	public FileDescriptor getFile2(final int fileDescriptorId, final boolean downloadMissingOrArchivedFile) {
		return this.getFile2(fileDescriptorId, downloadMissingOrArchivedFile, null, FileOperationAccessTypeIntent.CHANGE_INTENT);  // fontos, hogy a default a CHANGE_INTENT legyen
	}

	/**
	 * @param fileDescriptorId
	 * @param downloadMissingOrArchivedFile
	 * @param childTypeCsiId
	 * @return
	 * 
	 * @see #getFile2(int, boolean, Integer, FileOperationAccessTypeIntent)
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR)
	public FileDescriptor getFile2(final int fileDescriptorId, final boolean downloadMissingOrArchivedFile, final Integer childTypeCsiId) {
		return this.getFile2(fileDescriptorId, downloadMissingOrArchivedFile, childTypeCsiId, FileOperationAccessTypeIntent.CHANGE_INTENT);  // fontos, hogy a default a CHANGE_INTENT legyen
	}

	/**
	 * jogosultságot is ellenőriz (legacy okból getFile2 a neve, nincs is már getFile1)
	 *
	 * @param fileDescriptorId
	 * @param downloadMissingOrArchivedFile 
	 * 		archivált vagy barbár törölt fájl visszatöltés (szükség esetén) remote (Amazon stb.) szerverről 
	 * 		(plusz properties beállítások is számítanak, ott is engedélyezni kell pár dolgot...)
	 * @param childTypeCsiId
	 * 		lehet null is
	 * @param fileOperationAccessTypeIntent
	 * 		
	 *  
	 * @return
	 * 
	 * @see ToolboxSysKeys.FileDescriptorChildType
	 * @see ToolboxSysKeys.FileOperationAccessTypeIntent
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR)
	public FileDescriptor getFile2(final int fileDescriptorId, final boolean downloadMissingOrArchivedFile, final Integer childTypeCsiId, final FileOperationAccessTypeIntent fileOperationAccessTypeIntent) {

		final FileDescriptor fileDescriptor = this.fileDescriptorJdbcRepository.findOne(fileDescriptorId);
		this.checkAccessRight(fileDescriptor, fileOperationAccessTypeIntent);

		// log.debug("TransactionHelper: " + TransactionHelper.getCurrentTransactionStatus());

		// ---

		if (childTypeCsiId == null) {

			// archive vagy barbár törölt fájl visszatöltés (szükség esetén)
			// ha csak remote a file (csak Amazon, Google Drive stb. van meg) és újra le kell tölteni VAGY a file nem létezik és engedélyezve van a hiányzó (barbár törölt) fájlok letöltése property

			final File file = this.discFileStoreRepository.getFile(fileDescriptor.getFilePath(), fileDescriptor.getLocationType());

			if (downloadMissingOrArchivedFile && (Boolean.TRUE.equals(fileDescriptor.getRemoteOnly()) || (!file.exists() && this.downloadMissingFileProperty))) {

				final RemoteFile remoteFile = this.remoteFileService.findByFileDescriptorIdWithoutAccessRightCheck(fileDescriptorId);

				if (remoteFile == null) {
					throw new ToolboxGeneralException("Missing RemoteFile for fileDescriptorId (cannot restore file): " + fileDescriptorId);
				}

				if (remoteFile.getRemoteProviderType().equals(ToolboxSysKeys.RemoteProviderType.AMAZON_S3)) {

					// TODO: ilyenkor jobb lenne egy ketté választott input stream...
					// tehát az amazon letöltés rögtön "folyna" a klienshez is (ha FileStoreController-ből jött a hívás) és a szerveres fájlba is
					// viszont látszólag az Amazon S3 nem tud ilyet, nincs stream-es változat... lásd amazonS3Manager.download
					// (nem égető)

					log.debug("get back archived file (amazon s3)... fileDescriptorId: " + fileDescriptorId + ", " + remoteFile);

					this.discFileStoreRepository.makeParentDirAndTouch(file);

					this.amazonS3Manager.download(remoteFile.getRemoteId(), file);

					fileDescriptor.setFilePath(file.getName());
					fileDescriptor.setStatus(ToolboxSysKeys.FileDescriptorStatus.NORMAL);
					fileDescriptor.setRemoteOnly(false);

					this.fileDescriptorJdbcRepository.save(fileDescriptor); // itt nem jó a service-ban lévő setToNormal, direktben a repo-t kell hívni

				} else {
					throw new ToolboxGeneralException("Not supported RemoteProviderType!");
				}
			}

			fileDescriptor.setFile(file);

		} else {

			// változatok készítése, pl. kisebb képméret, más formátum

			FileDescriptor childFileDescriptor = this.findChild(fileDescriptorId, childTypeCsiId);

			if (childFileDescriptor == null) {

				final Lock lock = createChildVariantStripedLock.get(fileDescriptorId + "-" + childTypeCsiId);

				try {

					final boolean tryLock = lock.tryLock(); // true, ha magkapta a lock-ot

					if (!tryLock) {

						// ha nem tudta megkapni rögtön a lock-ot, akkor várunk, amíg szabad lesz

						lock.lock();

						// ha ilyen várás volt, akkor az azt jelenti 99%-ban, hogy épp most csinálta meg a szükséges kisképet egy másik szál
						// miért kell az egész metódust újrakezdeni? mert a lehetséges (pl. cactus) magas tranzakció izolációs szint miatt új tranzakció kell... (ha régi/várakozó tranzackció olvasna újra, akkor az nem látná a friss módosításokat db-ben)

						// TransactionHelper.doInTransaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW, new Runnable() {
						//
						// @Override
						// public void run() {
						// FileStoreService.this.getFile2(fileDescriptorId, downloadMissingOrArchivedFile, childTypeCsiId, fileOperationAccessTypeIntent); // TODO: lehet, hogy intent is kell
						// }
						// });

						return ApplicationContextHelper.getBean(FileScheduler.class).getFile2(fileDescriptorId, downloadMissingOrArchivedFile, childTypeCsiId, fileOperationAccessTypeIntent); 
					}

					final boolean isJpgOrPng = FileStoreHelper.isJpgOrPng(FilenameUtils.getExtension(fileDescriptor.getFilename()));

					if (childTypeCsiId == ToolboxSysKeys.FileDescriptorChildType.VARIANT_2048_2048_PROGR_JPG && isJpgOrPng) {
						childFileDescriptor = this.createChildVariantJpgMagicConvert(fileDescriptor, childTypeCsiId, 2048, 2048);
					} else if (childTypeCsiId == ToolboxSysKeys.FileDescriptorChildType.VARIANT_3096_3096_JPG && isJpgOrPng) {
						childFileDescriptor = this.createChildVariantJpgMagicConvert(fileDescriptor, childTypeCsiId, 3072, 3072);
					} else {
						childFileDescriptor = fileDescriptor; // fallback, akkor, ha olyan változatot kért, amire nincs implementáció még... problémás kicsit, mert így minden alkalommal újrapróbálja, ami lassú lehet... de elvileg ilyen nem lehet szinte soha
					}

					// TODO: a PDF-et még megcsinálni (nem égető!)

					if (fileDescriptor.getMeta1() != null) {
						this.saveMeta1(childFileDescriptor.getId(), fileDescriptor.getMeta1());
					}

				} finally {
					lock.unlock();
				}

			}

			return this.getFile2(childFileDescriptor.getId(), downloadMissingOrArchivedFile, null, fileOperationAccessTypeIntent);

		}

		// ---

		return fileDescriptor;
	}

	public void disownChildVariants(final int parentFileDescriptorId) {

		final List<FileDescriptor> childFileDescriptors = this.fileDescriptorJdbcRepository.findAllBy("parentId", parentFileDescriptorId);
		for (final FileDescriptor childFileDescriptor : childFileDescriptors) {

			childFileDescriptor.setParentId(null);
			this.fileDescriptorJdbcRepository.save(childFileDescriptor);

			this.setToBeDeleted(childFileDescriptor.getId());
		}

	}

	private FileDescriptor findChild(final int parentFileDescriptorId, final int childTypeCsiId) {

		final List<FileDescriptor> childFileDescriptors = this.fileDescriptorJdbcRepository.findAllBy("parentId", parentFileDescriptorId, "childType", childTypeCsiId);

		if (childFileDescriptors.isEmpty()) {
			return null;
		}

		return childFileDescriptors.get(0);
	}

	private FileDescriptor createChildVariantJpgMagicConvert(final FileDescriptor parentFileDescriptor, final Integer childTypeCsiId, final int width, final int height) {

		// TODO: ha állítani kell a jpgQuality is, akkor még kell egy plusz paraméter a metódusba (mert jelenleg fixen 0.55f használunk)

		final Pair<FileDescriptor, FileDescriptor> fileDescriptorFileDescriptorPair = this.pictureHelper.magicConvert(parentFileDescriptor, parentFileDescriptor.getMimeType(), parentFileDescriptor.getMimeType(), new Dimension(width, height), false, null, 0.55f);

		final FileDescriptor variantFileDescriptor = fileDescriptorFileDescriptorPair.getLeft();
		variantFileDescriptor.setParentId(parentFileDescriptor.getId());
		variantFileDescriptor.setChildType(childTypeCsiId);

		String fileNameForTheVariantFile = FilenameUtils.removeExtension(parentFileDescriptor.getFilename());
		fileNameForTheVariantFile = fileNameForTheVariantFile + ".jpg";
		variantFileDescriptor.setFilename(fileNameForTheVariantFile);

		FileDescriptor savedFd = this.fileDescriptorJdbcRepository.save(variantFileDescriptor);

		// ---

		// azért fontos itt mégegyszer megnézni a status-át, mert a) sokáig tarthat magicConvert (értsd pár sec) b) közben mi van, ha parentFileDescriptor objektumunk "stale" és valójában már törlendő stb. a fájl
		// ezzel az új status lekéréssel minimalizáljuk azt, hogy a child normal, a parent pedig törlendő lesz (plusz lásd a FileScheduler-es cleanUp())

		final FileDescriptor parentFileDescriptorAsItIsInTheDbInAtThisTime = this.fileDescriptorJdbcRepository.findOne(parentFileDescriptor.getId());

		if (parentFileDescriptorAsItIsInTheDbInAtThisTime != null && parentFileDescriptorAsItIsInTheDbInAtThisTime.getStatus().equals(ToolboxSysKeys.FileDescriptorStatus.NORMAL)) {
			savedFd = this.setToNormal(savedFd.getId()); // egyébként TMP-n hagyjuk
		}

		return savedFd;

	}

	/**
	 * fájl megjeleölése törlendőnek (job fogja érdemben törölni)
	 *
	 * @param fileDescriptorId
	 * @return
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public FileDescriptor setToBeDeleted(final int fileDescriptorId) {
		return setToBeDeletedInner(fileDescriptorId, true);
	}

	/**
	 * Óvatosan! Nincs semmilyen security check itt!
	 * 
	 * @param fileDescriptorId
	 * @param doFileAccessChecks
	 * 		method ROLE check true esetén sincs
	 * @return
	 * 
	 * @see #setToBeDeleted(int)
	 */
	public FileDescriptor setToBeDeletedInner(final int fileDescriptorId, boolean doFileAccessChecks) {
				
		final FileDescriptor fileDescriptor = this.fileDescriptorJdbcRepository.findOne(fileDescriptorId);

		if (doFileAccessChecks) {
			this.checkAccessRight(fileDescriptor);
			this.checkFileLock(fileDescriptor);
		}
		
		fileDescriptor.setStatus(ToolboxSysKeys.FileDescriptorStatus.TO_BE_DELETED);
		final FileDescriptor retVal = this.fileDescriptorJdbcRepository.save(fileDescriptor);

		// ---

		final List<FileDescriptor> childFileDescriptors = this.fileDescriptorJdbcRepository.findAllBy("parentId", fileDescriptorId);
		for (final FileDescriptor childFileDescriptor : childFileDescriptors) {
			this.setToBeDeletedInner(childFileDescriptor.getId(), doFileAccessChecks);
		}

		// ---

		return retVal;
	}

	/**
	 * TEMP fájl átállítása normálra (=maradandó) (ha nem TEMP már, akkor nem csinál ez a metódus semmit)
	 *
	 * @param fileDescriptorId
	 * @return
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public FileDescriptor setToNormal(final int fileDescriptorId) {
		return this.setToNormal(fileDescriptorId, ToolboxSysKeys.RemoteProviderPriority.MEDIUM, false);
	}

	@Deprecated

	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public FileDescriptor setToNormal2(final int fileDescriptorId) {
		return this.setToNormal(fileDescriptorId);
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public /* final */ FileDescriptor setToNormal(final int fileDescriptorId, Integer priority, final boolean requestAnyOneWithLink) { // nem lehet final-ra tenni, Spring-nek nem tetszik (null az Autowired mezőknél...)

		final FileDescriptor fileDescriptorFromDb = this.fileDescriptorJdbcRepository.findOne(fileDescriptorId); // mégegyszer lekérjük, hátha azóta változott stb.

		this.checkAccessRight(fileDescriptorFromDb);
		this.checkFileLock(fileDescriptorFromDb);

		if (!fileDescriptorFromDb.getStatus().equals(ToolboxSysKeys.FileDescriptorStatus.TEMPORARY)) {
			return fileDescriptorFromDb;
		}

		// ---

		fileDescriptorFromDb.setStatus(ToolboxSysKeys.FileDescriptorStatus.NORMAL);

		if (StringUtils.isBlank(fileDescriptorFromDb.getHashValue()) || fileDescriptorFromDb.getFileSize() == null) {

			final File file = this.discFileStoreRepository.getFile(fileDescriptorFromDb.getFilePath(), fileDescriptorFromDb.getLocationType());
			final String hashValue = this.discFileStoreRepository.generateHashFromFile(file);

			fileDescriptorFromDb.setHashValue(hashValue);
			fileDescriptorFromDb.setFileSize(file.length());

		}

		if (this.remoteProviderType > -1) {

			final RemoteFile remoteFile = new RemoteFile();
			remoteFile.setRemoteProviderType(this.remoteProviderType);

			if (priority == null) {
				priority = ToolboxSysKeys.RemoteProviderPriority.MEDIUM;
			}
			remoteFile.setPriority(priority);

			remoteFile.setFileId(fileDescriptorFromDb.getId());
			remoteFile.setRemoteId(null);
			remoteFile.setStatus(ToolboxSysKeys.RemoteProviderStatus.PENDING);
			remoteFile.setAnyoneWithLinkRequested(requestAnyOneWithLink);

			// this.remoteFileService.save(remoteFile);
			this.remoteFileJdbcRepository.insert(remoteFile, null, JdbcInsertConflictMode.ON_CONFLICT_DO_NOTHING, "file_id", null);

		}

		this.fileDescriptorJdbcRepository.save(fileDescriptorFromDb);

		// ---

		final List<FileDescriptor> childFileDescriptors = this.fileDescriptorJdbcRepository.findAllBy("parentId", fileDescriptorId);
		for (final FileDescriptor childFileDescriptor : childFileDescriptors) {
			this.setToNormal(childFileDescriptor.getId(), ToolboxSysKeys.RemoteProviderPriority.LOW, false);
		}

		// ---

		return getFile2(fileDescriptorId);
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public /* final */ FileDescriptor setBackToTmpFile(final int fileDescriptorId) {

		final FileDescriptor fileDescriptorFromDb = this.fileDescriptorJdbcRepository.findOne(fileDescriptorId); // mégegyszer lekérjük, hátha azóta változott stb.

		this.checkAccessRight(fileDescriptorFromDb);
		this.checkFileLock(fileDescriptorFromDb);

		fileDescriptorFromDb.setStatus(ToolboxSysKeys.FileDescriptorStatus.TEMPORARY);

		final FileDescriptor retVal = this.fileDescriptorJdbcRepository.save(fileDescriptorFromDb);

		// ---

		final List<FileDescriptor> childFileDescriptors = this.fileDescriptorJdbcRepository.findAllBy("parentId", fileDescriptorId);
		for (final FileDescriptor childFileDescriptor : childFileDescriptors) {
			this.setBackToTmpFile(childFileDescriptor.getId());
		}

		// ---

		return retVal;

	}

	/**
	 * @param jsonStr
	 * @see #setToNormal(int)
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public void setToNormal(final String jsonStr) {

		if (StringUtils.isBlank(jsonStr)) {
			return;
		}

		final JSONArray jsonArray = new JSONArray(jsonStr);

		jsonArray.forEach(x -> {
			this.setToNormal(Integer.parseInt(x.toString()));
		});

	}

	/**
	 * akkor jó, ha a {@link #setToNormalOrDelete(String, String)} nem alkalmazható, ilyenkor átmenileg tmp fájl statuszra állítunk
	 * majd kicsit később a megmaradókat {@link #setToNormal(int)}-lal újra normal-ra (értsd tranzakción belül még)
	 *
	 * @param jsonStr
	 * @see #setBackToTmpFile(int)
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public void setBackToTmpFile(final String jsonStr) {

		if (StringUtils.isBlank(jsonStr)) {
			return;
		}

		final JSONArray jsonArray = new JSONArray(jsonStr);

		jsonArray.forEach(x -> {
			this.setBackToTmpFile(Integer.parseInt(x.toString()));
		});

	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public void setToBeDeleted(final String jsonStr) {

		if (StringUtils.isBlank(jsonStr)) {
			return;
		}

		final JSONArray jsonArray = new JSONArray(jsonStr);

		jsonArray.forEach(x -> {
			this.setToBeDeleted(Integer.parseInt(x.toString()));
		});

	}

	/**
	 * model/enity update esetén a régi és az új fileIds rendezése
	 * (törölni a már nem kellőket, {@link #setToNormal(int)} a többire)
	 *
	 * @param jsonStrOld hiányzó esetén nincs törlés, a {@link #setToNormal(int)} hívások csak
	 * @param jsonStrNew
	 * @see #setToNormal(int)
	 * @see #setToBeDeleted(int)
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public void setToNormalOrDelete(final String jsonStrOld, final String jsonStrNew) {

		if (StringUtils.isBlank(jsonStrNew)) {
			return;
		}

		if (StringUtils.isBlank(jsonStrOld)) {
			this.setToNormal(jsonStrNew);
			return;
		}

		final JSONArray jsonArrayOld = new JSONArray(jsonStrOld);
		final JSONArray jsonArrayNew = new JSONArray(jsonStrNew);

		// ---

		final Set<Integer> oldSet = new HashSet<>();
		final Set<Integer> newSet = new HashSet<>();

		jsonArrayOld.forEach(x -> {
			oldSet.add(Integer.parseInt(x.toString()));
		});

		jsonArrayNew.forEach(x -> {
			newSet.add(Integer.parseInt(x.toString()));
		});

		// ---

		// deleted-re állítani, ami a régiben benne volt, de az újban nincs

		final Set<Integer> toBeDeletedSet = new HashSet<>(oldSet); 
		toBeDeletedSet.removeAll(newSet);

		toBeDeletedSet.forEach(x -> {
			this.setToBeDeleted(x);
		});

		// ---
		
		// normal-ra állítani, ami az új set-ben jelent meg újként 
		
		final Set<Integer> freshSet = new HashSet<>(newSet);
		freshSet.removeAll(oldSet);
		
		freshSet.forEach(x -> {
			this.setToNormal(x);
		});

	}

	/**
	 * öröklődik a childFileDescriptor-okra is (pl.: átméretezett képek)
	 * 
	 * @param fileDescriptorId
	 * @param meta1
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public void saveMeta1(final int fileDescriptorId, final String meta1) {

		final FileDescriptor fileDescriptor = this.fileDescriptorJdbcRepository.findOne(fileDescriptorId);

		this.checkAccessRight(fileDescriptor);

		// ---

		this.fileDescriptorJdbcRepository.saveMeta1(fileDescriptorId, meta1);

		// ---

		final List<FileDescriptor> childFileDescriptors = this.fileDescriptorJdbcRepository.findAllBy("parentId", fileDescriptorId);
		for (final FileDescriptor childFileDescriptor : childFileDescriptors) {
			this.saveMeta1(childFileDescriptor.getId(), meta1);
		}

	}

	/**
	 * öröklődik a childFileDescriptor-okra is (pl.: átméretezett képek)
	 * 
	 * @param fileDescriptorId
	 * @param csTypeId
	 * @param csItemId
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_LCU_STR)
	public void saveMeta3(final int fileDescriptorId, final int csTypeId, final Integer csItemId) {

		final FileDescriptor fileDescriptor = this.fileDescriptorJdbcRepository.findOne(fileDescriptorId);

		this.checkAccessRight(fileDescriptor);

		this.fileDescriptorJdbcRepository.saveMeta3(fileDescriptorId, csTypeId, csItemId);

		// ---

		final List<FileDescriptor> childFileDescriptors = this.fileDescriptorJdbcRepository.findAllBy("parentId", fileDescriptorId);
		for (final FileDescriptor childFileDescriptor : childFileDescriptors) {
			this.saveMeta3(childFileDescriptor.getId(), csTypeId, csItemId);
		}

	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public void saveSecurityType(final int fileDescriptorId, final int securityTypeId) {

		final FileDescriptor fileDescriptor = this.fileDescriptorJdbcRepository.findOne(fileDescriptorId);

		this.checkAccessRight(fileDescriptor);

		this.fileDescriptorJdbcRepository.saveSecurityType(fileDescriptorId, securityTypeId);

		// ---

		final List<FileDescriptor> childFileDescriptors = this.fileDescriptorJdbcRepository.findAllBy("parentId", fileDescriptorId);
		for (final FileDescriptor childFileDescriptor : childFileDescriptors) {
			this.saveSecurityType(childFileDescriptor.getId(), securityTypeId);
		}

	}

	/**
	 * nem lemezes átnevezés, csak DB
	 * 
	 * @param fileDescriptorId
	 * @param filename
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public void renameFile(final int fileDescriptorId, final String filename) {
		this.renameFile(fileDescriptorId, filename, true);
	}

	/**
	 * nem lemezes átnevezés, csak DB
	 *
	 * @param fileDescriptorId
	 * @param filename
	 * @param appendExtensionFromParent
	 * 		hozzáfűzze-e a parent kiterjesztést a filenévhez
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public void renameFile(final int fileDescriptorId, String filename, final boolean appendExtensionFromParent) {

		final FileDescriptor fileDescriptor = this.fileDescriptorJdbcRepository.findOne(fileDescriptorId);

		this.checkAccessRight(fileDescriptor);

		if (appendExtensionFromParent) {
			filename += "." + FilenameUtils.getExtension(fileDescriptor.getFilename());
		}

		this.fileDescriptorJdbcRepository.renameFile(fileDescriptorId, filename);

	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public FileDescriptor getFileWithMatchingFilename(final String filename) {
		final FileDescriptor matchingFileByName = this.fileDescriptorJdbcRepository.findOneBy("filename", filename);

		if (matchingFileByName != null) { // TODO: miért kell itt mégegy null check, findOneBy eleve ezt ad vissza nem?
			return matchingFileByName;
		}

		return null;
	}

	/**
	 * meta3 szerint válogatja szét, 
	 * talán HomeFood vagy Cactus használta, 
	 * szükség estén ét kell nézni 
	 * 
	 * @param fileIds
	 * @return
	 */
	@Deprecated
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public Map<Integer, List<FileDescriptor>> categorizeFileIdsFromString(final String fileIds) {

		// TODO: átnézni, eb

		final Map<Integer, List<FileDescriptor>> fdMap = new HashMap<>();

		if (StringUtils.isNotEmpty(fileIds)) {
			final JSONArray jsonArray = new JSONArray(fileIds);

			for (int i = 0; i < jsonArray.length(); i++) {

				final FileDescriptor fd = this.getFile2(jsonArray.getInt(i), false, null, FileOperationAccessTypeIntent.READ_ONLY_INTENT);

				if (fd.getMeta3() != null) {

					if (fdMap.get(fd.getMeta3()) != null) {
						fdMap.get(fd.getMeta3()).add(fd);
					} else {
						final List<FileDescriptor> newList = new ArrayList<>();
						newList.add(fd);
						fdMap.put(fd.getMeta3(), newList);
					}

				} else { // ha nem létezik akkor 0-os idval belerakjuk mint kategorizálatlan

					if (fdMap.get(0) != null) {
						fdMap.get(0).add(fd);
					} else {
						final List<FileDescriptor> newList = new ArrayList<>();
						newList.add(fd);
						fdMap.put(0, newList);
					}

				}
			}
		}

		return fdMap;
	}

	/**
	 * kvázi egy "szervátültetés"... 
	 * (értsd {@link FileDescriptor} mókolással, kicseréli az érdemi fájlt "benne")
	 * 
	 * @param fileDescriptorId
	 * @param request
	 */
	public void trickyReplaceFile(final int fileDescriptorId, final MultipartHttpServletRequest request, final String notifId) {

		// TODO: tisztázni és tesztelni még

		final FileDescriptor oldFile = this.getFile2(fileDescriptorId, false, null, FileOperationAccessTypeIntent.CHANGE_INTENT);

		this.checkAccessRight(oldFile);
		this.checkFileLock(oldFile);

		// if (ToolboxSysKeys.FileDescriptorStatus.NORMAL != oldFile.getStatus()) {
		// throw new ToolboxGeneralException("replaceFileUpload error 1");
		// }

		// ---

		final FileDescriptor newFile;

		try {

			final String pathPrefix = FilenameUtils.getFullPath(oldFile.getFilePath());

			if (StringUtils.isNotBlank(pathPrefix)) {
				FileStoreService.tlFilePathPrefix.set(pathPrefix);
			}

			final List<FileDescriptor> tmpFiles = FileStoreHelper.saveAsTmpFiles(request, oldFile.getLocationType(), oldFile.getSecurityType());

			if (tmpFiles.size() != 1) {
				throw new ToolboxGeneralException("replaceFileUpload error 2");
			}

			newFile = tmpFiles.get(0);

		} finally {
			FileStoreService.tlFilePathPrefix.remove();
		}

		// ---

		final FileDescriptor oldFileWithNewHeart;
		final FileDescriptor newFileWithOldHeart;

		try {

			// swap a legtöbb adatra (kivéve id)

			final int newFileId = newFile.getId();
			final int oldFileId = oldFile.getId();

			final String oldFilename = oldFile.getFilename();

			final FileDescriptor c = new FileDescriptor();
			BeanUtils.copyProperties(c, oldFile); // oldFile -> c
			BeanUtils.copyProperties(oldFile, newFile); // newFile -> oldFile
			BeanUtils.copyProperties(newFile, c); // c -> newFile

			oldFile.setId(oldFileId);
			oldFile.setFilename(oldFilename);

			newFile.setId(newFileId);

			oldFileWithNewHeart = oldFile;
			newFileWithOldHeart = newFile;

		} catch (final Exception e) {
			throw new ToolboxGeneralException(e);
		}

		// ---

		// itt az extra variálás a unique és a not null contraint-ek miatt kell

		final String a = oldFileWithNewHeart.getFilePath();
		oldFileWithNewHeart.setFilePath(UUID.randomUUID().toString());
		final String b = newFileWithOldHeart.getFilePath();
		newFileWithOldHeart.setFilePath(UUID.randomUUID().toString());

		oldFileWithNewHeart.setLockedBy(null); // lock-ot direkt leszedjük ilyenkor
		oldFileWithNewHeart.setLockedOn(null);
		newFileWithOldHeart.setLockedOn(null);
		newFileWithOldHeart.setLockedOn(null);

		this.fileDescriptorJdbcRepository.save(oldFileWithNewHeart);
		this.fileDescriptorJdbcRepository.save(newFileWithOldHeart);

		oldFileWithNewHeart.setFilePath(a);
		this.fileDescriptorJdbcRepository.save(oldFileWithNewHeart);

		newFileWithOldHeart.setFilePath(b);
		this.fileDescriptorJdbcRepository.save(newFileWithOldHeart);

		// ---

		this.remoteFileService.disownAndAdoptRemoteFile(oldFileWithNewHeart.getId(), newFileWithOldHeart.getId());

		// ---

		this.disownChildVariants(oldFileWithNewHeart.getId()); // a file id-hoz tartozó, immár elavult átmérezett stb. fájlok

		// ---

		this.setToNormal(oldFileWithNewHeart.getId());
		this.setToBeDeleted(newFileWithOldHeart.getId());

		// ---

		// log.debug("trickyReplaceFile");

		if (notifId != null) {
			final HashMap<String, Object> hm = new HashMap<>();
			hm.put("fileDescriptorId", fileDescriptorId);
			JmsManager.send(JmsManager.buildDestStr(JmsDestinationMode.TENANT, Integer.toString(SecurityUtil.getLoggedInUserTenantId()), "replace-fd-" + notifId), hm);
		}
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public FileDescriptor lockFileDescriptor(final int fileDescriptorId) {

		final FileDescriptor fileDescriptor = this.fileDescriptorJdbcRepository.findOne(fileDescriptorId);
		this.checkAccessRight(fileDescriptor);

		if (!this.hasFileLock(fileDescriptor)) {
			this.fileDescriptorJdbcRepository.setLockedByAndOn(fileDescriptor.getId(), SecurityUtil.getLoggedInUser().getId(), new Timestamp(System.currentTimeMillis()));
			return this.fileDescriptorJdbcRepository.findOne(fileDescriptorId);
		} else {
			throw new FileLockException("Cannot lock (already has a lock)!", I.trc("Notification", "Cannot lock (already has a lock)!"));
		}

	}

	/**
	 * @param fileDescriptorId
	 * 
	 * @see #hasFileLock(FileDescriptor)
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public FileDescriptor unlockFileDescriptor(final int fileDescriptorId) {

		final FileDescriptor fileDescriptor = this.fileDescriptorJdbcRepository.findOne(fileDescriptorId);

		this.checkAccessRight(fileDescriptor);

		if (!this.hasFileLock(fileDescriptor)) {
			this.fileDescriptorJdbcRepository.setLockedByAndOn(fileDescriptor.getId(), null, null);
			return this.fileDescriptorJdbcRepository.findOne(fileDescriptorId);
		} else {
			throw new FileLockException("Cannot unlock (yet)!", I.trc("Notification", "Cannot unlock (yet)!"));
		}

	}

	public boolean hasFileLock(final int fileDescriptorId) {
		final FileDescriptor fileDescriptor = this.fileDescriptorJdbcRepository.findOne(fileDescriptorId);
		return this.hasFileLock(fileDescriptor);
	}

	/**
	 * @param fileDescriptorId
	 * 
	 * @throws FileLockException
	 */
	public void checkFileLock(final int fileDescriptorId) throws FileLockException {
		final FileDescriptor fileDescriptor = this.fileDescriptorJdbcRepository.findOne(fileDescriptorId);
		this.checkFileLock(fileDescriptor);
	}

}