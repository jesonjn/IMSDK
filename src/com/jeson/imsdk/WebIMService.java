package com.jeson.imsdk;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;

import com.jeson.imsdk.AGPBMessage.PB_Text;
import com.jeson.imsdk.listener.ChatMessageListener;
import com.jeson.imsdk.listener.PBTextListener;

public class WebIMService extends Service {
	private WebSDK mSdk;
	private SharedPreferences mSPreferences;
	private static String FILENAME = "setinng";
	private WebSDKIBinner binner;
	public static final String action = "com.jeson.imsdk.webimservice";
	public Map<String, ChatMessageListener> chatMessageListener = new HashMap<String, ChatMessageListener>();
	private Timer mTimer;
	private BroadcastReceiver mNetReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

				ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo netInfo = connectivityManager
						.getActiveNetworkInfo();
				if (netInfo != null && netInfo.isAvailable()) {
					String name = netInfo.getTypeName();

					if (netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
						// ///WiFi网络
						mSdk.connect();

					} else if (netInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
						// ///有线网络
						mSdk.connect();
					} else if (netInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
						// ///////3g网络
						mSdk.connect();
					}
				} else {
					// //////网络断开
					mSdk.disconnection();

				}
			}

		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		mSPreferences = getSharedPreferences(FILENAME,
				Context.MODE_WORLD_WRITEABLE);
		IntentFilter mFilter = new IntentFilter();
		mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(mNetReceiver, mFilter);
		binner = new WebSDKIBinner();
		mSdk = WebSDK.getWebSDK(this);
		mSdk.connect();
		mSdk.addPBTextListener(new PBTextListener() {

			@Override
			public boolean proccessMessage(PB_Text packet) {

				if (chatMessageListener.containsKey(packet.getMessageFrom())) {
					if (packet.getMethod() == 4 || packet.getMethod() == 5) {
						return chatMessageListener.get(packet.getMessageFrom())
								.proccessVideoOffer(
										packet.getMessageFrom().replace(
												"_" + mSdk.getDemain(), ""),
										packet.getTextData(0));
					} else {
						return chatMessageListener.get(packet.getMessageFrom())
								.proccessMessage(packet);
					}
				} else {
					if (chatMessageListener.containsKey("gole")) {
						if (packet.getMethod() == 4 || packet.getMethod() == 5) {
							return chatMessageListener
									.get("gole")
									.proccessVideoOffer(
											packet.getMessageFrom().replace(
													"_" + mSdk.getDemain(), ""),
											packet.getTextData(0));
						} else {
							return chatMessageListener.get("gole")
									.proccessMessage(packet);
						}
					}
				}
				return false;
			}

		});
		mTimer = new Timer();
		mTimer.schedule(new TimerTask() {

			@Override
			public void run() {

				ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo netInfo = connectivityManager
						.getActiveNetworkInfo();
				if (netInfo != null && netInfo.isAvailable()) {
					String name = netInfo.getTypeName();

					if (netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
						// ///WiFi网络
						if (!mSdk.isconnect()) {
							mSdk.connect();
						} else {
							mSdk.login(mSdk.getUserName(), mSdk.getPassword(),
									new IloginListener() {

										@Override
										public boolean loginSuccess() {

											return true;
										}

										@Override
										public boolean loginFail(String error) {

											return true;
										}

									});
						}
					} else if (netInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
						// ///有线网络
						if (!mSdk.isconnect()) {
							mSdk.connect();
						} else {
							mSdk.login(mSdk.getUserName(), mSdk.getPassword(),
									new IloginListener() {

										@Override
										public boolean loginSuccess() {

											return true;
										}

										@Override
										public boolean loginFail(String error) {

											return true;
										}

									});
						}
					} else if (netInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
						// ///////3g网络
						if (!mSdk.isconnect()) {
							mSdk.connect();
						} else {
							mSdk.login(mSdk.getUserName(), mSdk.getPassword(),
									new IloginListener() {

										@Override
										public boolean loginSuccess() {

											return true;
										}

										@Override
										public boolean loginFail(String error) {

											return true;
										}

									});
						}
					}
				} else {
					// //////网络断开

				}

			}
		}, 10000);
	}

	public String getSetting() {
		return mSPreferences.getString("net_set", "1");
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mNetReceiver);
		super.onDestroy();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binner;
	}

	public interface IloginListener {
		public boolean loginSuccess();

		public boolean loginFail(String error);
	}

	public class WebSDKIBinner extends Binder {

		/**
		 * 用户登陆
		 * 
		 * @param username
		 * @param pwd
		 */
		public void login(String username, String pwd, IloginListener listener) {
			mSdk.login(username, pwd, listener);
		}

		/**
		 * 发送文本消息
		 * 
		 * @param username
		 * @param message
		 */
		public void sendMessage(String username, String message) {
			mSdk.sendMessage(username, message);
		}

		/**
		 * 发送文件
		 * 
		 * @param username
		 * @param message
		 * @param filePath
		 */
		public void sendFile(String username, String message, String filePath) {
			mSdk.sendFileMessage(username, filePath, message);
		}

		/**
		 * 发送群组消息
		 * 
		 * @param username
		 * @param message
		 * @param filePath
		 */
		public void sendGroupMessage(String toMessage, String message) {
			mSdk.sendGroupMessage(toMessage, message);
		}

		/**
		 * 创建群组
		 * 
		 * @param groupID
		 * @param member
		 */
		public void creatGroup(String groupID, String... member) {
			mSdk.creatGroup(groupID, member);
		}

		/**
		 * 添加群成员
		 * 
		 * @param group
		 * @param member
		 */
		public void addGroupMember(String group, String... member) {
			mSdk.addGroupMember(group, member);
		}

		/**
		 * 删除群成员
		 * 
		 * @param group
		 * @param message
		 */
		public void delGroupMember(String group, String... message) {
			mSdk.delGroupMember(group, message);
		}

		/**
		 * 删除群
		 * 
		 * @param group
		 */
		public void delGroup(String group) {
			mSdk.delGroup(group);
		}

		public void addChatMessage(String toUser,
				ChatMessageListener messageListener) {
			chatMessageListener
					.put(toUser + "_" + mSdk.domain, messageListener);
		}

		public void setGoleChatMessage(ChatMessageListener messageListener) {
			chatMessageListener.put("gole", messageListener);
		}

		public WebSDK getWebSDK() {
			return mSdk;
		}
	}
}
