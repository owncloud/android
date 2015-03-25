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


import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;

import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.IOException;

public class SimpleFactoryManager implements OwnCloudClientManager {
    
	private static final String TAG = SimpleFactoryManager.class.getSimpleName();

	@Override
	public OwnCloudClient getClientFor(OwnCloudAccount account, Context context)
            throws AccountNotFoundException, OperationCanceledException, AuthenticatorException,
            IOException {

		Log_OC.d(TAG, "getClientFor(OwnCloudAccount ... : ");

		OwnCloudClient client = OwnCloudClientFactory.createOwnCloudClient(
				account.getBaseUri(), 
				context.getApplicationContext(),
				true);

		Log_OC.v(TAG, "    new client {" +
				(account.getName() != null ?
						account.getName() :
						AccountUtils.buildAccountName(account.getBaseUri(), "")

                ) + ", " + client.hashCode() + "}");

        if (account.getCredentials() == null) {
            account.loadCredentials(context);
        }
        client.setCredentials(account.getCredentials());
		return client;
	}

	@Override
	public OwnCloudClient removeClientFor(OwnCloudAccount account) {
		// nothing to do - not taking care of tracking instances!
		return null;
	}

	@Override
	public void saveAllClients(Context context, String accountType) {
		// nothing to do - not taking care of tracking instances!
	}

}
