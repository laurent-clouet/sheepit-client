package com.sheepit.client.datamodel;

import lombok.Getter;
import lombok.ToString;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(strict = false, name = "job")
@ToString
public class RenderTask {
	
	@Attribute(name = "id")
	@Getter
	private String id;
	
	@Attribute(name = "use_gpu")
	@Getter
	private int useGpu;
	
	@Attribute(name = "archive_md5")
	@Getter
	private String archive_md5;
	
	@Attribute(name = "path")
	@Getter
	private String path;
	
	@Attribute(name = "frame")
	@Getter
	private String frame;
	
	@Attribute(name = "synchronous_upload")
	@Getter
	private String synchronous_upload;
	
	@Attribute(name = "extras")
	@Getter
	private String extras;

	@Attribute(name = "validation_url")
	@Getter
	private String validationUrl;
	
	@Attribute(name = "name")
	@Getter
	private String name;
	
	@Attribute(name = "password")
	@Getter
	private String password;
	
	@Element(name = "renderer")
	@Getter
	private RendererInfos rendererInfos;
	
	@Element(name = "script", data = true)
	@Getter
	private String script;
	
	public RenderTask() {
	
	}
}
