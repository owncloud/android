/* ownCloud Android Library is available under MIT license
 *   @author David A. Velasco
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

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.json.JSONException;
import org.json.JSONObject;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.shares.GetRemoteShareesOperation;
import com.owncloud.android.lib.test_project.R;
import com.owncloud.android.lib.test_project.SelfSignedConfidentSslSocketFactory;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * Class to test GetRemoteShareesOperation
 *
 * With this TestCase we are experimenting a bit to improve the test suite design, in two aspects:
 * 
 *  - Reduce the dependency from the set of test cases on the "test project" needed to 
 *  have an instrumented APK to install in the device, as required by the testing framework
 *  provided by Android. To get there, this class avoids calling TestActivity methods in the test 
 *  method.
 *  
 *  - Reduce the impact of creating a remote fixture over the Internet, while the structure of the 
 *  TestCase is kept easy to maintain. To get this, all the tests are done in a single test method, 
 *  granting this way that setUp and tearDown are run only once.
 *
 */

public class GetShareesTest extends RemoteTest {

	private static final String LOG_TAG = GetShareesTest.class.getCanonicalName();
		
	String mServerUri, mUser, mPass;
	OwnCloudClient mClient = null;
	
	public GetShareesTest() {
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


	/**
	 *  Test get sharees
	 * 
	 *  Requires OC server 8.2 or later
	 */
	public void testGetRemoteShareesOperation() {
		Log.v(LOG_TAG, "testGetRemoteSharees in");

		/// successful cases

		// search for sharees including "a"
		GetRemoteShareesOperation getShareesOperation = new GetRemoteShareesOperation("a", 1, 50);
		RemoteOperationResult result = getShareesOperation.execute(mClient);
		JSONObject resultItem;
		JSONObject value;
		byte type;
		int userCount = 0, groupCount = 0;
		assertTrue(result.isSuccess() && result.getData().size() == 3);
		try {
			for (int i=0; i<3; i++) {
				resultItem = (JSONObject) result.getData().get(i);
	            value = resultItem.getJSONObject(GetRemoteShareesOperation.NODE_VALUE);
	            type = (byte) value.getInt(GetRemoteShareesOperation.PROPERTY_SHARE_TYPE);
				if (GetRemoteShareesOperation.GROUP_TYPE.equals(type)) {
					groupCount++;
				} else {
					userCount++;
				}
			}
			assertEquals(userCount, 2);
			assertEquals(groupCount, 1);
		} catch (JSONException e) {
			AssertionFailedError afe = new AssertionFailedError(e.getLocalizedMessage());
			afe.setStackTrace(e.getStackTrace());
			throw afe;
		}
		
		// search for sharees including "ad"
		getShareesOperation = new GetRemoteShareesOperation("ad", 1, 50);
		result = getShareesOperation.execute(mClient);
		assertTrue(result.isSuccess() && result.getData().size() == 2);
		userCount = 0; groupCount = 0;
		try {
			for (int i=0; i<2; i++) {
				resultItem = (JSONObject) result.getData().get(i);
	            value = resultItem.getJSONObject(GetRemoteShareesOperation.NODE_VALUE);
	            type = (byte) value.getInt(GetRemoteShareesOperation.PROPERTY_SHARE_TYPE);
				if (GetRemoteShareesOperation.GROUP_TYPE.equals(type)) {
					groupCount++;
				} else {
					userCount++;
				}
			}
			assertEquals(userCount, 1);
			assertEquals(groupCount, 1);
		} catch (JSONException e) {
			AssertionFailedError afe = new AssertionFailedError(e.getLocalizedMessage());
			afe.setStackTrace(e.getStackTrace());
			throw afe;
		}
		
		
		// search for sharees including "b"
		getShareesOperation = new GetRemoteShareesOperation("b", 1, 50);
		result = getShareesOperation.execute(mClient);
		assertTrue(result.isSuccess() && result.getData().size() == 0);
		
		
		/// failed cases
		
		// search for sharees including wrong page values
		getShareesOperation = new GetRemoteShareesOperation("a", 0, 50);
		result = getShareesOperation.execute(mClient);
		assertTrue(!result.isSuccess() && result.getHttpCode() == HttpStatus.SC_BAD_REQUEST);
		
		getShareesOperation = new GetRemoteShareesOperation("a", 1, 0);
		result = getShareesOperation.execute(mClient);
		assertTrue(!result.isSuccess() && result.getHttpCode() == HttpStatus.SC_BAD_REQUEST);
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
