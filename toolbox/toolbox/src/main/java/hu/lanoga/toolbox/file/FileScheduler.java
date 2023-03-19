package hu.lanoga.toolbox.file;

import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.FileOperationAccessTypeIntent;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.tenant.TenantUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @see FileStoreService#cleanUp()
 */
@Slf4j
@NoArgsConstructor
@ConditionalOnMissingBean(name = "fileSchedulerOverrideBean")
@ConditionalOnProperty({ "tools.job-runner", "tools.file.scheduler.enabled" })
@Component
public class FileScheduler {

	@Autowired
	private FileStoreService fileStoreService;

	@Value("${tools.file.scheduler.archive.enabled}")
	private boolean archiveEnabled;

	@PostConstruct
	private void init() {
		log.info("FileScheduler initialized.");
	}
	
	@Scheduled(fixedDelay = 1000L)
	public void runPreResizeQueue2048ProgrJpgQueue() {

		final FileDescriptor to2048ProgrJpgFileDesc = FileStoreService.pollPreResizeQueue();

		if (to2048ProgrJpgFileDesc != null) {

			try {

				SecurityUtil.setSystemUser();
				JdbcRepositoryManager.setTlTenantId(to2048ProgrJpgFileDesc.getTenantId());

				// mj.: csak elő "pörgetjük" itt, ezért a visszatérési értékkel itt nem foglalkozunk

				this.fileStoreService.getFile2(to2048ProgrJpgFileDesc.getId(), true, ToolboxSysKeys.FileDescriptorChildType.VARIANT_2048_2048_PROGR_JPG);

			} finally {
				JdbcRepositoryManager.clearTlTenantId();
				SecurityUtil.clearAuthentication();
			}

		}

	}
	
	@Transactional(propagation = Propagation.REQUIRES_NEW) // ezért kell, hogy egy másik osztályban legyen
	public FileDescriptor getFile2(final int fileDescriptorId, final boolean downloadMissingOrArchivedFile, final Integer childTypeCsiId, final FileOperationAccessTypeIntent fileOperationAccessTypeIntent) {
		return this.fileStoreService.getFile2(fileDescriptorId, downloadMissingOrArchivedFile, childTypeCsiId, fileOperationAccessTypeIntent);
	}
	
	public static AtomicLong lastExecutionMillis = new AtomicLong(0);

	@Scheduled(cron = "${tools.file.scheduler.cronExpression:0 0 * * * *}")
	public void run() {

		try {

			SecurityUtil.setSystemUser();

			TenantUtil.runWithEachTenant(() -> {

				this.fileStoreService.cleanUp();

				return null;

			});

			// ---

			if (this.archiveEnabled) {
				this.fileStoreService.manageArchive();
			}
			
			// ---
			
			RapidTmpHelper.cleanUp();
			
			// ---
			
			lastExecutionMillis.set(System.currentTimeMillis());

		} catch (final Exception e) {
			log.error("FileScheduler error!", e);
		} finally {
			SecurityUtil.clearAuthentication();
		}

	}

}
