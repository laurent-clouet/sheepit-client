/*
 * Copyright (C) 2017 Laurent CLOUET
 * Author Rolf Aretz Lap <rolf.aretz@ottogroup.com>
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

package com.sheepit.client.standalone.text;

import com.sheepit.client.Client;
import com.sheepit.client.Configuration;
import com.sheepit.client.Job;

public class CLIInputActionHandler implements CLIInputListener {
	
	@Override public void commandEntered(Client client, String command) {
		int priorityLength = "priority".length();
		
		//prevent Null Pointer at next step
		if (command == null) {
			return;
		}
		if (client == null) {
			return;
		}
		if (command.equalsIgnoreCase("block")) {
			Job job = client.getRenderingJob();
			if (job != null) {
				job.block();
			}
		}
		else if (command.equalsIgnoreCase("resume")) {
			client.resume();
		}
		else if (command.equalsIgnoreCase("pause")) {
			client.suspend();
		}
		else if (command.equalsIgnoreCase("stop")) {
			client.askForStop();
		}
		else if (command.equalsIgnoreCase("status")) {
			displayStatus(client);
		}
		else if (command.equalsIgnoreCase("cancel")) {
			client.cancelStop();
		}
		else if (command.equalsIgnoreCase("quit")) {
			client.stop();
			System.exit(0);
		}
		else if ((command.length() > priorityLength) && (command.substring(0, priorityLength).equalsIgnoreCase("priority"))) {
			changePriority(client, command.substring(priorityLength));
		}
		else {
			System.out.println("Unknown command: " + command);
			System.out.println("status: display client status");
			System.out.println("priority <n>: set the priority for the next renderjob");
			System.out.println("block:  block project");
			System.out.println("pause:  pause client requesting new jobs");
			System.out.println("resume: resume after client was paused");
			System.out.println("stop:   exit after frame was finished");
			System.out.println("cancel: cancel exit");
			System.out.println("quit:   exit now");
		}
	}
	
	void changePriority(Client client, String newPriority) {
		Configuration config = client.getConfiguration();
		if (config != null) {
			try {
				config.setUsePriority(Integer.parseInt(newPriority.trim()));
			}
			catch (NumberFormatException e) {
				System.out.println("Invalid priority: " + newPriority);
			}
		}
		
	}
	
	void displayStatus(Client client) {
		if (client.isSuspended()) {
			System.out.println("Status: paused");
		}
		else if (client.isRunning()) {
			System.out.println("Status: running");
		}
		else {
			System.out.println("Status: will exit after the current frame");
		}
	}
}
