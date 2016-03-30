package com.jeson.imsdk.video;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnection.IceServer;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.StatsObserver;
import org.webrtc.StatsReport;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRenderer.I420Frame;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;

import com.jeson.imsdk.AGPBMessage.PB_Packet;
import com.jeson.imsdk.AGPBMessage.PB_Text;
import com.jeson.imsdk.AGPBMessage.PB_Text.TextData;
import com.jeson.imsdk.WebSDK;
import com.jeson.imsdk.file.DownloadFile;
import com.jeson.imsdk.file.UploadFile;
import com.jeson.imsdk.file.UploadFile.OnUploadSuccessListener;
import com.jeson.imsdk.listener.PacketListener;
import com.jeson.xutils.exception.HttpException;
import com.jeson.xutils.http.ResponseInfo;
import com.jeson.xutils.http.callback.RequestCallBack;
import com.jeson.xutils.util.FileService;

/**
 * 
 * @author jiangneng
 * 
 */
public class VideoManager implements VideoServerConfig.IceServersObserver {
	WakeLock wakeLock;
	VideoGLView vpv;
	private VideoRenderer.Callbacks localRender;
	private MediaConstraints sdpMediaConstraints;
	private PeerConnectionFactory factory;
	private final MessageHandler gaeHandler = new GAEHandler();
	private VideoServerConfig appRtcClient;
	private PeerConnection pc;
	private final PCObserver pcObserver = new PCObserver();
	private final SDPObserver sdpObserver = new SDPObserver();
	private static boolean mIsInited;
	private boolean mIsCalled;
	private Activity mActivity;
	static FileService fileService;
	private static String remoteName;
	WebSDK mWebSdk;
	private VideoStreamsView mVsv;
	// private LinkedList<IceCandidate> queuedRemoteCandidates = new
	// LinkedList<IceCandidate>();
	private final Boolean[] quit = new Boolean[] { false };
	private VideoCapturer capturer;
	private static String TAG = "VideoManager";
	public static HashMap<String, PacketListener> mSystemPacketListenrMap = new HashMap<String, PacketListener>();
	private static VideoManager videoManager;
	private static HashMap<String, VideoSystemListener> videoNotifiy = new HashMap<String, VideoSystemListener>();
	// VideoSystemListener mListener;

	private PacketListener packetListener = new PacketListener() {

		@Override
		public boolean proccessPacket(PB_Packet packet) {
			if ((remoteName + "_" + mWebSdk.domain).equals(packet.getText()
					.getMessageFrom())) {
				if (packet.getText().getMethod() == 4
						|| packet.getText().getMethod() == 5) {
					final PB_Text.TextData data = packet.getText().getTextData(
							0);
					Log.i(TAG, "文件id-----" + data.getTdvalue());
					// mActivity.runOnUiThread(new Runnable() {
					//
					// @Override
					// public void run() {
					// Toast.makeText(mActivity, "收到一个请求：" + data.getTdkey(),
					// 1).show();
					// }
					// });

					if ("candidate".equals(data.getTdkey())) {
						if (mIsInited)
							return acceptOffer(data);
					} else if ("offer".equals(data.getTdkey())) {
						if (videoNotifiy.containsKey(packet.getText()
								.getMessageFrom())) {
							return videoNotifiy.get(
									packet.getText().getMessageFrom())
									.receiverOffer(
											packet.getText().getMessageFrom(),
											"offer", data, VideoManager.this);
						}
						// else {
						// if (mListener != null)
						// mListener.receiverOffer(packet.getText()
						// .getMessageFrom(), "offer", data,
						// VideoManager.this);
						// }
					} else if ("answer".equals(data.getTdkey())) {
						if (mIsInited)
							return acceptOffer(data);
					}

					return false;

				}
				return mIsInited;
			}
			return false;
		}

	};

	// public void setVideoListener(VideoSystemListener listener) {
	// mListener = listener;
	// }

	public void addVideoListener(String user, VideoSystemListener listener) {
		videoNotifiy.put(user + "_" + WebSDK.domain, listener);
	}

	public void removeVideoListener(String user) {
		videoNotifiy.remove(user + "_" + WebSDK.domain);
	}

	public static VideoManager getVideoManagerIntance() {
		if (videoManager == null) {
			videoManager = new VideoManager();
		}
		return videoManager;
	}

	private VideoManager() {
		mSystemPacketListenrMap.put("video", packetListener);
	}

