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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.sheepit.client.Log;
import com.sheepit.client.hardware.cpu.CPU;

public class Linux extends OS {
	private final String NICE_BINARY_PATH = "nice";
	private Boolean hasNiceBinary;
	
	public Linux() {
		super();
		this.hasNiceBinary = null;
	}
	
	public String name() {
		return "linux";
	}
	
	@Override
	public String getRenderBinaryPath() {
		return "rend.exe";
	}
	
	@Override
	public CPU getCPU() {
		CPU ret = new CPU();
		try {
			String filePath = "/proc/cpuinfo";
			Scanner scanner = new Scanner(new File(filePath));
			
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.startsWith("model name")) {
					String buf[] = line.split(":");
					if (buf.length > 1) {
						ret.setName(buf[1].trim());
					}
				}
				
				if (line.startsWith("cpu family")) {
					String buf[] = line.split(":");
					if (buf.length > 1) {
						ret.setFamily(buf[1].trim());
					}
				}
				
				if (line.startsWith("model") && line.startsWith("model name") == false) {
					String buf[] = line.split(":");
					if (buf.length > 1) {
						ret.setModel(buf[1].trim());
					}
				}
			}
			scanner.close();
		}
		catch (java.lang.NoClassDefFoundError e) {
			System.err.println("OS.Linux::getCPU error " + e + " mostly because Scanner class was introduced by Java 5 and you are running a lower version");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	@Override
	public int getMemory() {
		try {
			String filePath = "/proc/meminfo";
			Scanner scanner = new Scanner(new File(filePath));
			
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				
				if (line.startsWith("MemTotal")) {
					String buf[] = line.split(":");
					if (buf.length > 0) {
						Integer buf2 = new Integer(buf[1].trim().split(" ")[0]);
						return (((buf2 / 262144) + 1) * 262144); // 256*1024 = 262144
					}
				}
			}
			scanner.close();
		}
		catch (java.lang.NoClassDefFoundError e) {
			System.err.println("Machine::type error " + e + " mostly because Scanner class was introducted by Java 5 and you are running a lower version");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return 0;
	}
	
	@Override
	public String getCUDALib() {
		return "cuda";
	}
	
	@Override
	public Process exec(List<String> command, Map<String, String> env_overight) throws IOException {
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
		if (this.hasNiceBinary == null) {
			this.checkNiceAvailability();
		}
		if (this.hasNiceBinary.booleanValue()) {
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
	
	@Override
	public boolean getSupportHighPriority() {
		// only the root user can create process with high (negative nice) value
		String logname = System.getenv("LOGNAME");
		String user = System.getenv("USER");
		
		if ((logname != null && logname.equals("root"))	|| (user != null && user.equals("root"))) {
			return true;
		}
		
		return false;
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
