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

package com.owncloud.android.lib.resources.status;

import java.util.ArrayList;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * Checks if the server is valid and if the server supports the Share API
 * 
 * @author David A. Velasco
 * @author masensio
 *
 */

public class GetRemoteStatusOperation extends RemoteOperation {
    
    /** 
     * Maximum time to wait for a response from the server when the connection is being tested, 
     * in MILLISECONDs.
     */
    public static final int TRY_CONNECTION_TIMEOUT = 5000;
    
    private static final String TAG = GetRemoteStatusOperation.class.getSimpleName();
    
    private static final String NODE_INSTALLED = "installed";
    private static final String NODE_VERSION = "version";
    
    private RemoteOperationResult mLatestResult;
    private Context mContext;

    public GetRemoteStatusOperation(Context context) {
        mContext = context;
    }
    
    private boolean tryConnection(OwnCloudClient client) {
        boolean retval = false;
        GetMethod get = null;
        String baseUrlSt = client.getBaseUri().toString();
        try {
            get = new GetMethod(baseUrlSt + AccountUtils.STATUS_PATH);

            HttpParams params = get.getParams().getDefaultParams();
            params.setParameter(HttpMethodParams.USER_AGENT,
                    OwnCloudClientManagerFactory.getUserAgent());
            get.getParams().setDefaults(params);

            client.setFollowRedirects(false);
            boolean isRedirectToNonSecureConnection = false;
            int status = client.executeMethod(get, TRY_CONNECTION_TIMEOUT, TRY_CONNECTION_TIMEOUT);
            mLatestResult = new RemoteOperationResult(
            		(status == HttpStatus.SC_OK),
            		status,
            		get.getResponseHeaders()
    		);

        	String redirectedLocation = mLatestResult.getRedirectedLocation();
        	while (redirectedLocation != null && redirectedLocation.length() > 0
							&& !mLatestResult.isSuccess()) {
        		
        		isRedirectToNonSecureConnection |= (
        				baseUrlSt.startsWith("https://") && 
        				redirectedLocation.startsWith("http://")
				);
        		get.releaseConnection();
        		get = new GetMethod(redirectedLocation);
        		status = client.executeMethod(get, TRY_CONNECTION_TIMEOUT, TRY_CONNECTION_TIMEOUT);
        		mLatestResult = new RemoteOperationResult(
        				(status == HttpStatus.SC_OK), 
        				status, 
        				get.getResponseHeaders()
				); 
        		redirectedLocation = mLatestResult.getRedirectedLocation();
        	}

            String response = get.getResponseBodyAsString();
            if (status == HttpStatus.SC_OK) {
                JSONObject json = new JSONObject(response);
                if (!json.getBoolean(NODE_INSTALLED)) {
                    mLatestResult = new RemoteOperationResult(
                    		RemoteOperationResult.ResultCode.INSTANCE_NOT_CONFIGURED);
                } else {
                    String version = json.getString(NODE_VERSION);
					OwnCloudVersion ocVersion = new OwnCloudVersion(version);
                    if (!ocVersion.isVersionValid()) {
                        mLatestResult = new RemoteOperationResult(
                        		RemoteOperationResult.ResultCode.BAD_OC_VERSION);
                        
                    } else {
                    	// success
                    	if (isRedirectToNonSecureConnection) {
                    		mLatestResult = new RemoteOperationResult(
                    				RemoteOperationResult.ResultCode.
                    					OK_REDIRECT_TO_NON_SECURE_CONNECTION
        					);
                    	} else {
                    		mLatestResult = new RemoteOperationResult(
                    				baseUrlSt.startsWith("https://") ?
                    						RemoteOperationResult.ResultCode.OK_SSL :
                							RemoteOperationResult.ResultCode.OK_NO_SSL
							);
                		}

						ArrayList<Object> data = new ArrayList<Object>();
						data.add(ocVersion);
						mLatestResult.setData(data);
						retval = true;
                    }
                }
                
            } else {
                mLatestResult = new RemoteOperationResult(false, status, get.getResponseHeaders());
            }

        } catch (JSONException e) {
            mLatestResult = new RemoteOperationResult(
            		RemoteOperationResult.ResultCode.INSTANCE_NOT_CONFIGURED);
            
        } catch (Exception e) {
            mLatestResult = new RemoteOperationResult(e);
            
        } finally {
            if (get != null)
                get.releaseConnection();
        }
        
        if (mLatestResult.isSuccess()) {
            Log_OC.i(TAG, "Connection check at " + baseUrlSt + ": " + mLatestResult.getLogMessage());
            
        } else if (mLatestResult.getException() != null) {
            Log_OC.e(TAG, "Connection check at " + baseUrlSt + ": " + mLatestResult.getLogMessage(), 
            		mLatestResult.getException());
            
        } else {
            Log_OC.e(TAG, "Connection check at " + baseUrlSt + ": " + mLatestResult.getLogMessage());
        }

        return retval;
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
        if (!isOnline()) {
        	return new RemoteOperationResult(RemoteOperationResult.ResultCode.NO_NETWORK_CONNECTION);
        }
        String baseUriStr = client.getBaseUri().toString();
        if (baseUriStr.startsWith("http://") || baseUriStr.startsWith("https://")) {
            tryConnection(client);
            
        } else {
            client.setBaseUri(Uri.parse("https://" + baseUriStr));
            boolean httpsSuccess = tryConnection(client); 
			if (!httpsSuccess && !mLatestResult.isSslRecoverableException()) {
                Log_OC.d(TAG, "establishing secure connection failed, trying non secure connection");
                client.setBaseUri(Uri.parse("http://" + baseUriStr));
                tryConnection(client);
            }
        }
        return mLatestResult;
	}
	
}
