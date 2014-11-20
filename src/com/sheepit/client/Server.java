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
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
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
import com.sheepit.client.exception.FermeExceptionNoRightToRender;
import com.sheepit.client.exception.FermeExceptionNoSession;
import com.sheepit.client.exception.FermeExceptionSessionDisabled;
import com.sheepit.client.os.OS;

public class Server extends Thread implements HostnameVerifier, X509TrustManager {
	private String base_url;
	private Configuration user_config;
	private Client client;
	private ArrayList<String> cookies;
	private HashMap<String, String> pages;
	private Log log;
	private long lastRequestTime;
	private int keepmealive_duration; // time is ms
	
	public Server(String url_, Configuration user_config_, Client client_) {
		super();
		this.base_url = url_;
		this.user_config = user_config_;
		this.client = client_;
		this.pages = new HashMap<String, String>();
		this.cookies = new ArrayList<String>();
		this.log = Log.getInstance(this.user_config);
		this.lastRequestTime = 0;
		this.keepmealive_duration = 15 * 60 * 1000; // default 15min
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
						if (this.client.getRenderingJob().getExtras() != null && this.client.getRenderingJob().getExtras().length() > 0) {
							args += "&extras=" + this.client.getRenderingJob().getExtras();
						}
					}
					
					HttpURLConnection connection = this.HTTPRequest(this.base_url + "/server/keepmealive.php" + args);
					
