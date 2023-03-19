package hu.lanoga.toolbox.file.remote;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.FileOperationAccessTypeIntent;
import hu.lanoga.toolbox.amazon.s3.AmazonS3Manager;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.tenant.TenantUtil;
import hu.lanoga.toolbox.util.ToolboxStringUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
@ConditionalOnMissingBean(name = "remoteFileSchedulerOverrideBean")
@ConditionalOnProperty({ "tools.job-runner", "tools.file.remote.scheduler.enabled" })
@Component
public class RemoteFileScheduler {

	@Value("${tools.misc.application-name-lower-case}")
	private String appNameLc;

	@Autowired
	private RemoteFileService remoteFileService;

//	@Autowired
//	private GoogleDriveManager googleDriveManager;

	@Autowired
	private AmazonS3Manager amazonS3Manager;

	@Autowired
	private FileStoreService fileStoreService;

	private final AtomicBoolean exceutorIsRunning = new AtomicBoolean(false);

	private ExecutorService executor;

	@PostConstruct
	private void init() {
		this.executor = Executors.newFixedThreadPool(1);
		log.info("RemoteFileScheduler initialized.");
	}
	
	public static AtomicLong lastExecutionMillis = new AtomicLong(0);

	@Scheduled(cron = "${tools.file.remote.scheduler.cronExpression}")
	private void runUpload() {

		if (!this.exceutorIsRunning.getAndSet(true)) {

			this.executor.execute(() -> {

				try {

					// log.debug("GoogleDriveScheduler started.");

					SecurityUtil.setSystemUser();

					TenantUtil.runWithEachTenant(() -> {

						final List<RemoteFile> remoteFiles = this.remoteFileService.findForScheduler();

						for (final RemoteFile gdf : remoteFiles) {
							if (gdf.getStatus().equals(ToolboxSysKeys.RemoteProviderStatus.UPLOADED)) {
								if (Boolean.TRUE.equals(gdf.getAnyoneWithLinkRequested())) {
									this.shareUploadedFile(gdf);
								}
							} else {
								this.uploadFile(gdf);
							}
						}

						return null;
					});
					
					lastExecutionMillis.set(System.currentTimeMillis());

				} catch (final Exception e) {
					log.error("RemoteFileScheduler error!", e);
				} finally {

					SecurityUtil.clearAuthentication();

					// log.debug("GoogleDriveScheduler finished.");

					this.exceutorIsRunning.set(false);

				}

			});

		}
	}

	/**
	 * Starvation elkerüléséhez naponta fut egy job, ami emel a prioritáson, ha már régóta be van ragadva a file.
	 */
	@Scheduled(cron = "${tools.file.remote.scheduler.starvation.fixer.cronExpression}")
	private void fixStarvation() {
		try {

			log.debug("RemoteFileScheduler (fixStarvation)...");

			SecurityUtil.setSystemUser();

			TenantUtil.runWithEachTenant(() -> {

				this.remoteFileService.increasePriorityIfNecessary();
				return null;

			});
		} catch (final Exception e) {
			log.error("RemoteFileScheduler (fixStarvation) error!", e);
		} finally {
			SecurityUtil.clearAuthentication();
		}
	}

	private void shareUploadedFile(final RemoteFile gdf) {

		if (gdf.getRemoteProviderType().intValue() == ToolboxSysKeys.RemoteProviderType.GOOGLE_DRIVE) {

			throw new UnsupportedOperationException("RemoteProviderType.GOOGLE_DRIVE is not supported currently!");
			// this.googleDriveManager.shareWithUrl(gdf.getRemoteId());

		} else if (gdf.getRemoteProviderType().intValue() == ToolboxSysKeys.RemoteProviderType.AMAZON_S3) {

			final String publicUrl = this.amazonS3Manager.generatePresignedUrl(gdf.getRemoteId());
			gdf.setPublicUrl(publicUrl);

		}

		gdf.setAnyoneWithLink(true);

		this.remoteFileService.save(gdf);
	}

	private void uploadFile(final RemoteFile gdf) {

		try {

			log.debug(String.format("RemoteFile (id: %d) upload has started (remote provider type: " + gdf.getRemoteProviderType() + ")", gdf.getFileId()));

			final FileDescriptor fileDescriptor = this.fileStoreService.getFile2(gdf.getFileId(), FileOperationAccessTypeIntent.READ_ONLY_INTENT);

			final String a = FilenameUtils.getBaseName(fileDescriptor.getFilename());
			final String b = FilenameUtils.getExtension(fileDescriptor.getFilename());

			if (gdf.getRemoteProviderType() == ToolboxSysKeys.RemoteProviderType.GOOGLE_DRIVE) {
				
				throw new UnsupportedOperationException("RemoteProviderType.GOOGLE_DRIVE is not supported currently!");

//				final String filename = fileDescriptor.getId() + "-" + ToolboxStringUtil.convertToUltraSafe(a, "-") + "." + ToolboxStringUtil.convertToUltraSafe(b, "-");
//
//				final Pair<String, String> pair = this.googleDriveManager.uploadFile(fileDescriptor.getFile(), "files-" + this.appNameLc + "-" + gdf.getTenantId(), filename, fileDescriptor.getMimeType());
//				gdf.setRemoteId(pair.getLeft());
//				gdf.setPublicUrl(pair.getRight());

			} else if (gdf.getRemoteProviderType() == ToolboxSysKeys.RemoteProviderType.AMAZON_S3) {

				final String filename = "files-" + this.appNameLc + "-" + gdf.getTenantId() + "-" + fileDescriptor.getId() + "-" + ToolboxStringUtil.convertToUltraSafe(a, "-") + "." + ToolboxStringUtil.convertToUltraSafe(b, "-");

				this.amazonS3Manager.upload(filename, fileDescriptor.getFile());
				gdf.setRemoteId(filename);

			}

			gdf.setStatus(ToolboxSysKeys.RemoteProviderStatus.UPLOADED);

			log.debug(String.format("RemoteFile (id: %d) upload has finished.", gdf.getFileId()));

		} catch (final Exception e) {

			gdf.setStatus(ToolboxSysKeys.RemoteProviderStatus.FAILED);

			log.debug(String.format("RemoteFile (id: %d) upload has failed.", gdf.getId()), e);

		} finally {
			gdf.incrementAttempt();
			this.remoteFileService.save(gdf);
		}
	}
}
