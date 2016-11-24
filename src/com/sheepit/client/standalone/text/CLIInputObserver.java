package com.sheepit.client.standalone.text;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.sheepit.client.Client;

public class CLIInputObserver implements Runnable {
	private BufferedReader in ;
	private Client client;
	
	public CLIInputObserver(Client client) {
		this.client = client;
	}
	
	
	private List<CLIIInputListener> listeners = new ArrayList<CLIIInputListener>();

    public void addListener(CLIIInputListener toAdd) {
        listeners.add(toAdd);
    }
	
	public void run() {
		in = new BufferedReader(new InputStreamReader(System.in));
		String line = "";

		while (line.equalsIgnoreCase("quit") == false) {
	       try {
		       line = in.readLine();
			} catch (Exception e) {
				// TODO: handle exception
			}
	       for (CLIIInputListener cliil : listeners)
	    	   cliil.commandEntered(client, line);	   
		}
		try {
			in.close();	
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}
}