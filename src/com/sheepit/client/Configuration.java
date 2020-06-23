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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import com.sheepit.client.hardware.cpu.CPU;
import com.sheepit.client.hardware.gpu.GPUDevice;
import com.sheepit.client.os.OS;

public class Configuration {
	public enum ComputeType {
		CPU_GPU, CPU, GPU
	} // accept job for ...
	
	public File workingDirectory;
	public File storageDirectory; // for permanent storage (binary archive)
	public boolean userSpecifiedACacheDir;
	public String static_exeDirName;
	private String login;
	private String password;
	private String proxy;
	private int maxUploadingJob;
	private int nbCores;
	private int maxMemory; // max memory allowed for render
	private int maxRenderTime; // max render time per frame allowed
	private int priority;
	private ComputeType computeMethod;
	private GPUDevice GPUDevice;
	private boolean printLog;
	public List<Pair<Calendar, Calendar>> requestTime;
	private String extras;
	private boolean autoSignIn;
	private String UIType;
	private int tileSize;
	private String hostname;
	private String incompatibleProcess;
	
	public Configuration(File cache_dir_, String login_, String password_) {
		this.login = login_;
		this.password = password_;
		this.proxy = null;
		this.hostname = this.getDefaultHostname();
		this.static_exeDirName = "exe";
		this.maxUploadingJob = 1;
		this.nbCores = -1; // ie not set
		this.maxMemory = -1; // ie not set
		this.maxRenderTime = -1; // ie not set
		this.priority = 19; // default lowest
		this.computeMethod = null;
		this.GPUDevice = null;
		this.userSpecifiedACacheDir = false;
		this.workingDirectory = null;
		this.storageDirectory = null;
		this.setCacheDir(cache_dir_);
		this.printLog = false;
		this.requestTime = null;
		this.extras = "";
		this.autoSignIn = false;
		this.UIType = null;
		this.tileSize = -1; // ie not set
		this.incompatibleProcess = "";
	}
	
	
	public String toString() {
		return String.format("Configuration (workingDirectory '%s')", this.workingDirectory.getAbsolutePath());
	}
	
	public String login() {
		return this.login;
	}
	
	public void setLogin(String login_) {
		this.login = login_;
	}
	
	public String password() {
		return this.password;
	}
	
	public void setPassword(String password_) {
		this.password = password_;
	}
	
	public String getProxy() {
		return this.proxy;
	}
	
	public void setProxy(String url) {
		this.proxy = url;
	}
	
	public int maxUploadingJob() {
		return this.maxUploadingJob;
	}
	
	public GPUDevice getGPUDevice() {
		return this.GPUDevice;
	}
	
	public void setMaxUploadingJob(int max) {
		this.maxUploadingJob = max;
	}
	
	public void setUseNbCores(int nbcores) {
		this.nbCores = nbcores;
	}
	
	public int getNbCores() {
		return this.nbCores;
	}
	
	public void setMaxMemory(int max) {
		this.maxMemory = max;
	}
	
	public int getMaxMemory() {
		return this.maxMemory;
	}
	
	public void setMaxRenderTime(int max) {
		this.maxRenderTime = max;
	}
	
	public int getMaxRenderTime() {
		return this.maxRenderTime;
	}
	
	public void setUsePriority(int priority) {
		if (priority > 19)
			priority = 19;
		if (priority < -19)
			priority = -19;
		
		this.priority = priority;
		
	}
	
	public int getPriority() {
		return this.priority;
	}
	
	public void setPrintLog(boolean val) {
		this.printLog = val;
	}
	
	public boolean getPrintLog() {
		return this.printLog;
	}
	
	public int computeMethodToInt() {
		return this.computeMethod.ordinal();
	}
	
	public ComputeType getComputeMethod() {
		return this.computeMethod;
	}
	
	public void setUseGPU(GPUDevice device) {
		this.GPUDevice = device;
	}
	
	public void setComputeMethod(ComputeType meth) {
		this.computeMethod = meth;
	}
	
