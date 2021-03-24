/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2020 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hd that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.preview;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.ActionBar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.FileListOption;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.Extras;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.PreferenceUtils;
import timber.log.Timber;

import java.util.List;

/**
 * Holds a swiping galley where image files contained in an ownCloud directory are shown
 */
public class PreviewImageActivity extends FileActivity implements
        FileFragment.ContainerActivity,
        ViewPager.OnPageChangeListener, OnRemoteOperationListener {

    private static final int INITIAL_HIDE_DELAY = 0; // immediate hide

    private ViewPager mViewPager;
    private PreviewImagePagerAdapter mPreviewImagePagerAdapter;
    private int mSavedPosition = 0;
    private boolean mHasSavedPosition = false;

    private LocalBroadcastManager mLocalBroadcastManager;
    private DownloadFinishReceiver mDownloadFinishReceiver;

    private View mFullScreenAnchorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.preview_image_activity);

        // ActionBar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        showActionBar(false);

        /// FullScreen and Immersive Mode
        mFullScreenAnchorView = getWindow().getDecorView();
        // to keep our UI controls visibility in line with system bars
        // visibility
        mFullScreenAnchorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @SuppressLint("InlinedApi")
                    @Override
                    public void onSystemUiVisibilityChange(int flags) {
                        boolean visible = (flags & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;
                        if (visible) {
                            showActionBar(true);
                            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                        } else {
                            showActionBar(false);
                            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                        }
                    }
                });

        getWindow().setStatusBarColor(getResources().getColor(R.color.owncloud_blue_dark_transparent));

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

    }

    private void initViewPager() {
        // get parent from path
        String parentPath = getFile().getRemotePath().substring(0,
                getFile().getRemotePath().lastIndexOf(getFile().getFileName()));
        OCFile parentFolder = getStorageManager().getFileByPath(parentPath);
        if (parentFolder == null) {
            // should not be necessary
            parentFolder = getStorageManager().getFileByPath(OCFile.ROOT_PATH);
        }

        List<OCFile> imageFiles = getStorageManager().getFolderImages(parentFolder);
        imageFiles = FileStorageUtils.sortFolder(
                imageFiles, FileStorageUtils.mSortOrderFileDisp,
                FileStorageUtils.mSortAscendingFileDisp
        );

        mPreviewImagePagerAdapter = new PreviewImagePagerAdapter(
                getSupportFragmentManager(),
                getAccount(),
                imageFiles
        );

        mViewPager = findViewById(R.id.fragmentPager);
        mViewPager.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)
        );

        int position = mHasSavedPosition ? mSavedPosition :
                mPreviewImagePagerAdapter.getFilePosition(getFile());
        position = (position >= 0) ? position : 0;
        mViewPager.setAdapter(mPreviewImagePagerAdapter);
        mViewPager.addOnPageChangeListener(this);
        mViewPager.setCurrentItem(position);
        if (position == 0) {
            mViewPager.post(new Runnable() {
                // this is necessary because mViewPager.setCurrentItem(0) does not trigger
                // a call to onPageSelected in the first layout request aftet mViewPager.setAdapter(...) ;
                // see, for example:
                // https://android.googlesource.com/platform/frameworks/support.git/+/android-6.0
                // .1_r55/v4/java/android/support/v4/view/ViewPager.java#541
                // ; or just:
                // http://stackoverflow.com/questions/11794269/onpageselected-isnt-triggered-when-calling
                // -setcurrentitem0
                @Override
                public void run() {
                    PreviewImageActivity.this.onPageSelected(mViewPager.getCurrentItem());
                }
            });
        }
    }

    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available
        delayedHide(INITIAL_HIDE_DELAY);

    }

    Handler mHideSystemUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            hideSystemUI(mFullScreenAnchorView);
            showActionBar(false);
        }
    };

    private void delayedHide(int delayMillis) {
        mHideSystemUiHandler.removeMessages(0);
        mHideSystemUiHandler.sendEmptyMessageDelayed(0, delayMillis);
    }

    /// handle Window Focus changes
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // When the window loses focus (e.g. the action overflow is shown),
        // cancel any pending hide action.
        if (!hasFocus) {
            mHideSystemUiHandler.removeMessages(0);
        }
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        super.onRemoteOperationFinish(operation, result);

        if (operation instanceof RemoveFileOperation) {
            finish();
        } else if (operation instanceof SynchronizeFileOperation) {
            onSynchronizeFileOperationFinish((SynchronizeFileOperation) operation, result);

        }
    }

    private void onSynchronizeFileOperationFinish(SynchronizeFileOperation operation,
                                                  RemoteOperationResult result) {
        if (result.isSuccess()) {
            invalidateOptionsMenu();
        }
    }

    @Override
    protected ServiceConnection newTransferenceServiceConnection() {
        return new PreviewImageServiceConnection();
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private class PreviewImageServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {

            if (component.equals(new ComponentName(PreviewImageActivity.this,
                    FileDownloader.class))) {
                Timber.d("onServiceConnected, FileDownloader");
                mDownloaderBinder = (FileDownloaderBinder) service;

            } else if (component.equals(new ComponentName(PreviewImageActivity.this,
                    FileUploader.class))) {
                Timber.d("onServiceConnected, FileUploader");
                mUploaderBinder = (FileUploaderBinder) service;
            }

            mPreviewImagePagerAdapter.onTransferServiceConnected();

        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(PreviewImageActivity.this,
                    FileDownloader.class))) {
                Timber.d("Download service suddenly disconnected");
                mDownloaderBinder = null;
            } else if (component.equals(new ComponentName(PreviewImageActivity.this,
                    FileUploader.class))) {
                Timber.d("Upload service suddenly disconnected");
                mUploaderBinder = null;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean returnValue;

        switch (item.getItemId()) {
            case android.R.id.home:
                if (isDrawerOpen()) {
                    closeDrawer();
                } else {
                    backToDisplayActivity();
                }
                returnValue = true;
                break;
            default:
                returnValue = super.onOptionsItemSelected(item);
        }

        return returnValue;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mDownloadFinishReceiver = new DownloadFinishReceiver();

        IntentFilter filter = new IntentFilter(FileDownloader.getDownloadFinishMessage());
        filter.addAction(FileDownloader.getDownloadAddedMessage());
        mLocalBroadcastManager.registerReceiver(mDownloadFinishReceiver, filter);
    }

    @Override
    public void onPause() {
        if (mDownloadFinishReceiver != null) {
            mLocalBroadcastManager.unregisterReceiver(mDownloadFinishReceiver);
            mDownloadFinishReceiver = null;
        }

        super.onPause();
    }

    private void backToDisplayActivity() {
        finish();
    }

    @Override
    public void showDetails(OCFile file) {
        Intent showDetailsIntent = new Intent(this, FileDisplayActivity.class);
        showDetailsIntent.setAction(FileDisplayActivity.ACTION_DETAILS);
        showDetailsIntent.putExtra(FileActivity.EXTRA_FILE, file);
        showDetailsIntent.putExtra(FileActivity.EXTRA_ACCOUNT,
                AccountUtils.getCurrentOwnCloudAccount(this));
        startActivity(showDetailsIntent);
    }

    /**
     * This method will be invoked when a new page becomes selected. Animation is not necessarily
     * complete.
     *
     * @param position Position index of the new selected page
     */
    @Override
    public void onPageSelected(int position) {
        Timber.d("onPageSelected %s", position);

        if (getOperationsServiceBinder() != null) {
            mSavedPosition = position;
            mHasSavedPosition = true;

            OCFile currentFile = mPreviewImagePagerAdapter.getFileAt(position);
            updateActionBarTitle(currentFile.getFileName());
            if (!mPreviewImagePagerAdapter.pendingErrorAt(position)) {
                getFileOperationsHelper().syncFile(currentFile);
            }

            // Call to reset image zoom to initial state
            ((PreviewImagePagerAdapter) mViewPager.getAdapter()).resetZoom();

        } else {
            // too soon! ; selection of page (first image) was faster than binding of FileOperationsService;
            // wait a bit!
            final int fPosition = position;
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    onPageSelected(fPosition);
                }
            });
        }
    }

    /**
     * Called when the scroll state changes. Useful for discovering when the user begins dragging,
     * when the pager is automatically settling to the current page. when it is fully stopped/idle.
     *
     * @param state The new scroll state (SCROLL_STATE_IDLE, _DRAGGING, _SETTLING
     */
    @Override
    public void onPageScrollStateChanged(int state) {
    }

    /**
     * This method will be invoked when the current page is scrolled, either as part of a
     * programmatically initiated smooth scroll or a user initiated touch scroll.
     *
     * @param position             Position index of the first page currently being displayed.
     *                             Page position+1 will be visible if positionOffset is
     *                             nonzero.
     * @param positionOffset       Value from [0, 1) indicating the offset from the page
     *                             at position.
     * @param positionOffsetPixels Value in pixels indicating the offset from position.
     */
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    /**
     * Class waiting for broadcast events from the {@link FileDownloader} service.
     * <p>
     * Updates the UI when a download is started or finished, provided that it is relevant for the
     * folder displayed in the gallery.
     */
    private class DownloadFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String accountName = intent.getStringExtra(Extras.EXTRA_ACCOUNT_NAME);
            String downloadedRemotePath = intent.getStringExtra(Extras.EXTRA_REMOTE_PATH);
            if (getAccount().name.equals(accountName) &&
                    downloadedRemotePath != null) {

                OCFile file = getStorageManager().getFileByPath(downloadedRemotePath);
                mPreviewImagePagerAdapter.onDownloadEvent(
                        file,
                        intent.getAction(),
                        intent.getBooleanExtra(Extras.EXTRA_DOWNLOAD_RESULT, false)
                );
            }
        }

    }

    @SuppressLint("InlinedApi")
    public void toggleFullScreen() {
        boolean visible = (mFullScreenAnchorView.getSystemUiVisibility()
                & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;

        if (visible) {
            hideSystemUI(mFullScreenAnchorView);
            // actionBar.hide(); // propagated through OnSystemUiVisibilityChangeListener()
        } else {
            showSystemUI(mFullScreenAnchorView);
            // actionBar.show(); // propagated through OnSystemUiVisibilityChangeListener()
        }
    }

    @Override
    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(stateWasRecovered);
        if (getAccount() != null) {
            OCFile file = getFile();
            /// Validate handled file  (first image to preview)
            if (file == null) {
                throw new IllegalStateException("Instanced with a NULL OCFile");
            }
            if (!file.isImage()) {
                throw new IllegalArgumentException("Non-image file passed as argument");
            }

            // Update file according to DB file, if it is possible
            if (file.getId() > FileDataStorageManager.ROOT_PARENT_ID) {
                file = getStorageManager().getFileById(file.getId());
            }

            if (file != null) {
                /// Refresh the activity according to the Account and OCFile set
                setFile(file);  // reset after getting it fresh from storageManager
                updateActionBarTitle(getFile().getFileName());
                initViewPager();

            } else {
                // handled file not in the current Account
                finish();
            }
        }
    }

    @Override
    public void onBrowsedDownTo(OCFile folder) {
        // TODO Auto-generated method stub

    }

    @SuppressLint("InlinedApi")
    private void hideSystemUI(View anchorView) {
        anchorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION         // hides NAVIGATION BAR; Android >= 4.0
                        | View.SYSTEM_UI_FLAG_FULLSCREEN              // hides STATUS BAR;     Android >= 4.1
                        | View.SYSTEM_UI_FLAG_IMMERSIVE               // stays interactive;    Android >= 4.4
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE           // draw full window;     Android >= 4.1
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN       // draw full window;     Android >= 4.1
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION  // draw full window;     Android >= 4.1
        );
    }

    @SuppressLint("InlinedApi")
    private void showSystemUI(View anchorView) {
        anchorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE           // draw full window;     Android >= 4.1
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN       // draw full window;     Android >= 4.1
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION  // draw full window;     Android >= 4.1
        );
    }

    @Override
    public void navigateToOption(FileListOption fileListOption) {
        backToDisplayActivity();
        super.navigateToOption(fileListOption);
    }

    private void showActionBar(boolean show) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        if (show) {
            actionBar.show();
        } else {
            actionBar.hide();
        }
    }

    private void updateActionBarTitle(String title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

}