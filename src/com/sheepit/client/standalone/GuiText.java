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

package com.sheepit.client.standalone;

import com.sheepit.client.Client;
import com.sheepit.client.Gui;
import com.sheepit.client.Log;
import com.sheepit.client.Stats;
import com.sheepit.client.standalone.text.CLIInputActionHandler;
import com.sheepit.client.standalone.text.CLIInputObserver;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GuiText implements Gui {
	public static final String type = "text";
	
	private int framesRendered;
	
	private int sigIntCount = 0;
	private Log log;
	private DateFormat df;
	
	private Client client;
	
	public GuiText() {
		this.framesRendered = 0;
		this.log = Log.getInstance(null);
		this.df = new SimpleDateFormat("MMM dd HH:mm:ss");
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
					
					if (sigIntCount == 4) {
						// This is only for ugly issues that might occur
						System.out.println("WARNING: Hitting Ctrl-C again will force close the application.");
					}
					else if (sigIntCount == 5) {
						Signal.raise(new Signal("INT"));
						Runtime.getRuntime().halt(0);
					}
					else if (client.isRunning() && client.isSuspended() == false) {
						client.askForStop();
						System.out.println("Will exit after current frame... Press Ctrl+C again to exit now.");
					}
					else {
						client.stop();
						GuiText.this.stop();
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
		log.debug("GUI " + msg_);
		
		if (client != null && client.isSuspended()) {
			if (overwriteSuspendedMsg) {
				System.out.println(String.format("%s %s", this.df.format(new Date()), msg_));
			}
		}
		else {
			System.out.println(String.format("%s %s", this.df.format(new Date()), msg_));
		}
	}
	
	@Override public void error(String err_) {
		System.out.println(String.format("ERROR: %s %s", this.df.format(new Date()), err_));
		log.error("Error " + err_);
	}
	
	@Override public void AddFrameRendered() {
		this.framesRendered += 1;
		System.out.println(String.format("%s Frames rendered: %d", this.df.format(new Date()), this.framesRendered));
	}
	
	@Override public void displayStats(Stats stats) {
		System.out.println(String.format("%s Frames remaining: %d", this.df.format(new Date()), stats.getRemainingFrame()));
		System.out.println(String.format("%s Credits earned: %d", this.df.format(new Date()), stats.getCreditsEarnedDuringSession()));
	}
	
	@Override public void displayUploadQueueStats(int queueSize, long queueVolume) {
		// No need to check if the queue is not empty to show the volume bc this line is always shown at the end
		// of the render process in text GUI (unless an error occurred, where the file is uploaded synchronously)
		System.out.println(String.format("%s Queued uploads: %d (%.2fMB)", this.df.format(new Date()), queueSize, (queueVolume / 1024.0 / 1024.0)));
	}
	
	@Override public void setRenderingProjectName(String name_) {
		if (name_ != null && name_.isEmpty() == false) {
			System.out.println(String.format("%s Rendering project \"%s\"", this.df.format(new Date()), name_));
		}
	}
	
	@Override public void setRemainingTime(String time_) {
		System.out.println(String.format("%s Rendering (remaining %s)", this.df.format(new Date()), time_));
	}
	
	@Override public void setRenderingTime(String time_) {
		System.out.println(String.format("%s Rendering %s", this.df.format(new Date()), time_));
	}
	
	@Override public void setClient(Client cli) {
		client = cli;
	}
	
	@Override public void setComputeMethod(String computeMethod) {
		System.out.println(String.format("%s Compute method: %s", this.df.format(new Date()), computeMethod));
	}
	
	@Override public Client getClient() {
		return client;
	}
	
	@Override public void successfulAuthenticationEvent(String publickey) {
	
	}
}
