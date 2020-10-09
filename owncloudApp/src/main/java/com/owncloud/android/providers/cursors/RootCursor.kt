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

import android.accounts.Account
import android.content.Context
import android.database.MatrixCursor
import android.provider.DocumentsContract.Root
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile

class RootCursor(projection: Array<String>?) : MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION) {

    fun addRoot(account: Account, context: Context) {
        val manager = FileDataStorageManager(context, account, context.contentResolver)
        val mainDir = manager.getFileByPath(OCFile.ROOT_PATH)

        val flags = Root.FLAG_SUPPORTS_SEARCH or Root.FLAG_SUPPORTS_CREATE

        newRow()
            .add(Root.COLUMN_ROOT_ID, account.name)
            .add(Root.COLUMN_DOCUMENT_ID, mainDir?.id)
            .add(Root.COLUMN_SUMMARY, account.name)
            .add(Root.COLUMN_TITLE, context.getString(R.string.app_name))
            .add(Root.COLUMN_ICON, R.mipmap.icon)
            .add(Root.COLUMN_FLAGS, flags)
    }

    fun addProtectedRoot(context: Context, passcodeState: Boolean) {
        newRow()
            .add(
                Root.COLUMN_SUMMARY,
                if (passcodeState) context.getString(R.string.pass_code_locked)
                else context.getString(R.string.pattern_locked)
            )
            .add(Root.COLUMN_TITLE, context.getString(R.string.app_name))
            .add(Root.COLUMN_ICON, R.mipmap.icon)
    }

    companion object {
        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_FLAGS
        )
    }
}
