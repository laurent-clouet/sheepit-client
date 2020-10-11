/*
 * Copyright (C) 2016 Laurent CLOUET
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

package com.sheepit.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Stats {
	private final int remainingFrame;
	private final int creditsEarned;
	private final int creditsEarnedSession;
	private final int renderableProject;
	private final int waitingProject;
	private final int connectedMachine;
	
	@Override public String toString() {
		return "Stats [remainingFrame=" + remainingFrame + ", creditsEarned=" + creditsEarned + ", creditsEarnedSession=" + creditsEarnedSession
				+ ", renderableProject=" + renderableProject + ", waitingProject=" + waitingProject + ", connectedMachine=" + connectedMachine + "]";
	}
}
