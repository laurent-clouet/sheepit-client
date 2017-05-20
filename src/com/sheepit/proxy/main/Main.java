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

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import static org.kohsuke.args4j.ExampleMode.REQUIRED;
import org.kohsuke.args4j.Option;

import java.io.File;

import com.sheepit.proxy.main.GuiSwing;
import com.sheepit.proxy.main.GuiText;
import com.sheepit.proxy.ProxyRunner;
import com.sheepit.proxy.Configuration;
import com.sheepit.proxy.Gui;
import com.sheepit.proxy.Log;
import com.sheepit.proxy.SettingsLoader;

public class Main {
	@Option(name = "-server", usage = "Render-farm server, default https://client.sheepit-renderfarm.com", metaVar = "URL", required = false)
	private String server = "http://sandbox.sheepit-renderfarm.com";
	
	@Option(name = "-port", usage = "Bind port", metaVar = "8080", required = false)
	private int port = 2060;
	
	@Option(name = "-cache-dir", usage = "Cache/Working directory. Caution, everything in it not related to the render-farm will be removed", metaVar = "/tmp/cache", required = false)
	private String cache_dir = null;
	
	@Option(name = "--verbose", usage = "Display log", required = false)
	private boolean print_log = false;
	
	@Option(name = "-ui", usage = "Specify the user interface to use, default '" + GuiSwing.type + "', available '" + GuiText.type + "', '" + GuiSwing.type + "' (graphical)", required = false)
	private String ui_type = null;
	
	@Option(name = "-config", usage = "Specify the configuration file", required = false)
	private String config_file = null;
	
	@Option(name = "--version", usage = "Display application version", required = false, handler = VersionParameterHandler.class)
	private VersionParameterHandler versionHandler;
	
	public static void main(String[] args) {
		new Main().doMain(args);
	}
	
	public void doMain(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		}
		catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("Usage: ");
			parser.printUsage(System.err);
			System.err.println();
			System.err.println("Example: java " + this.getClass().getName() + " " + parser.printExample(REQUIRED));
			return;
		}
		
		Configuration config = new Configuration(server, null, port);
		config.setPrintLog(print_log);
		
		if (cache_dir != null) {
			File a_dir = new File(cache_dir);
			a_dir.mkdirs();
			if (a_dir.isDirectory() && a_dir.canWrite()) {
				config.setCacheDir(a_dir);
			}
		}
		
		if (ui_type != null) {
			config.setUIType(ui_type);
		}
		
		if (config_file != null) {
			if (new File(config_file).exists() == false) {
				System.err.println("Configuration file not found.");
				System.err.println("Aborting");
				System.exit(2);
			}
			new SettingsLoader(config_file).merge(config);
		}
		
		Log.getInstance(config).debug("client version " + config.getJarVersion());
		
		Gui gui;
		String type = config.getUIType();
		if (type == null) {
			type = "swing";
		}
		switch (type) {
			case GuiText.type:
				gui = new GuiText();
				break;
			default:
			case GuiSwing.type:
				if (java.awt.GraphicsEnvironment.isHeadless()) {
					System.out.println("Graphical ui can not be launch.");
					System.out.println("You should set a DISPLAY or use a text ui (with -ui " + GuiText.type + ").");
					System.exit(3);
				}
				gui = new GuiSwing();
				break;
		}
		ProxyRunner proxy = new ProxyRunner(gui, config, server);
		gui.setProxyRunner(proxy);
		
		gui.start();
	}
}
