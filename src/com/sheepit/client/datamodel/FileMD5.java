package com.sheepit.client.datamodel;

import lombok.Data;
import lombok.ToString;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(strict = false, name = "file") @Data @ToString public class FileMD5 {
	
	@Attribute private String md5;
	
	@Attribute(required = false) private String action;
	
	public FileMD5() {
	}
}
