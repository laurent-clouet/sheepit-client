package com.sheepit.client.standalone.text;

import com.sheepit.client.Client;
import com.sheepit.client.Job;

public class CLIInputActionHandler implements CLIIInputListener {

	@Override
	public void commandEntered(Client client, String command) {
		if(command.equalsIgnoreCase("block")){
			Job job = client.getRenderingJob();
			if(job != null){
				job.block();
			}	
		}
		else if (command.equalsIgnoreCase("resume")){
			client.resume();
		}
		else if(command.equalsIgnoreCase("pause")){
			client.suspend();
		}
		else if(command.equalsIgnoreCase("stop")){
			client.askForStop();
		}
		else if(command.equalsIgnoreCase("cancel")){
			client.cancelStop();
		}
		else if(command.equalsIgnoreCase("quit")){
			client.stop();
			System.exit(0);
		}
		else {
			System.out.println("Unknown command: " + command);
			System.out.println("block:  block project");
			System.out.println("pause:  pause client requesting new jobs");
			System.out.println("resume: resume after client was paused");
			System.out.println("stop:   exit after frame was finished");
			System.out.println("cancel: cancel exit");
			System.out.println("quit:   exit now");
		}
	}

}
