package hu.lanoga.toolbox.export;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Semaphore;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jodconverter.core.office.OfficeUtils;
import org.jodconverter.local.JodConverter;
import org.jodconverter.local.office.LocalOfficeManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.file.RapidTmpHelper;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * LibreOffice helper...
 */
@Slf4j
@ConditionalOnMissingBean(name = "sofficeHelperOverrideBean")
@DependsOn(value = "applicationContextHelper")
@Component
public class SofficeHelper {

	@AllArgsConstructor
	@Getter
	public enum ConvertMode {

		PDF_CALC_PDF_EXPORT("pdf:calc_pdf_Export"), PDF("pdf");

		private final String sofficeParamStr;
	}

	public static final int MAX_PARALLEL = 4; // Math.max((Runtime.getRuntime().availableProcessors() / 2) - 2, 1);
	private static final Semaphore writeToPdfCmdSemaphore = new Semaphore(MAX_PARALLEL);

	private LocalOfficeManager officeManager; // TODO: biztosan thread-safe?

	/**
	 * az újabb megoldáshoz kell, OS alapján megpróbálja kitalálni, ha üres
	 */
	@Value("${tools.soffice.folder}")
	private String toolsSofficeFolder;

	@Value("${tools.file.disk.rapidtmpdir}")
	private String rapidTmpFolder;
	
	/**
	 * old = command line, 
	 * "new" = open/libreoffice "szerver" mód, 
	 * az old mód egyben fallback is
	 */
	@Value("${tools.soffice.use-old}")
	private boolean useOld;

	/**
	 * useOld módhoz
	 */
	private String sofficeExecutable;

	private static String checkFolderExistence(final boolean checkIfItIsActuallyAFolder, final String... folderStrs) {

		for (final String folderStr : folderStrs) {

			log.info("soffice location check: " + folderStr);
			
			final File f = new File(folderStr);

			if ((checkIfItIsActuallyAFolder && f.isDirectory())
					|| ((!checkIfItIsActuallyAFolder) && f.exists())) {

				log.info("soffice location check success (checkIfItIsActuallyAFolder: " + checkIfItIsActuallyAFolder + "): " + folderStr);
				
				return folderStr;

			}
			
			log.info("soffice location check failed: " + folderStr);

		}

		return null;

	}

	private static void isCompletelyWritten(final File file) {

		final long fileCheckStarted = System.currentTimeMillis();

		while (!isCompletelyWrittenInner(file, fileCheckStarted)) {
			//
		}
	}

	private static boolean isCompletelyWrittenInner(final File file, final long fileCheckStarted) {

		if (System.currentTimeMillis() - fileCheckStarted > 60L * 1000L) {
			throw new ToolboxGeneralException("isCompletelyWritten check failed (timeout)");
		}

		final long fileSizeBefore = file.length();

		try {
			Thread.sleep(500);
		} catch (final InterruptedException e) {
			Thread.interrupted();
			return false;
		}

		final long fileSizeAfter = file.length();

		// log.debug("comparing file size " + fileSizeBefore + " with " + fileSizeAfter);

		return (fileSizeBefore == fileSizeAfter && fileSizeAfter > 0L);
	}

	@PostConstruct
	public void init() {
		
		try {

			if (StringUtils.isBlank(this.toolsSofficeFolder)) {

				if (OS.isFamilyWindows()) {
					this.toolsSofficeFolder = checkFolderExistence(true, "C:/Program Files/LibreOffice", "C:/Program Files (x86)/LibreOffice", "D:/Apps/LibreOfficePortable/App/libreoffice");
				} else if (OS.isFamilyMac()) {
					this.toolsSofficeFolder = checkFolderExistence(true, "/Applications/LibreOffice.app/Contents"); // lehet, hogy nem tesztelt
				} else {
					// Linux
					this.toolsSofficeFolder = checkFolderExistence(false,
							"/opt/libreoffice5.1",
							"/opt/libreoffice5.2", 
							"/opt/libreoffice5.3",
							"/opt/libreoffice5.4",
							"/opt/libreoffice6.0",
							"/opt/libreoffice6.1",
							"/opt/libreoffice6.2",
							"/opt/libreoffice6.3",
							"/opt/libreoffice6.4"
					);
				}

			}

			// ---

			if (this.toolsSofficeFolder != null) {

				if (OS.isFamilyWindows()) {
					this.sofficeExecutable = this.toolsSofficeFolder + "/program/soffice.exe";
				} else if (OS.isFamilyMac()) {
					this.sofficeExecutable = this.toolsSofficeFolder + "/MacOS/soffice";
				} else {
					// Linux
					this.sofficeExecutable = this.toolsSofficeFolder + "/program/soffice";
				}

			} else {

				this.sofficeExecutable = "soffice";

				log.warn("sofficeFolder not exists / not found");
				throw new ToolboxGeneralException("sofficeFolder not exists / not found");

			}

			// ---

			log.info("this.toolsSofficeFolder: " + this.toolsSofficeFolder);
			log.info("this.sofficeExecutable: " + this.sofficeExecutable);

			// ---

			if (this.useOld) {
				log.info("officeManager useOld true");
				return;
			}
			
			File workingDir = RapidTmpHelper.createTempDirectory4("soffice-home");
			
			this.officeManager = LocalOfficeManager.builder()
					.officeHome(this.toolsSofficeFolder)
					.workingDir(workingDir.getAbsolutePath())
					// .portNumbers(8100) // 81 a default
					.install().build(); // start an office process and connect to the started instance (on port 2002)
			this.officeManager.start();
			
			log.info("officeManager LocalOfficeManager isRunning(): " + officeManager.isRunning());

		} catch (final Exception e) {
			// throw new ToolboxGeneralException("officeManager init error!", e);
			log.warn("officeManager init error: " + e.getMessage(), e);
		}

	}

