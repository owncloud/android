package com.owncloud.android.lib.resources.files

import android.content.ContentResolver
import android.net.Uri
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.webdav.PutMethod
import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import timber.log.Timber
import java.io.IOException
import java.net.URL

class UploadFileFromContentUriOperation(
    private val uploadPath: String,
    private val lastModified: String,
    private val requestBody: ContentUriRequestBody
) : RemoteOperation<Unit>() {

    override fun run(client: OwnCloudClient): RemoteOperationResult<Unit> {
        val putMethod = PutMethod(URL(client.userFilesWebDavUri.toString() + WebdavUtils.encodePath(uploadPath)), requestBody).apply {
            setRetryOnConnectionFailure(false)
            addRequestHeader(HttpConstants.OC_TOTAL_LENGTH_HEADER, requestBody.contentLength().toString())
            addRequestHeader(HttpConstants.OC_X_OC_MTIME_HEADER, lastModified)
        }

        return try {
            val status = client.executeHttpMethod(putMethod)
            if (isSuccess(status)) {
                RemoteOperationResult<Unit>(RemoteOperationResult.ResultCode.OK)
            } else {
                RemoteOperationResult<Unit>(putMethod)
            }
        } catch (e: Exception) {
            val result = RemoteOperationResult<Unit>(e)
            Timber.e(e, "Upload from content uri failed : ${result.logMessage}")
            result
        }
    }

    fun isSuccess(status: Int): Boolean {
        return status == HttpConstants.HTTP_OK || status == HttpConstants.HTTP_CREATED || status == HttpConstants.HTTP_NO_CONTENT
    }
}

class ContentUriRequestBody(
    private val contentResolver: ContentResolver,
    private val contentUri: Uri
) : RequestBody() {

    override fun contentType(): MediaType? {
        val contentType = contentResolver.getType(contentUri) ?: return null
        return contentType.toMediaTypeOrNull()
    }

    override fun writeTo(sink: BufferedSink) {
        val inputStream = contentResolver.openInputStream(contentUri)
            ?: throw IOException("Couldn't open content URI for reading: $contentUri")

        inputStream.source().use { source ->
            sink.writeAll(source)
        }
    }
}
