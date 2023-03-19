package hu.lanoga.toolbox.db;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import hu.lanoga.toolbox.ToolboxSysKeys.FileDescriptorLocationType;
import hu.lanoga.toolbox.ToolboxSysKeys.FileDescriptorSecurityType;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreHelper;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.util.ToolboxAssert;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@NoArgsConstructor
@ConditionalOnMissingBean(name = "dbBackupSchedulerOverrideBean")
@ConditionalOnProperty({ "tools.job-runner", "tools.db.backup.scheduler.enabled" })
@Slf4j
public class DbBackupScheduler {

	@Value("${spring.datasource.url}")
	private String datasourceUrl;

	@Value("${spring.datasource.username}")
	private String datasourceUsername;

	@Value("${spring.datasource.password}")
	private String datasourcePassword;

	// @Value("${tools.db.backup.dir}")
	// private String backupDirStr;

	@Value("${datasource.database-name}")
	private String databaseName;

	@Value("${datasource.host}")
	private String databaseHost;

	@Value("${datasource.port}")
	private String datasourcePort;

	@Value("${tools.db.backup.zip-password}")
	private String zipPassword;

	@Autowired
	private FileStoreService fileStoreService;
	
	public static AtomicLong lastExecutionMillis = new AtomicLong(0);

	@Scheduled(cron = "${tools.db.backup.scheduler.cronExpression}")
	public void backup() {

		log.info("DB backup process started.");

		try {

			SecurityUtil.setSystemUser();
			
			final String physicalAddressHex = SecurityUtil.getPhysicalAddressHex();

			final String targetFilename = "db_backup_" + this.databaseName + "_" + (physicalAddressHex != null ? physicalAddressHex : "unkown") + "_" + System.currentTimeMillis() + "_" + RandomStringUtils.randomAlphanumeric(5);
			
			final FileDescriptor fdBackup = this.fileStoreService.createTmpFile2(targetFilename, FileDescriptorLocationType.PROTECTED_FOLDER, FileDescriptorSecurityType.SYSTEM_ONLY);

			final ProcessBuilder pb = new ProcessBuilder(this.buildPgDumpCommands(fdBackup.getFile().getAbsolutePath()));
			pb.environment().put("PGPASSWORD", this.datasourcePassword);

			final Process process = pb.start();

			final String strError = IOUtils.toString(process.getErrorStream(), Charset.defaultCharset());

			process.waitFor();
			process.destroy();

			if (StringUtils.isNotBlank(strError)) {
				log.info("Backup file created, exit value: " + process.exitValue());
			} else {
				log.error("Backup file creation error, exit value: " + process.exitValue() + ", " + strError);
			}

			// ---
			
			ToolboxAssert.isTrue(StringUtils.isNotBlank(this.zipPassword));

			final FileDescriptor fdZip = FileStoreHelper.createZip(Lists.newArrayList(fdBackup.getFile()), this.zipPassword, targetFilename + ".zip", FileDescriptorLocationType.PROTECTED_FOLDER, FileDescriptorSecurityType.SYSTEM_ONLY);
		
			this.fileStoreService.setToNormal2(fdZip.getId()); // setToNormal2 azért kell, mert egyes projektek már a setToNormal-ban csinálnak GoogleDriveFile insertet is (tehát duplán lenne)...
			this.fileStoreService.setToBeDeleted(fdBackup.getId()); // csak az zip-et tartjuk meg, az eredeti rögtön törlendőre állítjuk
			 
			// TODO: ötlet: két könyvtár, egy ahová dump-ol és egy másik a végső backup hely (ok: lassú hálózati meghajtó, plusz csak így van értelme a jelszavazásnak)

			lastExecutionMillis.set(System.currentTimeMillis());
			
		} catch (final Exception e) {
			log.error("DbBackupScheduler error.", e);
		} finally {
			SecurityUtil.clearAuthentication();
		}

	}

	private List<String> buildPgDumpCommands(final String filename) {

		// lásd: https://www.postgresql.org/docs/9.3/app-pgdump.html

		final ArrayList<String> commands = new ArrayList<>();
		commands.add("pg_dump");
		commands.add("-h"); // database server host
		commands.add(this.databaseHost);
		commands.add("-p"); // database server port number
		commands.add(this.datasourcePort);
		commands.add("-U"); // connect as specified database user
		commands.add(this.datasourceUsername);
		commands.add("-F"); // output file format (custom, directory, tar, plain text (default))
		commands.add("c");
		commands.add("-b"); // include large objects in dump
		commands.add("-v"); // verbose mode
		commands.add("-f"); // output file or directory name
		commands.add(filename);
		commands.add("-d"); // database name
		commands.add(this.databaseName);
		return commands;
	}
}
