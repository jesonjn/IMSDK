package com.jeson.imsdk.listener;

import com.jeson.imsdk.AGPBMessage.PB_Text;
import com.jeson.imsdk.AGPBMessage.PB_Text.TextData;

public interface ChatMessageListener {

	public boolean proccessMessage(PB_Text packet);

	public boolean proccessVideoOffer(String user, TextData data);

}
