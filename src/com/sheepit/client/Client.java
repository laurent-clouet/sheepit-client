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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.sheepit.client.Error.ServerCode;
import com.sheepit.client.Error.Type;
import com.sheepit.client.exception.FermeException;
import com.sheepit.client.exception.FermeExceptionNoRightToRender;
import com.sheepit.client.exception.FermeExceptionNoSession;
import com.sheepit.client.exception.FermeExceptionSessionDisabled;
import com.sheepit.client.hardware.gpu.GPUDevice;
import com.sheepit.client.os.OS;

public class Client {
	public static final String UPDATE_METHOD_BY_REMAINING_TIME = "remainingtime";
	public static final String UPDATE_METHOD_BLENDER_INTERNAL_BY_PART = "blenderinternal";
	
	private Gui gui;
	private Server server;
	private Configuration config;
	private Log log;
	private Job renderingJob;
	private BlockingQueue<Job> jobsToValidate;
	private boolean isValidatingJob;
	
	private boolean disableErrorSending;
	private boolean running;
	private boolean suspended;
	
	private int maxDownloadFileAttempts = 5;
	
	public Client(Gui gui_, Configuration config, String url_) {
		this.config = config;
		this.server = new Server(url_, this.config, this);
		this.log = Log.getInstance(this.config);
		this.gui = gui_;
		this.renderingJob = null;
		this.jobsToValidate = new ArrayBlockingQueue<Job>(1024);
		this.isValidatingJob = false;
		
		this.disableErrorSending = false;
		this.running = true;
		this.suspended = false;
	}
	
	public String toString() {
		return String.format("Client (config %s, server %s)", this.config, this.server);
	}
	
	public Job getRenderingJob() {
		return this.renderingJob;
	}
	
	public Gui getGui() {
		return this.gui;
	}
	
	public Configuration getConfiguration() {
		return this.config;
	}
	
	public Server getServer() {
		return this.server;
	}
	
