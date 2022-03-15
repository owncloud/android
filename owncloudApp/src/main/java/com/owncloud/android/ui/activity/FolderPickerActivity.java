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
import android.accounts.AuthenticatorException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.owncloud.android.R;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.presentation.ui.files.filelist.MainFileListFragment;
import com.owncloud.android.syncadapter.FileSyncAdapter;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.PreferenceUtils;
import timber.log.Timber;

import java.util.ArrayList;

public class FolderPickerActivity extends FileActivity implements FileFragment.ContainerActivity,
        OnClickListener, OnEnforceableRefreshListener, MainFileListFragment.FileActions {

    public static final String EXTRA_FOLDER = FolderPickerActivity.class.getCanonicalName()
            + ".EXTRA_FOLDER";
    public static final String EXTRA_FILES = FolderPickerActivity.class.getCanonicalName()
            + ".EXTRA_FILES";

    private static final String TAG_LIST_OF_FOLDERS = "LIST_OF_FOLDERS";

    public static final String EXTRA_PICKER_OPTION = "EXTRA_PICKER_OPTION";

    private LocalBroadcastManager mLocalBroadcastManager;
    private SyncBroadcastReceiver mSyncBroadcastReceiver;
    private boolean mSyncInProgress = false;

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

        // sets message for empty list of folders
        setBackgroundText();

        // Set action button text
        setActionButtonText();

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

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
                listOfFolders.listDirectory(folder);

                startSyncFolderOperation(folder, false);
            }

            updateNavigationElementsInActionBar();
        }
    }

    private void initAndShowListOfFilesFragment() {
        MainFileListFragment mainListOfFiles = MainFileListFragment.newInstance(false, true);
        mainListOfFiles.setFileActions(this);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, mainListOfFiles, TAG_LIST_OF_FOLDERS);
        transaction.commit();
    }

    /**
     * Show a text message on screen view for notifying user if content is
     * loading or folder is empty
     */
    public void setBackgroundText() {
        MainFileListFragment listFragment = getListOfFilesFragment();
        if (listFragment != null) {
            int message = R.string.file_list_loading;
            if (!mSyncInProgress) {
                // In case folder list is empty
                message = R.string.file_list_empty_moving;
                listFragment.getProgressBar();
                listFragment.getProgressBar().setVisibility(View.GONE);
                listFragment.getShadowView();
                listFragment.getShadowView().setVisibility(View.VISIBLE);
            }
            listFragment.setMessageForEmptyList(getString(message));
        } else {
            Timber.e("MainFileListFragment is null");
        }
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

    /**
     * {@inheritDoc}
     * <p>
     * Updates action bar and second fragment, if in dual pane mode.
     */
    @Override
    public void onBrowsedDownTo(OCFile directory) {
        setFile(directory);
        updateNavigationElementsInActionBar();
        // Sync Folder
        startSyncFolderOperation(directory, false);

    }

    @Override
    public void onSavedCertificate() {
        startSyncFolderOperation(getCurrentDir(), false);
    }

    public void startSyncFolderOperation(OCFile folder, boolean ignoreETag) {

        mSyncInProgress = true;

        // perform folder synchronization
        SyncOperation synchFolderOp = new RefreshFolderOperation(
                folder,
                ignoreETag,
                getAccount(),
                getApplicationContext()
        );
        synchFolderOp.execute(getStorageManager(), this, null, null);

        MainFileListFragment fileListFragment = getListOfFilesFragment();
        if (fileListFragment != null) {
            fileListFragment.setProgressBarAsIndeterminate(true);
        }

        setBackgroundText();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Timber.d("onResume() start");

        // Listen for sync messages
        IntentFilter syncIntentFilter = new IntentFilter(FileSyncAdapter.EVENT_FULL_SYNC_START);
        syncIntentFilter.addAction(FileSyncAdapter.EVENT_FULL_SYNC_END);
        syncIntentFilter.addAction(FileSyncAdapter.EVENT_FULL_SYNC_FOLDER_CONTENTS_SYNCED);
        syncIntentFilter.addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED);
        syncIntentFilter.addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED);
        mSyncBroadcastReceiver = new SyncBroadcastReceiver();
        mLocalBroadcastManager.registerReceiver(mSyncBroadcastReceiver, syncIntentFilter);

        Timber.d("onResume() end");
    }

    @Override
    protected void onPause() {
        Timber.d("onPause() start");
        if (mSyncBroadcastReceiver != null) {
            mLocalBroadcastManager.unregisterReceiver(mSyncBroadcastReceiver);
            mSyncBroadcastReceiver = null;
        }

        Timber.d("onPause() end");
        super.onPause();
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
            OCFile file = listOfFiles.getCurrentFile();
            if (file != null) {
                if (file.isFolder()) {
                    return file;
                } else if (getStorageManager() != null) {
                    String parentPath = file.getRemotePath().substring(0,
                            file.getRemotePath().lastIndexOf(file.getFileName()));
                    return getStorageManager().getFileByPath(parentPath);
                }
            } else if (getStorageManager() != null) {
                return getStorageManager().getFileByPath(OCFile.ROOT_PATH);
            }
        }
        return null;
    }

    public void browseToRoot() {
        MainFileListFragment listOfFiles = getListOfFilesFragment();
        if (listOfFiles != null) {  // should never be null, indeed
            OCFile root = getStorageManager().getFileByPath(OCFile.ROOT_PATH);
            listOfFiles.listDirectory(root);
            setFile(listOfFiles.getCurrentFile());
            updateNavigationElementsInActionBar();
            startSyncFolderOperation(root, false);
        }
    }

    @Override
    public void onBackPressed() {
        MainFileListFragment listOfFiles = getListOfFilesFragment();
        if (listOfFiles != null) {  // should never be null, indeed
            OCFile fileBeforeBrowsingUp = listOfFiles.getCurrentFile();
            if (fileBeforeBrowsingUp != null &&
                    fileBeforeBrowsingUp.getParentId() != null &&
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
    public void onCurrentFolderUpdated(@NonNull OCFile newCurrentFolder) {
        updateNavigationElementsInActionBar();
        setFile(newCurrentFolder);
    }

    @Override
    public void setImagePreview(@NonNull OCFile file) {

    }

    @Override
    public void initTextPreview(@NonNull OCFile file) {

    }

    @Override
    public void initAudioPreview(@NonNull OCFile file) {

    }

    @Override
    public void initVideoPreview(@NonNull OCFile file) {

    }

    @Override
    public void startSyncAndOpenFile(@NonNull OCFile file) {

    }

    @Override
    public void initSync(@NonNull OCFile file) {

    }

    @Override
    public void initSyncAndOpenFile(@NonNull OCFile file) {

    }

    @Override
    public void initDownloadForSending(@NonNull OCFile file) {

    }

    @Override
    public void cancelFileTransference(@NonNull ArrayList<OCFile> file) {

    }

    @Override
    public void setBottomBarVisibility(boolean isVisible) {

    }

    private class SyncBroadcastReceiver extends BroadcastReceiver {

        /**
         * {@link BroadcastReceiver} to enable syncing feedback in UI
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getAction();
            Timber.d("Received broadcast %s", event);
            String accountName = intent.getStringExtra(FileSyncAdapter.EXTRA_ACCOUNT_NAME);
            String synchFolderRemotePath = intent.getStringExtra(FileSyncAdapter.EXTRA_FOLDER_PATH);
            RemoteOperationResult synchResult = (RemoteOperationResult) intent.
                    getSerializableExtra(FileSyncAdapter.EXTRA_RESULT);
            boolean sameAccount = (getAccount() != null &&
                    accountName.equals(getAccount().name) && getStorageManager() != null);

            if (sameAccount) {

                if (FileSyncAdapter.EVENT_FULL_SYNC_START.equals(event)) {
                    mSyncInProgress = true;

                } else {
                    OCFile currentFile = (getFile() == null) ? null :
                            getStorageManager().getFileByPath(getFile().getRemotePath());
                    OCFile currentDir = (getCurrentFolder() == null) ? null :
                            getStorageManager().getFileByPath(getCurrentFolder().getRemotePath());

                    if (currentDir == null) {
                        // current folder was removed from the server
                        showSnackMessage(
                                String.format(
                                        getString(R.string.sync_current_folder_was_removed),
                                        getCurrentFolder().getFileName()
                                )
                        );
                        browseToRoot();

                    } else {
                        if (currentFile == null && !getFile().isFolder()) {
                            // currently selected file was removed in the server, and now we know it
                            currentFile = currentDir;
                        }

                        if (currentDir.getRemotePath().equals(synchFolderRemotePath)) {
                            MainFileListFragment fileListFragment = getListOfFilesFragment();
                            if (fileListFragment != null) {
                                fileListFragment.listDirectory(currentDir);
                            }
                        }
                        setFile(currentFile);
                    }

                    mSyncInProgress = (!FileSyncAdapter.EVENT_FULL_SYNC_END.equals(event) &&
                            !RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED.equals(event));

                    if (RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED.
                            equals(event) &&
                            /// TODO refactor and make common
                            synchResult != null && !synchResult.isSuccess()) {

                        if (ResultCode.UNAUTHORIZED.equals(synchResult.getCode()) ||
                                (synchResult.isException() && synchResult.getException()
                                        instanceof AuthenticatorException)) {

                            requestCredentialsUpdate();

                        } else if (RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED.equals(synchResult.getCode())) {

                            showUntrustedCertDialog(synchResult);
                        }

                    }
                }
                Timber.d("Setting progress visibility to %s", mSyncInProgress);

                MainFileListFragment fileListFragment = getListOfFilesFragment();
                if (fileListFragment != null) {
                    fileListFragment.setProgressBarAsIndeterminate(mSyncInProgress);
                }

                setBackgroundText();
            }
        }
    }

    /**
     * Shows the information of the {@link OCFile} received as a
     * parameter in the second fragment.
     *
     * @param file {@link OCFile} whose details will be shown
     */
    @Override
    public void showDetails(OCFile file) {

    }

    @Override
    public void onRefresh() {
        refreshList(true);
    }

    @Override
    public void onRefresh(boolean enforced) {
        refreshList(enforced);
    }

    private void refreshList(boolean ignoreETag) {
        MainFileListFragment listOfFiles = getListOfFilesFragment();
        if (listOfFiles != null) {
            OCFile folder = listOfFiles.getCurrentFile();
            if (folder != null) {
                startSyncFolderOperation(folder, ignoreETag);
            }
        }
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
