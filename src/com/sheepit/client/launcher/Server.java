/*
 * Copyright (C) 2017 Laurent CLOUET
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

package com.sheepit.client.launcher;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Server extends Thread implements HostnameVerifier, X509TrustManager {
	private String baseUrl;
	private UI ui;
	
	public Server(String url_, UI ui_) {
		super();
		baseUrl = url_;
		ui = ui_;
	}
	
	public String getClientMd5() {
		HttpURLConnection connection = null;
		try {
			String url_contents = String.format("%s%s?get=md5", baseUrl, "/media/applet/client-info.php");
			
			connection = this.HTTPRequest(url_contents);
			int r = connection.getResponseCode();
			
			if (r == HttpURLConnection.HTTP_OK) {
				InputStream in = connection.getInputStream();
				String output = "";
				String line;
				BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
				while ((line = reader.readLine()) != null) {
					output += line;
				}
				connection.disconnect();
				return output;
				
			}
			else {
				System.out.println("Server::getClientMd5 http code not ok, it's " + r);
				return "";
			}
			
		}
		catch (ConnectException e) {
			System.out.println("Server::getClientMd5 error ConnectException " + e);
			return "";
		}
		catch (UnsupportedEncodingException e) {
			System.out.println("Server::getClientMd5 UnsupportedEncodingException " + e);
			return "";
		}
		catch (IOException e) {
			System.out.println("Server::getClientMd5 IOException " + e);
			return "";
		}
		finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	public int downloadClient(String destination_) {
		System.out.println("Server::downloadClient(" + destination_ + ")");
		// the destination_ parent directory must exist
		try {
			HttpURLConnection httpCon = this.HTTPRequest(baseUrl + "/media/applet/client-latest.php");
			
			InputStream inStrm = httpCon.getInputStream();
			if (httpCon.getResponseCode() != HttpURLConnection.HTTP_OK) {
				System.out.println("Server::downloadClient(" + destination_ + ") HTTP code is not " + HttpURLConnection.HTTP_OK + " it's " + httpCon.getResponseCode());
				return -1;
			}
			int total = httpCon.getContentLength();
			FileOutputStream fos = new FileOutputStream(destination_);
			byte[] ch = new byte[1024];
			int nb;
			int written = 0;
			
			ui.progress(0, total);
			
			while ((nb = inStrm.read(ch)) != -1) {
				fos.write(ch, 0, nb);
				written += nb;
				ui.progress(written, total);
			}
			fos.close();
			inStrm.close();
			return 0;
		}
		catch (Exception e) {
			System.out.println("Server::downloadClient exception " + e);
			e.printStackTrace();
		}
		return -2;
	}
	
	public HttpURLConnection HTTPRequest(String url_) throws IOException {
		HttpURLConnection connection = null;
		URL url = new URL(url_);
		
		connection = (HttpURLConnection) url.openConnection();
		connection.setDoInput(true);
		connection.setDoOutput(true);
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
				e.printStackTrace();
				return null;
			}
			catch (KeyManagementException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		return connection;
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
