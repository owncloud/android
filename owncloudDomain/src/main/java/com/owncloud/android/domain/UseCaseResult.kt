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

    enum class Status {
        SUCCESS,
        ERROR
    }

    fun isSuccess(): Boolean = code?.equals(RemoteOperationResult.ResultCode.OK) ?: false
}
