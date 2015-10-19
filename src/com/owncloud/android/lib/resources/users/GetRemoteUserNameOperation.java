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

package com.owncloud.android.lib.resources.users;

import java.util.ArrayList;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;
import org.json.JSONObject;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;


/**
 * @author masensio
 *
 * Get the UserName for a SAML connection, from a JSON with the format:
 * 		id
 * 		display-name
 * 		email
 */

public class GetRemoteUserNameOperation extends RemoteOperation {
	
	private static final String TAG = GetRemoteUserNameOperation.class.getSimpleName();

	// OCS Route
	private static final String OCS_ROUTE ="/index.php/ocs/cloud/user?format=json";

	// JSON Node names
	private static final String NODE_OCS = "ocs";
	private static final String NODE_DATA = "data";
	private static final String NODE_ID = "id";
	private static final String NODE_DISPLAY_NAME= "display-name";
	private static final String NODE_EMAIL= "email";

	private String mUserName;

	public String getUserName() {
		return mUserName;
	}

	
	public GetRemoteUserNameOperation() {
	}

	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
		RemoteOperationResult result = null;
		int status = -1;
		GetMethod get = null;

		//Get the user
		try {
			get = new GetMethod(client.getBaseUri() + OCS_ROUTE);
			get.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);
			status = client.executeMethod(get);
			if(isSuccess(status)) {
				 String response = get.getResponseBodyAsString();
				 Log_OC.d(TAG, "Successful response: " + response);

				 // Parse the response
				 JSONObject respJSON = new JSONObject(response);
				 JSONObject respOCS = respJSON.getJSONObject(NODE_OCS);
				 JSONObject respData = respOCS.getJSONObject(NODE_DATA);
				 String id = respData.getString(NODE_ID);
				 String displayName = respData.getString(NODE_DISPLAY_NAME);
				 String email = respData.getString(NODE_EMAIL);
				 
				 // Result
				 result = new RemoteOperationResult(true, status, get.getResponseHeaders());
				 // Username in result.data
                 ArrayList<Object> data = new ArrayList<Object>();
                 data.add(displayName);
                 result.setData(data);
				 mUserName =  displayName;
				 
				 Log_OC.d(TAG, "*** Parsed user information: " + id + " - " + displayName + " - " + email);
				 
			} else {
				result = new RemoteOperationResult(false, status, get.getResponseHeaders());
				String response = get.getResponseBodyAsString();
				Log_OC.e(TAG, "Failed response while getting user information ");
				if (response != null) {
					Log_OC.e(TAG, "*** status code: " + status + " ; response message: " + response);
				} else {
					Log_OC.e(TAG, "*** status code: " + status);
				}
			}
		} catch (Exception e) {
			result = new RemoteOperationResult(e);
			Log_OC.e(TAG, "Exception while getting OC user information", e);
			
		} finally {
			get.releaseConnection();
		}
        
		return result;
	}

    private boolean isSuccess(int status) {
        return (status == HttpStatus.SC_OK);
    }
    
}
