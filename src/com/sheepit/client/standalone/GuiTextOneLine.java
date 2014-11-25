package com.sheepit.client.standalone;

import com.sheepit.client.Gui;

public class GuiTextOneLine implements Gui {
	private int rendered;
	private int remaining;
	private String status;
	private String line;
	
	public GuiTextOneLine() {
		rendered = 0;
		remaining = 0;
		status = "";
		line = "";
	}
	
	@Override
	public void start() {
	}
	
	@Override
	public void stop() {
	}
	
	@Override
	public void status(String msg_) {
		status = msg_;
		updateLine();
	}
	
	@Override
	public void error(String msg_) {
		status = "Error " + msg_;
		updateLine();
	}
	
	@Override
	public void AddFrameRendered() {
		rendered += 1;
		updateLine();
	}
	
	@Override
	public void framesRemaining(int n_) {
		remaining = n_;
		updateLine();
	}
	
	private void updateLine() {
		int charToRemove = line.length();
		
		System.out.print("\r");
		line = String.format("Frame rendered: %d remaining: %d | %s", rendered, remaining, status);
		System.out.print(line);
		for (int i = line.length(); i <= charToRemove; i++) {
			System.out.print(" ");
		}
	}
}
