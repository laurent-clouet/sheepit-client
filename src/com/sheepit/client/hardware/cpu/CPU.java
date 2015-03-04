/*
 * Copyright (C) 2010-2014 Laurent CLOUET
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

package com.sheepit.client.hardware.cpu;

public class CPU {
	private String name;
	private String model;
	private String family;
	private String arch; // 32 or 64 bits
	
	public CPU() {
		this.name = null;
		this.model = null;
		this.family = null;
		this.generateArch();
	}
	
	public String name() {
		return this.name;
	}
	
	public String model() {
		return this.model;
	}
	
	public String family() {
		return this.family;
	}
	
	public String arch() {
		return this.arch;
	}
	
	public int cores() {
		return Runtime.getRuntime().availableProcessors();
	}
	
	public void setName(String name_) {
		this.name = name_;
	}
	
	public void setModel(String model_) {
		this.model = model_;
	}
	
	public void setFamily(String family_) {
		this.family = family_;
	}
	
	public void setArch(String arch_) {
		this.arch = arch_;
	}
	
	public void generateArch() {
		String arch = System.getProperty("os.arch").toLowerCase();
		switch (arch) {
			case "i386":
			case "i686":
			case "x86":
				this.arch = "32bit";
				break;
			case "amd64":
			case "x86_64":
				this.arch = "64bit";
				break;
			default:
				this.arch = null;
				break;
		}
	}
	
	public boolean haveData() {
		return this.name != null && this.model != null && this.family != null && this.arch != null;
	}
	
}
