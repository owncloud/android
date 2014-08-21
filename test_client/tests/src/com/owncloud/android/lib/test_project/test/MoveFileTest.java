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

package com.owncloud.android.lib.test_project.test;

import java.security.GeneralSecurityException;
import java.util.Vector;

import junit.framework.AssertionFailedError;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.MoveRemoteFileOperation;
import com.owncloud.android.lib.test_project.R;
import com.owncloud.android.lib.test_project.SelfSignedConfidentSslSocketFactory;
import com.owncloud.android.lib.test_project.TestActivity;

import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

/**
 * Class to test MoveRemoteFileOperation
 * 
 * @author David A. Velasco
 */

public class MoveFileTest extends AndroidTestCase {
	
	private static final String LOG_TAG = MoveFileTest.class.getCanonicalName();
	
	/* Paths for tests on folders */
	private static final String SRC_FOLDER_NAME = "folderToMove";
	private static final String SRC_FOLDER_PARENT_PATH = "/src/";
	private static final String SRC_FOLDER_PATH = SRC_FOLDER_PARENT_PATH + SRC_FOLDER_NAME;
	private static final String TARGET_FOLDER_PARENT_PATH = "/target/";
	private static final String TARGET_FOLDER_PATH = TARGET_FOLDER_PARENT_PATH + SRC_FOLDER_NAME;
	
	private static boolean mGlobalSetupDone = false;
	
	String mServerUri, mUser, mPass;
	OwnCloudClient mClient = null;
	
	private Vector<String> mToCleanUpInServer = new Vector<String>(2);
	
	public MoveFileTest() {
		super();
		
		mGlobalSetupDone = false;
		
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
	    
		mServerUri = getContext().getString(R.string.server_base_url);
		mUser = getContext().getString(R.string.username);
		mPass = getContext().getString(R.string.password);
		
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
    
	    if (!mGlobalSetupDone) {
	    	
		    Log.v(LOG_TAG, "Starting global set up");
		    RemoteOperationResult result = 
		    		TestActivity.createFolder(SRC_FOLDER_PATH, true, mClient);
			if (!result.isSuccess()) {
				Utils.logAndThrow(LOG_TAG, result);
			}
		    result = TestActivity.createFolder(TARGET_FOLDER_PARENT_PATH, true, mClient);
			if (!result.isSuccess()) {
				Utils.logAndThrow(LOG_TAG, result);
			}
			
			Log.v(LOG_TAG, "Global set up done");
		    
		    mGlobalSetupDone = true;
	    }
	}
	
	/**
	 * Test move folder
	 */
	public void testMoveFolder() {

		mToCleanUpInServer.add(SRC_FOLDER_PARENT_PATH);
		mToCleanUpInServer.add(TARGET_FOLDER_PARENT_PATH);
		
		MoveRemoteFileOperation renameOperation = new MoveRemoteFileOperation(
				SRC_FOLDER_PATH,
				TARGET_FOLDER_PATH,
				false
		);
		
		RemoteOperationResult result = renameOperation.execute(mClient);
		assertTrue(result.isSuccess());
	}
	
	@Override
	protected void tearDown() throws Exception {
		for (String path : mToCleanUpInServer) {
			RemoteOperationResult removeResult = 
					TestActivity.removeFile(path, mClient);
			if (!removeResult.isSuccess()) {
				Utils.logAndThrow(LOG_TAG, removeResult);
			}
		}
		mToCleanUpInServer.clear();
		super.tearDown();
	}
	
}
