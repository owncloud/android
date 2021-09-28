/*
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
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
 *
 */

package com.owncloud.android.extensions

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.owncloud.android.R
import com.owncloud.android.lib.common.network.WebdavUtils
import timber.log.Timber
import java.io.File



fun File.sizeInBytes(): Int = if (!exists()) 0 else length().toInt()
fun File.sizeInKb() = sizeInBytes() / 1024
fun File.sizeInMb() = sizeInKb() / 1024

fun File.getSize(): String {
    return if (sizeInBytes() < 1024) {
        "${sizeInBytes()} bytes"
    } else if (sizeInBytes() > 1024 && sizeInKb() < 1024) {
        "${sizeInKb()} KB"
    } else {
        "${sizeInMb()} MB"
    }
}

fun File.getExposedFileUri(context: Context, localPath: String): Uri? {
    var exposedFileUri: Uri? = null

    if (localPath.isEmpty()) {
        return null
    }

    if (exposedFileUri == null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            // TODO - use FileProvider with any Android version, with deeper testing -> 2.2.0
            exposedFileUri = Uri.parse(
                ContentResolver.SCHEME_FILE + "://" + WebdavUtils.encodePath(localPath)
            )
        } else {
            // Use the FileProvider to get a content URI
            try {
                exposedFileUri = FileProvider.getUriForFile(
                    context,
                    context.getString(R.string.file_provider_authority),
                    File(localPath)
                )
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "File can't be exported")
            }
        }
    }
    return exposedFileUri
}

