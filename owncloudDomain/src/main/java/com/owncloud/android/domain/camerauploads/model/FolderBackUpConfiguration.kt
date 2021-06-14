/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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

sealed class FolderBackUpConfiguration(
    val accountName: String,
    val behavior: Behavior,
    val sourcePath: String,
    val uploadPath: String,
    val wifiOnly: Boolean,
) {
    class PictureUploadsConfiguration(
        accountName: String,
        behavior: Behavior,
        sourcePath: String,
        uploadPath: String,
        wifiOnly: Boolean,
    ) : FolderBackUpConfiguration(accountName, behavior, sourcePath, uploadPath, wifiOnly)

    class VideoUploadsConfiguration(
        accountName: String,
        behavior: Behavior,
        sourcePath: String,
        uploadPath: String,
        wifiOnly: Boolean,
    ) : FolderBackUpConfiguration(accountName, behavior, sourcePath, uploadPath, wifiOnly)

    enum class Behavior {
        MOVE, COPY;

        companion object {
            fun fromString(string: String): Behavior {
                return if (string.equals("MOVE", ignoreCase = true)) {
                    MOVE
                } else {
                    COPY
                }
            }
        }
    }
}
