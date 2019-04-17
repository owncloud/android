/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.operations

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.FileUtils
import com.owncloud.android.lib.resources.shares.GetRemoteShareOperation
import com.owncloud.android.lib.resources.shares.RemoteShare
import com.owncloud.android.lib.resources.shares.ShareParserResult
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.UpdateRemoteShareOperation
import com.owncloud.android.operations.common.SyncOperation
import com.owncloud.android.shares.db.OCShare

/**
 * Updates an existing public share for a given file
 */

class UpdateShareViaLinkOperation
/**
 * Constructor
 *
 * @param shareId          Local id of public share to update.
 */
    (private val shareId: Long) : SyncOperation<ShareParserResult>() {
    private var name: String? = null
    private var password: String? = null
    private var publicUpload: Boolean? = null
    private var expirationDateInMillis: Long = 0
    private var permissions: Int = 0

    init {
        name = null
        password = null
        expirationDateInMillis = 0
        publicUpload = null
        permissions = RemoteShare.DEFAULT_PERMISSION
    }

    /**
     * Set name to update in public link.
     *
     * @param name          Name to set to the public link.
     * Empty string clears the current name.
     * Null results in no update applied to the name.
     */
    fun setName(name: String) {
        this.name = name
    }

    /**
     * Set password to update in public link.
     *
     * @param password      Password to set to the public link.
     * Empty string clears the current password.
     * Null results in no update applied to the password.
     */
    fun setPassword(password: String) {
        this.password = password
    }

    /**
     * Set expiration date to update in Share resource.
     *
     * @param expirationDateInMillis    Expiration date to set to the public link.
     * A negative value clears the current expiration date.
     * Zero value (start-of-epoch) results in no update done on
     * the expiration date.
     */
    fun setExpirationDate(expirationDateInMillis: Long) {
        this.expirationDateInMillis = expirationDateInMillis
    }

    /**
     * Enable upload permissions to update in Share resource.
     *
     * @param publicUpload    Upload Permission to set to the public link.
     * Null results in no update applied to the upload permission.
     */
    fun setPublicUpload(publicUpload: Boolean?) {
        this.publicUpload = publicUpload
    }

    /**
     * Set permissions to allow or not specific actions in the share
     *
     * @param permissions permissions to set to the public link.
     */
    fun setPermissions(permissions: Int) {
        this.permissions = permissions
    }

    override fun run(client: OwnCloudClient): RemoteOperationResult<ShareParserResult> {

        val storedShare = storageManager.getShareById(shareId)

        if (storedShare == null || ShareType.PUBLIC_LINK.value != storedShare.shareType) {
            return RemoteOperationResult(
                RemoteOperationResult.ResultCode.SHARE_NOT_FOUND
            )
        }

        // Update remote share with password
        val updateOp = UpdateRemoteShareOperation(
            storedShare.remoteId
        )
        updateOp.name = name
        updateOp.password = password
        updateOp.expirationDateInMillis = expirationDateInMillis
        updateOp.publicUpload = publicUpload
        updateOp.permissions = permissions
        var result = updateOp.execute(client)

        if (result.isSuccess) {
            // Retrieve updated share / save directly with password? -> no; the password is not to be saved
            val getShareOp = GetRemoteShareOperation(storedShare.remoteId)
            result = getShareOp.execute(client)
            if (result.isSuccess) {
                val remoteShare = result.data.shares[0]
                updateData(storedShare, remoteShare)
            }
        }

        return result
    }

    private fun updateData(oldShare: OCShare, newShare: RemoteShare) {
        // undesired magic - TODO map remote OCShare class to proper local OCShare class
        newShare.path = oldShare.path
        newShare.isFolder = oldShare.path.endsWith(FileUtils.PATH_SEPARATOR)

        // Update DB with the response
        storageManager.saveShare(newShare)

        // Update OCFile with data from share
        val file = storageManager.getFileByPath(oldShare.path)
        if (file != null) {
            file.isSharedViaLink = true
            storageManager.saveFile(file)
        }
    }
}
