package com.jeson.imsdk;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import android.text.TextUtils;
import android.util.Log;

import com.jeson.imsdk.AGPBMessage.PB_Auth;
import com.jeson.imsdk.AGPBMessage.PB_Business;
import com.jeson.imsdk.AGPBMessage.PB_Business.BusiData;
import com.jeson.imsdk.AGPBMessage.PB_Business.BusiData.Builder;
import com.jeson.imsdk.AGPBMessage.PB_Packet;
import com.jeson.imsdk.AGPBMessage.PB_PacketType;
import com.jeson.imsdk.AGPBMessage.PB_Text;
import com.jeson.imsdk.AGPBMessage.PB_Text.TextData;
import com.jeson.imsdk.WebSDK.OnReceivePacketListener;
import com.jeson.imsdk.exception.WebSDKException;
import com.jeson.imsdk.listener.ConnectionListener;
import com.jeson.imsdk.listener.PBTextListener;
import com.jeson.imsdk.thread.ReaderPBPacket;
import com.jeson.imsdk.thread.WritePBPacket;
import com.jeson.imsdk.video.VideoManager;

public class WebConnection extends Connection {
	Socket mSocketClient;
	WritePBPacket writerThread;
	ReaderPBPacket readerThread;
	private String mUser;
	private String mPwd;
	private String mUserToken;
	volatile boolean done = false;
	Connection mConnection;
	volatile boolean connected = false;
	volatile boolean socketClosed = false;
	volatile boolean isLogin = false;
	ConnectionConfiguration mConfiguration;
	public static ConnectionListener mConnectionListener;
	public static volatile LinkedList<PB_Packet> packetLinked = new LinkedList<AGPBMessage.PB_Packet>();
	private WebSDK mWebSDK;
	private Thread mThread;
	

	protected WebConnection(ConnectionConfiguration configuration,
			ConnectionListener connectionListener, WebSDK sdk) {
		super(configuration);
		mConnectionListener = connectionListener;
		mConfiguration = configuration;
		mWebSDK = sdk;
		try {
			initSocket();
		} catch (WebSDKException e) {
			e.printStackTrace();
			connectionListener.connectionClosedOnError(e);
		} catch (IOException e) {
			e.printStackTrace();
			connectionListener.connectionClosedOnError(e);
		}
	}

	public boolean creatGroup(String groupName, String... memberID) {

		PB_Business.Builder authBuilder = PB_Business.newBuilder();
		authBuilder.setMethod(1);
		authBuilder.setUserToken(mUserToken);
		Builder builder = BusiData.newBuilder();
		builder.setBdkey("create");
		builder.setBdvalue(groupName);
		for (String member : memberID) {
			builder.setBdkey("people");
			builder.setBdvalue(member);
		}
		authBuilder.addBusiData(builder.build());
		return writerThread.sendPacket(authBuilder.build());
	}

	public boolean offeLineMessage() {

		PB_Business.Builder authBuilder = PB_Business.newBuilder();
		authBuilder.setMethod(2);
		authBuilder.setUserToken(mUserToken);
		// authBuilder.
		// Builder builder = BusiData.newBuilder();
		// builder.setBdkey("create");
		// builder.setBdvalue(groupName);
		// builder.setBdkey("people");
		// builder.setBdvalue(memberID);
		// authBuilder.addBusiData(builder.build());
		return writerThread.sendPacket(authBuilder.build());
	}

	public boolean sendCandidate(String toMessage, String fileID) {
		PB_Text.Builder builder = PB_Text.newBuilder();
		builder.setMethod(5);
		builder.setUserToken(mUserToken);
		builder.setMessageTo(toMessage + "_" + mConfiguration.getDemain());
		builder.addTextData(TextData.newBuilder().setTdkey("candidate")
				.setTdvalue(fileID));
		builder.setSendTime(System.currentTimeMillis() + "");
		builder.setMessageId(System.currentTimeMillis() + "");
		builder.setMessageFrom(mUser + "_" + mConfiguration.getDemain());
		return writerThread.sendPacket(builder.build());
	}

	public boolean addGroupMember(String groupName, String... memberID) {

		PB_Business.Builder authBuilder = PB_Business.newBuilder();
		authBuilder.setMethod(5);
		authBuilder.setUserToken(mUserToken);
		Builder builder = BusiData.newBuilder();
		builder.setBdkey("group");
		builder.setBdvalue(groupName);
		for (String member : memberID) {
			builder.setBdkey("people");
			builder.setBdvalue(member);
		}
		authBuilder.addBusiData(builder.build());
		return writerThread.sendPacket(authBuilder.build());
	}

	public boolean delGroupMember(String groupID, String... memberID) {

		PB_Business.Builder authBuilder = PB_Business.newBuilder();
		authBuilder.setMethod(6);
		authBuilder.setUserToken(mUserToken);

		Builder builder = BusiData.newBuilder();
		builder.setBdkey("group");
		builder.setBdvalue(groupID);
		for (String member : memberID) {
			builder.setBdkey("people");
			builder.setBdvalue(member);
		}
		authBuilder.addBusiData(builder.build());
		return writerThread.sendPacket(authBuilder.build());
	}

