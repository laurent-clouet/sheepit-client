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

package com.sheepit.proxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Configuration {
	private String baseUrl;
	private File cacheDirectory;
	private boolean userSpecifiedACacheDir;
	private int port;
	private String UIType;
	private boolean printLog;
	
	public Configuration(String baseUrl_, File cache_dir_, int port_) {
		this.baseUrl = baseUrl_;
		this.setCacheDir(cache_dir_);
		this.port = port_;
		this.UIType = null;
		this.printLog = false;
	}
	
	@Override
	public String toString() {
		return "Configuration [cacheDirectory=" + cacheDirectory + ", port=" + port + ", UIType=" + UIType + "]";
	}
	
	public String getServerUrl() {
		return baseUrl;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port_) {
		this.port = port_;
	}
	
	public void setPrintLog(boolean val) {
		this.printLog = val;
	}
	
	public boolean getPrintLog() {
		return this.printLog;
	}
	
	public void setCacheDir(File cache_dir_) {
		removeWorkingDirectory();
		if (cache_dir_ == null) {
			this.userSpecifiedACacheDir = false;
			try {
				File tmp = File.createTempFile("farm_", "");
				//this.cacheDirectory.createNewFile(); // hoho...
				tmp.delete();
				//this.cacheDirectory.mkdir();
				//this.cacheDirectory.deleteOnExit();
				
				// since there is no working directory and the client will be working in the system temp directory, 
				// we can also set up a 'permanent' directory for immutable files (like renderer binary)
				
				this.cacheDirectory = new File(tmp.getParent() + File.separator + "sheepit_archive");
				this.cacheDirectory.mkdir();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			this.userSpecifiedACacheDir = true;
			this.cacheDirectory = cache_dir_;
		}
		
	}
	
	public File getCacheDir() {
		return this.cacheDirectory;
	}
	
	public boolean getUserSpecifiedACacheDir() {
		return this.userSpecifiedACacheDir;
	}
	
	public void setUIType(String ui) {
		this.UIType = ui;
	}
	
	public String getUIType() {
		return this.UIType;
	}
	
	
	public void cleanWorkingDirectory() {
		this.cleanDirectory(this.cacheDirectory);
	}
	
	public boolean cleanDirectory(File dir) {
		if (dir == null) {
			return false;
		}
		
		File[] files = dir.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					Utils.delete(file);
				}
				else {
					try {
						String extension = file.getName().substring(file.getName().lastIndexOf('.')).toLowerCase();
						String name = file.getName().substring(0, file.getName().length() - 1 * extension.length());
						if (extension.equals(".zip")) {
							// check if the md5 of the file is ok
							String md5_local = Utils.md5(file.getAbsolutePath());
							
							if (md5_local.equals(name) == false) {
								file.delete();
							}
							
							// TODO: remove old one
						}
						else {
							file.delete();
						}
					}
					catch (StringIndexOutOfBoundsException e) { // because the file does not have an . in his path
						file.delete();
					}
				}
			}
		}
		return true;
	}
	
	public void removeWorkingDirectory() {
		if (this.userSpecifiedACacheDir == true) {
			this.cleanWorkingDirectory();
		}
		else {
			Utils.delete(this.cacheDirectory);
		}
	}
	
	public String getJarVersion() {
		String versionPath = "/VERSION";
		
		InputStream versionStream = ProxyRunner.class.getResourceAsStream(versionPath);
		if (versionStream == null) {
			System.err.println("Configuration::getJarVersion Failed to get version file");
			return "";
		}
		
		try {
			InputStreamReader reader = new InputStreamReader(versionStream);
			BufferedReader in = new BufferedReader(reader);
			String version = in.readLine();
			
			return version;
		}
		catch (IOException ex) {
			System.err.println("Configuration::getJarVersion error while reading manifest file (" + versionPath + "): " + ex.getMessage());
			return "";
		}
	}
}
