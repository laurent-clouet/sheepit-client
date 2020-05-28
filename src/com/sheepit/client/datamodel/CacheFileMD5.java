package com.sheepit.client.datamodel;

import lombok.Data;
import lombok.ToString;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(strict = false, name = "cache") @Data @ToString public class CacheFileMD5 {
	
	@ElementList(inline = true) private List<FileMD5> md5s;
	
	public CacheFileMD5() {
	}
}
