/* ownCloud Android Library is available under MIT license
 *
 *   Copyright (C) 2018 ownCloud Inc.
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

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.WebdavEntry;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

/**
 * @author marcello
 * @author David González Verdugo
 */
public class GetRemoteUserQuotaOperation extends RemoteOperation {

    static public class Quota {

        // Not computed yet, e.g. external storage mounted but folder sizes need scanning
        public static final int PENDING_FREE_QUOTA = -1;

        // Storage not accessible, e.g. external storage with no API to ask for the free space
        public static final int UNKNOWN_FREE_QUOTA = -2;

        // Quota using all the storage
        public static final int UNLIMITED_FREE_QUOTA = -3;

        long mFree, mUsed, mTotal;
        double mRelative;

        public Quota(long free, long used, long total, double relative) {
            mFree = free;
            mUsed = used;
            mTotal = total;
            mRelative = relative;
        }

        public long getFree() { return mFree; }
        public long getUsed() { return mUsed; }
        public long getTotal() { return mTotal; }
        public double getRelative() { return mRelative; }
    }

    private static final String TAG = GetRemoteUserQuotaOperation.class.getSimpleName();

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
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        PropFindMethod query = null;

        try {
            // remote request
            query = new PropFindMethod(client.getWebdavUri() + WebdavUtils.encodePath(mRemotePath),
                    WebdavUtils.getQuotaPropSet(),
                    DavConstants.DEPTH_0);

            int status = client.executeMethod(query);

            if (isSuccess(status)) {
                // get data from remote folder
                MultiStatus dataInServer = query.getResponseBodyAsMultiStatus();
                Quota quota = readData(dataInServer, client);

                // Result of the operation
                result = new RemoteOperationResult(true, query);

                ArrayList<Object> data = new ArrayList<>();
                data.add(quota);

                // Add data to the result
                if (result.isSuccess()) {
                    result.setData(data);
                }
            } else {
                // synchronization failed
                result = new RemoteOperationResult(false, query);
            }

        } catch (Exception e) {
            result = new RemoteOperationResult(e);


        } finally {
            if (query != null)
                query.releaseConnection();  // let the connection available for other methods
            if (result.isSuccess()) {
                Log_OC.i(TAG, "Get quota from " + mRemotePath + ": " + result.getLogMessage());
            } else {
                if (result.isException()) {
                    Log_OC.e(TAG, "Get quota from " + mRemotePath + ": " + result.getLogMessage(),
                            result.getException());
                } else {
                    Log_OC.e(TAG, "Get quota from " + mRemotePath + ": " + result.getLogMessage());
                }
            }
        }

        return result;
    }

    private boolean isSuccess(int status) {
        return status == HttpStatus.SC_MULTI_STATUS || status == HttpStatus.SC_OK;
    }

    /**
     * Read the data retrieved from the server about the quota
     *
     * @param remoteData Full response got from the server with the data of the quota
     * @param client     Client instance to the remote server where the data were retrieved
     * @return new Quota instance representing the data read from the server
     */
    private Quota readData(MultiStatus remoteData, OwnCloudClient client) {

        // parse data from remote folder
        WebdavEntry we = new WebdavEntry(remoteData.getResponses()[0], client.getWebdavUri().getPath());

        // If there's a special case, available bytes will contain a negative code
        // -1, PENDING: Not computed yet, e.g. external storage mounted but folder sizes need scanning
        // -2, UNKNOWN: Storage not accessible, e.g. external storage with no API to ask for the free space
        // -3, UNLIMITED: Quota using all the storage
        if (we.quotaAvailableBytes().compareTo(new BigDecimal(1)) == -1) {
            return new Quota(
                    we.quotaAvailableBytes().longValue(),
                    we.quotaUsedBytes().longValue(),
                    0,
                    0
            );

        } else {

            BigDecimal totalQuota = we.quotaAvailableBytes().add(we.quotaUsedBytes());

            BigDecimal relativeQuota = we.quotaUsedBytes()
                    .multiply(new BigDecimal(100))
                    .divide(totalQuota, 2, RoundingMode.HALF_UP);

            return new Quota(
                    we.quotaAvailableBytes().longValue(),
                    we.quotaUsedBytes().longValue(),
                    totalQuota.longValue(),
                    relativeQuota.doubleValue()
            );
        }
    }
}
