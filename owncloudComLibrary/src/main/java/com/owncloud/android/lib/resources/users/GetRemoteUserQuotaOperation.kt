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
package com.owncloud.android.lib.resources.users

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.property.QuotaAvailableBytes
import at.bitfire.dav4jvm.property.QuotaUsedBytes
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.webdav.DavConstants
import com.owncloud.android.lib.common.http.methods.webdav.DavUtils
import com.owncloud.android.lib.common.http.methods.webdav.PropfindMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.users.GetRemoteUserQuotaOperation.RemoteQuota
import timber.log.Timber
import java.net.URL
import kotlin.math.roundToLong

/**
 * @author marcello
 * @author David González Verdugo
 */
class GetRemoteUserQuotaOperation : RemoteOperation<RemoteQuota>() {
    override fun run(client: OwnCloudClient): RemoteOperationResult<RemoteQuota> =
        try {
            val propfindMethod = PropfindMethod(
                client,
                URL(client.userFilesWebDavUri.toString()),
                DavConstants.DEPTH_0,
                DavUtils.quotaPropSet
            )
            with(client.executeHttpMethod(propfindMethod)) {
                if (isSuccess(this)) {
                    RemoteOperationResult<RemoteQuota>(ResultCode.OK).apply {
                        data = readData(propfindMethod.root?.properties)
                    }.also {
                        Timber.i("Get quota completed: ${it.data} and message: ${it.logMessage}")
                    }
                } else { // synchronization failed
                    RemoteOperationResult<RemoteQuota>(propfindMethod).also {
                        Timber.e("Get quota without success: ${it.logMessage}")
                    }
                }
            }
        } catch (e: Exception) {
            RemoteOperationResult<RemoteQuota>(e).also {
                Timber.e(it.exception, "Get quota: ${it.logMessage}")
            }
        }

    private fun isSuccess(status: Int) = status == HttpConstants.HTTP_MULTI_STATUS || status == HttpConstants.HTTP_OK

    /**
     * Read the data retrieved from the server about the quota
     *
     * @param properties WebDAV properties containing quota data
     * @return new [RemoteQuota] instance representing the data read from the server
     */
    private fun readData(properties: List<Property>?): RemoteQuota {
        var quotaAvailable: Long = 0
        var quotaUsed: Long = 0

        if (properties == null) {
            // Should not happen
            Timber.d("Unable to get quota")
            return RemoteQuota(0, 0, 0, 0.0)
        }

        for (property in properties) {
            if (property is QuotaAvailableBytes) {
                quotaAvailable = property.quotaAvailableBytes
            }
            if (property is QuotaUsedBytes) {
                quotaUsed = property.quotaUsedBytes
            }
        }
        Timber.d("Quota used: $quotaUsed, QuotaAvailable: $quotaAvailable")
        // If there's a special case, quota available will contain a negative code
        // -1, PENDING: Not computed yet, e.g. external storage mounted but folder sizes need scanning
        // -2, UNKNOWN: Storage not accessible, e.g. external storage with no API to ask for the free space
        // -3, UNLIMITED: Quota using all the storage
        return if (quotaAvailable < 0) {
            RemoteQuota(
                free = quotaAvailable,
                used = quotaUsed,
                total = 0,
                relative = 0.0
            )
        } else {
            val totalQuota = quotaAvailable + quotaUsed
            val roundedRelativeQuota = if (totalQuota > 0) {
                val relativeQuota = (quotaUsed * 100).toDouble() / totalQuota
                (relativeQuota * 100).roundToLong() / 100.0
            } else 0.0

            RemoteQuota(quotaAvailable, quotaUsed, totalQuota, roundedRelativeQuota)
        }
    }

    data class RemoteQuota(var free: Long, var used: Long, var total: Long, var relative: Double)
}
