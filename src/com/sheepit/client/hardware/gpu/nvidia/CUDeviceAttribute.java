/*
 * Copyright (C) 2018 Laurent CLOUET
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

/**
 * CUDA Device properties. Taken directly from the online manual:
 * https://docs.nvidia.com/cuda/cuda-driver-api
 */
public class CUDeviceAttribute {
	/**
	 * PCI bus ID of the device
	 */
	public static final int CU_DEVICE_ATTRIBUTE_PCI_BUS_ID = 33;
	
	/**
	 * PCI device ID of the device
	 */
	public static final int CU_DEVICE_ATTRIBUTE_PCI_DEVICE_ID = 34;
	
	/**
	 * PCI domain ID of the device
	 */
	public static final int CU_DEVICE_ATTRIBUTE_PCI_DOMAIN_ID = 50;
}
