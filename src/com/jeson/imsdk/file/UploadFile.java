package com.jeson.imsdk.file;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.text.TextUtils;
import android.util.Log;

import com.jeson.xutils.HttpUtils;
import com.jeson.xutils.exception.HttpException;
import com.jeson.xutils.http.HttpHandler;
import com.jeson.xutils.http.RequestParams;
import com.jeson.xutils.http.ResponseInfo;
import com.jeson.xutils.http.callback.RequestCallBack;
import com.jeson.xutils.http.client.HttpRequest.HttpMethod;
import com.jeson.xutils.http.client.entity.FileUploadEntity;

public class UploadFile {

	public static String upload(String actionUrl, String FileName)
			throws IOException {
		// 产生随机分隔内容
		String BOUNDARY = java.util.UUID.randomUUID().toString();
		String PREFFIX = "--", LINEND = "\r\n";
		String MULTIPART_FROM_DATA = "multipart/form-data";
		String CHARSET = "UTF-8";
		// 定义URL实例
		URL uri = new URL(actionUrl);
		HttpURLConnection conn = (HttpURLConnection) uri.openConnection();
		// 设置从主机读取数据超时
		conn.setReadTimeout(10 * 1000);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setUseCaches(false);
		conn.setRequestMethod("POST");
		// 维持长连接
		conn.setRequestProperty("connection", "keep-alive");
		conn.setRequestProperty("Charset", "UTF-8");
		// 设置文件类型
		conn.setRequestProperty("Content-Type", MULTIPART_FROM_DATA
				+ ";boundary=" + BOUNDARY);
		// 创建一个新的数据输出流，将数据写入指定基础输出流
		DataOutputStream outStream = new DataOutputStream(
				conn.getOutputStream());
		// 发送文件数据
		if (FileName != null) {
			// 构建发送字符串数据
			StringBuilder sb1 = new StringBuilder();
			sb1.append(PREFFIX);
			sb1.append(BOUNDARY);
			sb1.append(LINEND);
			sb1.append("Content-Disposition: form-data; name=\"file\"; filename=\""
					+ FileName + "\"" + LINEND);
			sb1.append("Content-Type: application/octet-stream;chartset="
					+ CHARSET + LINEND);
			sb1.append(LINEND);
			// 写入到输出流中
			outStream.write(sb1.toString().getBytes());
			// 将文件读入输入流中
			InputStream is = new FileInputStream(FileName);
			byte[] buffer = new byte[1024];
			int len = 0;
			// 写入输出流
			while ((len = is.read(buffer)) != -1) {

				outStream.write(buffer, 0, len);
			}
			is.close();
			// 添加换行标志
			outStream.write(LINEND.getBytes());
		}
		// 请求结束标志
		byte[] end_data = (PREFFIX + BOUNDARY + PREFFIX + LINEND).getBytes();
		outStream.write(end_data);
		// 刷新发送数据
		outStream.flush();
		// 得到响应码
		int res = conn.getResponseCode();

		InputStream in = null;
		StringBuilder sb2 = new StringBuilder();
		// 上传成功返回200
		if (res == 200) {
			in = conn.getInputStream();
			int ch;

			// 保存数据
			while ((ch = in.read()) != -1) {
				sb2.append((char) ch);
			}
		}

		// 如果数据不为空，则以字符串方式返回数据，否则返回null
		return in == null ? null : sb2.toString();
	};

	/**
	 * 上传文件
	 * 
	 * @param domain
	 *            ：域名地址
	 * @param topicId
	 *            ：主题id
	 * @param filePath
	 *            ：文件路径
	 */
	public static void upload(String domain, String filePath,
			OnUploadSuccessListener listener) {
		RequestParams requestParams = new RequestParams(); // 默认编码UTF-8
		// requestParams.addQueryStringParameter("topicId", topicId + "");
		// 用于非multipart表单的单文件上传
		requestParams.setBodyEntity(new FileUploadEntity(new File(filePath),
				"multipart/form-data"));
		// upload(domain, requestParams, listener);
		// uploadConntionUrl(domain, filePath, listener);
		try {
			listener.onUploadSuccess(upload(domain, filePath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			listener.onUploadError(e.getMessage());
		}

	}

	

	/**
	 * xUtils框架Http请求工具
	 * 
	 */

	private static HttpUtils httpUtils;

	/**
	 * 上传文件
	 * 
	 * @param url
	 * @param requestParams
	 * @param onUploadSuccessListener
	 *            ：上传完成监听接口
	 * @return
	 */
	private static HttpHandler<String> upload(String url,
			RequestParams requestParams,
			final OnUploadSuccessListener onUploadSuccessListener) {
		HttpHandler<String> httpHandler = getHttpUtils().send(HttpMethod.POST,
				url, requestParams, new RequestCallBack<String>() {
					@Override
					public void onSuccess(ResponseInfo<String> responseInfo) {
						Log.i("up", "responseInfo.result--:"
								+ responseInfo.result);
						if (!TextUtils.isEmpty(responseInfo.result)) { // 自行判断结果是否为null或空串
							onUploadSuccessListener
									.onUploadSuccess(responseInfo.result);
						}
					}

					@Override
					public void onFailure(HttpException error, String msg) {
						onUploadSuccessListener.onUploadError(msg);
					}

				});
		return httpHandler;
	}

	public static HttpUtils getHttpUtils() {
		if (null != httpUtils) {
			return httpUtils;
		} else {
			httpUtils = new HttpUtils();
			httpUtils.configCurrentHttpCacheExpiry(3000);
			httpUtils.configTimeout(10 * 1000);
			return httpUtils;
		}
	}

	/**
	 * 上传成功，回调接口
	 * 
	 */
	public interface OnUploadSuccessListener {

		public void onUploadSuccess(String result);

		public void onUploadError(String result);

	}
}