	public boolean delGroup(String groupID) {

		PB_Business.Builder authBuilder = PB_Business.newBuilder();
		authBuilder.setMethod(7);
		authBuilder.setUserToken(mUserToken);
		Builder builder = BusiData.newBuilder();
		builder.setBdkey("group");
		builder.setBdvalue(groupID);
		authBuilder.addBusiData(builder.build());
		return writerThread.sendPacket(authBuilder.build());
	}

	public boolean sendGroupMessage(String toMessage, String message) {

		PB_Text.Builder builder = PB_Text.newBuilder();
		builder.setMethod(2);
		builder.setUserToken(mUserToken);
		builder.setMessageTo(toMessage + "_" + mConfiguration.getDemain());
		builder.setContent(message);
		builder.setSendTime(System.currentTimeMillis() + "");
		builder.setMessageId(System.currentTimeMillis() + "");
		builder.setMessageFrom(mUser + "_" + mConfiguration.getDemain());
		return writerThread.sendPacket(builder.build());
	}

	public boolean sendMessage(String toMessage, String message, String fileID) {
		if (null == message) {
			message = "";
		}

		PB_Text.Builder builder = PB_Text.newBuilder();
		builder.setMethod(1);
		builder.setUserToken(mUserToken);
		builder.setMessageTo(toMessage + "_" + mConfiguration.getDemain());
		builder.setContent(message);
		if (!TextUtils.isEmpty(fileID)) {
			builder.addTextData(TextData.newBuilder().setTdkey("voice")
					.setTdvalue(fileID));
		}
		builder.setSendTime(System.currentTimeMillis() + "");
		builder.setMessageId(System.currentTimeMillis() + "");
		builder.setMessageFrom(mUser + "_" + mConfiguration.getDemain());
		return writerThread.sendPacket(builder.build());
	}

	public boolean sendOffer(String toMessage, String fileID) {

		PB_Text.Builder builder = PB_Text.newBuilder();
		builder.setMethod(4);
		builder.setUserToken(mUserToken);
		builder.setMessageTo(toMessage + "_" + mConfiguration.getDemain());
		builder.addTextData(TextData.newBuilder().setTdkey("offer")
				.setTdvalue(fileID));
		builder.setSendTime(System.currentTimeMillis() + "");
		builder.setMessageId(System.currentTimeMillis() + "");
		builder.setMessageFrom(mUser + "_" + mConfiguration.getDemain());
		return writerThread.sendPacket(builder.build());
	}

	public boolean sendAnswer(String toMessage, String fileID) {

		PB_Text.Builder builder = PB_Text.newBuilder();
		builder.setMethod(4);
		builder.setUserToken(mUserToken);
		builder.setMessageTo(toMessage + "_" + mConfiguration.getDemain());
		builder.addTextData(TextData.newBuilder().setTdkey("answer")
				.setTdvalue(fileID));
		builder.setSendTime(System.currentTimeMillis() + "");
		builder.setMessageId(System.currentTimeMillis() + "");
		builder.setMessageFrom(mUser + "_" + mConfiguration.getDemain());
		return writerThread.sendPacket(builder.build());
	}

	public void login(String user, String pwd) {
		Log.e("login", "  is login  connet is  :" + connected);
		if (isConnected()) {
			PB_Packet.Builder packetBuilder = PB_Packet.newBuilder();
			packetBuilder.setVersion(1);
			packetBuilder.setType(PB_PacketType.TYPE_AUTH);

			PB_Auth.Builder authBuilder = PB_Auth.newBuilder();
			authBuilder.setMethod(1);// 1為登錄，2為登出
			authBuilder.setUsername(user);
			authBuilder.setPassword(pwd);
			authBuilder.setDomain(mConfiguration.getDemain());
			// 0為成功，1為登錄失敗，2為未登錄，3為重複登錄
			mUser = user;
			mPwd = pwd;
			packetBuilder.setAuth(authBuilder.build());
			Log.i("web--IM", "-------send-----" + writerThread);
			try {
				writerThread.sendPacket(packetBuilder.build());
			} catch (IOException e) {
				e.printStackTrace();
				mConnectionListener.connectionClosedOnError(e);
			}
		} else {
			reconnect();

		}
	}

	public boolean logout() throws IOException {
		PB_Packet.Builder packetBuilder = PB_Packet.newBuilder();
		packetBuilder.setVersion(1);
		packetBuilder.setType(PB_PacketType.TYPE_AUTH);

		PB_Auth.Builder authBuilder = PB_Auth.newBuilder();
		authBuilder.setMethod(2);// 1為登錄，2為登出
		authBuilder.setUsername(mUser);
		authBuilder.setPassword(mPwd);
		authBuilder.setDomain(mConfiguration.getDemain());
		// 0為成功，1為登錄失敗，2為未登錄，3為重複登錄
		packetBuilder.setAuth(authBuilder.build());
		return writerThread.sendPacket(packetBuilder.build());
	}

