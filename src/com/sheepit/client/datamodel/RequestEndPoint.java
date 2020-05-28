package com.sheepit.client.datamodel;

import lombok.Getter;
import lombok.ToString;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(strict = false, name = "request") @ToString public class RequestEndPoint {
	
	@Attribute @Getter private String type;
	
	@Attribute @Getter private String path;
	
	@Attribute(name = "max-period", required = false) @Getter private int maxPeriod;
	
	public RequestEndPoint() {
	}
}
