/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Shashvat Kedia
 * @author Abel García de Prada
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2019 ownCloud GmbH.
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

import android.Manifest;
import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SyncRequest;
import android.content.pm.PackageManager;
import android.content.res.Resources.NotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.material.snackbar.Snackbar;
import com.owncloud.android.AppRater;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.FingerprintManager;
import com.owncloud.android.authentication.PassCodeManager;
import com.owncloud.android.authentication.PatternManager;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.files.services.TransferRequester;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.CopyFileOperation;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.MoveFileOperation;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.syncadapter.FileSyncAdapter;
import com.owncloud.android.ui.errorhandling.ErrorMessageAdapter;
import com.owncloud.android.ui.fragment.FileDetailFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.ui.fragment.TaskRetainerFragment;
import com.owncloud.android.ui.helpers.FilesUploadHelper;
import com.owncloud.android.ui.helpers.UriUploader;
import com.owncloud.android.ui.preview.PreviewAudioFragment;
import com.owncloud.android.ui.preview.PreviewImageActivity;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.ui.preview.PreviewTextFragment;
import com.owncloud.android.ui.preview.PreviewVideoActivity;
import com.owncloud.android.ui.preview.PreviewVideoFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.Extras;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.PermissionUtil;
import com.owncloud.android.utils.PreferenceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.owncloud.android.db.PreferenceManager.getSortOrder;

/**
 * Displays, what files the user has available in his ownCloud. This is the main view.
 */

