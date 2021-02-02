/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
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
package com.owncloud.android.utils

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import io.mockk.every
import io.mockk.mockk

fun <T> createRemoteOperationResultMock(
    data: T,
    isSuccess: Boolean,
    httpPhrase: String? = null,
    resultCode: RemoteOperationResult.ResultCode? = null,
    exception: Exception? = null,
    authenticationHeader: List<String> = listOf(),
    httpCode: Int? = null,
    redirectedLocation: String? = null
): RemoteOperationResult<T> {
    val remoteOperationResult = mockk<RemoteOperationResult<T>>(relaxed = true)

    every { remoteOperationResult.data } returns data

    every { remoteOperationResult.isSuccess } returns isSuccess

    if (httpPhrase != null) {
        every { remoteOperationResult.httpPhrase } returns httpPhrase
    }

    if (resultCode != null) {
        every { remoteOperationResult.code } returns resultCode
    }

    if (exception != null) {
        throw exception
    }

    if (authenticationHeader.isNotEmpty()) {
        every { remoteOperationResult.authenticateHeaders } returns authenticationHeader
    }

    if (httpCode != null) {
        every { remoteOperationResult.httpCode } returns httpCode
    }

    if (redirectedLocation != null) {
        every { remoteOperationResult.redirectedLocation } returns redirectedLocation
    }

    return remoteOperationResult
}
