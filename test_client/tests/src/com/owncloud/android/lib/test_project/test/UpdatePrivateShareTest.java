/* ownCloud Android Library is available under MIT license
 *   @author masensio
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

import java.io.File;
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
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.RemoveRemoteShareOperation;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.shares.UpdateRemoteShareOperation;
import com.owncloud.android.lib.test_project.R;
import com.owncloud.android.lib.test_project.SelfSignedConfidentSslSocketFactory;
import com.owncloud.android.lib.test_project.TestActivity;

/**
 * Class to test UpdateRemoteShareOperation
 * with private shares
 *
 */
public class UpdatePrivateShareTest extends RemoteTest {
	private static final String LOG_TAG = UpdatePrivateShareTest.class.getCanonicalName();
	
	/* File to share and update */
	private static final String FILE_TO_SHARE = "/fileToShare.txt";

	/* Folder to share and update */
	private static final String FOLDER_TO_SHARE = "/folderToShare";

	/* Sharees */
	private static final String USER_SHAREE = "admin";
	private static final String GROUP_SHAREE = "admin";

	private String mFullPath2FileToShare;
	private String mFullPath2FolderToShare;

	private OCShare mFileShare;
	private OCShare mFolderShare;

	String mServerUri, mUser, mPass;
	OwnCloudClient mClient = null;
	
	public UpdatePrivateShareTest(){
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
	    
	    // Upload the file
	    mFullPath2FileToShare = mBaseFolderPath + FILE_TO_SHARE;
	    
	    File textFile = getActivity().extractAsset(TestActivity.ASSETS__TEXT_FILE_NAME);
		RemoteOperationResult result = getActivity().uploadFile(
				textFile.getAbsolutePath(), 
				mFullPath2FileToShare, 
				"txt/plain");
		if (!result.isSuccess()) {
			Utils.logAndThrow(LOG_TAG, result);
		}

		// Share the file privately with other user
		result = getActivity().createShare(
				mFullPath2FileToShare, 
				ShareType.USER,
				USER_SHAREE,
				false, 
				"", 
				OCShare.MAXIMUM_PERMISSIONS_FOR_FILE);

	    if (result.isSuccess()){
	    	mFileShare = (OCShare) result.getData().get(0);
	    } else{
	    	mFileShare = null;
	    }

		// Create the folder
		mFullPath2FolderToShare = mBaseFolderPath + FOLDER_TO_SHARE;
		result = getActivity().createFolder(
				mFullPath2FolderToShare,
				true);
		if (!result.isSuccess()) {
			Utils.logAndThrow(LOG_TAG, result);
		}

		// Share the folder privately with a group
		result = getActivity().createShare(
				mFullPath2FolderToShare,
				ShareType.GROUP,
				GROUP_SHAREE,
				false,
				"",
				OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER);

		if (result.isSuccess()){
			mFolderShare = (OCShare) result.getData().get(0);
		} else{
			mFolderShare = null;
		}

		Log.v(LOG_TAG, "Remote fixture created.");
		
	}
	
	
	public void testUpdateSharePermissions() {
		Log.v(LOG_TAG, "testUpdateSharePermissions in");
		
		if (mFileShare != null) {
			/// successful tests
			// Update Share permissions on a shared file
			UpdateRemoteShareOperation updateShare = new UpdateRemoteShareOperation(
					mFileShare.getRemoteId()
			);
			updateShare.setPermissions(OCShare.READ_PERMISSION_FLAG);	// minimum permissions
			RemoteOperationResult result = updateShare.execute(mClient);
			assertTrue(result.isSuccess());

			// Update Share permissions on a shared folder
			updateShare = new UpdateRemoteShareOperation(mFolderShare.getRemoteId());
			updateShare.setPermissions(OCShare.READ_PERMISSION_FLAG + OCShare.DELETE_PERMISSION_FLAG);
			result = updateShare.execute(mClient);
			assertTrue(result.isSuccess());


			/// unsuccessful tests
			// Update Share with invalid permissions
			updateShare = new UpdateRemoteShareOperation(mFileShare.getRemoteId());
			// greater than maximum value
			updateShare.setPermissions(OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER + 1);
			result = updateShare.execute(mClient);
			assertFalse(result.isSuccess());

			// Unshare the file before next unsuccessful tests
			RemoveRemoteShareOperation unshare = new RemoveRemoteShareOperation(
					(int) mFileShare.getRemoteId()
			);
			result = unshare.execute(mClient);
			
			if (result.isSuccess()) {				
				// Update Share permissions on unknown share
				UpdateRemoteShareOperation updateNoShare = new UpdateRemoteShareOperation(
						mFileShare.getRemoteId()
				);
				updateNoShare.setPermissions(OCShare.READ_PERMISSION_FLAG);	// minimum permissions
				result = updateNoShare.execute(mClient);
				assertFalse(result.isSuccess());
			}
				
		} 		
		
	}
	
	@Override
	protected void tearDown() throws Exception {
	    Log.v(LOG_TAG, "Deleting remote fixture...");
		if (mFullPath2FileToShare != null) {
			RemoteOperationResult removeResult = getActivity().removeFile(mFullPath2FileToShare);
			if (!removeResult.isSuccess() && removeResult.getCode() != ResultCode.TIMEOUT) {
				Utils.logAndThrow(LOG_TAG, removeResult);
			}
		}
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
