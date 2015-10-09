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

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by masensio on 08/10/2015.
 *
 * Retrieves a list of users or groups from the ownCloud server.
 * Authentication is done by sending a Basic HTTP Authorization header.
 * Syntax: ocs/v1.php/cloud/users (Users)
 *         ocs/v1.php/cloud/groups (Groups)
 *    HTTP method: GET
 *    url arguments: search - string, optional search string
 *    url arguments: limit - int, optional limit value
 *    url arguments: offset - int, optional offset value
 *
 * Status codes:
 *    100 - successful
 */
public class GetRemoteUsersOrGroupsOperation extends RemoteOperation{

    private static final String TAG = GetRemoteUsersOrGroupsOperation.class.getSimpleName();

    // OCS Routes
    private static final String OCS_ROUTE_USERS ="/ocs/v1.php/cloud/users?format=json";
    private static final String OCS_ROUTE_GROUPS ="/ocs/v1.php/cloud/groups?format=json";

    // Arguments
    private static final String PARAM_SEARCH = "search";
    private static final String PARAM_LIMIT = "limit";
    private static final String PARAM_OFFSET = "offset";

    // JSON Node names
    private static final String NODE_OCS = "ocs";
    private static final String NODE_DATA = "data";
    private static final String NODE_USERS = "users";
    private static final String NODE_GROUPS = "groups";

    private ArrayList<String> mNames;  // List of users or groups

    private String mSearchString;
    private int mLimit;
    private int mOffset;
    private boolean mGetGroup = false;

    /**
     * Constructor
     *
     * @param searchString  	string for searching users, optional
     * @param limit 			limit, optional
     * @param offset			offset, optional
     * @param getGroups         true: for searching groups, false: for searching users
     */
    public GetRemoteUsersOrGroupsOperation(String searchString, int limit, int offset,
                                           boolean getGroups) {
        mSearchString = searchString;
        mLimit = limit;
        mOffset = offset;
        mGetGroup = getGroups;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        int status = -1;
        GetMethod get = null;

        String ocs_route = OCS_ROUTE_USERS;
        String nodeUsersOrGroups = NODE_USERS;
        if(mGetGroup){
            ocs_route = OCS_ROUTE_GROUPS;
            nodeUsersOrGroups = NODE_GROUPS;
        }

        try{
            // Get Method
            get = new GetMethod(client.getBaseUri() + ocs_route);

            // Add Parameters to Get Method
            get.setQueryString(new NameValuePair[]{
                    new NameValuePair(PARAM_SEARCH, mSearchString),
                    new NameValuePair(PARAM_LIMIT, String.valueOf(mLimit)),
                    new NameValuePair(PARAM_OFFSET, String.valueOf(mOffset))
            });

            //¿?
            get.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            status = client.executeMethod(get);

            if(isSuccess(status)) {
                String response = get.getResponseBodyAsString();
                Log_OC.d(TAG, "Successful response: " + response);

                // Parse the response
                JSONObject respJSON = new JSONObject(response);
                JSONObject respOCS = respJSON.getJSONObject(NODE_OCS);
                JSONObject respData = respOCS.getJSONObject(NODE_DATA);
                JSONArray  respUsersOrGroups = respData.getJSONArray(nodeUsersOrGroups);
                mNames = new ArrayList<String>();
                ArrayList<Object> data = new ArrayList<Object>(); // For result data
                for(int i=0; i<= respUsersOrGroups.length(); i++){
                    JSONObject jsonUser = respUsersOrGroups.getJSONObject(i);
                    String name = jsonUser.toString();
                    mNames.add(name);
                    data.add(name);
                    Log_OC.d(TAG, "*** "+ nodeUsersOrGroups + " : " + name);
                }

                // Result
                result = new RemoteOperationResult(true, status, get.getResponseHeaders());
                result.setData(data);

                Log_OC.d(TAG, "*** Get Users or groups completed " );

            } else {
                result = new RemoteOperationResult(false, status, get.getResponseHeaders());
                String response = get.getResponseBodyAsString();
                Log_OC.e(TAG, "Failed response while getting users/groups from the server ");
                if (response != null) {
                    Log_OC.e(TAG, "*** status code: " + status + "; response message: " + response);
                } else {
                    Log_OC.e(TAG, "*** status code: " + status);
                }
            }

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Exception while getting users/groups", e);

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
