/*
 * Copyright (C) 2010-2014 Laurent CLOUET
 * Author Laurent CLOUET <laurent.clouet@nopnop.net>
 *
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.sheepit.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.sheepit.client.datamodel.CacheFileMD5;
import com.sheepit.client.datamodel.FileMD5;
import com.sheepit.client.datamodel.HeartBeatInfos;
import com.sheepit.client.datamodel.JobInfos;
import com.sheepit.client.datamodel.JobValidation;
import com.sheepit.client.datamodel.RequestEndPoint;
import com.sheepit.client.datamodel.ServerConfig;
import lombok.Getter;
import org.simpleframework.xml.core.Persister;

import com.sheepit.client.Configuration.ComputeType;
import com.sheepit.client.Error.ServerCode;
import com.sheepit.client.exception.FermeException;
import com.sheepit.client.exception.FermeExceptionBadResponseFromServer;
import com.sheepit.client.exception.FermeExceptionNoRendererAvailable;
import com.sheepit.client.exception.FermeExceptionNoRightToRender;
import com.sheepit.client.exception.FermeExceptionNoSession;
import com.sheepit.client.exception.FermeExceptionNoSpaceLeftOnDevice;
import com.sheepit.client.exception.FermeExceptionServerInMaintenance;
import com.sheepit.client.exception.FermeExceptionServerOverloaded;
import com.sheepit.client.exception.FermeExceptionSessionDisabled;
import com.sheepit.client.exception.FermeServerDown;
import com.sheepit.client.os.OS;

public class Server extends Thread implements HostnameVerifier, X509TrustManager {
	private String base_url;

	@Getter
	private ServerConfig serverConfig;

	private Configuration user_config;
	private Client client;
	private Log log;
	private long lastRequestTime;
	private int keepmealive_duration; // time in ms
	
	public Server(String url_, Configuration user_config_, Client client_) {
		super();
		this.base_url = url_;
		this.user_config = user_config_;
		this.client = client_;
		this.log = Log.getInstance(this.user_config);
		this.lastRequestTime = 0;
		this.keepmealive_duration = 15 * 60 * 1000; // default 15min
		
		CookieManager cookies = new CookieManager();
		CookieHandler.setDefault(cookies);
	}
	
	public void run() {
		this.stayAlive();
	}
	
	public void stayAlive() {
		while (true) {
			long current_time = new Date().getTime();
			if ((current_time - this.lastRequestTime) > this.keepmealive_duration) {
				try {
					String args = "";
					if (this.client != null && this.client.getRenderingJob() != null) {
						args = "?frame=" + this.client.getRenderingJob().getFrameNumber() + "&job=" + this.client.getRenderingJob().getId();
						if (this.client.getRenderingJob().getExtras() != null && this.client.getRenderingJob().getExtras().isEmpty() == false) {
							args += "&extras=" + this.client.getRenderingJob().getExtras();
						}
						if (this.client.getRenderingJob().getProcessRender() != null) {
							args += "&rendertime=" + this.client.getRenderingJob().getProcessRender().getDuration();
							args += "&remainingtime=" + this.client.getRenderingJob().getProcessRender().getRemainingDuration();
						}
					}
					
					HttpURLConnection connection = this.HTTPRequest(this.getPage("keepmealive") + args);
					
					if (connection.getResponseCode() == HttpURLConnection.HTTP_OK && connection.getContentType().startsWith("text/xml")) {
						DataInputStream in = new DataInputStream(connection.getInputStream());
						try {
							HeartBeatInfos heartBeartInfos = new Persister().read(HeartBeatInfos.class, in);
							ServerCode serverCode = ServerCode.fromInt(heartBeartInfos.getStatus());
							if (serverCode == ServerCode.KEEPMEALIVE_STOP_RENDERING) {
								this.log.debug("Server::stayAlive server asked to kill local render process");
								// kill the current process, it will generate an error but it's okay
								if (this.client != null && this.client.getRenderingJob() != null && this.client.getRenderingJob().getProcessRender().getProcess() != null) {
									this.client.getRenderingJob().setServerBlockJob(true);
									this.client.getRenderingJob().setAskForRendererKill(true);
									OS.getOS().kill(this.client.getRenderingJob().getProcessRender().getProcess());
								}
							}
						}
						catch (Exception e) { // for the read
							this.log.debug("Server::stayAlive Exception " + e);
						}
					}
				}
				catch (NoRouteToHostException e) {
					this.log.debug("Server::stayAlive can not connect to server");
				}
				catch (IOException e) {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					this.log.debug("Server::stayAlive IOException " + e + " stacktrace: " + sw.toString());
				}
			}
			try {
				Thread.sleep(60 * 1000); // 1min
			}
			catch (InterruptedException e) {
				return;
			}
			catch (Exception e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				this.log.debug("Server::stayAlive Exception " + e + " stacktrace: " + sw.toString());
			}
		}
	}
	
	public String toString() {
		return String.format("Server (base_url '%s', user_config %s", this.base_url, this.user_config);
	}
	
	public Error.Type getConfiguration() {
		OS os = OS.getOS();
		HttpURLConnection connection = null;
		String publickey = null;
		try {
			String url_remote = this.base_url + "/server/config.php";
			String parameters = String.format("login=%s&password=%s&cpu_family=%s&cpu_model=%s&cpu_model_name=%s&cpu_cores=%s&os=%s&ram=%s&bits=%s&version=%s&hostname=%s&ui=%s&extras=%s",
				URLEncoder.encode(this.user_config.getLogin(), "UTF-8"),
				URLEncoder.encode(this.user_config.getPassword(), "UTF-8"),
				URLEncoder.encode(os.getCPU().family(), "UTF-8"),
				URLEncoder.encode(os.getCPU().model(), "UTF-8"),
				URLEncoder.encode(os.getCPU().name(), "UTF-8"),
				((this.user_config.getNbCores() == -1) ? os.getCPU().cores() : this.user_config.getNbCores()),
				URLEncoder.encode(os.name(), "UTF-8"),
				os.getMemory(),
				URLEncoder.encode(os.getCPU().arch(), "UTF-8"),
				this.user_config.getJarVersion(),
				URLEncoder.encode(this.user_config.getHostname(), "UTF-8"),
				this.client.getGui().getClass().getSimpleName(),
				this.user_config.getExtras());
			this.log.debug("Server::getConfiguration url " + url_remote);
			
			connection = this.HTTPRequest(url_remote, parameters);
			int r = connection.getResponseCode();
			String contentType = connection.getContentType();
			
			if (r == HttpURLConnection.HTTP_OK && contentType.startsWith("text/xml")) {
				DataInputStream in = new DataInputStream(connection.getInputStream());
				serverConfig = new Persister().read(ServerConfig.class, in);
				
				if (ServerCode.fromInt(serverConfig.getStatus()) != ServerCode.OK) {
					return Error.ServerCodeToType(ServerCode.fromInt(serverConfig.getStatus()));
				}
				
				publickey = serverConfig.getPublickey();
				if (publickey.isEmpty()) {
					publickey = null;
				}
				else {
					this.user_config.setPassword(publickey);
				}
			}
		}
		catch (ConnectException e) {
			this.log.error("Server::getConfiguration error ConnectException " + e);
			return Error.Type.NETWORK_ISSUE;
		}
		catch (UnknownHostException e) {
			this.log.error("Server::getConfiguration: exception UnknownHostException " + e);
			return Error.Type.NETWORK_ISSUE;
		}
		catch (UnsupportedEncodingException e) {
			this.log.error("Server::getConfiguration: exception UnsupportedEncodingException " + e);
			return Error.Type.UNKNOWN;
		}
		catch (IOException e) {
			this.log.error("Server::getConfiguration: exception IOException " + e);
			return Error.Type.UNKNOWN;
		}
		catch (Exception e) {
			this.log.error("Server::getConfiguration: exception Exception " + e);
			return Error.Type.UNKNOWN;
		}
		finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		
		this.client.getGui().successfulAuthenticationEvent(publickey);
		
		return Error.Type.OK;
	}
	
	public Job requestJob() throws FermeException {
		this.log.debug("Server::requestJob");
		String url_contents = "";
		
		HttpURLConnection connection = null;
		try {
			OS os = OS.getOS();
			long maxMemory = this.user_config.getMaxMemory();
			long freeMemory = os.getFreeMemory();
			if (maxMemory < 0) {
				maxMemory = freeMemory;
			}
			else if (freeMemory > 0 && maxMemory > 0) {
				maxMemory = Math.min(maxMemory, freeMemory);
			}
			String url = String.format("%s?computemethod=%s&cpu_cores=%s&ram_max=%s&rendertime_max=%s", this.getPage("request-job"), this.user_config.computeMethodToInt(), ((this.user_config.getNbCores() == -1) ? os.getCPU().cores() : this.user_config.getNbCores()), maxMemory, this.user_config.getMaxRenderTime());
			if (this.user_config.getComputeMethod() != ComputeType.CPU && this.user_config.getGPUDevice() != null) {
				String gpu_model = "";
				try {
					gpu_model = URLEncoder.encode(this.user_config.getGPUDevice().getModel(), "UTF-8");
				}
				catch (UnsupportedEncodingException e) {
				}
				url += "&gpu_model=" + gpu_model + "&gpu_ram=" + this.user_config.getGPUDevice().getMemory() + "&gpu_type=" + this.user_config.getGPUDevice().getType();
			}
			
			connection = this.HTTPRequest(url, this.generateXMLForMD5cache());
			
			int r = connection.getResponseCode();
			String contentType = connection.getContentType();
			
			if (r == HttpURLConnection.HTTP_OK && contentType.startsWith("text/xml")) {
				DataInputStream in = new DataInputStream(connection.getInputStream());

				JobInfos jobData = new Persister().read(JobInfos.class, in);

				handleFileMD5DeleteDocument(jobData.getFileMD5s());
				
				if (jobData.getSessionStats() != null) {
					this.client.getGui().displayStats(new Stats(
							jobData.getSessionStats().getRemainingFrames(),
							jobData.getSessionStats().getPointsEarnedByUser(),
							jobData.getSessionStats().getPointsEarnedOnSession(),
							jobData.getSessionStats().getRenderableProjects(),
							jobData.getSessionStats().getWaitingProjects(),
							jobData.getSessionStats().getConnectedMachines()));
				}

				ServerCode serverCode = ServerCode.fromInt(jobData.getStatus());
				if (serverCode != ServerCode.OK) {
					switch (serverCode) {
						case JOB_REQUEST_NOJOB:
							return null;
						case JOB_REQUEST_ERROR_NO_RENDERING_RIGHT:
							throw new FermeExceptionNoRightToRender();
						case JOB_REQUEST_ERROR_DEAD_SESSION:
							throw new FermeExceptionNoSession();
						case JOB_REQUEST_ERROR_SESSION_DISABLED:
							throw new FermeExceptionSessionDisabled();
						case JOB_REQUEST_ERROR_INTERNAL_ERROR:
							throw new FermeExceptionBadResponseFromServer();
						case JOB_REQUEST_ERROR_RENDERER_NOT_AVAILABLE:
							throw new FermeExceptionNoRendererAvailable();
						case JOB_REQUEST_SERVER_IN_MAINTENANCE:
							throw new FermeExceptionServerInMaintenance();
						case JOB_REQUEST_SERVER_OVERLOADED:
							throw new FermeExceptionServerOverloaded();
						default:
							throw new FermeException("error requestJob: status is not ok (it's " + serverCode + ")");
					}
				}

				String script = "import bpy\n";
				// blender 2.7x
				script += "try:\n";
				script += "\tbpy.context.user_preferences.filepaths.temporary_directory = \"" + this.user_config.getWorkingDirectory().getAbsolutePath().replace("\\", "\\\\") + "\"\n";
				script += "except AttributeError:\n";
				script += "\tpass\n";
				
				// blender 2.80
				script += "try:\n";
				script += "\tbpy.context.preferences.filepaths.temporary_directory = \"" + this.user_config.getWorkingDirectory().getAbsolutePath().replace("\\", "\\\\") + "\"\n";
				script += "except AttributeError:\n";
				script += "\tpass\n";
				
				script += jobData.getRenderTask().getScript();

				String validationUrl = URLDecoder.decode(jobData.getRenderTask().getValidationUrl());

				Job a_job = new Job(
						this.user_config,
						this.client.getGui(),
						this.client.getLog(),
						jobData.getRenderTask().getId(),
						jobData.getRenderTask().getFrame(),
						jobData.getRenderTask().getPath().replace("/", File.separator),
						jobData.getRenderTask().getUseGpu() == 1,
						jobData.getRenderTask().getRendererInfos().getCommandline(),
						validationUrl,
						script,
						jobData.getRenderTask().getArchive_md5(),
						jobData.getRenderTask().getRendererInfos().getMd5(),
						jobData.getRenderTask().getName(),
						jobData.getRenderTask().getPassword(),
						jobData.getRenderTask().getExtras(),
						jobData.getRenderTask().getSynchronous_upload().equals("1"),
						jobData.getRenderTask().getRendererInfos().getUpdate_method()
				);
				
				return a_job;
			}
			else {
				System.out.println("Server::requestJob url " + url_contents + " r " + r + " contentType " + contentType);
				if (r == HttpURLConnection.HTTP_UNAVAILABLE || r == HttpURLConnection. HTTP_CLIENT_TIMEOUT) {
					// most likely varnish is up but apache down
					throw new FermeServerDown();
				}
				else if (r == HttpURLConnection.HTTP_OK && contentType.startsWith("text/html")) {
					throw new FermeExceptionBadResponseFromServer();
				}
				InputStream in = connection.getInputStream();
				String line;
				BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
				while ((line = reader.readLine()) != null) {
					System.out.print(line);
				}
				System.out.println("");
			}
		}
		catch (FermeException e) {
			throw e;
		}
		catch (NoRouteToHostException e) {
			throw new FermeServerDown();
		}
		catch (UnknownHostException e) {
			throw new FermeServerDown();
		}
		catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			throw new FermeException("error requestJob: unknown exception " + e + " stacktrace: " + sw.toString());
		}
		finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		throw new FermeException("error requestJob, end of function");
	}

	public HttpURLConnection HTTPRequest(String url_) throws IOException {
		return this.HTTPRequest(url_, null);
	}
	
	public HttpURLConnection HTTPRequest(String url_, String data_) throws IOException {
		this.log.debug("Server::HTTPRequest url(" + url_ + ")");
		HttpURLConnection connection = null;
		URL url = new URL(url_);
		
		connection = (HttpURLConnection) url.openConnection();
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setInstanceFollowRedirects(true);
		connection.setRequestMethod("GET");
		
		if (url_.startsWith("https://")) {
			try {
				SSLContext sc;
				sc = SSLContext.getInstance("SSL");
				sc.init(null, new TrustManager[] { this }, null);
				SSLSocketFactory factory = sc.getSocketFactory();
				((HttpsURLConnection) connection).setSSLSocketFactory(factory);
				((HttpsURLConnection) connection).setHostnameVerifier(this);
			}
			catch (NoSuchAlgorithmException e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				this.log.debug("Server::HTTPRequest NoSuchAlgorithmException " + e + " stacktrace: " + sw.toString());
				return null;
			}
			catch (KeyManagementException e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				this.log.debug("Server::HTTPRequest KeyManagementException " + e + " stacktrace: " + sw.toString());
				return null;
			}
		}
		
		if (data_ != null) {
			connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
			connection.setRequestMethod("POST");
			OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
			out.write(data_);
			out.flush();
			out.close();
		}
		
		// actually use the connection to, in case of timeout, generate an exception
		connection.getResponseCode();
		
		this.lastRequestTime = new Date().getTime();
		
		return connection;
	}
	
	public int HTTPGetFile(String url_, String destination_, Gui gui_, String status_) throws FermeExceptionNoSpaceLeftOnDevice {
		// the destination_ parent directory must exist
		try {
			HttpURLConnection httpCon = this.HTTPRequest(url_);
			
			InputStream inStrm = httpCon.getInputStream();
			if (httpCon.getResponseCode() != HttpURLConnection.HTTP_OK) {
				this.log.error("Server::HTTPGetFile(" + url_ + ", ...) HTTP code is not " + HttpURLConnection.HTTP_OK + " it's " + httpCon.getResponseCode());
				return -1;
			}
			int size = httpCon.getContentLength();
			long start = new Date().getTime();
			
			FileOutputStream fos = new FileOutputStream(destination_);
			byte[] ch = new byte[512 * 1024];
			int nb;
			long written = 0;
			long last_gui_update = 0; // size in byte
			while ((nb = inStrm.read(ch)) != -1) {
				fos.write(ch, 0, nb);
				written += nb;
				if ((written - last_gui_update) > 1000000) { // only update the gui every 1MB
					gui_.status(String.format(status_, (int) (100.0 * written / size)));
					last_gui_update = written;
				}
			}
			fos.close();
			inStrm.close();
			gui_.status(String.format(status_, 100));
			long end = new Date().getTime();
			this.log.debug(String.format("File downloaded at %.1f kB/s, written %d B", ((float) (size / 1000)) / ((float) (end - start) / 1000), written));
			this.lastRequestTime = new Date().getTime();
			return 0;
		}
		catch (Exception e) {
			if (Utils.noFreeSpaceOnDisk(new File(destination_).getParent())) {
				throw new FermeExceptionNoSpaceLeftOnDevice();
			}
			
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			this.log.error("Server::HTTPGetFile Exception " + e + " stacktrace " + sw.toString());
		}
		this.log.debug("Server::HTTPGetFile(" + url_ + ", ...) will failed (end of function)");
		return -2;
	}
	
	public ServerCode HTTPSendFile(String surl, String file1) {
		this.log.debug("Server::HTTPSendFile(" + surl + "," + file1 + ")");
		
		HttpURLConnection conn = null;
		DataOutputStream dos = null;
		BufferedReader inStream = null;
		
		String exsistingFileName = file1;
		File fFile2Snd = new File(exsistingFileName);
		
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "***232404jkg4220957934FW**";
		
		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1 * 1024 * 1024;
		
		String urlString = surl;
		
		try {
			FileInputStream fileInputStream = new FileInputStream(new File(exsistingFileName));
			URL url = new URL(urlString);
			
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(true);
			conn.setUseCaches(false);
			
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
			
			if (urlString.startsWith("https://")) {
				try {
					SSLContext sc;
					sc = SSLContext.getInstance("SSL");
					sc.init(null, new TrustManager[] { this }, null);
					SSLSocketFactory factory = sc.getSocketFactory();
					((HttpsURLConnection) conn).setSSLSocketFactory(factory);
					((HttpsURLConnection) conn).setHostnameVerifier(this);
				}
				catch (NoSuchAlgorithmException e) {
					this.log.error("Server::HTTPSendFile, exception NoSuchAlgorithmException " + e);
					try {
						fileInputStream.close();
					}
					catch (Exception e1) {
						
					}
					return ServerCode.UNKNOWN;
				}
				catch (KeyManagementException e) {
					this.log.error("Server::HTTPSendFile, exception KeyManagementException " + e);
					try {
						fileInputStream.close();
					}
					catch (Exception e1) {
						
					}
					return ServerCode.UNKNOWN;
				}
			}
			
			dos = new DataOutputStream(conn.getOutputStream());
			dos.writeBytes(twoHyphens + boundary + lineEnd);
			dos.writeBytes("Content-Disposition: form-data; name=\"file\";" + " filename=\"" + fFile2Snd.getName() + "\"" + lineEnd);
			dos.writeBytes(lineEnd);
			
			bytesAvailable = fileInputStream.available();
			bufferSize = Math.min(bytesAvailable, maxBufferSize);
			buffer = new byte[bufferSize];
			
			bytesRead = fileInputStream.read(buffer, 0, bufferSize);
			
			while (bytesRead > 0) {
				dos.write(buffer, 0, bufferSize);
				bytesAvailable = fileInputStream.available();
				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);
			}
			
			dos.writeBytes(lineEnd);
			dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
			fileInputStream.close();
			dos.flush();
			dos.close();
		}
		catch (MalformedURLException e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			this.log.error("Server::HTTPSendFile, MalformedURLException " + e + " stacktrace " + sw.toString());
			return ServerCode.UNKNOWN;
		}
		catch (IOException e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			this.log.error("Server::HTTPSendFile, IOException " + e + " stacktrace " + sw.toString());
			return ServerCode.UNKNOWN;
		}
		catch (OutOfMemoryError e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			this.log.error("Server::HTTPSendFile, OutOfMemoryError " + e + " stacktrace " + sw.toString());
			return ServerCode.JOB_VALIDATION_ERROR_UPLOAD_FAILED;
		}
		catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			this.log.error("Server::HTTPSendFile, Exception " + e + " stacktrace " + sw.toString());
			return ServerCode.UNKNOWN;
		}
		
		int r;
		try {
			r = conn.getResponseCode();
		}
		catch (IOException e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			this.log.debug("Server::HTTPSendFile IOException " + e + " stacktrace: " + sw.toString());
			return ServerCode.UNKNOWN;
		}
		String contentType = conn.getContentType();
		
		if (r == HttpURLConnection.HTTP_OK && contentType.startsWith("text/xml")) {
			DataInputStream in;
			try {
				in = new DataInputStream(conn.getInputStream());
			}
			catch (IOException e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				this.log.debug("Server::HTTPSendFile IOException " + e + " stacktrace: " + sw.toString());
				return ServerCode.UNKNOWN;
			}

			try {
				JobValidation jobValidation = new Persister().read(JobValidation.class, in);
				
				this.lastRequestTime = new Date().getTime();

				ServerCode serverCode = ServerCode.fromInt(jobValidation.getStatus());
				if (serverCode != ServerCode.OK) {
					this.log.error("Server::HTTPSendFile wrong status (is " + serverCode + ")");
					return serverCode;
				}
			}
			catch (Exception e) { // for the .read
				e.printStackTrace();
			}
			
			return ServerCode.OK;
		}
		else if (r == HttpURLConnection.HTTP_OK && contentType.startsWith("text/html")) {
			return ServerCode.ERROR_BAD_RESPONSE;
		}
		else {
			try {
				inStream = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				
				String str;
				while ((str = inStream.readLine()) != null) {
					System.out.println(str);
					System.out.println("");
				}
				inStream.close();
			}
			catch (IOException ioex) {
			}
		}
		return ServerCode.UNKNOWN;
	}
	
	private String generateXMLForMD5cache() {
		List<FileMD5> md5s = new ArrayList<>();
		for (File local_file : this.user_config.getLocalCacheFiles()) {
			try {
				String extension = local_file.getName().substring(local_file.getName().lastIndexOf('.')).toLowerCase();
				String name = local_file.getName().substring(0, local_file.getName().length() - 1 * extension.length());
				if (extension.equals(".zip")) {
					// node_file.setAttribute("md5", name);
					FileMD5 fileMD5 = new FileMD5();
					fileMD5.setMd5(name);
					
					md5s.add(fileMD5);
				}
			}
			catch (StringIndexOutOfBoundsException e) { // because the file does not have an . its path
			}
		}

		CacheFileMD5 cache = new CacheFileMD5();
		cache.setMd5s(md5s);

		final Persister persister = new Persister();
		try (StringWriter writer = new StringWriter()) {
			persister.write(cache, writer);
			return writer.toString();
		}
		catch (final Exception e) {
			log.debug("Failed to dump md5s " + e);
			return "";
		}
	}

	private void handleFileMD5DeleteDocument(List<FileMD5> fileMD5s) {
		if (fileMD5s != null && fileMD5s.isEmpty() == false) {
			for(FileMD5 fileMD5 : fileMD5s) {
				if ("delete".equals(fileMD5.getAction()) && fileMD5.getMd5() != null && fileMD5.getMd5().isEmpty() == false) {
					String path = this.user_config.getWorkingDirectory().getAbsolutePath() + File.separatorChar + fileMD5.getMd5();
					this.log.debug("Server::handleFileMD5DeleteDocument delete old file " + path);
					File file_to_delete = new File(path + ".zip");
					file_to_delete.delete();
					Utils.delete(new File(path));
				}
			}
		}
	}
	
	public String getPage(String key) {
		if (this.serverConfig != null) {
			RequestEndPoint endpoint = this.serverConfig.getRequestEndPoint(key);
			if (endpoint != null) {
				return this.base_url + endpoint.getPath();
			}
		}
		return "";
	}
	
	@Override
	public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
	}
	
	@Override
	public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
	}
	
	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return null;
	}
	
	@Override
	public boolean verify(String arg0, SSLSession arg1) {
		return true; // trust every ssl certificate
	}
}
