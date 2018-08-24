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
	
	public GPUDevice(String type, String model, long ram, String id) {
		this.type = type;
		this.model = model;
		this.memory = ram;
		this.id = id;
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
	
	@Override
	public String toString() {
		return "GPUDevice [type=" + type + ", model='" + model + "', memory=" + memory + ", id=" + id + "]";
	}
	
	public int getRecommandedTileSize() {
		// GPU
		// if the vram is lower than 1G reduce the size of tile to avoid black output
		return (getMemory() > 1073741824L) ? 256 : 128;
	}
	
}
