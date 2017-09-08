package com.sheepit.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public class FileProxy {

	private String fileProxyUrl;
	private int fileProxyPort = -1;
	private String fileProxyUser;
	private String fileProxyPaswd;
	private Log log;
	
	
	
	private FTPClient ftpClient;

	public FileProxy(Configuration config) {
		this.fileProxyUrl = config.getFileProxyUrl();
		this.fileProxyPort = config.getFileProxyPort();
		this.fileProxyUser = config.getFileProxyUser();
		this.fileProxyPaswd = config.getFileProxyPasswd();
		
		this.log = Log.getInstance(config);
		
		if(fileProxyPort == -1){
			fileProxyPort = 21;
		}
		
		this.ftpClient = new FTPClient();

	}

	private boolean initConnection() throws SocketException, IOException {
		if (fileProxyUrl == null) {
			log.error("noUrl");
			return false;
		}
		if(ftpClient.isConnected()){
			log.debug("is connected");
			return true;
		}

		ftpClient.connect(fileProxyUrl, fileProxyPort);
		ftpClient.login(this.fileProxyUser, this.fileProxyPaswd);
		int replyCode = ftpClient.getReplyCode();
		if (!FTPReply.isPositiveCompletion(replyCode)) {
			log.error("not connected");
			ftpClient.disconnect();
			return false;
		}
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		ftpClient.enterLocalPassiveMode();		
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
			log.debug( "staret upload");
			boolean rc = ftpClient.storeFile(remoteFilename, uploadStream);
			log.debug("upload " + remoteFilename + ": " + rc);
			log.debug(ftpClient.getReplyString());
			return rc;
		} catch (Exception e) {
			log.error( e.getMessage() + e.getLocalizedMessage());
			return false;
		}
	}

	public boolean downloadFile(String remoteFilename, OutputStream downloadStream) {
		try {
			if (!initConnection()) {
				return false;
				// transfer file
			}
			boolean rc = ftpClient.retrieveFile(remoteFilename, downloadStream);
			log.debug("download " + remoteFilename + ": " + rc);
			return rc;
		} catch (Exception e) {
			log.error(e.getMessage());
			return false;
		}
	}

}
