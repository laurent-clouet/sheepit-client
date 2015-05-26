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
import java.util.TimeZone;

import com.sheepit.client.Error.Type;
import com.sheepit.client.hardware.gpu.GPUDevice;
import com.sheepit.client.os.OS;

public class Job {
	public static final String UPDATE_METHOD_BY_REMAINING_TIME = "remainingtime";
	public static final String UPDATE_METHOD_BLENDER_INTERNAL_BY_PART = "blenderinternal";
	
	private String numFrame;
	private String sceneMD5;
	private String rendererMD5;
	private String id;
	private String revision;
	private String pictureFilename;
	private String path; // path inside of the archive
	private String rendererCommand;
	private String script;
	private boolean useGPU;
	private String extras;
	private String updateRenderingStatusMethod;
	private boolean synchronousUpload;
	private RenderProcess render;
	private boolean askForRendererKill;
	private Gui gui;
	private Configuration config;
	private Log log;
	
	public Job(Configuration config_, Gui gui_, Log log_, String id_, String frame_, String revision_, String path_, boolean use_gpu, String command_, String script_, String sceneMd5_, String rendererMd5_, String extras_, boolean synchronous_upload_, String update_method_) {
		config = config_;
		id = id_;
		numFrame = frame_;
		revision = revision_;
		path = path_;
		useGPU = use_gpu;
		rendererCommand = command_;
		sceneMD5 = sceneMd5_;
		rendererMD5 = rendererMd5_;
		extras = extras_;
		synchronousUpload = synchronous_upload_;
		gui = gui_;
		pictureFilename = null;
		script = script_;
		updateRenderingStatusMethod = update_method_;
		askForRendererKill = false;
		log = log_;
		render = new RenderProcess();
	}
	
	public RenderProcess getProcessRender() {
		return render;
	}
	
	public String toString() {
		return String.format("Job (numFrame '%s' sceneMD5 '%s' rendererMD5 '%s' ID '%s' revision '%s' pictureFilename '%s' jobPath '%s' gpu %s extras '%s' updateRenderingStatusMethod '%s' render %s)", numFrame, sceneMD5, rendererMD5, id, revision, pictureFilename, path, useGPU, extras, updateRenderingStatusMethod, render);
	}
	
	public String getId() {
		return id;
	}
	
	public String getFrameNumber() {
		return numFrame;
	}
	
	public String getExtras() {
		return extras;
	}
	
	public String getScript() {
		return script;
	}
	
	public String getSceneMD5() {
		return sceneMD5;
	}
	
	public String getRenderMd5() {
		return rendererMD5;
	}
	
	public String getPath() {
		return path;
	}
	
	public String getUpdateRenderingStatusMethod() {
		return updateRenderingStatusMethod;
	}
	
	public void setAskForRendererKill(boolean val) {
		askForRendererKill = val;
	}
	
	public boolean getAskForRendererKill() {
		return askForRendererKill;
	}
	
	public String getRenderCommand() {
		return rendererCommand;
	}
	
	public boolean getUseGPU() {
		return useGPU;
	}
	
	public String getRevision() {
		return revision;
	}
	
	public void setOutputImagePath(String path) {
		pictureFilename = path;
	}
	
	public String getOutputImagePath() {
		return pictureFilename;
	}
	
	public String getPrefixOutputImage() {
		return id + "_";
	}
	
	public String getRendererDirectory() {
		return config.workingDirectory.getAbsolutePath() + File.separator + rendererMD5;
	}
	
	public String getRendererPath() {
		return getRendererDirectory() + File.separator + OS.getOS().getRenderBinaryPath();
	}
	
	public String getRendererArchivePath() {
		return config.getStorageDir().getAbsolutePath() + File.separator + rendererMD5 + ".zip";
	}
	
	public String getSceneDirectory() {
		return config.workingDirectory.getAbsolutePath() + File.separator + sceneMD5;
	}
	
	public String getScenePath() {
		return getSceneDirectory() + File.separator + this.path;
	}
	
	public String getSceneArchivePath() {
		return config.workingDirectory.getAbsolutePath() + File.separator + sceneMD5 + ".zip";
	}
	
	public boolean simultaneousUploadIsAllowed() {
		return synchronousUpload;
	}
	
