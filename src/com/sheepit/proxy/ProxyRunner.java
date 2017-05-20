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

package com.sheepit.proxy;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.jetty.server.Server;

public class ProxyRunner {
	private Gui gui;
	private Configuration config;
	private Log log;
	private boolean running;
	
	private Server server;
	
	public ProxyRunner(Gui gui_, Configuration config, String url_) {
		this.config = config;
		this.log = Log.getInstance(this.config);
		this.gui = gui_;
		this.running = false;
		this.server = null;
	}
	
	public Gui getGui() {
		return gui;
	}
	
	public Configuration getConfiguration() {
		return config;
	}
	
	public Log getLog() {
		return log;
	}
	
	public int run() {
		log.debug("Proxy::run()");
		running = true;
		
		try {
			
			while (this.running) {
				log.debug("Proxy::run start web server on " + config.getPort());
				gui.status("proxy started");
				server = new Server(config.getPort());
				server.setHandler(new Proxy(config.getServerUrl(), config.getCacheDir().getAbsolutePath()));
				server.start();
				//server.dumpStdErr();
				server.join();
				
			}
		}
		catch (Exception e1) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e1.printStackTrace(pw);
			this.log.debug("Proxy::run exception(D) " + e1 + " stacktrace: " + sw.toString());
			return -99;
		}
		
	//	this.gui.stop();
		return 0;
	}
	
	public boolean isRunning() {
		return this.running;
	}
	
	public synchronized int stop() {
		log.debug("Proxy::stop()");
		running = false;
		try {
			server.stop();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		server = null;
		
		gui.status("proxy stoped");
		//this.config.removeWorkingDirectory();
		
		return 0;
	}
}
