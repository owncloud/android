package com.owncloud.android.lib.common;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;

import com.owncloud.android.lib.common.network.BearerAuthScheme;
import com.owncloud.android.lib.common.network.BearerCredentials;

public class OwnCloudBearerCredentials implements OwnCloudCredentials {

	private String mAccessToken;
	
	public OwnCloudBearerCredentials(String accessToken) {
		mAccessToken = accessToken != null ? accessToken : "";
	}

	@Override
	public void applyTo(OwnCloudClient client) {
	    AuthPolicy.registerAuthScheme(BearerAuthScheme.AUTH_POLICY, BearerAuthScheme.class);
	    
	    List<String> authPrefs = new ArrayList<String>(1);
	    authPrefs.add(BearerAuthScheme.AUTH_POLICY);
	    client.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);        
	    
	    client.getParams().setAuthenticationPreemptive(true);
	    client.getState().setCredentials(
	    		AuthScope.ANY, 
	    		new BearerCredentials(mAccessToken)
		);
	}

	@Override
	public String getUsername() {
		// its unknown
		return null;
	}
	
	@Override
	public String getAuthToken() {
		return mAccessToken;
	}

	@Override
	public boolean authTokenExpires() {
		return true;
	}

}
