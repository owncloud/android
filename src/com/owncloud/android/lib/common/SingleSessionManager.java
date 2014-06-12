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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.util.Log;
import android.net.Uri;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants;

/**
 * Implementation of {@link OwnCloudClientManager}
 * 
 * TODO check multithreading safety
 * 
 * TODO better mapping?
 * 
 * @author David A. Velasco
 * @author masensio
 */

public class SingleSessionManager implements OwnCloudClientManager {
    
	private static final String TAG = SingleSessionManager.class.getSimpleName();

	private static OwnCloudClientManager mInstance = null;
	
    private Map<String, Map<OwnCloudCredentials, OwnCloudClient>> mClientsPerServer = 
            new HashMap<String, Map<OwnCloudCredentials, OwnCloudClient>>();
    
    private Map<String, OwnCloudClient> mClientsWithKnownUsername = 
    		new HashMap<String, OwnCloudClient>();
    
    private Map<String, OwnCloudClient> mClientsWithUnknownUsername = 
    		new HashMap<String, OwnCloudClient>();
    
    
    @Override
    public synchronized OwnCloudClient getClientFor(OwnCloudAccount account, Context context) {
		Log.d(TAG, "getClientFor(OwnCloudAccount ... : ");
    	if (account == null) {
    		throw new IllegalArgumentException("Cannot get an OwnCloudClient for a null account");
    	}

    	OwnCloudClient client = null;
    	String accountName = account.getName();
    	String sessionName = AccountUtils.buildAccountName(
    			account.getBaseUri(), 
    			account.getCredentials().getAuthToken());
    	
    	if (accountName != null) {
    		client = mClientsWithKnownUsername.get(accountName);
    	}
    	if (client == null) {
    		if (accountName != null) {
    			client = mClientsWithUnknownUsername.remove(sessionName);
    			if (client != null) {
    	    		Log.d(TAG, "    reusing client {" + sessionName + ", " + client.hashCode() + "}");
    				mClientsWithKnownUsername.put(accountName, client);
    	    		Log.d(TAG, "    moved client to {" + accountName + ", " + client.hashCode() + "}");
    			}
    		} else {
        		client = mClientsWithUnknownUsername.get(sessionName);
    		}
    	} else {
    		Log.d(TAG, "    reusing client {" + accountName + ", " + client.hashCode() + "}");
    	}
    	
    	if (client == null) {
    		// no client to reuse - create a new one
    		client = OwnCloudClientFactory.createOwnCloudClient(
    				account.getBaseUri(), 
    				context.getApplicationContext(), 
    				true);
    		client.setCredentials(account.getCredentials());
    		if (accountName != null) {
    			mClientsWithKnownUsername.put(accountName, client);
    			Log.d(TAG, "    new client {" + accountName + ", " + client.hashCode() + "}");

    		} else {
    			mClientsWithUnknownUsername.put(sessionName, client);
    			Log.d(TAG, "    new client {" + sessionName + ", " + client.hashCode() + "}");
    		}
    	} else {
    		Log.d(TAG, "    reusing client {" + sessionName + ", " + client.hashCode() + "}");
    	}
    	
    	return client;
    }
    
    
	@Override
	public synchronized OwnCloudClient getClientFor(Account savedAccount, Context context)
			throws OperationCanceledException, AuthenticatorException, AccountNotFoundException,
			IOException {
		
        Uri serverBaseUri = 
        		Uri.parse(AccountUtils.getBaseUrlForAccount(context, savedAccount));
        
        OwnCloudCredentials credentials = 
        		AccountUtils.getCredentialsForAccount(context, savedAccount);
        
        return getClientFor(serverBaseUri, credentials, context);
		
    }

    
	@Override
	public synchronized OwnCloudClient getClientFor(
			Uri serverBaseUri, OwnCloudCredentials credentials, Context context) {
		
		Map<OwnCloudCredentials, OwnCloudClient> clientsPerAccount = 
				mClientsPerServer.get(serverBaseUri.toString());
		
		if (clientsPerAccount == null) {
			clientsPerAccount = new HashMap<OwnCloudCredentials, OwnCloudClient>();
			mClientsPerServer.put(
					serverBaseUri.toString(), 
					clientsPerAccount);
		}
		
		if (credentials == null) {
			credentials = OwnCloudCredentialsFactory.getAnonymousCredentials(); 
		}
		
		/// TODO - CRITERIA FOR MATCH OF KEYS!!!
		OwnCloudClient client = clientsPerAccount.get(credentials);
    	if (client == null) {
    		client = OwnCloudClientFactory.createOwnCloudClient(
    				serverBaseUri, 
    				context.getApplicationContext(), 
    				true);
    		
    		client.setCredentials(credentials);
    		clientsPerAccount.put(credentials, client);
    	}
				
    	return client;
    }
    
    
	@Override
    public synchronized OwnCloudClient removeClientFor(Account savedAccount, Context context) 
    		throws AccountNotFoundException, OperationCanceledException, AuthenticatorException, IOException {
		
        Uri serverBaseUri = 
        		Uri.parse(AccountUtils.getBaseUrlForAccount(context, savedAccount));
        
        Map<OwnCloudCredentials, OwnCloudClient> clientsPerAccount = 
        		mClientsPerServer.get(serverBaseUri.toString());
        
        if (clientsPerAccount != null) {
            OwnCloudCredentials credentials = 
            		AccountUtils.getCredentialsForAccount(context, savedAccount);
            
        	return clientsPerAccount.remove(credentials);
        }
		return null;
    }
    
    
    @Override
    public synchronized void saveClient(Account savedAccount, Context context) 
    		throws  AccountNotFoundException, AuthenticatorException, IOException, 
    		OperationCanceledException {
    	
    	// Account Manager
    	AccountManager ac = AccountManager.get(context.getApplicationContext());
    	
    	if (savedAccount != null) {
            Uri serverBaseUri = 
            		Uri.parse(AccountUtils.getBaseUrlForAccount(context, savedAccount));
            
    		Map<OwnCloudCredentials, OwnCloudClient> clientsPerAccount = 
    				mClientsPerServer.get(serverBaseUri.toString());

    		if (clientsPerAccount != null) {
                OwnCloudCredentials credentials = 
                		AccountUtils.getCredentialsForAccount(context, savedAccount);
    		
	    		/// TODO - CRITERIA FOR MATCH OF KEYS!!!
	    		OwnCloudClient client = clientsPerAccount.get(credentials);
	    		
	    		if (client != null) {

		    		Cookie[] cookies = client.getState().getCookies(); 
		    		String cookiesString ="";
		    		for (Cookie cookie: cookies) {
		    			cookiesString = cookiesString + cookie.toString() + ";";
		    			
		    			logCookie(cookie);
		    		}
		    		ac.setUserData(savedAccount, Constants.KEY_COOKIES, cookiesString); 
		    		Log.d(TAG, "Saving Cookies: "+ cookiesString );
	    		}
    		}
    	}
    	
    }
    
    @Override
    public synchronized void saveAllClients(Context context, String accountType) 
    		throws AccountNotFoundException, AuthenticatorException, IOException,
    		OperationCanceledException {
    	
    	// Get all accounts
    	Account [] accounts = AccountManager.get(context.getApplicationContext())
    			.getAccountsByType(accountType);
    	
    	// Save cookies for all accounts
    	for(Account account: accounts){
    		saveClient(account, context.getApplicationContext());
    	}
    	
    }
    
    private void logCookie(Cookie cookie) {
    	Log.d(TAG, "Cookie name: "+ cookie.getName() );
    	Log.d(TAG, "       value: "+ cookie.getValue() );
    	Log.d(TAG, "       domain: "+ cookie.getDomain());
    	Log.d(TAG, "       path: "+ cookie.getPath() );
    	Log.d(TAG, "       version: "+ cookie.getVersion() );
    	Log.d(TAG, "       expiryDate: " + 
    			(cookie.getExpiryDate() != null ? cookie.getExpiryDate().toString() : "--"));
    	Log.d(TAG, "       comment: "+ cookie.getComment() );
    	Log.d(TAG, "       secure: "+ cookie.getSecure() );
    }


}
