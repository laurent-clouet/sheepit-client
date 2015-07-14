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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.sheepit.client.Error.ServerCode;
import com.sheepit.client.Error.Type;
import com.sheepit.client.exception.FermeException;
import com.sheepit.client.exception.FermeExceptionNoRightToRender;
import com.sheepit.client.exception.FermeExceptionNoSession;
import com.sheepit.client.exception.FermeExceptionSessionDisabled;
import com.sheepit.client.exception.FermeServerDown;
import com.sheepit.client.os.OS;

public class Client {
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
	
	private ResourceBundle guiResources, exceptionResources;
	
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
		
		this.guiResources = ResourceBundle.getBundle("GUIResources", this.config.getLocale());
		this.exceptionResources = ResourceBundle.getBundle("ExceptionResources", this.config.getLocale());
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
	
	public int run() {
		if (this.config.checkOSisSupported() == false) {
			this.gui.error(Error.humanString(Error.Type.OS_NOT_SUPPORTED, this.config.getLocale()));
			return -3;
		}
		
		if (this.config.checkCPUisSupported() == false) {
			this.gui.error(Error.humanString(Error.Type.CPU_NOT_SUPPORTED, this.config.getLocale()));
			return -4;
		}
		
		int step;
		try {
			step = this.log.newCheckPoint();
			this.gui.status(guiResources.getString("Starting"));
			
			this.config.cleanWorkingDirectory();
			
			Error.Type ret;
			ret = this.server.getConfiguration();
			
			if (ret != Error.Type.OK) {
				this.gui.error(Error.humanString(ret, this.config.getLocale()));
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
						MessageFormat formatter = new MessageFormat(this.guiResources.getString("JobWaiting"), this.guiResources.getLocale());
						this.gui.status(formatter.format(new Object[]{String.format("%tR", next_request)}));
						try {
							Thread.sleep(next_request.getTimeInMillis() - now.getTime());
						}
						catch (InterruptedException e3) {
							
						}
						catch (IllegalArgumentException e3) {
							this.log.errorF("SleepAFail", new Object[]{e3});
						}
					}
					this.gui.status(this.guiResources.getString("RequestingJob"));
					this.renderingJob = this.server.requestJob();
				}
				catch (FermeExceptionNoRightToRender e) {
					this.gui.error(this.exceptionResources.getString("InsufficientRights"));
					return -2;
				}
				catch (FermeExceptionSessionDisabled e) {
					this.gui.error(Error.humanString(Error.Type.SESSION_DISABLED, this.config.getLocale()));
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
								MessageFormat formatter = new MessageFormat(this.guiResources.getString("JobWaiting"), this.guiResources.getLocale());
								this.gui.status(formatter.format(new Object[]{String.format("%tR", next_request)}));
								try {
									Thread.sleep(next_request.getTimeInMillis() - now.getTime());
								}
								catch (InterruptedException e3) {
									
								}
								catch (IllegalArgumentException e3) {
									this.log.errorF("SleepBFail", new Object[]{e3});
								}
							}
							this.gui.status(this.guiResources.getString("RequestingJob"));
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
					MessageFormat formatter = new MessageFormat(this.guiResources.getString("CantConnect"), this.guiResources.getLocale());
					this.gui.status(formatter.format(new Object[]{15}));
					try {
						Thread.sleep(time_sleep);
					}
					catch (InterruptedException e1) {
						return -3;
					}
					continue; // go back to ask job
				}
				catch (FermeException e) {
					//e.getMessage()
					MessageFormat formatter = new MessageFormat(this.exceptionResources.getString("RequestJob"), this.exceptionResources.getLocale());
					this.gui.error(formatter.format(new Object[]{e.getMessage()}));
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					this.log.debugF("RunException", new Object[]{e, sw.toString()});
					this.sendError(step);
					continue;
				}
				
