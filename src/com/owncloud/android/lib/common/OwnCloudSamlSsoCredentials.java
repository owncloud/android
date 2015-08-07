/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */
package com.owncloud.android.lib.common;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.cookie.CookiePolicy;

import android.net.Uri;

public class OwnCloudSamlSsoCredentials implements OwnCloudCredentials {

	private String mUsername;
	private String mSessionCookie;

	public OwnCloudSamlSsoCredentials(String username, String sessionCookie) {
		mUsername = username != null ? username : "";
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
		// not relevant for authentication, but relevant for informational purposes
		return mUsername;
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
