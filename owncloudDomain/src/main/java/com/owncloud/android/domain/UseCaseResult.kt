package com.owncloud.android.domain

import com.owncloud.android.lib.common.operations.RemoteOperationResult

data class UseCaseResult<out T>(
    val status: Status,
    val code: RemoteOperationResult.ResultCode? = null,
    val data: T? = null,
    val msg: String? = null,
    val exception: Exception? = null
) {
    companion object {
        fun <T> success(data: T? = null): UseCaseResult<T> {
            return UseCaseResult(Status.SUCCESS, RemoteOperationResult.ResultCode.OK, data)
        }

        fun <T> error(
            code: RemoteOperationResult.ResultCode? = null,
            data: T? = null,
            msg: String? = null,
            exception: Exception? = null
        ): UseCaseResult<T> {
            return UseCaseResult(Status.ERROR, code, data, msg, exception)
        }
    }

    fun isSuccess(): Boolean = code?.equals(RemoteOperationResult.ResultCode.OK) ?: false
}
