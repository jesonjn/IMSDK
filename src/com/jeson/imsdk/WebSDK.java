package com.jeson.imsdk;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

import com.jeson.imsdk.AGPBMessage.PB_Packet;
import com.jeson.imsdk.WebIMService.IloginListener;
import com.jeson.imsdk.file.UploadFile;
import com.jeson.imsdk.file.UploadFile.OnUploadSuccessListener;
import com.jeson.imsdk.listener.ConnectionListener;
import com.jeson.imsdk.listener.PBTextListener;
import com.jeson.imsdk.listener.PacketListener;
import com.jeson.xutils.util.FileService;

/**
 * 通讯 sdk
 * 
 * @author jiangneng
 * 
 */
public class WebSDK {
	private Connection mConnection;
	private static Context mContext;
	private static SharedPreferences mFileSharePre;
	private static String fileName = "WEB_IM";
	public static String domain = "yu";
	private IloginListener mLoginListener;
	ConnectionListener mConnectionListener;
	FileService mFileService;
	private PacketListener mPacketListener;
	private static WebSDK mSDK;
	private HashSet<ConnectionListener> mConnetionSet = new HashSet<ConnectionListener>();
	private HashMap<Integer, PBTextListener> mTextMap = new HashMap<Integer, PBTextListener>();

	public interface OnReceivePacketListener {
		public boolean onReceivePacketListener(PB_Packet packet);
	}

	/**
	 * 聊天消息监听
	 * 
	 * @return
	 */
	public HashMap<Integer, PBTextListener> getPBTextListener() {
		return mTextMap;
	}

	public void addPBTextListener(PBTextListener chatMessageListener) {
		mTextMap.put(chatMessageListener.hashCode(), chatMessageListener);
	}

	public void addPacketListener(PacketListener listener) {
		mPacketListener = listener;
	}

	public PacketListener getMPacketListener() {
		return mPacketListener;
	}

	public boolean addConnectionLister(ConnectionListener listener) {
		return mConnetionSet.add(listener);
	}

	public boolean removeConnectionLister(ConnectionListener listener) {
		return mConnetionSet.remove(listener);
	}

	/**
	 * 返回当前sdk联接实例
	 * 
	 * @param context
	 * @return
	 */
	public static WebSDK getWebSDK(Context context) {
		mContext = context;
		synchronized (domain) {
			if (mSDK == null) {
				mSDK = new WebSDK(context);
			}
			return mSDK;
		}

	}

	private WebSDK(Context context) {
		mFileService = new FileService(context);
		mFileSharePre = context.getSharedPreferences(fileName,
				context.MODE_WORLD_WRITEABLE);
		mConnectionListener = new ConnectionListener() {

			@Override
			public void reconnectionSuccessful() {
				Iterator<ConnectionListener> iterator = mConnetionSet
						.iterator();
				while (iterator.hasNext()) {
					iterator.next().reconnectionSuccessful();
				}
			}

			@Override
			public void reconnectionFailed(Exception e) {
				Iterator<ConnectionListener> iterator = mConnetionSet
						.iterator();
				while (iterator.hasNext()) {
					iterator.next().reconnectionFailed(e);
				}
			}

			@Override
			public void reconnectingIn(int seconds) {
				Iterator<ConnectionListener> iterator = mConnetionSet
						.iterator();
				while (iterator.hasNext()) {
					iterator.next().reconnectingIn(seconds);
				}
			}

			@Override
			public void connectionClosedOnError(Exception e) {
				e.printStackTrace();
				Log.e("connectionListenr", e.getMessage() + "");
				mConnection.disconnect();
				mConnection.reconnect();
				Iterator<ConnectionListener> iterator = mConnetionSet
						.iterator();
				while (iterator.hasNext()) {
					iterator.next().connectionClosedOnError(e);
				}
			}

			@Override
			public void connectionClosed() {
				Iterator<ConnectionListener> iterator = mConnetionSet
						.iterator();
				while (iterator.hasNext()) {
					iterator.next().connectionClosed();
				}
			}
		};
		mConnection = new WebConnection(new ConnectionConfiguration(),
				mConnectionListener, this);
	}

	public void disconnection() {
		mConnection.disconnect();
	}

	public void saveUserInfo(String name, String pwd, String token) {
		Editor editor = mFileSharePre.edit();
		if (!TextUtils.isEmpty(name)) {
			editor.putString("name", name);
		}
		if (!TextUtils.isEmpty(pwd)) {
			editor.putString("pwd", pwd);
		}
		if (!TextUtils.isEmpty(token)) {
			editor.putString("token", token);
		}
		editor.commit();
	}

	public String getUserName() {
		return mFileSharePre.getString("name", "");
	}

	public String getPassword() {
		return mFileSharePre.getString("pwd", "");
	}

	public String getToken() {
		return mFileSharePre.getString("token", "");
	}

	public String getDemain() {
		return mFileSharePre.getString("DOMAIN", "");
	}

	public void connect() {
		// new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		mConnection.reconnect();
		// }
		// }).start();

	}

	public void offeLineMessage() {
		mConnection.offeLineMessage();
	}

	public boolean sendOffer(String user, String fileId) {
		return mConnection.sendOffer(user, fileId);
	}

	public boolean sendAnswer(String user, String fileId) {
		return mConnection.sendAnswer(user, fileId);
	}

	public boolean sendCandidate(String user, String fileId) {
		return mConnection.sendCandidate(user, fileId);
	}

	public boolean isconnect() {
		return mConnection.isConnected();
	}

	public void login(String user, String pwd, IloginListener listener) {
		try {
			mLoginListener = listener;
			mConnection.login(user, pwd);
			saveUserInfo(user, pwd, null);
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {

				@Override
				public void run() {
					sendLoginListener(null, null, "登陆超时，请检查网络状况", false);
				}
			}, 10000);

		} catch (IOException e) {
			sendLoginListener(null, null, e.getMessage(), false);
		}
	}

	public void sendLoginListener(String token, String password, String desc,
			boolean bo) {
		if (mLoginListener != null) {
			synchronized (mLoginListener) {
				if (mConnection.isAuthenticated() && mConnection.isConnected()) {
					bo = true;
				}
				if (bo) {
					mLoginListener.loginSuccess();
				} else {
					mConnectionListener.connectionClosedOnError(new Exception(
							desc));
					mLoginListener.loginFail(desc);
				}
				mLoginListener = null;

			}
		}
	}

	public void sendMessage(String toMessage, String message) {
		mConnection.sendMessage(toMessage, message, null);
	}

	public void sendFileMessage(final String toMessage, final String file,
			String message) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					UploadFile.upload("http://139.162.30.167:18080",
							mFileService.getFilePath(file),
							new OnUploadSuccessListener() {

								@Override
								public void onUploadSuccess(final String result) {
									Log.e("hj", "result===:" + result);
									mConnection.sendMessage(toMessage, "",
											result);
								}

								@Override
								public void onUploadError(final String result) {
									Log.e("hj", "result===:" + result);
								}
							});
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}).start();

	}

	public void sendGroupMessage(String toMessage, String message) {
		mConnection.sendGroupMessage(toMessage, message);
	}

	public void creatGroup(String groupID, String... memberID) {
		mConnection.creatGroup(groupID, memberID);
	}

	public void addGroupMember(String group, String... member) {
		mConnection.addGroupMember(group, member);
	}

	public void delGroupMember(String group, String... member) {
		mConnection.delGroupMember(group, member);
	}

	public void delGroup(String group) {
		mConnection.delGroup(group);
	}
}
