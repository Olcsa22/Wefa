package hu.lanoga.toolbox.file;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import lombok.extern.slf4j.Slf4j;

/**
 * ha feldolgozáshoz, generáláshoz kell egy "light" tmp megoldás... 
 * köztes lépésekhez, ahol a {@link FileDescriptor} alapú megoldás nem kell (overkill lenne), nem jó... 
 */
@Slf4j
public class RapidTmpHelper {

	/**
	 * 1 napnál régebbiek
	 */
	private static long CLEAN_UP_DELETE_THIS_OLD = 24L * 60L * 60L * 1000L;

	private RapidTmpHelper() {
		//
	}

	private static Path createTempDirectory(String subfolderName) {
		
		try {
			final String rapidTmpDirStr = ApplicationContextHelper.getConfigProperty("tools.file.disk.rapidtmpdir");
			final File rapidTmpDir = new File(rapidTmpDirStr);
			
			FileUtils.forceMkdir(rapidTmpDir);

			return Files.createTempDirectory(rapidTmpDir.toPath(), subfolderName);
			
		} catch (final Exception e) {
			throw new ToolboxGeneralException("createTempDirectory error!", e);
		}

	}
	
	public static Path createTempDirectory1() {
		return createTempDirectory(UUID.randomUUID().toString());
	}

	public static File createTempDirectory2() {
		return createTempDirectory1().toFile();
	}
	
	public static Path createTempDirectory3(String subfolderName) {
		return createTempDirectory(subfolderName);
	}
	
	public static File createTempDirectory4(String subfolderName) {
		return createTempDirectory3(subfolderName).toFile();
	}

	/**
	 * soha nem dob exception-t
	 */
	public static void cleanUp() {
		
		// TODO: nem töröl mindent, üres könyvárak maradtak! konkrétan az hu.lanoga.toolbox.export.SofficeHelper után
		
		final String rapidTmpDirStr = ApplicationContextHelper.getConfigProperty("tools.file.disk.rapidtmpdir");

		try {

			final File rapidTmpDir = new File(rapidTmpDirStr);

			if (!rapidTmpDir.exists()) {
				return;
			}

			final List<Path> list = Files.list(rapidTmpDir.toPath()).collect(Collectors.toList());

			final long limitTime = System.currentTimeMillis() - CLEAN_UP_DELETE_THIS_OLD;

			for (final Path p : list) {

				final File f = p.toFile();

				if (f.lastModified() < limitTime) {
					FileUtils.deleteQuietly(f); // elvileg könyvtárt is töröl
				}

			}

		} catch (final Exception e) {
			log.error("RapidTmpHelper cleanUp failed!", e);
		}

	}

}
