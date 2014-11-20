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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.sheepit.client.Error.ServerCode;
import com.sheepit.client.Error.Type;
import com.sheepit.client.exception.FermeException;
import com.sheepit.client.exception.FermeExceptionNoRightToRender;
import com.sheepit.client.exception.FermeExceptionNoSession;
import com.sheepit.client.exception.FermeExceptionSessionDisabled;
import com.sheepit.client.os.OS;

public class Client {
	public static final String UPDATE_METHOD_BY_LINE_NUMBER = "linenumber";
	public static final String UPDATE_METHOD_BY_REMAINING_TIME = "remainingtime";
	
	private Gui gui;
	private Server server;
	private Configuration config;
	private Log log;
	private Job renderingJob;
	private BlockingQueue<Job> jobsToValidate;
	private boolean isValidatingJob;
	
	private boolean disableErrorSending;
	private boolean running;
	
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
	
	public int run() {
		int step;
		try {
			step = this.log.newCheckPoint();
			this.gui.status("Starting");
			
			this.config.cleanWorkingDirectory();
			
			Error.Type ret;
			ret = this.server.getConfiguration();
			
			if (ret != Error.Type.OK) {
				this.gui.error(Error.humainString(ret));
				if (ret != Error.Type.AUTHENTICATION_FAILED) {
					Log.printCheckPoint(step);
				}
				return -1;
			}
			
			this.server.start(); // for staying alive
			
			// create a thread who will send the frame
			Runnable runnable_sender = new Runnable() {
				public void run() {
					senderLoop();
				}
			};
			Thread thread_sender = new Thread(runnable_sender);
			thread_sender.start();
			
			this.renderingJob = null;
			
			while (this.running == true) {
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
					this.gui.error("User does not enough right to render scene");
					return -2;
				}
				catch (FermeExceptionSessionDisabled e) {
					this.gui.error(Error.humainString(Error.Type.SESSION_DISABLED));
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
					// User have no session need to re-authenticate
					
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
				if (ret != Error.Type.OK) {
					Job frame_to_reset = this.renderingJob; // copie it because the sendError will take ~5min to execute
					this.renderingJob = null;
					this.gui.error(Error.humainString(ret));
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
			while (this.jobsToValidate.size() > 0) {
				try {
					Thread.sleep(2300); // wait a little bit
				}
				catch (InterruptedException e3) {
				}
			}
		}
		catch (Exception e1) {
			// no exception should be raise to actual launcher (applet or standalone)
			return -99; // the this.stop will be done after the return of this.run()
		}
		
		return 0;
	}
	
	public synchronized int stop() {
		System.out.println("Client::stop");
		this.running = false;
		this.disableErrorSending = true;
		
		if (this.renderingJob != null) {
			if (this.renderingJob.getProcess() != null) {
				OS.getOS().kill(this.renderingJob.getProcess());
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
				job_to_send = (Job) jobsToValidate.take();
				this.log.debug("will validate " + job_to_send);
				//gui.status("Sending frame");
				ret = confirmJob(job_to_send);
				if (ret != Error.Type.OK) {
					this.gui.error(Error.humainString(ret));
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
			this.log.debug("Error sending is disable, do not send log");
			return;
		}
		
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
			String args = "";
			if (job_to_reset_ != null) {
				args = "?frame=" + job_to_reset_.getFrameNumber() + "&job=" + job_to_reset_.getId() + "&render_time=" + job_to_reset_.getRenderDuration();
				if (job_to_reset_.getExtras() != null && job_to_reset_.getExtras().length() > 0) {
					args += "&extras=" + job_to_reset_.getExtras();
				}
			}
			this.server.HTTPSendFile(this.server.getPage("error") + args, temp_file.getAbsolutePath());
			temp_file.delete();
		}
		catch (Exception e1) {
			e1.printStackTrace();
			// no exception should be raise to actual launcher (applet or standalone)
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
	 * @return the date of the next request, or null is there is not delay (null <=> now)
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
		if (ajob.workeable() == false) {
			this.log.error("Client::work The received job is not workeable");
			return Error.Type.WRONG_CONFIGURATION;
		}
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
		
		ret = this.prepareWorkeableDirectory(ajob); // decompress renderer and scene archives
		if (ret != 0) {
			this.log.error("Client::work problem with this.prepareWorkeableDirectory (ret " + ret + ")");
			return Error.Type.CAN_NOT_CREATE_DIRECTORY;
		}
		
		File scene_file = new File(ajob.getScenePath());
		File renderer_file = new File(ajob.getRendererPath());
		
		if (scene_file.exists() == false) {
			this.log.error("Client::work job prepration failed (scene file '" + scene_file.getAbsolutePath() + "' does not exist)");
			return Error.Type.MISSING_SCENE;
		}
		
		if (renderer_file.exists() == false) {
			this.log.error("Client::work job prepration failed (renderer file '" + renderer_file.getAbsolutePath() + "' does not exist)");
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
		String core_script = "";
		if (ajob.getUseGPU() && this.config.getGPUDevice() != null) {
			core_script += "import bpy\n" + "bpy.context.user_preferences.system.compute_device_type = \"CUDA\"" + "\n" + "bpy.context.scene.cycles.device = \"GPU\"" + "\n" + "bpy.context.user_preferences.system.compute_device = \"" + this.config.getGPUDevice().getCudaName() + "\"\n" + "bpy.context.scene.render.tile_x = 256" + "\n" + "bpy.context.scene.render.tile_y = 256" + "\n";
		}
		else {
			core_script += "import bpy\n" + "bpy.context.user_preferences.system.compute_device_type = \"NONE\"" + "\n" + "bpy.context.scene.cycles.device = \"CPU\"" + "\n" + "bpy.context.scene.render.tile_x = 32" + "\n" + "bpy.context.scene.render.tile_y = 32" + "\n";
		}
		File script_file = null;
		String command1[] = ajob.getRenderCommand().split(" ");
		int size_command = command1.length + 2; // + 2 for script
		
		if (this.config.getNbCores() > 0) { // user have specified something
			size_command += 2;
		}
		
		String[] command = new String[size_command];
		
		int index = 0;
		for (int i = 0; i < command1.length; i++) {
			if (command1[i].equals(".c")) {
				command[index] = ajob.getScenePath();
				index += 1;
				command[index] = "-P";
				index += 1;
				
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
					
					command[index] = script_file.getAbsolutePath();
				}
				catch (IOException e) {
					return Error.Type.UNKNOWN;
				}
				script_file.deleteOnExit();
			}
			else if (command1[i].equals(".e")) {
				command[index] = ajob.getRendererPath();
				// the number of cores have to be put after the binary and before the scene arg
				if (this.config.getNbCores() > 0) {
					index += 1;
					command[index] = "-t";
					index += 1;
					command[index] = Integer.toString(this.config.getNbCores());
					//index += 1; // do not do it, it will be done at the end of the loop 
				}
			}
			else if (command1[i].equals(".o")) {
				command[index] = this.config.workingDirectory.getAbsolutePath() + File.separator + ajob.getPrefixOutputImage();
			}
			else if (command1[i].equals(".f")) {
				command[index] = ajob.getFrameNumber();
			}
			else {
				command[index] = command1[i];
			}
			index += 1;
		}
		
		long rending_start = new Date().getTime();
		
		int nb_lines = 0;
		try {
			String line;
			this.log.debug(Arrays.toString(command));
			OS os = OS.getOS();
			ajob.setProcess(os.exec(command));
			BufferedReader input = new BufferedReader(new InputStreamReader(ajob.getProcess().getInputStream()));
			
			long last_update_status = 0;
			this.log.debug("renderer output");
			while ((line = input.readLine()) != null) {
				nb_lines++;
				this.updateRenderingMemoryPeak(line, ajob);
				
				this.log.debug(line);
				if ((new Date().getTime() - last_update_status) > 2000) { // only call the update every two seconds
					this.updateRenderingStatus(line, nb_lines, ajob);
					last_update_status = new Date().getTime();
				}
			}
			input.close();
			this.log.debug("end of rendering");
		}
		catch (Exception err) {
			StringWriter sw = new StringWriter();
			err.printStackTrace(new PrintWriter(sw));
			this.log.error("Client:runRenderer exception(A) " + err + " stacktrace " + sw.toString());
			return Error.Type.FAILED_TO_EXECUTE;
		}
		
		long rending_end = new Date().getTime();
		
		if (script_file != null) {
			script_file.delete();
		}
		
		ajob.setRenderDuration((int) ((rending_end - rending_start) / 1000 + 1)); // render time is in seconds but the getTime is in millisecond
		
		ajob.setMaxOutputNbLines(nb_lines);
		int exit_value = 0;
		try {
			exit_value = ajob.getProcess().exitValue();
		}
		catch (IllegalThreadStateException e) {
			// the process is not finished yet
			exit_value = 0;
		}
		catch (Exception e) {
			// actually is for java.io.IOException: GetExitCodeProcess error=6, The handle is invalid
			// it was not declared throwable
			
			// the process is not finished yet
			exit_value = 0;
		}
		
		ajob.setProcess(null);
		
		// find the picture file
		final String namefile_without_extension = ajob.getPrefixOutputImage() + ajob.getFrameNumber();
		
		FilenameFilter textFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith(namefile_without_extension);
			}
		};
		
		File[] files = this.config.workingDirectory.listFiles(textFilter);
		
		if (files.length == 0) {
			this.log.error("Client::runRenderer no picture file found (after finished render (namefile_without_extension " + namefile_without_extension + ")");
			
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
			
			if (exit_value == 127 && ajob.getRenderDuration() < 10) {
				this.log.error("Client::runRenderer renderer return 127 and render time took " + ajob.getRenderDuration() + "s, mostly missing libraries");
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
		if (date_modification_scene_directory > rending_start) {
			scene_dir.delete();
		}
		
		this.gui.status(String.format("Frame rendered in %dmin%ds", ajob.getRenderDuration() / 60, ajob.getRenderDuration() % 60));
		
		return Error.Type.OK;
	}
	
	protected int downloadSceneFile(Job ajob_) {
		this.gui.status("Downloading scene");
		
		String achive_local_path = ajob_.getSceneArchivePath();
		
		File renderer_achive_local_path_file = new File(achive_local_path);
		
		if (renderer_achive_local_path_file.exists()) {
			// the archive have been already downloaded
		}
		else {
			// we must download the archive
			int ret;
			String real_url;
			real_url = String.format("%s?type=job&job=%s&revision=%s", this.server.getPage("download-archive"), ajob_.getId(), ajob_.getRevision());
			ret = this.server.HTTPGetFile(real_url, achive_local_path, this.gui, "Downloading scene %s %%");
			if (ret != 0) {
				this.gui.error("Client::downloadSceneFile problem with Utils.DownloadFile returned " + ret);
				return -1;
			}
			
			String md5_local;
			md5_local = Utils.md5(achive_local_path);
			
			if (md5_local.equals(ajob_.getSceneMD5()) == false) {
				System.err.println("md5 of the downloaded file  and the local file are not the same (local '" + md5_local + "' scene: '" + ajob_.getSceneMD5() + "')");
				this.log.error("Client::downloadSceneFile mismatch on md5  local: '" + md5_local + "' server: '" + ajob_.getSceneMD5() + "'");
				// md5 of the file downloaded and the file excepted is not the same
				return -2;
			}
		}
		return 0;
	}
	
	protected int downloadExecutable(Job ajob) {
		this.gui.status("Downloading renderer");
		String real_url = new String();
		real_url = String.format("%s?type=binary&job=%s", this.server.getPage("download-archive"), ajob.getId());
		
		// we have the MD5 of the renderer archive
		String renderer_achive_local_path = ajob.getRendererArchivePath();
		File renderer_achive_local_path_file = new File(renderer_achive_local_path);
		
		if (renderer_achive_local_path_file.exists()) {
			// the archive have been already downloaded
		}
		else {
			// we must download the archive
			int ret;
			ret = this.server.HTTPGetFile(real_url, renderer_achive_local_path, this.gui, "Downloading renderer %s %%");
			if (ret != 0) {
				this.gui.error("Client::downloadExecutable problem with Utils.DownloadFile returned " + ret);
				return -9;
			}
		}
		
		String md5_local;
		md5_local = Utils.md5(renderer_achive_local_path);
		
		if (md5_local.equals(ajob.getRenderMd5()) == false) {
			this.log.error("Client::downloadExecutable mismatch on md5  local: '" + md5_local + "' server: '" + ajob.getRenderMd5() + "'");
			// md5 of the file downloaded and the file excepted is not the same
			return -10;
		}
		return 0;
	}
	
	protected int prepareWorkeableDirectory(Job ajob) {
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
				this.gui.error("Client::prepareWorkeableDirectory, error with Utils.unzipFileIntoDirectory of the renderer (returned " + ret + ")");
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
				this.gui.error("Client::prepareWorkeableDirectory, error with Utils.unzipFileIntoDirectory of the scene (returned " + ret + ")");
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
		
		String url_real = String.format("%s?job=%s&frame=%s&rendertime=%d&revision=%s&memoryused=%s&extras=%s%s", this.server.getPage("validate-job"), ajob.getId(), ajob.getFrameNumber(), ajob.getRenderDuration(), ajob.getRevision(), ajob.getMemoryUsed(), ajob.getExtras(), extras_config);
		
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
	
	protected void updateRenderingStatus(String line, int current_number_of_lines, Job ajob) {
		if (ajob.getUpdateRenderingStatusMethod() != null && ajob.getUpdateRenderingStatusMethod().equals(Client.UPDATE_METHOD_BY_LINE_NUMBER) && ajob.getMaxOutputNbLines() > 0) {
			this.gui.status(String.format("Rendering %s %%", (int) (100.0 * current_number_of_lines / ajob.getMaxOutputNbLines())));
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
						SimpleDateFormat date_output_minute = new SimpleDateFormat("mm'min'ss");
						SimpleDateFormat date_output_hour = new SimpleDateFormat("HH'h'mm'min'ss");
						DateFormat date_parse = date_parse_minute;
						DateFormat date_output = date_output_minute;
						if (remaining_time.split(":").length > 2) {
							date_parse = date_parse_hour;
							date_output = date_output_hour;
						}
						Date d1 = date_parse.parse(remaining_time);
						this.gui.status(String.format("Rendering (remaining %s)", date_output.format(d1)));
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
					if (mem > ajob.getMemoryUsed()) {
						ajob.setMemoryUsed(mem);
					}
				}
			}
			else {
				if (element.isEmpty() == false && element.charAt(0) == ':') {
					int end = element.indexOf('|');
					if (end > 0) {
						long mem = Utils.parseNumber(element.substring(1, end).trim());
						if (mem > ajob.getMemoryUsed()) {
							ajob.setMemoryUsed(mem);
						}
					}
				}
			}
		}
	}
}
