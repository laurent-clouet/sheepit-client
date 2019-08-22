package com.sheepit.proxy;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

class Proxy extends AbstractHandler implements HostnameVerifier, X509TrustManager {
	private String baseUrlServer;
	private String localDirectory;
	
	Proxy(String url_, String dir_) {
		baseUrlServer = url_;
		localDirectory = dir_;
	}
	
	public void handle(String target, Request baseRequest, HttpServletRequest requestFromClient, HttpServletResponse responseToClient) throws IOException, ServletException {
		if ("/server/archive.php".equals(requestFromClient.getRequestURI())) { // TODO: should be parse from a config request
			handle_archive_download(target, baseRequest, requestFromClient, responseToClient);
		}
		else {
			handle_forward_request(target, baseRequest, requestFromClient, responseToClient);
		}
		
	}
	
	public void handle_archive_download(String target, Request baseRequest, HttpServletRequest requestFromClient, HttpServletResponse responseToClient) throws IOException, ServletException {
		String newURL = baseUrlServer + requestFromClient.getRequestURI() + (requestFromClient.getQueryString() != null ? "?" + requestFromClient.getQueryString() : "");
		
		System.out.println("------------------------handle_archive_download START " + newURL + " ----------------------------------");
		try {
			String type = "";
			String job = "";
			
			List<NameValuePair> toto = new URIBuilder(newURL).getQueryParams();
			for (NameValuePair v : toto) {
				if ("type".equals(v.getName())) {
					type = v.getValue();
				}
				
				if ("job".equals(v.getName())) {
					job = v.getValue();
				}
				
			}
			
			if (type.equals("binary")) {
				handle_forward_request(target, baseRequest, requestFromClient, responseToClient);
				return;
			}
			
			else {
				File destination = new File(localDirectory + File.separator + job);
				
				if (destination.exists() == false) {
					// download the file first
					
					HttpURLConnection requestToServer = this.createRequestToServer(newURL, requestFromClient);
					InputStream inStrm = requestToServer.getInputStream();
					FileOutputStream fos = new FileOutputStream(destination);
					byte[] ch = new byte[512 * 1024];
					int nb;
					while ((nb = inStrm.read(ch)) != -1) {
						fos.write(ch, 0, nb);
					}
					requestToServer.disconnect();
					fos.close();
				}
				
				// give file to client
				
				byte[] buffer = new byte[1024];
				int bytesRead;
				OutputStream output1 = responseToClient.getOutputStream();
				InputStream datafromServer = new FileInputStream(destination);
				while ((bytesRead = datafromServer.read(buffer)) != -1) {
					output1.write(buffer, 0, bytesRead);
				}
				datafromServer.close();
				
				// cookies
				//				List<HttpCookie> cookies1 = new ArrayList<HttpCookie>(1);
				//				String headerName = null;
				//				for (int i = 1; (headerName = requestToServer.getHeaderFieldKey(i)) != null; i++) {
				//					if (headerName.equals("Set-Cookie")) {
				//						cookies1.addAll(HttpCookie.parse(requestToServer.getHeaderField(i)));
				//					}
				//				}
				
				//	System.out.println("sending back cookie: ");
				//				for (HttpCookie c : cookies1) {
				//					responseToClient.addCookie(toJettyCookie(c));
				//				}
				
				baseRequest.setContentType("application/octet-stream"); // requestToServer.getContentType());
				responseToClient.setStatus(200); // requestToServer.getResponseCode());
				responseToClient.flushBuffer();
				baseRequest.setHandled(true);
				
				return;
			}
			
		}
		catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public HttpURLConnection createRequestToServer(String url_, HttpServletRequest requestFromClient) throws IOException {
		URL url = new URL(url_);
		
		HttpURLConnection request = (HttpURLConnection) url.openConnection();
		request.setDoInput(true);
		request.setDoOutput(true);
		request.setInstanceFollowRedirects(true);
		
		if (requestFromClient.getContentType() != null) {
			request.setRequestProperty("Content-type", requestFromClient.getContentType());
		}
		request.setRequestProperty("X-HTTP-Method-Override", requestFromClient.getMethod());
		
		Cookie[] cookies = requestFromClient.getCookies();
		if (cookies != null) {
			String cookie_str = "";
			for (Cookie c : cookies) {
				cookie_str += c.getName() + "=" + c.getValue() + ":";
			}
			if (cookie_str.isEmpty() == false) {
				cookie_str = cookie_str.substring(0, cookie_str.length() - 1); // remove the :
			}
			request.setRequestProperty("Cookie", cookie_str);
		}
		
		if (url_.startsWith("https://")) {
			try {
				SSLContext sc;
				sc = SSLContext.getInstance("SSL");
				sc.init(null, new TrustManager[] { this }, null);
				SSLSocketFactory factory = sc.getSocketFactory();
				((HttpsURLConnection) request).setSSLSocketFactory(factory);
				((HttpsURLConnection) request).setHostnameVerifier(this);
			}
			catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				//	this.log.debug("Server::HTTPRequest NoSuchAlgorithmException " + e + " stacktrace: " + sw.toString());
				return null;
			}
			catch (KeyManagementException e) {
				e.printStackTrace();
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				//	this.log.debug("Server::HTTPRequest KeyManagementException " + e + " stacktrace: " + sw.toString());
				return null;
			}
		}
		
		return request;
	}
	
