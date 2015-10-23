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

package com.owncloud.android.lib.resources.shares;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/** 
 * Get the data about a Share resource, known its remote ID.
 */

public class GetRemoteShareOperation extends RemoteOperation {

	private static final String TAG = GetRemoteShareOperation.class.getSimpleName();

	private long mRemoteId;


	public GetRemoteShareOperation(long remoteId) {
		mRemoteId = remoteId;
	}


	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
		RemoteOperationResult result = null;
		int status = -1;

		// Get Method        
		GetMethod get = null;

		// Get the response
		try{
			get = new GetMethod(client.getBaseUri() + ShareUtils.SHARING_API_PATH + "/" + Long.toString(mRemoteId));
			//get.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);
			status = client.executeMethod(get);

			if(isSuccess(status)) {
				String response = get.getResponseBodyAsString();
				ArrayList<Object> resultData = new ArrayList<Object>();

				// Parse xml response --> obtain the response in ShareFiles ArrayList
				// convert String into InputStream
				InputStream is = new ByteArrayInputStream(response.getBytes());
				ShareXMLParser xmlParser = new ShareXMLParser();
				List<OCShare> shares = xmlParser.parseXMLResponse(is);
				if (xmlParser.isSuccess()) {
					if (shares != null && shares.size() > 0) {
						Log_OC.d(TAG, "Got " + shares.size() + " shares");
						result = new RemoteOperationResult(ResultCode.OK);
						resultData.add(shares.get(0));
						result.setData(resultData);
					} else {
						result = new RemoteOperationResult(ResultCode.WRONG_SERVER_RESPONSE);
						Log_OC.e(TAG, "Successful status with no share in it");
					}

				} else if (xmlParser.isWrongParameter()){
					result = new RemoteOperationResult(ResultCode.SHARE_WRONG_PARAMETER);
					resultData.add(xmlParser.getMessage());
					result.setData(resultData);

				} else if (xmlParser.isNotFound()){
					result = new RemoteOperationResult(ResultCode.SHARE_NOT_FOUND);
					resultData.add(xmlParser.getMessage());
					result.setData(resultData);

				} else if (xmlParser.isForbidden()) {
					result = new RemoteOperationResult(ResultCode.SHARE_FORBIDDEN);
					resultData.add(xmlParser.getMessage());
					result.setData(resultData);

				} else {
					result = new RemoteOperationResult(ResultCode.WRONG_SERVER_RESPONSE);
				}

			} else {
				result = new RemoteOperationResult(false, status, get.getResponseHeaders());
			}
			
		} catch (Exception e) {
			result = new RemoteOperationResult(e);
			Log_OC.e(TAG, "Exception while getting remote shares ", e);
			
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
