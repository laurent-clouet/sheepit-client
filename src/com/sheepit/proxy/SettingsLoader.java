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

package com.sheepit.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.sheepit.proxy.Configuration;

public class SettingsLoader {
	private String path;
	
	private String port;
	private String cacheDir;
	private String ui;
	
	public SettingsLoader() {
		path = getDefaultFilePath();
	}
	
	public SettingsLoader(String path_) {
		path = path_;
	}
	
	public SettingsLoader(String port_, String cacheDir_, String ui_) {
		path = getDefaultFilePath();
		port = port_;
		cacheDir = cacheDir_;
		ui = ui_;
	}
	
	public static String getDefaultFilePath() {
		return System.getProperty("user.home") + File.separator + ".sheepit.proxy.conf";
	}
	
	public String getFilePath() {
		return path;
	}
	
	public void saveFile() {
		Properties prop = new Properties();
		OutputStream output = null;
		try {
			output = new FileOutputStream(path);
			
			if (cacheDir != null) {
				prop.setProperty("cache-dir", cacheDir);
			}
			
			if (port != null) {
				prop.setProperty("port", port);
			}
			
			if (ui != null) {
				prop.setProperty("ui", ui);
			}
			
			prop.store(output, null);
		}
		catch (IOException io) {
			io.printStackTrace();
		}
		finally {
			if (output != null) {
				try {
					output.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		// Set Owner read/write
		Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
		perms.add(PosixFilePermission.OWNER_READ);
		perms.add(PosixFilePermission.OWNER_WRITE);
		
		try {
			Files.setPosixFilePermissions(Paths.get(path), perms);
		}
		catch (UnsupportedOperationException e) {
			// most likely because it's MS Windows
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void loadFile() {
		this.port = null;
		this.cacheDir = null;
		this.ui = null;
		
		if (new File(path).exists() == false) {
			return;
		}
		
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream(path);
			prop.load(input);
			
			if (prop.containsKey("cache-dir")) {
				this.cacheDir = prop.getProperty("cache-dir");
			}
			
			if (prop.containsKey("ui")) {
				this.ui = prop.getProperty("ui");
			}
		}
		catch (IOException io) {
			io.printStackTrace();
		}
		finally {
			if (input != null) {
				try {
					input.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Merge the Settings file with the Configuration.
	 * The Configuration will have high priority.
	 */
	public void merge(Configuration config) {
		if (config == null) {
			System.out.println("SettingsLoader::merge config is null");
			return;
		}
		
		loadFile();
		
		if (config.getPort() == -1 && port != null) {
			config.setPort(Integer.valueOf(port));
		}
		
		if (config.getUserSpecifiedACacheDir() == false && cacheDir != null && new File(cacheDir).exists()) {
			config.setCacheDir(new File(cacheDir));
		}
		
		if (config.getUIType() == null && ui != null) {
			config.setUIType(ui);
		}
		
	}

	@Override
	public String toString() {
		return "SettingsLoader [path=" + path + ", port=" + port + ", cacheDir=" + cacheDir + ", ui=" + ui + "]";
	}
}