					if (connection.getResponseCode() == HttpURLConnection.HTTP_OK && connection.getContentType().startsWith("text/xml")) {
						DataInputStream in = new DataInputStream(connection.getInputStream());
						try {
							Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
							ServerCode ret = Utils.statusIsOK(document, "keepmealive");
							if (ret == ServerCode.KEEPMEALIVE_STOP_RENDERING) {
								this.log.debug("Server::keeepmealive server ask to kill local render process");
								// kill the current process, it will generate an error but it's okay
								if (this.client != null && this.client.getRenderingJob() != null && this.client.getRenderingJob().getProcess() != null) {
									OS.getOS().kill(this.client.getRenderingJob().getProcess());
								}
							}
						}
						catch (SAXException e) {
						}
						catch (ParserConfigurationException e) {
						}
					}
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				Thread.sleep(60 * 1000); // 1min
			}
			catch (InterruptedException e) {
				return;
			}
			catch (Exception e) {
				return;
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
			String url_contents = String.format("%s%s?login=%s&password=%s&cpu_family=%s&cpu_model=%s&cpu_model_name=%s&cpu_cores=%s&os=%s&ram=%s&bits=%s&version=%s&extras=%s", 
				this.base_url,
				"/server/config.php",
				URLEncoder.encode(this.user_config.login(), "UTF-8"),
				URLEncoder.encode(this.user_config.password(), "UTF-8"),
				os.getCPU().family(),
				os.getCPU().model(),
				URLEncoder.encode(os.getCPU().name(), "UTF-8"),
				((this.user_config.getNbCores() == -1) ? os.getCPU().cores() : this.user_config.getNbCores()),
				os.name(),
				os.getMemory(),
				os.getCPU().arch(),
				this.user_config.getJarVersion(),
				this.user_config.getExtras());
			this.log.debug("Server::getConfiguration url " + url_contents);
			
			connection = this.HTTPRequest(url_contents);
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
					this.log.error("getConfiguration error: failed to parse XML, no node 'config_serveur'");
					return Error.Type.WRONG_CONFIGURATION;
				}
				config_node = (Element) ns.item(0);
				
				ns = config_node.getElementsByTagName("request");
				if (ns.getLength() == 0) {
					this.log.error("getConfiguration error: failed to parse XML, node 'config' have no child node 'request'");
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
			else {
				this.log.error("Server::getConfiguration: Invalid response " + contentType + " " + r);
				return Error.Type.WRONG_CONFIGURATION;
			}
		}
		catch (ConnectException e) {
			this.log.error("Server::getConfiguration error ConnectException " + e);
			return Error.Type.NETWORK_ISSUE;
		}
		catch (UnknownHostException e) {
			this.log.error("Server::getConfiguration error UnknownHostException " + e);
			return Error.Type.NETWORK_ISSUE;
		}
		catch (NoRouteToHostException e) {
			this.log.error("Server::getConfiguration error NoRouteToHost " + e);
			return Error.Type.NETWORK_ISSUE;
		}
		catch (Exception e) {
			this.log.error("Server::getConfiguration: exception 02R " + e);
			e.printStackTrace();
			return Error.Type.UNKNOWN;
		}
		finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		return Error.Type.OK;
	}
	
	public Job requestJob() throws FermeException, FermeExceptionNoRightToRender, FermeExceptionNoSession, FermeExceptionSessionDisabled {
		this.log.debug("Server::requestJob");
		String url_contents = "";
		
		HttpURLConnection connection = null;
		try {
			String url = String.format("%s?computemethod=%s", this.getPage("request-job"), this.user_config.computeMethodToInt());
			if (this.user_config.getComputeMethod() != ComputeType.CPU_ONLY && this.user_config.getGPUDevice() != null) {
				String gpu_model = "";
				try {
					gpu_model = URLEncoder.encode(this.user_config.getGPUDevice().getModel(), "UTF-8");
				}
				catch (UnsupportedEncodingException e) {
				}
				url += "&gpu_model=" + gpu_model + "&gpu_ram=" + this.user_config.getGPUDevice().getMemory();
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
					else if (ret == ServerCode.JOB_REQUEST_ERROR_SESSION_DISABLED) {
						throw new FermeExceptionSessionDisabled();
					}
					this.log.error("Server::requestJob: Utils.statusIsOK(document, 'jobrequest') -> ret " + ret);
					throw new FermeException("error requestJob: status is not ok (it's " + ret + ")");
				}
				
				handleFileMD5DeleteDocument(document, "jobrequest");
				
				Element a_node = null;
				NodeList ns = null;
				
				ns = document.getElementsByTagName("frames");
				if (ns.getLength() == 0) {
					throw new FermeException("error requestJob: parseXML failed, no 'frame' node");
				}
				a_node = (Element) ns.item(0);
				
				int remaining_frames = -1;
				if (a_node.hasAttribute("remaining")) {
					remaining_frames = Integer.parseInt(a_node.getAttribute("remaining"));
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
						script += new String(a_node3.getTextContent());
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				
				String[] job_node_require_attribute = { "id", "archive_md5", "path", "revision", "use_gpu", "frame", "extras" };
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
				
				String frame_extras = "";
				if (job_node.hasAttribute("extras")) {
					frame_extras = job_node.getAttribute("extras");
				}
				
				Job a_job = new Job(
						this.user_config,
						job_node.getAttribute("id"),
						job_node.getAttribute("frame"),
						job_node.getAttribute("revision"),
						job_node.getAttribute("path").replace("/", File.separator),
						use_gpu,
						renderer_node.getAttribute("commandline"),
						script,
						job_node.getAttribute("archive_md5"),
						renderer_node.getAttribute("md5"),
						frame_extras
						);
				
				this.client.getGui().framesRemaining(remaining_frames);
				
				return a_job;
			}
			else {
				System.out.println("Server::requestJob url " + url_contents + " r " + r + " contentType " + contentType);
				InputStream in = connection.getInputStream();
				String line;
				BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
				while ((line = reader.readLine()) != null) {
					System.out.print(line);
				}
				System.out.println("");
			}
		}
		catch (FermeExceptionNoRightToRender e) {
			throw e;
		}
		catch (FermeExceptionNoSession e) {
			throw e;
		}
		catch (FermeExceptionSessionDisabled e) {
			throw e;
		}
		catch (FermeException e) {
			throw new FermeException(e.getMessage());
		}
		catch (Exception e) {
			throw new FermeException("error requestJob: unknow exception " + e);
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
		connection.setRequestMethod("GET");
		for (String cookie : this.cookies) {
			connection.setRequestProperty("Cookie", cookie);
		}
		
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
				e.printStackTrace();
				return null;
			}
			catch (KeyManagementException e) {
				e.printStackTrace();
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
		
		String headerName = null;
		for (int i = 1; (headerName = connection.getHeaderFieldKey(i)) != null; i++) {
			if (headerName.equals("Set-Cookie")) {
				String cookie = connection.getHeaderField(i);
				
				boolean cookieIsPresent = false;
				for (String value : this.cookies) {
					if (value.equalsIgnoreCase(cookie))
						cookieIsPresent = true;
				}
				if (!cookieIsPresent)
					this.cookies.add(cookie);
			}
		}
		
		this.lastRequestTime = new Date().getTime();
		
		return connection;
	}
	
	public int HTTPGetFile(String url_, String destination_, Gui gui_, String status_) {
		// the destination_ parent directory must exist
		try {
			HttpURLConnection httpCon = this.HTTPRequest(url_);
			
			InputStream inStrm = httpCon.getInputStream();
			if (httpCon.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return -1;
			}
			int size = httpCon.getContentLength();
			long start = new Date().getTime();
			
			FileOutputStream fos = new FileOutputStream(destination_);
			byte[] ch = new byte[512 * 1024];
			int nb;
			long writed = 0;
			long last_gui_update = 0; // size in byte
			while ((nb = inStrm.read(ch)) != -1) {
				fos.write(ch, 0, nb);
				writed += nb;
				if ((writed - last_gui_update) > 1000000) { // only update the gui every 1MB
					gui_.status(String.format(status_, (int) (100.0 * writed / size)));
					last_gui_update = writed;
				}
			}
			fos.close();
			inStrm.close();
			long end = new Date().getTime();
			this.log.debug(String.format("File downloaded at %.1f kB/s", ((float) (size / 1000)) / ((float) (end - start) / 1000)));
			this.lastRequestTime = new Date().getTime();
			return 0;
		}
		catch (Exception e) {
			System.err.println("Server::HTTPGetFile exception");
			e.printStackTrace();
		}
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
			conn.setUseCaches(false);
			for (String cookie : this.cookies) {
				conn.setRequestProperty("Cookie", cookie);
			}
			
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
		catch (MalformedURLException ex) {
			this.log.error("Server::HTTPSendFile, exception MalformedURLException " + ex);
			return ServerCode.UNKNOWN;
		}
		catch (IOException ioe) {
			this.log.error("Server::HTTPSendFile, exception IOException " + ioe);
			return ServerCode.UNKNOWN;
		}
		catch (Exception e6) {
			this.log.error("Server::HTTPSendFile, exception Exception " + e6);
			return ServerCode.UNKNOWN;
		}
		
		int r;
		try {
			r = conn.getResponseCode();
		}
		catch (IOException e1) {
			e1.printStackTrace();
			return ServerCode.UNKNOWN;
		}
		String contentType = conn.getContentType();
		
		if (r == HttpURLConnection.HTTP_OK && contentType.startsWith("text/xml")) {
			DataInputStream in;
			try {
				in = new DataInputStream(conn.getInputStream());
			}
			catch (IOException e1) {
				e1.printStackTrace();
				return ServerCode.UNKNOWN;
			}
			Document document = null;
			try {
				document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
			}
			catch (SAXException e) {
				e.printStackTrace();
				return ServerCode.UNKNOWN;
			}
			catch (IOException e) {
				e.printStackTrace();
				return ServerCode.UNKNOWN;
			}
			catch (ParserConfigurationException e) {
				e.printStackTrace();
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
