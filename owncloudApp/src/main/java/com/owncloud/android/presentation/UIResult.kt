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

import com.owncloud.android.domain.UseCaseResult

sealed class UIResult<out T> {
    companion object {
        fun <T> fromUseCaseResult(useCaseResult: UseCaseResult<T>): UIResult<T> =
            if (useCaseResult.isSuccess) {
                Success(useCaseResult.getDataOrNull())
            } else {
                Error(useCaseResult.getThrowableOrNull())
            }
    }

    data class Loading<out T>(val data: T? = null) : UIResult<T>()
    data class Success<out T>(val data: T? = null) : UIResult<T>()
    data class Error<out T>(val error: Throwable? = null) : UIResult<T>()

    fun getDataOrNull(): T? =
        when (this) {
            is Success -> data
            else -> null
        }

    fun getThrowableOrNull(): Throwable? =
        when (this) {
            is Error -> error
            else -> null
        }

    fun getLoadingOrNull(): T? =
        when (this) {
            is Loading -> data
            else -> null
        }
}
