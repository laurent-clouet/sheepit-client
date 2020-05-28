/*
 * Copyright (C) 2015 Laurent CLOUET
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

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import com.sheepit.client.Configuration;

public class VersionParameterHandler<T> extends OptionHandler<T> {
	public VersionParameterHandler(CmdLineParser parser, OptionDef option, Setter<? super T> setter) {
		super(parser, option, setter);
	}
	
	@Override public int parseArguments(Parameters params) throws CmdLineException {
		Configuration config = new Configuration(null, "", "");
		System.out.println("Version: " + config.getJarVersion());
		System.exit(0);
		return 0;
	}
	
	@Override public String getDefaultMetaVariable() {
		return null;
	}
}
