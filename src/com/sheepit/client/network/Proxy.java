/*
 * Copyright (C) 2015 Laurent CLOUET
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

package com.sheepit.client.network;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.Authenticator;

public class Proxy {
	
	public static void set(String url_) throws MalformedURLException {
		if (url_ == null || url_.isEmpty()) {
			return;
		}
		URL url = new URL(url_);
		String userinfo = url.getUserInfo();
		if (userinfo != null) {
			String[] elements = userinfo.split(":");
			if (elements.length == 2) {
				String proxy_user = elements[0];
				String proxy_password = elements[1];
				
				if (proxy_user != null && proxy_password != null) {
					Authenticator.setDefault(new ProxyAuthenticator(proxy_user, proxy_password));
				}
			}
		}
		
		int port = url.getPort();
		if (port == -1) {
			port = 8080;
		}
		
		System.setProperty("http.proxyHost", url.getHost());
		System.setProperty("http.proxyPort", Integer.toString(port));
		
		System.setProperty("https.proxyHost", url.getHost());
		System.setProperty("https.proxyPort", Integer.toString(port));
	}
	
	public static boolean isValidURL(String url_) {
		if (url_ == null || url_.isEmpty()) {
			return true;
		}
		try {
			new URL(url_);
			
			return true;
		}
		catch (MalformedURLException e) {
			return false;
		}
	}
}