				if (this.renderingJob == null) { // no job
					int time_sleep = 1000 * 60 * 15;
					Date wakeup_time = new Date(new Date().getTime() + time_sleep);
					MessageFormat formatter = new MessageFormat(this.guiResources.getString("NoJobs"), this.guiResources.getLocale());
					this.gui.status(formatter.format(new Object[]{String.format("%tR", wakeup_time)}));
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
				
				this.log.debugF("GotWork", new Object[]{this.renderingJob.getId(), this.renderingJob.getFrameNumber()});
				
				ret = this.work(this.renderingJob);
				if (ret == Error.Type.RENDERER_KILLED) {
					this.log.removeCheckPoint(step);
					continue;
				}
				
				if (ret != Error.Type.OK) {
					Job frame_to_reset = this.renderingJob; // copy it because the sendError will take ~5min to execute
					this.renderingJob = null;
					this.gui.error(Error.humanString(ret, this.config.getLocale()));
					this.sendError(step, frame_to_reset, ret);
					this.log.removeCheckPoint(step);
					continue;
				}
				
				if (this.renderingJob.simultaneousUploadIsAllowed() == false) { // power or compute_method job, need to upload right away
					ret = confirmJob(this.renderingJob);
					if (ret != Error.Type.OK) {
						MessageFormat formatter = new MessageFormat(this.exceptionResources.getString("ConfirmJobProblem"), this.exceptionResources.getLocale());
						this.gui.error(formatter.format(new Object[]{ret}));
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
			this.log.debugF("RunExceptionD", new Object[]{e1, sw.toString()});
			return -99; // the this.stop will be done after the return of this.run()
		}
		
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
		
		this.gui.stop();
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
	
	public int senderLoop() {
		int step = log.newCheckPoint();
		Error.Type ret;
		while (true) {
			Job job_to_send;
			try {
				job_to_send = jobsToValidate.take();
				this.log.debugF("WillValidate", new Object[]{job_to_send});
				//gui.status("Sending frame");
				ret = confirmJob(job_to_send);
				if (ret != Error.Type.OK) {
					this.gui.error(Error.humanString(ret, this.config.getLocale()));
					this.log.debugF("SenderLoopFail", new Object[]{ret});
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
			this.log.debug("ErrorSendingDisabled");
			return;
		}
		
		this.log.debugF("SendingError", new Object[]{error});
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
			this.log.errorF("DownloadExecutableProblem", new Object[]{ret});
			return Error.Type.DOWNLOAD_FILE;
		}
		
		ret = this.downloadSceneFile(ajob);
		if (ret != 0) {
			this.log.errorF("DownloadSceneFileProblem", new Object[]{ret});
			return Error.Type.DOWNLOAD_FILE;
		}
		
		ret = this.prepareWorkingDirectory(ajob); // decompress renderer and scene archives
		if (ret != 0) {
			this.log.errorF("PrepareWorkingDirectory", new Object[]{ret});
			return Error.Type.CAN_NOT_CREATE_DIRECTORY;
		}
		
		File scene_file = new File(ajob.getScenePath());
		File renderer_file = new File(ajob.getRendererPath());
		
		if (scene_file.exists() == false) {
			this.log.errorF("NoSceneFile", new Object[]{scene_file.getAbsolutePath()});
			return Error.Type.MISSING_SCENE;
		}
		
		if (renderer_file.exists() == false) {
			this.log.errorF("NoRendererFile", new Object[]{scene_file.getAbsolutePath()});
			return Error.Type.MISSING_RENDER;
		}
		
		Error.Type err = ajob.render();
		if (err != Error.Type.OK) {
			this.log.errorF("WorkProblem", new Object[]{err});
			return err;
		}
		
		return Error.Type.OK;
	}
	
	protected int downloadSceneFile(Job ajob_) {
		this.gui.status(this.guiResources.getString("DownloadingProject"));
		return this.downloadFile(ajob_, ajob_.getSceneArchivePath(), ajob_.getSceneMD5(), String.format("%s?type=job&job=%s&revision=%s", this.server.getPage("download-archive"), ajob_.getId(), ajob_.getRevision()), "DownloadingProjectPercent");
	}
	
	protected int downloadExecutable(Job ajob) {
		this.gui.status(this.guiResources.getString("DownloadingRenderer"));
		return this.downloadFile(ajob, ajob.getRendererArchivePath(), ajob.getRenderMd5(), String.format("%s?type=binary&job=%s", this.server.getPage("download-archive"), ajob.getId()), "DownloadingRendererPercent");
	}
	
	private int downloadFile(Job ajob, String local_path, String md5_server, String url, String update_ui_key) {
		File local_path_file = new File(local_path);
		
		if (local_path_file.exists() == true) {
			return 0;
		}
		
		// must download the archive
		int ret = this.server.HTTPGetFile(url, local_path, this.gui, update_ui_key);
		boolean md5_check = this.checkFile(ajob, local_path, md5_server);
		int attempts = 1;
		
		while ((ret != 0 || md5_check == false) && attempts < this.maxDownloadFileAttempts) {
			if (ret != 0) {
				MessageFormat formatter = new MessageFormat(this.exceptionResources.getString("HTTPGetFileProblem"), this.exceptionResources.getLocale());
				this.gui.error(formatter.format(new Object[]{ret}));
				this.log.debugF("HTTPGetFileProblemDetailed", new Object[]{ret, local_path});
			}
			else if (md5_check == false) {
				this.gui.error(this.exceptionResources.getString("MD5Mismatch"));
				this.log.debugF("MD5MismatchDetailed2", new Object[]{local_path});
			}
			local_path_file.delete();
			
			this.log.debugF("DownloadFileRetry", new Object[]{new Integer(attempts + 1), new Integer(this.maxDownloadFileAttempts)});
			
			ret = this.server.HTTPGetFile(url, local_path, this.gui, update_ui_key);
			md5_check = this.checkFile(ajob, local_path, md5_server);
			attempts++;
			
			if ((ret != 0 || md5_check == false) && attempts >= this.maxDownloadFileAttempts) {
				this.log.debugF("DownloadFileFail", new Object[]{this.maxDownloadFileAttempts, local_path});
				local_path_file.delete();
				return -9;
			}
		}
		
		return 0;
	}
	
	private boolean checkFile(Job ajob, String local_path, String md5_server) {
		File local_path_file = new File(local_path);
		
		if (local_path_file.exists() == false) {
			this.log.errorF("MD5NonExistent", new Object[]{local_path});
			return false;
		}
		
		String md5_local = Utils.md5(local_path);
		
		if (md5_local.equals(md5_server) == false) {
			this.log.errorF("MD5MismatchDetailed1", new Object[]{md5_local, md5_server, new File(local_path).length()});
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
				MessageFormat formatter = new MessageFormat(this.exceptionResources.getString("UnzipErrorRenderer"), this.exceptionResources.getLocale());
				this.gui.error(formatter.format(new Object[]{ret}));
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
				MessageFormat formatter = new MessageFormat(this.exceptionResources.getString("UnzipErrorScene"), this.exceptionResources.getLocale());
				this.gui.error(formatter.format(new Object[]{ret}));
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
					this.log.debug("ReuploadSleep");
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
}
