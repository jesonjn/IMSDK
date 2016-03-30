package com.jeson.imsdk.listener;

import com.jeson.imsdk.AGPBMessage.PB_Packet;

public interface PacketListener {
     public boolean  proccessPacket(PB_Packet  packet);
}
