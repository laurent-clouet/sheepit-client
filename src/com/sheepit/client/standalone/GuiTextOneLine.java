package com.sheepit.client.standalone;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

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
		MessageFormat formatter = new MessageFormat(ResourceBundle.getBundle("LogResources", client != null ? client.getConfiguration().getLocale() : Locale.getDefault()).getString("GenericError"), client != null ? client.getConfiguration().getLocale() : Locale.getDefault());
		status = formatter.format(new Object[]{msg_});
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
		MessageFormat formatter = new MessageFormat(ResourceBundle.getBundle("GUIResources", client != null ? client.getConfiguration().getLocale() : Locale.getDefault()).getString("UpdateLine"), client != null ? client.getConfiguration().getLocale() : Locale.getDefault());
		line = formatter.format(new Object[]{rendered,remaining,status})
		System.out.print(line);
		for (int i = line.length(); i <= charToRemove; i++) {
			System.out.print(" ");
		}
	}
}
