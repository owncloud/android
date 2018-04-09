/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   @author Christian Schabesberger
 *   Copyright (C) 2016 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.fragment.LocalFileListFragment;

import java.io.File;

/**
 * Displays local folders and let the user choose one of them, which path is set as result.
 */

public class LocalFolderPickerActivity extends ToolbarActivity implements LocalFileListFragment.ContainerActivity {

    private static final String TAG = LocalFolderPickerActivity.class.getName();

    public static final String EXTRA_PATH = LocalFolderPickerActivity.class.getCanonicalName() + ".PATH";

    private static final String FTAG_LIST_OF_FOLDERS = "LIST_OF_FOLDERS";

    private File mCurrentFolder = null;

    protected Button mCancelBtn;
    protected Button mChooseBtn;
    protected ImageButton mHomeBtn;


    /**
     * Helper to launch a {@link LocalFolderPickerActivity} for which you would like a result when finished.
     * Your onActivityResult() method will be called with the given requestCode.
     *
     * @param activity      Activity calling {@link LocalFolderPickerActivity} for a result.
     * @param startPath     Absolute path to the local folder to show when the activity is shown.
     * @param requestCode   If >= 0, this code will be returned in onActivityResult().
     */
    public static void startLocalFolderPickerActivityForResult(
        Activity activity,
        String startPath,
        int requestCode
    ) {
        Intent action = new Intent(activity, LocalFolderPickerActivity.class);
        action.putExtra(LocalFolderPickerActivity.EXTRA_PATH, startPath);
        activity.startActivityForResult(action, requestCode);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);

        // set current folder
        String startPath = (savedInstanceState != null) ?
            savedInstanceState.getString(LocalFolderPickerActivity.EXTRA_PATH) :
            getIntent().getStringExtra(EXTRA_PATH)
        ;
        if (startPath != null) {
            mCurrentFolder = new File(startPath);
        }
        if (mCurrentFolder == null || !mCurrentFolder.exists()) {
            mCurrentFolder = Environment.getExternalStorageDirectory(); // default
        } else if (!mCurrentFolder.isDirectory()) {
            mCurrentFolder = mCurrentFolder.getParentFile();
        }

        // inflate and set the layout view
        setContentView(R.layout.files_folder_picker);   // beware - inflated in other activities too
        if (savedInstanceState == null) {
            createFragments();
       }

        // set input controllers
        mCancelBtn = findViewById(R.id.folder_picker_btn_cancel);
        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        mChooseBtn = findViewById(R.id.folder_picker_btn_choose);
        mChooseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // return the path of the current folder
                Intent data = new Intent();
                data.putExtra(EXTRA_PATH, mCurrentFolder.getAbsolutePath());
                setResult(RESULT_OK, data);
                finish();
            }
        });
        mHomeBtn = findViewById(R.id.folder_picker_btn_home);
        mHomeBtn.setVisibility(View.VISIBLE);
        mHomeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentFolder = Environment.getExternalStorageDirectory();
                getListFragment().listFolder(mCurrentFolder);
                updateActionBar();
            }
        });


        // init toolbar
        setupToolbar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
        }
        updateActionBar();

        Log_OC.d(TAG, "onCreate() end");
    }

    private void createFragments() {
        LocalFileListFragment listOfFiles = LocalFileListFragment.newInstance(true);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, listOfFiles, FTAG_LIST_OF_FOLDERS);
        transaction.commit();
    }

    /**
     * Updates contents shown by action bar.
     */
    private void updateActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            boolean mayBrowseUp = mayBrowseUp();
            actionBar.setHomeButtonEnabled(mayBrowseUp);
            actionBar.setDisplayHomeAsUpEnabled(mayBrowseUp);
            actionBar.setTitle(mayBrowseUp ? mCurrentFolder.getName() : File.separator);
        } else {
            Log_OC.w(TAG, "Action bar missing in action");
        }
    }

    /**
     * Handles presses on 'Up' button, not exactly the same as 'BACK' button.
     *
     * @param   item    Action in option menu tapped by the user.
     * @return          'true' if consumed, 'false' otherwise.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home: {
                if(mayBrowseUp()) {
                    onBackPressed();
                }
                break;
            }
            default:
                retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }


    /**
     * Handles presses on 'BACK' button.
     */
    @Override
    public void onBackPressed() {
        if(!mayBrowseUp()) {
            finish();
            return;
        }
        LocalFileListFragment listFragment = getListFragment();
        if (listFragment != null) {
            listFragment.browseUp();
            mCurrentFolder = listFragment.getCurrentFolder();
            updateActionBar();
        } else {
            Log_OC.e(TAG, "List of files not found - cannot browse up");
        }
    }


    /**
     * @return  'true' when browsing to the parent folder is possible, 'false' otherwise
     */
    private boolean mayBrowseUp() {
        return (mCurrentFolder != null && mCurrentFolder.getParentFile() != null);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log_OC.d(TAG, "onSaveInstanceState() start");
        super.onSaveInstanceState(outState);
        outState.putString(LocalFolderPickerActivity.EXTRA_PATH, mCurrentFolder.getAbsolutePath());
        Log_OC.d(TAG, "onSaveInstanceState() end");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onFolderClicked(File folder) {
        if (folder.isDirectory()) {
            mCurrentFolder = folder;
        }
        updateActionBar();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFileClicked(File file) {
        // nothing to do
    }

    @Override
    public File getCurrentFolder() {
        return mCurrentFolder;
    }

    @Nullable
    protected LocalFileListFragment getListFragment() {
        Fragment listOfFiles = getSupportFragmentManager().findFragmentByTag(
            FTAG_LIST_OF_FOLDERS
        );
        if (listOfFiles != null) {
            return (LocalFileListFragment) listOfFiles;
        }
        return null;
    }

}
