/**
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * @author Christian Schabesberger
 * Copyright (C) 2020 ownCloud GmbH.
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
package com.owncloud.android.ui.activity

import androidx.appcompat.widget.Toolbar
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile

/**
 * Base class providing toolbar registration functionality, see [.setupToolbar].
 */
abstract class ToolbarActivity : BaseActivity() {

    /**
     * Toolbar setup that must be called in implementer's [.onCreate] after [.setContentView] if they
     * want to use the toolbar.
     */
    protected fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    /**
     * Updates title bar and home buttons (state and icon).
     */
    protected open fun updateActionBarTitleAndHomeButton(chosenFile: OCFile?) {
        var title: String? = getString(R.string.default_display_name_for_root_folder) // default

        if (!isRoot(chosenFile)) {
            title = chosenFile?.fileName ?: getString(R.string.default_display_name_for_root_folder)
        }
        updateActionBarTitleAndHomeButtonByString(title)
    }

    /**
     * Updates title bar and home buttons (state and icon).
     */
    protected fun updateActionBarTitleAndHomeButtonByString(title: String?) {
        val titleToSet = title ?: getString(R.string.app_name) // default

        // set the chosen title
        val actionBar = supportActionBar
        actionBar?.title = titleToSet

        // set home button properties
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setDisplayShowTitleEnabled(true)
    }

    /**
     * checks if the given file is the root folder.
     *
     * @param file file to be checked if it is the root folder
     * @return `true` if it is `null` or the root folder, else returns `false`
     */
    fun isRoot(file: OCFile?): Boolean {
        return file == null ||
                (file.isFolder && file.parentId == FileDataStorageManager.ROOT_PARENT_ID.toLong())
    }
}
