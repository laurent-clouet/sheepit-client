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

import com.sheepit.client.dto.Stats;
import com.sheepit.client.dto.TransferStats;

public interface Gui {
	void start();
	
	void stop();
	
	void status(String msg_);
	
	void status(String msg_, boolean overwriteSuspendedMsg);
	
	void status(String msg_, int progress);
	
	void status(String msg_, int progress, long size);
	
	void updateTrayIcon(Integer percentage_);
	
	void setRenderingProjectName(String name_);
	
	void setRemainingTime(String time_);
	
	void setRenderingTime(String time_);
	
	void displayTransferStats(TransferStats downloads, TransferStats uploads);
	
	void displayStats(Stats stats);
	
	void displayUploadQueueStats(int queueSize, long queueVolume);
	
	void error(String err_);
	
	void AddFrameRendered();
	
	void successfulAuthenticationEvent(String publickey);
	
	void setClient(Client cli);
	
	void setComputeMethod(String computeMethod_);
	
	Client getClient();
}
