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

	private FTPClient ftpClient;

	public FileProxy(String fileProxyUrl) {
		this.fileProxyUrl = fileProxyUrl;
		this.ftpClient = new FTPClient();

	}

	private boolean initConnection() throws SocketException, IOException {
		if (fileProxyUrl == null) {
			System.out.println("noUrl");
			return false;
		}
		if(ftpClient.isConnected()){
			System.out.println("is connected");
			return true;
		}

		ftpClient.connect(fileProxyUrl);
		ftpClient.login("bob", "bob");
		int replyCode = ftpClient.getReplyCode();
		if (!FTPReply.isPositiveCompletion(replyCode)) {
			System.out.println("not connected");
			ftpClient.disconnect();
			return false;
		}
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		ftpClient.enterLocalPassiveMode();		
		System.out.println("connected");
		return true;
	}

	public boolean uploadFile(String remoteFilename, InputStream uploadStream) {
		try {
			System.out.println("init connection");
			if (!this.initConnection()) {
				System.out.println("faild to init connection");
				return false;
				// transfer file
			}
			System.out.println( "staret upload");
			boolean rc = ftpClient.storeFile(remoteFilename, uploadStream);
			System.out.println("upload " + remoteFilename + ": " + rc);
			System.out.println(ftpClient.getReplyString());
			return rc;
		} catch (Exception e) {
			System.out.println("error: " + e.getMessage() + e.getLocalizedMessage());
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
			System.out.println(rc);
			return rc;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return false;
		}
	}

}
