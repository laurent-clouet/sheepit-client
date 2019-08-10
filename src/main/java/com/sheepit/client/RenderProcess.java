/*
 * Copyright (C) 2015 Laurent CLOUET
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

package com.sheepit.client;

import lombok.Data;

import java.util.Date;

@Data
public class RenderProcess {
	private long startTime;
	private long endTime;
	private int remainingDuration; // in seconds
	private long memoryUsed; // in kB
	private int coresUsed;
	private Process process;
	
	public RenderProcess() {
		process = null;
		startTime = -1;
		endTime = -1;
		memoryUsed = 0;
		coresUsed = 0;
		remainingDuration = 0;
	}
	
	/**
	 * 
	 * @return duration in seconds
	 */
	public int getDuration() {
		if (startTime != -1 && endTime != -1) {
			return (int) ((endTime - startTime) / 1000);
		}
		else if (startTime != -1) {
			return (int) ((new Date().getTime() - startTime) / 1000);
		}
		return 0;
	}
	
	public void finish() {
		endTime = new Date().getTime();
		process = null;
	}
	
	public void start() {
		startTime = new Date().getTime();
	}
	
	public int exitValue() {
		int value = 0;
		if (process == null) {
			return -1;
		}
		try {
			value = process.exitValue();
		}
		catch (IllegalThreadStateException e) {
			// the process is not finished yet
			value = 0;
		}
		catch (Exception e) {
			// actually is for java.io.IOException: GetExitCodeProcess error=6, The handle is invalid
			// it was not declared throwable
			
			// the process is not finished yet
			value = 0;
		}
		return value;
	}
}
