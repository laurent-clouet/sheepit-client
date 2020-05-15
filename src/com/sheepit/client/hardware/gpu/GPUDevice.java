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

package com.sheepit.client.hardware.gpu;

import com.sheepit.client.hardware.gpu.nvidia.Nvidia;
import com.sheepit.client.hardware.gpu.opencl.OpenCL;

public class GPUDevice {
	private String type;
	private String model;
	private long memory; // in B
	private int renderBucketSize;
	
	private String id;
	
	private String oldId; // for backward compatibility
	
	public GPUDevice(String type, String model, long ram, String id) {
		this.type = type;
		this.model = model;
		this.memory = ram;
		this.id = id;
		this.renderBucketSize = 32;
	}
	
	public GPUDevice(String type, String model, long ram, String id, String oldId) {
		this(type, model, ram, id);
		this.oldId = oldId;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public String getModel() {
		return model;
	}
	
	public void setModel(String model) {
		this.model = model;
	}
	
	public long getMemory() {
		return memory;
	}
	
	public void setMemory(long memory) {
		this.memory = memory;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getOldId() {
		return oldId;
	}
	
	public void setOldId(String id) {
		this.oldId = id;
	}
	
	public int getRenderbucketSize() {
		return this.renderBucketSize;
	}
	
	public void setRenderbucketSize(int proposedRenderbucketSize) {
		int renderBucketSize = 32;
		GPULister gpu;
		
		if (type.equals("CUDA")) {
			gpu = new Nvidia();
		}
		else if (type.equals("OPENCL")) {
			gpu = new OpenCL();
		}
		else {
			// If execution takes this branch is because we weren't able to detect the proper GPU technology or
			// because is a new one (different from CUDA and OPENCL). In that case, move into the safest position
			// of 32x32 pixel tile sizes
			System.out.println("GPUDevice::setRenderbucketSize Unable to detect GPU technology. Render bucket size set to 32x32 pixels");
			this.renderBucketSize = 32;
			return;
		}
		
		if (proposedRenderbucketSize >= 32) {
			if (proposedRenderbucketSize <= gpu.getMaximumRenderBucketSize(getMemory())) {
				renderBucketSize = proposedRenderbucketSize;
			}
			else {
				renderBucketSize = gpu.getRecommendedRenderBucketSize(getMemory());
			}
		}
		
		this.renderBucketSize = renderBucketSize;
	}
	
	@Override
	public String toString() {
		return "GPUDevice [type=" + type + ", model='" + model + "', memory=" + memory + ", id=" + id + ", renderbucketSize=" + renderBucketSize + "]";
	}
}
