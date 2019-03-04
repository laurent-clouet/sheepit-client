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

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import com.sheepit.client.hardware.cpu.CPU;
import com.sheepit.client.hardware.gpu.GPUDevice;
import com.sheepit.client.os.OS;

public class Configuration {
	public enum ComputeType {
		CPU_GPU, CPU, GPU
	} // accept job for ...

	public DirectoryLock directoryLock;
	private String configFilePath;
	public File workingDirectory;
	public File storageDirectory; // for permanent storage (binary archive)
	public boolean userHasSpecifiedACacheDir;
	public String static_exeDirName;
	private String login;
	private String password;
	private String proxy;
	private int maxUploadingJob;
	private int nbCores;
	private long maxMemory; // max memory allowed for render
	private int maxRenderTime; // max render time per frame allowed
	private int priority;
	private ComputeType computeMethod;
	private GPUDevice GPUDevice;
	private boolean detectGPUs;
	private boolean printLog;
	public List<Pair<Calendar, Calendar>> requestTime;
	private String extras;
	private boolean autoSignIn;
	private String UIType;
	private int tileSize;
	private String hostname;

	public Configuration(File cache_dir_, String login_, String password_) {
		this.directoryLock = new DirectoryLock();
		this.configFilePath = null;
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
		this.userHasSpecifiedACacheDir = false;
		this.detectGPUs = true;
		this.workingDirectory = null;
		this.storageDirectory = null;
		this.setCacheDir(cache_dir_);
		this.printLog = false;
		this.requestTime = null;
		this.extras = "";
		this.autoSignIn = false;
		this.UIType = null;
		this.tileSize = -1; // ie not set
	}


	public String toString() {
		return String.format("Configuration (workingDirectory '%s')", this.workingDirectory.getAbsolutePath());
	}

	public String getConfigFilePath() {
		return this.configFilePath;
	}

	public void setConfigPath(String val) {
		this.configFilePath = val;
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

	public boolean getDetectGPUs() {
		return this.detectGPUs;
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

	public void setMaxMemory(long max) {
		this.maxMemory = max;
	}

	public long getMaxMemory() {
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

	public void setDetectGPUs(boolean val) {
		this.detectGPUs = val;
	}

	public void setComputeMethod(ComputeType meth) {
		this.computeMethod = meth;
	}

	public void setCacheDir(File cache_dir_) {
		removeWorkingDirectory();
		if (cache_dir_ == null) {
			this.userHasSpecifiedACacheDir = false;
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
			this.userHasSpecifiedACacheDir = true;
			this.workingDirectory = new File(cache_dir_.getAbsolutePath() + File.separator + "sheepit");
			this.storageDirectory = new File(cache_dir_.getAbsolutePath() + File.separator + "sheepit_binary_cache");
			this.workingDirectory.mkdirs();
			this.storageDirectory.mkdirs();

			this.directoryLock.lock();

//			try {
//				Path path = Paths.get(this.workingDirectory + File.separator + LOCK_FILE_NAME);
//				System.out.println("path: " + path);
//				EnumSet<StandardOpenOption> options	= EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.DELETE_ON_CLOSE, StandardOpenOption.SYNC);
//				//				StandardOpenOption.CREATE_NEW, StandardOpenOption.READ);
////				FileChannel channel = FileChannel.open(path, options);
//
//
//				FileChannel fileChannel = FileChannel.open(path, options);
//
//				//fileChannel.lock();
//
////				APPEND
////				If the file is opened for WRITE access then bytes will be written to the end of the file rather than the beginning.
//
////				CREATE
////				Create a new file if it does not exist.
//
////				CREATE_NEW
////				Create a new file, failing if the file already exists.
//
////				DELETE_ON_CLOSE
////				Delete on close.
//
////				DSYNC
////				Requires that every update to the file's content             be written synchronously to the underlying storage device.
////				Requires that every update to the file's content or metadata be written synchronously to the underlying storage device.
//
////				READ
////				Open for read access.
//
////				SPARSE
////				Sparse file.
//
////				SYNC
////				Requires that every update to the file's content or metadata be written synchronously to the underlying storage device.
//
////				TRUNCATE_EXISTING
////				If the file already exists and it is opened for WRITE access, then its length is truncated to 0.
//
////				WRITE
////				Open for write access.
//
//
//			System.out.println("File channel opened for read. Acquiring lock...");
//			this.fileLock = fileChannel.lock(0, Long.MAX_VALUE, true);
//			System.out.println("file locked");
//
////			System.out.println("Lock acquired: " + lock.isValid());
////			System.out.println("Lock is shared: " + lock.isShared());
////
////			ByteBuffer buffer = ByteBuffer.allocate(20);
////			int noOfBytesRead = fileChannel.read(buffer);
////			System.out.println("Buffer contents: ");
////
////			while (noOfBytesRead != -1) {
////
////				buffer.flip();
////				System.out.print("    ");
////
////				while (buffer.hasRemaining()) {
////
////					System.out.print((char) buffer.get());
////				}
////
////				System.out.println(" ");
////
////				buffer.clear();
////				Thread.sleep(1000);
////				noOfBytesRead = fileChannel.read(buffer);
////			}
////
////			fileChannel.close(); // also releases the lock
////			System.out.print("Closing the channel and releasing lock.");
//			}
//			catch (Exception e) {
//				e.printStackTrace();
//			}

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

	public boolean getUserHasSpecifiedACacheDir() {
		return this.userHasSpecifiedACacheDir;
	}

	public File getCacheDirForSettings() {
		if (this.getUserHasSpecifiedACacheDir() == false) {
			return null;
		}
		else {
			// when the user have a cache directory a "sheepit" and "sheepit_binary_cache" is be automaticaly added
			return this.workingDirectory.getParentFile();
		}
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
						else if (this.directoryLock.getLockFileName().equals(file.getName())) {
							// ignore lock file
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
		if (this.userHasSpecifiedACacheDir) {
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
			File[] filesInDirectory = this.workingDirectory.listFiles();
			if (filesInDirectory != null) {
				files.addAll(Arrays.asList(filesInDirectory));
			}
		}
		if (this.storageDirectory != null) {
			File[] filesInDirectory = this.storageDirectory.listFiles();
			if (filesInDirectory != null) {
				files.addAll(Arrays.asList(filesInDirectory));
			}
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

	public class DirectoryLock {
		private File f;
		private FileChannel channel;
		private FileLock lock;

		public String getLockFileName() {
			return ".lock";
		}

		public void lock() {
			try {
				f = new File(getLockFileName());
				// Check if the lock exist
				if (f.exists()) { // if exist try to delete it
					f.delete();
				}
				// Try to get the lock
				channel = new RandomAccessFile(f, "rw").getChannel();
				lock = channel.tryLock();
				if (lock == null) {
					// File is lock by other application
					channel.close();
					throw new RuntimeException("Two instance cant run at a time.");
				}
				// Add shutdown hook to release lock when application shutdown
				//				ShutdownHook shutdownHook = new ShutdownHook();
				//				Runtime.getRuntime().addShutdownHook(shutdownHook);

			}
			catch (IOException e) {
				throw new RuntimeException("Could not start process.", e);
			}
		}

		public void unlock() {
			// release and delete file lock
			try {
				if (lock != null) {
					lock.release();
					channel.close();
					f.delete();
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
