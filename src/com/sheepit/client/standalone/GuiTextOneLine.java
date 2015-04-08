package com.sheepit.client.standalone;

import com.sheepit.client.Client;
import com.sheepit.client.Gui;

public class GuiTextOneLine implements Gui {
	public static final String type = "oneLine";
	
	private int rendered;
	private int remaining;
	private String status;
	private String line;
	
	private Client client;
	
	public GuiTextOneLine() {
		rendered = 0;
		remaining = 0;
		status = "";
		line = "";
	}
	
	@Override
	public void start() {
		if (client != null) {
			client.run();
			client.stop();
		}
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
	
	@Override
	public void setClient(Client cli) {
		client = cli;
	}
	
	@Override
	public Client getClient() {
		return client;
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
