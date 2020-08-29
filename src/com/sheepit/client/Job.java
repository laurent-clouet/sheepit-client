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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.sheepit.client.Configuration.ComputeType;
import com.sheepit.client.Error.Type;
import com.sheepit.client.hardware.cpu.CPU;
import com.sheepit.client.hardware.gpu.GPUDevice;
import com.sheepit.client.hardware.gpu.opencl.OpenCL;
import com.sheepit.client.os.OS;
import lombok.Data;
import lombok.Getter;

@Data public class Job {
	public static final String UPDATE_METHOD_BY_REMAINING_TIME = "remainingtime";
	public static final String UPDATE_METHOD_BLENDER_INTERNAL_BY_PART = "blenderinternal";
	public static final String UPDATE_METHOD_BY_TILE = "by_tile";
	
	public static final int SHOW_BASE_ICON = -1;
	
	private String frameNumber;
	private String sceneMD5;
	private String rendererMD5;
	private String id;
	private String outputImagePath;
	private long outputImageSize;
	private String path; // path inside of the archive
	private String rendererCommand;
	private String validationUrl;
	private String script;
	private boolean useGPU;
	private String name;
	private String password;
	private String extras;
	private String updateRenderingStatusMethod;
	private String blenderShortVersion;
	private String blenderLongVersion;
	private boolean synchronousUpload;
	private RenderProcess render;
	private boolean askForRendererKill;
	private boolean userBlockJob;
	private boolean serverBlockJob;
	private Gui gui;
	private Configuration configuration;
	private Log log;
	
	public Job(Configuration config_, Gui gui_, Log log_, String id_, String frame_, String path_, boolean use_gpu, String command_, String validationUrl_,
			String script_, String sceneMd5_, String rendererMd5_, String name_, String password_, String extras_, boolean synchronous_upload_,
			String update_method_) {
		configuration = config_;
		id = id_;
		frameNumber = frame_;
		path = path_;
		useGPU = use_gpu;
		rendererCommand = command_;
		validationUrl = validationUrl_;
		sceneMD5 = sceneMd5_;
		rendererMD5 = rendererMd5_;
		name = name_;
		password = password_;
		extras = extras_;
		synchronousUpload = synchronous_upload_;
		gui = gui_;
		outputImagePath = null;
		outputImageSize = 0;
		script = script_;
		updateRenderingStatusMethod = update_method_;
		askForRendererKill = false;
		userBlockJob = false;
		serverBlockJob = false;
		log = log_;
		render = new RenderProcess();
		blenderShortVersion = null;
		blenderLongVersion = null;
	}
	
	public void block() {
		setAskForRendererKill(true);
		setUserBlockJob(true);
		RenderProcess process = getProcessRender();
		if (process != null) {
			OS.getOS().kill(process.getProcess());
		}
	}
	
	public RenderProcess getProcessRender() {
		return render;
	}
	
	public String toString() {
		return String
				.format("Job (numFrame '%s' sceneMD5 '%s' rendererMD5 '%s' ID '%s' pictureFilename '%s' jobPath '%s' gpu %s name '%s' extras '%s' updateRenderingStatusMethod '%s' render %s)",
						frameNumber, sceneMD5, rendererMD5, id, outputImagePath, path, useGPU, name, extras, updateRenderingStatusMethod, render);
	}
	
	public String getPrefixOutputImage() {
		return id + "_";
	}
	
	public String getRendererDirectory() {
		return configuration.getWorkingDirectory().getAbsolutePath() + File.separator + rendererMD5;
	}
	
	public String getRendererPath() {
		return getRendererDirectory() + File.separator + OS.getOS().getRenderBinaryPath();
	}
	
	public String getRendererArchivePath() {
		return configuration.getStorageDir().getAbsolutePath() + File.separator + rendererMD5 + ".zip";
	}
	
	public String getSceneDirectory() {
		return configuration.getWorkingDirectory().getAbsolutePath() + File.separator + sceneMD5;
	}
	
	public String getScenePath() {
		return getSceneDirectory() + File.separator + this.path;
	}
	
	public String getSceneArchivePath() {
		return configuration.getWorkingDirectory().getAbsolutePath() + File.separator + sceneMD5 + ".zip";
	}
	