	public void handle_forward_request(String target, Request baseRequest, HttpServletRequest requestFromClient, HttpServletResponse responseToClient) throws IOException, ServletException {
		String newURL = baseUrlServer + requestFromClient.getRequestURI() + (requestFromClient.getQueryString() != null ? "?" + requestFromClient.getQueryString() : "");
		
		System.out.println("------------------------handle_forward_request START " + newURL + " ----------------------------------");
		HttpURLConnection requestToServer = this.createRequestToServer(newURL, requestFromClient);
		
		// client data to server input
		InputStream input1 = requestFromClient.getInputStream();
		
		//	private void copyStream2(InputStream input1, OutputStream input2) {
		DataOutputStream dos = new DataOutputStream(requestToServer.getOutputStream());
		int total = 0;
		byte[] buffer = new byte[1024];
		int bytesRead;
		while ((bytesRead = input1.read(buffer)) != -1) {
			dos.write(buffer, 0, bytesRead);
			total += bytesRead;
		}
		
		dos.flush();
		dos.close();
		
		// server to client 
		total = 0;
		OutputStream output1 = responseToClient.getOutputStream();
		InputStream datafromServer = requestToServer.getInputStream();
		while ((bytesRead = datafromServer.read(buffer)) != -1) {
			output1.write(buffer, 0, bytesRead);
			total += bytesRead;
		}
		
		// cookies
		List<HttpCookie> cookies1 = new ArrayList<HttpCookie>(1);
		String headerName = null;
		for (int i = 1; (headerName = requestToServer.getHeaderFieldKey(i)) != null; i++) {
			if (headerName.equals("Set-Cookie")) {
				cookies1.addAll(HttpCookie.parse(requestToServer.getHeaderField(i)));
			}
		}
		
		for (HttpCookie c : cookies1) {
			responseToClient.addCookie(toJettyCookie(c));
		}
		responseToClient.setContentType(requestToServer.getContentType());
		responseToClient.setStatus(requestToServer.getResponseCode());
		requestToServer.disconnect();
		responseToClient.flushBuffer();
		baseRequest.setContentType(requestToServer.getContentType());
		baseRequest.setHandled(true);
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
	
	private Cookie toJettyCookie(HttpCookie httpCookie) {
		Cookie ret = new Cookie(httpCookie.getName(), httpCookie.getValue());
		if (httpCookie.getDomain() != null) {
			ret.setDomain(httpCookie.getDomain());
		}
		if (httpCookie.getPath() != null) {
			ret.setPath(httpCookie.getPath());
		}
		if (httpCookie.getComment() != null) {
			ret.setComment(httpCookie.getComment());
		}
		ret.setSecure(httpCookie.getSecure());
		ret.setVersion(httpCookie.getVersion());
		ret.setMaxAge((int) httpCookie.getMaxAge());
		
		return ret;
	}
	
}
