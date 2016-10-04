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
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class GuiTextOneLine implements Gui {
	public static final String type = "oneLine";
	
	private int rendered;
	private int remaining;
	private String creditsEarned;
	private int sigIntCount = 0;

	private String status;
	private String line;

	private boolean exiting = false;
	
	private Client client;
	
	public GuiTextOneLine() {
		rendered = 0;
		remaining = 0;
		creditsEarned = null;
		status = "";
		line = "";
	}
	
	@Override
	public void start() {
		if (client != null) {

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
	public void error(String msg_) {
		status = "Error " + msg_;
		updateLine();
	}
	
	@Override
	public void AddFrameRendered() {
		rendered += 1;
		creditsEarned = client.getServer().getCreditEarnedOnCurrentSession();
		updateLine();
	}
	
	@Override
	public void framesRemaining(int n_) {
		remaining = n_;
		updateLine();
	}
	
	@Override
	public void setClient(Client cli) {
		client = cli;
	}
	
	@Override
	public Client getClient() {
		return client;
	}
	
	private void updateLine() {
		int charToRemove = line.length();
		
		System.out.print("\r");
		line = String.format("Frames rendered: %d remaining: %d Credits earned: %s | %s", rendered, remaining, creditsEarned != null ? creditsEarned : "unknown", status + (exiting ? " (Exiting after this frame)" : ""));
		System.out.print(line);
		for (int i = line.length(); i <= charToRemove; i++) {
			System.out.print(" ");
		}
	}
}
