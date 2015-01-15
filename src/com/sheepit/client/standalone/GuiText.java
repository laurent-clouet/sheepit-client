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

public class GuiText implements Gui {
	private int framesRendered;
	private Log log;
	
	public GuiText() {
		this.framesRendered = 0;
		this.log = Log.getInstance(null);
	}
	
	@Override
	public void start() {
	}
	
	@Override
	public void stop() {
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
		System.out.println("frame rendered: " + this.framesRendered);
		
	}
	
	@Override
	public void framesRemaining(int n_) {
		System.out.println("frame remaining: " + n_);
	}
	
	@Override
	public void setClient(Client cli) {
	}
	
	@Override
	public Client getClient() {
		return null;
	}
	
}
