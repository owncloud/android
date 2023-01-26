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
import android.provider.DocumentsContract.Document
import com.owncloud.android.R
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.presentation.documentsprovider.cursors.FileCursor.Companion.DEFAULT_DOCUMENT_PROJECTION

class SpaceCursor(projection: Array<String>?) : MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION) {

    fun addSpace(space: OCSpace, context: Context?) {
        val iconRes = R.drawable.ic_spaces
        val mimeType = Document.MIME_TYPE_DIR
        var flags = Document.FLAG_DIR_SUPPORTS_CREATE

        if (space.isPersonal) {
            flags = flags or Document.FLAG_SUPPORTS_WRITE
        }

        val name = if (space.isPersonal) context?.getString(R.string.bottom_nav_personal) else space.name

        newRow()
            .add(Document.COLUMN_DOCUMENT_ID, space.id)
            .add(Document.COLUMN_DISPLAY_NAME, name)
            .add(Document.COLUMN_LAST_MODIFIED, space.lastModifiedDateTime)
            .add(Document.COLUMN_SIZE, space.quota?.used)
            .add(Document.COLUMN_FLAGS, flags)
            .add(Document.COLUMN_ICON, iconRes)
            .add(Document.COLUMN_MIME_TYPE, mimeType)
    }

    fun addRootForSpaces() {
        val mimeType = Document.MIME_TYPE_DIR

        newRow()
            .add(Document.COLUMN_DOCUMENT_ID, 0)
            .add(Document.COLUMN_DISPLAY_NAME, "Spaces")
            .add(Document.COLUMN_LAST_MODIFIED, null)
            .add(Document.COLUMN_SIZE, null)
            .add(Document.COLUMN_FLAGS, 0)
            .add(Document.COLUMN_MIME_TYPE, mimeType)
    }
}
