package com.sheepit.client.datamodel;

import lombok.Getter;
import lombok.ToString;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(strict = false, name = "stats") @ToString public class SessionStats {
	
	@Attribute(name = "credits_session") @Getter private int pointsEarnedOnSession;
	
	@Attribute(name = "credits_total") @Getter private int pointsEarnedByUser;
	
	@Attribute(name = "frame_remaining") @Getter private int remainingFrames;
	
	@Attribute(name = "waiting_project") @Getter private int waitingProjects;
	
	@Attribute(name = "renderable_project", required = false) @Getter private int renderableProjects;
	
	@Attribute(name = "connected_machine") @Getter private int connectedMachines;
	
	public SessionStats() {
		
	}
}
