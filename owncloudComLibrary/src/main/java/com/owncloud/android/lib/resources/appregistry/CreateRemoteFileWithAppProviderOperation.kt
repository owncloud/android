/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.lib.resources.appregistry

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.methods.nonwebdav.PostMethod
import com.owncloud.android.lib.common.network.WebdavUtils
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.FormBody
import okhttp3.RequestBody
import timber.log.Timber
import java.net.URL
import java.util.concurrent.TimeUnit

class CreateRemoteFileWithAppProviderOperation(
    private val createFileWithAppProviderEndpoint: String,
    private val parentContainerId: String,
    private val filename: String,
) : RemoteOperation<String>() {

    override fun run(client: OwnCloudClient): RemoteOperationResult<String> {
        return try {

            val createFileWithAppProviderRequestBody = CreateFileWithAppProviderParams(parentContainerId, filename)
                .toRequestBody()

            val stringUrl = client.baseUri.toString() + WebdavUtils.encodePath(createFileWithAppProviderEndpoint)

            val postMethod = PostMethod(URL(stringUrl), createFileWithAppProviderRequestBody).apply {
                setReadTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                setConnectionTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            }

            val status = client.executeHttpMethod(postMethod)
            Timber.d("Create file $filename with app provider in folder with ID $parentContainerId - $status${if (!isSuccess(status)) "(FAIL)" else ""}")

            if (isSuccess(status)) RemoteOperationResult<String>(ResultCode.OK).apply {
                val moshi = Moshi.Builder().build()
                val adapter: JsonAdapter<CreateFileWithAppProviderResponse> = moshi.adapter(CreateFileWithAppProviderResponse::class.java)

                data = postMethod.getResponseBodyAsString()?.let { adapter.fromJson(it)!!.fileId }
            }
            else RemoteOperationResult<String>(postMethod).apply { data = "" }

        } catch (e: Exception) {
            val result = RemoteOperationResult<String>(e)
            Timber.e(e, "Create file $filename with app provider in folder with ID $parentContainerId failed")
            result
        }
    }

    private fun isSuccess(status: Int) = status == HttpConstants.HTTP_OK

    data class CreateFileWithAppProviderParams(
        val parentContainerId: String,
        val filename: String,
    ) {
        fun toRequestBody(): RequestBody =
            FormBody.Builder()
                .add(PARAM_PARENT_CONTAINER_ID, parentContainerId)
                .add(PARAM_FILENAME, filename)
                .build()

        companion object {
            const val PARAM_PARENT_CONTAINER_ID = "parent_container_id"
            const val PARAM_FILENAME = "filename"
        }
    }

    @JsonClass(generateAdapter = true)
    data class CreateFileWithAppProviderResponse(
        @Json(name = "file_id")
        val fileId: String,
    )

    companion object {
        private const val TIMEOUT: Long = 5_000
    }
}
