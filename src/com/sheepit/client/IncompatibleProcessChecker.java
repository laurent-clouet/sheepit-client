/*
 * Copyright (C) 2017 Laurent CLOUET
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

import java.util.List;
import java.util.TimerTask;

import org.jutils.jprocesses.JProcesses;
import org.jutils.jprocesses.model.ProcessInfo;

import com.sheepit.client.os.OS;

public class IncompatibleProcessChecker extends TimerTask {
	
	private Client client;
	
	private boolean suspendedDueToOtherProcess;
	
	public IncompatibleProcessChecker(Client client_) {
		this.client = client_;
		this.suspendedDueToOtherProcess = false;
	}
	
	@Override
	public void run() {
		String search = this.client.getConfiguration().getIncompatibleProcessName().toLowerCase();
		if (search == null || search.isEmpty()) { // to nothing
			return;
		}
		
		if (isSearchProcessRunning(search)) {
			if (this.client != null && this.client.getRenderingJob() != null && this.client.getRenderingJob().getProcessRender().getProcess() != null) {
				this.client.getRenderingJob().setAskForRendererKill(true);
				this.client.getRenderingJob().setIncompatibleProcessKill(true);
				OS.getOS().kill(this.client.getRenderingJob().getProcessRender().getProcess());
			}
			this.client.suspend();
			this.client.getGui().status("Client paused due to 'incompatible process' feature");
			this.suspendedDueToOtherProcess = true;
		}
		else {
			if (this.client.isSuspended() && suspendedDueToOtherProcess) {
				// restart the client since the other process has been shutdown
				this.client.resume();
			}
		}
	}

	private boolean isSearchProcessRunning(String search) {
		JProcesses processes = JProcesses.get();
		processes.fastMode();
		List<ProcessInfo> processesList = processes.listProcesses();
		
		for (final ProcessInfo processInfo : processesList) {
			String name = processInfo.getName().toLowerCase();
			if (name == null || name.isEmpty()) {
				continue;
			}
			
			if (name.contains(search)) {
				this.client.getLog().debug("IncompatibleProcessChecker(" + search + ") found " + processInfo.getName());
				return true;
			}
		}
		
		return false;
	}
}
