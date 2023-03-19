package hu.lanoga.toolbox.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SystemUtil {
	
	private SystemUtil() {
		//
	}

	public static void logCurrentEnvVars() {
        final Map<String, String> env = System.getenv();
        for (final String envName : env.keySet()) {
            log.debug(envName + "=" + env.get(envName));
        }
	}
	
	public static String logAndReturnCurrentFileSystemPath() {
        final Path currentRelativePath = Paths.get("");
        final String s = currentRelativePath.toAbsolutePath().toString();
        log.debug("Current path is: " + s);
        return s;
	}
	
}
