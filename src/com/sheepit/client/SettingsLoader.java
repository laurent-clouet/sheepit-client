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

package com.sheepit.client;

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

import com.sheepit.client.Configuration;
import com.sheepit.client.Configuration.ComputeType;
import com.sheepit.client.hardware.gpu.GPU;
import com.sheepit.client.hardware.gpu.GPUDevice;

public class SettingsLoader {
	private String path;
	
	private String login;
	private String password;
	private String proxy;
	private String hostname;
	private String incompatibleProcess;
	private String computeMethod;
	private String gpu;
	private String cores;
	private String ram;
	private String renderTime;
	private String cacheDir;
	private String autoSignIn;
	private String ui;
	private String tileSize;
	private int    priority;
	
	public SettingsLoader() {
		path = getDefaultFilePath();
	}
	
	public SettingsLoader(String path_) {
		path = path_;
	}
	
	public SettingsLoader(String login_, String password_, String proxy_, String hostname_, String incompatibleProcess_, ComputeType computeMethod_, GPUDevice gpu_, int cores_, int maxRam_, int maxRenderTime_, String cacheDir_, boolean autoSignIn_, String ui_, String tileSize_, int priority_) {
		path = getDefaultFilePath();
		login = login_;
		password = password_;
		proxy = proxy_;
		hostname = hostname_;
		incompatibleProcess = incompatibleProcess_;
		cacheDir = cacheDir_;
		autoSignIn = String.valueOf(autoSignIn_);
		ui = ui_;
		tileSize = tileSize_;
		priority = priority_;
		if (cores_ > 0) {
			cores = String.valueOf(cores_);
		}
		if (maxRam_ > 0) {
			ram = String.valueOf(maxRam_);
		}
		if (maxRenderTime_ > 0) {
			renderTime = String.valueOf(maxRenderTime_);
		}
		if (computeMethod_ != null) {
			try {
				computeMethod = computeMethod_.name();
			}
			catch (IllegalArgumentException e) {
			}
		}
		
		if (gpu_ != null) {
			gpu = gpu_.getCudaName();
		}
	}
	
	public static String getDefaultFilePath() {
		return System.getProperty("user.home") + File.separator + ".sheepit.conf";
	}
	
	public String getFilePath() {
		return path;
	}
	
