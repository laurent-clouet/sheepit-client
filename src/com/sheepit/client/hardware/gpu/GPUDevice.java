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
	
	private String id;
	
	private String oldId; // for backward compatibility
	
	public GPUDevice(String type, String model, long ram, String id) {
		this.type = type;
		this.model = model;
		this.memory = ram;
		this.id = id;
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
	
	public int getRecommandedTileSize() {
		// check the optimal tile size for the GPU. As a tactical solution, we start with 256x256px tile size for
		// GPUs with more than 1GB of VRAM, 128x128x otherwise
		return (getMemory() > 1073741824L) ? 256 : 128;
	}
	
	@Override
	public String toString() {
		return "GPUDevice [type=" + type + ", model='" + model + "', memory=" + memory + ", id=" + id + "]";
	}
}
