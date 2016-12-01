package com.sheepit.client.standalone.text;

import javax.swing.plaf.synth.SynthSeparatorUI;

import com.sheepit.client.Client;
import com.sheepit.client.Configuration;
import com.sheepit.client.Job;
import com.sun.scenario.effect.impl.prism.PrImage;

public class CLIInputActionHandler implements CLIIInputListener {

	@Override
	public void commandEntered(Client client, String command) {
		int priorityLength = "priority".length();
		int blockTimeLength = "block_time".length();
		
		//prevent Null Pointer at next step
		if(command == null){
			return;
		}
		if(client  == null){
			return;
		}
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
		} else if((command.length() > priorityLength ) && 
					( command.substring(0, priorityLength).equalsIgnoreCase("priority"))	){
				changePriority(client, command.substring(priorityLength));
				
			}
		else if((command.length() > blockTimeLength ) && 
				( command.substring(0, blockTimeLength).equalsIgnoreCase("block_time"))	){
			changeBlockTime(client, command.substring(blockTimeLength));
			
		}
		else {
			System.out.println("Unknown command: " + command);
			System.out.println("block:  block project");
			System.out.println("block_time n: automated block projects needing more than n minutes to render. 0 disables");
			System.out.println("pause:  pause client requesting new jobs");
			System.out.println("resume: resume after client was paused");
			System.out.println("stop:   exit after frame was finished");
			System.out.println("cancel: cancel exit");
			System.out.println("quit:   exit now");
			System.out.println("priority <n>: set the priority for the next renderjob");
			
		}
	}

	void changePriority(Client client, String newPriority){
		Configuration config = client.getConfiguration();
		if(config != null){
			try {
				config.setUsePriority(Integer.parseInt(newPriority.trim()));
			}
			catch(NumberFormatException e){
				System.out.println("Invalid priority: " + newPriority);
			}
		}
		
	}
	
	void changeBlockTime(Client client, String newBlockTime){
		Configuration config = client.getConfiguration();
		if(config != null){
			try {
				config.setBlockTime(Integer.parseInt(newBlockTime.trim()));
			}
			catch(NumberFormatException e){
				System.out.println("Invalid block_time: " + newBlockTime);
			}
		}
		
	}
	
}