public class FileDisplayActivity extends FileActivity
        implements FileFragment.ContainerActivity, OnEnforceableRefreshListener {

    private SyncBroadcastReceiver mSyncBroadcastReceiver;
    private UploadBroadcastReceiver mUploadBroadcastReceiver;
    private DownloadBroadcastReceiver mDownloadBroadcastReceiver;
    private RemoteOperationResult mLastSslUntrustedServerResult = null;

    private View mLeftFragmentContainer;
    private View mRightFragmentContainer;
    private MenuItem mDescendingMenuItem;
    private MenuItem mSelectAllMenuItem;
    private Menu mMainMenu;

    private static final String KEY_WAITING_TO_PREVIEW = "WAITING_TO_PREVIEW";
    private static final String KEY_SYNC_IN_PROGRESS = "SYNC_IN_PROGRESS";
    private static final String KEY_WAITING_TO_SEND = "WAITING_TO_SEND";
    private static final String KEY_UPLOAD_HELPER = "FILE_UPLOAD_HELPER";

    public static final String ACTION_DETAILS = "com.owncloud.android.ui.activity.action.DETAILS";

    public static final int REQUEST_CODE__SELECT_CONTENT_FROM_APPS = REQUEST_CODE__LAST_SHARED + 1;
    public static final int REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM = REQUEST_CODE__LAST_SHARED + 2;
    public static final int REQUEST_CODE__MOVE_FILES = REQUEST_CODE__LAST_SHARED + 3;
    public static final int REQUEST_CODE__COPY_FILES = REQUEST_CODE__LAST_SHARED + 4;
    public static final int REQUEST_CODE__UPLOAD_FROM_CAMERA = REQUEST_CODE__LAST_SHARED + 5;

    private static final String TAG = FileDisplayActivity.class.getSimpleName();

    private static final String TAG_LIST_OF_FILES = "LIST_OF_FILES";
    private static final String TAG_SECOND_FRAGMENT = "SECOND_FRAGMENT";

    private OCFile mFileWaitingToPreview;

    private boolean mSyncInProgress = false;
    private boolean mOnlyAvailableOffline = false;

    private OCFile mWaitingToSend;

    private LocalBroadcastManager mLocalBroadcastManager;

    FilesUploadHelper mFilesUploadHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");

        super.onCreate(savedInstanceState); // this calls onAccountChanged() when ownCloud Account is valid

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        /// Load of saved instance state
        if (savedInstanceState != null) {
            Log.d(TAG, savedInstanceState.toString());
            
            mFileWaitingToPreview = savedInstanceState.getParcelable(FileDisplayActivity.KEY_WAITING_TO_PREVIEW);
            mSyncInProgress = savedInstanceState.getBoolean(KEY_SYNC_IN_PROGRESS);
            mWaitingToSend = savedInstanceState.getParcelable(FileDisplayActivity.KEY_WAITING_TO_SEND);
            mFilesUploadHelper = savedInstanceState.getParcelable(KEY_UPLOAD_HELPER);
            if (getAccount() != null) {
                mFilesUploadHelper.init(this, getAccount().name);
            }
        } else {
            mFileWaitingToPreview = null;
            mSyncInProgress = false;
            mWaitingToSend = null;

            mFilesUploadHelper = new FilesUploadHelper(this,
                    getAccount() == null ? "" : getAccount().name);
        }

        // Check if only available offline option is set
        mOnlyAvailableOffline = getIntent().getBooleanExtra(FileActivity.EXTRA_ONLY_AVAILABLE_OFFLINE, false);

        /// USER INTERFACE

        // Inflate and set the layout view
        setContentView(R.layout.files);

        // setup toolbar
        setupToolbar();


        // setup drawer
        if(!mOnlyAvailableOffline) {
            setupDrawer(R.id.nav_all_files);
        } else {
            setupDrawer(R.id.nav_only_available_offline);
        }

        mLeftFragmentContainer = findViewById(R.id.left_fragment_container);
        mRightFragmentContainer = findViewById(R.id.right_fragment_container);

        // Action bar setup
        getSupportActionBar().setHomeButtonEnabled(true);

        // Init Fragment without UI to retain AsyncTask across configuration changes
        FragmentManager fm = getSupportFragmentManager();
        TaskRetainerFragment taskRetainerFragment =
                (TaskRetainerFragment) fm.findFragmentByTag(TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT);
        if (taskRetainerFragment == null) {
            taskRetainerFragment = new TaskRetainerFragment();
            fm.beginTransaction()
                    .add(taskRetainerFragment, TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT).commit();
        }   // else, Fragment already created and retained across configuration change

        Log_OC.v(TAG, "onCreate() end");

        if (getResources().getBoolean(R.bool.enable_rate_me_feature) && !MainApp.Companion.isBeta()) {
            AppRater.appLaunched(this, getPackageName());
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (PermissionUtil.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Check if we should show an explanation
            if (PermissionUtil.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show explanation to the user and then request permission
                Snackbar snackbar = Snackbar.make(findViewById(R.id.ListLayout), R.string.permission_storage_access,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                PermissionUtil.requestWriteExternalStoreagePermission(FileDisplayActivity.this);
                            }
                        });

                DisplayUtils.colorSnackbar(this, snackbar);

                snackbar.show();
            } else {
                // No explanation needed, request the permission.
                PermissionUtil.requestWriteExternalStoreagePermission(this);
            }
        }

        if (savedInstanceState == null) {
            createMinFragments();
        }

        setBackgroundText();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PermissionUtil.PERMISSIONS_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    startSyncFolderOperation(getFile(), false);
                } else {
                    // permission denied --> do nothing
                }
                return;
            }
        }
    }

    @Override
    protected void onStart() {
        Log_OC.v(TAG, "onStart() start");
        super.onStart();
        Log_OC.v(TAG, "onStart() end");
    }

    @Override
    protected void onStop() {
        Log_OC.v(TAG, "onStop() start");
        super.onStop();
        Log_OC.v(TAG, "onStop() end");
    }

    @Override
    protected void onDestroy() {
        Log_OC.v(TAG, "onDestroy() start");
        super.onDestroy();
        Log_OC.v(TAG, "onDestroy() end");
    }

    /**
     * Called when the ownCloud {@link Account} associated to the Activity was just updated.
     */
    @Override
    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(stateWasRecovered);
        if (getAccount() != null) {
            /// Check whether the 'main' OCFile handled by the Activity is contained in the
            // current Account
            OCFile file = getFile();
            // get parent from path
            String parentPath = "";
            if (file != null) {
                if (file.isDown() && file.getLastSyncDateForProperties() == 0) {
                    // upload in progress - right now, files are not inserted in the local
                    // cache until the upload is successful get parent from path
                    parentPath = file.getRemotePath().substring(0,
                            file.getRemotePath().lastIndexOf(file.getFileName()));
                    if (getStorageManager().getFileByPath(parentPath) == null) {
                        file = null; // not able to know the directory where the file is uploading
                    }
                } else {
                    file = getStorageManager().getFileByPath(file.getRemotePath());
                    // currentDir = null if not in the current Account
                }
            }
            if (file == null) {
                // fall back to root folder
                file = getStorageManager().getFileByPath(OCFile.ROOT_PATH);  // never returns null
            }
            setFile(file);

            if (mAccountWasSet) {
                setAccountInDrawer(getAccount());
            }

            if (!stateWasRecovered) {
                Log_OC.d(TAG, "Initializing Fragments in onAccountChanged..");
                initFragmentsWithFile();
                if (file.isFolder()) {
                    startSyncFolderOperation(file, false);
                }

            } else {
                updateFragmentsVisibility(!file.isFolder());
                updateActionBarTitleAndHomeButton(file.isFolder() ? null : file);
            }
        }
    }

    private void createMinFragments() {
        OCFileListFragment listOfFiles = OCFileListFragment.newInstance(false, mOnlyAvailableOffline, false, true);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.left_fragment_container, listOfFiles, TAG_LIST_OF_FILES);
        transaction.commit();
    }

    private void initFragmentsWithFile() {
        if (getAccount() != null && getFile() != null) {
            /// First fragment
            OCFileListFragment listOfFiles = getListOfFilesFragment();
            if (listOfFiles != null) {
                listOfFiles.listDirectory(getCurrentDir());

            } else {
                Log_OC.e(TAG, "Still have a chance to lose the initializacion of list fragment >(");
            }

            /// Second fragment
            OCFile file = getFile();
            Fragment secondFragment = chooseInitialSecondFragment(file);
            if (secondFragment != null) {
                setSecondFragment(secondFragment);
                updateFragmentsVisibility(true);
                updateActionBarTitleAndHomeButton(file);
            } else {
                cleanSecondFragment();
            }

        } else {
            Log_OC.e(TAG, "initFragments() called with invalid NULLs!");
            if (getAccount() == null) {
                Log_OC.e(TAG, "\t account is NULL");
            }
            if (getFile() == null) {
                Log_OC.e(TAG, "\t file is NULL");
            }
        }
    }

    /**
     * Choose the second fragment that is going to be shown
     *
     * @param file used to decide which fragment should be chosen
     * @return a new second fragment instance if it has not been chosen before, or the fragment
     * previously chosen otherwhise
     */
    private Fragment chooseInitialSecondFragment(OCFile file) {

        Fragment secondFragment = getSupportFragmentManager().findFragmentByTag(TAG_SECOND_FRAGMENT);

        if (secondFragment == null) { // If second fragment has not been chosen yet, choose it
            if (file != null && !file.isFolder()) {
                if ((PreviewAudioFragment.canBePreviewed(file) || PreviewVideoFragment.canBePreviewed(file)) &&
                        file.getLastSyncDateForProperties() > 0  // temporal fix
                ) {
                    int startPlaybackPosition =
                            getIntent().getIntExtra(PreviewVideoActivity.EXTRA_START_POSITION, 0);
                    boolean autoplay =
                            getIntent().getBooleanExtra(PreviewVideoActivity.EXTRA_AUTOPLAY, true);

                    if (PreviewAudioFragment.canBePreviewed(file)) {

                        secondFragment = PreviewAudioFragment.newInstance(
                                file,
                                getAccount(),
                                startPlaybackPosition,
                                autoplay
                        );

                    } else {

                        secondFragment = PreviewVideoFragment.newInstance(
                                file,
                                getAccount(),
                                startPlaybackPosition,
                                autoplay
                        );
                    }

                } else if (PreviewTextFragment.canBePreviewed(file)) {
                    secondFragment = PreviewTextFragment.newInstance(
                            file,
                            getAccount()
                    );

                } else {
                    secondFragment = FileDetailFragment.newInstance(file, getAccount());
                }
            }
        }

        return secondFragment;
    }

    /**
     * Replaces the second fragment managed by the activity with the received as
     * a parameter.
     * <p/>
     * Assumes never will be more than two fragments managed at the same time.
     *
     * @param fragment New second Fragment to set.
     */
    private void setSecondFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.right_fragment_container, fragment, TAG_SECOND_FRAGMENT);
        transaction.commit();
    }

    private void updateFragmentsVisibility(boolean existsSecondFragment) {
        if (existsSecondFragment) {
            if (mLeftFragmentContainer.getVisibility() != View.GONE) {
                mLeftFragmentContainer.setVisibility(View.GONE);
            }
            if (mRightFragmentContainer.getVisibility() != View.VISIBLE) {
                mRightFragmentContainer.setVisibility(View.VISIBLE);
            }

        } else {
            if (mLeftFragmentContainer.getVisibility() != View.VISIBLE) {
                mLeftFragmentContainer.setVisibility(View.VISIBLE);
            }
            if (mRightFragmentContainer.getVisibility() != View.GONE) {
                mRightFragmentContainer.setVisibility(View.GONE);
            }
        }
    }

    private OCFileListFragment getListOfFilesFragment() {
        Fragment listOfFiles = getSupportFragmentManager().findFragmentByTag(
                FileDisplayActivity.TAG_LIST_OF_FILES);
        if (listOfFiles != null) {
            return (OCFileListFragment) listOfFiles;
        }
        Log_OC.e(TAG, "Access to unexisting list of files fragment!!");
        return null;
    }

    public FileFragment getSecondFragment() {
        Fragment second = getSupportFragmentManager().findFragmentByTag(
                FileDisplayActivity.TAG_SECOND_FRAGMENT);
        if (second != null) {
            return (FileFragment) second;
        }
        return null;
    }

    protected void cleanSecondFragment() {
        Fragment second = getSecondFragment();
        if (second != null) {
            FragmentTransaction tr = getSupportFragmentManager().beginTransaction();
            tr.remove(second);
            tr.commit();
        }
        updateFragmentsVisibility(false);
        updateActionBarTitleAndHomeButton(null);
    }

    protected void refreshListOfFilesFragment(boolean reloadData) {
        OCFileListFragment fileListFragment = getListOfFilesFragment();
        if (fileListFragment != null) {
            fileListFragment.listDirectory(reloadData);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = isDrawerOpen();
        menu.findItem(R.id.action_sync_account).setVisible(!drawerOpen);
        menu.findItem(R.id.action_switch_view).setVisible(!drawerOpen);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        // Allow or disallow touches with other visible windows
        View actionBarView = findViewById(R.id.action_bar);
        if (actionBarView != null) {
            actionBarView.setFilterTouchesWhenObscured(
                    PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(getApplicationContext())
            );
        }

        inflater.inflate(R.menu.main_menu, menu);
        inflater.inflate(R.menu.sort_menu, menu.findItem(R.id.action_sort).getSubMenu());
        menu.findItem(R.id.action_create_dir).setVisible(false);

        mDescendingMenuItem = menu.findItem(R.id.action_sort_descending);
        mSelectAllMenuItem = menu.findItem(R.id.action_select_all);
        if (getSecondFragment() == null) {
            mSelectAllMenuItem.setVisible(true);
        }
        mMainMenu = menu;

        recoverSortMenuFormPreferences(menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_select_all: {
                getListOfFilesFragment().selectAll();
                break;
            }
            case R.id.action_sync_account: {
                startSynchronization();
                break;
            }
            case android.R.id.home: {
                FileFragment second = getSecondFragment();
                OCFile currentDir = getCurrentDir();

                boolean inRootFolder = currentDir != null && currentDir.getParentId() == 0;
                boolean fileFragmentVisible = second != null && second.getFile() != null;

                if (!inRootFolder || (fileFragmentVisible)) {
                    onBackPressed();
                } else if (isDrawerOpen()) {
                    closeDrawer();
                } else {
                    openDrawer();
                }
                break;
            }
            case R.id.action_sort_descending: {
                item.setChecked(!item.isChecked());
                final boolean sortAscending = !item.isChecked();
                PreferenceManager.setSortAscending(sortAscending, this, FileStorageUtils.FILE_DISPLAY_SORT);
                switch (getSortOrder(this, FileStorageUtils.FILE_DISPLAY_SORT)) {
                    case FileStorageUtils.SORT_NAME:
                        sortByName(sortAscending);
                        break;
                    case FileStorageUtils.SORT_DATE:
                        sortByDate(sortAscending);
                        break;
                    case FileStorageUtils.SORT_SIZE:
                        sortBySize(sortAscending);
                }
                break;
            }
            case R.id.action_switch_view: {
                if (isGridView()) {
                    item.setTitle(getString(R.string.action_switch_grid_view));
                    item.setIcon(ContextCompat.getDrawable(getApplicationContext(),
                            R.drawable.ic_view_module));
                    getListOfFilesFragment().setListAsPreferred();
                } else {
                    item.setTitle(getApplicationContext().getString(R.string.action_switch_list_view));
                    item.setIcon(ContextCompat.getDrawable(getApplicationContext(),
                            R.drawable.ic_view_list));
                    getListOfFilesFragment().setGridAsPreferred();
                }
                return true;
            }
            case R.id.action_sort_by_date:
                item.setChecked(true);
                sortByDate(PreferenceManager.getSortAscending(this, FileStorageUtils.FILE_DISPLAY_SORT));
                return true;
            case R.id.action_sort_by_name:
                item.setChecked(true);
                sortByName(PreferenceManager.getSortAscending(this, FileStorageUtils.FILE_DISPLAY_SORT));
                return true;
            case R.id.action_sort_by_size:
                item.setChecked(true);
                sortBySize(PreferenceManager.getSortAscending(this, FileStorageUtils.FILE_DISPLAY_SORT));
                return true;
            default:
        }

        return super.onOptionsItemSelected(item);
    }

    private void recoverSortMenuFormPreferences(Menu menu) {
        // setup sort menu
        if (menu != null) {
            mDescendingMenuItem.setChecked(!PreferenceManager.getSortAscending(this,
                    FileStorageUtils.FILE_DISPLAY_SORT));

            switch (getSortOrder(this, FileStorageUtils.FILE_DISPLAY_SORT)) {
                case FileStorageUtils.SORT_NAME:
                    menu.findItem(R.id.action_sort_by_name).setChecked(true);
                    break;
                case FileStorageUtils.SORT_DATE:
                    menu.findItem(R.id.action_sort_by_date).setChecked(true);
                    break;
                case FileStorageUtils.SORT_SIZE:
                    menu.findItem(R.id.action_sort_by_size).setChecked(true);
                default:
            }
        }
    }

    private void startSynchronization() {
        Log_OC.d(TAG, "Got to start sync");
        Log_OC.d(TAG, "Requesting sync for " + getAccount().name + " at " +
                MainApp.Companion.getAuthority() + " with new API");
        SyncRequest.Builder builder = new SyncRequest.Builder();
        builder.setSyncAdapter(getAccount(), MainApp.Companion.getAuthority());
        builder.setExpedited(true);
        builder.setManual(true);
        builder.syncOnce();

        // Fix bug in Android Lollipop when you click on refresh the whole account
        Bundle extras = new Bundle();
        builder.setExtras(extras);

        SyncRequest request = builder.build();
        ContentResolver.requestSync(request);
    }

    /**
     * Called, when the user selected something for uploading
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            FingerprintManager.getFingerprintManager(this).bayPassUnlockOnce();
        }
        PassCodeManager.getPassCodeManager().bayPassUnlockOnce();
        PatternManager.getPatternManager().bayPassUnlockOnce();

        // Hanndle calls form internal activities.
        if (requestCode == REQUEST_CODE__SELECT_CONTENT_FROM_APPS &&
                (resultCode == RESULT_OK || resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE)) {

            requestUploadOfContentFromApps(data, resultCode);

        } else if (requestCode == REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM &&
                (resultCode == RESULT_OK || resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE)) {

            requestUploadOfFilesFromFileSystem(data, resultCode);

        } else if (requestCode == REQUEST_CODE__UPLOAD_FROM_CAMERA) {
            if (resultCode == RESULT_OK || resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE) {
                mFilesUploadHelper.onActivityResult(new FilesUploadHelper.OnCheckAvailableSpaceListener() {
                    @Override
                    public void onCheckAvailableSpaceStart() {

                    }

                    @Override
                    public void onCheckAvailableSpaceFinished(boolean hasEnoughSpace, String[] capturedFilePaths) {
                        if (hasEnoughSpace) {
                            requestUploadOfFilesFromFileSystem(capturedFilePaths, FileUploader.LOCAL_BEHAVIOUR_MOVE);
                        }
                    }
                });
            } else if (requestCode == RESULT_CANCELED) {
                mFilesUploadHelper.deleteImageFile();
            }

            // requestUploadOfFilesFromFileSystem(data,resultCode);
        } else if (requestCode == REQUEST_CODE__MOVE_FILES && resultCode == RESULT_OK) {
            final Intent fData = data;
            getHandler().postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            requestMoveOperation(fData);
                        }
                    },
                    DELAY_TO_REQUEST_OPERATIONS_LATER
            );

        } else if (requestCode == REQUEST_CODE__COPY_FILES && resultCode == RESULT_OK) {

            final Intent fData = data;
            final int fResultCode = resultCode;
            getHandler().postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            requestCopyOperation(fData);
                        }
                    },
                    DELAY_TO_REQUEST_OPERATIONS_LATER
            );

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void requestUploadOfFilesFromFileSystem(Intent data, int resultCode) {
        String[] filePaths = data.getStringArrayExtra(UploadFilesActivity.EXTRA_CHOSEN_FILES);
        int behaviour = (resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE)
                ? FileUploader.LOCAL_BEHAVIOUR_MOVE : FileUploader.LOCAL_BEHAVIOUR_COPY;
        requestUploadOfFilesFromFileSystem(filePaths, behaviour);
    }

    private void requestUploadOfFilesFromFileSystem(String[] filePaths, int behaviour) {
        if (filePaths != null) {
            String[] remotePaths = new String[filePaths.length];
            String remotePathBase = getCurrentDir().getRemotePath();
            for (int j = 0; j < remotePaths.length; j++) {
                remotePaths[j] = remotePathBase + (new File(filePaths[j])).getName();
            }

            TransferRequester requester = new TransferRequester();
            requester.uploadNewFiles(
                    this,
                    getAccount(),
                    filePaths,
                    remotePaths,
                    null,           // MIME type will be detected from file name
                    behaviour,
                    false,          // do not create parent folder if not existent
                    UploadFileOperation.CREATED_BY_USER
            );

        } else {
            Log_OC.d(TAG, "User clicked on 'Update' with no selection");
            showSnackMessage(getString(R.string.filedisplay_no_file_selected));
        }
    }

    private void requestUploadOfContentFromApps(Intent contentIntent, int resultCode) {

        ArrayList<Parcelable> streamsToUpload = new ArrayList<>();

        //getClipData is only supported on api level 16+, Jelly Bean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
                contentIntent.getClipData() != null &&
                contentIntent.getClipData().getItemCount() > 0) {

            for (int i = 0; i < contentIntent.getClipData().getItemCount(); i++) {
                streamsToUpload.add(contentIntent.getClipData().getItemAt(i).getUri());
            }

        } else {
            streamsToUpload.add(contentIntent.getData());
        }

        int behaviour = (resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE) ? FileUploader.LOCAL_BEHAVIOUR_MOVE :
                FileUploader.LOCAL_BEHAVIOUR_COPY;

        OCFile currentDir = getCurrentDir();
        String remotePath = (currentDir != null) ? currentDir.getRemotePath() : OCFile.ROOT_PATH;

        UriUploader uploader = new UriUploader(
                this,
                streamsToUpload,
                remotePath,
                getAccount(),
                behaviour,
                false, // Not show waiting dialog while file is being copied from private storage
                null  // Not needed copy temp task listener
        );

        uploader.uploadUris();
    }

    /**
     * Request the operation for moving the file/folder from one path to another
     *
     * @param data Intent received
     */
    private void requestMoveOperation(Intent data) {
        OCFile folderToMoveAt = data.getParcelableExtra(FolderPickerActivity.EXTRA_FOLDER);
        ArrayList<OCFile> files = data.getParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES);
        getFileOperationsHelper().moveFiles(files, folderToMoveAt);
    }

    /**
     * Request the operation for copying the file/folder from one path to another
     *
     * @param data Intent received
     */
    private void requestCopyOperation(Intent data) {
        OCFile folderToMoveAt = data.getParcelableExtra(FolderPickerActivity.EXTRA_FOLDER);
        ArrayList<OCFile> files = data.getParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES);
        getFileOperationsHelper().copyFiles(files, folderToMoveAt);
    }

    @Override
    public void onBackPressed() {
        boolean isFabOpen = isFabOpen();
        boolean isDrawerOpen = isDrawerOpen();

        /*
         * BackPressed priority/hierarchy:
         *    1. close drawer if opened
         *    2. close FAB if open (only if drawer isn't open)
         *    3. navigate up (only if drawer and FAB aren't open)
         */
        if (isDrawerOpen && isFabOpen) {
            // close drawer first
            super.onBackPressed();
        } else if (isDrawerOpen && !isFabOpen) {
            // close drawer
            super.onBackPressed();
        } else if (!isDrawerOpen && isFabOpen) {
            // close fab
            getListOfFilesFragment().getFabMain().collapse();
        } else {
            // all closed
            OCFileListFragment listOfFiles = getListOfFilesFragment();
            if (getSecondFragment() == null) {
                OCFile currentDir = getCurrentDir();
                if (currentDir == null || currentDir.getParentId() == FileDataStorageManager.ROOT_PARENT_ID) {
                    if(mOnlyAvailableOffline){
                        allFilesOption();
                    }else{
                        finish();
                    }
                    return;
                }
                if (listOfFiles != null) {  // should never be null, indeed
                    listOfFiles.onBrowseUp();
                }
            }
            if (listOfFiles != null) {  // should never be null, indeed
                setFile(listOfFiles.getCurrentFile());
                listOfFiles.listDirectory(false);
            }
            cleanSecondFragment();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // responsibility of restore is preferred in onCreate() before than in
        // onRestoreInstanceState when there are Fragments involved
        Log_OC.v(TAG, "onSaveInstanceState() start");
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileDisplayActivity.KEY_WAITING_TO_PREVIEW, mFileWaitingToPreview);
        outState.putBoolean(FileDisplayActivity.KEY_SYNC_IN_PROGRESS, mSyncInProgress);
        //outState.putBoolean(FileDisplayActivity.KEY_REFRESH_SHARES_IN_PROGRESS,
        // mRefreshSharesInProgress);
        outState.putParcelable(FileDisplayActivity.KEY_WAITING_TO_SEND, mWaitingToSend);
        outState.putParcelable(KEY_UPLOAD_HELPER, mFilesUploadHelper);

        Log_OC.v(TAG, "onSaveInstanceState() end");
    }

    public Menu getMainMenu() {
        return mMainMenu;
    }

    @Override
    protected void onResume() {
        Log_OC.v(TAG, "onResume() start");
        super.onResume();

        // refresh list of files
        refreshListOfFilesFragment(true);

        // Listen for sync messages
        IntentFilter syncIntentFilter = new IntentFilter(FileSyncAdapter.EVENT_FULL_SYNC_START);
        syncIntentFilter.addAction(FileSyncAdapter.EVENT_FULL_SYNC_END);
        syncIntentFilter.addAction(FileSyncAdapter.EVENT_FULL_SYNC_FOLDER_CONTENTS_SYNCED);
        syncIntentFilter.addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED);
        syncIntentFilter.addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED);
        mSyncBroadcastReceiver = new SyncBroadcastReceiver();
        mLocalBroadcastManager.registerReceiver(mSyncBroadcastReceiver, syncIntentFilter);

        // Listen for upload messages
        IntentFilter uploadIntentFilter = new IntentFilter(FileUploader.getUploadFinishMessage());
        uploadIntentFilter.addAction(FileUploader.getUploadStartMessage());
        mUploadBroadcastReceiver = new UploadBroadcastReceiver();
        mLocalBroadcastManager.registerReceiver(mUploadBroadcastReceiver, uploadIntentFilter);

        // Listen for download messages
        IntentFilter downloadIntentFilter = new IntentFilter(
                FileDownloader.getDownloadAddedMessage());
        downloadIntentFilter.addAction(FileDownloader.getDownloadFinishMessage());
        mDownloadBroadcastReceiver = new DownloadBroadcastReceiver();
        mLocalBroadcastManager.registerReceiver(mDownloadBroadcastReceiver, downloadIntentFilter);

        recoverSortMenuFormPreferences(mMainMenu);

        Log_OC.v(TAG, "onResume() end");

    }

    @Override
    protected void onPause() {
        Log_OC.v(TAG, "onPause() start");
        if (mSyncBroadcastReceiver != null) {
            mLocalBroadcastManager.unregisterReceiver(mSyncBroadcastReceiver);
            mSyncBroadcastReceiver = null;
        }
        if (mUploadBroadcastReceiver != null) {
            mLocalBroadcastManager.unregisterReceiver(mUploadBroadcastReceiver);
            mUploadBroadcastReceiver = null;
        }
        if (mDownloadBroadcastReceiver != null) {
            mLocalBroadcastManager.unregisterReceiver(mDownloadBroadcastReceiver);
            mDownloadBroadcastReceiver = null;
        }

        super.onPause();
        Log_OC.v(TAG, "onPause() end");
    }

    public boolean isFabOpen() {
        return getListOfFilesFragment() != null
                && getListOfFilesFragment().getFabMain() != null
                && getListOfFilesFragment().getFabMain().isExpanded();
    }

    private class SyncBroadcastReceiver extends BroadcastReceiver {

        /**
         * {@link BroadcastReceiver} to enable syncing feedback in UI
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getAction();
            Log_OC.d(TAG, "Received broadcast " + event);
            String accountName = intent.getStringExtra(FileSyncAdapter.EXTRA_ACCOUNT_NAME);

            String synchFolderRemotePath =
                    intent.getStringExtra(FileSyncAdapter.EXTRA_FOLDER_PATH);
            RemoteOperationResult synchResult =
                    (RemoteOperationResult) intent.getSerializableExtra(
                            FileSyncAdapter.EXTRA_RESULT);
            boolean sameAccount = (getAccount() != null &&
                    accountName.equals(getAccount().name) && getStorageManager() != null);

            if (sameAccount) {

                if (FileSyncAdapter.EVENT_FULL_SYNC_START.equals(event)) {
                    mSyncInProgress = true;

                } else {
                    OCFile currentFile = (getFile() == null) ? null :
                            getStorageManager().getFileByPath(getFile().getRemotePath());
                    OCFile currentDir = (getCurrentDir() == null) ? null :
                            getStorageManager().getFileByPath(getCurrentDir().getRemotePath());

                    if (currentDir == null) {
                        // current folder was removed from the server
                        showSnackMessage(
                                String.format(
                                        getString(R.string.sync_current_folder_was_removed),
                                        synchFolderRemotePath
                                )
                        );
                        browseToRoot();

                    } else {
                        if (currentFile == null && !getFile().isFolder()) {
                            // currently selected file was removed in the server, and now we
                            // know it
                            cleanSecondFragment();
                            currentFile = currentDir;
                        }

                        if (synchFolderRemotePath != null &&
                                currentDir.getRemotePath().equals(synchFolderRemotePath)) {
                            OCFileListFragment fileListFragment = getListOfFilesFragment();
                            if (fileListFragment != null) {
                                fileListFragment.listDirectory(true);
                            }
                        }
                        setFile(currentFile);
                    }

                    mSyncInProgress = (!FileSyncAdapter.EVENT_FULL_SYNC_END.equals(event) &&
                            !RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED
                                    .equals(event));

                    if (RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED.
                            equals(event)) {

                        if (synchResult != null && !synchResult.isSuccess()) {
                            /// TODO refactor and make common

                            if (ResultCode.UNAUTHORIZED.equals(synchResult.getCode()) ||
                                    (synchResult.isException() && synchResult.getException()
                                            instanceof AuthenticatorException)) {

                                // If we have saml enabled we consider the user to only have
                                // one account with which he is logged into the app. This is because
                                // only branded versions of the app have saml support.
                                if (getString(R.string.auth_method_saml_web_sso).equals("on")) {
                                    requestCredentialsUpdate();
                                } else {
                                    showRequestAccountChangeNotice();
                                }

                            } else if (RemoteOperationResult.ResultCode.
                                    SSL_RECOVERABLE_PEER_UNVERIFIED.equals(
                                    synchResult.getCode())) {

                                showUntrustedCertDialog(synchResult);
                            }

                        }

                        if (synchFolderRemotePath.equals(OCFile.ROOT_PATH)) {
                            setAccountInDrawer(getAccount());
                        }
                    }

                }

                OCFileListFragment fileListFragment = getListOfFilesFragment();
                if (fileListFragment != null) {
                    fileListFragment.setProgressBarAsIndeterminate(mSyncInProgress);
                }
                Log_OC.d(TAG, "Setting progress visibility to " + mSyncInProgress);

                setBackgroundText();
            }

            if (synchResult != null) {
                if (synchResult.getCode().equals(
                        RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED)) {
                    mLastSslUntrustedServerResult = synchResult;
                } else if (synchResult.getCode().equals(RemoteOperationResult.ResultCode.SPECIFIC_SERVICE_UNAVAILABLE)) {
                    if (synchResult.getHttpCode() == 503) {
                        if (synchResult.getHttpPhrase()
                                .equals("Error: Call to a member function getUID() on null")) {
                            showRequestAccountChangeNotice();
                        } else {
                            showSnackMessage(synchResult.getHttpPhrase());
                        }
                    } else {
                        showRequestAccountChangeNotice();
                    }
                }
            }
        }
    }

    /**
     * Show a text message on screen view for notifying user if content is
     * loading or folder is empty
     */
    public void setBackgroundText() {
        OCFileListFragment ocFileListFragment = getListOfFilesFragment();
        if (ocFileListFragment != null) {
            if (mSelectAllMenuItem != null) {
                mSelectAllMenuItem.setVisible(true);
                if (ocFileListFragment.getNoOfItems() == 0) {
                    mSelectAllMenuItem.setVisible(false);
                }
            }
            int message = R.string.file_list_loading;
            if (!mSyncInProgress) {
                // In case file list is empty
                message = mOnlyAvailableOffline ? R.string.file_list_empty_available_offline : R.string.file_list_empty;
                ocFileListFragment.getProgressBar().setVisibility(View.GONE);
                ocFileListFragment.getShadowView().setVisibility(View.VISIBLE);
            }
            ocFileListFragment.setMessageForEmptyList(getString(message));
        } else {
            Log_OC.e(TAG, "OCFileListFragment is null");
        }
    }

    /**
     * Once the file upload has finished -> update view
     */
    private class UploadBroadcastReceiver extends BroadcastReceiver {
        /**
         * Once the file upload has finished -> update view
         *
         * @author David A. Velasco
         * {@link BroadcastReceiver} to enable upload feedback in UI
         */
        @SuppressLint("StringFormatInvalid")
        @Override
        public void onReceive(Context context, Intent intent) {
            String uploadedRemotePath = intent.getStringExtra(Extras.EXTRA_REMOTE_PATH);
            String accountName = intent.getStringExtra(Extras.EXTRA_ACCOUNT_NAME);
            boolean sameAccount = getAccount() != null && accountName.equals(getAccount().name);
            OCFile currentDir = getCurrentDir();
            boolean isDescendant =
                    (currentDir != null) &&
                            (uploadedRemotePath != null) &&
                            (uploadedRemotePath.startsWith(currentDir.getRemotePath()));
            boolean renamedInUpload = getFile().getRemotePath().equals(
                    intent.getStringExtra(Extras.EXTRA_OLD_REMOTE_PATH)
            );
            boolean sameFile = renamedInUpload ||
                    getFile().getRemotePath().equals(uploadedRemotePath);
            boolean success = intent.getBooleanExtra(Extras.EXTRA_UPLOAD_RESULT, false);

            if (sameAccount && isDescendant) {
                String linkedToRemotePath =
                        intent.getStringExtra(Extras.EXTRA_LINKED_TO_PATH);
                if (linkedToRemotePath == null || isAscendant(linkedToRemotePath)) {
                    refreshListOfFilesFragment(true);
                }
            }

            if (sameAccount && sameFile) {
                if (success) {
                    setFile(getStorageManager().getFileByPath(uploadedRemotePath));
                }
                refreshSecondFragment(
                        intent.getAction(),
                        success
                );
                if (renamedInUpload) {
                    String newName = (new File(uploadedRemotePath)).getName();
                    showSnackMessage(
                            String.format(
                                    getString(R.string.filedetails_renamed_in_upload_msg),
                                    newName
                            )
                    );
                    updateActionBarTitleAndHomeButton(getFile());
                }
            }
        }

        private void refreshSecondFragment(String uploadEvent, boolean success) {

            FileFragment secondFragment = getSecondFragment();

            if (secondFragment != null) {
                if (!success && !getFile().fileExists()) {
                    cleanSecondFragment();
                } else {
                    OCFile file = getFile();
                    boolean fragmentReplaced = false;
                    if (success && secondFragment instanceof FileDetailFragment) {
                        // start preview if previewable
                        fragmentReplaced = true;
                        if (PreviewImageFragment.canBePreviewed(file)) {
                            startImagePreview(file);
                        } else if (PreviewAudioFragment.canBePreviewed(file)) {
                            startAudioPreview(file, 0);
                        } else if (PreviewVideoFragment.canBePreviewed(file)) {
                            startVideoPreview(file, 0);
                        } else if (PreviewTextFragment.canBePreviewed(file)) {
                            startTextPreview(file);
                        } else {
                            fragmentReplaced = false;
                        }
                    }
                    if (!fragmentReplaced) {
                        secondFragment.onSyncEvent(uploadEvent, success, file);
                    }
                }
            }
        }

        // TODO refactor this receiver, and maybe DownloadBroadcastReceiver; this method is duplicated :S
        private boolean isAscendant(String linkedToRemotePath) {
            OCFile currentDir = getCurrentDir();
            return (
                    currentDir != null &&
                            currentDir.getRemotePath().startsWith(linkedToRemotePath)
            );
        }

    }

    /**
     * Class waiting for broadcast events from the {@link FileDownloader} service.
     * <p/>
     * Updates the UI when a download is started or finished, provided that it is relevant for the
     * current folder.
     */
    private class DownloadBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean sameAccount = isSameAccount(intent);
            String downloadedRemotePath =
                    intent.getStringExtra(Extras.EXTRA_REMOTE_PATH);
            boolean isDescendant = isDescendant(downloadedRemotePath);

            if (sameAccount && isDescendant) {
                String linkedToRemotePath =
                        intent.getStringExtra(Extras.EXTRA_LINKED_TO_PATH);
                if (linkedToRemotePath == null || isAscendant(linkedToRemotePath)) {
                    refreshListOfFilesFragment(true);
                }
                refreshSecondFragment(
                        intent.getAction(),
                        downloadedRemotePath,
                        intent.getBooleanExtra(Extras.EXTRA_DOWNLOAD_RESULT, false)
                );
                invalidateOptionsMenu();
            }

            if (mWaitingToSend != null) {
                mWaitingToSend =
                        getStorageManager().getFileByPath(mWaitingToSend.getRemotePath());
                if (mWaitingToSend.isDown()) {
                    sendDownloadedFile();
                }
            }
        }

        private boolean isDescendant(String downloadedRemotePath) {
            OCFile currentDir = getCurrentDir();
            return (
                    currentDir != null &&
                            downloadedRemotePath != null &&
                            downloadedRemotePath.startsWith(currentDir.getRemotePath())
            );
        }

        private boolean isAscendant(String linkedToRemotePath) {
            OCFile currentDir = getCurrentDir();
            return (
                    currentDir != null &&
                            currentDir.getRemotePath().startsWith(linkedToRemotePath)
            );
        }

        private boolean isSameAccount(Intent intent) {
            String accountName = intent.getStringExtra(Extras.EXTRA_ACCOUNT_NAME);
            return (accountName != null && getAccount() != null &&
                    accountName.equals(getAccount().name));
        }

        protected void refreshSecondFragment(String downloadEvent, String downloadedRemotePath,
                                             boolean success) {
            FileFragment secondFragment = getSecondFragment();
            if (secondFragment != null) {
                boolean fragmentReplaced = false;
                if (secondFragment instanceof FileDetailFragment) {
                    /// user was watching download progress
                    FileDetailFragment detailsFragment = (FileDetailFragment) secondFragment;
                    OCFile fileInFragment = detailsFragment.getFile();
                    if (fileInFragment != null &&
                            !downloadedRemotePath.equals(fileInFragment.getRemotePath())) {
                        // the user browsed to other file ; forget the automatic preview
                        mFileWaitingToPreview = null;

                    } else if (downloadEvent.equals(FileDownloader.getDownloadFinishMessage())) {
                        //  replace the right panel if waiting for preview
                        boolean waitedPreview = (
                                mFileWaitingToPreview != null &&
                                        mFileWaitingToPreview.getRemotePath().equals(downloadedRemotePath)
                        );
                        if (waitedPreview) {
                            if (success) {
                                // update the file from database, to get the local storage path
                                mFileWaitingToPreview = getStorageManager().getFileById(
                                        mFileWaitingToPreview.getFileId()
                                );
                                if (PreviewAudioFragment.canBePreviewed(mFileWaitingToPreview)) {
                                    fragmentReplaced = true;
                                    startAudioPreview(mFileWaitingToPreview, 0);
                                } else if (PreviewVideoFragment.canBePreviewed(mFileWaitingToPreview)) {
                                    fragmentReplaced = true;
                                    startVideoPreview(mFileWaitingToPreview, 0);
                                } else if (PreviewTextFragment.canBePreviewed(mFileWaitingToPreview)) {
                                    fragmentReplaced = true;
                                    startTextPreview(mFileWaitingToPreview);
                                } else {
                                    getFileOperationsHelper().openFile(mFileWaitingToPreview);
                                }
                            }
                            mFileWaitingToPreview = null;
                        }
                    }
                }
                if (!fragmentReplaced && downloadedRemotePath.equals(secondFragment.getFile().getRemotePath())) {
                    secondFragment.onSyncEvent(downloadEvent, success, null);
                }
            }
        }
    }

    public void browseToRoot() {
        OCFileListFragment listOfFiles = getListOfFilesFragment();
        if (listOfFiles != null) {  // should never be null, indeed
            OCFile root = getStorageManager().getFileByPath(OCFile.ROOT_PATH);
            listOfFiles.listDirectory(root);
            setFile(listOfFiles.getCurrentFile());
            startSyncFolderOperation(root, false);
        }
        cleanSecondFragment();
    }

    /**
     * {@inheritDoc}
     * Updates action bar and second fragment, if in dual pane mode.
     */
    @Override
    public void onBrowsedDownTo(OCFile directory) {
        setFile(directory);
        cleanSecondFragment();
        // Sync Folder
        startSyncFolderOperation(directory, false);
    }

    /**
     * Shows the information of the {@link OCFile} received as a
     * parameter in the second fragment.
     *
     * @param file {@link OCFile} whose details will be shown
     */
    @Override
    public void showDetails(OCFile file) {
        Fragment detailFragment = FileDetailFragment.newInstance(file, getAccount());
        setSecondFragment(detailFragment);
        updateFragmentsVisibility(true);
        updateActionBarTitleAndHomeButton(file);
        setFile(file);
    }

    @Override
    protected void updateActionBarTitleAndHomeButton(OCFile chosenFile) {
        if (chosenFile == null) {
            chosenFile = getFile();     // if no file is passed, current file decides
        }
        super.updateActionBarTitleAndHomeButton(chosenFile);
        if(chosenFile.getRemotePath().equals(OCFile.ROOT_PATH) && mOnlyAvailableOffline) {
            updateActionBarTitleAndHomeButtonByString(
                    getResources().getString(R.string.drawer_item_only_available_offline));
        }
    }

    @Override
    protected ServiceConnection newTransferenceServiceConnection() {
        return new ListServiceConnection();
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private class ListServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
            if (component.equals(new ComponentName(
                    FileDisplayActivity.this, FileDownloader.class))) {
                Log_OC.d(TAG, "Download service connected");
                mDownloaderBinder = (FileDownloaderBinder) service;

                if (mFileWaitingToPreview != null) {
                    if (getStorageManager() != null) {
                        // update the file
                        mFileWaitingToPreview =
                                getStorageManager().getFileById(mFileWaitingToPreview.getFileId());
                        if (!mFileWaitingToPreview.isDown()) {
                            // If the file to preview isn't downloaded yet, check if it is being
                            // downloaded in this moment or not
                            requestForDownload();
                        }
                    }
                }

                if (getFile() != null && mDownloaderBinder.isDownloading(getAccount(), getFile())) {

                    // If the file is being downloaded, assure that the fragment to show is details
                    // fragment, not the streaming video fragment which has been previously
                    // set in chooseInitialSecondFragment method

                    FileFragment secondFragment = getSecondFragment();
                    if (secondFragment != null && secondFragment instanceof PreviewVideoFragment) {
                        cleanSecondFragment();

                        showDetails(getFile());
                    }
                }

            } else if (component.equals(new ComponentName(FileDisplayActivity.this,
                    FileUploader.class))) {
                Log_OC.d(TAG, "Upload service connected");
                mUploaderBinder = (FileUploaderBinder) service;
            } else {
                return;
            }
            OCFileListFragment listOfFiles = getListOfFilesFragment();
            if (listOfFiles != null) {
                listOfFiles.listDirectory(false);
            }
            FileFragment secondFragment = getSecondFragment();
            if (secondFragment != null) {
                secondFragment.onTransferServiceConnected();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(FileDisplayActivity.this,
                    FileDownloader.class))) {
                Log_OC.d(TAG, "Download service disconnected");
                mDownloaderBinder = null;
            } else if (component.equals(new ComponentName(FileDisplayActivity.this,
                    FileUploader.class))) {
                Log_OC.d(TAG, "Upload service disconnected");
                mUploaderBinder = null;
            }
        }
    }

    /**
     * Updates the view associated to the activity after the finish of some operation over files
     * in the current account.
     *
     * @param operation Removal operation performed.
     * @param result    Result of the removal.
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        super.onRemoteOperationFinish(operation, result);

        if (operation instanceof RemoveFileOperation) {
            onRemoveFileOperationFinish((RemoveFileOperation) operation, result);

        } else if (operation instanceof RenameFileOperation) {
            onRenameFileOperationFinish((RenameFileOperation) operation, result);

        } else if (operation instanceof SynchronizeFileOperation) {
            onSynchronizeFileOperationFinish((SynchronizeFileOperation) operation, result);

        } else if (operation instanceof CreateFolderOperation) {
            onCreateFolderOperationFinish((CreateFolderOperation) operation, result);

        } else if (operation instanceof MoveFileOperation) {
            onMoveFileOperationFinish((MoveFileOperation) operation, result);

        } else if (operation instanceof CopyFileOperation) {
            onCopyFileOperationFinish((CopyFileOperation) operation, result);
        }

    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to
     * remove a file.
     *
     * @param operation Removal operation performed.
     * @param result    Result of the removal.
     */
    private void onRemoveFileOperationFinish(RemoveFileOperation operation,
                                             RemoteOperationResult result) {

        if (getListOfFilesFragment().isSingleItemChecked() || result.isException() || !result.isSuccess()) {
            showSnackMessage(
                    ErrorMessageAdapter.Companion.getResultMessage(result, operation, getResources())
            );
        }

        if (result.isSuccess()) {
            OCFile removedFile = operation.getFile();
            FileFragment second = getSecondFragment();
            if (second != null && removedFile.equals(second.getFile())) {
                if (second instanceof PreviewAudioFragment) {
                    ((PreviewAudioFragment) second).stopPreview();
                } else if (second instanceof PreviewVideoFragment) {
                    ((PreviewVideoFragment) second).releasePlayer();
                }
                setFile(getStorageManager().getFileById(removedFile.getParentId()));
                cleanSecondFragment();
            }
            if (getStorageManager().getFileById(removedFile.getParentId()).equals(getCurrentDir())) {
                refreshListOfFilesFragment(true);
            }
            invalidateOptionsMenu();
        } else {
            if (result.isSslRecoverableException()) {
                mLastSslUntrustedServerResult = result;
                showUntrustedCertDialog(mLastSslUntrustedServerResult);
            }
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to move a
     * file.
     *
     * @param operation Move operation performed.
     * @param result    Result of the move operation.
     */
    private void onMoveFileOperationFinish(MoveFileOperation operation,
                                           RemoteOperationResult result) {
        if (result.isSuccess()) {
            refreshListOfFilesFragment(true);
        } else {
            try {
                showSnackMessage(
                        ErrorMessageAdapter.Companion.getResultMessage(result, operation, getResources())
                );

            } catch (NotFoundException e) {
                Log_OC.e(TAG, "Error while trying to show fail message ", e);
            }
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to copy a
     * file.
     *
     * @param operation Copy operation performed.
     * @param result    Result of the copy operation.
     */
    private void onCopyFileOperationFinish(CopyFileOperation operation, RemoteOperationResult result) {
        if (result.isSuccess()) {
            refreshListOfFilesFragment(true);
        } else {
            try {
                showSnackMessage(
                        ErrorMessageAdapter.Companion.getResultMessage(result, operation, getResources())
                );

            } catch (NotFoundException e) {
                Log_OC.e(TAG, "Error while trying to show fail message ", e);
            }
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to rename
     * a file.
     *
     * @param operation Renaming operation performed.
     * @param result    Result of the renaming.
     */
    private void onRenameFileOperationFinish(RenameFileOperation operation,
                                             RemoteOperationResult result) {
        OCFile renamedFile = operation.getFile();
        if (result.isSuccess()) {
            FileFragment details = getSecondFragment();
            if (details != null && renamedFile.equals(details.getFile())) {
                renamedFile = getStorageManager().getFileById(renamedFile.getFileId());
                setFile(renamedFile);
                details.onFileMetadataChanged(renamedFile);
                updateActionBarTitleAndHomeButton(renamedFile);
            }

            if (getStorageManager().getFileById(renamedFile.getParentId()).equals(getCurrentDir())) {
                refreshListOfFilesFragment(true);
            }

        } else {
            showSnackMessage(
                    ErrorMessageAdapter.Companion.getResultMessage(result, operation, getResources())
            );

            if (result.isSslRecoverableException()) {
                mLastSslUntrustedServerResult = result;
                showUntrustedCertDialog(mLastSslUntrustedServerResult);
            }
        }
    }

    private void onSynchronizeFileOperationFinish(SynchronizeFileOperation operation,
                                                  RemoteOperationResult result) {
        if (result.isSuccess()) {
            if (operation.transferWasRequested()) {
                // this block is probably useless duy
                OCFile syncedFile = operation.getLocalFile();
                refreshListOfFilesFragment(false);
                FileFragment secondFragment = getSecondFragment();
                if (secondFragment != null && syncedFile.equals(secondFragment.getFile())) {
                    secondFragment.onSyncEvent(FileDownloader.getDownloadAddedMessage(), false, null);
                    invalidateOptionsMenu();
                }

            } else if (getSecondFragment() == null) {
                showSnackMessage(
                        ErrorMessageAdapter.Companion.getResultMessage(result, operation, getResources())
                );
            }
        }

        /// no matter if sync was right or not - if there was no transfer and the file is down, OPEN it
        boolean waitedForPreview = (
                mFileWaitingToPreview != null &&
                        mFileWaitingToPreview.equals(operation.getLocalFile())
                        && mFileWaitingToPreview.isDown()
        );
        if (!operation.transferWasRequested() & waitedForPreview) {
            getFileOperationsHelper().openFile(mFileWaitingToPreview);
            mFileWaitingToPreview = null;
        }

    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying create a
     * new folder
     *
     * @param operation Creation operation performed.
     * @param result    Result of the creation.
     */
    private void onCreateFolderOperationFinish(CreateFolderOperation operation,
                                               RemoteOperationResult result) {
        if (result.isSuccess()) {
            refreshListOfFilesFragment(true);
        } else {
            try {
                showSnackMessage(
                        ErrorMessageAdapter.Companion.getResultMessage(result, operation, getResources())
                );
            } catch (NotFoundException e) {
                Log_OC.e(TAG, "Error while trying to show fail message ", e);
            }
        }
    }

    private void requestForDownload() {
        Account account = getAccount();

        //if (!mFileWaitingToPreview.isDownloading()) {
        // If the file is not being downloaded, start the download
        if (!mDownloaderBinder.isDownloading(account, mFileWaitingToPreview)) {
            Intent i = new Intent(this, FileDownloader.class);
            i.putExtra(FileDownloader.KEY_ACCOUNT, account);
            i.putExtra(FileDownloader.KEY_FILE, mFileWaitingToPreview);
            startService(i);
        }
    }

    @Override
    public void onSavedCertificate() {
        startSyncFolderOperation(getCurrentDir(), false);
    }

    /**
     * Starts an operation to refresh the requested folder.
     * <p>
     * The operation is run in a new background thread created on the fly.
     * <p>
     * The refresh updates is a "light sync": properties of regular files in folder are updated (including
     * associated shares), but not their contents. Only the contents of files marked to be kept-in-sync are
     * synchronized too.
     *
     * @param folder     Folder to refresh.
     * @param ignoreETag If 'true', the data from the server will be fetched and sync'ed even if the eTag
     *                   didn't change.
     */
    public void startSyncFolderOperation(final OCFile folder, final boolean ignoreETag) {

        // the execution is slightly delayed to allow the activity get the window focus if it's being started
        // or if the method is called from a dialog that is being dismissed
        getHandler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (hasWindowFocus()) {
                            mSyncInProgress = true;

                            // perform folder synchronization
                            SyncOperation synchFolderOp = new RefreshFolderOperation(
                                    folder,
                                    getFileOperationsHelper().isSharedSupported(),
                                    ignoreETag,
                                    getAccount(),
                                    getApplicationContext()
                            );
                            synchFolderOp.execute(
                                    getStorageManager(),
                                    MainApp.Companion.getAppContext(),
                                    null,   // unneeded, handling via SyncBroadcastReceiver
                                    null
                            );

                            OCFileListFragment fileListFragment = getListOfFilesFragment();
                            if (fileListFragment != null) {
                                fileListFragment.setProgressBarAsIndeterminate(true);
                            }

                            setBackgroundText();
                        }   // else: NOTHING ; lets' not refresh when the user rotates the device but there is
                        // another window floating over
                    }
                },
                DELAY_TO_REQUEST_OPERATIONS_LATER + 350
        );

    }

    private void requestForDownload(OCFile file) {
        Account account = getAccount();
        if (!mDownloaderBinder.isDownloading(account, mFileWaitingToPreview)) {
            Intent i = new Intent(this, FileDownloader.class);
            i.putExtra(FileDownloader.KEY_ACCOUNT, account);
            i.putExtra(FileDownloader.KEY_FILE, file);
            startService(i);
        }
    }

    private void sendDownloadedFile() {
        getFileOperationsHelper().sendDownloadedFile(mWaitingToSend);
        mWaitingToSend = null;
    }

    /**
     * Requests the download of the received {@link OCFile} , updates the UI
     * to monitor the download progress and prepares the activity to send the file
     * when the download finishes.
     *
     * @param file {@link OCFile} to download and preview.
     */
    public void startDownloadForSending(OCFile file) {
        mWaitingToSend = file;
        requestForDownload(mWaitingToSend);
        boolean hasSecondFragment = (getSecondFragment() != null);
        updateFragmentsVisibility(hasSecondFragment);
    }

    /**
     * Opens the image gallery showing the image {@link OCFile} received as parameter.
     *
     * @param file Image {@link OCFile} to show.
     */
    public void startImagePreview(OCFile file) {
        Intent showDetailsIntent = new Intent(this, PreviewImageActivity.class);
        showDetailsIntent.putExtra(EXTRA_FILE, file);
        showDetailsIntent.putExtra(EXTRA_ACCOUNT, getAccount());
        startActivity(showDetailsIntent);

    }

    /**
     * Stars the preview of an already down audio {@link OCFile}.
     *
     * @param file                  Media {@link OCFile} to preview.
     * @param startPlaybackPosition Media position where the playback will be started,
     *                              in milliseconds.
     */
    public void startAudioPreview(OCFile file, int startPlaybackPosition) {
        Fragment mediaFragment = PreviewAudioFragment.newInstance(
                file,
                getAccount(),
                startPlaybackPosition,
                true
        );
        setSecondFragment(mediaFragment);
        updateFragmentsVisibility(true);
        updateActionBarTitleAndHomeButton(file);
        setFile(file);
    }

    /**
     * Stars the preview of an already down video {@link OCFile}.
     *
     * @param file                  Media {@link OCFile} to preview.
     * @param startPlaybackPosition Media position where the playback will be started,
     *                              in milliseconds.
     */
    public void startVideoPreview(OCFile file, int startPlaybackPosition) {
        Fragment mediaFragment = PreviewVideoFragment.newInstance(
                file,
                getAccount(),
                startPlaybackPosition,
                true
        );
        setSecondFragment(mediaFragment);
        updateFragmentsVisibility(true);
        updateActionBarTitleAndHomeButton(file);
        setFile(file);
    }

    /**
     * Stars the preview of a text file {@link OCFile}.
     *
     * @param file Text {@link OCFile} to preview.
     */
    public void startTextPreview(OCFile file) {
        Fragment textPreviewFragment = PreviewTextFragment.newInstance(
                file,
                getAccount()
        );
        setSecondFragment(textPreviewFragment);
        updateFragmentsVisibility(true);
        updateActionBarTitleAndHomeButton(file);
        setFile(file);
    }

    /**
     * Requests the synchronization of the received {@link OCFile},
     * updates the UI to monitor the progress and prepares the activity
     * to preview or open the file when the download finishes.
     *
     * @param file {@link OCFile} to sync and open.
     */
    public void startSyncThenOpen(OCFile file) {
        FileDetailFragment detailFragment = FileDetailFragment.newInstance(file, getAccount());
        setSecondFragment(detailFragment);
        mFileWaitingToPreview = file;
        getFileOperationsHelper().syncFile(file);
        updateFragmentsVisibility(true);
        updateActionBarTitleAndHomeButton(file);
        setFile(file);
    }

    /**
     * Request stopping the upload/download operation in progress over the given {@link OCFile} file.
     *
     * @param file {@link OCFile} file which operation are wanted to be cancel
     */
    public void cancelTransference(OCFile file) {
        getFileOperationsHelper().cancelTransference(file);
        if (mFileWaitingToPreview != null &&
                mFileWaitingToPreview.getRemotePath().equals(file.getRemotePath())) {
            mFileWaitingToPreview = null;
        }
        if (mWaitingToSend != null &&
                mWaitingToSend.getRemotePath().equals(file.getRemotePath())) {
            mWaitingToSend = null;
        }
        refreshListOfFilesFragment(false);

        FileFragment secondFragment = getSecondFragment();
        if (secondFragment != null && file.equals(secondFragment.getFile())) {
            if (!file.fileExists()) {
                cleanSecondFragment();
            } else {
                secondFragment.onSyncEvent(
                        FileDownloader.getDownloadFinishMessage(),
                        false,
                        null
                );
            }
        }

        invalidateOptionsMenu();
    }

    /**
     * Request stopping all upload/download operations in progress over the given {@link OCFile} files.
     *
     * @param files list of {@link OCFile} files which operations are wanted to be cancel
     */
    public void cancelTransference(List<OCFile> files) {
        for (OCFile file : files) {
            cancelTransference(file);
        }
    }

    @Override
    public void onRefresh(boolean ignoreETag) {
        refreshList(ignoreETag);
    }

    @Override
    public void onRefresh() {
        refreshList(true);
    }

    private void refreshList(boolean ignoreETag) {
        OCFileListFragment listOfFiles = getListOfFilesFragment();
        if (listOfFiles != null) {
            OCFile folder = listOfFiles.getCurrentFile();
            if (folder != null) {
                /*mFile = mContainerActivity.getStorageManager().getFileById(mFile.getFileId());
                listDirectory(mFile);*/
                startSyncFolderOperation(folder, ignoreETag);
            }
        }
    }

    private void sortByDate(boolean ascending) {
        getListOfFilesFragment().sortByDate(ascending);
    }

    private void sortBySize(boolean ascending) {
        getListOfFilesFragment().sortBySize(ascending);
    }

    private void sortByName(boolean ascending) {
        getListOfFilesFragment().sortByName(ascending);
    }

    private boolean isGridView() {
        return getListOfFilesFragment().isGridEnabled();
    }

    public void allFilesOption() {
        if(mOnlyAvailableOffline){
            super.allFilesOption();
        }else{
            browseToRoot();
        }
    }

    public void onlyAvailableOfflineOption() {
        if(!mOnlyAvailableOffline){
            super.onlyAvailableOfflineOption();
        }else{
            browseToRoot();
        }
    }

    public FilesUploadHelper getFilesUploadHelper() {
        return mFilesUploadHelper;
    }
}