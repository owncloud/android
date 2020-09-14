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
import android.webkit.MimeTypeMap
import kotlinx.android.parcel.Parcelize
import java.util.Locale

//TODO: Add new attributes on demand. Let's try to perform a clean up :)
@Parcelize
data class OCFile(
    val id: Long? = null,
    val parentId: Long? = null,
    val owner: String,
    val length: Long,
    val creationTimestamp: Long? = null,
    val modifiedTimestamp: Long,
    val remotePath: String,
    val mimeType: String,
    val etag: String? = null,
    val permissions: String? = null,
    val remoteId: String? = null,
    val privateLink: String? = null
) : Parcelable {

    /**
     * Use this to find out if this file is a folder.
     *
     * @return true if it is a folder
     */
    val isFolder
        get() = mimeType == MIME_DIR || mimeType == MIME_DIR_UNIX

    /**
     * @return 'True' if the file contains audio
     */
    val isAudio: Boolean
        get() = isOfType(MIME_PREFIX_AUDIO)

    /**
     * @return 'True' if the file contains video
     */
    val isVideo: Boolean
        get() = isOfType(MIME_PREFIX_VIDEO)

    /**
     * @return 'True' if the file contains an image
     */
    val isImage: Boolean
        get() = isOfType(MIME_PREFIX_IMAGE)

    /**
     * @return 'True' if the file is simple text (e.g. not application-dependent, like .doc or .docx)
     */
    val isText: Boolean
        get() = isOfType(MIME_PREFIX_TEXT)

    /**
     * @param   type        Type to match in the file MIME type; it's MUST include the trailing "/"
     * @return              'True' if the file MIME type matches the received parameter in the type part.
     */
    private fun isOfType(type: String): Boolean =
        mimeType.startsWith(type) || getMimeTypeFromName()?.startsWith(type) ?: false

    private fun getMimeTypeFromName(): String? {
        val extension = remotePath.substringAfterLast('.').toLowerCase(Locale.ROOT)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    companion object {
        const val PATH_SEPARATOR = '/'
        const val ROOT_PATH = PATH_SEPARATOR
    }
}
