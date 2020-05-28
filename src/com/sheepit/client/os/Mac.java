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
package com.sheepit.client.os;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import com.sheepit.client.Log;
import com.sheepit.client.hardware.cpu.CPU;

public class Mac extends OS {
	private final String NICE_BINARY_PATH = "nice";
	private Boolean hasNiceBinary;
	
	public Mac() {
		super();
		this.hasNiceBinary = null;
	}
	
	public String name() {
		return "mac";
	}
	
	@Override public String getRenderBinaryPath() {
		return "Blender" + File.separator + "blender.app" + File.separator + "Contents" + File.separator + "MacOS" + File.separator + "blender";
	}
	
	@Override public CPU getCPU() {
		CPU ret = new CPU();
		
		String command = "sysctl machdep.cpu.family machdep.cpu.brand_string";
		
		Process p = null;
		BufferedReader input = null;
		try {
			String line;
			p = Runtime.getRuntime().exec(command);
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			while ((line = input.readLine()) != null) {
				String option_cpu_family = "machdep.cpu.family:";
				String option_model_name = "machdep.cpu.brand_string:";
				if (line.startsWith(option_model_name)) {
					ret.setName(line.substring(option_model_name.length()).trim());
				}
				if (line.startsWith(option_cpu_family)) {
					ret.setFamily(line.substring(option_cpu_family.length()).trim());
				}
			}
			input.close();
			input = null;
		}
		catch (Exception err) {
			System.out.println("exception " + err);
			err.printStackTrace();
			ret.setName("Unknown Mac name");
			ret.setFamily("Unknown Mac family");
		}
		finally {
			if (input != null) {
				try {
					input.close();
				}
				catch (IOException e) {
				}
			}
			
			if (p != null) {
				p.destroy();
			}
		}
		
		ret.setModel("Unknown");
		
		return ret;
	}
	
	@Override public long getMemory() {
		String command = "sysctl hw.memsize";
		
		Process p = null;
		BufferedReader input = null;
		try {
			String line;
			p = Runtime.getRuntime().exec(command);
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			while ((line = input.readLine()) != null) {
				String option = "hw.memsize:";
				if (line.startsWith(option)) {
					String memory = line.substring(option.length()).trim(); // memory in bytes
					
					return Long.parseLong(memory) / 1024;
				}
			}
			input.close();
			input = null;
		}
		catch (Exception err) {
			System.out.println("exception " + err);
			err.printStackTrace();
		}
		finally {
			if (input != null) {
				try {
					input.close();
				}
				catch (IOException e) {
				}
			}
			
			if (p != null) {
				p.destroy();
			}
		}
		
		return -1;
	}
	
	@Override public long getFreeMemory() {
		return -1;
	}
	
	@Override public Process exec(List<String> command, Map<String, String> env) throws IOException {
		List<String> actual_command = command;
		if (this.hasNiceBinary == null) {
			this.checkNiceAvailability();
		}
		if (this.hasNiceBinary.booleanValue()) {
			// launch the process in lowest priority
			if (env != null) {
				actual_command.add(0, env.get("PRIORITY"));
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
		if (env != null) {
			builder.environment().putAll(env);
		}
		return builder.start();
	}
	
	@Override public String getCUDALib() {
		return "/usr/local/cuda/lib/libcuda.dylib";
	}
	
	protected void checkNiceAvailability() {
		ProcessBuilder builder = new ProcessBuilder();
		builder.command(NICE_BINARY_PATH);
		builder.redirectErrorStream(true);
		Process process = null;
		try {
			process = builder.start();
			this.hasNiceBinary = true;
		}
		catch (IOException e) {
			this.hasNiceBinary = false;
			Log.getInstance(null).error("Failed to find low priority binary, will not launch renderer in normal priority (" + e + ")");
		}
		finally {
			if (process != null) {
				process.destroy();
			}
		}
	}
}
