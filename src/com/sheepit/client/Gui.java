/*
 * Copyright (C) 2010-2013 Laurent CLOUET
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

public interface Gui {
	public void start();
	
	public void stop();
	
	public void status(String msg_);
	
	public void status(String msg_, boolean overwriteSuspendedMsg);
	
	public void status(String msg_, int progress);
	
	public void status(String msg_, int progress, long size);
	
	public void updateTrayIcon(Integer percentage_);
	
	public void setRenderingProjectName(String name_);
	
	public void setRemainingTime(String time_);
	
	public void setRenderingTime(String time_);
	
	public void  displayTransferStats(TransferStats downloads, TransferStats uploads);
	
	public void displayStats(Stats stats);
	
	public void displayUploadQueueStats(int queueSize, long queueVolume);
	
	public void error(String err_);
	
	public void AddFrameRendered();
	
	public void successfulAuthenticationEvent(String publickey);
	
	public void setClient(Client cli);
	
	public void setComputeMethod(String computeMethod_);
	
	public Client getClient();
}
