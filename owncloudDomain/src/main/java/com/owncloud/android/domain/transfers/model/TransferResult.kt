/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2022 ownCloud GmbH.
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

package com.owncloud.android.domain.transfers.model

import com.owncloud.android.domain.exceptions.ConflictException
import com.owncloud.android.domain.exceptions.FileNotFoundException
import com.owncloud.android.domain.exceptions.ForbiddenException
import com.owncloud.android.domain.exceptions.LocalFileNotFoundException
import com.owncloud.android.domain.exceptions.NetworkErrorException
import com.owncloud.android.domain.exceptions.NoConnectionWithServerException
import com.owncloud.android.domain.exceptions.QuotaExceededException
import com.owncloud.android.domain.exceptions.SSLRecoverablePeerUnverifiedException
import com.owncloud.android.domain.exceptions.ServiceUnavailableException
import com.owncloud.android.domain.exceptions.SpecificForbiddenException
import com.owncloud.android.domain.exceptions.SpecificServiceUnavailableException
import com.owncloud.android.domain.exceptions.SpecificUnsupportedMediaTypeException
import com.owncloud.android.domain.exceptions.UnauthorizedException

enum class TransferResult constructor(val value: Int) {
    UNKNOWN(value = -1),
    UPLOADED(value = 0),
    NETWORK_CONNECTION(value = 1),
    CREDENTIAL_ERROR(value = 2),
    FOLDER_ERROR(value = 3),
    CONFLICT_ERROR(value = 4),
    FILE_ERROR(value = 5),
    PRIVILEGES_ERROR(value = 6),
    CANCELLED(value = 7),
    FILE_NOT_FOUND(value = 8),
    DELAYED_FOR_WIFI(value = 9),
    SERVICE_INTERRUPTED(value = 10),
    SERVICE_UNAVAILABLE(value = 11),
    QUOTA_EXCEEDED(value = 12),
    SSL_RECOVERABLE_PEER_UNVERIFIED(value = 13),
    SPECIFIC_FORBIDDEN(value = 14),
    SPECIFIC_SERVICE_UNAVAILABLE(value = 15),
    SPECIFIC_UNSUPPORTED_MEDIA_TYPE(value = 16);

    companion object {
        fun fromValue(value: Int): TransferResult =
            when (value) {
                0 -> UPLOADED
                1 -> NETWORK_CONNECTION
                2 -> CREDENTIAL_ERROR
                3 -> FOLDER_ERROR
                4 -> CONFLICT_ERROR
                5 -> FILE_ERROR
                6 -> PRIVILEGES_ERROR
                7 -> CANCELLED
                8 -> FILE_NOT_FOUND
                9 -> DELAYED_FOR_WIFI
                10 -> SERVICE_INTERRUPTED
                11 -> SERVICE_UNAVAILABLE
                12 -> QUOTA_EXCEEDED
                13 -> SSL_RECOVERABLE_PEER_UNVERIFIED
                14 -> SPECIFIC_FORBIDDEN
                15 -> SPECIFIC_SERVICE_UNAVAILABLE
                16 -> SPECIFIC_UNSUPPORTED_MEDIA_TYPE
                else -> UNKNOWN
            }

        fun fromThrowable(throwable: Throwable?): TransferResult {
            if (throwable == null) return UPLOADED

            return when (throwable) {
                is LocalFileNotFoundException -> FOLDER_ERROR
                is NoConnectionWithServerException -> NETWORK_CONNECTION
                is NetworkErrorException -> NETWORK_CONNECTION
                is UnauthorizedException -> CREDENTIAL_ERROR
                is FileNotFoundException -> FILE_NOT_FOUND
                is ConflictException -> CONFLICT_ERROR
                is ForbiddenException -> PRIVILEGES_ERROR
                is SpecificForbiddenException -> SPECIFIC_FORBIDDEN
                is ServiceUnavailableException -> SERVICE_UNAVAILABLE
                is SpecificServiceUnavailableException -> SPECIFIC_SERVICE_UNAVAILABLE
                is QuotaExceededException -> QUOTA_EXCEEDED
                is SpecificUnsupportedMediaTypeException -> SPECIFIC_UNSUPPORTED_MEDIA_TYPE
                is SSLRecoverablePeerUnverifiedException -> SSL_RECOVERABLE_PEER_UNVERIFIED
                else -> UNKNOWN
            }
        }

    }
}