	public int run() {
		if (this.config.checkOSisSupported() == false) {
			this.gui.error(Error.humanString(Error.Type.OS_NOT_SUPPORTED));
			return -3;
		}
		
		if (this.config.checkCPUisSupported() == false) {
			this.gui.error(Error.humanString(Error.Type.CPU_NOT_SUPPORTED));
			return -4;
		}
		
		int step;
		try {
			step = this.log.newCheckPoint();
			this.gui.status("Starting");
			
			this.config.cleanWorkingDirectory();
			
			Error.Type ret;
			ret = this.server.getConfiguration();
			
			if (ret != Error.Type.OK) {
				this.gui.error(Error.humanString(ret));
				if (ret != Error.Type.AUTHENTICATION_FAILED) {
					Log.printCheckPoint(step);
				}
				return -1;
			}
			
			this.server.start(); // for staying alive
			
			// create a thread which will send the frame
			Runnable runnable_sender = new Runnable() {
				public void run() {
					senderLoop();
				}
			};
			Thread thread_sender = new Thread(runnable_sender);
			thread_sender.start();
			
			while (this.running == true) {
				this.renderingJob = null;
				synchronized (this) {
					while (this.suspended) {
						wait();
					}
				}
				step = this.log.newCheckPoint();
				try {
					Calendar next_request = this.nextJobRequest();
					if (next_request != null) {
						// wait
						Date now = new Date();
						this.gui.status(String.format("Waiting until %tR before requesting job", next_request));
						try {
							Thread.sleep(next_request.getTimeInMillis() - now.getTime());
						}
						catch (InterruptedException e3) {
							
						}
						catch (IllegalArgumentException e3) {
							this.log.error("Client::run sleepA failed " + e3);
						}
					}
					this.gui.status("Requesting Job");
					this.renderingJob = this.server.requestJob();
				}
				catch (FermeExceptionNoRightToRender e) {
					this.gui.error("User does not have enough right to render scene");
					return -2;
				}
				catch (FermeExceptionSessionDisabled e) {
					this.gui.error(Error.humanString(Error.Type.SESSION_DISABLED));
					// should wait forever to actually display the message to the user
					while (true) {
						try {
							Thread.sleep(100000);
						}
						catch (InterruptedException e1) {
						}
					}
				}
				catch (FermeExceptionNoSession e) {
					// User has no session need to re-authenticate
					
					ret = this.server.getConfiguration();
					if (ret != Error.Type.OK) {
						this.renderingJob = null;
					}
					else {
						try {
							Calendar next_request = this.nextJobRequest();
							if (next_request != null) {
								// wait
								Date now = new Date();
								this.gui.status(String.format("Waiting until %tR before requesting job", next_request));
								try {
									Thread.sleep(next_request.getTimeInMillis() - now.getTime());
								}
								catch (InterruptedException e3) {
									
								}
								catch (IllegalArgumentException e3) {
									this.log.error("Client::run sleepB failed " + e3);
								}
							}
							this.gui.status("Requesting Job");
							this.renderingJob = this.server.requestJob();
						}
						catch (FermeException e1) {
							this.renderingJob = null;
						}
					}
				}
				catch (FermeException e) {
					this.gui.error("Client::renderingManagement exception requestJob (1) " + e.getMessage());
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					this.log.debug("Client::run exception " + e + " stacktrace: " + sw.toString());
					this.sendError(step);
					continue;
				}
				
				if (this.renderingJob == null) { // no job
					int time_sleep = 1000 * 60 * 15;
					Date wakeup_time = new Date(new Date().getTime() + time_sleep);
					this.gui.status(String.format("No job available. Sleeping for 15 minutes (will wake up at ~%tR)", wakeup_time));
					this.gui.framesRemaining(0);
					int time_slept = 0;
					while (time_slept < time_sleep && this.running == true) {
						try {
							Thread.sleep(5000);
						}
						catch (InterruptedException e) {
							return -3;
						}
						time_slept += 5000;
					}
					continue; // go back to ask job
				}
				
				this.log.debug("Got work to do id: " + this.renderingJob.getId() + " frame: " + this.renderingJob.getFrameNumber());
				
				ret = this.work(this.renderingJob);
				if (ret == Error.Type.RENDERER_KILLED) {
					this.log.removeCheckPoint(step);
					continue;
				}
				
				if (ret != Error.Type.OK) {
					Job frame_to_reset = this.renderingJob; // copy it because the sendError will take ~5min to execute
					this.renderingJob = null;
					this.gui.error(Error.humanString(ret));
					this.sendError(step, frame_to_reset, ret);
					this.log.removeCheckPoint(step);
					continue;
				}
				
				if (this.renderingJob.simultaneousUploadIsAllowed() == false) { // power or compute_method job, need to upload right away
					ret = confirmJob(this.renderingJob);
					if (ret != Error.Type.OK) {
						gui.error("Client::renderingManagement problem with confirmJob (returned " + ret + ")");
						sendError(step);
					}
					else {
						gui.AddFrameRendered();
					}
				}
				else {
					this.jobsToValidate.add(this.renderingJob);
					this.renderingJob = null;
				}
				
				while (this.shouldWaitBeforeRender() == true) {
					try {
						Thread.sleep(4000); // wait a little bit
					}
					catch (InterruptedException e3) {
					}
				}
				this.log.removeCheckPoint(step);
			}
			
			// not running but maybe still sending frame
			while (this.jobsToValidate.isEmpty() == false) {
				try {
					Thread.sleep(2300); // wait a little bit
				}
				catch (InterruptedException e3) {
				}
			}
		}
		catch (Exception e1) {
			// no exception should be raised in the actual launcher (applet or standalone)
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e1.printStackTrace(pw);
			this.log.debug("Client::run exception(D) " + e1 + " stacktrace: " + sw.toString());
			return -99; // the this.stop will be done after the return of this.run()
		}
		this.gui.stop();
		return 0;
	}
	
	public synchronized int stop() {
		this.running = false;
		this.disableErrorSending = true;
		
		if (this.renderingJob != null) {
			if (this.renderingJob.getProcessRender().getProcess() != null) {
				OS.getOS().kill(this.renderingJob.getProcessRender().getProcess());
				this.renderingJob.setAskForRendererKill(true);
			}
		}
		
		// 		this.config.workingDirectory.delete();
		this.config.removeWorkingDirectory();
		
		if (this.server == null) {
			return 0;
		}
		
		try {
			this.server.HTTPRequest(this.server.getPage("logout"));
		}
		catch (IOException e) {
			// nothing to do: if the logout failed that's ok
		}
		this.server.interrupt();
		try {
			this.server.join();
		}
		catch (InterruptedException e) {
		}
		
		this.server = null;
		
		return 0;
	}
	
	public boolean isSuspended() {
		return this.suspended;
	}
	
	public void suspend() {
		suspended = true;
	}
	
	public synchronized void resume() {
		suspended = false;
		notify();
	}
	
	public void askForStop() {
		System.out.println("Client::askForStop");
		this.running = false;
	}
        
        public void cancelStop() {
		System.out.println("Client::cancelStop");
		this.running = true;
	}
        