	public boolean acceptOffer(final String type, String fileID) {
		DownloadFile.download("http://139.162.30.167:18080?id=" + fileID,
				fileService.getFilePath("temp_" + fileID + ".txt"),
				new RequestCallBack<File>() {

					@Override
					public void onSuccess(ResponseInfo<File> responseInfo) {
						try {

							gaeHandler.onMessage(
									fileService.read(responseInfo.result),
									remoteName, type);
						} catch (Exception e) {
							e.printStackTrace();
						}

					}

					@Override
					public void onFailure(HttpException error, String msg) {
						error.printStackTrace();
						Log.e(TAG,
								"下载失败：" + msg + "----"
										+ error.getExceptionCode() + "===="
										+ error.getMessage());
					};
				});
		return true;
	}

	public boolean acceptOffer(final TextData data) {
		acceptOffer(data.getTdkey(), data.getTdvalue());
		return true;
	}

	public void init(Activity activity, WebSDK webSdk, String user) {
		mWebSdk = webSdk;
		mActivity = activity;
		remoteName = user;
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public boolean onResumeVideoInit(VideoStreamsView vsv, ViewGroup view,
			int type) {
		mVsv = vsv;
		fileService = new FileService(mActivity);
		appRtcClient = new VideoServerConfig(mActivity, gaeHandler, this);
		PowerManager powerManager = (PowerManager) mActivity
				.getSystemService(Activity.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(
				PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "jn");
		wakeLock.acquire();

		// 本地的surfaceview
		Point displaySize = new Point();
		mActivity.getWindowManager().getDefaultDisplay()
				.getRealSize(displaySize);
		vpv = new VideoGLView(mActivity, displaySize);
		VideoRendererGui.setView(vpv);
		localRender = VideoRendererGui.create(0, 0, 100, 100);
		view.addView(vpv);

		abortUnless(PeerConnectionFactory.initializeAndroidGlobals(mActivity,
				true, true), "Failed to initializeAndroidGlobals");

		// 这里是听筒或者扬声器设置的地方，根据需求自己固定或者动态设置，(此处固定某种模式后切换其他模式可能导致无法调节音量)，需要动态操作
		AudioManager audioManager = ((AudioManager) mActivity
				.getSystemService(Activity.AUDIO_SERVICE));
		audioManager
				.setMode(audioManager.isWiredHeadsetOn() ? AudioManager.MODE_IN_CALL
						: AudioManager.MODE_IN_CALL);
		audioManager.setSpeakerphoneOn(!audioManager.isWiredHeadsetOn());

		// Media条件信息SDP接口
		sdpMediaConstraints = new MediaConstraints();
		// 接受远程音频
		sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
				"OfferToReceiveAudio", "true"));
		// 音频call则不接受远程视频流
		if (type == 0) {
			sdpMediaConstraints.mandatory
					.add(new MediaConstraints.KeyValuePair(
							"OfferToReceiveVideo", "true"));
		} else {
			sdpMediaConstraints.mandatory
					.add(new MediaConstraints.KeyValuePair(
							"OfferToReceiveVideo", "false"));
		}

		// iceServer List对象获取
		List<PeerConnection.IceServer> iceServers = appRtcClient.initwebrtc(
				"114.215.81.102:3478", "100", "100pass");
		factory = new PeerConnectionFactory();

		// 创建peerconnection接口，用于发送offer 或者answer
		pc = factory.createPeerConnection(iceServers,
				appRtcClient.pcConstraints(), pcObserver);
		mIsInited = false;
		mIsCalled = false;
		{
			final PeerConnection finalPC = pc;
			final Runnable repeatedStatsLogger = new Runnable() {
				public void run() {

					synchronized (quit[0]) {
						if (quit[0]) {
							return;
						}
						final Runnable runnableThis = this;
						boolean success = finalPC.getStats(new StatsObserver() {
							public void onComplete(StatsReport[] reports) {
								for (StatsReport report : reports) {
									Log.d(TAG, "Stats: " + report.toString());
								}
								mVsv.postDelayed(runnableThis, 10000);
							}
						}, null);
						if (!success) {
							throw new RuntimeException(
									"getStats() return false!");
						}
					}
				}
			};
			vsv.postDelayed(repeatedStatsLogger, 10000);
		}
		{
			Log.i("", "Creating local video source...");
			MediaStream lMS = factory.createLocalMediaStream("ARDAMS");
			if (appRtcClient.videoConstraints() != null) {
				capturer = getVideoCapturer();
				VideoSource videoSource = factory.createVideoSource(capturer,
						appRtcClient.videoConstraints());
				VideoTrack videoTrack = factory.createVideoTrack("ARDAMSv0",
						videoSource);
				videoTrack.addRenderer(new VideoRenderer(localRender));
				lMS.addTrack(videoTrack);
			}
			if (appRtcClient.videoConstraints() != null) {
				lMS.addTrack(factory.createAudioTrack("ARDAMSa0", factory
						.createAudioSource(appRtcClient.videoConstraints())));
			}
			pc.addStream(lMS, new MediaConstraints());
		}

		mIsInited = true;

		return mIsInited;
	}

