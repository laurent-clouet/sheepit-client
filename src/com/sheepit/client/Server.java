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
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
	private Configuration user_config;
	private Client client;
	private HashMap<String, String> pages;
	private Log log;
	private long lastRequestTime;
	private int keepmealive_duration; // time in ms
	
	public Server(String url_, Configuration user_config_, Client client_) {
		super();
		this.base_url = url_;
		this.user_config = user_config_;
		this.client = client_;
		this.pages = new HashMap<String, String>();
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
							Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
							ServerCode ret = Utils.statusIsOK(document, "keepmealive");
							if (ret == ServerCode.KEEPMEALIVE_STOP_RENDERING) {
								this.log.debug("Server::stayAlive server asked to kill local render process");
								// kill the current process, it will generate an error but it's okay
								if (this.client != null && this.client.getRenderingJob() != null && this.client.getRenderingJob().getProcessRender().getProcess() != null) {
									this.client.getRenderingJob().setServerBlockJob(true);
									this.client.getRenderingJob().setAskForRendererKill(true);
									OS.getOS().kill(this.client.getRenderingJob().getProcessRender().getProcess());
								}
							}
						}
						catch (SAXException e) {
						}
						catch (ParserConfigurationException e) {
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
		return String.format("Server (base_url '%s', user_config %s, pages %s", this.base_url, this.user_config, this.pages);
	}
	
	public Error.Type getConfiguration() {
		OS os = OS.getOS();
		HttpURLConnection connection = null;
		try {
			String url_remote = this.base_url + "/server/config.php";
			String parameters = String.format("login=%s&password=%s&cpu_family=%s&cpu_model=%s&cpu_model_name=%s&cpu_cores=%s&os=%s&ram=%s&bits=%s&version=%s&hostname=%s&extras=%s", 
				URLEncoder.encode(this.user_config.login(), "UTF-8"),
				URLEncoder.encode(this.user_config.password(), "UTF-8"),
				URLEncoder.encode(os.getCPU().family(), "UTF-8"),
				URLEncoder.encode(os.getCPU().model(), "UTF-8"),
				URLEncoder.encode(os.getCPU().name(), "UTF-8"),
				((this.user_config.getNbCores() == -1) ? os.getCPU().cores() : this.user_config.getNbCores()),
				URLEncoder.encode(os.name(), "UTF-8"),
				os.getMemory(),
				URLEncoder.encode(os.getCPU().arch(), "UTF-8"),
				this.user_config.getJarVersion(),
				URLEncoder.encode(this.user_config.getHostname(), "UTF-8"),
				this.user_config.getExtras());
			this.log.debug("Server::getConfiguration url " + url_remote);
			
			connection = this.HTTPRequest(url_remote, parameters);
			int r = connection.getResponseCode();
			String contentType = connection.getContentType();
			
			if (r == HttpURLConnection.HTTP_OK && contentType.startsWith("text/xml")) {
				DataInputStream in = new DataInputStream(connection.getInputStream());
				Document document = null;
				
				try {
					document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
				}
				catch (SAXException e) {
					this.log.error("getConfiguration error: failed to parse XML SAXException " + e);
					return Error.Type.WRONG_CONFIGURATION;
				}
				catch (IOException e) {
					this.log.error("getConfiguration error: failed to parse XML IOException " + e);
					return Error.Type.WRONG_CONFIGURATION;
				}
				catch (ParserConfigurationException e) {
					this.log.error("getConfiguration error: failed to parse XML ParserConfigurationException " + e);
					return Error.Type.WRONG_CONFIGURATION;
				}
				
				ServerCode ret = Utils.statusIsOK(document, "config");
				if (ret != ServerCode.OK) {
					return Error.ServerCodeToType(ret);
				}
				
				Element config_node = null;
				NodeList ns = null;
				ns = document.getElementsByTagName("config");
				if (ns.getLength() == 0) {
					this.log.error("getConfiguration error: failed to parse XML, no node 'config'");
					return Error.Type.WRONG_CONFIGURATION;
				}
				config_node = (Element) ns.item(0);
				
				ns = config_node.getElementsByTagName("request");
				if (ns.getLength() == 0) {
					this.log.error("getConfiguration error: failed to parse XML, node 'config' has no child node 'request'");
					return Error.Type.WRONG_CONFIGURATION;
				}
				for (int i = 0; i < ns.getLength(); i++) {
					Element element = (Element) ns.item(i);
					if (element.hasAttribute("type") && element.hasAttribute("path")) {
						this.pages.put(element.getAttribute("type"), element.getAttribute("path"));
						if (element.getAttribute("type").equals("keepmealive") && element.hasAttribute("max-period")) {
							this.keepmealive_duration = (Integer.parseInt(element.getAttribute("max-period")) - 120) * 1000; // put 2min of safety net
						}
					}
				}
			}
			else if (r == HttpURLConnection.HTTP_OK && contentType.startsWith("text/html")) {
				return Error.Type.ERROR_BAD_RESPONSE;
			}
			else {
				this.log.error("Server::getConfiguration: Invalid response " + contentType + " " + r);
				return Error.Type.WRONG_CONFIGURATION;
			}
		}
		catch (ConnectException e) {
			this.log.error("Server::getConfiguration error ConnectException " + e);
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
		finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		return Error.Type.OK;
	}
	
	public Job requestJob() throws FermeException {
		this.log.debug("Server::requestJob");
		String url_contents = "";
		
		HttpURLConnection connection = null;
		try {
			OS os = OS.getOS();
			int maxMemory = this.user_config.getMaxMemory();
			int freeMemory = os.getFreeMemory();
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
				Document document = null;
				try {
					document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
				}
				catch (SAXException e) {
					throw new FermeException("error requestJob: parseXML failed, SAXException " + e);
				}
				catch (IOException e) {
					throw new FermeException("error requestJob: parseXML failed IOException " + e);
				}
				catch (ParserConfigurationException e) {
					throw new FermeException("error requestJob: parseXML failed ParserConfigurationException " + e);
				}
				
				ServerCode ret = Utils.statusIsOK(document, "jobrequest");
				if (ret != ServerCode.OK) {
					if (ret == ServerCode.JOB_REQUEST_NOJOB) {
						handleFileMD5DeleteDocument(document, "jobrequest");
						return null;
					}
					else if (ret == ServerCode.JOB_REQUEST_ERROR_NO_RENDERING_RIGHT) {
						throw new FermeExceptionNoRightToRender();
					}
					else if (ret == ServerCode.JOB_REQUEST_ERROR_DEAD_SESSION) {
						throw new FermeExceptionNoSession();
					}
					else if (ret == ServerCode.JOB_REQUEST_ERROR_RENDERER_NOT_AVAILABLE) {
						throw new FermeExceptionNoRendererAvailable();
					}
					else if (ret == ServerCode.JOB_REQUEST_ERROR_SESSION_DISABLED) {
						throw new FermeExceptionSessionDisabled();
					}
					else if (ret == ServerCode.JOB_REQUEST_SERVER_IN_MAINTENANCE) {
						throw new FermeExceptionServerInMaintenance();
					}
					else if (ret == ServerCode.JOB_REQUEST_SERVER_OVERLOADED) {
						throw new FermeExceptionServerOverloaded();
					}
					this.log.error("Server::requestJob: Utils.statusIsOK(document, 'jobrequest') -> ret " + ret);
					throw new FermeException("error requestJob: status is not ok (it's " + ret + ")");
				}
				
				handleFileMD5DeleteDocument(document, "jobrequest");
				
				Element a_node = null;
				NodeList ns = null;
				
				ns = document.getElementsByTagName("stats");
				if (ns.getLength() == 0) {
					throw new FermeException("error requestJob: parseXML failed, no 'frame' node");
				}
				a_node = (Element) ns.item(0);
				
				int remaining_frames = 0;
				int credits_earned = 0;
				int credits_earned_session = 0;
				int waiting_project = 0;
				int connected_machine = 0;
				if (a_node.hasAttribute("frame_remaining") && a_node.hasAttribute("credits_total") && a_node.hasAttribute("credits_session") && a_node.hasAttribute("waiting_project") && a_node.hasAttribute("connected_machine")) {
					remaining_frames = Integer.parseInt(a_node.getAttribute("frame_remaining"));
					credits_earned = Integer.parseInt(a_node.getAttribute("credits_total"));
					credits_earned_session = Integer.parseInt(a_node.getAttribute("credits_session"));
					waiting_project = Integer.parseInt(a_node.getAttribute("waiting_project"));
					connected_machine = Integer.parseInt(a_node.getAttribute("connected_machine"));
				}
				
				ns = document.getElementsByTagName("job");
				if (ns.getLength() == 0) {
					throw new FermeException("error requestJob: parseXML failed, no 'job' node");
				}
				Element job_node = (Element) ns.item(0);
				
				ns = job_node.getElementsByTagName("renderer");
				if (ns.getLength() == 0) {
					throw new FermeException("error requestJob: parseXML failed, node 'job' have no sub-node 'renderer'");
				}
				Element renderer_node = (Element) ns.item(0);
				
				String script = "import bpy\nbpy.context.user_preferences.filepaths.temporary_directory = \"" + this.user_config.workingDirectory.getAbsolutePath().replace("\\", "\\\\") + "\"\n";
				try {
					ns = job_node.getElementsByTagName("script");
					if (ns.getLength() != 0) {
						Element a_node3 = (Element) ns.item(0);
						script += a_node3.getTextContent();
					}
				}
				catch (Exception e) {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					this.log.debug("Server::requestJob Exception " + e + " stacktrace: " + sw.toString());
				}
				
				String[] job_node_require_attribute = { "id", "archive_md5", "path", "use_gpu", "frame", "name", "extras", "password" };
				String[] renderer_node_require_attribute = { "md5", "commandline" };
				
				for (String e : job_node_require_attribute) {
					if (job_node.hasAttribute(e) == false) {
						throw new FermeException("error requestJob: parseXML failed, job_node have to attribute '" + e + "'");
					}
				}
				
				for (String e : renderer_node_require_attribute) {
					if (renderer_node.hasAttribute(e) == false) {
						throw new FermeException("error requestJob: parseXML failed, renderer_node have to attribute '" + e + "'");
					}
				}
				
				boolean use_gpu = (job_node.getAttribute("use_gpu").compareTo("1") == 0);
				boolean synchronous_upload = true;
				if (job_node.hasAttribute("synchronous_upload")) {
					synchronous_upload = (job_node.getAttribute("synchronous_upload").compareTo("1") == 0);
				}
				
				String frame_extras = "";
				if (job_node.hasAttribute("extras")) {
					frame_extras = job_node.getAttribute("extras");
				}
				
				String update_method = null;
				if (renderer_node.hasAttribute("update_method")) {
					update_method = renderer_node.getAttribute("update_method");
				}
				
				Job a_job = new Job(
						this.user_config,
						this.client.getGui(),
						this.client.getLog(),
						job_node.getAttribute("id"),
						job_node.getAttribute("frame"),
						job_node.getAttribute("path").replace("/", File.separator),
						use_gpu,
						renderer_node.getAttribute("commandline"),
						script,
						job_node.getAttribute("archive_md5"),
						renderer_node.getAttribute("md5"),
						job_node.getAttribute("name"),
						job_node.getAttribute("password"),
						frame_extras,
						synchronous_upload,
						update_method
						);
				
				this.client.getGui().displayStats(new Stats(remaining_frames, credits_earned, credits_earned_session, waiting_project, connected_machine));
				
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
			Document document = null;
			try {
				document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
			}
			catch (SAXException e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				this.log.debug("Server::HTTPSendFile SAXException " + e + " stacktrace: " + sw.toString());
				return ServerCode.UNKNOWN;
			}
			catch (IOException e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				this.log.debug("Server::HTTPSendFile IOException " + e + " stacktrace: " + sw.toString());
				return ServerCode.UNKNOWN;
			}
			catch (ParserConfigurationException e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				this.log.debug("Server::HTTPSendFile ParserConfigurationException " + e + " stacktrace: " + sw.toString());
				return ServerCode.UNKNOWN;
			}
			
			this.lastRequestTime = new Date().getTime();
			
			ServerCode ret1 = Utils.statusIsOK(document, "jobvalidate");
			if (ret1 != ServerCode.OK) {
				this.log.error("Server::HTTPSendFile wrong status (is " + ret1 + ")");
				return ret1;
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
	
	public byte[] getLastRender() {
		try {
			HttpURLConnection httpCon = this.HTTPRequest(this.getPage("last-render-frame"));
			
			InputStream inStrm = httpCon.getInputStream();
			if (httpCon.getResponseCode() != HttpURLConnection.HTTP_OK) {
				this.log.debug("Server::getLastRender code not ok " + httpCon.getResponseCode());
				return null;
			}
			int size = httpCon.getContentLength();
			
			if (size <= 0) {
				this.log.debug("Server::getLastRender size is negative (size: " + size + ")");
				return null;
			}
			
			byte[] ret = new byte[size];
			byte[] ch = new byte[512 * 1024];
			int n = 0;
			int i = 0;
			while ((n = inStrm.read(ch)) != -1) {
				System.arraycopy(ch, 0, ret, i, n);
				i += n;
			}
			inStrm.close();
			return ret;
		}
		catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			this.log.debug("Server::getLastRender Exception " + e + " stacktrace: " + sw.toString());
		}
		return null;
	}
	
	private String generateXMLForMD5cache() {
		String xml_str = null;
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document document_cache = docBuilder.newDocument();
			
			Element rootElement = document_cache.createElement("cache");
			document_cache.appendChild(rootElement);
			
			List<File> local_files = this.user_config.getLocalCacheFiles();
			for (File local_file : local_files) {
				Element node_file = document_cache.createElement("file");
				rootElement.appendChild(node_file);
				try {
					String extension = local_file.getName().substring(local_file.getName().lastIndexOf('.')).toLowerCase();
					String name = local_file.getName().substring(0, local_file.getName().length() - 1 * extension.length());
					if (extension.equals(".zip")) {
						node_file.setAttribute("md5", name);
					}
				}
				catch (StringIndexOutOfBoundsException e) { // because the file does not have an . his path
				}
			}
			
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(document_cache), new StreamResult(writer));
			xml_str = writer.getBuffer().toString();
		}
		catch (TransformerConfigurationException e) {
			this.log.debug("Server::generateXMLForMD5cache " + e);
		}
		catch (TransformerException e) {
			this.log.debug("Server::generateXMLForMD5cache " + e);
		}
		catch (ParserConfigurationException e) {
			this.log.debug("Server::generateXMLForMD5cache " + e);
		}
		
		return xml_str;
	}
	
	private void handleFileMD5DeleteDocument(Document document, String root_nodename) {
		NodeList ns = document.getElementsByTagName(root_nodename);
		if (ns.getLength() > 0) {
			Element root_node = (Element) ns.item(0);
			ns = root_node.getElementsByTagName("file");
			if (ns.getLength() > 0) {
				for (int i = 0; i < ns.getLength(); ++i) {
					Element file_node = (Element) ns.item(i);
					if (file_node.hasAttribute("md5") && file_node.hasAttribute("action") && file_node.getAttribute("action").equals("delete")) {
						String path = this.user_config.workingDirectory.getAbsolutePath() + File.separatorChar + file_node.getAttribute("md5");
						this.log.debug("Server::handleFileMD5DeleteDocument delete old file " + path);
						File file_to_delete = new File(path + ".zip");
						file_to_delete.delete();
						Utils.delete(new File(path));
					}
				}
			}
		}
	}
	
	public String getPage(String key) {
		if (this.pages.containsKey(key)) {
			return this.base_url + this.pages.get(key);
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
