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
import com.sheepit.client.Job;
import com.sheepit.client.Log;
import com.sheepit.client.Stats;
import com.sheepit.client.standalone.text.CLIInputActionHandler;
import com.sheepit.client.standalone.text.CLIInputObserver;

import sun.misc.Signal;
import sun.misc.SignalHandler;

public class GuiText implements Gui {
	public static final String type = "text";
	
	private int framesRendered;
	
	private int sigIntCount = 0;
	
	private Log log;
	
	private Client client;
	
	public GuiText() {
		this.framesRendered = 0;
		this.log = Log.getInstance(null);
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
	
	@Override
	public void stop() {
		Runtime.getRuntime().halt(0);
	}
	
	@Override
	public void status(String msg_) {
		System.out.println(msg_);
		log.debug("GUI " + msg_);
	}
	
	@Override
	public void error(String err_) {
		System.out.println("Error " + err_);
		log.error("Error " + err_);
	}
	
	@Override
	public void AddFrameRendered() {
		this.framesRendered += 1;
		System.out.println("Frames rendered: " + this.framesRendered);
	}
	
	@Override
	public void displayStats(Stats stats) {
		System.out.println("Frames remaining: " + stats.getRemainingFrame());
		System.out.println("Credits earned: " + stats.getCreditsEarnedDuringSession());
	}
	
	@Override
	public void setRenderingProjectName(String name_) {
		if (name_ != null && name_.isEmpty() == false) {
			System.out.println("Rendering project \"" + name_ + "\"");
		}
	}
	
	@Override
	public void setRemainingTime(String time_) {
		System.out.println("Rendering (remaining " + time_ + ")");
	}
	
	@Override
	public void setRenderingTime(String time_) {
		System.out.println("Rendering " + time_);
	}
	
	@Override
	public void setClient(Client cli) {
		client = cli;
	}
	
	@Override
	public void setComputeMethod(String computeMethod) {
		System.out.println("Compute method: " + computeMethod);
	}
	
	@Override
	public Client getClient() {
		return client;
	}
	
	@Override
	public void setSuspended() {
	}

	@Override
	public void setResumed() {
	}
}
