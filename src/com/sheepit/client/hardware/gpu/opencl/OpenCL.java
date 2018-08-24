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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.sheepit.client.hardware.gpu.GPUDevice;
import com.sheepit.client.hardware.gpu.GPULister;
import com.sheepit.client.hardware.gpu.opencl.OpenCLLib.CLDeviceId;
import com.sheepit.client.hardware.gpu.opencl.OpenCLLib.CLPlatformId;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;

public class OpenCL implements GPULister {
	public static String TYPE = "OPENCL";
	
	@Override
	public List<GPUDevice> getGpus() {
		OpenCLLib lib = null;
		
		String path = "OpenCL";
		try {
			lib = (OpenCLLib) Native.loadLibrary(path, OpenCLLib.class);
		}
		catch (java.lang.UnsatisfiedLinkError e) {
			System.out.println("OpenCL::getGpus failed(A) to load OpenCL lib (path: " + path + ")");
			return null;
		}
		catch (java.lang.ExceptionInInitializerError e) {
			System.out.println("OpenCL::getGpus failed(B) ExceptionInInitializerError " + e);
			return null;
		}
		catch (Exception e) {
			System.out.println("OpenCL::getGpus failed(C) generic exception " + e);
			return null;
		}
		
		int status = -1;
		
		// get the number of platform
		IntByReference number_platforms = new IntByReference();
		
		status = lib.clGetPlatformIDs(0, null, number_platforms);
		if (status != OpenCLLib.CL_SUCCESS) {
			System.out.println("OpenCL::getGpus failed(D) status: " + status);
			return null;
		}
		
		// now we can create the platforms
		
		final OpenCLLib.CLPlatformId.ByReference e6ref = new OpenCLLib.CLPlatformId.ByReference();
		OpenCLLib.CLPlatformId.ByReference[] plateforms = (OpenCLLib.CLPlatformId.ByReference[]) e6ref.toArray(number_platforms.getValue());
		
		status = lib.clGetPlatformIDs(number_platforms.getValue(), plateforms, null);
		if (status != OpenCLLib.CL_SUCCESS) {
			System.out.println("OpenCL::getGpus failed(E) status: " + status);
			return null;
		}
		
		List<GPUDevice> available_devices = new ArrayList<GPUDevice>(1);
		// Devices are numbered consecutively across platforms.
		int id = 0;
		for (int i = 0; i < number_platforms.getValue(); i++) {
			// get number of devices in platform
			IntByReference device_count = new IntByReference();
			
			status = lib.clGetDeviceIDs(plateforms[i], OpenCLLib.CL_DEVICE_TYPE_GPU, 0, null, device_count);
			if (status == OpenCLLib.CL_DEVICE_NOT_FOUND) {
				System.out.println("OpenCL::getGpus no device found on plateforms[" + i + "]");
				continue;
			}
			if (status != OpenCLLib.CL_SUCCESS) {
				System.out.println("OpenCL::getGpus failed(F) status: " + status);
				return null;
			}
			
			final OpenCLLib.CLDeviceId.ByReference e6ref4 = new OpenCLLib.CLDeviceId.ByReference();
			
			OpenCLLib.CLDeviceId.ByReference[] devices = (OpenCLLib.CLDeviceId.ByReference[]) e6ref4.toArray(device_count.getValue());
			
			status = lib.clGetDeviceIDs(plateforms[i], OpenCLLib.CL_DEVICE_TYPE_GPU, device_count.getValue(), devices, null);
			if (status != OpenCLLib.CL_SUCCESS) {
				System.out.println("OpenCL::getGpus failed(G) status: " + status);
				return null;
			}
			
			for (int j = 0; j < device_count.getValue(); j++) {
				String platform_vendor = getInfoPlatform(lib, plateforms[i], OpenCLLib.CL_PLATFORM_VENDOR);
				if (platform_vendor != null && platform_vendor.toLowerCase().equals("advanced micro devices, inc.")) { // opencl is only used for amd gpus
					String name = getInfodeviceString(lib, devices[j], OpenCLLib.CL_DEVICE_BOARD_NAME_AMD);
					long vram = getInfodeviceLong(lib, devices[j], OpenCLLib.CL_DEVICE_GLOBAL_MEM_SIZE);
					if (name != null && vram > 0) {
						available_devices.add(new GPUDevice(TYPE, name, vram, TYPE + "_" + id));
					}
				}
				id++;
			}
		}
		
		return available_devices;
	}
	
	private static String getInfodeviceString(OpenCLLib lib, CLDeviceId.ByReference device, int type) {
		byte name[] = new byte[256];
		
		int status = lib.clGetDeviceInfo(device, type, 256, name, null);
		if (status != OpenCLLib.CL_SUCCESS) {
			System.out.println("OpenCL::getInfodeviceString failed(H) status: " + status + " type: " + type);
			return null;
		}
		
		return new String(name).trim();
	}
	
	private static long getInfodeviceLong(OpenCLLib lib, CLDeviceId.ByReference device, int type) {
		byte name[] = new byte[256];
		
		int status = lib.clGetDeviceInfo(device, type, 256, name, null);
		if (status != OpenCLLib.CL_SUCCESS) {
			System.out.println("OpenCL::getInfodeviceLong failed(I) status: " + status + " type: " + type);
			return -1;
		}
		
		ByteBuffer wrapped = ByteBuffer.wrap(name);
		wrapped.order(ByteOrder.LITTLE_ENDIAN);
		
		return wrapped.getLong();
	}
	
	private static String getInfoPlatform(OpenCLLib lib, CLPlatformId.ByReference platform, int type) {
		byte name[] = new byte[256];
		
		int status = lib.clGetPlatformInfo(platform, type, 256, name, null);
		if (status != OpenCLLib.CL_SUCCESS) {
			System.out.println("GPU::getInfoPlatform failed(J) status: " + status + " type: " + type);
			return null;
		}
		
		return new String(name).trim();
	}
}
