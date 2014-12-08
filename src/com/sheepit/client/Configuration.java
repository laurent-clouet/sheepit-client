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
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import com.sheepit.client.hardware.gpu.GPUDevice;

public class Configuration {
	public enum ComputeType {
		CPU_GPU, CPU_ONLY, GPU_ONLY
	}; // accept job for ...
	
	public File workingDirectory;
	public File storageDirectory; // for permanent storage (binary archive)
	public boolean userSpecifiedACacheDir;
	public String static_exeDirName;
	private String login;
	private String password;
	private int maxUploadingJob;
	private int nbCores;
	private ComputeType computeMethod;
	private GPUDevice GPUDevice;
	private boolean printLog;
	public List<Pair<Calendar, Calendar>> requestTime;
	private String extras;
	
	public Configuration(File cache_dir_, String login_, String password_) {
		this.login = login_;
		this.password = password_;
		this.static_exeDirName = "exe";
		this.maxUploadingJob = 1;
		this.nbCores = -1; // ie not set
		this.computeMethod = ComputeType.CPU_ONLY;
		this.GPUDevice = null;
		this.userSpecifiedACacheDir = false;
		this.workingDirectory = null;
		this.storageDirectory = null;
		this.setCacheDir(cache_dir_);
		this.printLog = false;
		this.requestTime = null;
		this.extras = "";
	}
	
	public String toString() {
		return String.format("Configuration (workingDirectory '%s')", this.workingDirectory.getAbsolutePath());
	}
	
	public String login() {
		return this.login;
	}
	
	public String password() {
		return this.password;
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
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			this.userSpecifiedACacheDir = true;
			this.workingDirectory = cache_dir_;
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
	
	public void setExtras(String str) {
		this.extras = str;
	}
	
	public String getExtras() {
		return this.extras;
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
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
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
								System.err.println("cleanDirectory find an partial file => remove (" + file.getAbsolutePath() + ")");
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
}
