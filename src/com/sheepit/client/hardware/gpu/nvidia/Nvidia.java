package com.sheepit.client.hardware.gpu.nvidia;

import java.util.LinkedList;
import java.util.List;

import com.sheepit.client.hardware.gpu.GPUDevice;
import com.sheepit.client.os.OS;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

public class Nvidia {
	
	public static List<GPUDevice> getGpus() {
		OS os = OS.getOS();
		String path = os.getCUDALib();
		if (path == null) {
			System.out.println("Nvidia::getGpus no CUDA lib path found");
			return null;
		}
		CUDA cudalib = null;
		try {
			cudalib = (CUDA) Native.loadLibrary(path, CUDA.class);
		}
		catch (java.lang.UnsatisfiedLinkError e) {
			System.out.println("Nvidia::getGpus failed to load CUDA lib (path: " + path + ")");
			return null;
		}
		catch (java.lang.ExceptionInInitializerError e) {
			System.out.println("Nvidia::getGpus ExceptionInInitializerError " + e);
			return null;
		}
		catch (Exception e) {
			System.out.println("Nvidia::getGpus generic exception " + e);
			return null;
		}
		
		int result = CUresult.CUDA_ERROR_UNKNOWN;
		
		result = cudalib.cuInit(0);
		if (result != CUresult.CUDA_SUCCESS) {
			System.out.println("Nvidia::getGpus cuInit failed (ret: " + result + ")");
			if (result == CUresult.CUDA_ERROR_UNKNOWN) {
				System.out.println("If you are running Linux, this error is usually due to nvidia kernel module 'nvidia_uvm' not loaded.");
				System.out.println("Relaunch the application as root or load the module.");
				System.out.println("Most of time it does fix the issue.");
			}
			return null;
		}
		
		if (result == CUresult.CUDA_ERROR_NO_DEVICE) {
			return null;
		}
		
		IntByReference count = new IntByReference();
		result = cudalib.cuDeviceGetCount(count);
		
		if (result != CUresult.CUDA_SUCCESS) {
			System.out.println("Nvidia::getGpus cuDeviceGetCount failed (ret: " + CUresult.stringFor(result) + ")");
			return null;
		}
		
		List<GPUDevice> devices = new LinkedList<GPUDevice>();
		
		for (int num = 0; num < count.getValue(); num++) {
			byte name[] = new byte[256];
			
			result = cudalib.cuDeviceGetName(name, 256, num);
			if (result != CUresult.CUDA_SUCCESS) {
				System.out.println("Nvidia::getGpus cuDeviceGetName failed (ret: " + CUresult.stringFor(result) + ")");
				continue;
			}
			
			LongByReference ram = new LongByReference();
			result = cudalib.cuDeviceTotalMem(ram, num);
			
			if (result != CUresult.CUDA_SUCCESS) {
				System.out.println("Nvidia::getGpus cuDeviceTotalMem failed (ret: " + CUresult.stringFor(result) + ")");
				return null;
			}
			
			devices.add(new GPUDevice("CUDA", new String(name).trim(), ram.getValue(), "CUDA_" + Integer.toString(num)));
		}
		return devices;
	}
	
}