	@PreDestroy
	public void stop() {

		if (this.useOld) {
			return;
		}

		try {
			OfficeUtils.stopQuietly(this.officeManager);
		} catch (final Exception e) {
			log.error("officeManager stop error!", e);
		}

	}

	/**
	 * @param inputFd
	 * @param outputFd
	 * 		opcionális, csinál egy TMP fájlt, ha null 
	 * @return
	 */
	public FileDescriptor convertToPdf(final SofficeHelper.ConvertMode convertMode, final FileDescriptor inputFd, final FileDescriptor outputFd) {

		if (!this.useOld) {
			try {
				return this.convertToPdfNew(inputFd, outputFd);
			} catch (final Exception e) {
				log.error("convertToPdfNew ('server' version) failed (trying to use convertToPdfOld (cli version) as a fallback)", e);
			}

		}

		// ---

		return this.convertToPdfOld(convertMode, inputFd, outputFd, !this.useOld);

	}

	private FileDescriptor convertToPdfNew(final FileDescriptor inputFd, final FileDescriptor outputFd) {

		final int permitCount = 1;

		try {

			writeToPdfCmdSemaphore.acquire(permitCount);

			// isCompletelyWritten(inputFd.getFile());

			// ---

			final FileDescriptor resultFd;

			if (outputFd == null) {
				resultFd = ApplicationContextHelper.getBean(FileStoreService.class).createTmpFile2(FilenameUtils.removeExtension(inputFd.getFilename()) + ".pdf", inputFd.getLocationType(), inputFd.getSecurityType());
			} else {
				resultFd = outputFd;
			}

			Thread.sleep(250);

			JodConverter
					.convert(inputFd.getFile())
					.to(resultFd.getFile())
					.execute();

			isCompletelyWritten(resultFd.getFile());

			return resultFd;

		} catch (final Exception e) {
			throw new ToolboxGeneralException("convertToPdfNew execute error!", e);
		} finally {
			writeToPdfCmdSemaphore.release(permitCount);
		}

	}

	/**
	 * régi command line verzió; 
	 * bugos, néha nem lesz PDF output (csak egyes Windows gépeken?)
	 * 
	 * @param inputFd
	 * @param outputFd
	 * 		opcionális, csinál egy TMP fájlt, ha null 
	 * @return
	 */
	private FileDescriptor convertToPdfOld(final SofficeHelper.ConvertMode convertMode, final FileDescriptor inputFd, final FileDescriptor outputFd, final boolean usedAsFallback) {

		final int permitCount = MAX_PARALLEL; // = mind, tehát csak egy futhat, LibreOffice 5-6 alatt van egy bug, ami miatt több cmd line példány nem megy rendesen párhuzamosan (https://bugs.documentfoundation.org/show_bug.cgi?id=37531)... LibreOffice 4 alatt még nem volt ilyen bug...

		Path tempDirectory = null;

		try {

			writeToPdfCmdSemaphore.acquire(permitCount);

			// isCompletelyWritten(inputFd.getFile());

			// ---

			tempDirectory = RapidTmpHelper.createTempDirectory1();
			final Path tempDirectoryInFile = Paths.get(tempDirectory.toString() + "/" + inputFd.getFile().getName());
			final Path tempDirectoryOutFile = Paths.get(tempDirectory.toString() + "/" + FilenameUtils.removeExtension(inputFd.getFile().getName()) + ".pdf");

			// ---

			Files.copy(inputFd.getFile().toPath(), tempDirectoryInFile);
			// isCompletelyWritten(tempDirectoryInFile.toFile());

			// ---

			final DefaultExecutor executor = new DefaultExecutor();
			executor.setExitValue(0);

			final CommandLine cmdLine = CommandLine.parse("\"" + this.sofficeExecutable + "\" --headless --convert-to " + convertMode.getSofficeParamStr() + " --outdir \"" + tempDirectory.toFile().getAbsolutePath() + "\" \"" + tempDirectoryInFile.toFile().getAbsolutePath() + "\"");

			final ExecuteWatchdog watchdog = new ExecuteWatchdog(300L * 1000L);
			executor.setWatchdog(watchdog);

			executor.setStreamHandler(new PumpStreamHandler(null, null, null)); // kikapcsolja a logolást/console output-ot

			executor.execute(cmdLine); // elég pazarló, két thread-et is létrehoz (és nincs pool), egyet az execute-nak és egyet a watchdog-nak

			// ---

			isCompletelyWritten(tempDirectoryOutFile.toFile());

			// ---

			final FileDescriptor resultFd;

			if (outputFd == null) {
				resultFd = ApplicationContextHelper.getBean(FileStoreService.class).createTmpFile2(FilenameUtils.removeExtension(inputFd.getFilename()) + ".pdf", inputFd.getLocationType(), inputFd.getSecurityType());
			} else {
				resultFd = outputFd;
			}

			// ---

			Files.copy(tempDirectoryOutFile, resultFd.getFile().toPath(), StandardCopyOption.REPLACE_EXISTING);

			// ---

			// isCompletelyWritten(resultFd.getFile());

			// ---

			return resultFd;

		} catch (final Exception e) {
			throw new ToolboxGeneralException("convertToPdfOld execute error (usedAsFallback: " + usedAsFallback + ")!", e);
		} finally {
			try {
				if (tempDirectory != null) {
					FileUtils.deleteDirectory(tempDirectory.toFile());
				}
			} catch (final Exception e) {
				log.error("Could not delete TMP folder!", e);
			}
			writeToPdfCmdSemaphore.release(permitCount);
		}

	}

}