	public Error.Type render(Observer renderStarted) {
		gui.status("Rendering");
		RenderProcess process = getProcessRender();
		Timer timerOfMaxRenderTime = null;
		String core_script = "";
		// When sending Ctrl+C to the terminal it also get's sent to all subprocesses e.g. also the render process.
		// The java program handles Ctrl+C but the renderer quits on Ctrl+C.
		// This script causes the renderer to ignore Ctrl+C.
		String ignore_signal_script = "import signal\n" + "def hndl(signum, frame):\n" + "    pass\n" + "signal.signal(signal.SIGINT, hndl)\n";
		if (isUseGPU() && configuration.getGPUDevice() != null && configuration.getComputeMethod() != ComputeType.CPU) {
			// If using a GPU, check the proper tile size
			int tileSize = configuration.getGPUDevice().getRenderbucketSize();
			
			core_script = "sheepit_set_compute_device(\"" + configuration.getGPUDevice().getType() + "\", \"GPU\", \"" + configuration.getGPUDevice().getId()
					+ "\")\n";
			core_script += String.format("bpy.context.scene.render.tile_x = %1$d\nbpy.context.scene.render.tile_y = %1$d\n", tileSize);
			
			log.debug(String.format("Rendering bucket size set to %1$dx%1$d pixels", tileSize));
			gui.setComputeMethod("GPU");
		}
		else {
			// Otherwise (CPU), fix the tile size to 32x32px
			core_script = "sheepit_set_compute_device(\"NONE\", \"CPU\", \"CPU\")\n";
			core_script += String.format("bpy.context.scene.render.tile_x = %1$d\nbpy.context.scene.render.tile_y = %1$d\n", CPU.MIN_RENDERBUCKET_SIZE);
			gui.setComputeMethod("CPU");
		}
		
		core_script += ignore_signal_script;
		File script_file = null;
		String command1[] = getRendererCommand().split(" ");
		int size_command = command1.length + 2; // + 2 for script
		
		if (configuration.getNbCores() > 0) { // user has specified something
			size_command += 2;
		}
		
		List<String> command = new ArrayList<String>(size_command);
		
		Map<String, String> new_env = new HashMap<String, String>();
		
		new_env.put("BLENDER_USER_CONFIG", configuration.getWorkingDirectory().getAbsolutePath().replace("\\", "\\\\"));
		new_env.put("CORES", Integer.toString(configuration.getNbCores()));
		new_env.put("PRIORITY", Integer.toString(configuration.getPriority()));
		new_env.put("PYTHONPATH", ""); // make sure blender is using the embedded python, if not it could create "Fatal Python error: Py_Initialize"
		new_env.put("PYTHONHOME", "");// make sure blender is using the embedded python, if not it could create "Fatal Python error: Py_Initialize"
		
		if (isUseGPU() && configuration.getGPUDevice() != null && configuration.getComputeMethod() != ComputeType.CPU && OpenCL.TYPE
				.equals(configuration.getGPUDevice().getType())) {
			new_env.put("CYCLES_OPENCL_SPLIT_KERNEL_TEST", "1");
		}
		
		for (String arg : command1) {
			switch (arg) {
				case ".c":
					command.add(getScenePath());
					command.add("-P");
					
					try {
						script_file = File.createTempFile("script_", "", configuration.getWorkingDirectory());
						File file = new File(script_file.getAbsolutePath());
						FileWriter txt;
						txt = new FileWriter(file);
						
						PrintWriter out = new PrintWriter(txt);
						out.write(getScript());
						out.write("\n");
						out.write(core_script); // GPU part
						out.write("\n"); // GPU part
						out.close();
						
						command.add(script_file.getAbsolutePath());
					}
					catch (IOException e) {
						StringWriter sw = new StringWriter();
						e.printStackTrace(new PrintWriter(sw));
						log.error("Job::render exception on script generation, will return UNKNOWN " + e + " stacktrace " + sw.toString());
						return Error.Type.UNKNOWN;
					}
					script_file.deleteOnExit();
					break;
				case ".e":
					command.add(getRendererPath());
					// the number of cores has to be put after the binary and before the scene arg
					if (configuration.getNbCores() > 0) {
						command.add("-t");
						command.add(Integer.toString(configuration.getNbCores()));
					}
					break;
				case ".o":
					command.add(configuration.getWorkingDirectory().getAbsolutePath() + File.separator + getPrefixOutputImage());
					break;
				case ".f":
					command.add(getFrameNumber());
					break;
				default:
					command.add(arg);
					break;
			}
		}
		
		try {
			renderStartedObservable event = new renderStartedObservable(renderStarted);
			String line;
			log.debug(command.toString());
			OS os = OS.getOS();
			process.setCoresUsed(configuration.getNbCores());
			process.start();
			getProcessRender().setProcess(os.exec(command, new_env));
			BufferedReader input = new BufferedReader(new InputStreamReader(getProcessRender().getProcess().getInputStream()));
			
			// Make initial test/power frames ignore the maximum render time in user configuration. Initial test frames have Job IDs below 20
			// so we just activate the user defined timeout when the scene is not one of the initial ones.
			if (configuration.getMaxRenderTime() > 0 && Integer.parseInt(this.getId()) >= 20) {
				timerOfMaxRenderTime = new Timer();
				timerOfMaxRenderTime.schedule(new TimerTask() {
					@Override public void run() {
						RenderProcess process = getProcessRender();
						if (process != null) {
							long duration = (new Date().getTime() - process.getStartTime()) / 1000; // in seconds
							if (configuration.getMaxRenderTime() > 0 && duration > configuration.getMaxRenderTime()) {
								setAskForRendererKill(true);
								log.debug("Killing render because process duration");
								OS.getOS().kill(process.getProcess());
							}
						}
					}
				}, configuration.getMaxRenderTime() * 1000 + 2000); // +2s to be sure the delay is over
			}
			
			log.debug("renderer output");
			try {
				int progress = -1;
				
				Pattern tilePattern = Pattern.compile(" (Rendered|Path Tracing Tile|Rendering) (\\d+)\\s?\\/\\s?(\\d+)( Tiles| samples|,)");
				
				// Initialise the progress bar in the icon and the UI (0% completed at this time)
				gui.updateTrayIcon(0);
				gui.status("Preparing scene", 0);
				
				while ((line = input.readLine()) != null) {
					log.debug(line);
					
					// Process lines until the version is loaded (usually first or second line of log)
					if (blenderLongVersion == null) {
						Pattern blenderPattern = Pattern.compile("Blender (([0-9]{1,3}\\.[0-9]{0,3}).*)$");
						Matcher blendDetectedVersion = blenderPattern.matcher(line);
						
						if (blendDetectedVersion.find()) {
							blenderLongVersion  = blendDetectedVersion.group(1);
							blenderShortVersion = blendDetectedVersion.group(2);
						}
					}
					
					progress = computeRenderingProgress(line, tilePattern, progress);
					
					updateRenderingMemoryPeak(line);
					if (configuration.getMaxMemory() != -1 && process.getMemoryUsed() > configuration.getMaxMemory()) {
						log.debug("Blocking render because process ram used (" + process.getMemoryUsed() + "k) is over user setting (" + configuration
								.getMaxMemory() + "k)");
						OS.getOS().kill(process.getProcess());
						process.finish();
						if (script_file != null) {
							script_file.delete();
						}
						
						// Once the process is finished (either finished successfully or with an error) move back to
						// base icon (isolated S with no progress bar)
						gui.updateTrayIcon(Job.SHOW_BASE_ICON);
						
						return Error.Type.RENDERER_OUT_OF_MEMORY;
					}
					
					updateRenderingStatus(line);
					Type error = detectError(line);
					if (error != Error.Type.OK) {
						if (script_file != null) {
							script_file.delete();
						}
						
						// Put back base icon
						gui.updateTrayIcon(Job.SHOW_BASE_ICON);
						
						return error;
					}
					
					if (event.isStarted() == false && (process.getMemoryUsed() > 0 || process.getRemainingDuration() > 0)) {
						event.doNotifyIsStarted();
					}
				}
				input.close();
			}
			catch (IOException err1) { // for the input.readline
				// most likely The handle is invalid
				log.error("Job::render exception(B) (silent error) " + err1);
			}
			
			// Put back base icon
			gui.updateTrayIcon(Job.SHOW_BASE_ICON);
			
			log.debug("end of rendering");
		}
		catch (Exception err) {
			if (script_file != null) {
				script_file.delete();
			}
			StringWriter sw = new StringWriter();
			err.printStackTrace(new PrintWriter(sw));
			log.error("Job::render exception(A) " + err + " stacktrace " + sw.toString());
			return Error.Type.FAILED_TO_EXECUTE;
		}
		
		int exit_value = process.exitValue();
		process.finish();
		if (timerOfMaxRenderTime != null) {
			timerOfMaxRenderTime.cancel();
		}
		
		if (script_file != null) {
			script_file.delete();
		}
		
		// find the picture file
		final String filename_without_extension = getPrefixOutputImage() + getFrameNumber();
		
		FilenameFilter textFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith(filename_without_extension);
			}
		};
		
		File[] files = configuration.getWorkingDirectory().listFiles(textFilter);
		
		if (isAskForRendererKill()) {
			log.debug("Job::render been asked to end render");
			
			long duration = (new Date().getTime() - process.getStartTime()) / 1000; // in seconds
			if (configuration.getMaxRenderTime() > 0 && duration > configuration.getMaxRenderTime() && Integer.parseInt(this.getId()) >= 20) {
				log.debug("Render killed because process duration (" + duration + "s) is over user setting (" + configuration.getMaxRenderTime() + "s)");
				return Error.Type.RENDERER_KILLED_BY_USER_OVER_TIME;
			}
			
			if (files.length != 0) {
				new File(files[0].getAbsolutePath()).delete();
			}
			if (isServerBlockJob()) {
				return Error.Type.RENDERER_KILLED_BY_SERVER;
			}
			if (isUserBlockJob()) {
				return Error.Type.RENDERER_KILLED_BY_USER;
			}
			return Error.Type.RENDERER_KILLED;
		}
		
		if (files.length == 0) {
			log.error("Job::render no picture file found (after finished render (filename_without_extension " + filename_without_extension + ")");
			
			String basename = "";
			try {
				basename = getPath().substring(0, getPath().lastIndexOf('.'));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			File crash_file = new File(configuration.getWorkingDirectory() + File.separator + basename + ".crash.txt");
			if (crash_file.exists()) {
				log.error("Job::render crash file found => the renderer crashed");
				crash_file.delete();
				return Error.Type.RENDERER_CRASHED;
			}
			
			if (exit_value == 127 && process.getDuration() < 10) {
				log.error("Job::render renderer returned 127 and took " + process.getDuration() + "s, some libraries may be missing");
				return Error.Type.RENDERER_MISSING_LIBRARIES;
			}
			
			return Error.Type.NOOUTPUTFILE;
		}
		else {
			setOutputImagePath(files[0].getAbsolutePath());
			this.outputImageSize = new File(getOutputImagePath()).length();
			log.debug(String.format("Job::render pictureFilename: %s, size: %d'", getOutputImagePath(), this.outputImageSize));
		}
		
		File scene_dir = new File(getSceneDirectory());
		long date_modification_scene_directory = (long) Utils.lastModificationTime(scene_dir);
		if (date_modification_scene_directory > process.getStartTime()) {
			scene_dir.delete();
		}
		
		gui.status(String.format("Frame rendered in %dmin%ds", process.getDuration() / 60, process.getDuration() % 60));
		
		return Error.Type.OK;
	}
	
	private int computeRenderingProgress(String line, Pattern tilePattern, int currentProgress) {
		Matcher standardTileInfo = tilePattern.matcher(line);
		int newProgress = currentProgress;
		
		if (standardTileInfo.find()) {
			int tileJustProcessed = Integer.parseInt(standardTileInfo.group(2));
			int totalTilesInJob = Integer.parseInt(standardTileInfo.group(3));
			
			newProgress = Math.abs((tileJustProcessed * 100) / totalTilesInJob);
		}
		
		// Only update the tray icon and the screen if percentage has changed
		if (newProgress != currentProgress) {
			gui.updateTrayIcon(newProgress);
			gui.status("Rendering", newProgress);
		}
		
		return newProgress;
	}
	
	private void updateRenderingStatus(String line) {
		if (getUpdateRenderingStatusMethod() != null && getUpdateRenderingStatusMethod().equals(Job.UPDATE_METHOD_BLENDER_INTERNAL_BY_PART)) {
			String search = " Part ";
			int index = line.lastIndexOf(search);
			if (index != -1) {
				String buf = line.substring(index + search.length());
				String[] parts = buf.split("-");
				if (parts.length == 2) {
					try {
						int current = Integer.parseInt(parts[0]);
						int total = Integer.parseInt(parts[1]);
						if (total != 0) {
							long end_render = (new Date().getTime() - this.render.getStartTime()) * total / current;
							Date date = new Date(end_render);
							gui.setRemainingTime(String.format("%s %% (%s)", (int) (100.0 - 100.0 * current / total), Utils.humanDuration(date)));
							getProcessRender().setRemainingDuration((int) (date.getTime() / 1000));
							return;
						}
					}
					catch (NumberFormatException e) {
						System.out.println("Exception 92: " + e);
					}
				}
			}
			gui.status("Rendering");
		}
		else if (getUpdateRenderingStatusMethod() == null || getUpdateRenderingStatusMethod().equals(Job.UPDATE_METHOD_BY_REMAINING_TIME)) {
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
						gui.setRemainingTime(Utils.humanDuration(date));
						getProcessRender().setRemainingDuration((int) (date.getTime() / 1000));
					}
					catch (ParseException err) {
						log.error("Client::updateRenderingStatus ParseException " + err);
					}
				}
			}
		}
		else if (getUpdateRenderingStatusMethod().equals(Job.UPDATE_METHOD_BY_TILE)) {
			String search = " Tile ";
			int index = line.lastIndexOf(search);
			if (index != -1) {
				String buf = line.substring(index + search.length());
				String[] parts = buf.split("/");
				if (parts.length == 2) {
					try {
						int current = Integer.parseInt(parts[0]);
						int total = Integer.parseInt(parts[1]);
						if (total != 0) {
							gui.status(String.format("Rendering %s %%", (int) (100.0 * current / total)));
							return;
						}
					}
					catch (NumberFormatException e) {
						System.out.println("Exception 94: " + e);
					}
				}
			}
			gui.status("Rendering");
		}
	}
	
	private void updateRenderingMemoryPeak(String line) {
		String[] elements = line.toLowerCase().split("(peak)");
		
		for (String element : elements) {
			if (element.isEmpty() == false && element.charAt(0) == ' ') {
				int end = element.indexOf(')');
				if (end > 0) {
					try {
						long mem = Utils.parseNumber(element.substring(1, end).trim()) / 1000; // internal use of ram is in kB
						if (mem > getProcessRender().getMemoryUsed()) {
							getProcessRender().setMemoryUsed(mem);
						}
					}
					catch (IllegalStateException | NumberFormatException e) {
						// failed to parseNumber
					}
				}
			}
			else {
				if (element.isEmpty() == false && element.charAt(0) == ':') {
					int end = element.indexOf('|');
					if (end > 0) {
						try {
							long mem = Utils.parseNumber(element.substring(1, end).trim()) / 1000; // internal use of ram is in kB
							if (mem > getProcessRender().getMemoryUsed()) {
								getProcessRender().setMemoryUsed(mem);
							}
						}
						catch (IllegalStateException | NumberFormatException e) {
							// failed to parseNumber
						}
					}
				}
			}
		}
	}
	
	private Type detectError(String line) {
		
		if (line.contains("CUDA error: Out of memory")) {
			// Fra:151 Mem:405.91M (0.00M, Peak 633.81M) | Mem:470.26M, Peak:470.26M | Scene, RenderLayer | Updating Device | Writing constant memory
			// Fra:151 Mem:405.91M (0.00M, Peak 633.81M) | Mem:470.26M, Peak:470.26M | Scene, RenderLayer | Path Tracing Tile 0/135, Sample 0/200
			// Fra:151 Mem:405.91M (0.00M, Peak 633.81M) | Mem:470.82M, Peak:470.82M | Scene, RenderLayer | Path Tracing Tile 1/135, Sample 0/200
			// CUDA error: Out of memory in cuLaunchKernel(cuPathTrace, xblocks , yblocks, 1, xthreads, ythreads, 1, 0, 0, args, 0)
			// Refer to the Cycles GPU rendering documentation for possible solutions:
			// http://www.blender.org/manual/render/cycles/gpu_rendering.html
			// Fra:151 Mem:405.91M (0.00M, Peak 633.81M) | Remaining:09:26.57 | Mem:470.26M, Peak:470.82M | Scene, RenderLayer | Path Tracing Tile 1/135, Sample 200/200
			// Fra:151 Mem:405.91M (0.00M, Peak 633.81M) | Remaining:00:00.06 | Mem:470.50M, Peak:470.82M | Scene, RenderLayer | Path Tracing Tile 134/135, Sample 0/200
			// Fra:151 Mem:405.91M (0.00M, Peak 633.81M) | Remaining:00:00.03 | Mem:470.26M, Peak:470.82M | Scene, RenderLayer | Path Tracing Tile 134/135, Sample 200/200
			// Fra:151 Mem:405.91M (0.00M, Peak 633.81M) | Remaining:00:00.03 | Mem:470.50M, Peak:470.82M | Scene, RenderLayer | Path Tracing Tile 135/135, Sample 0/200
			// Fra:151 Mem:405.91M (0.00M, Peak 633.81M) | Mem:470.26M, Peak:470.82M | Scene, RenderLayer | Path Tracing Tile 135/135, Sample 200/200
			// Error: CUDA error: Out of memory in cuLaunchKernel(cuPathTrace, xblocks , yblocks, 1, xthreads, ythreads, 1, 0, 0, args, 0)
			// Fra:151 Mem:405.91M (0.00M, Peak 633.81M) | Mem:470.26M, Peak:470.82M | Scene, RenderLayer | Cancel | CUDA error: Out of memory in cuLaunchKernel(cuPathTrace, xblocks , yblocks, 1, xthreads, ythreads, 1, 0, 0, args, 0)
			// Fra:151 Mem:405.89M (0.00M, Peak 633.81M) Sce: Scene Ve:0 Fa:0 La:0
			// Saved: /tmp/xx/26885_0151.png Time: 00:04.67 (Saving: 00:00.22)
			// Blender quit
			return Type.RENDERER_OUT_OF_VIDEO_MEMORY;
		}
		else if (line.contains("CUDA error at cuCtxCreate: Out of memory")) {
			// renderer output
			// CUDA error at cuCtxCreate: Out of memory
			// Refer to the Cycles GPU rendering documentation for possible solutions:
			// http://www.blender.org/manual/render/cycles/gpu_rendering.html
			// found bundled python: /tmp/aaaa/bbbb/2.78/python
			// read blend: /tmp/aaaa/bbbb/compute-method.blend
			// Fra:340 Mem:7.25M (0.00M, Peak 7.25M) | Time:00:00.13 | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Synchronizing object | Sun
			// Fra:340 Mem:7.25M (0.00M, Peak 7.25M) | Time:00:00.13 | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Synchronizing object | Plane
			// Fra:340 Mem:7.25M (0.00M, Peak 7.26M) | Time:00:00.13 | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Synchronizing object | Cube
			// Fra:340 Mem:7.25M (0.00M, Peak 7.26M) | Time:00:00.13 | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Synchronizing object | Camera
			// Fra:340 Mem:7.25M (0.00M, Peak 7.26M) | Time:00:00.13 | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Initializing
			// Fra:340 Mem:7.25M (0.00M, Peak 7.26M) | Time:00:00.13 | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Loading render kernels (may take a few minutes the first time)
			// Fra:340 Mem:7.25M (0.00M, Peak 7.26M) | Time:00:00.13 | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Error | CUDA error at cuCtxCreate: Out of memory
			// Error: CUDA error at cuCtxCreate: Out of memory
			// Fra:340 Mem:7.25M (0.00M, Peak 7.26M) | Time:00:00.13 | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Waiting for render to start
			// Fra:340 Mem:7.25M (0.00M, Peak 7.26M) | Time:00:00.13 | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Cancel | CUDA error at cuCtxCreate: Out of memory
			// CUDA error: Invalid value in cuCtxDestroy(cuContext)
			// Fra:340 Mem:7.25M (0.00M, Peak 7.26M) | Time:00:00.13 | Sce: Scene Ve:0 Fa:0 La:0
			// Blender quit
			// end of rendering
			return Type.RENDERER_OUT_OF_VIDEO_MEMORY;
		}
		else if (line.contains("CUDA error: Launch exceeded timeout in")) {
			// Fra:420 Mem:102.41M (0.00M, Peak 215.18M) | Remaining:01:08.44 | Mem:176.04M, Peak:199.23M | Scene, RenderLayer | Path Tracing Tile 2/24, Sample 10/14
			// Fra:420 Mem:102.41M (0.00M, Peak 215.18M) | Remaining:01:07.08 | Mem:175.48M, Peak:199.23M | Scene, RenderLayer | Path Tracing Tile 2/24, Sample 14/14
			// Fra:420 Mem:102.41M (0.00M, Peak 215.18M) | Remaining:01:07.11 | Mem:176.04M, Peak:199.23M | Scene, RenderLayer | Path Tracing Tile 3/24, Sample 0/14
			// CUDA error: Launch exceeded timeout in cuCtxSynchronize()
			// Refer to the Cycles GPU rendering documentation for possible solutions:
			// http://www.blender.org/manual/render/cycles/gpu_rendering.html
			// CUDA error: Launch exceeded timeout in cuMemcpyDtoH((uchar*)mem.data_pointer + offset, (CUdeviceptr)(mem.device_pointer + offset), size)
			// Fra:420 Mem:102.41M (0.00M, Peak 215.18M) | Remaining:03:04.30 | Mem:176.04M, Peak:199.23M | Scene, RenderLayer | Path Tracing Tile 3/24, Sample 1/14
			// CUDA error: Launch exceeded timeout in cuMemcpyDtoH((uchar*)mem.data_pointer + offset, (CUdeviceptr)(mem.device_pointer + offset), size)
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// Fra:420 Mem:102.41M (0.00M, Peak 215.18M) | Remaining:02:01.87 | Mem:175.48M, Peak:199.23M | Scene, RenderLayer | Path Tracing Tile 3/24, Sample 14/14
			// CUDA error: Launch exceeded timeout in cuMemAlloc(&device_pointer, size)
			// CUDA error: Launch exceeded timeout in cuMemAlloc(&device_pointer, size)
			// Fra:420 Mem:102.41M (0.00M, Peak 215.18M) | Remaining:02:01.87 | Mem:176.04M, Peak:199.23M | Scene, RenderLayer | Path Tracing Tile 4/24, Sample 0/14
			// Fra:420 Mem:102.41M (0.00M, Peak 215.18M) | Remaining:01:27.05 | Mem:176.04M, Peak:199.23M | Scene, RenderLayer | Path Tracing Tile 4/24, Sample 14/14
			// CUDA error: Launch exceeded timeout in cuMemAlloc(&device_pointer, size)
			// CUDA error: Launch exceeded timeout in cuMemAlloc(&device_pointer, size)
			// Fra:420 Mem:102.41M (0.00M, Peak 215.18M) | Remaining:00:00.75 | Mem:185.66M, Peak:199.23M | Scene, RenderLayer | Path Tracing Tile 24/24, Sample 0/14
			// Fra:420 Mem:102.41M (0.00M, Peak 215.18M) | Mem:185.66M, Peak:199.23M | Scene, RenderLayer | Path Tracing Tile 24/24, Sample 14/14
			// Error: CUDA error: Launch exceeded timeout in cuCtxSynchronize()
			// Fra:420 Mem:102.41M (0.00M, Peak 215.18M) | Mem:185.66M, Peak:199.23M | Scene, RenderLayer | Cancel | CUDA error: Launch exceeded timeout in cuCtxSynchronize()
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// CUDA error: Launch exceeded timeout in cuMemFree(cuda_device_ptr(mem.device_pointer))
			// Mem:109.00M (0.00M, Peak 215.18M) | Elapsed 00:00.00 | Tree Compositing Nodetree, Tile 1-6
			// Mem:109.00M (0.00M, Peak 215.18M) | Elapsed 00:00.00 | Tree Compositing Nodetree, Tile 2-6
			// Mem:109.00M (0.00M, Peak 215.18M) | Elapsed 00:00.00 | Tree Compositing Nodetree, Tile 3-6
			// Mem:109.00M (0.00M, Peak 215.18M) | Elapsed 00:00.00 | Tree Compositing Nodetree, Tile 4-6
			// Mem:109.00M (0.00M, Peak 215.18M) | Elapsed 00:00.00 | Tree Compositing Nodetree, Tile 5-6
			// Mem:109.00M (0.00M, Peak 215.18M) | Elapsed 00:00.00 | Tree Compositing Nodetree, Tile 6-6
			// Fra:420 Mem:109.00M (0.00M, Peak 215.18M) Sce: Scene Ve:0 Fa:0 La:0
			// Saved: /tmp/xx/1234_0420.bmp Time: 00:18.29 (Saving: 00:00.06)
			// Blender quit
			// end of rendering
			return Type.RENDERER_OUT_OF_VIDEO_MEMORY;
		}
		else if (line.contains("CUDA error: Invalid value in cuTexRefSetAddress(")) {
			// Fra:83 Mem:1201.77M (0.00M, Peak 1480.94M) | Time:00:59.30 | Mem:894.21M, Peak:894.21M | color 3, RenderLayer | Updating Mesh | Copying Strands to device
			// Fra:83 Mem:1316.76M (0.00M, Peak 1480.94M) | Time:01:02.84 | Mem:1010.16M, Peak:1010.16M | color 3, RenderLayer | Cancel | CUDA error: Invalid value in cuTexRefSetAddress(NULL, texref, cuda_device_ptr(mem.device_pointer), size)
			// Error: CUDA error: Invalid value in cuTexRefSetAddress(NULL, texref, cuda_device_ptr(mem.device_pointer), size)
			// Fra:83 Mem:136.82M (0.00M, Peak 1480.94M) | Time:01:03.40 | Sce: color 3 Ve:0 Fa:0 La:0
			// Blender quit
			// CUDA error: Invalid value in cuTexRefSetAddress(NULL, texref, cuda_device_ptr(mem.device_pointer), size)
			// Refer to the Cycles GPU rendering documentation for possible solutions:
			// http://www.blender.org/manual/render/cycles/gpu_rendering.html
			return Error.Type.RENDERER_OUT_OF_VIDEO_MEMORY;
		}
		else if (line.contains("CUDA error: Launch failed in cuCtxSynchronize()")) {
			// Fra:60 Mem:278.24M (0.00M, Peak 644.01M) | Time:05:08.95 | Remaining:00:03.88 | Mem:210.79M, Peak:210.79M | Scene, W Laser | Path Tracing Tile 16/18, Sample 36/36
			// Fra:60 Mem:278.24M (0.00M, Peak 644.01M) | Time:05:08.96 | Remaining:00:00.82 | Mem:211.04M, Peak:211.04M | Scene, W Laser | Path Tracing Tile 17/18, Sample 36/36
			// Fra:60 Mem:278.24M (0.00M, Peak 644.01M) | Time:05:08.96 | Mem:211.11M, Peak:211.11M | Scene, W Laser | Path Tracing Tile 18/18
			// Error: CUDA error: Launch failed in cuCtxSynchronize(), line 1372
			// Fra:60 Mem:278.24M (0.00M, Peak 644.01M) | Time:05:08.96 | Mem:211.11M, Peak:211.11M | Scene, W Laser | Cancel | CUDA error: Launch failed in cuCtxSynchronize(), line 1372
			// Cycles shader graph connect: can only connect closure to closure (Invert.Color to Mix Shader.Closure1).
			// Cycles shader graph connect: can only connect closure to closure (Mix Shader.Closure to Bump.Normal).
			// CUDA error: Launch failed in cuCtxSynchronize(), line 1372
			// Refer to the Cycles GPU rendering documentation for possible solutions:
			// https://docs.blender.org/manual/en/dev/render/cycles/gpu_rendering.html
			// CUDA error: Launch failed in cuMemcpyDtoH((uchar*)mem.data_pointer + offset, (CUdeviceptr)(mem.device_pointer + offset), size), line 591
			// CUDA error: Launch failed in cuMemcpyDtoH((uchar*)mem.data_pointer + offset, (CUdeviceptr)(mem.device_pointer + offset), size), line 591
			// CUDA error: Launch failed in cuMemFree(cuda_device_ptr(mem.device_pointer)), line 615
			return Error.Type.RENDERER_OUT_OF_VIDEO_MEMORY;
		}
		else if (line.contains("CUDA error: Illegal address in cuCtxSynchronize()")) {
			// Fra:124 Mem:434.77M (0.00M, Peak 435.34M) | Time:25:50.81 | Remaining:01:10:05.16 | Mem:175.14M, Peak:265.96M | Scene, RenderLayer | Path Tracing Tile 34/135, Sample 800/800, Denoised 17 tiles
			// Fra:124 Mem:432.71M (0.00M, Peak 435.34M) | Time:25:50.81 | Remaining:01:10:04.95 | Mem:264.84M, Peak:266.90M | Scene, RenderLayer | Path Tracing Tile 34/135, Sample 800/800, Denoised 18 tiles
			// Fra:124 Mem:434.77M (0.00M, Peak 435.34M) | Time:25:50.82 | Remaining:01:07:20.83 | Mem:266.90M, Peak:266.90M | Scene, RenderLayer | Path Tracing Tile 35/135, Sample 800/800, Denoised 18 tiles
			// Fra:124 Mem:432.71M (0.00M, Peak 435.34M) | Time:25:50.82 | Remaining:01:07:20.63 | Mem:356.60M, Peak:358.67M | Scene, RenderLayer | Path Tracing Tile 35/135, Sample 800/800, Denoised 19 tiles
			// Fra:124 Mem:434.77M (0.00M, Peak 435.34M) | Time:25:50.82 | Remaining:01:04:45.63 | Mem:358.67M, Peak:358.67M | Scene, RenderLayer | Path Tracing Tile 36/135, Sample 800/800, Denoised 19 tiles
			// Fra:124 Mem:432.71M (0.00M, Peak 435.34M) | Time:25:50.82 | Remaining:01:04:45.45 | Mem:448.37M, Peak:450.43M | Scene, RenderLayer | Path Tracing Tile 36/135, Sample 800/800, Denoised 20 tiles
			// Fra:124 Mem:434.77M (0.00M, Peak 435.34M) | Time:25:50.83 | Remaining:01:02:18.83 | Mem:450.43M, Peak:450.43M | Scene, RenderLayer | Path Tracing Tile 37/135, Sample 800/800, Denoised 20 tiles
			// CUDA error: Illegal address in cuCtxSynchronize(), line 1372
			// Refer to the Cycles GPU rendering documentation for possible solutions:
			return Error.Type.RENDERER_OUT_OF_VIDEO_MEMORY;
		}
		else if (line.contains("CUDA device supported only with compute capability")) {
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
		else if (line.contains("terminate called after throwing an instance of 'boost::filesystem::filesystem_error'")) {
			// Fra:2103 Mem:29.54M (0.00M, Peak 29.54M) | Time:00:00.24 | Mem:1.64M, Peak:1.64M | Scene, RenderLayer | Updating Mesh | Computing attributes
			// Fra:2103 Mem:29.54M (0.00M, Peak 29.54M) | Time:00:00.24 | Mem:1.64M, Peak:1.64M | Scene, RenderLayer | Updating Mesh | Copying Attributes to device
			// Fra:2103 Mem:29.54M (0.00M, Peak 29.54M) | Time:00:00.24 | Mem:1.97M, Peak:1.97M | Scene, RenderLayer | Updating Scene BVH | Building
			// Fra:2103 Mem:29.54M (0.00M, Peak 29.54M) | Time:00:00.24 | Mem:1.97M, Peak:1.97M | Scene, RenderLayer | Updating Scene BVH | Building BVH
			// Fra:2103 Mem:29.54M (0.00M, Peak 29.54M) | Time:00:00.24 | Mem:1.97M, Peak:1.97M | Scene, RenderLayer | Updating Scene BVH | Looking in BVH cache
			// Fra:2103 Mem:29.54M (0.00M, Peak 29.54M) | Time:00:00.27 | Mem:1.97M, Peak:1.97M | Scene, RenderLayer | Updating Scene BVH | Packing BVH triangles and strands
			// Fra:2103 Mem:29.54M (0.00M, Peak 29.54M) | Time:00:00.27 | Mem:1.97M, Peak:1.97M | Scene, RenderLayer | Updating Scene BVH | Packing BVH nodes
			// Fra:2103 Mem:29.54M (0.00M, Peak 29.54M) | Time:00:00.27 | Mem:1.97M, Peak:1.97M | Scene, RenderLayer | Updating Scene BVH | Writing BVH cache
			// terminate called after throwing an instance of 'boost::filesystem::filesystem_error'
			//   what():  boost::filesystem::create_directory: Permission denied: "/var/local/cache"
			return Error.Type.NOOUTPUTFILE;
		}
		else if (line.contains("terminate called after throwing an instance of 'std::bad_alloc'")) {
			// Fra:80 Mem:1333.02M (0.00M, Peak 1651.23M) | Mem:780.37M, Peak:780.37M | Scene, RenderLayer | Updating Mesh BVH Plane.083 171/2 | Building BVH
			// Fra:80 Mem:1333.02M (0.00M, Peak 1651.23M) | Mem:780.37M, Peak:780.37M | Scene, RenderLayer | Updating Mesh BVH Mesh 172/2 | Building BVH
			// Fra:80 Mem:1333.02M (0.00M, Peak 1651.23M) | Mem:780.37M, Peak:780.37M | Scene, RenderLayer | Updating Mesh BVH Mesh 172/2 | Packing BVH triangles and strands
			// Fra:80 Mem:1333.02M (0.00M, Peak 1651.23M) | Mem:780.37M, Peak:780.37M | Scene, RenderLayer | Updating Mesh BVH Mesh 172/2 | Packing BVH nodes
			// Fra:80 Mem:1333.02M (0.00M, Peak 1651.23M) | Mem:780.37M, Peak:780.37M | Scene, RenderLayer | Updating Scene BVH | Building
			// Fra:80 Mem:1333.02M (0.00M, Peak 1651.23M) | Mem:780.37M, Peak:780.37M | Scene, RenderLayer | Updating Scene BVH | Building BVH
			// terminate called after throwing an instance of 'std::bad_alloc'
			//   what():  std::bad_alloc
			return Error.Type.RENDERER_OUT_OF_MEMORY;
		}
		else if (line.contains("what(): std::bad_alloc")) {
			// Fra:7 Mem:1247.01M (0.00M, Peak 1247.01M) | Time:00:28.84 | Mem:207.63M, Peak:207.63M | Scene, RenderLayer | Updating Scene BVH | Building BVH 93%, duplicates 0%terminate called recursively
			// terminate called after throwing an instance of 'St9bad_alloc'
			// what(): std::bad_alloc
			// scandir: Cannot allocate memory
			return Error.Type.RENDERER_OUT_OF_MEMORY;
		}
		else if (line.contains("EXCEPTION_ACCESS_VIOLATION")) {
			// Fra:638 Mem:342.17M (63.28M, Peak 735.33M) | Time:00:07.65 | Remaining:02:38.28 | Mem:246.91M, Peak:262.16M | scene_top_01_90, chip_top_view_scene_01 | Path Tracing Tile 57/2040, Denoised 0 tiles
			// Fra:638 Mem:342.32M (63.28M, Peak 735.33M) | Time:00:07.70 | Remaining:02:38.20 | Mem:247.05M, Peak:262.16M | scene_top_01_90, chip_top_view_scene_01 | Path Tracing Tile 58/2040, Denoised 0 tiles
			// Error: EXCEPTION_ACCESS_VIOLATION
			return Error.Type.RENDERER_CRASHED;
		}
		else if (line.contains("Fatal Python error: Py_Initialize")) {
			// Fatal Python error: Py_Initialize: unable to load the file system codec
			// ImportError: No module named 'encodings'
			// Current thread 0x0000388c (most recent call first):
			return Error.Type.RENDERER_CRASHED_PYTHON_ERROR;
		}
		else if (line.contains("Calloc returns null")) {
			// Fra:1 Mem:976.60M (0.00M, Peak 1000.54M) | Time:00:01.34 | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Synchronizing object | Left
			// Calloc returns null: len=7186416 in CDMLoopUV, total 2145859048
			// Calloc returns null: len=7186416 in CDMLoopUV, total 2145859048
			// Malloc returns null: len=3190672 in CDMTexPoly, total 2149293176
			// Writing: /home/user/.sheepit/LEFT packed.crash.txt
			return Error.Type.RENDERER_OUT_OF_MEMORY;
		}
		else if (line.contains("Malloc returns null")) {
			// Fra:1 Mem:976.60M (0.00M, Peak 1000.54M) | Time:00:01.34 | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Synchronizing object | Left
			// Calloc returns null: len=7186416 in CDMLoopUV, total 2145859048
			// Calloc returns null: len=7186416 in CDMLoopUV, total 2145859048
			// Malloc returns null: len=3190672 in CDMTexPoly, total 2149293176
			// Writing: /home/user/.sheepit/LEFT packed.crash.txt
			return Error.Type.RENDERER_OUT_OF_MEMORY;
		}
		else if (line.contains("CUDA kernel compilation failed")) {
			// Fra:1 Mem:200.70M (0.00M, Peak 378.15M) | Time:00:01.02 | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Synchronizing object | Sun.001
			// Fra:1 Mem:200.70M (0.00M, Peak 378.15M) | Time:00:01.02 | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Synchronizing object | Sun.002
			// Fra:1 Mem:200.70M (0.00M, Peak 378.15M) | Time:00:01.02 | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Initializing
			// Fra:1 Mem:200.70M (0.00M, Peak 378.15M) | Time:00:01.02 | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Loading render kernels (may take a few minutes the first time)
			// nvcc fatal   : Value 'sm_61' is not defined for option 'gpu-architecture'
			// CUDA kernel compilation failed, see console for details.
			// Refer to the Cycles GPU rendering documentation for possible solutions:
			// http://www.blender.org/manual/render/cycles/gpu_rendering.html
			// Compiling CUDA kernel ...
			// "nvcc" -arch=sm_61 -m64 --cubin "/tmp/cache/c36db40aa5e59f5ea4ff139180353dbc/2.77/scripts/addons/cycles/kernel/kernels/cuda/kernel.cu" -o "/tmp/cache/cycles_kernel_sm61_079195D400571E023CC499D037AB6EE5.cubin" --ptxas-options="-v" --use_fast_math -I"/tmp/cache/c36db40aa5e59f5ea4ff139180353dbc/2.77/scripts/addons/cycles/kernel" -DNVCC -D__KERNEL_CUDA_VERSION__=75
			// Fra:1 Mem:200.70M (0.00M, Peak 378.15M) | Time:00:01.08 | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Error | CUDA kernel compilation failed, see console for details.
			// Error: CUDA kernel compilation failed, see console for details.
			// Fra:1 Mem:200.70M (0.00M, Peak 378.15M) | Time:00:01.08 | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Waiting for render to start
			// Fra:1 Mem:200.70M (0.00M, Peak 378.15M) | Time:00:01.08 | Mem:0.00M, Peak:0.00M | Scene, RenderLayer | Cancel | CUDA kernel compilation failed, see console for details.
			// Fra:1 Mem:147.74M (0.00M, Peak 378.15M) | Time:00:01.12 | Sce: Scene Ve:0 Fa:0 La:0
			// Saved: '/tmp/cache/8_0001.png'
			return Error.Type.GPU_NOT_SUPPORTED;
		}
		return Type.OK;
	}
	
	public static class renderStartedObservable extends Observable {
		
		@Getter private boolean isStarted;
		
		public renderStartedObservable(Observer observer) {
			super();
			addObserver(observer);
		}
		
		public void doNotifyIsStarted() {
			setChanged();
			notifyObservers();
			isStarted = true;
		}
	}
}