	private static void abortUnless(boolean condition, String msg) {
		if (!condition) {
			throw new RuntimeException(msg);
		}
	}

	public void onResume() {
		if (mVsv != null)
			mVsv.onResume();

	}

	public void onStop() {
		if (mVsv != null)
			mVsv.onPause();

	}

	public void onDestory() {
		disconnectAndExit();
	}

	/**
	 * 发送一个视频请求
	 * 
	 * @return
	 */
	public boolean sendOffer() {
		pc.createOffer(sdpObserver, sdpMediaConstraints);

		return true;

	}

	private void sendMessage(String json, String toAcount, String type) {
		Log.e("message", toAcount + "发送" + type + "==" + json.toString());

		if (!TextUtils.isEmpty(toAcount)) {
			if ("offer".equals(type)) {
				mWebSdk.sendOffer(toAcount, json);
			} else if ("answer".equals(type)) {
				mWebSdk.sendAnswer(toAcount, json);
				start();
			} else if ("candidate".equals(type)) {
				mWebSdk.sendCandidate(toAcount, json);
			}
		}
	}

	public void getBufferString(IceCandidate candidate) {
		stringBuffer.append(candidate.sdpMid);
		stringBuffer.append(",");
		stringBuffer.append(candidate.sdpMLineIndex);
		stringBuffer.append(",");
		stringBuffer.append(candidate.sdp);
		stringBuffer.append(";");

	}

	// Cycle through likely device names for the camera and return the first
	// capturer that works, or crash if none do.
	private VideoCapturer getVideoCapturer() {
		String[] cameraFacing = { "back", "front" };
		int[] cameraIndex = { 0, 1 };
		int[] cameraOrientation = { 0, 90, 180, 270 };
		for (String facing : cameraFacing) {
			for (int index : cameraIndex) {
				for (int orientation : cameraOrientation) {
					String name = "Camera " + index + ", Facing " + facing
							+ ", Orientation " + orientation;
					VideoCapturer capturer = VideoCapturer.create(name);
					Log.e(TAG, name + "---capturer---" + capturer);
					if (capturer != null) {
						// logAndToast("Using camera: " + name);
						return capturer;
					}
				}
			}
		}
		throw new RuntimeException("Failed to open capturer");
	}

