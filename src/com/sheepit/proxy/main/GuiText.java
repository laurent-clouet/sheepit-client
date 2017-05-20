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

package com.sheepit.proxy.main;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import com.sheepit.proxy.Log;
import com.sheepit.proxy.ProxyRunner;
import com.sheepit.proxy.Gui;

public class GuiText implements Gui {
	public static final String type = "text";
	
	private Log log;
	private ProxyRunner proxy;
	
	private int sigIntCount;
	
	public GuiText() {
		sigIntCount = 0;
		log = Log.getInstance(null);
	}
	
	@Override
	public void start() {
		if (proxy != null) {
			Signal.handle(new Signal("INT"), new SignalHandler() {
				@Override
				public void handle(Signal signal) {
					sigIntCount++;
					
					if (sigIntCount == 1) {
						System.out.println("WARNING: Hitting Ctrl-C again will force close the application.");
					}
					else if (sigIntCount == 2) {
						Signal.raise(new Signal("INT"));
						Runtime.getRuntime().halt(0);
					}
				}
			});
			
			proxy.run();
			proxy.stop();
		}
		
	}
	
	@Override
	public void stop() {
		
	}
	
	@Override
	public void status(String msg_) {
		System.out.println("status: " + msg_);
	}
	
	@Override
	public void error(String err_) {
		System.out.println("error: " + err_);
	}
	
	@Override
	public void setProxyRunner(ProxyRunner proxy_) {
		proxy = proxy_;
	}
	
	@Override
	public ProxyRunner getProxyRunner() {
		return proxy;
	}
}
