/* ownCloud Android Library is available under MIT license
 *   @author masensio
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

import junit.framework.AssertionFailedError;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.status.GetRemoteCapabilitiesOperation;
import com.owncloud.android.lib.test_project.R;
import com.owncloud.android.lib.test_project.SelfSignedConfidentSslSocketFactory;

/**
 * Class to test GetRemoteCapabilitiesOperation
 *
 */
public class GetCapabilitiesTest extends RemoteTest {
	private static final String LOG_TAG = GetCapabilitiesTest.class.getCanonicalName();
	
	String mServerUri, mUser, mPass;
	OwnCloudClient mClient = null;
	
	public GetCapabilitiesTest() {
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


	protected Context getContext() {
		return getActivity();
	}

	@Override
	protected void setUp() throws Exception {
	    super.setUp();

	    // Next initialization cannot be done in the constructor because getContext() is not 
	    // ready yet, returns NULL.
	    initAccessToServer(getContext());
	    
	    Log.v(LOG_TAG, "Setting up the remote fixture...");
	    
		Log.v(LOG_TAG, "Remote fixture created.");
		
	}
	
	
	// Tests
	/**
	 *  Test get capabilities
	 * 
	 *  Requires OC server 8.1 or later
	 */
	public void testGetRemoteCapabilitiesOperation() {
		// get capabilities
		GetRemoteCapabilitiesOperation getCapabilitiesOperation = new GetRemoteCapabilitiesOperation();
		RemoteOperationResult result = getCapabilitiesOperation.execute(mClient);
		assertTrue(result.isSuccess());
		assertTrue(result.getData() != null && result.getData().size() == 1);
		
	}
	
	@Override
	protected void tearDown() throws Exception {
	    Log.v(LOG_TAG, "Deleting remote fixture...");
		super.tearDown();
	    Log.v(LOG_TAG, "Remote fixture delete.");
	}

	
	private void initAccessToServer(Context context) {
	    Log.v(LOG_TAG, "Setting up client instance to access OC server...");
		
		mServerUri = context.getString(R.string.server_base_url);
		mUser = context.getString(R.string.username);
		mPass = context.getString(R.string.password);
		
		mClient = new OwnCloudClient(
				Uri.parse(mServerUri), 
				NetworkUtils.getMultiThreadedConnManager()
		);
		mClient.setDefaultTimeouts(
				OwnCloudClientFactory.DEFAULT_DATA_TIMEOUT, 
				OwnCloudClientFactory.DEFAULT_CONNECTION_TIMEOUT);
		mClient.setFollowRedirects(true);
		mClient.setCredentials(
				OwnCloudCredentialsFactory.newBasicCredentials(
						mUser, 
						mPass
				)
		);
		
	    Log.v(LOG_TAG, "Client instance set up.");
	    
	}
}
