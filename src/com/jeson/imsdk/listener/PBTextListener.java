package com.jeson.imsdk.listener;

import com.jeson.imsdk.AGPBMessage.PB_Text;

public interface PBTextListener {
	public boolean proccessMessage(PB_Text packet);
}
