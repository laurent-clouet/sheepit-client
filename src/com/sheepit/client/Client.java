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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.sheepit.client.Error.ServerCode;
import com.sheepit.client.Error.Type;
import com.sheepit.client.exception.FermeException;
import com.sheepit.client.exception.FermeExceptionBadResponseFromServer;
import com.sheepit.client.exception.FermeExceptionNoRendererAvailable;
import com.sheepit.client.exception.FermeExceptionNoRightToRender;
import com.sheepit.client.exception.FermeExceptionNoSession;
import com.sheepit.client.exception.FermeExceptionNoSpaceLeftOnDevice;
import com.sheepit.client.exception.FermeExceptionServerInMaintenance;
import com.sheepit.client.exception.FermeExceptionServerOverloaded;
import com.sheepit.client.exception.FermeExceptionSessionDisabled;
import com.sheepit.client.exception.FermeServerDown;
import com.sheepit.client.os.OS;

public class Client {
	private Gui gui;
	private Server server;
	private Configuration config;
	private Log log;
	private Job renderingJob;
	private Job previousJob;
	private BlockingQueue<Job> jobsToValidate;
	private boolean isValidatingJob;
	private long start_time;
	
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
		this.previousJob = null;
		this.jobsToValidate = new ArrayBlockingQueue<Job>(1024);
		this.isValidatingJob = false;
		
		this.disableErrorSending = false;
		this.running = false;
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
	
	public Log getLog() {
		return this.log;
	}
	
	public long getStartTime() {
		return this.start_time;
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
		
		this.running = true;
		
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
			
			this.start_time = new Date().getTime();
			this.server.start(); // for staying alive
			
			// create a thread which will send the frame
			Runnable runnable_sender = new Runnable() {
				public void run() {
					senderLoop();
				}
			};
			Thread thread_sender = new Thread(runnable_sender);
			thread_sender.start();
			
			IncompatibleProcessChecker incompatibleProcessChecker = new IncompatibleProcessChecker(this);
			Timer incompatibleProcessCheckerTimer = new Timer();
			incompatibleProcessCheckerTimer.schedule(incompatibleProcessChecker, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(1));
			
			incompatibleProcessChecker.run(); // before the first request, check if it should be stopped
			
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
				catch (FermeExceptionNoRendererAvailable e) {
					this.gui.error(Error.humanString(Error.Type.RENDERER_NOT_AVAILABLE));
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
						this.start_time = new Date().getTime(); // reset start session time because the server did it
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
				catch (FermeServerDown e) {
					int wait = 15;
					int time_sleep = 1000 * 60 * wait;
					this.gui.status(String.format("Can not connect to server. Please check your connectivity. Will retry in %s minutes", wait));
					try {
						Thread.sleep(time_sleep);
					}
					catch (InterruptedException e1) {
						return -3;
					}
					continue; // go back to ask job
				}
				catch (FermeExceptionServerOverloaded e) {
					int wait = 15;
					int time_sleep = 1000 * 60 * wait;
					this.gui.status(String.format("Server is overloaded and cannot give frame to render. Will retry in %s minutes", wait));
					try {
						Thread.sleep(time_sleep);
					}
					catch (InterruptedException e1) {
						return -3;
					}
					continue; // go back to ask job
				}
				catch (FermeExceptionServerInMaintenance e) {
					int wait = 15;
					int time_sleep = 1000 * 60 * wait;
					this.gui.status(String.format("Server is in maintenance and cannot give frame to render. Will retry in %s minutes", wait));
					try {
						Thread.sleep(time_sleep);
					}
					catch (InterruptedException e1) {
						return -3;
					}
					continue; // go back to ask job
				}
				catch (FermeExceptionBadResponseFromServer e) {
					int wait = 15;
					int time_sleep = 1000 * 60 * wait;
					this.gui.status(String.format("Bad answer from server. Will retry in %s minutes", wait));
					try {
						Thread.sleep(time_sleep);
					}
					catch (InterruptedException e1) {
						return -3;
					}
					continue; // go back to ask job
				}
				catch (FermeException e) {
					this.gui.error("Client::run exception requestJob (1) " + e.getMessage());
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
					this.gui.status(String.format("No job available. Sleeping for 15 minutes (will wake up at %tR)", wakeup_time));
					this.gui.displayStats(new Stats());
					this.suspended = true;
					int time_slept = 0;
					while (time_slept < time_sleep && this.running == true) {
						try {
							Thread.sleep(250);
						}
						catch (InterruptedException e) {
							return -3;
						}
						time_slept += 250;
					}
					this.suspended = false;
					continue; // go back to ask job
				}
				
				this.log.debug("Got work to do id: " + this.renderingJob.getId() + " frame: " + this.renderingJob.getFrameNumber());
				
				ret = this.work(this.renderingJob);
				if (ret == Error.Type.RENDERER_KILLED) {
					this.log.removeCheckPoint(step);
					continue;
				}
				
				if (ret == Error.Type.NO_SPACE_LEFT_ON_DEVICE) {
					Job frame_to_reset = this.renderingJob; // copy it because the sendError will take ~5min to execute
					this.renderingJob = null;
					this.gui.error(Error.humanString(ret));
					this.sendError(step, frame_to_reset, ret);
					this.log.removeCheckPoint(step);
					return -50;
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
						gui.error("Client::run problem with confirmJob (returned " + ret + ")");
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
		this.gui.setSuspended();
	}
	
	public synchronized void resume() {
		suspended = false;
		this.gui.setResumed();
		notify();
	}
	
	public void askForStop() {
		this.log.debug("Client::askForStop");
		this.running = false;
	}
	
	public void cancelStop() {
		this.log.debug("Client::cancelStop");
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
				args += "&frame=" + job_to_reset_.getFrameNumber() + "&job=" + job_to_reset_.getId() + "&render_time=" + job_to_reset_.getProcessRender().getDuration() + "&memoryused=" + job_to_reset_.getProcessRender().getMemoryUsed();
				if (job_to_reset_.getExtras() != null && job_to_reset_.getExtras().isEmpty() == false) {
					args += "&extras=" + job_to_reset_.getExtras();
				}
			}
			this.server.HTTPSendFile(this.server.getPage("error") + args, temp_file.getAbsolutePath());
			temp_file.delete();
		}
		catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			this.log.debug("Client::sendError Exception " + e + " stacktrace: " + sw.toString());
			// no exception should be raised to actual launcher (applet or standalone)
		}
		
