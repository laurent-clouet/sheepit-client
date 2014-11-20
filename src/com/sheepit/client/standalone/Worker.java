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
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.URL;
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
import com.sheepit.client.ShutdownHook;
import com.sheepit.client.hardware.gpu.GPU;
import com.sheepit.client.hardware.gpu.GPUDevice;
import com.sheepit.client.network.ProxyAuthenticator;

public class Worker {
	@Option(name = "-server", usage = "Render-farm server, default https://www.sheepit-renderfarm.com", metaVar = "URL", required = false)
	private String server = "https://www.sheepit-renderfarm.com";
	
	@Option(name = "-login", usage = "User's login", metaVar = "LOGIN", required = true)
	private String login = "";
	
	@Option(name = "-password", usage = "User's password", metaVar = "PASSWORD", required = true)
	private String password = "";
	
	@Option(name = "-cache-dir", usage = "Cache/Working directory. Caution, everything in it not related to the render-farm will be removed", metaVar = "/tmp/cache", required = false)
	private String cache_dir = null;
	
	@Option(name = "-max-uploading-job", usage = "", metaVar = "1", required = false)
	private int max_upload = -1;
	
	@Option(name = "-gpu", usage = "CUDA name of the GPU used for the render, for example CUDA_0", metaVar = "CUDA_0", required = false)
	private String gpu_device = null;
	
	@Option(name = "-compute-method", usage = "CPU: only use cpu, GPU: only use gpu, CPU_GPU: can use cpu and gpu (not at the same time) if -gpu is not use it will not use the gpu", metaVar = "CPU_GPU", required = false)
	private String method = null;
	
	@Option(name = "-cores", usage = "Number of core/thread to use for the render", metaVar = "3", required = false)
	private int nb_cores = -1;
	
	@Option(name = "--verbose", usage = "Display log", required = false)
	private boolean print_log = false;
	
	@Option(name = "-request-time", usage = "H1:M1-H2:M2,H3:M3-H4:M4 Use the 24h format. For example to request job between 2am-8.30am and 5pm-11pm you should do --request-time 2:00-8:30,17:00-23:00 Caution, it's the requesting job time to get a project not the working time", metaVar = "2:00-8:30,17:00-23:00", required = false)
	private String request_time = null;
	
	@Option(name = "-proxy", usage = "URL of the proxy", metaVar = "http://login:password@host:port", required = false)
	private String proxy = null;
	
	@Option(name = "-extras", usage = "Extras data push on the authentication request", required = false)
	private String extras = null;
	
	@Option(name = "-ui", usage = "Specify the user interface to you use, default 'text', available 'oneline', 'text'", required = false)
	private String ui_type = "text";
	
	@Option(name = "--version", usage = "Display application version", required = false)
	private boolean display_version = false;
	
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
		
		if (display_version) {
			Configuration config = new Configuration(null, "", "");
			System.out.println("Version: " + config.getJarVersion());
			return;
		}
		
		ComputeType compute_method = ComputeType.CPU_GPU;
		Configuration config = new Configuration(null, login, password);
		config.setPrintLog(print_log);
		
		if (cache_dir != null) {
			File a_dir = new File(cache_dir);
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
			String cuda_str = "CUDA_";
			if (gpu_device.startsWith(cuda_str) == false) {
				System.err.println("CUDA_DEVICE should look like 'CUDA_X' where X is a number");
				return;
			}
			try {
				Integer.parseInt(gpu_device.substring(cuda_str.length()));
			}
			catch (NumberFormatException en) {
				System.err.println("CUDA_DEVICE should look like 'CUDA_X' where X is a number");
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
		
		if (nb_cores < -1) {
			System.err.println("Error: use-number-core should be a greater than zero");
			return;
		}
		else {
			config.setUseNbCores(nb_cores);
		}
		
		if (method != null) {
			if (method.equalsIgnoreCase("cpu")) {
				compute_method = ComputeType.CPU_ONLY;
			}
			else if (method.equalsIgnoreCase("gpu")) {
				compute_method = ComputeType.GPU_ONLY;
			}
			else if (method.equalsIgnoreCase("cpu_gpu") || method.equalsIgnoreCase("gpu_cpu")) {
				compute_method = ComputeType.CPU_GPU;
			}
			else {
				System.err.println("Error: compute-method unknown");
				System.exit(2);
			}
		}
		
		if (proxy != null) {
			try {
				URL url = new URL(proxy);
				String userinfo = url.getUserInfo();
				if (userinfo != null) {
					String[] elements = userinfo.split(":");
					if (elements.length == 2) {
						String proxy_user = elements[0];
						String proxy_password = elements[1];
						
						if (proxy_user != null && proxy_password != null) {
							Authenticator.setDefault(new ProxyAuthenticator(proxy_user, proxy_password));
						}
					}
				}
				
				System.setProperty("http.proxyHost", url.getHost());
				System.setProperty("http.proxyPort", Integer.toString(url.getPort()));
				
				System.setProperty("https.proxyHost", url.getHost());
				System.setProperty("https.proxyPort", Integer.toString(url.getPort()));
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
		
		if (compute_method == ComputeType.CPU_ONLY) { // the client was to render with cpu but on the server side project type are cpu+gpu or gpu prefered but never cpu only
			compute_method = ComputeType.CPU_GPU;
			config.setComputeMethod(compute_method);
			config.setUseGPU(null); // remove the GPU
		}
		else {
			config.setComputeMethod(compute_method); // doing it here because it have to be done after the setUseGPU
		}
		
		Log.getInstance(config).debug("client version " + config.getJarVersion());
		
		Gui gui;
		if (ui_type.equals("oneline")) {
			if (config.getPrintLog()) {
				System.out.println("OneLine ui can not be used if the verbose mode is enable");
				System.exit(2); 
			}
			gui = new GuiTextOneLine();
		}
		else {
			gui = new GuiText();
		}
		Client cli = new Client(gui, config, server);
		
		ShutdownHook hook = new ShutdownHook(cli);
		hook.attachShutDownHook();
		
		cli.run();
		cli.stop();
	}
}
