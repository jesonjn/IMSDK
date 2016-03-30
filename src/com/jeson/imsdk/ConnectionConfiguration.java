package com.jeson.imsdk;

import com.jeson.imsdk.test.TestWebSDK;

/**
 * 
 * @author jiangneng
 * 
 */
public class ConnectionConfiguration {

	public String getServiceName() {
		// TODO Auto-generated method stub
		return TestWebSDK.ip;
	}

	public String getHost() {
		return TestWebSDK.ip;
	}

	public int getPort() {
		// TODO Auto-generated method stub
		return TestWebSDK.port;
	}

	public String getDemain() {
		return WebSDK.domain;
	}

	public boolean isReconnectionAllowed() {
		return false;
	}

}
