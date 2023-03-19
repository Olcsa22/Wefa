package hu.lanoga.toolbox.file.remote;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.service.AdminOnlyCrudService;
import hu.lanoga.toolbox.spring.SecurityUtil;

@Service
public class RemoteFileService extends AdminOnlyCrudService<RemoteFile, RemoteFileJdbcRepository> {

	@Autowired
	private FileStoreService fileStoreService;

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ANONYMOUS_STR)
	public RemoteFile save(final RemoteFile remoteFile) {

		if (!remoteFile.isNew()) {
			SecurityUtil.limitAccessAdminOrOwner(remoteFile.getCreatedBy());
		}

		return super.save(remoteFile);
	}

	/**
	 * visszaadja a Google Drive file-okat status alapján és rendezve priority (default: DESC) és createdOn (default: ASC) szerint
	 *
	 * @return
	 */
	public List<RemoteFile> findForScheduler() {

		SecurityUtil.limitAccessSystem();

		return this.repository.findForScheduler();
	}

	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public RemoteFile findForFileDescriptor(final int fileDescriptorId) {
		final FileDescriptor fileDescriptor = this.fileStoreService.getFile2(fileDescriptorId); // ez a access right check miatt kell így
		return this.repository.findOneBy("fileId", fileDescriptor.getId());
	}

	/**
	 * ha nincs vonatkozó {@link RemoteFile}, akkor semmit sem csinál érdemben
	 * 
	 * @param oldFileDescriptorId
	 * @param adoptiveFileDescriptorId
	 */
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public void disownAndAdoptRemoteFile(final int oldFileDescriptorId, final int adoptiveFileDescriptorId) {
		
		final FileDescriptor fileDescriptor = this.fileStoreService.getFile2(oldFileDescriptorId); // ez a access right check miatt kell így
		
		RemoteFile remoteFile = this.repository.findOneBy("fileId", fileDescriptor.getId());
		
		if (remoteFile != null) {
			remoteFile.setFileId(adoptiveFileDescriptorId);
			this.repository.save(remoteFile);
		}
	}
	
	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR)
	public RemoteFile findByFileDescriptorIdWithoutAccessRightCheck(final int fileDescriptorId) {
		return this.repository.findOneBy("fileId", fileDescriptorId);
	}

	/**
	 * ha a státusza {@link ToolboxSysKeys.RemoteProviderStatus#PENDING} és alacsonyan van a prioritása,
	 * de már régóta vár a feltöltésre, akkor emelünk a prioritásán...
	 */
	@Transactional
	public void increasePriorityIfNecessary() {

		SecurityUtil.limitAccessSystem();

		final List<RemoteFile> list = this.repository.findAllForPriorityIncrease();

		if (list != null && !list.isEmpty()) {
			for (final RemoteFile gdf : list) {
				gdf.setPriority(ToolboxSysKeys.RemoteProviderPriority.VERY_HIGH);
			}

			this.repository.save(list);
		}
	}

}