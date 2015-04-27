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

import java.io.File;

import com.sheepit.client.os.OS;

public class Job {
	private String numFrame;
	private String sceneMD5;
	private String rendererMD5;
	private String id;
	private String revision;
	private String pictureFilename;
	private String path; // path inside of the archive
	private String rendererCommand;
	private String script;
	private boolean useGPU;
	private String extras;
	private String updateRenderingStatusMethod;
	private boolean synchronousUpload;
	private RenderProcess render;
	private boolean askForRendererKill;
	
	private Configuration config;
	
	public Job(Configuration config_, String id_, String frame_, String revision_, String path_, boolean use_gpu, String command_, String script_, String sceneMd5_, String rendererMd5_, String extras_, boolean synchronous_upload_, String update_method_) {
		config = config_;
		id = id_;
		numFrame = frame_;
		revision = revision_;
		path = path_;
		useGPU = use_gpu;
		rendererCommand = command_;
		sceneMD5 = sceneMd5_;
		rendererMD5 = rendererMd5_;
		extras = extras_;
		synchronousUpload = synchronous_upload_;
		
		pictureFilename = null;
		script = script_;
		updateRenderingStatusMethod = update_method_;
		askForRendererKill = false;
		
		render = new RenderProcess();
	}
	
	public RenderProcess getProcessRender() {
		return render;
	}
	
	public String toString() {
		return String.format("Job (numFrame '%s' sceneMD5 '%s' rendererMD5 '%s' ID '%s' revision '%s' pictureFilename '%s' jobPath '%s' gpu %s extras '%s' updateRenderingStatusMethod '%s' render %s)", numFrame, sceneMD5, rendererMD5, id, revision, pictureFilename, path, useGPU, extras, updateRenderingStatusMethod, render);
	}
	
	public String getId() {
		return id;
	}
	
	public String getFrameNumber() {
		return numFrame;
	}
	
	public String getExtras() {
		return extras;
	}
	
	public String getScript() {
		return script;
	}
	
	public String getSceneMD5() {
		return sceneMD5;
	}
	
	public String getRenderMd5() {
		return rendererMD5;
	}
	
	public String getPath() {
		return path;
	}
	
	public String getUpdateRenderingStatusMethod() {
		return updateRenderingStatusMethod;
	}
	
	public void setAskForRendererKill(boolean val) {
		askForRendererKill = val;
	}
	
	public boolean getAskForRendererKill() {
		return askForRendererKill;
	}
	
	public String getRenderCommand() {
		return rendererCommand;
	}
	
	public boolean getUseGPU() {
		return useGPU;
	}
	
	public String getRevision() {
		return revision;
	}
	
	public void setOutputImagePath(String path) {
		pictureFilename = path;
	}
	
	public String getOutputImagePath() {
		return pictureFilename;
	}
	
	public String getPrefixOutputImage() {
		return id + "_";
	}
	
	public String getRendererDirectory() {
		return config.workingDirectory.getAbsolutePath() + File.separator + rendererMD5;
	}
	
	public String getRendererPath() {
		return getRendererDirectory() + File.separator + OS.getOS().getRenderBinaryPath();
	}
	
	public String getRendererArchivePath() {
		return config.getStorageDir().getAbsolutePath() + File.separator + rendererMD5 + ".zip";
	}
	
	public String getSceneDirectory() {
		return config.workingDirectory.getAbsolutePath() + File.separator + sceneMD5;
	}
	
	public String getScenePath() {
		return getSceneDirectory() + File.separator + this.path;
	}
	
	public String getSceneArchivePath() {
		return config.workingDirectory.getAbsolutePath() + File.separator + sceneMD5 + ".zip";
	}
	
	public boolean simultaneousUploadIsAllowed() {
		return synchronousUpload;
	}
}