	private void connect() throws Exception {

		connected = false;
		try {
			if (mSocketClient == null) {
				initSocket();
			}
			if (!mSocketClient.isConnected()) {
				mSocketClient.connect(new InetSocketAddress(config.getHost(),
						config.getPort()), 10000);
			}
			if (writerThread == null)
				writerThread = new WritePBPacket(mSocketClient, this);
			if (readerThread == null || !readerThread.isAlive())
				readerThread = new ReaderPBPacket(mSocketClient,
						new OnReceivePacketListener() {
							@Override
							public boolean onReceivePacketListener(
									PB_Packet packet) {
								if (null != mWebSDK.getMPacketListener()) {
									if (mWebSDK.getMPacketListener()
											.proccessPacket(packet)) {
										return true;
									}
								}
								/*** 登陆认证包 **/
								if (packet.hasAuth()) {
									mUserToken = packet.getAuth()
											.getUserToken();
									isLogin = true;

									mWebSDK.sendLoginListener(mUserToken,
											mUserToken, mUserToken, true);
									mWebSDK.saveUserInfo(null, null, mUserToken);
								} else if (packet.hasText()) {
									/*** 文本包 **/
									if ((packet.getText().getMethod() == 4 || packet
											.getText().getMethod() == 5)) {

										if (VideoManager.mSystemPacketListenrMap
												.containsKey("video")) {
											return VideoManager.mSystemPacketListenrMap
													.get("video")
													.proccessPacket(packet);
										}

									}

									Iterator<Entry<Integer, PBTextListener>> chatItertor = mWebSDK
											.getPBTextListener().entrySet()
											.iterator();
									while (chatItertor.hasNext()) {

										if (chatItertor
												.next()
												.getValue()
												.proccessMessage(
														packet.getText())) {
											Log.i("webIM",
													"-----packet----------"
															+ packet.hasText());
											return true;
										}
									}

								} else if (packet.hasSystem()) {
									/*** 系统包 **/
								} else if (packet.hasBusiness()) {
									/*** 系统包 **/
								} else if (packet.hasDelivery()) {
									/*** 状态包 **/
								}

								Log.i("webIM", "-----packet----d------"
										+ packet.toString());
								return true;
							}
						});
			Log.e("TAG",
					" (mThread == null || !mThread.isAlive()|| !readerThread.isAlive() || !readerThread.isStop)::"
							+ (mThread == null || !mThread.isAlive()
									|| !readerThread.isAlive() || !readerThread.isStop));
			if (mThread == null || !mThread.isAlive()
					|| !readerThread.isAlive() || !readerThread.isStop) {
				mThread = new Thread() {

					@Override
					public void run() {
						readerThread.start();
					}

				};
				mThread.setDaemon(true);
				mThread.setName("receive");
				mThread.start();
			}
			Log.i("web--IM", "-------init-----" + mThread.isAlive() + "----"
					+ readerThread.isStop + "------" + readerThread.isAlive());
			Log.i("web--IM", "-------init-----" + writerThread);
			Log.i("web--IM", "-------init-----" + readerThread);
			connected = true;
		} catch (Exception e) {
			e.printStackTrace();
			connected = false;
			throw e;
		} finally {
			// done = false;
		}
		// }
	}

	private void initSocket() throws WebSDKException, IOException {
		mSocketClient = new Socket();
		mSocketClient.setReuseAddress(true);
		mSocketClient.setKeepAlive(true);
	}

	@Override
	public String getUser() {
		return mUserToken;
	}

	@Override
	public String getConnectionID() {
		return null;
	}

	@Override
	public boolean isConnected() {
		return connected && !mSocketClient.isClosed()
				&& mSocketClient.isConnected() && mThread.isAlive()
				&& readerThread.isAlive() && readerThread.isStop;
	}

	@Override
	public boolean isIsreconnecting() {
		return false;
	}

	@Override
	public boolean isAuthenticated() {
		return isLogin;
	}

	@Override
	public void sendPacket(PB_Packet packet) {

	}

	@Override
	public void reconnect() {

		if (!done) {
			done = true;
			connected = false;
			isLogin = false;
			Thread connect = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						Log.i("reconnect--", "----connect-------");
						connect();
						Log.i("reconnect--", "----islogin-------" + isLogin);

						if (!isLogin) {
							login(mWebSDK.getUserName(), mWebSDK.getPassword());
						} else {
							sendMessage("", "", null);
						}
					} catch (Exception e) {
						e.printStackTrace();
						try {
							mSocketClient.close();
						} catch (IOException e1) {
							e1.printStackTrace();

						}
						mConnectionListener.connectionClosedOnError(e);
					} finally {
						done = false;
					}
				}
			});
			connect.setDaemon(true);
			connect.setName("reconnection");
			connect.start();
		} else {
			Log.i("user--", "------------");
		}

	}

	@Override
	public void disconnect() {
		isLogin = false;
		connected = false;
		if (mSocketClient != null) {
			try {
				mSocketClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		mSocketClient = null;
		readerThread.isStop = false;
		writerThread = null;
		readerThread = null;
		Log.e("断开连连", "断开连接");
	}

}
