/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2022 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.usecases

import com.owncloud.android.files.services.FileUploader


/**
 * Behavior of the file to upload.
 *
 * COPY_TO_TEMPORAL_AND_UPLOAD - Make a temporal file before uploading.
 * MOVE_TO_NEW_PLACE - Source file is potentially in a wrong place inside owncloud local storage and needs to be moved to a new one.
 * FORGET - Upload the file to server(normally a cached one) and do not keep a copy in the local storage.
 *
 * By default, the file will be copied. We do not want to remove the file without user approval.
 *
 * Analog to the old LOCAL_BEHAVIOUR but with fixed options.
 * Warning -> Order of elements is really important. The ordinal is used to store the value in the database.
 */
enum class UploadBehavior {
    COPY, MOVE, FORGET;

    companion object {
        fun fromLegacyLocalBehavior(oldLocalBehavior: Int): UploadBehavior {
            return when (oldLocalBehavior) {
                FileUploader.LEGACY_LOCAL_BEHAVIOUR_MOVE -> MOVE
                FileUploader.LEGACY_LOCAL_BEHAVIOUR_COPY -> COPY
                FileUploader.LEGACY_LOCAL_BEHAVIOUR_FORGET -> FORGET
                else -> COPY
            }
        }
    }
}
