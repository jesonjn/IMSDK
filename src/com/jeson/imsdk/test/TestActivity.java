package com.jeson.imsdk.test;

import java.io.IOException;

import android.app.Activity;
import android.graphics.Interpolator.Result;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

import com.jeson.imsdk.WebSDK;
import com.jeson.imsdk.AGPBMessage.PB_Auth;
import com.jeson.imsdk.AGPBMessage.PB_Business;
import com.jeson.imsdk.AGPBMessage.PB_Packet;
import com.jeson.imsdk.AGPBMessage.PB_PacketType;

public class TestActivity extends Activity {
	WebSDK sdk;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LinearLayout main = new LinearLayout(this);
		Button button = new Button(this);
		button.setText("发送消息----");
		main.setOrientation(LinearLayout.VERTICAL);
		main.addView(button);
		setContentView(main);
//		sdk = WebSDK.(TestActivity.this);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sdk.sendMessage("","我去 你好吗");
			}
		});
		new AsyncTask<String, String, Result>() {

			@Override
			protected Result doInBackground(String... params) {

				try {
					Log.i("test---", "user---------test--");
					sdk.connect();
					;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

//				sdk.login("test", "test");

				return null;
			}

		}.execute("");

	}
}
