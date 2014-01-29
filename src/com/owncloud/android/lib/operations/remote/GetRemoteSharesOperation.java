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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;
import org.xmlpull.v1.XmlPullParserException;

import com.owncloud.android.lib.network.OwnCloudClient;
import com.owncloud.android.lib.operations.common.RemoteOperation;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.lib.operations.common.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.operations.common.OCShare;
import com.owncloud.android.lib.utils.ShareXMLParser;

import android.util.Log;


/** 
 * Get the data from the server to know shares
 * 
 * @author masensio
 *
 */

public class GetRemoteSharesOperation extends RemoteOperation {

	private static final String TAG = GetRemoteSharesOperation.class.getSimpleName();

	// OCS Route
	private static final String SHAREAPI_ROUTE ="/ocs/v1.php/apps/files_sharing/api/v1/shares"; 

	private ArrayList<OCShare> mShares;  // List of shares for result

	
	public GetRemoteSharesOperation() {
	}

	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
		RemoteOperationResult result = null;
		int status = -1;

		// Get Method        
		GetMethod get = new GetMethod(client.getBaseUri() + SHAREAPI_ROUTE);
		Log.d(TAG, "URL ------> " + client.getBaseUri() + SHAREAPI_ROUTE);

		// Get the response
		try{
			status = client.executeMethod(get);
			if(isSuccess(status)) {
				Log.d(TAG, "Obtain RESPONSE");
				String response = get.getResponseBodyAsString();
				Log.d(TAG, response);

				// Parse xml response --> obtain the response in ShareFiles ArrayList
				// convert String into InputStream
				InputStream is = new ByteArrayInputStream(response.getBytes());
				ShareXMLParser xmlParser = new ShareXMLParser();
				mShares = xmlParser.parseXMLResponse(is);
				if (mShares != null) {
					Log.d(TAG, "Shares: " + mShares.size());
					result = new RemoteOperationResult(ResultCode.OK);
					ArrayList<Object> sharesObjects = new ArrayList<Object>();
					for (OCShare share: mShares) {
						sharesObjects.add(share);
					}
					result.setData(sharesObjects);
				}
			}
		} catch (HttpException e) {
			result = new RemoteOperationResult(e);
			e.printStackTrace();
		} catch (IOException e) {
			result = new RemoteOperationResult(e);
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			result = new RemoteOperationResult(e);
			e.printStackTrace();
		} finally {
			get.releaseConnection();
		}
		return result;
	}

	private boolean isSuccess(int status) {
		return (status == HttpStatus.SC_OK);
	}


}
