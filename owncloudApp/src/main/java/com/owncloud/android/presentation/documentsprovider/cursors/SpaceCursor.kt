/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.presentation.documentsprovider.cursors

import android.content.Context
import android.database.MatrixCursor
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import com.owncloud.android.R
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.presentation.documentsprovider.cursors.FileCursor.Companion.DEFAULT_DOCUMENT_PROJECTION

class SpaceCursor(projection: Array<String>?) : MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION) {
    private var cursorExtras = Bundle.EMPTY

    override fun getExtras(): Bundle = cursorExtras

    fun setMoreToSync(hasMoreToSync: Boolean) {
        cursorExtras = Bundle().apply { putBoolean(DocumentsContract.EXTRA_LOADING, hasMoreToSync) }
    }

    fun addSpace(space: OCSpace, rootFolder: OCFile, context: Context?) {
        val flags = if (rootFolder.hasAddFilePermission && rootFolder.hasAddSubdirectoriesPermission) {
            Document.FLAG_DIR_SUPPORTS_CREATE
        } else 0

        val name = if (space.isPersonal) context?.getString(R.string.bottom_nav_personal) else space.name

        newRow()
            .add(Document.COLUMN_DOCUMENT_ID, rootFolder.id)
            .add(Document.COLUMN_DISPLAY_NAME, name)
            .add(Document.COLUMN_LAST_MODIFIED, space.lastModifiedDateTime)
            .add(Document.COLUMN_SIZE, space.quota?.used)
            .add(Document.COLUMN_FLAGS, flags)
            .add(Document.COLUMN_ICON, R.drawable.ic_spaces)
            .add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
    }

    /**
     * Add root for spaces. Main difference is that we add the account name as the document id,
     * so we need to take it into account in order to display the list of spaces or
     * the actual list of files inside the folder.
     */
    fun addRootForSpaces(context: Context?, accountName: String) {
        newRow()
            .add(Document.COLUMN_DOCUMENT_ID, accountName)
            .add(Document.COLUMN_DISPLAY_NAME, context?.getString(R.string.bottom_nav_spaces))
            .add(Document.COLUMN_LAST_MODIFIED, null)
            .add(Document.COLUMN_SIZE, null)
            .add(Document.COLUMN_FLAGS, 0)
            .add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
    }
}