        public boolean isRunning() {
		return this.running;
	}
	
	public int senderLoop() {
		int step = log.newCheckPoint();
		Error.Type ret;
		while (true) {
			Job job_to_send;
			try {
				job_to_send = jobsToValidate.take();
				this.log.debug("will validate " + job_to_send);
				//gui.status("Sending frame");
				ret = confirmJob(job_to_send);
				if (ret != Error.Type.OK) {
					this.gui.error(Error.humanString(ret));
					this.log.debug("Client::senderLoop confirm failed, ret: " + ret);
					sendError(step);
				}
				else {
					gui.AddFrameRendered();
				}
			}
			catch (InterruptedException e) {
			}
		}
	}
	
	protected void sendError(int step_) {
		this.sendError(step_, null, null);
	}
	
	protected void sendError(int step_, Job job_to_reset_, Error.Type error) {
		if (this.disableErrorSending) {
			this.log.debug("Error sending is disabled, do not send log");
			return;
		}
		
		this.log.debug("Sending error to server (type: " + error + ")");
		try {
			File temp_file = File.createTempFile("farm_", "");
			temp_file.createNewFile();
			temp_file.deleteOnExit();
			FileOutputStream writer = new FileOutputStream(temp_file);
			
			ArrayList<String> logs = this.log.getForCheckPoint(step_);
			for (String line : logs) {
				writer.write(line.getBytes());
				writer.write('\n');
			}
			
			writer.close();
			String args = "?type=" + (error == null ? "" : error.getValue());
			if (job_to_reset_ != null) {
				args += "&frame=" + job_to_reset_.getFrameNumber() + "&job=" + job_to_reset_.getId() + "&render_time=" + job_to_reset_.getProcessRender().getDuration();
				if (job_to_reset_.getExtras() != null && job_to_reset_.getExtras().isEmpty() == false) {
					args += "&extras=" + job_to_reset_.getExtras();
				}
			}
			this.server.HTTPSendFile(this.server.getPage("error") + args, temp_file.getAbsolutePath());
			temp_file.delete();
		}
		catch (Exception e1) {
			e1.printStackTrace();
			// no exception should be raised to actual launcher (applet or standalone)
		}
		
		if (error != null && error == Error.Type.RENDERER_CRASHED) {
			// do nothing, we can ask for a job right away
		}
		else {
			try {
				Thread.sleep(300000); // sleeping for 5min
			}
			catch (InterruptedException e) {
			}
		}
	}
	
	/**
	 * 
	 * @return the date of the next request, or null if there is not delay (null <=> now)
	 */
	public Calendar nextJobRequest() {
		if (this.config.requestTime == null) {
			return null;
		}
		else {
			Calendar next = null;
			Calendar now = Calendar.getInstance();
			for (Pair<Calendar, Calendar> interval : this.config.requestTime) {
				Calendar start = (Calendar) now.clone();
				Calendar end = (Calendar) now.clone();
				start.set(Calendar.SECOND, 00);
				start.set(Calendar.MINUTE, interval.first.get(Calendar.MINUTE));
				start.set(Calendar.HOUR_OF_DAY, interval.first.get(Calendar.HOUR_OF_DAY));
				
				end.set(Calendar.SECOND, 59);
				end.set(Calendar.MINUTE, interval.second.get(Calendar.MINUTE));
				end.set(Calendar.HOUR_OF_DAY, interval.second.get(Calendar.HOUR_OF_DAY));
				
				if (start.before(now) && now.before(end)) {
					return null;
				}
				if (start.after(now)) {
					if (next == null) {
						next = start;
					}
					else {
						if (start.before(next)) {
							next = start;
						}
					}
				}
			}
			
			return next;
		}
	}
	
	public Error.Type work(Job ajob) {
		int ret;
		
		ret = this.downloadExecutable(ajob);
		if (ret != 0) {
			this.log.error("Client::work problem with downloadExecutable (ret " + ret + ")");
			return Error.Type.DOWNLOAD_FILE;
		}
		
		ret = this.downloadSceneFile(ajob);
		if (ret != 0) {
			this.log.error("Client::work problem with downloadSceneFile (ret " + ret + ")");
			return Error.Type.DOWNLOAD_FILE;
		}
		
		ret = this.prepareWorkingDirectory(ajob); // decompress renderer and scene archives
		if (ret != 0) {
			this.log.error("Client::work problem with this.prepareWorkingDirectory (ret " + ret + ")");
			return Error.Type.CAN_NOT_CREATE_DIRECTORY;
		}
		
		File scene_file = new File(ajob.getScenePath());
		File renderer_file = new File(ajob.getRendererPath());
		
		if (scene_file.exists() == false) {
			this.log.error("Client::work job preparation failed (scene file '" + scene_file.getAbsolutePath() + "' does not exist)");
			return Error.Type.MISSING_SCENE;
		}
		
		if (renderer_file.exists() == false) {
			this.log.error("Client::work job preparation failed (renderer file '" + renderer_file.getAbsolutePath() + "' does not exist)");
			return Error.Type.MISSING_RENDER;
		}
		
		Error.Type err = this.runRenderer(ajob);
		if (err != Error.Type.OK) {
			this.log.error("Client::work problem with runRenderer (ret " + err + ")");
			return err;
		}
		
		return Error.Type.OK;
	}
	