		if (error != null && (error == Error.Type.RENDERER_CRASHED || error == Error.Type.RENDERER_KILLED_BY_USER || error == Error.Type.RENDERER_KILLED_BY_SERVER)) {
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
		
		gui.setRenderingProjectName(ajob.getName());
		
		try {
			ret = this.downloadExecutable(ajob);
			if (ret != 0) {
				gui.setRenderingProjectName("");
				this.log.error("Client::work problem with downloadExecutable (ret " + ret + ")");
				return Error.Type.DOWNLOAD_FILE;
			}
			
			ret = this.downloadSceneFile(ajob);
			if (ret != 0) {
				gui.setRenderingProjectName("");
				this.log.error("Client::work problem with downloadSceneFile (ret " + ret + ")");
				return Error.Type.DOWNLOAD_FILE;
			}
			
			ret = this.prepareWorkingDirectory(ajob); // decompress renderer and scene archives
			if (ret != 0) {
				gui.setRenderingProjectName("");
				this.log.error("Client::work problem with this.prepareWorkingDirectory (ret " + ret + ")");
				return Error.Type.CAN_NOT_CREATE_DIRECTORY;
			}
		}
		catch (FermeExceptionNoSpaceLeftOnDevice e) {
			gui.setRenderingProjectName("");
			return Error.Type.NO_SPACE_LEFT_ON_DEVICE;
		}
		
		File scene_file = new File(ajob.getScenePath());
		File renderer_file = new File(ajob.getRendererPath());
		
		if (scene_file.exists() == false) {
			gui.setRenderingProjectName("");
			this.log.error("Client::work job preparation failed (scene file '" + scene_file.getAbsolutePath() + "' does not exist)");
			return Error.Type.MISSING_SCENE;
		}
		
		if (renderer_file.exists() == false) {
			gui.setRenderingProjectName("");
			this.log.error("Client::work job preparation failed (renderer file '" + renderer_file.getAbsolutePath() + "' does not exist)");
			return Error.Type.MISSING_RENDER;
		}
		
		Error.Type err = ajob.render();
		gui.setRenderingProjectName("");
		gui.setRemainingTime("");
		gui.setRenderingTime("");
		gui.setComputeMethod("");
		if (err != Error.Type.OK) {
			this.log.error("Client::work problem with runRenderer (ret " + err + ")");
			return err;
		}
		
		return Error.Type.OK;
	}
	
