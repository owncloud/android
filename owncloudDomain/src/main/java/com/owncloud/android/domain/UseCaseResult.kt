/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2020 ownCloud GmbH.
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

sealed class UseCaseResult<out T> {
    data class Success<out T>(val data: T) : UseCaseResult<T>()
    data class Error<out T>(val throwable: Throwable) : UseCaseResult<T>()

    val isSuccess get() = this is Success
    val isError get() = this is Error

    fun getDataOrNull(): T? =
        when (this) {
            is Success -> data
            else -> null
        }

    fun getThrowableOrNull(): Throwable? =
        when (this) {
            is Error -> throwable
            else -> null
        }
}
