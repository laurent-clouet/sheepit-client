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

package com.sheepit.client.standalone;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import static org.kohsuke.args4j.ExampleMode.REQUIRED;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import com.sheepit.client.Client;
import com.sheepit.client.Configuration;
import com.sheepit.client.Configuration.ComputeType;
import com.sheepit.client.Gui;
import com.sheepit.client.Log;
import com.sheepit.client.Pair;
import com.sheepit.client.SettingsLoader;
import com.sheepit.client.ShutdownHook;
import com.sheepit.client.hardware.gpu.GPU;
import com.sheepit.client.hardware.gpu.GPUDevice;
import com.sheepit.client.hardware.gpu.nvidia.Nvidia;
import com.sheepit.client.hardware.gpu.opencl.OpenCL;
import com.sheepit.client.network.Proxy;

public class Worker {
	@Option(name = "-server", usage = "Render-farm server, default https://client.sheepit-renderfarm.com", metaVar = "URL", required = false)
	private String server = "https://client.sheepit-renderfarm.com";
	
	@Option(name = "-login", usage = "User's login", metaVar = "LOGIN", required = false)
	private String login = "";
	
	@Option(name = "-password", usage = "User's password", metaVar = "PASSWORD", required = false)
	private String password = "";
	
	@Option(name = "-cache-dir", usage = "Cache/Working directory. Caution, everything in it not related to the render-farm will be removed", metaVar = "/tmp/cache", required = false)
	private String cache_dir = null;
	
	@Option(name = "-max-uploading-job", usage = "", metaVar = "1", required = false)
	private int max_upload = -1;
	
	@Option(name = "-gpu", usage = "Name of the GPU used for the render, for example CUDA_0 for Nvidia or OPENCL_0 for AMD/Intel card", metaVar = "CUDA_0", required = false)
	private String gpu_device = null;
	
	@Option(name = "--no-gpu", usage = "Don't detect GPUs", required = false)
	private boolean no_gpu_detection = false;
	
	@Option(name = "-compute-method", usage = "CPU: only use cpu, GPU: only use gpu, CPU_GPU: can use cpu and gpu (not at the same time) if -gpu is not use it will not use the gpu", metaVar = "CPU", required = false)
	private String method = null;
	
	@Option(name = "-cores", usage = "Number of cores/threads to use for the render", metaVar = "3", required = false)
	private int nb_cores = -1;
	
	@Option(name = "-memory", usage = "Maximum memory allow to be used by renderer (in MB)", required = false)
	private int max_ram = -1;
	
	@Option(name = "-rendertime", usage = "Maximum time allow for each frame (in minute)", required = false)
	private int max_rendertime = -1;
	
	@Option(name = "--verbose", usage = "Display log", required = false)
	private boolean print_log = false;
	
	@Option(name = "-request-time", usage = "H1:M1-H2:M2,H3:M3-H4:M4 Use the 24h format. For example to request job between 2am-8.30am and 5pm-11pm you should do --request-time 2:00-8:30,17:00-23:00 Caution, it's the requesting job time to get a project not the working time", metaVar = "2:00-8:30,17:00-23:00", required = false)
	private String request_time = null;
	
	@Option(name = "-proxy", usage = "URL of the proxy", metaVar = "http://login:password@host:port", required = false)
	private String proxy = null;
	
	@Option(name = "-extras", usage = "Extras data push on the authentication request", required = false)
	private String extras = null;
	
	@Option(name = "-ui", usage = "Specify the user interface to use, default '" + GuiSwing.type + "', available '" + GuiTextOneLine.type + "', '" + GuiText.type + "', '" + GuiSwing.type + "' (graphical)", required = false)
	private String ui_type = null;
	
	@Option(name = "-config", usage = "Specify the configuration file", required = false)
	private String config_file = null;
	
	@Option(name = "--version", usage = "Display application version", required = false, handler = VersionParameterHandler.class)
	private VersionParameterHandler versionHandler;
	
	@Option(name = "--show-gpu", usage = "Print available CUDA devices and exit", required = false, handler = ListGpuParameterHandler.class)
	private ListGpuParameterHandler listGpuParameterHandler;
	
	@Option(name = "--no-systray", usage = "Don't use systray", required = false)
	private boolean no_systray = false;
	
	@Option(name = "-priority", usage = "Set render process priority (19 lowest to -19 highest)", required = false)
	private int priority = 19;

	@Option(name = "-title", usage = "Custom title for the GUI Client", required = false)
	private String title = "SheepIt Render Farm";
	
	public static void main(String[] args) {
		new Worker().doMain(args);
	}
	
