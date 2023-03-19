//package hu.lanoga.toolbox.sftp;
//
//import hu.lanoga.toolbox.exception.ToolboxGeneralException;
//import hu.lanoga.toolbox.file.FileDescriptor;
//import net.schmizz.sshj.SSHClient;
//import net.schmizz.sshj.connection.channel.direct.Session;
//import net.schmizz.sshj.sftp.SFTPClient;
//import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
//import net.schmizz.sshj.userauth.keyprovider.PKCS8KeyFile;
//import net.schmizz.sshj.xfer.FileSystemFile;
//import org.apache.commons.lang3.StringUtils;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.concurrent.TimeUnit;
//
///**
// * nincs befejezve még, experimental
// */
//public class SftpManager {
//	
//	// TODO: nincs befejezve még, experimental
//
//    public enum AuthType {PASSWORD, KEY}
//
//    private AuthType authType;
//
//    private String hostname;
//    private String userName;
//    private String password;
//
//    private File privateKeyFile;
//    private PKCS8KeyFile keyFile;
//
//    private SSHClient sshClient;
//    private SFTPClient sftpClient;
//
//    public SftpManager(String hostname, String userName, String password) {
//        this.hostname = hostname;
//        this.userName = userName;
//        this.password = password;
//        this.authType = AuthType.PASSWORD;
//
//        // TODO: auto connectSftp ?
//        connectSftp();
//    }
//
//    public SftpManager(String hostname, String userName, File privateKeyFile) {
//        this.hostname = hostname;
//        this.userName = userName;
//        this.privateKeyFile = privateKeyFile;
//        this.authType = AuthType.KEY;
//
//        // TODO: auto connectSftp ?
//        connectSftp();
//    }
//
//    public void upload(final FileDescriptor fileDescriptor, String destinationDirPath) {
//        upload(fileDescriptor, destinationDirPath, true);
//    }
//
//    public void uploadWithoutDisconnect(final FileDescriptor fileDescriptor, String destinationDirPath) {
//        upload(fileDescriptor, destinationDirPath, false);
//    }
//
//    public void download(final String remoteFilePath, String localeDirPath) {
//        download(remoteFilePath, localeDirPath, true);
//    }
//
//    public void downloadWithoutDisconnect(final String remoteFilePath, String localeDirPath) {
//        download(remoteFilePath, localeDirPath, false);
//    }
//
//    private void upload(final FileDescriptor fileDescriptor, String destinationDirPath, final boolean isDisconnectNecessary) {
//
//        if (fileDescriptor == null || fileDescriptor.getFile() == null || StringUtils.isBlank(fileDescriptor.getFilePath())) {
//            throw new ToolboxGeneralException("SFTP upload: locale file is not exists");
//        }
//        if (StringUtils.isBlank(destinationDirPath)) {
//            throw new ToolboxGeneralException("SFTP upload: remote file dir path is empty");
//        }
//
//        connectSftp();
//
//        try {
//            sftpClient.put(new FileSystemFile(fileDescriptor.getFile().getAbsolutePath()), destinationDirPath);
//        } catch (IOException e) {
//            throw new ToolboxGeneralException("SFTP PUT error: ", e);
//        } finally {
//            if (isDisconnectNecessary) {
//                disconnectSftp();
//            }
//        }
//    }
//
//    private void download(final String remoteFilePath, String localeDirPath, final boolean isDisconnectNecessary) {
//
//        if (StringUtils.isBlank(remoteFilePath)) {
//            throw new ToolboxGeneralException("SFTP download: remote file path is empty");
//        }
//        if (localeDirPath == null || !(new File(localeDirPath).exists())) {
//            throw new ToolboxGeneralException("SFTP download: locale file dir path is not exists");
//        }
//
//        connectSftp();
//
//        try {
//            sftpClient.get(remoteFilePath, new FileSystemFile(localeDirPath));
//        } catch (IOException e) {
//            throw new ToolboxGeneralException("SFTP GET error: ", e);
//        } finally {
//            if (isDisconnectNecessary) {
//                disconnectSftp();
//            }
//        }
//    }
//
//    // TODO ...
//    public Integer exec(final String command) {
//
//        connectSsh();
//
//        Integer exitStatus = null;
//        try {
//
//            final Session session = sshClient.startSession();
//
//            try {
//
//                final Session.Command cmd = session.exec(command);  //... "ping -c 1 google.com"
////                System.out.println(IOUtils.readFully(cmd.getInputStream()).toString());
//                cmd.join(5, TimeUnit.SECONDS);
//                exitStatus = cmd.getExitStatus();
//
//            } finally {
//                session.close();
//            }
//
//        } catch (Exception e) {
//            throw new ToolboxGeneralException("SSH exec error: ", e);
//        } finally {
//            disconnectSsh();
//        }
//
//        return exitStatus;
//    }
//
//    public void connectSftp() {
//
//        if (sftpClient == null) {
//
//            try {
//
//                connectSsh();
//                sftpClient = sshClient.newSFTPClient();
//
//            } catch (Exception e) {
//                throw new ToolboxGeneralException("SFTP connect error: ", e);
//            }
//        }
//    }
//
//    public void connectSsh() {
//
//        if (sshClient == null || !sshClient.isConnected() || !sshClient.isAuthenticated()) {
//
//            try {
//
//                // connectSftp
//                sshClient = new SSHClient();
//                sshClient.addHostKeyVerifier(new PromiscuousVerifier());
//                sshClient.loadKnownHosts();
//                sshClient.connect(hostname);
//
//                // auth
//                if (AuthType.PASSWORD.equals(authType)) {
//
//                    sshClient.authPassword(userName, password);
//
//                } else if (AuthType.KEY.equals(authType)) {
//
//                    // TODO... key probl.
//                    keyFile = new PKCS8KeyFile();
//                    keyFile.init(privateKeyFile);
//                    sshClient.authPublickey(userName, keyFile);
//
//                } else {
//                    throw new ToolboxGeneralException("SFTP auth: no valid auth type");
//                }
//
//            } catch (Exception e) {
//                throw new ToolboxGeneralException("SFTP/SSH connectSftp error: ", e);
//            }
//        }
//    }
//
//    public void disconnectSftp() {
//
//        try {
//
//            if (sftpClient != null) {
//                sftpClient.close();
//            }
//            disconnectSsh();
//
//        } catch (IOException e) {
//            throw new ToolboxGeneralException("SFTP/SSH disconnectSftp error: ", e);
//        }
//    }
//
//    public void disconnectSsh() {
//
//        try {
//
//            if (sshClient != null && sshClient.isConnected()) {
//                sshClient.disconnect();
//            }
//
//        } catch (IOException e) {
//            throw new ToolboxGeneralException("SSH disconnect error: ", e);
//        }
//    }
//}
