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

package com.sheepit.client.standalone;

import com.sheepit.client.Client;
import com.sheepit.client.Gui;
import com.sheepit.client.Job;
import com.sheepit.client.Stats;
import com.sheepit.client.standalone.text.CLIInputActionHandler;
import com.sheepit.client.standalone.text.CLIInputObserver;

import sun.misc.Signal;
import sun.misc.SignalHandler;

public class GuiTextOneLine implements Gui {
	public static final String type = "oneLine";
	
	private String project;
	private int rendered;
	private int remaining;
	private String creditsEarned;
	private int sigIntCount = 0;
	
	private String computeMethod;
	private String status;
	private String line;
	
	private boolean exiting = false;
	
	private Client client;
	
	public GuiTextOneLine() {
		project = "";
		rendered = 0;
		remaining = 0;
		creditsEarned = null;
		status = "";
		computeMethod = "";
		line = "";
	}
	
	@Override
	public void start() {
		if (client != null) {
			
			CLIInputObserver cli_input_observer = new CLIInputObserver(client);
			cli_input_observer.addListener(new CLIInputActionHandler());
			Thread cli_input_observer_thread = new Thread(cli_input_observer);
			cli_input_observer_thread.start();
			
			Signal.handle(new Signal("INT"), new SignalHandler() {
				@Override
				public void handle(Signal signal) {
					sigIntCount++;
					
					if (sigIntCount == 5) {
						Signal.raise(new Signal("INT"));
						Runtime.getRuntime().halt(0);
					}
					else if (client.isRunning() && client.isSuspended() == false) {
						client.askForStop();
						exiting = true;
					}
					else {
						client.stop();
						GuiTextOneLine.this.stop();
					}
				}
			});
			
			client.run();
			client.stop();
		}
	}
	
	@Override
	public void stop() {
		Runtime.getRuntime().halt(0);
	}
	
	@Override
	public void status(String msg_) {
		status = msg_;
		updateLine();
	}
	
	@Override
	public void setRenderingProjectName(String name_) {
		if (name_ == null || name_.isEmpty()) {
			project = "";
		}
		else {
			project = "Project \"" + name_ + "\" |";
		}
		updateLine();
	}
	
	@Override
	public void error(String msg_) {
		status = "Error " + msg_;
		updateLine();
	}
	
	@Override
	public void AddFrameRendered() {
		rendered += 1;
		updateLine();
	}
	
	@Override
	public void displayStats(Stats stats) {
		remaining = stats.getRemainingFrame();
		creditsEarned = String.valueOf(stats.getCreditsEarnedDuringSession());
		updateLine();
	}
	
	@Override
	public void setRemainingTime(String time_) {
		status = "(remaining " + time_ + ")";
		updateLine();
	}
	
	@Override
	public void setRenderingTime(String time_) {
		status = "Rendering " + time_;
		updateLine();
	}
	
	@Override
	public void setClient(Client cli) {
		client = cli;
	}
	
	@Override
	public void setComputeMethod(String computeMethod_) {
		computeMethod = computeMethod_;
	}
	
	@Override
	public Client getClient() {
		return client;
	}
	
	private void updateLine() {
		int charToRemove = line.length();
		
		System.out.print("\r");
		line = String.format("Frames: %d Points: %s | %s %s %s", rendered, creditsEarned != null ? creditsEarned : "unknown", project, computeMethod, status + (exiting ? " (Exiting after this frame)" : ""));
		System.out.print(line);
		for (int i = line.length(); i <= charToRemove; i++) {
			System.out.print(" ");
		}
	}

	@Override
	public void setSuspended() {
	}

	@Override
	public void setResumed() {
	}
}
