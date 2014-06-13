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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;

/**
 * Implementation of {@link OwnCloudClientManager}
 * 
 * TODO check multithreading safety
 * 
 * @author David A. Velasco
 * @author masensio
 */

public class SingleSessionManager implements OwnCloudClientManager {
    
	private static final String TAG = SingleSessionManager.class.getSimpleName();

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
    	boolean reusingKnown = false;	// just for logs
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
    		reusingKnown = true;
    		keepCredentialsUpdated(account, client);
    	}
    	
    	if (client == null) {
    		// no client to reuse - create a new one
    		client = OwnCloudClientFactory.createOwnCloudClient(
    				account.getBaseUri(), 
    				context.getApplicationContext(), 
    				true);
    		
    		// Restore Cookies ??
    		AccountUtils.restoreCookies(accountName, client, context);		
    		
    		client.setCredentials(account.getCredentials());
    		if (accountName != null) {
    			mClientsWithKnownUsername.put(accountName, client);
    			Log.d(TAG, "    new client {" + accountName + ", " + client.hashCode() + "}");

    		} else {
    			mClientsWithUnknownUsername.put(sessionName, client);
    			Log.d(TAG, "    new client {" + sessionName + ", " + client.hashCode() + "}");
    		}
    	} else {
    		if (!reusingKnown) {
    			Log.d(TAG, "    reusing client {" + sessionName + ", " + client.hashCode() + "}");
    		}
    	}
    	
    	return client;
    }
    
    
	@Override
	public OwnCloudClient removeClientFor(OwnCloudAccount account) {
		
    	if (account == null) {
    		return null;
    	}

    	OwnCloudClient client = null;
    	String accountName = account.getName();
    	if (accountName != null) {
    		client = mClientsWithKnownUsername.remove(accountName);
        	if (client != null) {
        		Log.d(TAG, "Removed client {" + accountName + ", " + client.hashCode() + "}");
        		return client;
        	} else {
        		Log.d(TAG, "No client tracked for  {" + accountName + "}");
        	}
    	}
    	
    	String sessionName = AccountUtils.buildAccountName(
    			account.getBaseUri(), 
    			account.getCredentials().getAuthToken());
    	client = mClientsWithUnknownUsername.remove(sessionName);
    	if (client != null) {
			Log.d(TAG, "Removed client {" + sessionName + ", " + client.hashCode() + "}");
			return  client;
    	}
		Log.d(TAG, "No client tracked for  {" + sessionName + "}");
    		
		Log.d(TAG, "No client removed");
		return null;
		
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

    		Uri serverBaseUri = 
    				Uri.parse(AccountUtils.getBaseUrlForAccount(context, account));

    		Map<OwnCloudCredentials, OwnCloudClient> clientsPerAccount = 
    				mClientsPerServer.get(serverBaseUri.toString());

    		if (clientsPerAccount != null) {
    			OwnCloudCredentials credentials = 
    					AccountUtils.getCredentialsForAccount(context, account);

    			/// TODO - CRITERIA FOR MATCH OF KEYS!!!
    			OwnCloudClient client = clientsPerAccount.get(credentials);
    			if (client != null) {
    				AccountUtils.saveClient(client, account, context.getApplicationContext());
    			}
    		}
    	}
    	
    }

    
	private void keepCredentialsUpdated(OwnCloudAccount account, OwnCloudClient reusedClient) {
		OwnCloudCredentials recentCredentials = account.getCredentials();
		if (!recentCredentials.getAuthToken().equals(
				reusedClient.getCredentials().getAuthToken())) {
			reusedClient.setCredentials(recentCredentials);
		}
		
	}

}
