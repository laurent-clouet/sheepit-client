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

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import com.sheepit.client.Client;
import com.sheepit.client.Gui;
import com.sheepit.client.Log;

public class GuiText implements Gui {
	public static final String type = "text";
	
	private int framesRendered;
	private Log log;
	
	private Client client;
	
	private ResourceBundle guiResources;
	
	public GuiText() {
		this.framesRendered = 0;
		this.log = Log.getInstance(null);
		this.guiResources = ResourceBundle.getBundle("GUIResources", Locale.getDefault());
	}
	
	@Override
	public void start() {
		if (client != null) {
			client.run();
			client.stop();
		}
	}
	
	@Override
	public void stop() {
	}
	
	@Override
	public void status(String msg_) {
		System.out.println(msg_);
		log.debugF("GenericGUIStatus", new Object[]{msg_});
	}
	
	@Override
	public void error(String err_) {
		MessageFormat formatter = new MessageFormat(ResourceBundle.getBundle("LogResources", this.guiResources.getLocale()).getString("GenericError"), this.guiResources.getLocale());
		System.out.println(formatter.format(new Object[]{err_}));
		log.errorF("GenericError", new Object[]{err_});
	}
	
	@Override
	public void AddFrameRendered() {
		this.framesRendered += 1;
		MessageFormat formatter = new MessageFormat(this.guiResources.getString("FrameRendered"), this.guiResources.getLocale());
		System.out.println(formatter.format(new Object[]{this.framesRendered}));
	}
	
	@Override
	public void framesRemaining(int n_) {
		MessageFormat formatter = new MessageFormat(this.guiResources.getString("FrameRemaining"), this.guiResources.getLocale());
		System.out.println(formatter.format(new Object[]{n_}));
	}
	
	@Override
	public void setClient(Client cli) {
		client = cli;
		guiResources = ResourceBundle.getBundle("GUIResources", cli.getConfiguration().getLocale());
	}
	
	@Override
	public Client getClient() {
		return client;
	}
	
}
