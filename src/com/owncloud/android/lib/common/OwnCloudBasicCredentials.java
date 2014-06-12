package com.owncloud.android.lib.common;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;

public class OwnCloudBasicCredentials implements OwnCloudCredentials {

	private String mUsername;
	private String mPassword;

	public OwnCloudBasicCredentials(String username, String password) {
		mUsername = username != null ? username : "";
		mPassword = password != null ? password : "";
	}

	@Override
	public void applyTo(OwnCloudClient client) {
        List<String> authPrefs = new ArrayList<String>(1);
        authPrefs.add(AuthPolicy.BASIC);
        client.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);        
        
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(
        		AuthScope.ANY, 
        		new UsernamePasswordCredentials(mUsername, mPassword)
		);
	}

	@Override
	public String getUsername() {
		return mUsername;
	}

	@Override
	public String getAuthToken() {
		return mPassword;
	}

	@Override
	public boolean authTokenExpires() {
		return false;
	}

}
