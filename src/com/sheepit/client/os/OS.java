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
package com.sheepit.client.os;

import java.io.IOException;

import com.sheepit.client.hardware.cpu.CPU;

public abstract class OS {
	public String name() {
		return "others";
	}
	
	public abstract CPU getCPU();
	
	public abstract int getMemory();
	
	public abstract String getRenderBinaryPath();
		
	public String getCUDALib() {
		return null;
	}
	
	public Process exec(String[] command) throws IOException {
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectErrorStream(true);
		return builder.start();
	}
	
	public boolean kill(Process proc) {
		if (proc != null) {
			proc.destroy();
			return true;
		}
		return false;
	}
	
	public static OS getOS() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.indexOf("win") >= 0) {
			return new Windows();
		}
		else if (os.indexOf("mac") >= 0) {
			return new Mac();
		}
		else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {
			return new Linux();
		}
		else {
			return null;
		}
	}
}
