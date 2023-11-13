package com.owncloud.android.lib.resources.files

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.nonwebdav.HeadMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import timber.log.Timber
import java.net.URL

class GetPathForFileId(val fileId: String) : RemoteOperation<String>() {
    override fun run(client: OwnCloudClient): RemoteOperationResult<String> {

        return try {
            val stringUrl = "${client.baseUri}${PATH}$fileId"
            val headMethod = HeadMethod(URL(stringUrl))
            val status = client.executeHttpMethod(headMethod)
            if (isSuccess(status)) {
                RemoteOperationResult<String>(RemoteOperationResult.ResultCode.OK).apply {
                    val fileName = headMethod.response.request.url.queryParameter(SCROLL_TO_QUERY)
                    data = if (fileName != null) {
                        "${headMethod.response.request.url.queryParameter(DIR_QUERY)}/$fileName"
                    } else {
                        headMethod.response.request.url.queryParameter(DIR_QUERY)
                    }
                }
            } else {
                RemoteOperationResult<String>(headMethod)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error while execute head of get file method.")
            RemoteOperationResult<String>(e)
        }
    }

    private fun isSuccess(status: Int) = status == HttpConstants.HTTP_OK || status == HttpConstants.HTTP_MULTI_STATUS

    companion object {
        private const val PATH = "/f/"
        private const val DIR_QUERY = "dir"
        private const val SCROLL_TO_QUERY = "scrollto"
    }

}
