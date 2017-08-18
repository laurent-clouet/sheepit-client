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

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class Updater {
	
	public static void main(String[] argsFromMain) {
		String serverBase = "https://www.sheepit-renderfarm.com";
		String deployementDirectory = System.getProperty("java.io.tmpdir");
		UI ui = new UI();
		
		ui.start();
		
		if (deployementDirectory == null || deployementDirectory.isEmpty()) {
			ui.error("Failed to find deployement directory");
			return;
		}
		
		Server server = new Server(serverBase, ui);
		String md5Client = server.getClientMd5();
		if (md5Client == null) {
			ui.error("Failed to client information on server");
			return;
		}
		
		File jar = new File(deployementDirectory + File.separator + md5Client + ".jar");
		String jarPath = jar.getAbsolutePath();
		
		if (jar.exists() == false || Utils.md5(jarPath).equals(md5Client) == false) {
			jar.delete();
			int r = server.downloadClient(jarPath);
			if (r != 0) {
				ui.error("Failed to download new client");
				return;
			}
			
		}
		
		ui.stop();
		
		System.out.println("everything is fine will launch " + jarPath);
		
		try {
			ClassLoader classLoader = new URLClassLoader(new URL[] { new File(jarPath).toURI().toURL() });
			Class<?> c = classLoader.loadClass("com.sheepit.client.standalone.Worker");
			
			Method m = c.getMethod("main", new Class[] { argsFromMain.getClass() });
			m.setAccessible(true);
			m.invoke(null, new Object[] { argsFromMain });
		}
		catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
	}
}
