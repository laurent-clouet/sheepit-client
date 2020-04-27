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

public class GPUDevice {
	private String type;
	private String model;
	private long memory; // in B
	private int renderbucketSize;
	
	private String id;
	
	private String oldId; // for backward compatibility
	
	public GPUDevice(String type, String model, long ram, String id) {
		this.type = type;
		this.model = model;
		this.memory = ram;
		this.id = id;
		this.renderbucketSize = getRecommandedTileSize(type);
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
		return this.renderbucketSize;
	}
	
	public void setRenderbucketSize(int proposedRenderbucketSize) {
		int renderbucketSize = getRecommandedTileSize(this.type);    // minimum recommended renderbucket size for GPUs
		
		if (proposedRenderbucketSize > 32) {
			if (getType().equals("CUDA")) {
				if (proposedRenderbucketSize <= 512) {
					renderbucketSize = proposedRenderbucketSize;
				}
				else {
					renderbucketSize = getRecommandedTileSize("CUDA");
				}
			}
			else if (getType().equals("OPENGL")) {
				if (proposedRenderbucketSize <= 2048) {
					renderbucketSize = proposedRenderbucketSize;
				}
				else {
					renderbucketSize = getRecommandedTileSize("OPENCL");
				}
			}
		}
		
		this.renderbucketSize = renderbucketSize;
	}
	
	private int getRecommandedTileSize(String type) {
		if (getType().equals("CUDA")) {
			// Optimal CUDA-based GPUs Renderbucket algorithm
			return (getMemory() > 1073741824L) ? 256 : 128;
		}
		else if (getType().equals("OPENGL")) {
			// Optimal OpenCL-based GPUs Renderbucket algorithm
			return (getMemory() > 1073741824L) ? 256 : 128;
		}
		else {
			// This branch should not be reached, but if it does, then set the size to 32x32 pixels (safest option)
			return 32;
		}
	}
	
	@Override
	public String toString() {
		return "GPUDevice [type=" + type + ", model='" + model + "', memory=" + memory + ", id=" + id + "]";
	}
}
