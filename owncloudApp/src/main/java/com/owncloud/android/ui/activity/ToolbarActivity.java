/**
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * @author Christian Schabesberger
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

package com.owncloud.android.ui.activity;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.domain.files.model.OCFile;

/**
 * Base class providing toolbar registration functionality, see {@link #setupToolbar()}.
 */
public abstract class ToolbarActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Toolbar setup that must be called in implementer's {@link #onCreate} after {@link #setContentView} if they
     * want to use the toolbar.
     */
    protected void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    /**
     * Updates title bar and home buttons (state and icon).
     */
    protected void updateActionBarTitleAndHomeButton(OCFile chosenFile) {
        String title = getString(R.string.default_display_name_for_root_folder);    // default
        boolean inRoot;

        // choose the appropriate title
        inRoot = (
                chosenFile == null ||
                        (chosenFile.isFolder() && chosenFile.getParentId() == FileDataStorageManager.ROOT_PARENT_ID)
        );
        if (!inRoot) {
            title = chosenFile.getName();
        }

        updateActionBarTitleAndHomeButtonByString(title);
    }

    /**
     * Updates title bar and home buttons (state and icon).
     */
    protected void updateActionBarTitleAndHomeButtonByString(String title) {
        String titleToSet = getString(R.string.app_name);    // default

        if (title != null) {
            titleToSet = title;
        }

        // set the chosen title
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(titleToSet);

        // also as content description
        View actionBarTitleView = getWindow().getDecorView().findViewById(
                getResources().getIdentifier("action_bar_title", "id", "android")
        );
        // TODO remove legacy code
        if (actionBarTitleView != null) {    // it's null in Android 2.x
            actionBarTitleView.setContentDescription(titleToSet);
        }

        // set home button properties
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
    }

    /**
     * checks if the given file is the root folder.
     *
     * @param file file to be checked if it is the root folder
     * @return <code>true</code> if it is <code>null</code> or the root folder, else returns <code>false</code>
     */
    public boolean isRoot(OCFile file) {
        return file == null ||
                (file.isFolder() && file.getParentId() == FileDataStorageManager.ROOT_PARENT_ID);
    }

}
