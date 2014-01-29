
/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.lib.operations.remote;

import java.io.IOException;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.owncloud.android.lib.network.OwnCloudClient;
import com.owncloud.android.lib.operations.common.RemoteOperation;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;


/**
 * @author masensio
 *
 * Get the UserName for a SAML connection, from a JSON with the format:
 * 		id
 * 		display-name
 * 		email
 */

public class GetUserNameRemoteOperation extends RemoteOperation {
	
	private static final String TAG = GetUserNameRemoteOperation.class.getSimpleName();

	// HEADER
	private static final String HEADER_OCS_API = "OCS-APIREQUEST";
	private static final String HEADER_OCS_API_VALUE = "true";

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

	
	public GetUserNameRemoteOperation() {
	}

	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        int status = -1;
        GetMethod get = null;
        
        //Get the user
        try {
            get = new GetMethod(client.getBaseUri() + OCS_ROUTE);
            Log.e(TAG, "Getting OC user information from " + client.getBaseUri() + OCS_ROUTE);
            // Add the Header
            get.addRequestHeader(HEADER_OCS_API, HEADER_OCS_API_VALUE);
			status = client.executeMethod(get);
			if(isSuccess(status)) {
				 String response = get.getResponseBodyAsString();
				 Log.d(TAG, "Successful response: " + response);

				 // Parse the response
				 JSONObject respJSON = new JSONObject(response);
				 JSONObject respOCS = respJSON.getJSONObject(NODE_OCS);
				 JSONObject respData = respOCS.getJSONObject(NODE_DATA);
				 String id = respData.getString(NODE_ID);
				 String displayName = respData.getString(NODE_DISPLAY_NAME);
				 String email = respData.getString(NODE_EMAIL);
				 
				 // Result
				 result = new RemoteOperationResult(true, status, get.getResponseHeaders());
				 mUserName =  displayName;
				 
				 Log.d(TAG, "*** Parsed user information: " + id + " - " + displayName + " - " + email);
				 
			} else {
				result = new RemoteOperationResult(false, status, get.getResponseHeaders());
				String response = get.getResponseBodyAsString();
				Log.e(TAG, "Failed response while getting user information ");
				if (response != null) {
					Log.e(TAG, "*** status code: " + status + " ; response message: " + response);
				} else {
					Log.e(TAG, "*** status code: " + status);
				}
			}
		} catch (Exception e) {
			result = new RemoteOperationResult(e);
			Log.e(TAG, "Exception while getting OC user information", e);
			
		} finally {
			get.releaseConnection();
		}
        
		return result;
	}

    private boolean isSuccess(int status) {
        return (status == HttpStatus.SC_OK);
    }
    
}
