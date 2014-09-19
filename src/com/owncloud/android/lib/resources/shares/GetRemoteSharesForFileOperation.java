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

package com.owncloud.android.lib.resources.shares;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * Provide a list shares for a specific file.  
 * The input is the full path of the desired file.  
 * The output is a list of everyone who has the file shared with them.
 * 
 * @author masensio
 *
 */

public class GetRemoteSharesForFileOperation extends RemoteOperation {

	private static final String TAG = GetRemoteSharesForFileOperation.class.getSimpleName();
	
	private static final String PARAM_PATH = "path";
	private static final String PARAM_RESHARES = "reshares";
	private static final String PARAM_SUBFILES = "subfiles";

	private ArrayList<OCShare> mShares;  // List of shares for result, one share in this case
	
	private String mRemoteFilePath;
	private boolean mReshares;
	private boolean mSubfiles;
	
	/**
	 * Constructor
	 * 
	 * @param remoteFilePath	Path to file or folder
	 * @param reshares			If set to false (default), only shares from the current user are returned
	 * 							If set to true, all shares from the given file are returned
	 * @param subfiles			If set to false (default), lists only the folder being shared
	 * 							If set to true, all shared files within the folder are returned.
	 */
	public GetRemoteSharesForFileOperation(String remoteFilePath, boolean reshares, boolean subfiles) {
		mRemoteFilePath = remoteFilePath;
		mReshares = reshares;
		mSubfiles = subfiles;
	}

	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
		RemoteOperationResult result = null;
		int status = -1;

		GetMethod get = null;

		try {
			// Get Method
			get = new GetMethod(client.getBaseUri() + ShareUtils.SHARING_API_PATH);

			// Add Parameters to Get Method
			get.setQueryString(new NameValuePair[] { 
				    new NameValuePair(PARAM_PATH, mRemoteFilePath),
				    new NameValuePair(PARAM_RESHARES, String.valueOf(mReshares)),
				    new NameValuePair(PARAM_SUBFILES, String.valueOf(mSubfiles))
				}); 

			get.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);
            
			status = client.executeMethod(get);

			if(isSuccess(status)) {
				String response = get.getResponseBodyAsString();

				result = new RemoteOperationResult(ResultCode.OK);
				
				// Parse xml response --> obtain the response in ShareFiles ArrayList
				// convert String into InputStream
				InputStream is = new ByteArrayInputStream(response.getBytes());
				ShareXMLParser xmlParser = new ShareXMLParser();
				mShares = xmlParser.parseXMLResponse(is);
				if (mShares != null) {
					Log_OC.d(TAG, "Got " + mShares.size() + " shares");
					result = new RemoteOperationResult(ResultCode.OK);
					ArrayList<Object> sharesObjects = new ArrayList<Object>();
					for (OCShare share: mShares) {
						// Build the link 
						if (share.getToken().length() > 0) {
							share.setShareLink(client.getBaseUri() + ShareUtils.SHARING_LINK_TOKEN + share.getToken());
						}
						sharesObjects.add(share);
					}
					result.setData(sharesObjects);
				}

			} else {
				result = new RemoteOperationResult(false, status, get.getResponseHeaders());
			}
			
		} catch (Exception e) {
			result = new RemoteOperationResult(e);
			Log_OC.e(TAG, "Exception while getting shares", e);
			
		} finally {
			if (get != null) {
				get.releaseConnection();
			}
		}
		return result;
	}

	private boolean isSuccess(int status) {
		return (status == HttpStatus.SC_OK);
	}

}
