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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.sheepit.client.hardware.cpu.CPU;
import com.sheepit.client.os.windows.Kernel32Lib;
import com.sheepit.client.os.windows.WinProcess;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase.MEMORYSTATUSEX;
import com.sun.jna.platform.win32.WinReg;

public class Windows extends OS {
	
	public String name() {
		return "windows";
	}
	
	@Override
	public String getRenderBinaryPath() {
		return "rend.exe";
	}
	
	@Override
	public CPU getCPU() {
		CPU ret = new CPU();
		try {
			String[] identifier = java.lang.System.getenv("PROCESSOR_IDENTIFIER").split(" ");
			for (int i = 0; i < (identifier.length - 1); i++) {
				if (identifier[i].equals("Family")) {
					ret.setFamily(identifier[i + 1]);
				}
				if (identifier[i].equals("Model")) {
					ret.setModel(identifier[i + 1]);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			final String cpuRegistryRoot = "HARDWARE\\DESCRIPTION\\System\\CentralProcessor";
			String[] processorIds = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryRoot);
			if (processorIds.length > 0) {
				String processorId = processorIds[0];
				String cpuRegistryPath = cpuRegistryRoot + "\\" + processorId;
				ret.setName(Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath, "ProcessorNameString").trim());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		// override the arch
		String env_arch = java.lang.System.getenv("PROCESSOR_ARCHITEW6432");
		if (env_arch == null || env_arch.compareTo("") == 0) {
			env_arch = java.lang.System.getenv("PROCESSOR_ARCHITECTURE");
		}
		if (env_arch.compareTo("AMD64") == 0) {
			ret.setArch("64bit");
		}
		else {
			ret.setArch("32bit");
		}
		
		return ret;
	}
	
	@Override
	public int getMemory() {
		try {
			MEMORYSTATUSEX _memory = new MEMORYSTATUSEX();
			if (Kernel32.INSTANCE.GlobalMemoryStatusEx(_memory)) {
				return (int) (_memory.ullTotalPhys.longValue() / 1024); // size in KB
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	@Override
	public String getCUDALib() {
		return "nvcuda";
	}
	
	@Override
	public Process exec(List<String> command, Map<String, String> env) throws IOException {
		// disable a popup because the renderer might crash (seg fault)
		Kernel32Lib kernel32lib = null;
		try {
			kernel32lib = (Kernel32Lib) Native.loadLibrary(Kernel32Lib.path, Kernel32Lib.class);
			kernel32lib.SetErrorMode(Kernel32Lib.SEM_NOGPFAULTERRORBOX);
		}
		catch (java.lang.UnsatisfiedLinkError e) {
			System.out.println("OS.Windows::exec failed to load kernel32lib " + e);
		}
		catch (java.lang.ExceptionInInitializerError e) {
			System.out.println("OS.Windows::exec failed to load kernel32lib " + e);
		}
		catch (Exception e) {
			System.out.println("OS.Windows::exec failed to load kernel32lib " + e);
		}
		
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectErrorStream(true);
		if (env != null) {
			builder.environment().putAll(env);
		}
		Process p = builder.start();
		WinProcess wproc = new WinProcess(p);
		if (env != null) {
			String priority = env.get("PRIORITY");
			wproc.setPriority(getPriorityClass(Integer.parseInt(priority)));
		}
		else {
			wproc.setPriority(WinProcess.PRIORITY_BELOW_NORMAL);
		}
		return p;
	}
	
	int getPriorityClass(int priority) {
		int process_class = WinProcess.PRIORITY_IDLE;
		switch (priority) {
			case 19:
			case 18:
			case 17:
			case 16:
			case 15:
				process_class = WinProcess.PRIORITY_IDLE;
				break;
			
			case 14:
			case 13:
			case 12:
			case 11:
			case 10:
			case 9:
			case 8:
			case 7:
			case 6:
			case 5:
				process_class = WinProcess.PRIORITY_BELOW_NORMAL;
				break;
			case 4:
			case 3:
			case 2:
			case 1:
			case 0:
			case -1:
			case -2:
			case -3:
				process_class = WinProcess.PRIORITY_NORMAL;
				break;
			case -4:
			case -5:
			case -6:
			case -7:
			case -8:
			case -9:
				process_class = WinProcess.PRIORITY_ABOVE_NORMAL;
				break;
			case -10:
			case -11:
			case -12:
			case -13:
			case -14:
				process_class = WinProcess.PRIORITY_HIGH;
				break;
			case -15:
			case -16:
			case -17:
			case -18:
			case -19:
				process_class = WinProcess.PRIORITY_REALTIME;
				break;
		}
		return process_class;
	}
	
	@Override
	public boolean kill(Process process) {
		if (process != null) {
			WinProcess wproc = new WinProcess(process);
			wproc.kill();
			return true;
		}
		return false;
	}
}
