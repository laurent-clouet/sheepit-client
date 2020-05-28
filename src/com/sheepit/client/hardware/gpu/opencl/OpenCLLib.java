/*
 * Copyright (C) 2013-2014 Laurent CLOUET
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

package com.sheepit.client.hardware.gpu.opencl;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

public interface OpenCLLib extends Library {
	// status
	public static final int CL_SUCCESS = 0;
	public static final int CL_DEVICE_NOT_FOUND = -1;
	
	public static final int CL_PLATFORM_VENDOR = 0x0903;
	public static final int CL_PLATFORM_NAME = 0x0902;
	
	// cl_device_type
	public static final int CL_DEVICE_TYPE_DEFAULT = (1 << 0);
	public static final int CL_DEVICE_TYPE_CPU = (1 << 1);
	public static final int CL_DEVICE_TYPE_GPU = (1 << 2);
	public static final int CL_DEVICE_TYPE_ACCELERATOR = (1 << 3);
	public static final int CL_DEVICE_TYPE_CUSTOM = (1 << 4);
	public static final int CL_DEVICE_TYPE_ALL = 0xFFFFFFFF;
	
	// cl_device_info
	public static final int CL_DEVICE_NAME = 0x102B;
	public static final int CL_DEVICE_VENDOR = 0x102C;
	public static final int CL_DEVICE_VERSION = 0x102D;
	public static final int CL_DEVICE_MAX_COMPUTE_UNITS = 0x1002;
	public static final int CL_DEVICE_GLOBAL_MEM_SIZE = 0x101F;
	public static final int CL_DEVICE_BOARD_NAME_AMD = 0x4038;
	public static final int CL_DEVICE_TOPOLOGY_AMD = 0x4037;
	
	public int clGetPlatformIDs(int num_entries, CLPlatformId.ByReference[] platforms, IntByReference num_platforms);
	
	public int clGetPlatformInfo(CLPlatformId.ByReference platform, int param_name, long param_value_size, byte[] destination, long size_ret[]);
	
	public int clGetDeviceIDs(CLPlatformId.ByReference platform, int param_name, int num_entries, CLDeviceId.ByReference[] devices,
			IntByReference device_count);
	
	public int clGetDeviceInfo(CLDeviceId.ByReference device, int param_name, long param_value_size, byte[] destination, long size_ret[]);
	
	public static class CLPlatformId extends Structure {
		public static class ByReference extends CLPlatformId implements Structure.ByReference {
		}
		
		public int id;
		
		@Override protected List<String> getFieldOrder() {
			return Arrays.asList(new String[] { "id" });
		}
	}
	
	public static class CLDeviceId extends Structure {
		public static class ByReference extends CLDeviceId implements Structure.ByReference {
		}
		
		public int id;
		
		@Override protected List<String> getFieldOrder() {
			return Arrays.asList(new String[] { "id" });
		}
	}
}
