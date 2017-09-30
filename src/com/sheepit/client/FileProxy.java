package com.sheepit.client;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public class FileProxy {

	private String fileProxyHost;
	private int fileProxyPort = -1;
	private String fileProxyUser;
	private String fileProxyPaswd;
	private Log log;
	private int fileProxyMaxCacheWaitTime;
	private boolean passive_mode;
	private String fileProxyDirectory;

	private FTPClient ftpClient;

	public FileProxy(Configuration config) {
		this.fileProxyPort = config.getFileProxyPort();
		this.fileProxyUser = config.getFileProxyUser();
		this.fileProxyPaswd = config.getFileProxyPasswd();
		this.fileProxyMaxCacheWaitTime = config.getFileProxyMaxCacheWaitTime();
		this.passive_mode = config.isFileProxyPassiveMode();

		this.log = Log.getInstance(null);
		log.debug("initialice FileProxy");
		this.fileProxyDirectory = "/";

		URI fileProxyUrl ;
		try {
			fileProxyUrl = new URI(config.getFileProxyUrl());
		} catch (URISyntaxException e) {
			log.error("Url " + config.getFileProxyUrl() + " is not valid" );
			return;
		}
		
		String authority_str = fileProxyUrl.getAuthority();
		
		if (authority_str.indexOf('@') > 0) {
			String authority[] = authority_str.split("@")[0].split(":");
			this.fileProxyUser = authority[0];
			this.fileProxyPaswd = authority[1];
		}
		fileProxyPort = fileProxyUrl.getPort();
		if (fileProxyPort == -1) {
			fileProxyPort = 21;
		}

		fileProxyHost = fileProxyUrl.getHost();
		fileProxyDirectory = fileProxyUrl.getPath();

		this.ftpClient = new FTPClient();

		try {
			initConnection();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		log.debug("FileProxy initalized");
	}

	private boolean initConnection() throws SocketException, IOException {
		if (fileProxyHost == null) {
			log.error("noHost");
			return false;
		}
		if (ftpClient.isConnected()) {
			log.debug("is connected");
			return true;
		}

		ftpClient.connect(fileProxyHost, fileProxyPort);
	
		log.debug("login with User: " + this.fileProxyUser + " passwd: " + this.fileProxyPaswd);
		ftpClient.login(this.fileProxyUser, this.fileProxyPaswd);
		int replyCode = ftpClient.getReplyCode();
		if (!FTPReply.isPositiveCompletion(replyCode)) {
			log.error("not connected " + ftpClient.getReplyString());
			ftpClient.disconnect();
			return false;
		}
		
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		if (passive_mode) {
			ftpClient.enterLocalPassiveMode();
		}
		
		if ((fileProxyDirectory != null) && (!fileProxyDirectory.equals(""))) {
			ftpClient.changeWorkingDirectory(fileProxyDirectory);
		}
		
		log.debug("connected");
		return true;
	}

	public boolean uploadFile(String remoteFilename, InputStream uploadStream) {
		try {
			log.debug("init connection");
			if (!this.initConnection()) {
				log.error("faild to init connection");
				return false;
				// transfer file
			}
			log.debug("staret upload");
			boolean rc = ftpClient.storeFile(remoteFilename, uploadStream);
			log.debug("uploaded " + remoteFilename + ": " + rc);
			log.debug(ftpClient.getReplyString());

			String prepareFilename = remoteFilename + ".prepare";
			ftpClient.deleteFile(prepareFilename);
			return rc;
		} catch (Exception e) {
			log.error("failed to upload file");
			log.error(e.getMessage() + e.getLocalizedMessage());
			return false;
		}
	}

	public boolean downloadFile(String remoteFilename, OutputStream downloadStream) {
		try {
			if (!initConnection()) {
				return false;
				// transfer file
			}
			boolean download_succeeded = ftpClient.retrieveFile(remoteFilename, downloadStream);
			if (download_succeeded) {
				log.debug("download succeeded");
				return download_succeeded;
			} else {
				return wait_for_upload(remoteFilename, downloadStream);
			}
		} catch (Exception e) {
			log.error("downlaod failed");
			log.error(e.getMessage());
			return false;
		}
	}

	private boolean wait_for_upload(String remoteFilename, OutputStream downloadStream)
			throws IOException, InterruptedException {
		// check if the file would be downloaded by another process
		String prepareFilename = remoteFilename + ".prepare";
		OutputStream prepareOutputStream = new ByteArrayOutputStream();

		long max_wait_time = 1000 * 5 * fileProxyMaxCacheWaitTime; // 5Min
		long sleeptime = max_wait_time / 20;
		if (sleeptime > 30000) {
			sleeptime = 30000;
		}
		boolean rc;
		while ((rc = ftpClient.retrieveFile(prepareFilename, prepareOutputStream)) && (max_wait_time > 0)) {
			max_wait_time = max_wait_time - sleeptime;
			log.info("Wait " + (max_wait_time / 1000) + " seconds, that the file would be uploaded to fileproxy");
			Thread.sleep(sleeptime);
		}
		if (rc == true) {
			ftpClient.deleteFile(prepareFilename);
		} else {
			rc = ftpClient.retrieveFile(remoteFilename, downloadStream);
			if (rc) {
				return true;
			} else {
				// not downloaded and no preparefile so this may be the first on
				ftpClient.storeFile(prepareFilename, new ByteArrayInputStream("prepare".getBytes()));
				return false;
			}
		}
		return false;
	}

}
