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
import java.util.List;
import java.util.Map;

import com.sheepit.client.hardware.cpu.CPU;

public abstract class OS {
	private static OS instance = null;
	
	public abstract String name();
	
	public abstract CPU getCPU();
	
	public abstract long getMemory();
	
	public abstract long getFreeMemory();
	
	public abstract String getRenderBinaryPath();
	
	public String getCUDALib() {
		return null;
	}
	
	public boolean getSupportHighPriority() {
		return true;
	}
	
	public Process exec(List<String> command, Map<String, String> env) throws IOException {
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectErrorStream(true);
		if (env != null) {
			builder.environment().putAll(env);
		}
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
		if (instance == null) {
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("win")) {
				instance = new Windows();
			}
			else if (os.contains("mac")) {
				instance = new Mac();
			}
			else if (os.contains("nix") || os.contains("nux")) {
				instance = new Linux();
			}
			else if (os.contains("freebsd")) {
				instance = new FreeBSD();
			}
		}
		
		return instance;
	}
}