	public void setCacheDir(File cache_dir_) {
		removeWorkingDirectory();
		if (cache_dir_ == null) {
			this.userSpecifiedACacheDir = false;
			try {
				this.workingDirectory = File.createTempFile("farm_", "");
				this.workingDirectory.createNewFile(); // hoho...
				this.workingDirectory.delete(); // hoho
				this.workingDirectory.mkdir();
				this.workingDirectory.deleteOnExit();
				
				// since there is no working directory and the client will be working in the system temp directory, 
				// we can also set up a 'permanent' directory for immutable files (like renderer binary)
				
				this.storageDirectory = new File(this.workingDirectory.getParent() + File.separator + "sheepit_binary_cache");
				this.storageDirectory.mkdir();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			this.userSpecifiedACacheDir = true;
			this.workingDirectory = cache_dir_;
			this.storageDirectory = cache_dir_;
		}
		
	}
	
	public void setStorageDir(File dir) {
		if (dir != null) {
			if (dir.exists() == false) {
				dir.mkdir();
			}
			this.storageDirectory = dir;
		}
	}
	
	public File getStorageDir() {
		if (this.storageDirectory == null) {
			return this.workingDirectory;
		}
		else {
			return this.storageDirectory;
		}
	}
	
	public boolean getUserSpecifiedACacheDir() {
		return this.userSpecifiedACacheDir;
	}
	
	public void setExtras(String str) {
		this.extras = str;
	}
	
	public String getExtras() {
		return this.extras;
	}
	
	public void setAutoSignIn(boolean v) {
		this.autoSignIn = v;
	}
	
	public boolean getAutoSignIn() {
		return this.autoSignIn;
	}
	
	public void setUIType(String ui) {
		this.UIType = ui;
	}
	
	public String getUIType() {
		return this.UIType;
	}
	
	public void setTileSize(int size) {
		this.tileSize = size;
	}
	
	public int getTileSize() {
		return this.tileSize;
	}
	
	public void setHostname(String hostname_) {
		this.hostname = hostname_;
	}
	
	public String getHostname() {
		return this.hostname;
	}
	
	public String getDefaultHostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException e) {
			return "";
		}
	}
	
	public void setIncompatibleProcessName(String val) {
		this.incompatibleProcess = val;
	}
	
	public String getIncompatibleProcessName() {
		return this.incompatibleProcess;
	}
	
	public void cleanWorkingDirectory() {
		this.cleanDirectory(this.workingDirectory);
		this.cleanDirectory(this.storageDirectory);
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
			Utils.delete(this.workingDirectory);
		}
	}
	
	public List<File> getLocalCacheFiles() {
		List<File> files_local = new LinkedList<File>();
		List<File> files = new LinkedList<File>();
		if (this.workingDirectory != null) {
			files.addAll(Arrays.asList(this.workingDirectory.listFiles()));
		}
		if (this.storageDirectory != null) {
			files.addAll(Arrays.asList(this.storageDirectory.listFiles()));
		}
		
		for (File file : files) {
			if (file.isFile()) {
				try {
					String extension = file.getName().substring(file.getName().lastIndexOf('.')).toLowerCase();
					String name = file.getName().substring(0, file.getName().length() - 1 * extension.length());
					if (extension.equals(".zip")) {
						// check if the md5 of the file is ok
						String md5_local = Utils.md5(file.getAbsolutePath());
						
						if (md5_local.equals(name)) {
							files_local.add(file);
						}
					}
				}
				catch (StringIndexOutOfBoundsException e) { // because the file does not have an . his path
				}
			}
		}
		return files_local;
	}
	
	public String getJarVersion() {
		String versionPath = "/VERSION";
		
		InputStream versionStream = Client.class.getResourceAsStream(versionPath);
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
	
	public boolean checkOSisSupported() {
		return OS.getOS() != null;
	}
	
	public boolean checkCPUisSupported() {
		OS os = OS.getOS();
		if (os != null) {
			CPU cpu = os.getCPU();
			return cpu != null && cpu.haveData();
		}
		return false;
	}
}
