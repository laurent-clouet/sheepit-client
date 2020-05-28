package com.sheepit.client.datamodel;

import lombok.Getter;
import lombok.ToString;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(strict = false, name = "config") @ToString public class ServerConfig {
	
	@Attribute @Getter private int status;
	
	@Attribute(required = false) @Getter private String publickey;
	
	@ElementList(name = "request", inline = true, required = false) private List<RequestEndPoint> requestEndPoints;
	
	public ServerConfig() {
	}
	
	public RequestEndPoint getRequestEndPoint(String type) {
		if (requestEndPoints != null) {
			for (RequestEndPoint endPoint : requestEndPoints) {
				if (type.equals(endPoint.getType())) {
					return endPoint;
				}
			}
		}
		return null;
	}
}
