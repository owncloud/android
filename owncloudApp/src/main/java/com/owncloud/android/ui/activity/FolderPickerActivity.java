/**
 * ownCloud Android client application
 *
 * @author Shashvat Kedia
 * @author David González Verdugo
 * @author Abel García de Prada
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

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.domain.files.model.FileListOption;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.domain.spaces.model.OCSpace;
import com.owncloud.android.presentation.files.filelist.MainFileListFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.PreferenceUtils;
import timber.log.Timber;

import java.util.ArrayList;

public class FolderPickerActivity extends FileActivity implements FileFragment.ContainerActivity,
        OnClickListener, MainFileListFragment.FileActions {

    public static final String EXTRA_FOLDER = FolderPickerActivity.class.getCanonicalName()
            + ".EXTRA_FOLDER";
    public static final String EXTRA_FILES = FolderPickerActivity.class.getCanonicalName()
            + ".EXTRA_FILES";

    private static final String TAG_LIST_OF_FOLDERS = "LIST_OF_FOLDERS";

    public static final String EXTRA_PICKER_OPTION = "EXTRA_PICKER_OPTION";

    protected Button mCancelBtn;
    protected Button mChooseBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate() start");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.files_folder_picker);     // beware - inflated in other activities too

        // Allow or disallow touches with other visible windows
        LinearLayout filesFolderPickerLayout = findViewById(R.id.filesFolderPickerLayout);
        filesFolderPickerLayout.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)
        );

        if (savedInstanceState == null) {
            initAndShowListOfFilesFragment();
        }

        // sets callback listeners for UI elements
        initPickerListeners();

        // Action bar setup
        setupStandardToolbar(null, false, false, true);

        // Set action button text
        setActionButtonText();

        Timber.d("onCreate() end");
    }

    /**
     * Called when the ownCloud {@link Account} associated to the Activity was just updated.
     */
    @Override
    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(stateWasRecovered);
        if (getAccount() != null) {

            updateFileFromDB();

            OCFile folder = getFile();
            if (folder == null || !folder.isFolder()) {
                // fall back to root folder
                setFile(getStorageManager().getFileByPath(OCFile.ROOT_PATH));
                folder = getFile();
            }

            if (!stateWasRecovered) {
                MainFileListFragment listOfFolders = getListOfFilesFragment();
                listOfFolders.navigateToFolder(folder);
            }

            updateNavigationElementsInActionBar();
        }
    }

    private void initAndShowListOfFilesFragment() {
        OCFile safeInitialFolder;
        if (getFile() == null) {
            FileDataStorageManager fileDataStorageManager = new FileDataStorageManager(getAccount());
            safeInitialFolder = fileDataStorageManager.getFileByPath(OCFile.ROOT_PATH);
        } else {
            safeInitialFolder = getFile();
        }

        MainFileListFragment mainListOfFiles = MainFileListFragment.newInstance(safeInitialFolder, true, FileListOption.ALL_FILES);
        mainListOfFiles.setFileActions(this);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, mainListOfFiles, TAG_LIST_OF_FOLDERS);
        transaction.commit();
    }

    private void setActionButtonText() {
        PickerMode actionButton = (PickerMode) getIntent().getSerializableExtra(EXTRA_PICKER_OPTION);
        Button chooseButton = findViewById(R.id.folder_picker_btn_choose);
        chooseButton.setText(getString(actionButton.getButtonString()));
    }

    protected MainFileListFragment getListOfFilesFragment() {
        Fragment listOfFiles = getSupportFragmentManager().findFragmentByTag(FolderPickerActivity.TAG_LIST_OF_FOLDERS);
        if (listOfFiles != null) {
            return (MainFileListFragment) listOfFiles;
        }
        Timber.e("Access to unexisting list of files fragment!!");
        return null;
    }

    @Override
    public void onSavedCertificate() {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home: {
                OCFile currentDir = getCurrentFolder();
                if (currentDir != null && currentDir.getParentId() != 0) {
                    onBackPressed();
                }
                break;
            }
            default:
                retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }

    protected OCFile getCurrentFolder() {
        MainFileListFragment listOfFiles = getListOfFilesFragment();
        if (listOfFiles != null) {  // should never be null, indeed
            return listOfFiles.getCurrentFile();
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        MainFileListFragment listOfFiles = getListOfFilesFragment();
        if (listOfFiles != null) {  // should never be null, indeed
            OCFile fileBeforeBrowsingUp = listOfFiles.getCurrentFile();
            if (fileBeforeBrowsingUp.getParentId() != null &&
                    fileBeforeBrowsingUp.getParentId() == OCFile.ROOT_PARENT_ID
            ) {
                // If we are already at root, let's finish the picker. No sense to keep browsing up.
                finish();
                return;
            }
            listOfFiles.onBrowseUp();
            setFile(listOfFiles.getCurrentFile());
            updateNavigationElementsInActionBar();
        }
    }

    protected void updateNavigationElementsInActionBar() {
        OCFile currentDir;

        try {
            currentDir = getCurrentFolder();
        } catch (NullPointerException e) {
            currentDir = getFile();
        }

        boolean atRoot = (currentDir == null || currentDir.getParentId() == 0);
        updateStandardToolbar(
                atRoot ? getString(R.string.default_display_name_for_root_folder) : currentDir.getFileName(),
                !atRoot,
                !atRoot
        );
    }

    /**
     * Set per-view controllers
     */
    private void initPickerListeners() {
        mCancelBtn = findViewById(R.id.folder_picker_btn_cancel);
        mCancelBtn.setOnClickListener(this);
        mChooseBtn = findViewById(R.id.folder_picker_btn_choose);
        mChooseBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == mCancelBtn) {
            finish();
        } else if (v == mChooseBtn) {
            Intent i = getIntent();
            ArrayList<Parcelable> targetFiles = i.getParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES);

            Intent data = new Intent();
            data.putExtra(EXTRA_FOLDER, getCurrentFolder());
            data.putParcelableArrayListExtra(EXTRA_FILES, targetFiles);
            setResult(RESULT_OK, data);

            finish();
        }
    }

    @Override
    public void onCurrentFolderUpdated(@NonNull OCFile newCurrentFolder, @Nullable OCSpace currentSpace) {
        updateNavigationElementsInActionBar();
        setFile(newCurrentFolder);
    }

    @Override
    public void initDownloadForSending(@NonNull OCFile file) {

    }

    @Override
    public void cancelFileTransference(@NonNull ArrayList<OCFile> files) {

    }

    @Override
    public void setBottomBarVisibility(boolean isVisible) {

    }

    @Override
    public void onFileClicked(@NonNull OCFile file) {
        // Nothing to do. Clicking on files is not allowed.
    }

    @Override
    public void onShareFileClicked(@NonNull OCFile file) {
        // Nothing to do. Clicking on files is not allowed.
    }

    @Override
    public void syncFile(@NonNull OCFile file) {
        // Nothing to do. Clicking on files is not allowed.
    }

    @Override
    public void openFile(@NonNull OCFile file) {
        // Nothing to do. Clicking on files is not allowed.
    }

    @Override
    public void sendDownloadedFile(@NonNull OCFile file) {
        // Nothing to do. Clicking on files is not allowed.

    }

    /**
     * Nothing to do. Details can't be opened from {@link FolderPickerActivity}
     *
     * @param file {@link OCFile} whose details will be shown
     */
    @Override
    public void showDetails(OCFile file) {

    }

    public enum PickerMode {
        MOVE, COPY, CAMERA_FOLDER;

        public Integer getButtonString() {
            switch (this) {
                case MOVE:
                    return R.string.folder_picker_move_here_button_text;
                case COPY:
                    return R.string.folder_picker_copy_here_button_text;
                default:
                    return R.string.folder_picker_choose_button_text;
            }
        }
    }
}
