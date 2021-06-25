/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Shashvat Kedia
 * @author Juan Carlos Garrote Gascón
 * <p>
 * Copyright (C) 2021 ownCloud GmbH.
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
package com.owncloud.android.data

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import java.io.File

class LocalStorageProvider(
    private val rootFolderName: String
) {
    /**
     * Get local storage path for accountName.
     */
    fun getSavePath(accountName: String?): String = getRootFolderPath() + File.separator + getEncodedAccountName(accountName)

    /**
     * Get local path where OCFile file is to be stored after upload. That is,
     * corresponding local path (in local owncloud storage) to remote uploaded
     * file.
     */
    fun getDefaultSavePathFor(accountName: String?, remotePath: String): String = getSavePath(accountName) + remotePath

    /**
     * Get absolute path to tmp folder inside datafolder in sd-card for given accountName.
     */
    fun getTemporalPath(accountName: String?): String = getRootFolderPath() + "/tmp/" + getEncodedAccountName(accountName)

    /**
     * Optimistic number of bytes available on sd-card.
     *
     * @return Optimistic number of available bytes (can be less)
     */
    @SuppressLint("UsableSpace")
    fun getUsableSpace(): Long = getPrimaryStorageDirectory().usableSpace

    fun getDefaultCameraSourcePath(): String {
        return DocumentFile.fromFile(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        ).createDirectory(CAMERA_FOLDER)?.uri.toString()
    }

    /**
     * Return the root path of primary shared/external storage directory for this application.
     * For example: /storage/emulated/0/owncloud
     */
    private fun getRootFolderPath(): String = getPrimaryStorageDirectory().absolutePath + File.separator + rootFolderName

    /**
     * Return the primary shared/external storage directory where files will be stored.
     * For example: /storage/emulated/0
     */
    private fun getPrimaryStorageDirectory(): File = Environment.getExternalStorageDirectory()

    /**
     * URL encoding is an 'easy fix' to overcome that NTFS and FAT32 don't allow ":" in file names,
     * that can be in the accountName since 0.1.190B
     */
    private fun getEncodedAccountName(accountName: String?): String = Uri.encode(accountName, "@")

    companion object {
        private const val CAMERA_FOLDER = "/Camera"
    }
}
