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
package com.owncloud.android.lib.test_project.test;

import java.security.GeneralSecurityException;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import android.net.Uri;
import android.test.AndroidTestCase;

import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.SingleSessionManager;
import com.owncloud.android.lib.test_project.R;
import com.owncloud.android.lib.test_project.SelfSignedConfidentSslSocketFactory;

import junit.framework.AssertionFailedError;

/**
 * Unit test for SingleSessionManager 
 * 
 * @author David A. Velasco
 */
public class SingleSessionManagerTest extends AndroidTestCase {

	private SingleSessionManager mSSMgr;
	
	private Uri mServerUri;
	private String mUsername;
	private OwnCloudAccount mValidAccount;
	private OwnCloudAccount mAnonymousAccount;
	
	public SingleSessionManagerTest() {
		super();
		
		Protocol pr = Protocol.getProtocol("https");
		if (pr == null || !(pr.getSocketFactory() instanceof SelfSignedConfidentSslSocketFactory)) {
			try {
				ProtocolSocketFactory psf = new SelfSignedConfidentSslSocketFactory();
				Protocol.registerProtocol(
						"https",
						new Protocol("https", psf, 443));
				
			} catch (GeneralSecurityException e) {
				throw new AssertionFailedError(
						"Self-signed confident SSL context could not be loaded");
			}
		}
	}
	
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mSSMgr = new SingleSessionManager();
		mServerUri = Uri.parse(getContext().getString(R.string.server_base_url));
		mUsername = getContext().getString(R.string.username);
		
		mValidAccount = new OwnCloudAccount(
				mServerUri, OwnCloudCredentialsFactory.newBasicCredentials(
						mUsername, 
						getContext().getString(R.string.password)
				)
		);
		
		mAnonymousAccount = new OwnCloudAccount(
				mServerUri, OwnCloudCredentialsFactory.getAnonymousCredentials());
				
	}
	
	public void testGetClientFor() {
		try {
			OwnCloudClient client1 = mSSMgr.getClientFor(mValidAccount, getContext());
			OwnCloudClient client2 = mSSMgr.getClientFor(mAnonymousAccount, getContext());

			assertNotSame("Got same client instances for different accounts",
					client1, client2);
			assertSame("Got different client instances for same account",
					client1, mSSMgr.getClientFor(mValidAccount,  getContext()));

		} catch (Exception e) {
			throw new AssertionFailedError("Exception getting client for account: " + e.getMessage());
		}

		// TODO harder tests
	}
    
	public void testRemoveClientFor() {
		try {
			OwnCloudClient client1 = mSSMgr.getClientFor(mValidAccount, getContext());
			mSSMgr.removeClientFor(mValidAccount);
			assertNotSame("Got same client instance after removing it from manager",
					client1, mSSMgr.getClientFor(mValidAccount,  getContext()));
		} catch (Exception e) {
			throw new AssertionFailedError("Exception getting client for account: " + e.getMessage());
		}
		
		// TODO harder tests
	}

    
	public void testSaveAllClients() {
		// TODO implement test;
		// 		or refactor saveAllClients() method out of OwnCloudClientManager to make 
		//		it independent of AccountManager
	}

}
