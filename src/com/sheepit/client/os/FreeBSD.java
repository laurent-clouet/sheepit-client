/*
 * Copyright (C) 2010-2015 Laurent CLOUET
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
package com.sheepit.client.os;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.sheepit.client.Log;
import com.sheepit.client.hardware.cpu.CPU;

public class FreeBSD extends OS {
	private final String NICE_BINARY_PATH = "nice";
	private final String ID_COMMAND_INVOCATION = "id -u";
	
	public FreeBSD() {
		super();
	}
	
	public String name() {
		return "freebsd";
	}
	
	@Override public String getRenderBinaryPath() {
		return "rend.exe";
	}
	
	@Override public CPU getCPU() {
		CPU ret = new CPU();
		try {
			Runtime r = Runtime.getRuntime();
			Process p = r.exec("dmesg");
			BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";
			
			while ((line = b.readLine()) != null) {
				if (line.startsWith("CPU:")) {
					String buf[] = line.split(":");
					if (buf.length > 1) {
						ret.setName(buf[1].trim());
					}
				}
				
				if (line.contains("Family=") && line.contains("Model=")) {
					String buf[] = line.split(" ");
					for (int i = 0; i < buf.length; i++) {
						if (buf[i].contains("Family")) {
							String family = buf[i].split("=")[1];
							ret.setFamily(family.split("x")[1]);
						}
						
						if (buf[i].contains("Model")) {
							String model = buf[i].split("=")[1];
							ret.setModel(model.split("x")[1]);
						}
					}
				}
			}
			b.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		if (ret.haveData() == false) {
			Log.getInstance(null).debug("OS::FreeBSD::getCPU failed to get CPU from dmesg, using partia sysctl");
			ret.setModel("0");
			ret.setFamily("0");
			try {
				Runtime run = Runtime.getRuntime();
				Process sysctl = run.exec("sysctl -n hw.model");
				BufferedReader buf = new BufferedReader(new InputStreamReader(sysctl.getInputStream()));
				String name = "";
				
				name = buf.readLine();
				buf.close();
				if (name == "") {
					ret.setName("0");
				}
				else {
					ret.setName(name);
				}
			}
			catch (IOException e) {
				Log.getInstance(null).debug("OS::FreeBSD::getCPU exception " + e);
			}
		}
		return ret;
	}
	
	@Override public long getMemory() {
		try {
			Runtime r = Runtime.getRuntime();
			Process p = r.exec("sysctl -n hw.usermem");
			BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";
			
			line = b.readLine();
			b.close();
			if (line.isEmpty()) {
				return 0;
			}
			Long mem_byte = Long.parseLong(line.trim());
			return mem_byte / Long.valueOf(1024);
		}
		catch (IOException e) {
			Log.getInstance(null).debug("OS::FreeBSD::getMemory exception " + e);
		}
		
		return 0;
	}
	
	@Override public long getFreeMemory() {
		return -1;
	}
	
	@Override public String getCUDALib() {
		return "cuda";
	}
	
	@Override public Process exec(List<String> command, Map<String, String> env_overight) throws IOException {
		// the renderer have a lib directory so add to the LD_LIBRARY_PATH
		// (even if we are not sure that it is the renderer who is launch
		
		Map<String, String> new_env = new HashMap<String, String>();
		new_env.putAll(java.lang.System.getenv()); // clone the env
		Boolean has_ld_library_path = new_env.containsKey("LD_LIBRARY_PATH");
		
		String lib_dir = (new File(command.get(0))).getParent() + File.separator + "lib";
		if (has_ld_library_path == false) {
			new_env.put("LD_LIBRARY_PATH", lib_dir);
		}
		else {
			new_env.put("LD_LIBRARY_PATH", new_env.get("LD_LIBRARY_PATH") + ":" + lib_dir);
		}
		
		List<String> actual_command = command;
		if (checkNiceAvailability()) {
			// launch the process in lowest priority
			if (env_overight != null) {
				actual_command.add(0, env_overight.get("PRIORITY"));
			}
			else {
				actual_command.add(0, "19");
			}
			actual_command.add(0, "-n");
			actual_command.add(0, NICE_BINARY_PATH);
		}
		else {
			Log.getInstance(null).error("No low priority binary, will not launch renderer in normal priority");
		}
		
		ProcessBuilder builder = new ProcessBuilder(actual_command);
		builder.redirectErrorStream(true);
		Map<String, String> env = builder.environment();
		env.putAll(new_env);
		if (env_overight != null) {
			env.putAll(env_overight);
		}
		return builder.start();
	}
	
	@Override public boolean getSupportHighPriority() {
		try {
			ProcessBuilder builder = new ProcessBuilder();
			builder.command("bash", "-c", ID_COMMAND_INVOCATION);
			builder.redirectErrorStream(true);
			
			Process process = builder.start();
			InputStream is = process.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			
			String userLevel = null;
			if ((userLevel = reader.readLine()) != null) {
				// Root user in *ix systems -independently of the alias used to login- has a id value of 0. On top of being a user with root capabilities,
				// to support changing the priority the nice tool must be accessible from the current user
				return (userLevel.equals("0")) & checkNiceAvailability();
			}
		}
		catch (IOException e) {
			System.err.println(String.format("ERROR FreeBSD::getSupportHighPriority Unable to execute id command. IOException %s", e.getMessage()));
		}
		
		return false;
	}
	
	@Override public boolean checkNiceAvailability() {
		ProcessBuilder builder = new ProcessBuilder();
		builder.command(NICE_BINARY_PATH);
		builder.redirectErrorStream(true);
		
		Process process = null;
		boolean hasNiceBinary = false;
		try {
			process = builder.start();
			hasNiceBinary = true;
		}
		catch (IOException e) {
			Log.getInstance(null).error("Failed to find low priority binary, will not launch renderer in normal priority (" + e + ")");
		}
		finally {
			if (process != null) {
				process.destroy();
			}
		}
		return hasNiceBinary;
	}
	
	@Override public void shutdownComputer(int delayInMinutes) {
		try {
			// Shutdown the computer waiting 1 minute to allow all SheepIt threads to close and exit the app
			ProcessBuilder builder = new ProcessBuilder("shutdown", "-h", String.valueOf(delayInMinutes));
			Process process = builder.inheritIO().start();
		}
		catch (IOException e) {
			System.err.println(String.format("FreeBSD::shutdownComputer Unable to execute the 'shutdown -h 1' command. Exception %s", e.getMessage()));
		}
	}
}
