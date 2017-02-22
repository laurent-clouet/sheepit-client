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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.sheepit.client.Client;

public class CLIInputObserver implements Runnable {
	private BufferedReader in;
	private Client client;
	
	public CLIInputObserver(Client client) {
		this.client = client;
	}
	
	private List<CLIInputListener> listeners = new ArrayList<CLIInputListener>();
	
	public void addListener(CLIInputListener toAdd) {
		listeners.add(toAdd);
	}
	
	public void run() {
		in = new BufferedReader(new InputStreamReader(System.in));
		String line = "";
		
		while ((line != null) && (line.equalsIgnoreCase("quit") == false)) {
			try {
				line = in.readLine();
			}
			catch (Exception e) {
				// TODO: handle exception
			}
			for (CLIInputListener cliil : listeners)
				cliil.commandEntered(client, line);
		}
		try {
			in.close();
		}
		catch (Exception e) {
			// TODO: handle exception
		}
		
	}
}
