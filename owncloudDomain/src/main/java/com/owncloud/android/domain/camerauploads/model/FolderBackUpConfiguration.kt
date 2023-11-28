/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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

package com.owncloud.android.domain.camerauploads.model

data class FolderBackUpConfiguration(
    val accountName: String,
    val behavior: UploadBehavior,
    val sourcePath: String,
    val uploadPath: String,
    val wifiOnly: Boolean,
    val chargingOnly: Boolean,
    val lastSyncTimestamp: Long,
    val name: String,
    val spaceId: String?,
) {

    val isPictureUploads get() = name == pictureUploadsName
    val isVideoUploads get() = name == videoUploadsName

    companion object {
        const val pictureUploadsName = "Picture uploads"
        const val videoUploadsName = "Video uploads"
    }
}

/**
 * Behaviour to after uploading a file.
 *
 * Move - Remove the source file after a successful upload
 * Copy - Keep the source file after a successful upload
 */
enum class UploadBehavior {
    MOVE, COPY;

    @Deprecated("Legacy Local Behavior. Remove asap")
    fun toLegacyLocalBehavior(): Int {
        return when (this) {
            MOVE -> LEGACY_LOCAL_BEHAVIOUR_MOVE
            COPY -> LEGACY_LOCAL_BEHAVIOUR_COPY
        }
    }

    companion object {
        private const val LEGACY_LOCAL_BEHAVIOUR_COPY = 0
        private const val LEGACY_LOCAL_BEHAVIOUR_MOVE = 1

        fun fromString(string: String): UploadBehavior {
            return if (string.equals("MOVE", ignoreCase = true)) {
                MOVE
            } else {
                COPY
            }
        }
    }
}
