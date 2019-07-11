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

package com.owncloud.android.presentation

data class UIResult<out T>(
    val status: Status,
    val data: T? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun <T> success(data: T? = null): UIResult<T> {
            return UIResult(Status.SUCCESS, data)
        }

        fun <T> error(
            data: T? = null,
            errorMessage: String? = null
        ): UIResult<T> {
            return UIResult(Status.ERROR, data, errorMessage)
        }

        fun <T> loading(data: T? = null): UIResult<T> {
            return UIResult(Status.LOADING, data = data)
        }
    }

    enum class Status {
        SUCCESS,
        LOADING,
        ERROR
    }
}