	protected int downloadSceneFile(Job ajob_) throws FermeExceptionNoSpaceLeftOnDevice {
		return this.downloadFile(ajob_, ajob_.getSceneArchivePath(), ajob_.getSceneMD5(), String.format("%s?type=job&job=%s", this.server.getPage("download-archive"), ajob_.getId()), "project");
	}
	
	protected int downloadExecutable(Job ajob) throws FermeExceptionNoSpaceLeftOnDevice {
		return this.downloadFile(ajob, ajob.getRendererArchivePath(), ajob.getRenderMd5(), String.format("%s?type=binary&job=%s", this.server.getPage("download-archive"), ajob.getId()), "renderer");
	}
	
	private int downloadFile(Job ajob, String local_path, String md5_server, String url, String download_type) throws FermeExceptionNoSpaceLeftOnDevice {
		File local_path_file = new File(local_path);
		String update_ui = "Downloading " + download_type + " %s %%";
		
		if (local_path_file.exists() == true) {
			this.gui.status("Reusing cached " + download_type);
			return 0;
		}
		
		this.gui.status("Downloading " + download_type);
		
		// must download the archive
		int ret = this.server.HTTPGetFile(url, local_path, this.gui, update_ui);
		boolean md5_check = this.checkFile(ajob, local_path, md5_server);
		int attempts = 1;
		
		while ((ret != 0 || md5_check == false) && attempts < this.maxDownloadFileAttempts) {
			if (ret != 0) {
				this.gui.error("Client::downloadFile problem with Server.HTTPGetFile returned " + ret);
				this.log.debug("Client::downloadFile problem with Server.HTTPGetFile (return: " + ret + ") removing local file (path: " + local_path + ")");
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
	
	protected int prepareWorkingDirectory(Job ajob) throws FermeExceptionNoSpaceLeftOnDevice {
		int ret;
		String renderer_archive = ajob.getRendererArchivePath();
		String renderer_path = ajob.getRendererDirectory();
		File renderer_path_file = new File(renderer_path);
		
		if (renderer_path_file.exists()) {
			// Directory already exists -> do nothing
		}
		else {
			this.gui.status("Extracting renderer");
			// we create the directory
			renderer_path_file.mkdir();
			
			// unzip the archive
			ret = Utils.unzipFileIntoDirectory(renderer_archive, renderer_path, null);
			if (ret != 0) {
				this.gui.error("Client::prepareWorkingDirectory, error with Utils.unzipFileIntoDirectory of the renderer (returned " + ret + ")");
				return -1;
			}
			
			try {
				File f = new File(ajob.getRendererPath());
				f.setExecutable(true);
			}
			catch (SecurityException e) {
			}
		}
		
		String scene_archive = ajob.getSceneArchivePath();
		String scene_path = ajob.getSceneDirectory();
		File scene_path_file = new File(scene_path);
		
		if (scene_path_file.exists()) {
			// Directory already exists -> do nothing
		}
		else {
			this.gui.status("Extracting project");
			// we create the directory
			scene_path_file.mkdir();
			
			// unzip the archive
			ret = Utils.unzipFileIntoDirectory(scene_archive, scene_path, ajob.getSceneArchivePassword());
			if (ret != 0) {
				this.gui.error("Client::prepareWorkingDirectory, error with Utils.unzipFileIntoDirectory of the scene (returned " + ret + ")");
				return -2;
			}
		}
		
		return 0;
	}
	
	protected Error.Type confirmJob(Job ajob) {
		String extras_config = "";
		RenderProcess process = ajob.getProcessRender();
		if (process != null && process.getCoresUsed() > 0) {
			extras_config = "&cores=" + process.getCoresUsed();
		}
		
		String url_real = String.format("%s?job=%s&frame=%s&rendertime=%d&memoryused=%s&extras=%s%s", this.server.getPage("validate-job"), ajob.getId(), ajob.getFrameNumber(), ajob.getProcessRender().getDuration(), ajob.getProcessRender().getMemoryUsed(), ajob.getExtras(), extras_config);
		
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
		this.previousJob = ajob;
		return Error.Type.OK;
	}
	
	public Job getPreviousJob() {
		return this.previousJob;
	}
	
	protected boolean shouldWaitBeforeRender() {
		int concurrent_job = this.jobsToValidate.size();
		if (this.isValidatingJob) {
			concurrent_job++;
		}
		return (concurrent_job >= this.config.maxUploadingJob());
	}
}