	public Error.Type render() {
		gui.status("Rendering");
		RenderProcess process = getProcessRender();
		String core_script = "import bpy\n" + "bpy.context.user_preferences.system.compute_device_type = \"%s\"\n" + "bpy.context.scene.cycles.device = \"%s\"\n" + "bpy.context.user_preferences.system.compute_device = \"%s\"\n";
		if (getUseGPU() && config.getGPUDevice() != null) {
			core_script = String.format(core_script, "CUDA", "GPU", config.getGPUDevice().getCudaName());
		}
		else {
			core_script = String.format(core_script, "NONE", "CPU", "CPU");
		}
		core_script += String.format("bpy.context.scene.render.tile_x = %1$d\nbpy.context.scene.render.tile_y = %1$d\n", getTileSize());
		File script_file = null;
		String command1[] = getRenderCommand().split(" ");
		int size_command = command1.length + 2; // + 2 for script
		
		if (config.getNbCores() > 0) { // user has specified something
			size_command += 2;
		}
		
		List<String> command = new ArrayList<String>(size_command);
		
		Map<String, String> new_env = new HashMap<String, String>();
		
		new_env.put("BLENDER_USER_CONFIG", config.workingDirectory.getAbsolutePath().replace("\\", "\\\\"));
		
		for (String arg : command1) {
			switch (arg) {
				case ".c":
					command.add(getScenePath());
					command.add("-P");
					
					try {
						script_file = File.createTempFile("script_", "", config.workingDirectory);
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
						log.error("Client:runRenderer exception on script generation, will return UNKNOWN " + e + " stacktrace " + sw.toString());
						return Error.Type.UNKNOWN;
					}
					script_file.deleteOnExit();
					break;
				case ".e":
					command.add(getRendererPath());
					// the number of cores has to be put after the binary and before the scene arg
					if (config.getNbCores() > 0) {
						command.add("-t");
						command.add(Integer.toString(config.getNbCores()));
					}
					break;
				case ".o":
					command.add(config.workingDirectory.getAbsolutePath() + File.separator + getPrefixOutputImage());
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
			String line;
			log.debug(command.toString());
			OS os = OS.getOS();
			process.setCoresUsed(config.getNbCores());
			process.start();
			getProcessRender().setProcess(os.exec(command, new_env));
			BufferedReader input = new BufferedReader(new InputStreamReader(getProcessRender().getProcess().getInputStream()));
			
			long last_update_status = 0;
			log.debug("renderer output");
			try {
				while ((line = input.readLine()) != null) {
					updateRenderingMemoryPeak(line);
					
					log.debug(line);
					if ((new Date().getTime() - last_update_status) > 2000) { // only call the update every two seconds
						updateRenderingStatus(line);
						last_update_status = new Date().getTime();
					}
					Type error = detectError(line);
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
				log.error("Client:runRenderer exception(B) (silent error) " + err1);
			}
			log.debug("end of rendering");
		}
		catch (Exception err) {
			if (script_file != null) {
				script_file.delete();
			}
			StringWriter sw = new StringWriter();
			err.printStackTrace(new PrintWriter(sw));
			log.error("Client:runRenderer exception(A) " + err + " stacktrace " + sw.toString());
			return Error.Type.FAILED_TO_EXECUTE;
		}
		
		int exit_value = process.exitValue();
		process.finish();
		
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
		
		File[] files = config.workingDirectory.listFiles(textFilter);
		
		if (files.length == 0) {
			log.error("Client::runRenderer no picture file found (after finished render (filename_without_extension " + filename_without_extension + ")");
			
			if (getAskForRendererKill()) {
				log.debug("Client::runRenderer renderer didn't generate any frame but died due to a kill request");
				return Error.Type.RENDERER_KILLED;
			}
			
			String basename = "";
			try {
				basename = getPath().substring(0, getPath().lastIndexOf('.'));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			File crash_file = new File(config.workingDirectory + File.separator + basename + ".crash.txt");
			if (crash_file.exists()) {
				log.error("Client::runRenderer crash file found => the renderer crashed");
				crash_file.delete();
				return Error.Type.RENDERER_CRASHED;
			}
			
			if (exit_value == 127 && process.getDuration() < 10) {
				log.error("Client::runRenderer renderer returned 127 and took " + process.getDuration() + "s, some libraries may be missing");
				return Error.Type.RENDERER_MISSING_LIBRARIES;
			}
			
			return Error.Type.NOOUTPUTFILE;
		}
		else {
			setOutputImagePath(files[0].getAbsolutePath());
			log.debug("Client::runRenderer pictureFilename: '" + getOutputImagePath() + "'");
		}
		
		File scene_dir = new File(getSceneDirectory());
		long date_modification_scene_directory = (long) Utils.lastModificationTime(scene_dir);
		if (date_modification_scene_directory > process.getStartTime()) {
			scene_dir.delete();
		}
		
		gui.status(String.format("Frame rendered in %dmin%ds", process.getDuration() / 60, process.getDuration() % 60));
		
		return Error.Type.OK;
	}
	
	private void updateRenderingStatus(String line) {
		if (getUpdateRenderingStatusMethod() != null && getUpdateRenderingStatusMethod().equals(Job.UPDATE_METHOD_BLENDER_INTERNAL_BY_PART)) {
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
							gui.status(String.format("Rendering %s %%", (int) (100.0 * current / total)));
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
						gui.status(String.format("Rendering (remaining %s)", Utils.humanDuration(date)));
						getProcessRender().setRemainingDuration((int) (date.getTime() / 1000));
					}
					catch (ParseException err) {
						log.error("Client::updateRenderingStatus ParseException " + err);
					}
				}
			}
		}
	}
	
	private void updateRenderingMemoryPeak(String line) {
		String[] elements = line.toLowerCase().split("(peak)");
		
		for (String element : elements) {
			if (element.isEmpty() == false && element.charAt(0) == ' ') {
				int end = element.indexOf(')');
				if (end > 0) {
					long mem = Utils.parseNumber(element.substring(1, end).trim());
					if (mem > getProcessRender().getMemoryUsed()) {
						getProcessRender().setMemoryUsed(mem);
					}
				}
			}
			else {
				if (element.isEmpty() == false && element.charAt(0) == ':') {
					int end = element.indexOf('|');
					if (end > 0) {
						long mem = Utils.parseNumber(element.substring(1, end).trim());
						if (mem > getProcessRender().getMemoryUsed()) {
							getProcessRender().setMemoryUsed(mem);
						}
					}
				}
			}
		}
	}
	
	private Type detectError(String line) {
		
		if (line.indexOf("CUDA error: Out of memory") != -1) {
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
	
	private int getTileSize() {
		int size = 32; // CPU
		GPUDevice gpu = this.config.getGPUDevice();
		if (getUseGPU() && this.config.getGPUDevice() != null) {
			// GPU
			// if the vram is lower than 1G reduce the size of tile to avoid black output
			size = (gpu.getMemory() > 1073741824L) ? 256 : 128;
		}
		return size;
	}
}
