package com.sheepit.client;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
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
	private int fileProxyMaxCacheWaitTime;
	
	
	
	private FTPClient ftpClient;

	public FileProxy(Configuration config) {
		this.fileProxyUrl = config.getFileProxyUrl();
		this.fileProxyPort = config.getFileProxyPort();
		this.fileProxyUser = config.getFileProxyUser();
		this.fileProxyPaswd = config.getFileProxyPasswd();
		this.fileProxyMaxCacheWaitTime = config.getFileProxyMaxCacheWaitTime();
		
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

			String prepareFilename = remoteFilename + ".prepare";
			ftpClient.deleteFile(prepareFilename);
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
			boolean download_succeeded = ftpClient.retrieveFile(remoteFilename, downloadStream);
			log.debug("download " + remoteFilename + ": " + download_succeeded);
			if(download_succeeded){
				return download_succeeded;
			}
			else{
				return wait_for_upload(remoteFilename, downloadStream);
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			return false;
		}
	}
	
	private boolean wait_for_upload(String remoteFilename, OutputStream downloadStream) throws IOException, InterruptedException{
		//check if the file would be downloaded by another process
		String prepareFilename = remoteFilename + ".prepare";
		OutputStream prepareOutputStream = new ByteArrayOutputStream();
		
		long max_wait_time = 1000 * 5 * fileProxyMaxCacheWaitTime; // 5Min
		long sleeptime = 10000;
		boolean rc ;
		while((rc = ftpClient.retrieveFile(prepareFilename, prepareOutputStream)) && (max_wait_time > 0)){
			max_wait_time = max_wait_time - sleeptime ;
			Thread.sleep(sleeptime );
		}
		if (rc == true){
			ftpClient.deleteFile(prepareFilename);
		}
		else{
			rc = ftpClient.retrieveFile(remoteFilename, downloadStream);
			if(rc){
				return true;
			}
			else{
				// not downloaded and no preparefile so this may be the first on
				ftpClient.storeFile(prepareFilename, new ByteArrayInputStream("prepare".getBytes()));
				return false;
			}
		}
		return false;
	}

}
