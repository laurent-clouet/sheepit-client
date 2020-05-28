/* This file was originally taken from JNA project (https://github.com/twall/jna)
 * filename: contrib/platform/src/com/sun/jna/platform/win32/Tlhelp32.java
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package com.sheepit.client.os.windows;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;

public interface Kernel32Lib extends Library {
	public static final String path = "kernel32";
	
	/**
	 * Includes all heaps of the process specified in th32ProcessID in the snapshot. To enumerate the heaps, see
	 * Heap32ListFirst.
	 */
	WinDef.DWORD TH32CS_SNAPHEAPLIST = new WinDef.DWORD(0x00000001);
	
	/**
	 * Includes all processes in the system in the snapshot. To enumerate the processes, see Process32First.
	 */
	WinDef.DWORD TH32CS_SNAPPROCESS = new WinDef.DWORD(0x00000002);
	
	/**
	 * Includes all threads in the system in the snapshot. To enumerate the threads, see Thread32First.
	 */
	WinDef.DWORD TH32CS_SNAPTHREAD = new WinDef.DWORD(0x00000004);
	
	/**
	 * Includes all modules of the process specified in th32ProcessID in the snapshot. To enumerate the modules, see
	 * Module32First. If the function fails with ERROR_BAD_LENGTH, retry the function until it succeeds.
	 */
	WinDef.DWORD TH32CS_SNAPMODULE = new WinDef.DWORD(0x00000008);
	
	/**
	 * Includes all 32-bit modules of the process specified in th32ProcessID in the snapshot when called from a 64-bit
	 * process. This flag can be combined with TH32CS_SNAPMODULE or TH32CS_SNAPALL. If the function fails with
	 * ERROR_BAD_LENGTH, retry the function until it succeeds.
	 */
	WinDef.DWORD TH32CS_SNAPMODULE32 = new WinDef.DWORD(0x00000010);
	
	/**
	 * Includes all processes and threads in the system, plus the heaps and modules of the process specified in th32ProcessID.
	 */
	WinDef.DWORD TH32CS_SNAPALL = new WinDef.DWORD(
			(TH32CS_SNAPHEAPLIST.intValue() | TH32CS_SNAPPROCESS.intValue() | TH32CS_SNAPTHREAD.intValue() | TH32CS_SNAPMODULE.intValue()));
	
	/**
	 * Indicates that the snapshot handle is to be inheritable.
	 */
	WinDef.DWORD TH32CS_INHERIT = new WinDef.DWORD(0x80000000);
	
	/**
	 * The system does not display the Windows Error Reporting dialog.
	 * See: http://msdn.microsoft.com/en-us/library/ms680621%28VS.85%29.aspx
	 */
	WinDef.DWORD SEM_NOGPFAULTERRORBOX = new WinDef.DWORD(0x0002);
	
	/**
	 * Describes an entry from a list of the processes residing in the system address space when a snapshot was taken.
	 */
	public static class PROCESSENTRY32 extends Structure {
		
		public static class ByReference extends PROCESSENTRY32 implements Structure.ByReference {
			public ByReference() {
			}
			
			public ByReference(Pointer memory) {
				super(memory);
			}
		}
		
		public PROCESSENTRY32() {
			dwSize = new WinDef.DWORD(size());
		}
		
		public PROCESSENTRY32(Pointer memory) {
			useMemory(memory);
			read();
		}
		
		/**
		 * The size of the structure, in bytes. Before calling the Process32First function, set this member to
		 * sizeof(PROCESSENTRY32). If you do not initialize dwSize, Process32First fails.
		 */
		public WinDef.DWORD dwSize;
		
		/**
		 * This member is no longer used and is always set to zero.
		 */
		public WinDef.DWORD cntUsage;
		
		/**
		 * The process identifier.
		 */
		public WinDef.DWORD th32ProcessID;
		
		/**
		 * This member is no longer used and is always set to zero.
		 */
		public BaseTSD.ULONG_PTR th32DefaultHeapID;
		
		/**
		 * This member is no longer used and is always set to zero.
		 */
		public WinDef.DWORD th32ModuleID;
		
		/**
		 * The number of execution threads started by the process.
		 */
		public WinDef.DWORD cntThreads;
		
		/**
		 * The identifier of the process that created this process (its parent process).
		 */
		public WinDef.DWORD th32ParentProcessID;
		
		/**
		 * The base priority of any threads created by this process.
		 */
		public WinDef.LONG pcPriClassBase;
		
		/**
		 * This member is no longer used, and is always set to zero.
		 */
		public WinDef.DWORD dwFlags;
		
		/**
		 * The name of the executable file for the process. To retrieve the full path to the executable file, call the
		 * Module32First function and check the szExePath member of the MODULEENTRY32 structure that is returned.
		 * However, if the calling process is a 32-bit process, you must call the QueryFullProcessImageName function to
		 * retrieve the full path of the executable file for a 64-bit process.
		 */
		public char[] szExeFile = new char[WinDef.MAX_PATH];
		
		@Override protected List<String> getFieldOrder() {
			return Arrays
					.asList("dwSize", "cntUsage", "th32ProcessID", "th32DefaultHeapID", "th32ModuleID", "cntThreads", "th32ParentProcessID", "pcPriClassBase",
							"dwFlags", "szExeFile");
		}
	}
	
	/**
	 * Takes a snapshot of the specified processes, as well as the heaps, modules, and threads used by these processes.
	 *
	 * @param dwFlags       The portions of the system to be included in the snapshot.
	 * @param th32ProcessID The process identifier of the process to be included in the snapshot. This parameter can be zero to indicate
	 *                      the current process. This parameter is used when the TH32CS_SNAPHEAPLIST, TH32CS_SNAPMODULE,
	 *                      TH32CS_SNAPMODULE32, or TH32CS_SNAPALL value is specified. Otherwise, it is ignored and all processes are
	 *                      included in the snapshot.
	 *                      <p/>
	 *                      If the specified process is the Idle process or one of the CSRSS processes, this function fails and the last
	 *                      error code is ERROR_ACCESS_DENIED because their access restrictions prevent user-level code from opening them.
	 *                      <p/>
	 *                      If the specified process is a 64-bit process and the caller is a 32-bit process, this function fails and the
	 *                      last error code is ERROR_PARTIAL_COPY (299).
	 * @return If the function succeeds, it returns an open handle to the specified snapshot.
	 * <p/>
	 * If the function fails, it returns INVALID_HANDLE_VALUE. To get extended error information, call GetLastError.
	 * Possible error codes include ERROR_BAD_LENGTH.
	 */
	public WinNT.HANDLE CreateToolhelp32Snapshot(WinDef.DWORD dwFlags, WinDef.DWORD th32ProcessID);
	
	/**
	 * Retrieves information about the first process encountered in a system snapshot.
	 *
	 * @param hSnapshot A handle to the snapshot returned from a previous call to the CreateToolhelp32Snapshot function.
	 * @param lppe      A pointer to a PROCESSENTRY32 structure. It contains process information such as the name of the
	 *                  executable file, the process identifier, and the process identifier of the parent process.
	 * @return Returns TRUE if the first entry of the process list has been copied to the buffer or FALSE otherwise. The
	 * ERROR_NO_MORE_FILES error value is returned by the GetLastError function if no processes exist or the snapshot
	 * does not contain process information.
	 */
	public boolean Process32First(WinNT.HANDLE hSnapshot, Kernel32Lib.PROCESSENTRY32.ByReference lppe);
	
	/**
	 * Retrieves information about the next process recorded in a system snapshot.
	 *
	 * @param hSnapshot A handle to the snapshot returned from a previous call to the CreateToolhelp32Snapshot function.
	 * @param lppe      A pointer to a PROCESSENTRY32 structure.
	 * @return Returns TRUE if the next entry of the process list has been copied to the buffer or FALSE otherwise. The
	 * ERROR_NO_MORE_FILES error value is returned by the GetLastError function if no processes exist or the snapshot
	 * does not contain process information.
	 */
	public boolean Process32Next(WinNT.HANDLE hSnapshot, Kernel32Lib.PROCESSENTRY32.ByReference lppe);
	
	public boolean SetPriorityClass(HANDLE hProcess, int dwPriorityClass);
	
	/**
	 * Controls whether the system will handle the specified types of serious errors or whether the process will handle them.
	 * See: http://msdn.microsoft.com/en-us/library/ms680621%28VS.85%29.aspx
	 *
	 * @param uMode The process error mode. This parameter can be one or more of the following values.
	 */
	public int SetErrorMode(DWORD uMode);
	
}
