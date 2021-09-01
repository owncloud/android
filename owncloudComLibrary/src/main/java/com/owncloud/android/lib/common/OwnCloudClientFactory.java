/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2020 ownCloud GmbH.
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

package com.owncloud.android.lib.common;

import android.content.Context;
import android.net.Uri;

import com.owncloud.android.lib.common.http.HttpClient;
import com.owncloud.android.lib.resources.status.GetRemoteStatusOperation;

public class OwnCloudClientFactory {

    /**
     * Creates a OwnCloudClient to access a URL and sets the desired parameters for ownCloud
     * client connections.
     *
     * @param uri     URL to the ownCloud server; BASE ENTRY POINT, not WebDavPATH
     * @param context Android context where the OwnCloudClient is being created.
     * @return A OwnCloudClient object ready to be used
     */
    public static OwnCloudClient createOwnCloudClient(Uri uri, Context context, boolean followRedirects) {
        OwnCloudClient client = new OwnCloudClient(uri);
        client.setFollowRedirects(followRedirects);
        HttpClient.setContext(context);

        return client;
    }
}
