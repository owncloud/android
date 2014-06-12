package com.owncloud.android.lib.common;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.cookie.CookiePolicy;

import android.net.Uri;

public class OwnCloudSamlSsoCredentials implements OwnCloudCredentials {

	private String mSessionCookie;

	public OwnCloudSamlSsoCredentials(String sessionCookie) {
		mSessionCookie = sessionCookie != null ? mSessionCookie : "";
	}

	@Override
	public void applyTo(OwnCloudClient client) {
        client.getParams().setAuthenticationPreemptive(false);
        client.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
        client.setFollowRedirects(false);
        
    	Uri serverUri = client.getBaseUri();
    	if (serverUri == null) {
    		// TODO fix the mess of Uris in OwnCloudClient
    		serverUri = client.getWebdavUri();
    	}
        
        String[] cookies = mSessionCookie.split(";");
        if (cookies.length > 0) {
            for (int i=0; i<cookies.length; i++) {
            	Cookie cookie = new Cookie();
            	int equalPos = cookies[i].indexOf('=');
            	cookie.setName(cookies[i].substring(0, equalPos));
    	        cookie.setValue(cookies[i].substring(equalPos + 1));
    	        cookie.setDomain(serverUri.getHost());	// VERY IMPORTANT 
    	        cookie.setPath(serverUri.getPath());	// VERY IMPORTANT
    	        client.getState().addCookie(cookie);
            }
        }
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
