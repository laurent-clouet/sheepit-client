/*
 * Copyright (C) 2017 Laurent CLOUET
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

package com.sheepit.client.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

public class Utils {
	
	public static String md5(String path_of_file_) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			InputStream is = Files.newInputStream(Paths.get(path_of_file_));
			DigestInputStream dis = new DigestInputStream(is, md);
			byte[] buffer = new byte[8192];
			while (dis.read(buffer) > 0)
				; // process the entire file
			String data = DatatypeConverter.printHexBinary(md.digest()).toLowerCase();
			dis.close();
			is.close();
			return data;
		}
		catch (NoSuchAlgorithmException | IOException e) {
			return "";
		}
	}
}
