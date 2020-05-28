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

package com.sheepit.client.hardware.gpu.nvidia;

import com.sun.jna.Library;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

public interface CUDA extends Library {
	public int cuInit(int flags);
	
	/*
	 * @return: CUDA_SUCCESS, CUDA_ERROR_DEINITIALIZED, CUDA_ERROR_NOT_INITIALIZED, CUDA_ERROR_INVALID_CONTEXT, CUDA_ERROR_INVALID_VALUE
	 */
	public int cuDeviceGetCount(IntByReference count);
	
	public int cuDeviceGetName(byte[] name, int len, int dev);
	
	public int cuDeviceGet(IntByReference device, int ordinal);
	
	public int cuDeviceGetAttribute(IntByReference pi, int attrib, int dev);
	
	public int cuDeviceTotalMem_v2(LongByReference bytes, int dev);
	
	public int cuDeviceTotalMem(LongByReference bytes, int dev);
	
}
