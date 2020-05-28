package com.sheepit.client.datamodel;

import lombok.Getter;
import lombok.ToString;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(strict = false, name = "renderer") @ToString public class RendererInfos {
	
	@Attribute(name = "md5") @Getter private String md5;
	
	@Attribute(name = "commandline") @Getter private String commandline;
	
	@Attribute(name = "update_method") @Getter private String update_method;
	
	public RendererInfos() {
		
	}
}
