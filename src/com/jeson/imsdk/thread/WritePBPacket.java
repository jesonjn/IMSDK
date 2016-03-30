package com.jeson.imsdk.thread;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.LinkedList;

import android.util.Log;

import com.jeson.imsdk.AGPBMessage;
import com.jeson.imsdk.Connection;
import com.jeson.imsdk.WebConnection;
import com.jeson.imsdk.AGPBMessage.PB_Business;
import com.jeson.imsdk.AGPBMessage.PB_DeliveryState;
import com.jeson.imsdk.AGPBMessage.PB_Packet;
import com.jeson.imsdk.AGPBMessage.PB_PacketType;
import com.jeson.imsdk.AGPBMessage.PB_System;
import com.jeson.imsdk.AGPBMessage.PB_Text;

public class WritePBPacket {
	private Socket socket;
	private DataOutputStream out;
	InputStream reader;

	Thread sendThread;
	Connection connection;

	public WritePBPacket(Socket socket, Connection con) {
		this.socket = socket;
		connection = con;
	}

	public boolean sendPacket(PB_Business packet) {
		PB_Packet.Builder packetBuilder = PB_Packet.newBuilder();
		packetBuilder.setVersion(1);
		packetBuilder.setType(PB_PacketType.TYPE_BUSINESS);
		packetBuilder.setBusiness(packet);
		try {
			return sendPacket(packetBuilder.build());
		} catch (IOException e) {
			e.printStackTrace();
			WebConnection.mConnectionListener.connectionClosedOnError(e);
			return false;
		}
	}

	public boolean sendPacket(PB_System packet) {
		PB_Packet.Builder packetBuilder = PB_Packet.newBuilder();
		packetBuilder.setVersion(1);
		packetBuilder.setType(PB_PacketType.TYPE_SYSTEM);
		packetBuilder.setSystem(packet);
		try {
			return sendPacket(packetBuilder.build());
		} catch (IOException e) {
			e.printStackTrace();
			WebConnection.mConnectionListener.connectionClosedOnError(e);
			return false;
		}
	}

	public boolean sendPacket(PB_Text packet) {
		PB_Packet.Builder packetBuilder = PB_Packet.newBuilder();
		packetBuilder.setVersion(1);
		packetBuilder.setType(PB_PacketType.TYPE_TEXT);
		packetBuilder.setText(packet);
		try {
			return sendPacket(packetBuilder.build());
		} catch (IOException e) {
			e.printStackTrace();
			WebConnection.mConnectionListener.connectionClosedOnError(e);
			return false;
		}
	}

	public boolean sendPacket(PB_DeliveryState packet) {
		PB_Packet.Builder packetBuilder = PB_Packet.newBuilder();
		packetBuilder.setVersion(1);
		packetBuilder.setType(PB_PacketType.TYPE_DELIVERY);
		packetBuilder.setDelivery(packet);
		try {
			return sendPacket(packetBuilder.build());
		} catch (IOException e) {
			e.printStackTrace();
			WebConnection.mConnectionListener.connectionClosedOnError(e);
			return false;
		}
	}

	public boolean sendPacket(PB_Packet packet) throws IOException {
		Log.i("webIM", "test---发送--------im" + socket);
		if (out == null) {
			out = new DataOutputStream(socket.getOutputStream());
		}
		WebConnection.packetLinked.add(packet);
		if (connection.isConnected()) {
			if (sendThread != null) {
				synchronized (sendThread) {
					if (!sendThread.isAlive()) {
						sendThread = new Thread(new Runnable() {

							@Override
							public void run() {
								if (socket == null
										|| !(socket.isConnected() && !socket
												.isClosed())
										|| socket.isOutputShutdown()) {
									WebConnection.mConnectionListener
											.connectionClosedOnError(new Exception(
													"socket  is close"));
									return;
								}
								while (!WebConnection.packetLinked.isEmpty()) {
									PB_Packet packet = WebConnection.packetLinked
											.poll();
									Log.i("webIM", "test---发送--------im"
											+ packet.getType());

									try {
										out.write(packet.toByteArray());

									} catch (Exception e) {
										e.printStackTrace();
										if (out != null) {
											try {
												out.close();
											} catch (IOException e1) {
												// TODO Auto-generated catch
												// block
												e1.printStackTrace();
											}
											out = null;
										}
										WebConnection.mConnectionListener
												.connectionClosedOnError(e);
									}
									try {
										Thread.sleep(500);
									} catch (InterruptedException e1) {
										e1.printStackTrace();
									}
								}
							}
						});
						;
						try {
							sendThread.join();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (sendThread.isDaemon()) {
							sendThread.setDaemon(true);
							sendThread.setName("send");
						}
						sendThread.start();
					}
				}
			} else {

				sendThread = new Thread(new Runnable() {

					@Override
					public void run() {
						if (socket == null
								|| !(socket.isConnected() && !socket.isClosed())
								|| socket.isOutputShutdown()) {
							WebConnection.mConnectionListener
									.connectionClosedOnError(new Exception(
											"socket  is close"));
							return;
						}

						while (!WebConnection.packetLinked.isEmpty()) {
							PB_Packet packet = WebConnection.packetLinked
									.poll();
							Log.i("webIM",
									"test---发送--------im" + packet.getType());
							System.out.println("-----" + packet.toByteArray());

							try {
								out.write(packet.toByteArray());
								Log.i("web--IM", "-------sendPacket-----");

							} catch (Exception e) {
								e.printStackTrace();
								if (out != null) {
									try {
										out.close();
									} catch (IOException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}
									out = null;
								}
								WebConnection.mConnectionListener
										.connectionClosedOnError(e);
							}
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e1) {
								e1.printStackTrace();
							}
						}
					}
				});
				;
				try {
					sendThread.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (sendThread.isDaemon()) {
					sendThread.setDaemon(true);
					sendThread.setName("send");
				}
				sendThread.start();

			}
		}
		return true;
	}

}
