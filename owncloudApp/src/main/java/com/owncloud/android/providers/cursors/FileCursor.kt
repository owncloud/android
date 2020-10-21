/**
 * ownCloud Android client application
 *
 * @author Bartosz Przybylski
 * @author Abel García de Prada
 * Copyright (C) 2015  Bartosz Przybylski
 * Copyright (C) 2020 ownCloud GmbH.
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
package com.owncloud.android.providers.cursors

import android.database.MatrixCursor
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.utils.MimetypeIconUtil

class FileCursor(projection: Array<String>?) : MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION) {
    private var cursorExtras = Bundle.EMPTY

    override fun getExtras(): Bundle = cursorExtras

    fun setMoreToSync(hasMoreToSync: Boolean) {
        cursorExtras = Bundle().apply { putBoolean(DocumentsContract.EXTRA_LOADING, hasMoreToSync) }
    }

    fun addFile(file: OCFile) {
        val iconRes = MimetypeIconUtil.getFileTypeIconId(file.mimeType, file.fileName)
        val mimeType = if (file.isFolder) Document.MIME_TYPE_DIR else file.mimeType
        val imagePath = if (file.isImage && file.isDown()) file.storagePath else null
        var flags = if (imagePath != null) Document.FLAG_SUPPORTS_THUMBNAIL else 0

        flags = flags or Document.FLAG_SUPPORTS_DELETE or Document.FLAG_SUPPORTS_WRITE

        if (mimeType == Document.MIME_TYPE_DIR) {
            flags = flags or Document.FLAG_DIR_SUPPORTS_CREATE
        }

        flags = flags or Document.FLAG_SUPPORTS_RENAME

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            flags = flags or Document.FLAG_SUPPORTS_COPY
            flags = flags or Document.FLAG_SUPPORTS_MOVE
        }

        newRow()
            .add(Document.COLUMN_DOCUMENT_ID, file.id.toString())
            .add(Document.COLUMN_DISPLAY_NAME, file.fileName)
            .add(Document.COLUMN_LAST_MODIFIED, file.modificationTimestamp)
            .add(Document.COLUMN_SIZE, file.length)
            .add(Document.COLUMN_FLAGS, flags)
            .add(Document.COLUMN_ICON, iconRes)
            .add(Document.COLUMN_MIME_TYPE, mimeType)
    }

    companion object {
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_SIZE,
            Document.COLUMN_FLAGS,
            Document.COLUMN_LAST_MODIFIED
        )

    }
}
