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

public class OwnCloudClientManagerFactory {
	
	public static enum Policy {
		ALWAYS_NEW_CLIENT,
		SINGLE_SESSION_PER_ACCOUNT
	}
	
	private static Policy sDefaultPolicy = Policy.ALWAYS_NEW_CLIENT;
	
	private static OwnCloudClientManager sDefaultSingleton;

    private static String sUserAgent;

	public static OwnCloudClientManager newDefaultOwnCloudClientManager() {
		return newOwnCloudClientManager(sDefaultPolicy);
	}
	
	public static OwnCloudClientManager newOwnCloudClientManager(Policy policy) {
		switch (policy) {
			case ALWAYS_NEW_CLIENT:
				return new SimpleFactoryManager();
				
			case SINGLE_SESSION_PER_ACCOUNT:
				return new SingleSessionManager();
				
			default:
				throw new IllegalArgumentException("Unknown policy");
		}
	}
	
    public static OwnCloudClientManager getDefaultSingleton() {
    	if (sDefaultSingleton == null) {
    		sDefaultSingleton = newDefaultOwnCloudClientManager();
    	}
    	return sDefaultSingleton;
    }
    
    public static Policy getDefaultPolicy() {
    	return sDefaultPolicy;
    }

    public static void setDefaultPolicy(Policy policy) {
    	if (policy == null) {
    		throw new IllegalArgumentException("Default policy cannot be NULL");
    	}
    	if (defaultSingletonMustBeUpdated(policy)) {
    		sDefaultSingleton = null;
    	}
    	sDefaultPolicy = policy;
    }

    public static void setUserAgent(String userAgent){
        sUserAgent = userAgent;
    }

    public static String getUserAgent() {
        return sUserAgent;
    }

	private static boolean defaultSingletonMustBeUpdated(Policy policy) {
		if (sDefaultSingleton == null) {
			return false;
		}
		if (policy == Policy.ALWAYS_NEW_CLIENT && 
				!(sDefaultSingleton instanceof SimpleFactoryManager)) {
			return true;
		}
		if (policy == Policy.SINGLE_SESSION_PER_ACCOUNT && 
				!(sDefaultSingleton instanceof SingleSessionManager)) {
			return true;
		}
		return false;
	}

}
