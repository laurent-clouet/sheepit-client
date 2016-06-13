/*
 * Copyright (C) 2013 Laurent CLOUET
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

package com.sheepit.client.os.windows;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD.DWORD_PTR;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT;

public class WinProcess {
	public static final int PRIORITY_IDLE = 0x40;
	public static final int PRIORITY_BELOW_NORMAL = 0x4000;
	public static final int PRIORITY_NORMAL = 0x20;
	public static final int PRIORITY_ABOVE_NORMAL = 0x8000;
	public static final int PRIORITY_HIGH = 0x80;
	public static final int PRIORITY_REALTIME = 0x100;
	
	private WinNT.HANDLE handle;
	private int pid;
	Kernel32Lib kernel32lib;
	
	public WinProcess() {
		this.handle = null;
		this.pid = -1;
		this.kernel32lib = null;
		try {
			this.kernel32lib = (Kernel32Lib) Native.loadLibrary(Kernel32Lib.path, Kernel32Lib.class);
		}
		catch (UnsatisfiedLinkError | Exception | ExceptionInInitializerError e) {
			System.out.println("WinProcess::construct " + e);
		}
	}
	
	public WinProcess(Process process) {
		this();
		try {
			Field f = process.getClass().getDeclaredField("handle");
			f.setAccessible(true);
			long val = f.getLong(process);
			this.handle = new WinNT.HANDLE();
			this.handle.setPointer(Pointer.createConstant(val));
			this.pid = Kernel32.INSTANCE.GetProcessId(this.handle);
		}
		catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
		}
	}
	
	public WinProcess(int pid_) throws IOException {
		this();
		this.handle = Kernel32.INSTANCE.OpenProcess(0x0400 | // PROCESS_QUERY_INFORMATION
													0x0800 | // PROCESS_SUSPEND_RESUME
													0x0001 | // PROCESS_TERMINATE
													0x0200 | // PROCESS_SET_INFORMATION
													0x00100000, // SYNCHRONIZE
													false, pid_);
		if (this.handle == null) {
			throw new IOException("OpenProcess failed: " + Kernel32Util.formatMessageFromLastErrorCode(Kernel32.INSTANCE.GetLastError()) + " (pid: " + pid_ + ")");
		}
		this.pid = pid_;
	}
	
	@Override
	protected void finalize() throws Throwable {
		if (this.handle != null) {
			// Kernel32.INSTANCE.CloseHandle(this.handle); // do not close the handle because the parent Process object might still be alive
			this.handle = null;
		}
		this.pid = -1;
	}
	
	public boolean kill() {
		try {
			List<WinProcess> children = this.getChildren();
			this.terminate();
			for (WinProcess child : children) {
				child.kill();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean setPriority(int priority) {
		return this.kernel32lib.SetPriorityClass(this.handle, priority);
	}
	
	public boolean setAffinity(int numberCores) {
		// affects the process to a specific core/cpu, it will reduce the number of switch between core
		// that way the machine will look "less busy" since default behavior for windows is to put 4 cores 
		// at 25% instead of 1 core at 100% but from the user point of view, it introduce lag.
		if (numberCores > 0) {
			long coreAffinity = 0;
			for (int i = 0; i < numberCores; i++) {
				coreAffinity |= 1L << i;
			}
			return this.kernel32lib.SetProcessAffinityMask(this.handle, new DWORD_PTR(coreAffinity));
		}
		return false;
	}
	
	private void terminate() {
		Kernel32.INSTANCE.TerminateProcess(this.handle, 0);
		Kernel32.INSTANCE.CloseHandle(this.handle); // we are sure that the parent Process object is dead
	}
	
	private List<WinProcess> getChildren() throws IOException {
		ArrayList<WinProcess> result = new ArrayList<WinProcess>();
		
		WinNT.HANDLE hSnap = this.kernel32lib.CreateToolhelp32Snapshot(Kernel32Lib.TH32CS_SNAPPROCESS, new DWORD(0));
		Kernel32Lib.PROCESSENTRY32.ByReference ent = new Kernel32Lib.PROCESSENTRY32.ByReference();
		if (!this.kernel32lib.Process32First(hSnap, ent)) {
			return result;
		}
		do {
			if (ent.th32ParentProcessID.intValue() == this.pid) {
				try {
					result.add(new WinProcess(ent.th32ProcessID.intValue()));
				}
				catch (IOException e) {
					System.err.println("WinProcess::getChildren, IOException " + e);
				}
			}
		}
		while (this.kernel32lib.Process32Next(hSnap, ent));
		
		Kernel32.INSTANCE.CloseHandle(hSnap);
		
		return result;
	}
	
	public String toString() {
		return "WinProcess(pid: " + this.pid + ", handle " + this.handle + ")";
	}
}
