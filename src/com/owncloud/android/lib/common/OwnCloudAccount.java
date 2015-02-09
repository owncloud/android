/* ownCloud Android client application
 *   Copyright (C) 2014 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.lib.common;


import java.io.IOException;

import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.net.Uri;

/**
 * OwnCloud Account
 * 
 * @author David A. Velasco
 */
public class OwnCloudAccount {

    private Uri mBaseUri; 
    
    private OwnCloudCredentials mCredentials;
    
    private String mSavedAccountName;

    private Account mSavedAccount;


    /**
     * Constructor for already saved OC accounts.
     *
     * Do not use for anonymous credentials.
     */
    public OwnCloudAccount(Account savedAccount, Context context) throws AccountNotFoundException {
        if (savedAccount == null) {
            throw new IllegalArgumentException("Parameter 'savedAccount' cannot be null");
        }
        mSavedAccount = savedAccount;
        mSavedAccountName = savedAccount.name;
        mBaseUri = Uri.parse(AccountUtils.getBaseUrlForAccount(context, mSavedAccount));
        mCredentials = null;
    }

    /**
     * Method for deferred load of account attributes from AccountManager
     *
     * @param context
     * @throws AccountNotFoundException
     * @throws AuthenticatorException
     * @throws IOException
     * @throws OperationCanceledException
     */
    public void loadCredentials(Context context)
        throws AccountNotFoundException, AuthenticatorException,
                IOException, OperationCanceledException {

        mCredentials = AccountUtils.getCredentialsForAccount(context, mSavedAccount);
    }

    /*
    public OwnCloudAccount(Account savedAccount, Context context)
    		throws AccountNotFoundException, AuthenticatorException, 
    		IOException, OperationCanceledException {
    	
    	if (savedAccount == null) {
    		throw new IllegalArgumentException("Parameter 'savedAccount' cannot be null");
    	}
    	if (context == null) {
    		throw new IllegalArgumentException("Parameter 'context' cannot be null");
    	}
    	
    	mSavedAccountName = savedAccount.name;
        mBaseUri = Uri.parse(AccountUtils.getBaseUrlForAccount(context, savedAccount));
        mCredentials = AccountUtils.getCredentialsForAccount(context, savedAccount);
        if (mCredentials == null) {
        	mCredentials = OwnCloudCredentialsFactory.getAnonymousCredentials();
        }
    }
    */

    /**
     * Constructor for non yet saved OC accounts.
     *
     * @param baseUri           URI to the OC server to get access to.
     * @param credentials       Credentials to authenticate in the server. NULL is valid for anonymous credentials.
     */
    public OwnCloudAccount(Uri baseUri, OwnCloudCredentials credentials) {
        if (baseUri == null) {
            throw new IllegalArgumentException("Parameter 'baseUri' cannot be null");
        }
        mSavedAccount = null;
        mSavedAccountName = null;
        mBaseUri = baseUri;
        mCredentials = credentials != null ? 
        		credentials : OwnCloudCredentialsFactory.getAnonymousCredentials();
        String username = mCredentials.getUsername();
        if (username != null) {
        	mSavedAccountName = AccountUtils.buildAccountName(mBaseUri, username);
        }
    }
    

	public boolean isAnonymous() {
        return (mCredentials == null);
    }   // TODO no more
    
    public Uri getBaseUri() {
        return mBaseUri;
    }
            
    public OwnCloudCredentials getCredentials() {
        return mCredentials;
    }
    
    public String getName() {
    	return mSavedAccountName;
    }

    
}