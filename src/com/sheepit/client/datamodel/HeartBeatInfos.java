package com.sheepit.client.datamodel;

import lombok.Getter;
import lombok.ToString;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(strict = false, name = "keepmealive") @ToString public class HeartBeatInfos {
	@Attribute @Getter private int status;
}
