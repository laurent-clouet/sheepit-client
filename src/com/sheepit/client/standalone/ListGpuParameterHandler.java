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

package com.sheepit.client.standalone;

import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import com.sheepit.client.Configuration;
import com.sheepit.client.hardware.gpu.GPU;
import com.sheepit.client.hardware.gpu.GPUDevice;

public class ListGpuParameterHandler<T> extends OptionHandler<T> {
	public ListGpuParameterHandler(CmdLineParser parser, OptionDef option, Setter<? super T> setter) {
		super(parser, option, setter);
	}
	
	@Override public int parseArguments(Parameters params) throws CmdLineException {
		List<GPUDevice> gpus = GPU.listDevices(new Configuration(null, null, null));
		if (gpus != null) {
			for (GPUDevice gpu : gpus) {
				System.out.println("GPU_ID    : " + gpu.getOldId());
				System.out.println("Long ID   : " + gpu.getId());
				System.out.println("Model     : " + gpu.getModel());
				System.out.println("Memory, MB: " + (int) (gpu.getMemory() / (1024 * 1024)));
				System.out.println();
			}
		}
		
		System.exit(0);
		return 0;
	}
	
	@Override public String getDefaultMetaVariable() {
		return null;
	}
}
