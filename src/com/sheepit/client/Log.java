/*
 * Copyright (C) 2011-2014 Laurent CLOUET
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

package com.sheepit.client;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class Log {
	private static Log instance = null;
	
	private Map<Integer, ArrayList<String>> checkpoints = new HashMap<Integer, ArrayList<String>>();
	private int lastCheckPoint;
	private DateFormat dateFormat;
	
	private boolean printStdOut;
	
	private ResourceBundle logResourcesEnglish, logResourcesLocal;
	
	private Log(boolean print_, Locale locale_) {
		this.printStdOut = print_;
		this.lastCheckPoint = 0;
		this.checkpoints.put(this.lastCheckPoint, new ArrayList<String>());
		this.dateFormat = new SimpleDateFormat("dd-MM kk:mm:ss");
		this.logResourcesEnglish = ResourceBundle.getBundle("LogResources", new Locale("en", "US"));
		this.logResourcesLocal = ResourceBundle.getBundle("LogResources", locale_);
	}
	
	public void debug(String msgKey_) {
		this.append("Debug", msgKey_);
	}
	
	public void debugF(String msgKey_, Object[] objects_) {
		this.appendF("Debug", msgKey_, objects_);
	}
	
	public void debugR(String msg) {
		this.appendR("Debug", msg);
	}
	
	public void info(String msgKey_) {
		this.append("Info", msgKey_);
	}
	
	public void infoF(String msgKey_, Object[] objects_) {
		this.appendF("Info", msgKey_, objects_);
	}
	
	public void infoR(String msg) {
		this.appendR("Info", msg);
	}
	
	public void error(String msgKey_) {
		this.append("Error", msgKey_);
	}
	
	public void errorF(String msgKey_, Object[] objects_) {
		this.appendF("Error", msgKey_, objects_);
	}
	
	public void errorR(String msg) {
		this.appendR("Error", msg);
	}
	
	private void append(String levelKey_, String msgKey_) {
		if (!msgKey_.equals("")) {
			if (this.checkpoints.containsKey(this.lastCheckPoint)) {
				this.checkpoints.get(this.lastCheckPoint).add(this.dateFormat.format(new java.util.Date()) + " (" + this.logResourcesEnglish.getString(levelKey_) + ") " + this.logResourcesEnglish.getString(msgKey_));
			}
			if (this.printStdOut == true) {
				System.out.println(this.dateFormat.format(new java.util.Date()) + " (" + this.logResourcesLocal.getString(levelKey_) + ") " + this.logResourcesLocal.getString(msgKey_));
			}
		}
	}
	
	private void appendF(String levelKey_, String msgKey_, Object[] objects_) {
		if(objects_.length == 0) {
			this.append(levelKey_, msgKey_);
			return;
		}
		if (!msgKey_.equals("")) {
			if (this.checkpoints.containsKey(this.lastCheckPoint)) {
				MessageFormat level = new MessageFormat(this.logResourcesEnglish.getString(levelKey_), this.logResourcesEnglish.getLocale());
				MessageFormat msg = new MessageFormat(this.logResourcesEnglish.getString(msgKey_), this.logResourcesEnglish.getLocale());
				this.checkpoints.get(this.lastCheckPoint).add(this.dateFormat.format(new java.util.Date()) + " (" + level.format(objects_) + ") " + msg.format(objects_));
			}
			if (this.printStdOut == true) {
				MessageFormat msg = new MessageFormat(this.logResourcesLocal.getString(msgKey_), this.logResourcesLocal.getLocale());
				System.out.println(this.dateFormat.format(new java.util.Date()) + " (" + this.logResourcesLocal.getString(levelKey_) + ") " + msg.format(objects_));
			}
		}
	}
	
	private void appendR(String levelKey_, String msg_) {
		if (!msg_.equals("")) {
			if (this.checkpoints.containsKey(this.lastCheckPoint)) {
				this.checkpoints.get(this.lastCheckPoint).add(this.dateFormat.format(new java.util.Date()) + " (" + this.logResourcesEnglish.getString(levelKey_) + ") " + msg_);
			}
			if (this.printStdOut == true) {
				System.out.println(this.dateFormat.format(new java.util.Date()) + " (" + this.logResourcesLocal.getString(levelKey_) + ") " + msg_);
			}
		}
	}
	
	public int newCheckPoint() {
		int time = (int) (new Date().getTime());
		this.checkpoints.put(time, new ArrayList<String>());
		this.lastCheckPoint = time;
		return this.lastCheckPoint;
	}
	
	public ArrayList<String> getForCheckPoint(int point_) {
		return this.checkpoints.get(point_);
	}
	
	public void removeCheckPoint(int point_) {
		try {
			this.checkpoints.remove(point_);
		}
		catch (UnsupportedOperationException e) {
		}
	}
	
	public static synchronized Log getInstance(Configuration config) {
		if (instance == null) {
			boolean print = false;
			if (config != null) {
				print = config.getPrintLog();
			}
			instance = new Log(print, config.getLocale());
		}
		return instance;
	}
	
	public static synchronized void printCheckPoint(int point_) {
		Log log = Log.getInstance(null);
		ArrayList<String> logs = log.getForCheckPoint(point_);
		for (String alog : logs) {
			System.out.println(alog);
		}
	}
}
