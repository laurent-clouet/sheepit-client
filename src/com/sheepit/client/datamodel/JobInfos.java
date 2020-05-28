package com.sheepit.client.datamodel;

import lombok.Getter;
import lombok.ToString;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(strict = false, name = "jobrequest") @ToString public class JobInfos {
	
	@Attribute @Getter private int status;
	
	@Element(name = "stats", required = false) @Getter private SessionStats sessionStats;
	
	@Element(name = "job", required = false) @Getter() private RenderTask renderTask;
	
	@ElementList(name = "file", inline = true, required = false) @Getter private List<FileMD5> fileMD5s;
	
	public JobInfos() {
	}
}
