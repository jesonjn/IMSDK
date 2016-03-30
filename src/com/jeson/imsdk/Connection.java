package com.jeson.imsdk;

import java.io.IOException;
import java.util.Collection;

import javax.sql.ConnectionEventListener;

import com.jeson.imsdk.AGPBMessage.PB_Packet;
import com.jeson.imsdk.exception.WebSDKException;
import com.jeson.imsdk.listener.ConnectionCreatListener;
import com.jeson.imsdk.listener.ConnectionListener;
import com.jeson.imsdk.listener.PacketListener;

public abstract class Connection {
	public ConnectionConfiguration config;
	private Object recvListeners;

	protected Connection(ConnectionConfiguration configuration) {
		config = configuration;
	}

	/**
	 * Returns the configuration used to connect to the server.
	 * 
	 * @return the configuration used to connect to the server.
	 */
	protected ConnectionConfiguration getConfiguration() {
		return config;
	}

	/**
	 * Returns the name of the service provided by the XMPP server for this
	 * connection. This is also called XMPP domain of the connected server.
	 * After authenticating with the server the returned value may be different.
	 * 
	 * @return the name of the service provided by the XMPP server.
	 */
	public String getServiceName() {
		return config.getServiceName();
	}

	/**
	 * Returns the host name of the server where the XMPP server is running.
	 * This would be the IP address of the server or a name that may be resolved
	 * by a DNS server.
	 * 
	 * @return the host name of the server where the XMPP server is running.
	 */
	public String getHost() {
		return config.getHost();
	}

	public int getPort() {
		return config.getPort();
	}

	public abstract String getUser();

	public abstract String getConnectionID();

	public abstract boolean isConnected();

	public abstract boolean isIsreconnecting();

	public abstract boolean isAuthenticated();

	protected boolean isReconnectionAllowed() {
		return config.isReconnectionAllowed();
	}

	// public abstract void connect() throws WebSDKException, IOException;

	public abstract void login(String user, String pwd) throws IOException;

	public abstract void sendPacket(PB_Packet packet);

	public abstract void disconnect();

	public static void addConnectionCreationListener(
			ConnectionEventListener connectionCreationListener) {
	}

	public static void removeConnectionCreationListener(
			ConnectionCreatListener connectionCreationListener) {
	}

	protected static Collection<ConnectionCreatListener> getConnectionCreationListeners() {
		return null;
	}

	public void addConnectionListener(ConnectionListener connectionListener) {
		if (!isConnected()) {
			throw new IllegalStateException("Not connected to server.");
		}
		if (connectionListener == null) {
			return;
		}
	}

	public void removeConnectionListener(ConnectionListener connectionListener) {
	}

	/**
	 * Get the collection of listeners that are interested in connection events.
	 * 
	 * @return a collection of listeners interested on connection events.
	 */
	public Collection<ConnectionListener> getConnectionListeners() {
		return null;
	}

	public synchronized void addPacketListener(PacketListener packetListener) {
		synchronized (recvListeners) {
			if (packetListener == null) {
				throw new NullPointerException("Packet listener is null.");
			}
			// recvListeners.put("", packetListener);
		}
	}

	public void removePacketListener(PacketListener packetListener) {
	}

	public abstract void reconnect();

	public abstract boolean offeLineMessage();

	public abstract boolean creatGroup(String groupName, String... memberID);

	public abstract boolean addGroupMember(String groupName, String... memberID);

	public abstract boolean delGroupMember(String groupID, String... memberID);

	public abstract boolean delGroup(String groupID);

	public abstract boolean sendGroupMessage(String toMessage, String message);

	public abstract boolean sendMessage(String toMessage, String message,
			String fileID);

	public abstract boolean sendOffer(String user, String fileId);

	public abstract boolean sendAnswer(String user, String fileId);

	public abstract boolean sendCandidate(String user, String fileId);
}
