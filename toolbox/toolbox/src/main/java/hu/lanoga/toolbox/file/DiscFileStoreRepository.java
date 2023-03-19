package hu.lanoga.toolbox.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import com.google.common.hash.Hashing;

import hu.lanoga.toolbox.ToolboxSysKeys.FileDescriptorLocationType;

@ConditionalOnMissingBean(name = "discFileStoreRepositoryOverrideBean")
@Repository
public class DiscFileStoreRepository {

	@Value("${tools.file.disk.storedir}")
	private String fileDiscStoreDirStr;

	@Value("${tools.file.disk.cdndir}")
	private String fileDiscCdnDirStr;

	/**
	 * SHA-256
	 *
	 * @param file
	 * @return
	 * @throws IOException
	 */
	String generateHashFromFile(final File file) {

		try {

			// ne változtasd az algoritmust, DB-ben is tárolva van...

			return com.google.common.io.Files.asByteSource(file).hash(Hashing.sha256()).toString();

		} catch (final Exception e) {
			throw new DiscFileStoreRepositoryException("hash calculation error", e);
		}

	}

	/**
	 * nem hoz létre semmit, csak összerakja a {@link File}-t... (ami csak egy "pointer"/path jellegű dolog)
	 * 
	 * @param pathStr
	 * @param fileDescriptorLocationType
	 * @return
	 */
	File getFile(final String pathStr, final int fileDescriptorLocationType) {

		if (FileDescriptorLocationType.PROTECTED_FOLDER == fileDescriptorLocationType) {
			return new File(this.fileDiscStoreDirStr + File.separator + pathStr);
		}

		if (FileDescriptorLocationType.PUBLIC_FOLDER_FOR_CDN == fileDescriptorLocationType) {
			return new File(this.fileDiscCdnDirStr + File.separator + pathStr);
		}

		throw new DiscFileStoreRepositoryException("Bad FileDescriptorLocationType param!");
	}

	void deleteFile(final String pathStr, final int fileDescriptorLocationType) {

		final File file = this.getFile(pathStr, fileDescriptorLocationType);

		try {
			if (file.exists()) {
				Files.delete(file.toPath());
			}
		} catch (final Exception e) {
			throw new DiscFileStoreRepositoryException("File delete error!", e);
		}

	}

	void makeParentDirAndTouch(final File file) {
		try {
			FileUtils.forceMkdirParent(file);
			FileUtils.touch(file);
		} catch (final Exception e) {
			throw new DiscFileStoreRepositoryException("File create error!", e);
		}
	}

}
