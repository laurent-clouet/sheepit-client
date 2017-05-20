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

package com.sheepit.proxy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Iterator;

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
	
	public static boolean noFreeSpaceOnDisk(String destination_) {
		try {
			File file = new File(destination_);
			return (file.getUsableSpace() < 512 * 1024); // at least the same amount as Server.HTTPGetFile
		}
		catch (SecurityException e) {
		}
		
		return false;
	}
	
	
	
	
	
	
	


	  public static String dumpToString (Collection c, String separator)
	  {
	    String retval = "";
	    for (Iterator it = c.iterator(); it.hasNext();) {
	      retval += String.valueOf(it.next());
	      retval += separator;
	    }
	    return retval;
	  }

	  public static String dumpToString (Collection c)
	  {
	    return dumpToString (c, " ");
	  }

	  public static void print (Collection c) 
	  {
	    System.out.println (dumpToString (c));
	  }

	  public static void print (Collection c, String separator) 
	  {
	    System.out.println (dumpToString (c, separator));
	  }

	
	
}
