package com.owncloud.android.lib.common;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.cookie.CookiePolicy;

import android.net.Uri;

public class OwnCloudSamlSsoCredentials implements OwnCloudCredentials {

	private String mSessionCookie;

	public OwnCloudSamlSsoCredentials(String sessionCookie) {
		mSessionCookie = sessionCookie != null ? sessionCookie : "";
	}

	@Override
	public void applyTo(OwnCloudClient client) {
        client.getParams().setAuthenticationPreemptive(false);
        client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        client.setFollowRedirects(false);
        
    	Uri serverUri = client.getBaseUri();
        
        String[] cookies = mSessionCookie.split(";");
        if (cookies.length > 0) {
        	Cookie cookie = null;
            for (int i=0; i<cookies.length; i++) {
            	int equalPos = cookies[i].indexOf('=');
            	if (equalPos >= 0) {
                	cookie = new Cookie();
	            	cookie.setName(cookies[i].substring(0, equalPos));
	    	        cookie.setValue(cookies[i].substring(equalPos + 1));
	    	        cookie.setDomain(serverUri.getHost());	// VERY IMPORTANT 
	    	        cookie.setPath(serverUri.getPath());	// VERY IMPORTANT
	    	        client.getState().addCookie(cookie);
            	}
            }
        }
	}

	@Override
	public String getUsername() {
		// its unknown
		return null;
	}
	
	@Override
	public String getAuthToken() {
		return mSessionCookie;
	}

	@Override
	public boolean authTokenExpires() {
		return true;
	}

}