	public void saveFile() {
		Properties prop = new Properties();
		OutputStream output = null;
		try {
			output = new FileOutputStream(path);
			prop.setProperty("priority", new Integer(priority).toString());
			
			if (cacheDir != null) {
				prop.setProperty("cache-dir", cacheDir);
			}
			
			if (computeMethod != null) {
				prop.setProperty("compute-method", computeMethod);
			}
			
			if (gpu != null) {
				prop.setProperty("compute-gpu", gpu);
			}
			
			if (cores != null) {
				prop.setProperty("cpu-cores", cores);
			}
			
			if (ram != null) {
				prop.setProperty("ram", ram);
			}
			
			if (renderTime != null) {
				prop.setProperty("rendertime", renderTime);
			}
			
			if (login != null) {
				prop.setProperty("login", login);
			}
			
			if (password != null) {
				prop.setProperty("password", password);
			}
			
			if (proxy != null) {
				prop.setProperty("proxy", proxy);
			}
			
			if (hostname != null) {
				prop.setProperty("hostname", hostname);
			}
			
			if (incompatibleProcess != null) {
				prop.setProperty("incompatible-process", incompatibleProcess);
			}
			
			if (autoSignIn != null) {
				prop.setProperty("auto-signin", autoSignIn);
			}
			
			if (ui != null) {
				prop.setProperty("ui", ui);
			}
			
			if (tileSize != null) {
				prop.setProperty("tile-size", tileSize);
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
		this.login = null;
		this.password = null;
		this.proxy = null;
		this.hostname = null;
		this.computeMethod = null;
		this.gpu = null;
		this.cacheDir = null;
		this.autoSignIn = null;
		this.ui = null;
		this.tileSize = null;
		this.priority = 19; // must be the same default as Configuration
		this.ram = null;
		this.renderTime = null;
		this.incompatibleProcess = null;
		
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
			
			if (prop.containsKey("compute-method")) {
				this.computeMethod = prop.getProperty("compute-method");
			}
			
			if (prop.containsKey("compute-gpu")) {
				this.gpu = prop.getProperty("compute-gpu");
			}
			
			if (prop.containsKey("cpu-cores")) {
				this.cores = prop.getProperty("cpu-cores");
			}
			
			if (prop.containsKey("ram")) {
				this.ram = prop.getProperty("ram");
			}
			
			if (prop.containsKey("rendertime")) {
				this.renderTime = prop.getProperty("rendertime");
			}
			
			if (prop.containsKey("login")) {
				this.login = prop.getProperty("login");
			}
			
			if (prop.containsKey("password")) {
				this.password = prop.getProperty("password");
			}
			
			if (prop.containsKey("proxy")) {
				this.proxy = prop.getProperty("proxy");
			}
			
			if (prop.containsKey("hostname")) {
				this.hostname = prop.getProperty("hostname");
			}
			
			if (prop.containsKey("incompatible-process")) {
				this.incompatibleProcess = prop.getProperty("incompatible-process");
			}
			
			if (prop.containsKey("auto-signin")) {
				this.autoSignIn = prop.getProperty("auto-signin");
			}
			
			if (prop.containsKey("ui")) {
				this.ui = prop.getProperty("ui");
			}
			
			if (prop.containsKey("tile-size")) {
				this.tileSize = prop.getProperty("tile-size");
			}
			
			if (prop.containsKey("priority")) {
				this.priority = Integer.parseInt(prop.getProperty("priority"));
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
		}
		
		loadFile();
		
		if (config.login().isEmpty() && login != null) {
			config.setLogin(login);
		}
		if (config.password().isEmpty() && password != null) {
			config.setPassword(password);
		}
		
		if ((config.getProxy() == null || config.getProxy().isEmpty()) && proxy != null) {
			config.setProxy(proxy);
		}
		
		if ((config.getHostname() == null || config.getHostname().isEmpty() || config.getHostname().equals(config.getDefaultHostname())) && hostname != null) {
			config.setHostname(hostname);
		}
		
		if ((config.getIncompatibleProcessName() == null || config.getIncompatibleProcessName().isEmpty()) && incompatibleProcess != null) {
			config.setIncompatibleProcessName(incompatibleProcess);
		}
		
		if (config.getPriority() == 19) { // 19 is default value
			config.setUsePriority(priority);
		}
		try {
			if ((config.getComputeMethod() == null && computeMethod != null) || (computeMethod != null && config.getComputeMethod() != ComputeType.valueOf(computeMethod))) {
				config.setComputeMethod(ComputeType.valueOf(computeMethod));
			}
		}
		catch (IllegalArgumentException e) {
			System.err.println("SettingsLoader::merge failed to handle compute method (raw value: '" + computeMethod + "')");
			computeMethod = null;
		}
		if (config.getGPUDevice() == null && gpu != null) {
			GPUDevice device = GPU.getGPUDevice(gpu);
			if (device != null) {
				config.setUseGPU(device);
			}
		}
		if (config.getNbCores() == -1 && cores != null) {
			config.setUseNbCores(Integer.valueOf(cores));
		}
		
		if (config.getMaxMemory() == -1 && ram != null) {
			config.setMaxMemory(Integer.valueOf(ram));
		}
		
		if (config.getMaxRenderTime() == -1 && renderTime != null) {
			config.setMaxRenderTime(Integer.valueOf(renderTime));
		}
		
		if (config.getUserSpecifiedACacheDir() == false && cacheDir != null && new File(cacheDir).exists()) {
			config.setCacheDir(new File(cacheDir));
		}
		
		if (config.getUIType() == null && ui != null) {
			config.setUIType(ui);
		}
		
		if (config.getTileSize() == -1 && tileSize != null) {
			config.setTileSize(Integer.valueOf(tileSize));
		}
		
		config.setAutoSignIn(Boolean.valueOf(autoSignIn));
	}
	
	@Override
	public String toString() {
		return "SettingsLoader [path=" + path + ", login=" + login + ", password=" + password + ", computeMethod=" + computeMethod + ", gpu=" + gpu + ", cacheDir=" + cacheDir + "priority="+priority+"]";
	}
}
