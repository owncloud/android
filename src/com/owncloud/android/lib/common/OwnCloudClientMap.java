/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud Inc.
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

import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.httpclient.Cookie;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants;

/**
 * Map for {@link OwnCloudClient} instances associated to ownCloud {@link Account}s
 * 
 * TODO check synchronization
 * 
 * TODO consider converting into a non static object saved in the application context 
 * @author David A. Velasco
 * @author masensio
 */

public class OwnCloudClientMap {
    
	
    private static ConcurrentMap<String, OwnCloudClient> mClients = 
            new java.util.concurrent.ConcurrentHashMap<String, OwnCloudClient>();
    
    public static synchronized OwnCloudClient getClientFor(Account account, Context context) 
            throws OperationCanceledException, AuthenticatorException, 
            AccountNotFoundException, IOException {
        
        OwnCloudClient client = mClients.get(account.name);
        if (client == null) {
            client = OwnCloudClientFactory.createOwnCloudClient(
                            account, 
                            context.getApplicationContext());
            mClients.putIfAbsent(account.name, client);
        }
        return client;
    }

    
    public static synchronized OwnCloudClient removeClientFor(Account account) {
    	return mClients.remove(account.name);
    }
    
    
    public static synchronized void clearPool() {
        mClients.clear();
    }
    
    public static synchronized void saveClient(Account account, Context context) {
    	
    	// Account Manager
    	AccountManager ac = AccountManager.get(context);
    	
    	OwnCloudClient client = mClients.get(account.name);
    	
    	Cookie[] cookies =client.getState().getCookies(); 
    	String cookiesString ="";
    	for (Cookie cookie: cookies) {
    		cookiesString = cookiesString + cookie.toString();
    	}
    	ac.setUserData(account, Constants.KEY_COOKIES, cookiesString); 
    	//Log.d("OwnCloudClientMap", "Saving Cookies: "+ cookiesString );
    }
}
