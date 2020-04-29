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

import at.bitfire.dav4android.Property
import at.bitfire.dav4android.property.QuotaAvailableBytes
import at.bitfire.dav4android.property.QuotaUsedBytes
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
    override fun run(client: OwnCloudClient): RemoteOperationResult<RemoteQuota> {
        lateinit var result: RemoteOperationResult<RemoteQuota>
        try {
            val propfindMethod = PropfindMethod(
                URL(client.userFilesWebDavUri.toString()),
                DavConstants.DEPTH_0,
                DavUtils.getQuotaPropSet()
            )
            val status = client.executeHttpMethod(propfindMethod)
            if (isSuccess(status)) {
                val remoteQuota = readData(propfindMethod.root.properties)
                result = RemoteOperationResult(ResultCode.OK)

                // Add data to the result
                if (result.isSuccess) {
                    result.data = remoteQuota
                }
            } else { // synchronization failed
                result = RemoteOperationResult(propfindMethod)
            }
        } catch (e: Exception) {
            result = RemoteOperationResult(e)
        } finally {
            if (result.isSuccess) {
                Timber.i("Get quota completed: ${result.data} and message: ${result.logMessage}")
            } else {
                if (result.isException) {
                    Timber.e(result.exception, "Get quota: ${result.logMessage}")
                } else {
                    Timber.e("Get quota without success: ${result.logMessage}")
                }
            }
        }
        return result
    }

    private fun isSuccess(status: Int) = status == HttpConstants.HTTP_MULTI_STATUS || status == HttpConstants.HTTP_OK

    /**
     * Read the data retrieved from the server about the quota
     *
     * @param properties WebDAV properties containing quota data
     * @return new [RemoteQuota] instance representing the data read from the server
     */
    private fun readData(properties: List<Property>): RemoteQuota {
        var quotaAvailable: Long = 0
        var quotaUsed: Long = 0
        for (property in properties) {
            if (property is QuotaAvailableBytes) {
                quotaAvailable = property.quotaAvailableBytes
            }
            if (property is QuotaUsedBytes) {
                quotaUsed = property.quotaUsedBytes
            }
        }

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
            val relativeQuota = (quotaUsed * 100).toDouble() / totalQuota
            val roundedRelativeQuota = (relativeQuota * 100).roundToLong() / 100.0
            RemoteQuota(quotaAvailable, quotaUsed, totalQuota, roundedRelativeQuota)
        }
    }

    data class RemoteQuota(var free: Long, var used: Long, var total: Long, var relative: Double)
}
