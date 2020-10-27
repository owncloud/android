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
import com.owncloud.android.domain.ext.isOneOf
import kotlinx.android.parcel.Parcelize
import java.io.File
import java.util.Locale

//TODO: Add new attributes on demand. Let's try to perform a clean up :)
@Parcelize
data class OCFile(
    var id: Long? = null,
    var parentId: Long? = null,
    val owner: String,
    var length: Long,
    var creationTimestamp: Long? = 0,
    var modificationTimestamp: Long,
    val remotePath: String,
    var mimeType: String,
    var etag: String? = "",
    val permissions: String? = null,
    var remoteId: String? = null,
    val privateLink: String? = "",
    var storagePath: String? = null,
    var treeEtag: String? = "",

    //TODO: May not needed
    val keepInSync: Int? = null,
    var lastSyncDateForData: Int? = 0,
    var lastSyncDateForProperties: Long? = 0,
    var needsToUpdateThumbnail: Boolean = false,
    var modifiedAtLastSyncForData: Int? = 0,
    var etagInConflict: String? = null,
    val fileIsDownloading: Boolean? = false,
    var sharedWithSharee: Boolean? = false,
    var sharedByLink: Boolean = false
) : Parcelable {

    val fileName: String
        get() = File(remotePath).name.let { if (it.isBlank()) ROOT_PATH else it }

    @Deprecated("Do not use this constructor. Remove it as soon as possible")
    constructor(remotePath: String, mimeType: String, parentId: Long?, owner: String) : this(
        remotePath = remotePath,
        mimeType = mimeType,
        parentId = parentId,
        owner = owner,
        modificationTimestamp = 0,
        length = 0
    )

    /**
     * Use this to find out if this file is a folder.
     *
     * @return true if it is a folder
     */
    val isFolder
        get() = mimeType.isOneOf(MIME_DIR, MIME_DIR_UNIX)

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
        return if (parentPath.endsWith("$PATH_SEPARATOR")) parentPath else "$parentPath$PATH_SEPARATOR"
    }

    /**
     * Use this to check if this file is available locally
     *
     * @return true if it is
     */
    val isAvailableLocally: Boolean
        get() =
            storagePath?.takeIf {
                it.isNotBlank()
            }?.let { storagePath ->
                File(storagePath).exists()
            } ?: false

    /**
     * Can be used to check, whether or not this file exists in the database
     * already
     *
     * @return true, if the file exists in the database
     */
    val fileExists: Boolean
        get() = id != null && id != -1L

    /**
     * @return 'True' if the file is hidden
     */
    val isHidden: Boolean
        get() = fileName.startsWith(".")

    val isSharedWithMe
        get() = permissions != null && permissions.contains(PERMISSION_SHARED_WITH_ME)

    val localModificationTimestamp: Long
        get() =
            storagePath?.takeIf {
                it.isNotBlank()
            }?.let { storagePath ->
                File(storagePath).lastModified()
            } ?: 0

    fun copyLocalPropertiesFrom(sourceFile: OCFile) {
        parentId = sourceFile.parentId
        id = sourceFile.id
        lastSyncDateForData = sourceFile.lastSyncDateForData
        modifiedAtLastSyncForData = sourceFile.modifiedAtLastSyncForData
        storagePath = sourceFile.storagePath
        treeEtag = sourceFile.treeEtag
        etagInConflict = sourceFile.etagInConflict
        // FIXME: 19/10/2020 : New_arch: Av.Offline
//        setAvailableOfflineStatus(sourceFile.getAvailableOfflineStatus())
        // FIXME: 19/10/2020 : New_arch: Shared by link
//        setSharedViaLink(sourceFile.isSharedViaLink())
//        setSharedWithSharee(sourceFile.isSharedWithSharee())
    }

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
        const val ROOT_PARENT_ID: Long = 0
    }
}
