/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2018 ownCloud GmbH.
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
 */

package com.owncloud.android.lib.refactor.operations.files;

import com.owncloud.android.lib.refactor.OCContext;
import com.owncloud.android.lib.refactor.operations.RemoteOperation;
import at.bitfire.dav4android.DavOCResource;
import static com.owncloud.android.lib.refactor.operations.RemoteOperationResult.ResultCode.OK;

/**
 * Remote operation performing the download of a remote file in the ownCloud server.
 *
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 */
public class DownloadRemoteFileOperation extends RemoteOperation<Void> {

    private String mRemotePath;
    private String mLocalFolderPath;

    public DownloadRemoteFileOperation(OCContext ocContext, String remotePath, String localFolderPath) {
        super(ocContext);
        mRemotePath = remotePath.replaceAll("^/+", "");
        mLocalFolderPath = localFolderPath;
    }

    @Override
    public Result exec() {

        try {
            DavOCResource davOCResource = new DavOCResource(
                    getClient(),
                    getWebDavHttpUrl(mRemotePath)
            );
            davOCResource.get("*/*");

            //TODO Create local file from the downloaded one and implement progress listener

            return new Result(OK);

        } catch (Exception e) {
            return new Result(e);
        }
    }
}