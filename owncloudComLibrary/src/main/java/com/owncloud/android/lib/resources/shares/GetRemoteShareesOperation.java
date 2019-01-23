/* ownCloud Android Library is available under MIT license
 *
 *   @author masensio
 *   @author David A. Velasco
 *   @author David González Verdugo
 *   Copyright (C) 2019 ownCloud GmbH.
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

import android.net.Uri;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.nonwebdav.GetMethod;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;

import static com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK;

/**
 * Created by masensio on 08/10/2015.
 *
 * Retrieves a list of sharees (possible targets of a share) from the ownCloud server.
 *
 * Currently only handles users and groups. Users in other OC servers (federation) should be added later.
 *
 * Depends on SHAREE API. {@See https://github.com/owncloud/documentation/issues/1626}
 *
 * Syntax:
 *    Entry point: ocs/v2.php/apps/files_sharing/api/v1/sharees
 *    HTTP method: GET
 *    url argument: itemType - string, required
 *    url argument: format - string, optional
 *    url argument: search - string, optional
 *    url arguments: perPage - int, optional
 *    url arguments: page - int, optional
 *
 * Status codes:
 *    100 - successful
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 */
public class GetRemoteShareesOperation extends RemoteOperation<ArrayList<JSONObject>> {

    private static final String TAG = GetRemoteShareesOperation.class.getSimpleName();

    // OCS Routes
    private static final String OCS_ROUTE = "ocs/v2.php/apps/files_sharing/api/v1/sharees";    // from OC 8.2

    // Arguments - names
    private static final String PARAM_FORMAT = "format";
    private static final String PARAM_ITEM_TYPE = "itemType";
    private static final String PARAM_SEARCH = "search";
    private static final String PARAM_PAGE = "page";                //  default = 1
    private static final String PARAM_PER_PAGE = "perPage";         //  default = 200

    // Arguments - constant values
    private static final String VALUE_FORMAT = "json";
    private static final String VALUE_ITEM_TYPE = "file";         //  to get the server search for users / groups

    // JSON Node names
    private static final String NODE_OCS = "ocs";
    private static final String NODE_DATA = "data";
    private static final String NODE_EXACT = "exact";
    private static final String NODE_USERS = "users";
    private static final String NODE_GROUPS = "groups";
    private static final String NODE_REMOTES = "remotes";
    public static final String NODE_VALUE = "value";
    public static final String PROPERTY_LABEL = "label";
    public static final String PROPERTY_SHARE_TYPE = "shareType";
    public static final String PROPERTY_SHARE_WITH = "shareWith";
    public static final String PROPERTY_SHARE_WITH_ADDITIONAL_INFO = "shareWithAdditionalInfo";

    private String mSearchString;
    private int mPage;
    private int mPerPage;

    /**
     * Constructor
     *
     * @param searchString  	string for searching users, optional
     * @param page			    page index in the list of results; beginning in 1
     * @param perPage           maximum number of results in a single page
     */
    public GetRemoteShareesOperation(String searchString, int page, int perPage) {
        mSearchString = searchString;
        mPage = page;
        mPerPage = perPage;
    }

    @Override
    protected RemoteOperationResult<ArrayList<JSONObject>> run(OwnCloudClient client) {
        RemoteOperationResult<ArrayList<JSONObject>> result;

        try{
            Uri requestUri = client.getBaseUri();
            Uri.Builder uriBuilder = requestUri.buildUpon()
                    .appendEncodedPath(OCS_ROUTE)
                    .appendQueryParameter(PARAM_FORMAT, VALUE_FORMAT)
                    .appendQueryParameter(PARAM_ITEM_TYPE, VALUE_ITEM_TYPE)
                    .appendQueryParameter(PARAM_SEARCH, mSearchString)
                    .appendQueryParameter(PARAM_PAGE, String.valueOf(mPage))
                    .appendQueryParameter(PARAM_PER_PAGE, String.valueOf(mPerPage));

            GetMethod getMethod = new GetMethod(new URL(uriBuilder.build().toString()));

            getMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            int status = client.executeHttpMethod(getMethod);
            String response = getMethod.getResponseBodyAsString();

            if(isSuccess(status)) {
                Log_OC.d(TAG, "Successful response: " + response);

                // Parse the response
                JSONObject respJSON = new JSONObject(response);
                JSONObject respOCS = respJSON.getJSONObject(NODE_OCS);
                JSONObject respData = respOCS.getJSONObject(NODE_DATA);
                JSONObject respExact = respData.getJSONObject(NODE_EXACT);
                JSONArray respExactUsers = respExact.getJSONArray(NODE_USERS);
                JSONArray respExactGroups = respExact.getJSONArray(NODE_GROUPS);
                JSONArray respExactRemotes = respExact.getJSONArray(NODE_REMOTES);
                JSONArray respPartialUsers = respData.getJSONArray(NODE_USERS);
                JSONArray respPartialGroups = respData.getJSONArray(NODE_GROUPS);
                JSONArray respPartialRemotes = respData.getJSONArray(NODE_REMOTES);
                JSONArray[] jsonResults = {
                        respExactUsers,
                        respExactGroups,
                        respExactRemotes,
                        respPartialUsers,
                        respPartialGroups,
                        respPartialRemotes
                };

                ArrayList<JSONObject> data = new ArrayList<>(); // For result data
                for (int i=0; i<6; i++) {
                    for(int j=0; j< jsonResults[i].length(); j++){
                        JSONObject jsonResult = jsonResults[i].getJSONObject(j);
                        data.add(jsonResult);
                        Log_OC.d(TAG, "*** Added item: " + jsonResult.getString(PROPERTY_LABEL));
                    }
                }

                result = new RemoteOperationResult<>(OK);
                result.setData(data);

                Log_OC.d(TAG, "*** Get Users or groups completed " );

            } else {
                result = new RemoteOperationResult<>(getMethod);
                Log_OC.e(TAG, "Failed response while getting users/groups from the server ");
                if (response != null) {
                    Log_OC.e(TAG, "*** status code: " + status + "; response message: " + response);
                } else {
                    Log_OC.e(TAG, "*** status code: " + status);
                }
            }
        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);
            Log_OC.e(TAG, "Exception while getting users/groups", e);
        }

        return result;
    }

    private boolean isSuccess(int status) {
        return (status == HttpConstants.HTTP_OK);
    }
}