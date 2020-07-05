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

package com.sheepit.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sheepit.client.Error.ServerCode;
import com.sheepit.client.exception.FermeExceptionNoSpaceLeftOnDevice;

public class Utils {
	public static int unzipFileIntoDirectory(String zipFileName_, String destinationDirectory, String password, Log log)
			throws FermeExceptionNoSpaceLeftOnDevice {
		try {
			ZipFile zipFile = new ZipFile(zipFileName_);
			UnzipParameters unzipParameters = new UnzipParameters();
			unzipParameters.setIgnoreDateTimeAttributes(true);
			
			if (password != null && zipFile.isEncrypted()) {
				zipFile.setPassword(password);
			}
			zipFile.extractAll(destinationDirectory, unzipParameters);
		}
		catch (ZipException e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			log.debug("Utils::unzipFileIntoDirectory(" + zipFileName_ + "," + destinationDirectory + ") exception " + e + " stacktrace: " + sw.toString());
			return -1;
		}
		return 0;
	}
	
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
	
	public static double lastModificationTime(File directory_) {
		double max = 0.0;
		if (directory_.isDirectory()) {
			File[] list = directory_.listFiles();
			if (list != null) {
				for (File aFile : list) {
					double max1 = lastModificationTime(aFile);
					if (max1 > max) {
						max = max1;
					}
				}
			}
		}
		else if (directory_.isFile()) {
			return directory_.lastModified();
		}
		
		return max;
	}
	
	public static ServerCode statusIsOK(Document document_, String rootname_) {
		if (document_ == null) {
			return Error.ServerCode.UNKNOWN;
		}
		NodeList ns = document_.getElementsByTagName(rootname_);
		if (ns.getLength() == 0) {
			return Error.ServerCode.ERROR_NO_ROOT;
		}
		Element a_node = (Element) ns.item(0);
		if (a_node.hasAttribute("status")) {
			return Error.ServerCode.fromInt(Integer.parseInt(a_node.getAttribute("status")));
		}
		return Error.ServerCode.UNKNOWN;
	}
	
	/**
	 * Will recursively delete a directory
	 */
	public static void delete(File file) {
		if (file == null) {
			return;
		}
		if (file.isDirectory()) {
			String files[] = file.list();
			if (files != null) {
				if (files.length != 0) {
					for (String temp : files) {
						File fileDelete = new File(file, temp);
						delete(fileDelete);
					}
				}
			}
		}
		file.delete();
	}
	
	/**
	 * Parse a number string to a number.
	 * Input can be as "32", "10k", "100K", "100G", "1.3G", "0.4T"
	 */
	public static long parseNumber(String in) {
		in = in.trim();
		in = in.replaceAll(",", ".");
		try {
			return Long.parseLong(in);
		}
		catch (NumberFormatException e) {
		}
		final Matcher m = Pattern.compile("([\\d.,]+)\\s*(\\w)").matcher(in);
		m.find();
		int scale = 1;
		switch (m.group(2).charAt(0)) {
			case 'T':
			case 't':
				scale = 1000 * 1000 * 1000 * 1000;
				break;
			case 'G':
			case 'g':
				scale = 1000 * 1000 * 1000;
				break;
			case 'M':
			case 'm':
				scale = 1000 * 1000;
				break;
			case 'K':
			case 'k':
				scale = 1000;
				break;
		}
		return Math.round(Double.parseDouble(m.group(1)) * scale);
	}
	
	public static String humanDuration(Date date) {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		calendar.setTime(date);
		
		int hours = (calendar.get(Calendar.DAY_OF_MONTH) - 1) * 24 + calendar.get(Calendar.HOUR_OF_DAY);
		int minutes = calendar.get(Calendar.MINUTE);
		int seconds = calendar.get(Calendar.SECOND);
		
		String output = "";
		if (hours > 0) {
			output += hours + "h ";
		}
		if (minutes > 0) {
			output += minutes + "min ";
		}
		if (seconds > 0) {
			output += seconds + "s";
		}
		return output;
	}
	
	public static boolean noFreeSpaceOnDisk(String destination_) {
		try {
			File file = new File(destination_);
			return (file.getUsableSpace() < 512 * 1024); // at least the same amount as Server.HTTPGetFile
		}
		catch (SecurityException e) {
		}
		
		return false;
	}

	public static String findMimeType(String file) throws IOException {
		String mimeType = Files.probeContentType(Paths.get(file));
		if (mimeType == null) {
			InputStream stream = new BufferedInputStream(new FileInputStream(file));
			mimeType = URLConnection.guessContentTypeFromStream(stream);
		}
		if (mimeType == null) {
			mimeType = URLConnection.guessContentTypeFromName(file);
		}

		return mimeType;
	}
}