	public void doMain(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		}
		catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("Usage: ");
			parser.printUsage(System.err);
			System.err.println();
			System.err.println("Example: java " + this.getClass().getName() + " " + parser.printExample(REQUIRED));
			return;
		}
		
		ComputeType compute_method = ComputeType.CPU;
		Configuration config = new Configuration(null, login, password);
		config.setPrintLog(print_log);
		config.setUsePriority(priority);
		config.setDetectGPUs(! no_gpu_detection);
		
		if (cache_dir != null) {
			File a_dir = new File(cache_dir);
			a_dir.mkdirs();
			if (a_dir.isDirectory() && a_dir.canWrite()) {
				config.setCacheDir(a_dir);
			}
		}
		
		if (max_upload != -1) {
			if (max_upload <= 0) {
				System.err.println("Error: max upload should be a greater than zero");
				return;
			}
			config.setMaxUploadingJob(max_upload);
		}
		
		if (gpu_device != null) {
			if (gpu_device.startsWith(Nvidia.TYPE) == false && gpu_device.startsWith(OpenCL.TYPE) == false) {
				System.err.println("CUDA_DEVICE should look like '" + Nvidia.TYPE + "_X' or '" + OpenCL.TYPE + "_X' where X is a number");
				return;
			}
			String family = "";
			try {
				if (gpu_device.startsWith(Nvidia.TYPE)) {
					family = Nvidia.TYPE;
				}
				else if (gpu_device.startsWith(OpenCL.TYPE)) {
					family = OpenCL.TYPE;
				}
				Integer.parseInt(gpu_device.substring(family.length() + 1)); // for the _
			}
			catch (NumberFormatException en) {
				System.err.println("Gpu device code should look like '" + family + "_X' where X is a number");
				return;
			}
			GPUDevice gpu = GPU.getGPUDevice(gpu_device);
			if (gpu == null) {
				System.err.println("GPU unknown");
				System.exit(2);
			}
			config.setUseGPU(gpu);
		}
		
		if (request_time != null) {
			String[] intervals = request_time.split(",");
			if (intervals != null) {
				config.requestTime = new LinkedList<Pair<Calendar, Calendar>>();
				
				SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
				for (String interval : intervals) {
					String[] times = interval.split("-");
					if (times != null && times.length == 2) {
						Calendar start = Calendar.getInstance();
						Calendar end = Calendar.getInstance();
						
						try {
							start.setTime(timeFormat.parse(times[0]));
							end.setTime(timeFormat.parse(times[1]));
						}
						catch (ParseException e) {
							System.err.println("Error: wrong format in request time");
							System.exit(2);
						}
						
						if (start.before(end)) {
							config.requestTime.add(new Pair<Calendar, Calendar>(start, end));
						}
						else {
							System.err.println("Error: wrong request time " + times[0] + " is after " + times[1]);
							System.exit(2);
						}
					}
				}
			}
		}
		
		if (nb_cores < -1 || nb_cores == 0) { // -1 is the default
			System.err.println("Error: use-number-core should be a greater than zero");
			return;
		}
		else {
			config.setUseNbCores(nb_cores);
		}
		
		if (max_ram > 0) {
			config.setMaxMemory(max_ram * 1000);
		}
		
		if (max_rendertime > 0) {
			config.setMaxRenderTime(max_rendertime * 60);
		}
		
		if (method != null) {
			try {
				compute_method = ComputeType.valueOf(method);
			}
			catch (IllegalArgumentException e) {
				System.err.println("Error: compute-method unknown");
				System.exit(2);
			}
		}
		else {
			if (config.getGPUDevice() == null) {
				compute_method = ComputeType.CPU;
			}
			else {
				compute_method = ComputeType.GPU;
			}
		}
		
		if (proxy != null) {
			try {
				Proxy.set(proxy);
			}
			catch (MalformedURLException e) {
				System.err.println("Error: wrong url for proxy");
				System.err.println(e);
				System.exit(2);
			}
		}
		
		if (extras != null) {
			config.setExtras(extras);
		}
		
		if (compute_method == ComputeType.CPU && config.getGPUDevice() != null) {
			System.err.println("You choose to only use the CPU but a GPU was also provided. You can not do both.");
			System.err.println("Aborting");
			System.exit(2);
		}
		else if (compute_method == ComputeType.CPU_GPU && config.getGPUDevice() == null) {
			System.err.println("You choose to only use the CPU and GPU but no GPU device was provided.");
			System.err.println("Aborting");
			System.exit(2);
		}
		else if (compute_method == ComputeType.GPU && config.getGPUDevice() == null) {
			System.err.println("You choose to only use the GPU but no GPU device was provided.");
			System.err.println("Aborting");
			System.exit(2);
		}
		else if (compute_method == ComputeType.CPU) {
			config.setUseGPU(null); // remove the GPU
		}
		
		config.setComputeMethod(compute_method);
		
		if (ui_type != null) {
			config.setUIType(ui_type);
		}
		
		if (config_file != null) {
			if (new File(config_file).exists() == false) {
				System.err.println("Configuration file not found.");
				System.err.println("Aborting");
				System.exit(2);
			}
			new SettingsLoader(config_file).merge(config);
		}
		
		Log.getInstance(config).debug("client version " + config.getJarVersion());
		
		Gui gui;
		String type = config.getUIType();
		if (type == null) {
			type = "swing";
		}
		switch (type) {
			case GuiTextOneLine.type:
				if (config.getPrintLog()) {
					System.out.println("OneLine UI can not be used if verbose mode is enabled");
					System.exit(2);
				}
				gui = new GuiTextOneLine();
				break;
			case GuiText.type:
				gui = new GuiText();
				break;
			default:
			case GuiSwing.type:
				if (java.awt.GraphicsEnvironment.isHeadless()) {
					System.out.println("Graphical ui can not be launch.");
					System.out.println("You should set a DISPLAY or use a text ui (with -ui " + GuiTextOneLine.type + " or -ui " + GuiText.type + ").");
					System.exit(3);
				}
				gui = new GuiSwing(no_systray == false, title);
				break;
		}
		Client cli = new Client(gui, config, server);
		gui.setClient(cli);
		
		ShutdownHook hook = new ShutdownHook(cli);
		hook.attachShutDownHook();
		
		gui.start();
	}
}
