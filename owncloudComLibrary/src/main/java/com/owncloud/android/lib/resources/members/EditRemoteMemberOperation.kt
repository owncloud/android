/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2026 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.lib.resources.members

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.http.HttpConstants
import com.owncloud.android.lib.common.http.HttpConstants.CONTENT_TYPE_JSON
import com.owncloud.android.lib.common.http.methods.nonwebdav.PatchMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.net.URL

class EditRemoteMemberOperation(
    private val spaceId: String,
    private val memberId: String,
    private val roleId: String,
    private val expirationDate: String?
): RemoteOperation<Unit>() {
    override fun run(client: OwnCloudClient): RemoteOperationResult<Unit> {
        var result: RemoteOperationResult<Unit>
        try {
            val uriBuilder = client.baseUri.buildUpon().apply {
                appendEncodedPath(GRAPH_API_DRIVES_PATH)
                appendEncodedPath(spaceId)
                appendEncodedPath(GRAPH_API_INVITE_PATH)
                appendEncodedPath(memberId)
            }

            val requestBody = JSONObject().apply {
                put(EXPIRATION_DATE_BODY_PARAM, expirationDate ?: JSONObject.NULL)
                put(ROLES_BODY_PARAM, JSONArray().apply { put(roleId) })
            }.toString().toRequestBody(CONTENT_TYPE_JSON.toMediaType())

            val patchMethod = PatchMethod(URL(uriBuilder.build().toString()), requestBody)

            val status = client.executeHttpMethod(patchMethod)

            val response = patchMethod.getResponseBodyAsString()

            if (status == HttpConstants.HTTP_OK) {
                Timber.d("Successful response: $response")
                result = RemoteOperationResult(ResultCode.OK)
                Timber.d("Edit member operation completed and parsed to ${result.data}")
            } else {
                result = RemoteOperationResult(patchMethod)
                Timber.e("Failed response while editing a member; status code: $status, response: $response")
            }
        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Exception while editing a member")
        }
        return result
    }

    companion object {
        private const val GRAPH_API_DRIVES_PATH = "graph/v1beta1/drives/"
        private const val GRAPH_API_INVITE_PATH = "root/permissions"
        private const val EXPIRATION_DATE_BODY_PARAM = "expirationDateTime"
        private const val ROLES_BODY_PARAM = "roles"
    }
}