	protected Error.Type runRenderer(Job ajob) {
		this.gui.status("Rendering");
		RenderProcess process = ajob.getProcessRender();
		String core_script = "import bpy\n" + "bpy.context.user_preferences.system.compute_device_type = \"%s\"\n" + "bpy.context.scene.cycles.device = \"%s\"\n" + "bpy.context.user_preferences.system.compute_device = \"%s\"\n";
		if (ajob.getUseGPU() && this.config.getGPUDevice() != null) {
			core_script = String.format(core_script, "CUDA", "GPU", this.config.getGPUDevice().getCudaName());
		}
		else {
			core_script = String.format(core_script, "NONE", "CPU", "CPU");
		}
		core_script += String.format("bpy.context.scene.render.tile_x = %1$d\nbpy.context.scene.render.tile_y = %1$d\n", this.getTileSize(ajob));
		File script_file = null;
		String command1[] = ajob.getRenderCommand().split(" ");
		int size_command = command1.length + 2; // + 2 for script
		
		if (this.config.getNbCores() > 0) { // user has specified something
			size_command += 2;
		}
		
		List<String> command = new ArrayList<String>(size_command);
		
		Map<String, String> new_env = new HashMap<String, String>();
		
		new_env.put("BLENDER_USER_CONFIG", this.config.workingDirectory.getAbsolutePath().replace("\\", "\\\\"));
		
		for (String arg : command1) {
			switch (arg) {
				case ".c":
					command.add(ajob.getScenePath());
					command.add("-P");
					
					try {
						script_file = File.createTempFile("script_", "", this.config.workingDirectory);
						File file = new File(script_file.getAbsolutePath());
						FileWriter txt;
						txt = new FileWriter(file);
						
						PrintWriter out = new PrintWriter(txt);
						out.write(ajob.getScript());
						out.write("\n");
						out.write(core_script); // GPU part
						out.write("\n"); // GPU part
						out.close();
						
						command.add(script_file.getAbsolutePath());
					}
					catch (IOException e) {
						StringWriter sw = new StringWriter();
						e.printStackTrace(new PrintWriter(sw));
						this.log.error("Client:runRenderer exception on script generation, will return UNKNOWN " + e + " stacktrace " + sw.toString());
						return Error.Type.UNKNOWN;
					}
					script_file.deleteOnExit();
					break;
				case ".e":
					command.add(ajob.getRendererPath());
					// the number of cores has to be put after the binary and before the scene arg
					if (this.config.getNbCores() > 0) {
						command.add("-t");
						command.add(Integer.toString(this.config.getNbCores()));
					}
					break;
				case ".o":
					command.add(this.config.workingDirectory.getAbsolutePath() + File.separator + ajob.getPrefixOutputImage());
					break;
				case ".f":
					command.add(ajob.getFrameNumber());
					break;
				default:
					command.add(arg);
					break;
			}
		}
		
		try {
			String line;
			this.log.debug(command.toString());
			OS os = OS.getOS();
			process.start();
			ajob.getProcessRender().setProcess(os.exec(command, new_env));
			BufferedReader input = new BufferedReader(new InputStreamReader(ajob.getProcessRender().getProcess().getInputStream()));
			
			long last_update_status = 0;
			this.log.debug("renderer output");
			try {
				while ((line = input.readLine()) != null) {
					this.updateRenderingMemoryPeak(line, ajob);
					
					this.log.debug(line);
					if ((new Date().getTime() - last_update_status) > 2000) { // only call the update every two seconds
						this.updateRenderingStatus(line, ajob);
						last_update_status = new Date().getTime();
					}
					Type error = this.detectError(line, ajob);
					if (error != Error.Type.OK) {
						if (script_file != null) {
							script_file.delete();
						}
						return error;
					}
				}
				input.close();
			}
			catch (IOException err1) { // for the input.readline
				// most likely The handle is invalid
				this.log.error("Client:runRenderer exception(B) (silent error) " + err1);
			}
			this.log.debug("end of rendering");
		}
		catch (Exception err) {
			if (script_file != null) {
				script_file.delete();
			}
			StringWriter sw = new StringWriter();
			err.printStackTrace(new PrintWriter(sw));
			this.log.error("Client:runRenderer exception(A) " + err + " stacktrace " + sw.toString());
			return Error.Type.FAILED_TO_EXECUTE;
		}
		
		int exit_value = process.exitValue();
		process.finish();
		
		if (script_file != null) {
			script_file.delete();
		}
		
		// find the picture file
		final String filename_without_extension = ajob.getPrefixOutputImage() + ajob.getFrameNumber();
		
		FilenameFilter textFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith(filename_without_extension);
			}
		};
		
		File[] files = this.config.workingDirectory.listFiles(textFilter);
		
		if (files.length == 0) {
			this.log.error("Client::runRenderer no picture file found (after finished render (filename_without_extension " + filename_without_extension + ")");
			
			if (ajob.getAskForRendererKill()) {
				this.log.debug("Client::runRenderer renderer didn't generate any frame but died due to a kill request");
				return Error.Type.RENDERER_KILLED;
			}
			
			String basename = "";
			try {
				basename = ajob.getPath().substring(0, ajob.getPath().lastIndexOf('.'));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			File crash_file = new File(this.config.workingDirectory + File.separator + basename + ".crash.txt");
			if (crash_file.exists()) {
				this.log.error("Client::runRenderer crash file found => the renderer crashed");
				crash_file.delete();
				return Error.Type.RENDERER_CRASHED;
			}
			
			if (exit_value == 127 && process.getDuration() < 10) {
				this.log.error("Client::runRenderer renderer returned 127 and took " + process.getDuration() + "s, some libraries may be missing");
				return Error.Type.RENDERER_MISSING_LIBRARIES;
			}
			
			return Error.Type.NOOUTPUTFILE;
		}
		else {
			ajob.setOutputImagePath(files[0].getAbsolutePath());
			this.log.debug("Client::runRenderer pictureFilename: '" + ajob.getOutputImagePath() + "'");
		}
		
		File scene_dir = new File(ajob.getSceneDirectory());
		long date_modification_scene_directory = (long) Utils.lastModificationTime(scene_dir);
		if (date_modification_scene_directory > process.getStartTime()) {
			scene_dir.delete();
		}
		
		this.gui.status(String.format("Frame rendered in %dmin%ds", process.getDuration() / 60, process.getDuration() % 60));
		
		return Error.Type.OK;
	}
	
	protected int downloadSceneFile(Job ajob_) {
		this.gui.status("Downloading project");
		return this.downloadFile(ajob_, ajob_.getSceneArchivePath(), ajob_.getSceneMD5(), String.format("%s?type=job&job=%s&revision=%s", this.server.getPage("download-archive"), ajob_.getId(), ajob_.getRevision()), "Downloading project %s %%");
	}
	
	protected int downloadExecutable(Job ajob) {
		this.gui.status("Downloading renderer");
		return this.downloadFile(ajob, ajob.getRendererArchivePath(), ajob.getRenderMd5(), String.format("%s?type=binary&job=%s", this.server.getPage("download-archive"), ajob.getId()), "Downloading renderer %s %%");
	}
	
	private int downloadFile(Job ajob, String local_path, String md5_server, String url, String update_ui) {
		File local_path_file = new File(local_path);
		
		if (local_path_file.exists() == true) {
			return 0;
		}
		
		// must download the archive
		int ret = this.server.HTTPGetFile(url, local_path, this.gui, update_ui);
		boolean md5_check = this.checkFile(ajob, local_path, md5_server);
		int attempts = 1;
		
		while ((ret != 0 || md5_check == false) && attempts < this.maxDownloadFileAttempts) {
			if (ret != 0) {
				this.gui.error("Client::downloadFile problem with Utils.HTTPGetFile returned " + ret);
				this.log.debug("Client::downloadFile problem with Utils.HTTPGetFile (return: " + ret + ") removing local file (path: " + local_path + ")");
			}
			else if (md5_check == false) {
				this.gui.error("Client::downloadFile problem with Client::checkFile mismatch on md5");
				this.log.debug("Client::downloadFile problem with Client::checkFile mismatch on md5, removing local file (path: " + local_path + ")");
			}
			local_path_file.delete();
			
			this.log.debug("Client::downloadFile failed, let's try again (" + (attempts + 1) + "/" + this.maxDownloadFileAttempts + ") ...");
			
			ret = this.server.HTTPGetFile(url, local_path, this.gui, update_ui);
			md5_check = this.checkFile(ajob, local_path, md5_server);
			attempts++;
			
			if ((ret != 0 || md5_check == false) && attempts >= this.maxDownloadFileAttempts) {
				this.log.debug("Client::downloadFile failed after " + this.maxDownloadFileAttempts + " attempts, removing local file (path: " + local_path + "), stopping...");
				local_path_file.delete();
				return -9;
			}
		}
		
		return 0;
	}
	
	private boolean checkFile(Job ajob, String local_path, String md5_server) {
		File local_path_file = new File(local_path);
		
		if (local_path_file.exists() == false) {
			this.log.error("Client::checkFile cannot check md5 on a nonexistent file (path: " + local_path + ")");
			return false;
		}
		
		String md5_local = Utils.md5(local_path);
		
		if (md5_local.equals(md5_server) == false) {
			this.log.error("Client::checkFile mismatch on md5 local: '" + md5_local + "' server: '" + md5_server + "' (local size: " + new File(local_path).length() + ")");
			return false;
		}
		
		return true;
	}
	
	protected int prepareWorkingDirectory(Job ajob) {
		int ret;
		String renderer_archive = ajob.getRendererArchivePath();
		String renderer_path = ajob.getRendererDirectory();
		File renderer_path_file = new File(renderer_path);
		
		if (renderer_path_file.exists()) {
			// Directory already exists -> do nothing
		}
		else {
			// we create the directory
			renderer_path_file.mkdir();
			
			// unzip the archive
			ret = Utils.unzipFileIntoDirectory(renderer_archive, renderer_path);
			if (ret != 0) {
				this.gui.error("Client::prepareWorkingDirectory, error with Utils.unzipFileIntoDirectory of the renderer (returned " + ret + ")");
				return -1;
			}
		}
		
		String scene_archive = ajob.getSceneArchivePath();
		String scene_path = ajob.getSceneDirectory();
		File scene_path_file = new File(scene_path);
		
		if (scene_path_file.exists()) {
			// Directory already exists -> do nothing
		}
		else {
			// we create the directory
			scene_path_file.mkdir();
			
			// unzip the archive
			ret = Utils.unzipFileIntoDirectory(scene_archive, scene_path);
			if (ret != 0) {
				this.gui.error("Client::prepareWorkingDirectory, error with Utils.unzipFileIntoDirectory of the scene (returned " + ret + ")");
				return -2;
			}
		}
		
		return 0;
	}
	
	protected Error.Type confirmJob(Job ajob) {
		String extras_config = "";
		if (this.config.getNbCores() > 0) {
			extras_config = "&cores=" + this.config.getNbCores();
		}
		
		String url_real = String.format("%s?job=%s&frame=%s&rendertime=%d&revision=%s&memoryused=%s&extras=%s%s", this.server.getPage("validate-job"), ajob.getId(), ajob.getFrameNumber(), ajob.getProcessRender().getDuration(), ajob.getRevision(), ajob.getProcessRender().getMemoryUsed(), ajob.getExtras(), extras_config);
		
		this.isValidatingJob = true;
		int nb_try = 1;
		int max_try = 3;
		ServerCode ret = ServerCode.UNKNOWN;
		while (nb_try < max_try && ret != ServerCode.OK) {
			ret = this.server.HTTPSendFile(url_real, ajob.getOutputImagePath());
			switch (ret) {
				case OK:
					// no issue, exit the loop
					nb_try = max_try;
					break;
				
				case JOB_VALIDATION_ERROR_SESSION_DISABLED:
				case JOB_VALIDATION_ERROR_BROKEN_MACHINE:
					return Type.SESSION_DISABLED;
					
				case JOB_VALIDATION_ERROR_MISSING_PARAMETER:
					// no point to retry the request
					return Error.Type.UNKNOWN;
					
				default:
					// do nothing, try to do a request on the next loop
					break;
			}
			
			nb_try++;
			if (ret != ServerCode.OK && nb_try < max_try) {
				try {
					this.log.debug("Sleep for 32s before trying to re-upload the frame");
					Thread.sleep(32000);
				}
				catch (InterruptedException e) {
					return Error.Type.UNKNOWN;
				}
			}
		}
		this.isValidatingJob = false;
		
		// we can remove the frame file
		File frame = new File(ajob.getOutputImagePath());
		frame.delete();
		ajob.setOutputImagePath(null);
		
		this.isValidatingJob = false;
		return Error.Type.OK;
	}
	
	protected boolean shouldWaitBeforeRender() {
		int concurrent_job = this.jobsToValidate.size();
		if (this.isValidatingJob) {
			concurrent_job++;
		}
		return (concurrent_job >= this.config.maxUploadingJob());
	}
	
	protected void updateRenderingStatus(String line, Job ajob) {
		if (ajob.getUpdateRenderingStatusMethod() != null && ajob.getUpdateRenderingStatusMethod().equals(Client.UPDATE_METHOD_BLENDER_INTERNAL_BY_PART)) {
			String search = " Part ";
			int index = line.lastIndexOf(search);
			if (index != -1) {
				String buf = line.substring(index + search.length());
				String[] parts = buf.split("-");
				if (parts != null && parts.length == 2) {
					try {
						int current = Integer.parseInt(parts[0]);
						int total = Integer.parseInt(parts[1]);
						if (total != 0) {
							this.gui.status(String.format("Rendering %s %%", (int) (100.0 * current / total)));
							return;
						}
					}
					catch (NumberFormatException e) {
						System.out.println("Exception 92: " + e);
					}
				}
			}
			this.gui.status("Rendering");
		}
		else if (ajob.getUpdateRenderingStatusMethod() == null || ajob.getUpdateRenderingStatusMethod().equals(Client.UPDATE_METHOD_BY_REMAINING_TIME)) {
			String search_remaining = "remaining:";
			int index = line.toLowerCase().indexOf(search_remaining);
			if (index != -1) {
				String buf1 = line.substring(index + search_remaining.length());
				index = buf1.indexOf(" ");
				
				if (index != -1) {
					String remaining_time = buf1.substring(0, index).trim();
					int last_index = remaining_time.lastIndexOf('.'); //format 00:00:00.00 (hr:min:sec)
					if (last_index > 0) {
						remaining_time = remaining_time.substring(0, last_index);
					}
					
					try {
						DateFormat date_parse_minute = new SimpleDateFormat("m:s");
						DateFormat date_parse_hour = new SimpleDateFormat("h:m:s");
						DateFormat date_parse = date_parse_minute;
						if (remaining_time.split(":").length > 2) {
							date_parse = date_parse_hour;
						}
						date_parse.setTimeZone(TimeZone.getTimeZone("GMT"));
						Date date = date_parse.parse(remaining_time);
						this.gui.status(String.format("Rendering (remaining %s)", Utils.humanDuration(date)));
						ajob.getProcessRender().setRemainingDuration((int) (date.getTime() / 1000));
					}
					catch (ParseException err) {
						this.log.error("Client::updateRenderingStatus ParseException " + err);
					}
				}
			}
		}
	}
	
	protected void updateRenderingMemoryPeak(String line, Job ajob) {
		String[] elements = line.toLowerCase().split("(peak)");
		
		for (String element : elements) {
			if (element.isEmpty() == false && element.charAt(0) == ' ') {
				int end = element.indexOf(')');
				if (end > 0) {
					long mem = Utils.parseNumber(element.substring(1, end).trim());
					if (mem > ajob.getProcessRender().getMemoryUsed()) {
						ajob.getProcessRender().setMemoryUsed(mem);
					}
				}
			}
			else {
				if (element.isEmpty() == false && element.charAt(0) == ':') {
					int end = element.indexOf('|');
					if (end > 0) {
						long mem = Utils.parseNumber(element.substring(1, end).trim());
						if (mem > ajob.getProcessRender().getMemoryUsed()) {
							ajob.getProcessRender().setMemoryUsed(mem);
						}
					}
				}
			}
		}
	}
	
	private Type detectError(String line, Job ajob) {
		
		if (line.indexOf("CUDA error: Out of memory") != -1) {
			//	Fra:151 Mem:405.91M (0.00M, Peak 633.81M) | Mem:470.26M, Peak:470.26M | Scene, RenderLayer | Updating Device | Writing constant memory
			//	Fra:151 Mem:405.91M (0.00M, Peak 633.81M) | Mem:470.26M, Peak:470.26M | Scene, RenderLayer | Path Tracing Tile 0/135, Sample 0/200
			//	Fra:151 Mem:405.91M (0.00M, Peak 633.81M) | Mem:470.82M, Peak:470.82M | Scene, RenderLayer | Path Tracing Tile 1/135, Sample 0/200
			//	CUDA error: Out of memory in cuLaunchKernel(cuPathTrace, xblocks , yblocks, 1, xthreads, ythreads, 1, 0, 0, args, 0)
			//	Refer to the Cycles GPU rendering documentation for possible solutions:
			//	http://www.blender.org/manual/render/cycles/gpu_rendering.html
			//	Fra:151 Mem:405.91M (0.00M, Peak 633.81M) | Remaining:09:26.57 | Mem:470.26M, Peak:470.82M | Scene, RenderLayer | Path Tracing Tile 1/135, Sample 200/200
			//	Fra:151 Mem:405.91M (0.00M, Peak 633.81M) | Remaining:00:00.06 | Mem:470.50M, Peak:470.82M | Scene, RenderLayer | Path Tracing Tile 134/135, Sample 0/200
			//	Fra:151 Mem:405.91M (0.00M, Peak 633.81M) | Remaining:00:00.03 | Mem:470.26M, Peak:470.82M | Scene, RenderLayer | Path Tracing Tile 134/135, Sample 200/200
			//	Fra:151 Mem:405.91M (0.00M, Peak 633.81M) | Remaining:00:00.03 | Mem:470.50M, Peak:470.82M | Scene, RenderLayer | Path Tracing Tile 135/135, Sample 0/200
			//	Fra:151 Mem:405.91M (0.00M, Peak 633.81M) | Mem:470.26M, Peak:470.82M | Scene, RenderLayer | Path Tracing Tile 135/135, Sample 200/200
			//	Error: CUDA error: Out of memory in cuLaunchKernel(cuPathTrace, xblocks , yblocks, 1, xthreads, ythreads, 1, 0, 0, args, 0)
			//	Fra:151 Mem:405.91M (0.00M, Peak 633.81M) | Mem:470.26M, Peak:470.82M | Scene, RenderLayer | Cancel | CUDA error: Out of memory in cuLaunchKernel(cuPathTrace, xblocks , yblocks, 1, xthreads, ythreads, 1, 0, 0, args, 0)
			//	Fra:151 Mem:405.89M (0.00M, Peak 633.81M) Sce: Scene Ve:0 Fa:0 La:0
			//	Saved: /tmp/xx/26885_0151.png Time: 00:04.67 (Saving: 00:00.22)
			//	Blender quit
			return Type.RENDERER_OUT_OF_VIDEO_MEMORY;
		}
		else if (line.indexOf("CUDA device supported only with compute capability") != -1) {
			// found bundled python: /tmp/xx/2.73/python
			// read blend: /tmp/xx/compute-method.blend
			// Fra:340 Mem:7.64M (0.00M, Peak 8.23M) | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Synchronizing object | Sun
			// Fra:340 Mem:7.64M (0.00M, Peak 8.23M) | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Synchronizing object | Plane
			// Fra:340 Mem:7.64M (0.00M, Peak 8.23M) | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Synchronizing object | Cube
			// Fra:340 Mem:7.64M (0.00M, Peak 8.23M) | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Synchronizing object | Camera
			// Fra:340 Mem:7.64M (0.00M, Peak 8.23M) | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Initializing
			// Fra:340 Mem:7.64M (0.00M, Peak 8.23M) | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Loading render kernels (may take a few minutes the first time)
			// CUDA device supported only with compute capability 2.0 or up, found 1.2.
			// Refer to the Cycles GPU rendering documentation for possible solutions:
			// http://www.blender.org/manual/render/cycles/gpu_rendering.html
			// Fra:340 Mem:7.64M (0.00M, Peak 8.23M) | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Error | CUDA device supported only with compute capability 2.0 or up, found 1.2.
			// Error: CUDA device supported only with compute capability 2.0 or up, found 1.2.
			// Fra:340 Mem:7.64M (0.00M, Peak 8.23M) | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Waiting for render to start
			// Fra:340 Mem:7.64M (0.00M, Peak 8.23M) | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Cancel | CUDA device supported only with compute capability 2.0 or up, found 1.2.
			// Fra:340 Mem:7.64M (0.00M, Peak 8.23M) Sce: Scene Ve:0 Fa:0 La:0
			// Saved: /tmp/xx/0_0340.png Time: 00:00.12 (Saving: 00:00.03)
			// Blender quit
			return Type.GPU_NOT_SUPPORTED;
		}
		return Type.OK;
	}
	
	protected int getTileSize(Job ajob) {
		int size = 32; // CPU
		GPUDevice gpu = this.config.getGPUDevice();
		if (ajob.getUseGPU() && this.config.getGPUDevice() != null) {
			// GPU
			// if the vram is lower than 1G reduce the size of tile to avoid black output
			size = (gpu.getMemory() > 1073741824L) ? 256 : 128;
		}
		return size;
	}
}
