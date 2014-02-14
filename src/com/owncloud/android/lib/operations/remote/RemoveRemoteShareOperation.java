/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/)
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

package com.owncloud.android.lib.operations.remote;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;

import android.util.Log;

import com.owncloud.android.lib.network.OwnCloudClient;
import com.owncloud.android.lib.operations.common.RemoteOperation;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.lib.operations.common.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.utils.ShareUtils;
import com.owncloud.android.lib.utils.ShareXMLParser;

/**
 * Remove a share
 * 
 * @author masensio
 *
 */

public class RemoveRemoteShareOperation extends RemoteOperation {

	private static final String TAG = RemoveRemoteShareOperation.class.getSimpleName();
	
	private int mIdShare;
	
	/**
	 * Constructor
	 * 
	 * @param idShare	Share ID
	 */
	
	public RemoveRemoteShareOperation(int idShare) {
		mIdShare = idShare;
		
	}

	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
		RemoteOperationResult result = null;
		int status = -1;

		DeleteMethod delete = null;

		try {
			String id = "/" + String.valueOf(mIdShare);
			delete = new DeleteMethod(client.getBaseUri() + ShareUtils.SHAREAPI_ROUTE + id);
			Log.d(TAG, "URL ------> " + client.getBaseUri() + ShareUtils.SHAREAPI_ROUTE + id);

			status = client.executeMethod(delete);

			if(isSuccess(status)) {
				String response = delete.getResponseBodyAsString();
				Log.d(TAG, "Successful response: " + response);

				result = new RemoteOperationResult(ResultCode.OK);
				
				// Parse xml response
				// convert String into InputStream
				InputStream is = new ByteArrayInputStream(response.getBytes());
				ShareXMLParser xmlParser = new ShareXMLParser();
				xmlParser.parseXMLResponse(is);
				if (xmlParser.isSuccess()) {
					result = new RemoteOperationResult(ResultCode.OK);
				} else if (xmlParser.isFileNotFound()){
					result = new RemoteOperationResult(ResultCode.SHARE_NOT_FOUND);
				} else {
					result = new RemoteOperationResult(false, status, delete.getResponseHeaders());	
				}
				
				Log.i(TAG, "Unshare " + id + ": " + result.getLogMessage());
			} else {
				result = new RemoteOperationResult(false, status, delete.getResponseHeaders());
			}
		} catch (Exception e) {
			result = new RemoteOperationResult(e);
			Log.e(TAG, "Unshare Link Exception " + result.getLogMessage(), e);

		} finally {
			if (delete != null)
				delete.releaseConnection();
		}
		return result;
	}


	private boolean isSuccess(int status) {
		return (status == HttpStatus.SC_OK);
	}
}