	StringBuffer stringBuffer = new StringBuffer();
	boolean isSend = false;
	Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			isSend = false;
			final String str;
			synchronized (stringBuffer) {
				if (stringBuffer.length() > 0)
					stringBuffer = stringBuffer.deleteCharAt(stringBuffer
							.length() - 1);
				str = stringBuffer.toString();
				stringBuffer = new StringBuffer();
			}

			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						fileService.save(
								fileService.getFileName("candidate.txt"), str);
						UploadFile.upload("http://139.162.30.167:18080",
								fileService.getFilePath("candidate.txt"),
								new OnUploadSuccessListener() {

									@Override
									public void onUploadSuccess(
											final String result) {
										Log.e("hj", "result===:" + result);

										VideoManager.this.sendMessage(result,
												remoteName, "candidate");
									}

									@Override
									public void onUploadError(
											final String result) {
										Log.e("hj", "result===:" + result);
									}
								});
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			}).start();

		}

	};

	public void start() {
		if (!isSend) {
			handler.sendMessageDelayed(new Message(), 1000);
			isSend = true;
		}
	}

	// Implementation detail: observe ICE & stream changes and react
	// accordingly.
	private class PCObserver implements PeerConnection.Observer {
		// ICE回调接口，给对方发送candidate sdpMLineIndex sdpMid三个类型数据
		@Override
		public void onIceCandidate(final IceCandidate candidate) {
			Log.e("hj", "onIceCandidate");

			getBufferString(candidate);
			// start();

		}

		@Override
		public void onError() {
			Log.e("onError", "onError");
			mActivity.runOnUiThread(new Runnable() {
				public void run() {
					throw new RuntimeException("PeerConnection error!");
				}
			});
		}

		@Override
		public void onSignalingChange(PeerConnection.SignalingState newState) {
			Log.e("hj", "PCObserver onSignalingChange" + newState.toString());
		}

		@Override
		public void onIceConnectionChange(
				PeerConnection.IceConnectionState newState) {
			Log.e("hj",
					"PCObserver onIceConnectionChange" + newState.toString());
		}

		@Override
		public void onIceGatheringChange(
				PeerConnection.IceGatheringState newState) {
			Log.e("hj", "PCObserver onIceGatheringChange" + newState.toString());
		}

		// 添加流的回调，用于渲染和画数据的操作
		@Override
		public void onAddStream(final MediaStream stream) {
			mActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.e("demoActivity", "PCObserver onAddStream:"
							+ stream.videoTracks.size());
					if (stream.videoTracks.size() > 0) {
						abortUnless(stream.audioTracks.size() == 1
								&& stream.videoTracks.size() == 1,
								"Weird-looking stream: " + stream);
						stream.videoTracks.get(0).addRenderer(
								new VideoRenderer(new VideoCallbacks(mVsv,
										VideoStreamsView.Endpoint.REMOTE)));
					}

				}
			});
		}

		@Override
		public void onRemoveStream(final MediaStream stream) {
			mActivity.runOnUiThread(new Runnable() {
				public void run() {
					stream.videoTracks.get(0).dispose();
				}
			});
		}

		@Override
		public void onDataChannel(final DataChannel dc) {
			mActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.e("hj", "onDataChannel");
					throw new RuntimeException(
							"AppRTC doesn't use data channels, but got: "
									+ dc.label() + " anyway!");
				}
			});
		}

		@Override
		public void onRenegotiationNeeded() {

		}
	}

	// Implementation detail: handle offer creation/signaling and answer
	// setting,
	// as well as adding remote ICE candidates once the answer SDP is set.
	private class SDPObserver implements SdpObserver {
		// sdp监听者回调，获取sdp信息(音视频相关编码选项等参数信息，ip地址信息等)发送给对方，
		@Override
		public void onCreateSuccess(final SessionDescription sdp) {
			Log.e("demoActivity", "SDPObserver local sdp onCreateSuccess:"
					+ sdp.type);
			pc.setLocalDescription(sdpObserver, sdp);
			Log.e("demoActivity", "SDPObserver setLocalDescription:");

			try {
				String fileName = System.currentTimeMillis() + "_sdp.txt";
				fileService.save(fileService.getFileName(fileName),
						sdp.description);
				UploadFile.upload("http://139.162.30.167:18080",
						fileService.getFilePath(fileName),
						new OnUploadSuccessListener() {

							@Override
							public void onUploadSuccess(final String result) {
								Log.e("hj", "result==onUploadSuccess=:"
										+ result);
								mActivity.runOnUiThread(new Runnable() {

									public void run() {
										sendMessage(result, remoteName,
												sdp.type.canonicalForm());
									}
								});
							}

							@Override
							public void onUploadError(final String result) {
								Log.e("hj", "result=onUploadError==:" + result);
								mActivity.runOnUiThread(new Runnable() {

									public void run() {
										// sendMessage(result, remoteName);
									}
								});
							}
						});
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		@Override
		public void onSetSuccess() {
			mActivity.runOnUiThread(new Runnable() {
				public void run() {
					Log.e("demoActivity", "SDPObserver onSetSuccess::"
							+ mIsCalled);
					// if (appRtcClient.isInitiator()) {
					if (!mIsCalled) {
						if (pc.getRemoteDescription() != null) {
							drainRemoteCandidates();
						}
					} else {
						if (pc.getLocalDescription() == null) {
							// We just set the remote offer, time to create our
							// answer.
							// logAndToast("Creating answer");
							Log.e("demoActivity", "SDPObserver create answer");
						} else {
							// Sent our answer and set it as local description;
							// drain
							// candidates.
							drainRemoteCandidates();
						}
					}
				}
			});
		}

		@Override
		public void onCreateFailure(final String error) {
			mActivity.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(mActivity, "createSDP error: " + error, 1)
							.show();
				}
			});
		}

		@Override
		public void onSetFailure(final String error) {
			mActivity.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(mActivity, "setSDP error: " + error, 1)
							.show();
				}
			});
		}

		private void drainRemoteCandidates() {
			Log.e("demo Activity", " drainRemoteCandidates");
			// if (queuedRemoteCandidates == null)
			// return;
			// // for (IceCandidate candidate : queuedRemoteCandidates) {
			// // pc.addIceCandidate(candidate);
			// // }
			// for (int i = 0; i < queuedRemoteCandidates.size(); i++)
			// pc.addIceCandidate(queuedRemoteCandidates.get(i));
			// queuedRemoteCandidates = null;
		}
	}

	// Implementation detail: handler for receiving GAE messages and dispatching
	// them appropriately.
	private class GAEHandler implements MessageHandler {

		// 创建一个远程的offer操作
		public void onOpen() {
			if (!appRtcClient.isInitiator()) {
				return;
			}
			// logAndToast("Creating offer...");
			// Create a new offer.
			// The CreateSessionDescriptionObserver callback will be called when
			// done.
			pc.createOffer(sdpObserver, sdpMediaConstraints);

		}

		// 接收到对方的信息后进行处理，不管是offer还是answer的SDP等信息
		public int onMessage(final String msg, String fromuser,
				final String type) {
			Log.e("activity", "GAEHandler onMessage:" + msg);
//			mActivity.runOnUiThread(new Runnable() {
//
//				@Override
//				public void run() {
//					Toast.makeText(mActivity, msg + "收到一个请求：" + type, 1).show();
//				}
//			});
			if (type.equals("candidate")) {
				Log.e("hj", "candidate");
				String[] candidates = msg.split(";");
				IceCandidate candidate;
				for (String candidteString : candidates) {
					String[] candidateStr = candidteString.split(",");
					candidate = new IceCandidate(candidateStr[0],
							Integer.valueOf(candidateStr[1]), candidateStr[2]);

					// if (queuedRemoteCandidates != null) {
					// queuedRemoteCandidates.add(candidate);
					// } else {
					//
					// }
					pc.addIceCandidate(candidate);
				}

			} else if (type.equals("answer") || type.equals("offer")) {
				SessionDescription sdp;
				if (type.equals("offer")) {
					mIsCalled = true;
					sdp = new SessionDescription(SessionDescription.Type.OFFER,
							msg);
				} else {
					start();
					sdp = new SessionDescription(
							SessionDescription.Type.ANSWER, msg);
				}
				pc.setRemoteDescription(sdpObserver, sdp);

				// 创建一个远程的answser操作
				if (type.equals("offer")) {
					pc.createAnswer(sdpObserver, sdpMediaConstraints);
				}
			} else if (type.equals("bye")) {
				Log.e("bye", "Remote end hung up; dropping PeerConnection");
				disconnectAndExit();
			} else {
				return 1;
				// throw new RuntimeException("Unexpected message: " +
				// data);
			}
			return 0;

		}

		// @JavascriptInterface
		public void onClose() {
			disconnectAndExit();
		}

		// @JavascriptInterface
		public void onError(int code, String description) {
			disconnectAndExit();
		}
	}

	// Disconnect from remote resources, dispose of local resources, and exit.
	private void disconnectAndExit() {
		synchronized (quit[0]) {
			if (quit[0]) {
				return;
			}
			quit[0] = true;
			if (wakeLock != null)
				wakeLock.release();

			if (pc != null) {
				pc.dispose();
				pc = null;
			}

			if (appRtcClient != null) {
				appRtcClient.disconnect();
				appRtcClient = null;
			}
			if (capturer != null)
				capturer.dispose();
			videoManager.removeVideoListener(remoteName);

		}
	}

	// Implementation detail: bridge the VideoRenderer.Callbacks interface to
	// the
	// VideoStreamsView implementation.
	// 视频渲染的回调接口
	private class VideoCallbacks implements VideoRenderer.Callbacks {
		private final VideoStreamsView view;
		private final VideoStreamsView.Endpoint stream;

		public VideoCallbacks(VideoStreamsView view,
				VideoStreamsView.Endpoint stream) {
			this.view = view;
			this.stream = stream;
		}

		@Override
		public void setSize(final int width, final int height) {
			view.queueEvent(new Runnable() {
				public void run() {
					view.setSize(stream, width, height);
				}
			});
		}

		@Override
		public void renderFrame(I420Frame frame) {
			view.queueFrame(stream, frame);
		}
	}

	@Override
	public void onIceServers(List<IceServer> iceServers) {
		// Log.e("activity", "peer con created success!");
	}

	public interface VideoSystemListener {

		public abstract boolean receiverOffer(String user, String type,
				TextData data, VideoManager manager);

		public abstract boolean proccess(int i);

		public abstract boolean finish(boolean opt);
	}

}
