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
 * Behavior of the upload after uploading.
 *
 * COPY - Source file will be kept
 * MOVE - Source file will be removed
 *
 * By default, the file will be copied. We do not want to remove the file without user approval.
 *
 * Analog to the old LOCAL_BEHAVIOUR but with fixed options.
 * Warning -> Order of elements is really important. The ordinal is used to store the value in the database.
 */
enum class UploadBehavior {
    COPY, MOVE;

    companion object {
        fun fromLegacyLocalBehavior(oldLocalBehavior: Int): UploadBehavior {
            return when (oldLocalBehavior) {
                FileUploader.LOCAL_BEHAVIOUR_MOVE -> MOVE
                FileUploader.LOCAL_BEHAVIOUR_COPY -> COPY
                else -> COPY
            }
        }
    }
}
