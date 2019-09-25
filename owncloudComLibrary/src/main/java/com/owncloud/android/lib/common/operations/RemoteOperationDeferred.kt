/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.lib.common.operations

suspend fun <T> awaitToRemoteOperationResult(remoteOperation: suspend () -> RemoteOperationResult<T>): T {

    val remoteOperationResult = remoteOperation.invoke()

    if (remoteOperationResult.isSuccess) {
        return remoteOperationResult.data
    }

    // Errors
    when (remoteOperationResult.code) {
        RemoteOperationResult.ResultCode.UNHANDLED_HTTP_CODE -> throw Exception()
        RemoteOperationResult.ResultCode.UNAUTHORIZED -> throw Exception()
        RemoteOperationResult.ResultCode.FILE_NOT_FOUND -> throw Exception()
        RemoteOperationResult.ResultCode.INSTANCE_NOT_CONFIGURED -> throw Exception()
        RemoteOperationResult.ResultCode.UNKNOWN_ERROR -> throw Exception()
        RemoteOperationResult.ResultCode.WRONG_CONNECTION -> throw Exception()
        RemoteOperationResult.ResultCode.TIMEOUT -> throw Exception()
        RemoteOperationResult.ResultCode.INCORRECT_ADDRESS -> throw Exception()
        RemoteOperationResult.ResultCode.HOST_NOT_AVAILABLE -> throw Exception()
        RemoteOperationResult.ResultCode.NO_NETWORK_CONNECTION -> throw Exception()
        RemoteOperationResult.ResultCode.SSL_ERROR -> throw Exception()
        RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED -> throw Exception()
        RemoteOperationResult.ResultCode.BAD_OC_VERSION -> throw Exception()
        RemoteOperationResult.ResultCode.CANCELLED -> throw Exception()
        RemoteOperationResult.ResultCode.INVALID_LOCAL_FILE_NAME -> throw Exception()
        RemoteOperationResult.ResultCode.INVALID_OVERWRITE -> throw Exception()
        RemoteOperationResult.ResultCode.CONFLICT -> throw Exception()
        RemoteOperationResult.ResultCode.OAUTH2_ERROR -> throw Exception()
        RemoteOperationResult.ResultCode.SYNC_CONFLICT -> throw Exception()
        RemoteOperationResult.ResultCode.LOCAL_STORAGE_FULL -> throw Exception()
        RemoteOperationResult.ResultCode.LOCAL_STORAGE_NOT_MOVED -> throw Exception()
        RemoteOperationResult.ResultCode.LOCAL_STORAGE_NOT_COPIED -> throw Exception()
        RemoteOperationResult.ResultCode.OAUTH2_ERROR_ACCESS_DENIED -> throw Exception()
        RemoteOperationResult.ResultCode.QUOTA_EXCEEDED -> throw Exception()
        RemoteOperationResult.ResultCode.ACCOUNT_NOT_FOUND -> throw Exception()
        RemoteOperationResult.ResultCode.ACCOUNT_EXCEPTION -> throw Exception()
        RemoteOperationResult.ResultCode.ACCOUNT_NOT_NEW -> throw Exception()
        RemoteOperationResult.ResultCode.ACCOUNT_NOT_THE_SAME -> throw Exception()
        RemoteOperationResult.ResultCode.INVALID_CHARACTER_IN_NAME -> throw Exception()
        RemoteOperationResult.ResultCode.SHARE_NOT_FOUND -> throw Exception()
        RemoteOperationResult.ResultCode.LOCAL_STORAGE_NOT_REMOVED -> throw Exception()
        RemoteOperationResult.ResultCode.FORBIDDEN -> throw Exception()
        RemoteOperationResult.ResultCode.SHARE_FORBIDDEN -> throw Exception()
        RemoteOperationResult.ResultCode.SPECIFIC_FORBIDDEN -> throw Exception()
        RemoteOperationResult.ResultCode.OK_REDIRECT_TO_NON_SECURE_CONNECTION -> throw Exception()
        RemoteOperationResult.ResultCode.INVALID_MOVE_INTO_DESCENDANT -> throw Exception()
        RemoteOperationResult.ResultCode.INVALID_COPY_INTO_DESCENDANT -> throw Exception()
        RemoteOperationResult.ResultCode.PARTIAL_MOVE_DONE -> throw Exception()
        RemoteOperationResult.ResultCode.PARTIAL_COPY_DONE -> throw Exception()
        RemoteOperationResult.ResultCode.SHARE_WRONG_PARAMETER -> throw Exception()
        RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE -> throw Exception()
        RemoteOperationResult.ResultCode.INVALID_CHARACTER_DETECT_IN_SERVER -> throw Exception()
        RemoteOperationResult.ResultCode.DELAYED_FOR_WIFI -> throw Exception()
        RemoteOperationResult.ResultCode.LOCAL_FILE_NOT_FOUND -> throw Exception()
        RemoteOperationResult.ResultCode.SERVICE_UNAVAILABLE -> throw Exception()
        RemoteOperationResult.ResultCode.SPECIFIC_SERVICE_UNAVAILABLE -> throw Exception()
        RemoteOperationResult.ResultCode.SPECIFIC_UNSUPPORTED_MEDIA_TYPE -> throw Exception()
        RemoteOperationResult.ResultCode.SPECIFIC_METHOD_NOT_ALLOWED -> throw Exception()
        else -> throw Exception()
    }
}
