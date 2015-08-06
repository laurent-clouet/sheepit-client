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

import java.util.Locale;
import java.util.ResourceBundle;

public class Error {
	public enum Type {
		// id have to be kept synchronised with the server side.
		OK(0),
		UNKNOWN(99),
		WRONG_CONFIGURATION(1),
		AUTHENTICATION_FAILED(2),
		TOO_OLD_CLIENT(3),
		SESSION_DISABLED(4),
		RENDERER_NOT_AVAILABLE(5),
		MISSING_RENDER(6),
		MISSING_SCENE(7),
		NOOUTPUTFILE(8),
		DOWNLOAD_FILE(9),
		CAN_NOT_CREATE_DIRECTORY(10),
		NETWORK_ISSUE(11),
		RENDERER_CRASHED(12),
		RENDERER_OUT_OF_VIDEO_MEMORY(13),
		RENDERER_KILLED(14),
		RENDERER_MISSING_LIBRARIES(15),
		FAILED_TO_EXECUTE(16),
		OS_NOT_SUPPORTED(17),
		CPU_NOT_SUPPORTED(18),
		GPU_NOT_SUPPORTED(19);
		
		private final int id;
		
		private Type(int id) {
			this.id = id;
		}
		
		public int getValue() {
			return id;
		}
	}
	
	public enum ServerCode {
		OK(0),
		UNKNOWN(999),
		
		CONFIGURATION_ERROR_NO_CLIENT_VERSION_GIVEN(100),
		CONFIGURATION_ERROR_CLIENT_TOO_OLD(101),
		CONFIGURATION_ERROR_AUTH_FAILED(102),
		CONFIGURATION_ERROR_WEB_SESSION_EXPIRED(103),
		CONFIGURATION_ERROR_MISSING_PARAMETER(104),
		
		JOB_REQUEST_NOJOB(200),
		JOB_REQUEST_ERROR_NO_RENDERING_RIGHT(201),
		JOB_REQUEST_ERROR_DEAD_SESSION(202),
		JOB_REQUEST_ERROR_SESSION_DISABLED(203),
		JOB_REQUEST_ERROR_INTERNAL_ERROR(204),
		JOB_REQUEST_ERROR_RENDERER_NOT_AVAILABLE(205),
		
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
			for (ServerCode A : As) {
				if (A.getValue() == val) {
					return A;
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
			case CONFIGURATION_ERROR_WEB_SESSION_EXPIRED:
				return Type.WRONG_CONFIGURATION;
				
			case JOB_REQUEST_ERROR_SESSION_DISABLED:
			case JOB_VALIDATION_ERROR_SESSION_DISABLED:
				return Type.SESSION_DISABLED;
				
			case JOB_REQUEST_ERROR_RENDERER_NOT_AVAILABLE:
				return Type.RENDERER_NOT_AVAILABLE;
			
			default:
				return Type.UNKNOWN;
		}
	}
	
	public static String humanString(Type in, Locale locale) {
		ResourceBundle exceptionResources = ResourceBundle.getBundle("ExceptionResources", locale);
		switch (in) {
			case TOO_OLD_CLIENT:
				return exceptionResources.getString("OutdatedClient");
			case AUTHENTICATION_FAILED:
				return exceptionResources.getString("AuthenticationFailed");
			case DOWNLOAD_FILE:
				return exceptionResources.getString("DownloadError");
			case NOOUTPUTFILE:
				return exceptionResources.getString("NoRendererOutput");
			case RENDERER_CRASHED:
				return exceptionResources.getString("CrashedRenderer");
			case RENDERER_OUT_OF_VIDEO_MEMORY:
				return exceptionResources.getString("CrashedRendererVram");
			case GPU_NOT_SUPPORTED:
				return exceptionResources.getString("UnsupportedGPU");
			case RENDERER_MISSING_LIBRARIES:
				return exceptionResources.getString("MissingLibraries");
			case RENDERER_KILLED:
				return exceptionResources.getString("KilledRenderer");
			case SESSION_DISABLED:
				return exceptionResources.getString("SessionDisabled");
			case RENDERER_NOT_AVAILABLE:
				return exceptionResources.getString("NoRendererAvailable");
			case OS_NOT_SUPPORTED:
				return exceptionResources.getString("UnsupportedOS");
			case CPU_NOT_SUPPORTED:
				return exceptionResources.getString("UnsupportedCPU");
			default:
				return in.toString();
		}
	}
}
