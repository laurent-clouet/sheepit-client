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
import com.sheepit.client.Stats;
import com.sheepit.client.standalone.text.CLIInputActionHandler;
import com.sheepit.client.standalone.text.CLIInputObserver;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

public class GuiTextOneLine implements Gui {
	public static final String type = "oneLine";
	
	private String project;
	private int rendered;
	private int remaining;
	private String creditsEarned;
	private int sigIntCount = 0;
	private DateFormat df;
	
	private String computeMethod;
	private String status;
	private String line;
	private String eta;
	
	private int uploadQueueSize;
	private long uploadQueueVolume;
	
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
		uploadQueueSize = 0;
		uploadQueueVolume = 0;
		df = new SimpleDateFormat("MMM dd HH:mm:ss");
		eta = "";
	}
	
	@Override public void start() {
		if (client != null) {
			
			CLIInputObserver cli_input_observer = new CLIInputObserver(client);
			cli_input_observer.addListener(new CLIInputActionHandler());
			Thread cli_input_observer_thread = new Thread(cli_input_observer);
			cli_input_observer_thread.start();
			
			Signal.handle(new Signal("INT"), new SignalHandler() {
				@Override public void handle(Signal signal) {
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
	
	@Override public void stop() {
		Runtime.getRuntime().halt(0);
	}
	
	@Override public void updateTrayIcon(Integer percentage) {
	}
	
	@Override public void status(String msg_) {
		status(msg_, false);
	}
	
	@Override public void status(String msg_, boolean overwriteSuspendedMsg) {
		if (client != null && client.isSuspended()) {
			if (overwriteSuspendedMsg) {
				status = msg_;
				updateLine();
			}
		}
		else {
			status = msg_;
			updateLine();
		}
	}
	
	@Override public void status(String msg, int progress) {
		this.status(msg, progress, 0);
	}
	
	@Override public void status(String msg, int progress, long size) {
		status = showProgress(msg, progress, size);
		updateLine();
	}
	
	@Override public void setRenderingProjectName(String name_) {
		if (name_ == null || name_.isEmpty()) {
			project = "";
		}
		else {
			project = name_ + " |";
		}
		updateLine();
	}
	
	@Override public void error(String msg_) {
		status = "Error " + msg_;
		updateLine();
	}
	
	@Override public void AddFrameRendered() {
		rendered += 1;
		updateLine();
	}
	
	@Override public void displayStats(Stats stats) {
		remaining = stats.getRemainingFrame();
		creditsEarned = String.valueOf(stats.getCreditsEarnedDuringSession());
		updateLine();
	}
	
	@Override public void displayUploadQueueStats(int queueSize, long queueVolume) {
		this.uploadQueueSize = queueSize;
		this.uploadQueueVolume = queueVolume;
	}
	
	@Override public void setRemainingTime(String time_) {
		this.eta = time_;
		updateLine();
	}
	
	@Override public void setRenderingTime(String time_) {
		status = "Rendering " + time_;
		updateLine();
	}
	
	@Override public void setClient(Client cli) {
		client = cli;
	}
	
	@Override public void setComputeMethod(String computeMethod_) {
		computeMethod = computeMethod_;
	}
	
	@Override public Client getClient() {
		return client;
	}
	
	@Override public void successfulAuthenticationEvent(String publickey) {
	
	}
	
	private void updateLine() {
		int charToRemove = line.length();
		
		System.out.print("\r");
		
		line = String.format("%s Frames: %d Points: %s | Upload Queue: %d%s | %%s %s %s", df.format(new Date()), rendered,
			creditsEarned != null ? creditsEarned : "unknown", this.uploadQueueSize,
			(this.uploadQueueSize > 0 ? String.format(" (%.2fMB)", (this.uploadQueueVolume / 1024.0 / 1024.0)) : ""), computeMethod,
			status + (exiting ? " (Exiting after all frames are uploaded)" : ""));
		
		if (line.length() + project.length() > 120) {
			// If the line without the project name is already >120 characters (might happen if the user has thousands of frames and millions of points in the
			// session + is exiting after all frames are uploaded) then set the line to 117c to avoid a negative number exception in substring function
			int lineLength = (line.length() >= 120 ? 117 : line.length());
			line = String.format(line, project.substring(0, 117 - lineLength) + "...");
		}
		else {
			line = String.format(line, project);
		}
		
		System.out.print(line);
		for (int i = line.length(); i <= charToRemove; i++) {
			System.out.print(" ");
		}
	}
	
	private String showProgress(String message, int progress, long size) {
		StringBuilder progressBar = new StringBuilder(140);
		progressBar
			.append(message)
			.append(String.join("", Collections.nCopies(progress == 0 ? 2 : 2 - (int) (Math.log10(progress)), " ")))
			.append(String.format(" %d%%%% [", progress))
			.append(String.join("", Collections.nCopies((int) (progress / 10), "=")))
			.append('>')
			.append(String.join("", Collections.nCopies(10 - (int) (progress / 10), " ")))
			.append(']');
		
		if (size > 0) {
			progressBar.append(String.format(" %dMB", (size / 1024 / 1024)));
		}
		
		if (!this.eta.equals("")) {
			progressBar.append(String.format(" ETA %s", this.eta));
		}
		
		return progressBar.toString();
	}
}
