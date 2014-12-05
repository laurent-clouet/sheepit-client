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
	private int renderDuration; // in second
	private long memoryUsed; // in kB
	private String rendererCommand;
	private String script;
	private int maxOutputNbLines;
	private boolean useGPU;
	private String extras;
	private String updateRenderingStatusMethod;
	private boolean synchronousUpload;
	
	private Process process;
	private boolean askForRendererKill;
	
	private Configuration config;
	
	public Job(Configuration config_, String id_, String frame_, String revision_, String path_, boolean use_gpu, String command_, String script_, String sceneMd5_, String rendererMd5_, String extras_, boolean synchronous_upload_) {
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
		renderDuration = 0;
		memoryUsed = 0;
		script = script_;
		maxOutputNbLines = 0;
		updateRenderingStatusMethod = null;
		process = null;
		askForRendererKill = false;
		
	}
	
	public String toString() {
		return String.format("Job (numFrame '%s' sceneMD5 '%s' rendererMD5 '%s' ID '%s' revision '%s' pictureFilename '%s' jobPath '%s' renderDuration '%s', memoryUsed %skB gpu %s extras '%s' updateRenderingStatusMethod '%s')", this.numFrame, this.sceneMD5, this.rendererMD5, this.id, this.revision, this.pictureFilename, this.path, this.renderDuration, this.memoryUsed, this.useGPU, this.extras, this.updateRenderingStatusMethod);
	}
	
	public boolean workeable() {
		return true;
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
	
	public int getMaxOutputNbLines() {
		return maxOutputNbLines;
	}
	
	public void setAskForRendererKill(boolean val) {
	    askForRendererKill = val;
    }
    
    public boolean getAskForRendererKill() {
        return askForRendererKill;
    }
    
	public void setProcess(Process val) {
		process = val;
	}
	
	public Process getProcess() {
		return process;
	}
	
	public long getMemoryUsed() {
		return memoryUsed;
	}
	
	public void setMemoryUsed(long val) {
		memoryUsed = val;
	}
	
	public int getRenderDuration() {
		return renderDuration;
	}
	
	public void setRenderDuration(int val) {
		renderDuration = val;
	}
	
	public void setMaxOutputNbLines(int val) {
		maxOutputNbLines = val;
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
