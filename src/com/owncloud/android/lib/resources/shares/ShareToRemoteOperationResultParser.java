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

import android.net.Uri;

import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ShareToRemoteOperationResultParser {

    private static final String TAG = ShareToRemoteOperationResultParser.class.getSimpleName();

    private ShareXMLParser mShareXmlParser = null;
    private boolean mOneOrMoreSharesRequired = false;
    private OwnCloudVersion mOwnCloudVersion = null;
    private Uri mServerBaseUri = null;


    public ShareToRemoteOperationResultParser(ShareXMLParser shareXmlParser) {
        mShareXmlParser = shareXmlParser;
    }

    public void setOneOrMoreSharesRequired(boolean oneOrMoreSharesRequired) {
        mOneOrMoreSharesRequired = oneOrMoreSharesRequired;
    }

    public void setOwnCloudVersion(OwnCloudVersion ownCloudVersion) {
        mOwnCloudVersion = ownCloudVersion;
    }

    public void setServerBaseUri(Uri serverBaseURi) {
        mServerBaseUri = serverBaseURi;
    }

    public RemoteOperationResult parse(String serverResponse) {
        if (serverResponse == null || serverResponse.length() == 0) {
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE);
        }

        RemoteOperationResult result = null;
        ArrayList<Object> resultData = new ArrayList<Object>();

        try {
            // Parse xml response and obtain the list of shares
            InputStream is = new ByteArrayInputStream(serverResponse.getBytes());
            if (mShareXmlParser == null) {
                Log_OC.w(TAG, "No ShareXmlParser provided, creating new instance ");
                mShareXmlParser = new ShareXMLParser();
            }
            List<OCShare> shares = mShareXmlParser.parseXMLResponse(is);

            if (mShareXmlParser.isSuccess()) {
                if ((shares != null && shares.size() > 0) || !mOneOrMoreSharesRequired) {
                    result = new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
                    if (shares != null) {
                        for (OCShare share : shares) {
                            resultData.add(share);
                            // build the share link if not in the response (only received when the share is created)
                            if (share.getShareType() == ShareType.PUBLIC_LINK &&
                                    (share.getShareLink() == null ||
                                            share.getShareLink().length() <= 0) &&
                                    share.getToken().length() > 0
                                    ) {
                                if (mServerBaseUri != null) {
                                    String sharingLinkPath = ShareUtils.getSharingLinkPath(mOwnCloudVersion);
                                    share.setShareLink(mServerBaseUri + sharingLinkPath + share.getToken());
                                } else {
                                    Log_OC.e(TAG, "Couldn't build link for public share");
                                }
                            }
                        }
                    }
                    result.setData(resultData);

                } else {
                    result = new RemoteOperationResult(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE);
                    Log_OC.e(TAG, "Successful status with no share in the response");
                }

            } else if (mShareXmlParser.isWrongParameter()){
                result = new RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_WRONG_PARAMETER);
                resultData.add(mShareXmlParser.getMessage());
                result.setData(resultData);

            } else if (mShareXmlParser.isNotFound()){
                result = new RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND);
                resultData.add(mShareXmlParser.getMessage());
                result.setData(resultData);

            } else if (mShareXmlParser.isForbidden()) {
                result = new RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_FORBIDDEN);
                resultData.add(mShareXmlParser.getMessage());
                result.setData(resultData);

            } else {
                result = new RemoteOperationResult(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE);

            }

        } catch (XmlPullParserException e) {
            Log_OC.e(TAG, "Error parsing response from server ", e);
            result = new RemoteOperationResult(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE);

        } catch (IOException e) {
            Log_OC.e(TAG, "Error reading response from server ", e);
            result = new RemoteOperationResult(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE);
        }

        return result;
    }

}
