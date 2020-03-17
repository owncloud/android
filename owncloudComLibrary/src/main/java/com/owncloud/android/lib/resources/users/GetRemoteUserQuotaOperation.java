/* ownCloud Android Library is available under MIT license
 *
 *   Copyright (C) 2020 ownCloud Inc.
 *   Copyright (C) 2015 Bartosz Przybylski
 *   Copyright (C) 2014 Marcello Steiner
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

import at.bitfire.dav4android.Property;
import at.bitfire.dav4android.property.QuotaAvailableBytes;
import at.bitfire.dav4android.property.QuotaUsedBytes;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.webdav.DavUtils;
import com.owncloud.android.lib.common.http.methods.webdav.PropfindMethod;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import timber.log.Timber;

import java.net.URL;
import java.util.List;

import static com.owncloud.android.lib.common.http.methods.webdav.DavConstants.DEPTH_0;
import static com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK;

/**
 * @author marcello
 * @author David González Verdugo
 */
public class GetRemoteUserQuotaOperation extends RemoteOperation<GetRemoteUserQuotaOperation.RemoteQuota> {

    private String mRemotePath;

    /**
     * Constructor
     *
     * @param remotePath Remote path of the file.
     */
    public GetRemoteUserQuotaOperation(String remotePath) {
        mRemotePath = remotePath;
    }

    @Override
    protected RemoteOperationResult<RemoteQuota> run(OwnCloudClient client) {
        RemoteOperationResult<RemoteQuota> result = null;

        try {
            PropfindMethod propfindMethod = new PropfindMethod(
                    new URL(client.getUserFilesWebDavUri() + WebdavUtils.encodePath(mRemotePath)),
                    DEPTH_0,
                    DavUtils.getQuotaPropSet());

            int status = client.executeHttpMethod(propfindMethod);

            if (isSuccess(status)) {
                RemoteQuota remoteQuota = readData(propfindMethod.getRoot().getProperties());

                result = new RemoteOperationResult<>(OK);

                // Add data to the result
                if (result.isSuccess()) {
                    result.setData(remoteQuota);
                }

            } else { // synchronization failed
                result = new RemoteOperationResult<>(propfindMethod);
            }

        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);

        } finally {
            if (result.isSuccess()) {
                Timber.i("Get quota from " + mRemotePath + ": " + result.getLogMessage());
            } else {
                if (result.isException()) {
                    Timber.e(result.getException(), "Get quota from " + mRemotePath + ": " + result.getLogMessage());
                } else {
                    Timber.e("Get quota from " + mRemotePath + ": " + result.getLogMessage());
                }
            }
        }

        return result;
    }

    private boolean isSuccess(int status) {
        return status == HttpConstants.HTTP_MULTI_STATUS || status == HttpConstants.HTTP_OK;
    }

    /**
     * Read the data retrieved from the server about the quota
     *
     * @param properties WebDAV properties containing quota data
     * @return new {@link RemoteQuota} instance representing the data read from the server
     */
    private RemoteQuota readData(List<Property> properties) {
        long quotaAvailable = 0;
        long quotaUsed = 0;

        for (Property property : properties) {
            if (property instanceof QuotaAvailableBytes) {
                quotaAvailable = ((QuotaAvailableBytes) property).getQuotaAvailableBytes();
            }
            if (property instanceof QuotaUsedBytes) {
                quotaUsed = ((QuotaUsedBytes) property).getQuotaUsedBytes();
            }
        }

        // If there's a special case, quota available will contain a negative code
        // -1, PENDING: Not computed yet, e.g. external storage mounted but folder sizes need scanning
        // -2, UNKNOWN: Storage not accessible, e.g. external storage with no API to ask for the free space
        // -3, UNLIMITED: Quota using all the storage
        if (quotaAvailable < 0) {
            return new RemoteQuota(
                    quotaAvailable,
                    quotaUsed,
                    0,
                    0
            );
        } else {
            long totalQuota = quotaAvailable + quotaUsed;
            double relativeQuota = (double) (quotaUsed * 100) / totalQuota;
            double roundedRelativeQuota = Math.round(relativeQuota * 100) / 100.0d;

            return new RemoteQuota(
                    quotaAvailable,
                    quotaUsed,
                    totalQuota,
                    roundedRelativeQuota
            );
        }
    }

    static public class RemoteQuota {

        long mFree, mUsed, mTotal;
        double mRelative;

        public RemoteQuota(long free, long used, long total, double relative) {
            mFree = free;
            mUsed = used;
            mTotal = total;
            mRelative = relative;
        }

        public long getFree() {
            return mFree;
        }

        public long getUsed() {
            return mUsed;
        }

        public long getTotal() {
            return mTotal;
        }

        public double getRelative() {
            return mRelative;
        }
    }
}