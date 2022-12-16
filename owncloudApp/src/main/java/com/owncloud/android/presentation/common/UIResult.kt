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

package com.owncloud.android.presentation.common

sealed class UIResult<out T> {
    data class Loading<out T>(val data: T? = null) : UIResult<T>()
    data class Success<out T>(val data: T? = null) : UIResult<T>()
    data class Error<out T>(val error: Throwable? = null, val data: T? = null) : UIResult<T>()

    val isLoading get() = this is Loading
    val isSuccess get() = this is Success
    val isError get() = this is Error

    @Deprecated(message = "Start to use new extensions")
    fun getStoredData(): T? =
        when (this) {
            is Loading -> data
            is Success -> data
            is Error -> data  // Even when there's an error we still want to show database data
        }

    fun getThrowableOrNull(): Throwable? =
        when (this) {
            is Error -> error
            else -> null
        }
}

fun <T> UIResult<T>.onLoading(action: (data: T?) -> Unit): UIResult<T> {
    if (this is UIResult.Loading) action(data)
    return this
}

fun <T> UIResult<T>.onSuccess(action: (data: T?) -> Unit): UIResult<T> {
    if (this is UIResult.Success) action(data)
    return this
}

fun <T> UIResult<T>.onError(action: (error: Throwable?) -> Unit): UIResult<T> {
    if (this is UIResult.Error) action(error)
    return this
}

fun <T> UIResult<T>.fold(
    onLoading: (data: T?) -> Unit,
    onSuccess: (data: T?) -> Unit,
    onFailure: (error: Throwable?) -> Unit
) {
    when (this) {
        is UIResult.Loading -> onLoading(data)
        is UIResult.Success -> onSuccess(data)
        is UIResult.Error -> onFailure(error)
    }
}
