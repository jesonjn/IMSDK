package com.jeson.imsdk.thread;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import android.util.Log;

import com.jeson.imsdk.WebConnection;
import com.jeson.imsdk.AGPBMessage.PB_Packet;
import com.jeson.imsdk.WebSDK.OnReceivePacketListener;

public class ReaderPBPacket {
	public Socket socket;
	InputStream reader;
	OnReceivePacketListener mListener;
	boolean isAlive = false;
	public boolean isStop = true;

	public boolean isAlive() {
		return isAlive;
	}

	public ReaderPBPacket(Socket socket, OnReceivePacketListener listener) {
		this.socket = socket;
		mListener = listener;
	}

	public void start() {
		isAlive = true;
		while (true && isStop) {
			try {
				Log.i("web--IM",
						"-------recive-----"
								+ (socket == null
										|| !(socket.isConnected() && !socket
												.isClosed()) || socket
											.isInputShutdown()));
				if (socket == null
						|| !(socket.isConnected() && !socket.isClosed())
						|| socket.isInputShutdown()) {
					WebConnection.mConnectionListener
							.connectionClosedOnError(new Exception(
									"socket  is close"));
					break;
				}
				Log.i("web--IM", "-------recive-dddd----reader-----" );
				if (reader == null)
					reader = socket.getInputStream();

				byte len[] = new byte[1024];
				int count = reader.read(len);
				if (count < 0) {
					Log.i("web--IM", "-------recive-d----" + count);
					continue;
				}
				final byte[] temp = new byte[count];
				for (int i = 0; i < count; i++) {
					temp[i] = len[i];
				}
				try {
					final PB_Packet packet = PB_Packet.parseFrom(temp);

					mListener.onReceivePacketListener(packet);
					Log.i("webim", "endMessage::::"
							+ packet.getAuth().toString());

				} catch (Exception e) {
					if (reader != null)
						reader.close();
					reader = null;
					try {
						socket.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					e.printStackTrace();
					isAlive = false;
					WebConnection.mConnectionListener
							.connectionClosedOnError(e);
				}
			} catch (Exception e) {
				e.printStackTrace();
				if (reader != null)
					try {
						reader.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				reader = null;
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				isAlive = false;
				WebConnection.mConnectionListener.connectionClosedOnError(e);
				break;
			} finally {

			}

		}
		isAlive = false;
	}
}
