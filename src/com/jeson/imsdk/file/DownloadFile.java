package com.jeson.imsdk.file;

import java.io.File;

import android.text.TextUtils;

import com.jeson.xutils.HttpUtils;
import com.jeson.xutils.exception.HttpException;
import com.jeson.xutils.http.HttpHandler;
import com.jeson.xutils.http.ResponseInfo;
import com.jeson.xutils.http.callback.RequestCallBack;

public class DownloadFile {
	public static void download(String url, String file,
			RequestCallBack<File> requestCallBack) {

		HttpUtils http = new HttpUtils();
		HttpHandler handler = http.download(url, file, true, // 如果目标文件存在，接着未完成的部分继续下载。服务器不支持RANGE时将从新下载。
				true, // 如果从请求返回信息中获取到文件名，下载完成后自动重命名。
				requestCallBack);
	}
}
