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

package com.owncloud.android.domain.files.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

//TODO: Add new attributes on demand. Let's try to perform a clean up :)
@Parcelize
data class OCFile(
    val id: Long? = null,
    val parentId: Long? = null,
    val owner: String,
    val length: Long,
    val creationTimestamp: Long,
    val modifiedTimestamp: Long,
    val remotePath: String,
    val mimeType: String,
    val etag: String,
    val permissions: String,
    val remoteId: String,
    val privateLink: String
) : Parcelable {

    /**
     * Use this to find out if this file is a folder.
     *
     * @return true if it is a folder
     */
    fun isFolder() = mimeType == "DIR" || mimeType == "httpd/unix-directory"

    companion object {
        const val PATH_SEPARATOR = "/"
        const val ROOT_PATH = PATH_SEPARATOR
    }
}
