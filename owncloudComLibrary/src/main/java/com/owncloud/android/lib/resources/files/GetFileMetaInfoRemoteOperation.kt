package com.owncloud.android.lib.resources.files

import at.bitfire.dav4jvm.PropStat
import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyRegistry
import at.bitfire.dav4jvm.Response
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.webdav.DavConstants
import com.owncloud.android.lib.common.http.methods.webdav.PropfindMethod
import com.owncloud.android.lib.common.http.methods.webdav.properties.OCMetaPathForUser
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import timber.log.Timber
import java.net.URL
import java.util.concurrent.TimeUnit

class GetFileMetaInfoRemoteOperation(val fileId: String) : RemoteOperation<String>() {


    override fun run(client: OwnCloudClient): RemoteOperationResult<String> {
        PropertyRegistry.register(OCMetaPathForUser.Factory())

        val stringUrl = "${client.baseUri}$META_PATH$fileId"
        return try {

            val propFindMethod =
                PropfindMethod(URL(stringUrl), DavConstants.DEPTH_0, arrayOf(OCMetaPathForUser.NAME)).apply {
                    setReadTimeout(TIMEOUT, TimeUnit.SECONDS)
                    setConnectionTimeout(TIMEOUT, TimeUnit.SECONDS)
                }

            val status = client.executeHttpMethod(propFindMethod)
            if (isSuccess(status)) RemoteOperationResult<String>(RemoteOperationResult.ResultCode.OK).apply {
                data = propFindMethod.root?.properties?.find { property -> property is OCMetaPathForUser }
                    ?.let { property -> (property as OCMetaPathForUser).path } ?: ""
            }
            else RemoteOperationResult<String>(propFindMethod)
        } catch (e: Exception) {
            Timber.e(e, "Could not get actual (or redirected) base URL from base url (/).")
            RemoteOperationResult<String>(e)
        }
    }

    private fun isSuccess(status: Int) = status == HttpConstants.HTTP_OK || status == HttpConstants.HTTP_MULTI_STATUS

    private fun PropStat.isSuccessOrPostProcessing() = (status.code / 100 == 2 || status.code == HttpConstants.HTTP_TOO_EARLY)

    companion object {
        private const val META_PATH = "/remote.php/dav/meta/"
        private const val TIMEOUT = 10_000L
    }

}
