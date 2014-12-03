/*
 * Copyright (C) 2013-2014 Laurent CLOUET
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

public class Error {
	public enum Type {
		OK,
		WRONG_CONFIGURATION,
		AUTHENTICATION_FAILED,
		TOO_OLD_CLIENT,
		SESSION_DISABLED,
		MISSING_RENDER,
		MISSING_SCENE,
		NOOUTPUTFILE,
		DOWNLOAD_FILE,
		CAN_NOT_CREATE_DIRECTORY,
		NETWORK_ISSUE, RENDERER_CRASHED,
		RENDERER_KILLED,
		RENDERER_MISSING_LIBRARIES,
		FAILED_TO_EXECUTE,
		UNKNOWN
	};
	
	public enum ServerCode {
		OK(0),
		UNKNOWN(999),
		
		CONFIGURATION_ERROR_NO_CLIENT_VERSION_GIVEN(100),
		CONFIGURATION_ERROR_CLIENT_TOO_OLD(101),
		CONFIGURATION_ERROR_AUTH_FAILED(102),
		CONFIGURATION_ERROR_WEB_SESSION_EXPIRATED(103),
		CONFIGURATION_ERROR_MISSING_PARAMETER(104),
		
		JOB_REQUEST_NOJOB(200),
		JOB_REQUEST_ERROR_NO_RENDERING_RIGHT(201),
		JOB_REQUEST_ERROR_DEAD_SESSION(202),
		JOB_REQUEST_ERROR_SESSION_DISABLED(203),
		JOB_REQUEST_ERROR_INTERNAL_ERROR(204),
		
		JOB_VALIDATION_ERROR_MISSING_PARAMETER(300),
		JOB_VALIDATION_ERROR_BROKEN_MACHINE(301), // in GPU the generated frame is black
		JOB_VALIDATION_ERROR_FRAME_IS_NOT_IMAGE(302),
		JOB_VALIDATION_ERROR_UPLOAD_FAILED(303),
		JOB_VALIDATION_ERROR_SESSION_DISABLED(304), // missing heartbeat or broken machine
		
		KEEPMEALIVE_STOP_RENDERING(400),
		
		// internal error handling
		ERROR_NO_ROOT(2),
		ERROR_REQUEST_FAILED(5);
		
		private final int id;
		
		private ServerCode(int id) {
			this.id = id;
		}
		
		public int getValue() {
			return id;
		}
		
		public static ServerCode fromInt(int val) {
			ServerCode[] As = ServerCode.values();
			for (int i = 0; i < As.length; i++) {
				if (As[i].getValue() == val) {
					return As[i];
				}
			}
			return ServerCode.UNKNOWN;
		}
	}
	
	public static Type ServerCodeToType(ServerCode sc) {
		switch (sc) {
			case OK:
				return Type.OK;
			case UNKNOWN:
				return Type.UNKNOWN;
			case CONFIGURATION_ERROR_CLIENT_TOO_OLD:
				return Type.TOO_OLD_CLIENT;
			case CONFIGURATION_ERROR_AUTH_FAILED:
				return Type.AUTHENTICATION_FAILED;
				
			case CONFIGURATION_ERROR_NO_CLIENT_VERSION_GIVEN:
			case CONFIGURATION_ERROR_WEB_SESSION_EXPIRATED:
				return Type.WRONG_CONFIGURATION;
				
			case JOB_REQUEST_ERROR_SESSION_DISABLED:
			case JOB_VALIDATION_ERROR_SESSION_DISABLED:
				return Type.SESSION_DISABLED;
				
			default:
				return Type.UNKNOWN;
		}
	}
	
	public static String humainString(Type in) {
		switch (in) {
			case TOO_OLD_CLIENT:
				return "This client is too old, you need to update it";
			case AUTHENTICATION_FAILED:
				return "Failed to authenticate, please check your login and password";
			case NOOUTPUTFILE:
				return "Renderer have generated no output file, it's mostly a wrong project configuration or your are missing required libraries. Will try an another project in few minutes.";
			case RENDERER_CRASHED:
				return "Renderer have crashed. It's mostly due to a bad project or not enough memory. There is nothing you can do about it. Will try an another project in few minutes.";
			case RENDERER_MISSING_LIBRARIES:
				return "Failed to launch runderer. Please check if you have necessary libraries installed and if you have enough free place in working directory.";
			case RENDERER_KILLED:
			    return "The render stop because either you ask to stop or the server (usually render time too high).";
			case SESSION_DISABLED:
				return "The server have disabled your session. It's mostly because your client generate broken frame (gpu not compatible for example).";
			default:
				return in.toString();
		}
	}
}
