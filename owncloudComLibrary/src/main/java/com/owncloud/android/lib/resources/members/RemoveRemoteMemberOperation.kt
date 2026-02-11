/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2025 ownCloud GmbH.
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
import com.owncloud.android.lib.common.http.methods.nonwebdav.DeleteMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import timber.log.Timber
import java.net.URL

class RemoveRemoteMemberOperation(
    private val spaceId: String,
    private val memberId: String
): RemoteOperation<Unit>() {
    override fun run(client: OwnCloudClient): RemoteOperationResult<Unit> {
        var result: RemoteOperationResult<Unit>
        try {
            val uriBuilder = client.baseUri.buildUpon().apply {
                appendEncodedPath(GRAPH_API_SPACES_PATH)
                appendEncodedPath(spaceId)
                appendEncodedPath(GRAPH_API_ROOT_PERMISSIONS_PATH)
                appendEncodedPath(memberId)
            }

            val deleteMethod = DeleteMethod(URL(uriBuilder.build().toString()))

            val status = client.executeHttpMethod(deleteMethod)

            val response = deleteMethod.getResponseBodyAsString()

            if (status == HttpConstants.HTTP_NO_CONTENT) {
                Timber.d("Successful response: $response")
                result = RemoteOperationResult(ResultCode.OK)
            } else {
                result = RemoteOperationResult(deleteMethod)
                Timber.e("Failed response while removing a member; status code: $status, response: $response")
            }
        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Timber.e(e, "Exception while removing a member")
        }
        return result
    }

    companion object {
        private const val GRAPH_API_SPACES_PATH = "graph/v1beta1/drives/"
        private const val GRAPH_API_ROOT_PERMISSIONS_PATH = "root/permissions"
    }
}
