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
import java.io.File
import java.util.Locale

//TODO: Add new attributes on demand. Let's try to perform a clean up :)
//TODO: Make name not nullable.
@Parcelize
data class OCFile(
    val id: Long? = null,
    var parentId: Long? = null,
    val owner: String,
    var length: Long,
    val creationTimestamp: Long? = null,
    val modificationTimestamp: Long,
    val remotePath: String,
    var mimeType: String,
    val etag: String? = null,
    val permissions: String? = null,
    val remoteId: String? = null,
    val privateLink: String? = null,
    var storagePath: String? = null,
    var name: String? = null,
    var treeEtag: String? = null,

    // May not needed
    val keepInSync: Int? = null,
    val lastSyncDateForData: Int? = null,
    var lastSyncDateForProperties: Long? = null,
    var needsToUpdateThumbnail: Boolean? = null,
    val publicLink: String? = null,
    val modifiedAtLastSyncForData: Int? = null,
    val etagInConflict: String? = null,
    val fileIsDownloading: Boolean? = null,
    var sharedWithSharee: Boolean? = false,
    var sharedByLink: Boolean = false
) : Parcelable {

    init {
        name = File(remotePath).name.let { if (it.isBlank()) ROOT_PATH else it }
    }

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
     * get remote path of parent file
     * @return remote path
     */
    fun getParentRemotePath(): String {
        val parentPath: String = File(remotePath).parent ?: throw IllegalArgumentException("Parent path is null")
        return if (parentPath.endsWith("/")) parentPath else "$parentPath/"
    }

    /**
     * Use this to check if this file is available locally
     *
     * @return true if it is
     */
    fun isDown(): Boolean {
        if (!storagePath.isNullOrEmpty()) {
            val file = File(storagePath)
            return file.exists()
        }
        return false
    }

    /**
     * Can be used to check, whether or not this file exists in the database
     * already
     *
     * @return true, if the file exists in the database
     */
    fun fileExists(): Boolean {
        return id != -1L
    }

    /**
     * @return 'True' if the file is hidden
     */
    fun isHidden(): Boolean {
        return name?.startsWith(".") ?: false
    }

    val isSharedWithMe
        get() = permissions != null && permissions.contains(PERMISSION_SHARED_WITH_ME)

    /**
     * @param   type        Type to match in the file MIME type; it's MUST include the trailing "/"
     * @return              'True' if the file MIME type matches the received parameter in the type part.
     */
    private fun isOfType(type: String): Boolean =
        mimeType.startsWith(type) || getMimeTypeFromName()?.startsWith(type) ?: false

    fun getMimeTypeFromName(): String? {
        val extension = remotePath.substringAfterLast('.').toLowerCase(Locale.ROOT)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    companion object {
        const val PATH_SEPARATOR = '/'
        const val ROOT_PATH: String = "/"
    }
}
