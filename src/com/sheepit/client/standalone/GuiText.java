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
					else if (client.isRunning() && !client.isSuspended()) {
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
		String creditsEarned = this.client.getServer().getCreditEarnedOnCurrentSession();
		System.out.println("Credits earned: " + (creditsEarned != null ? creditsEarned : "unknown"));
	}
	
	@Override
	public void framesRemaining(int n_) {
		System.out.println("Frames remaining: " + n_);
	}
	
	@Override
	public void setClient(Client cli) {
		client = cli;
	}
	
	@Override
	public Client getClient() {
		return client;
	}

	@Override
	public void setJobPercentage(int percent) {

	}

	@Override
	public void frameDone() {

	}

}
