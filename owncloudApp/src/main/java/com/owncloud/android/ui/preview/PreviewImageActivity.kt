/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * Copyright (C) 2020 ownCloud GmbH.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hd that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.preview

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.view.MenuItem
import android.view.View
import android.view.Window
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.files.services.FileDownloader
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.operations.SynchronizeFileOperation
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.FileListOption
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.preview.PreviewImageActivity
import com.owncloud.android.utils.Extras
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.PreferenceUtils
import timber.log.Timber

/**
 * Holds a swiping galley where image files contained in an ownCloud directory are shown
 */
class PreviewImageActivity : FileActivity(), FileFragment.ContainerActivity, OnPageChangeListener,
    OnRemoteOperationListener {
    private var mViewPager: ViewPager? = null
    private var mPreviewImagePagerAdapter: PreviewImagePagerAdapter? = null
    private var mSavedPosition = 0
    private var mHasSavedPosition = false
    private var mLocalBroadcastManager: LocalBroadcastManager? = null
    private var mDownloadFinishReceiver: DownloadFinishReceiver? = null
    private var mFullScreenAnchorView: View? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.preview_image_activity)

        // ActionBar
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeButtonEnabled(true)
        }
        showActionBar(false)

        /// FullScreen and Immersive Mode
        mFullScreenAnchorView = window.decorView
        // to keep our UI controls visibility in line with system bars
        // visibility
        mFullScreenAnchorView!!.setOnSystemUiVisibilityChangeListener { flags ->
            val visible = flags and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0
            if (visible) {
                showActionBar(true)
                setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            } else {
                showActionBar(false)
                setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }
        }
        window.statusBarColor = resources.getColor(R.color.owncloud_blue_dark_transparent)
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this)
    }

    private fun initViewPager() {
        // get parent from path
        val parentPath = file.remotePath.substring(
            0,
            file.remotePath.lastIndexOf(file.fileName)
        )
        var parentFolder = storageManager.getFileByPath(parentPath)
        if (parentFolder == null) {
            // should not be necessary
            parentFolder = storageManager.getFileByPath(OCFile.ROOT_PATH)
        }
        var imageFiles: List<OCFile?>? = storageManager.getFolderImages(parentFolder)
        imageFiles = FileStorageUtils.sortFolder(
            imageFiles, FileStorageUtils.mSortOrderFileDisp,
            FileStorageUtils.mSortAscendingFileDisp
        )
        mPreviewImagePagerAdapter = PreviewImagePagerAdapter(
            supportFragmentManager,
            account,
            imageFiles
        )
        mViewPager = findViewById(R.id.fragmentPager)
        mViewPager.setFilterTouchesWhenObscured(
            PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this)
        )
        var position = if (mHasSavedPosition) mSavedPosition else mPreviewImagePagerAdapter!!.getFilePosition(file)
        position = if (position >= 0) position else 0
        mViewPager.setAdapter(mPreviewImagePagerAdapter)
        mViewPager.addOnPageChangeListener(this)
        mViewPager.setCurrentItem(position)
        if (position == 0) {
            mViewPager.post(Runnable
            // this is necessary because mViewPager.setCurrentItem(0) does not trigger
            // a call to onPageSelected in the first layout request aftet mViewPager.setAdapter(...) ;
            // see, for example:
            // https://android.googlesource.com/platform/frameworks/support.git/+/android-6.0
            // .1_r55/v4/java/android/support/v4/view/ViewPager.java#541
            // ; or just:
            // http://stackoverflow.com/questions/11794269/onpageselected-isnt-triggered-when-calling
            // -setcurrentitem0
            { onPageSelected(mViewPager.getCurrentItem()) })
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available
        delayedHide(INITIAL_HIDE_DELAY)
    }

    var mHideSystemUiHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            hideSystemUI(mFullScreenAnchorView)
            showActionBar(false)
        }
    }

    private fun delayedHide(delayMillis: Int) {
        mHideSystemUiHandler.removeMessages(0)
        mHideSystemUiHandler.sendEmptyMessageDelayed(0, delayMillis.toLong())
    }

    /// handle Window Focus changes
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        // When the window loses focus (e.g. the action overflow is shown),
        // cancel any pending hide action.
        if (!hasFocus) {
            mHideSystemUiHandler.removeMessages(0)
        }
    }

    override fun onRemoteOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        super.onRemoteOperationFinish(operation, result)
        if (operation is RemoveFileOperation) {
            finish()
        } else if (operation is SynchronizeFileOperation) {
            onSynchronizeFileOperationFinish(operation, result)
        }
    }

    private fun onSynchronizeFileOperationFinish(
        operation: SynchronizeFileOperation,
        result: RemoteOperationResult<*>
    ) {
        if (result.isSuccess) {
            invalidateOptionsMenu()
        }
    }

    override fun newTransferenceServiceConnection(): ServiceConnection {
        return PreviewImageServiceConnection()
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private inner class PreviewImageServiceConnection : ServiceConnection {
        override fun onServiceConnected(component: ComponentName, service: IBinder) {
            if (component == ComponentName(
                    this@PreviewImageActivity,
                    FileDownloader::class.java
                )
            ) {
                Timber.d("onServiceConnected, FileDownloader")
                mDownloaderBinder = service as FileDownloaderBinder
            } else if (component == ComponentName(
                    this@PreviewImageActivity,
                    FileUploader::class.java
                )
            ) {
                Timber.d("onServiceConnected, FileUploader")
                mUploaderBinder = service as FileUploaderBinder
            }
            mPreviewImagePagerAdapter!!.onTransferServiceConnected()
        }

        override fun onServiceDisconnected(component: ComponentName) {
            if (component == ComponentName(
                    this@PreviewImageActivity,
                    FileDownloader::class.java
                )
            ) {
                Timber.d("Download service suddenly disconnected")
                mDownloaderBinder = null
            } else if (component == ComponentName(
                    this@PreviewImageActivity,
                    FileUploader::class.java
                )
            ) {
                Timber.d("Upload service suddenly disconnected")
                mUploaderBinder = null
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val returnValue: Boolean
        returnValue = when (item.itemId) {
            android.R.id.home -> {
                if (isDrawerOpen()) {
                    closeDrawer()
                } else {
                    backToDisplayActivity()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
        return returnValue
    }

    override fun onResume() {
        super.onResume()
        mDownloadFinishReceiver = DownloadFinishReceiver()
        val filter = IntentFilter(FileDownloader.getDownloadFinishMessage())
        filter.addAction(FileDownloader.getDownloadAddedMessage())
        mLocalBroadcastManager!!.registerReceiver(mDownloadFinishReceiver!!, filter)
    }

    public override fun onPause() {
        if (mDownloadFinishReceiver != null) {
            mLocalBroadcastManager!!.unregisterReceiver(mDownloadFinishReceiver!!)
            mDownloadFinishReceiver = null
        }
        super.onPause()
    }

    private fun backToDisplayActivity() {
        finish()
    }

    override fun showDetails(file: OCFile) {
        val showDetailsIntent = Intent(this, FileDisplayActivity::class.java)
        showDetailsIntent.action = FileDisplayActivity.ACTION_DETAILS
        showDetailsIntent.putExtra(EXTRA_FILE, file)
        showDetailsIntent.putExtra(
            EXTRA_ACCOUNT,
            AccountUtils.getCurrentOwnCloudAccount(this)
        )
        startActivity(showDetailsIntent)
    }

    /**
     * This method will be invoked when a new page becomes selected. Animation is not necessarily
     * complete.
     *
     * @param position Position index of the new selected page
     */
    override fun onPageSelected(position: Int) {
        Timber.d("onPageSelected %s", position)
        if (operationsServiceBinder != null) {
            mSavedPosition = position
            mHasSavedPosition = true
            val currentFile = mPreviewImagePagerAdapter!!.getFileAt(position)
            updateActionBarTitle(currentFile.fileName)
            if (!mPreviewImagePagerAdapter!!.pendingErrorAt(position)) {
                fileOperationsHelper.syncFile(currentFile)
            }

            // Call to reset image zoom to initial state
            (mViewPager!!.adapter as PreviewImagePagerAdapter?)!!.resetZoom()
        } else {
            // too soon! ; selection of page (first image) was faster than binding of FileOperationsService;
            // wait a bit!
            handler.post { onPageSelected(position) }
        }
    }

    /**
     * Called when the scroll state changes. Useful for discovering when the user begins dragging,
     * when the pager is automatically settling to the current page. when it is fully stopped/idle.
     *
     * @param state The new scroll state (SCROLL_STATE_IDLE, _DRAGGING, _SETTLING
     */
    override fun onPageScrollStateChanged(state: Int) {}

    /**
     * This method will be invoked when the current page is scrolled, either as part of a
     * programmatically initiated smooth scroll or a user initiated touch scroll.
     *
     * @param position             Position index of the first page currently being displayed.
     * Page position+1 will be visible if positionOffset is
     * nonzero.
     * @param positionOffset       Value from [0, 1) indicating the offset from the page
     * at position.
     * @param positionOffsetPixels Value in pixels indicating the offset from position.
     */
    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    /**
     * Class waiting for broadcast events from the [FileDownloader] service.
     *
     *
     * Updates the UI when a download is started or finished, provided that it is relevant for the
     * folder displayed in the gallery.
     */
    private inner class DownloadFinishReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val accountName = intent.getStringExtra(Extras.EXTRA_ACCOUNT_NAME)
            val downloadedRemotePath = intent.getStringExtra(Extras.EXTRA_REMOTE_PATH)
            if (account.name == accountName &&
                downloadedRemotePath != null
            ) {
                val file = storageManager.getFileByPath(downloadedRemotePath)
                mPreviewImagePagerAdapter!!.onDownloadEvent(
                    file!!,
                    intent.action!!,
                    intent.getBooleanExtra(Extras.EXTRA_DOWNLOAD_RESULT, false)
                )
            }
        }
    }

    @SuppressLint("InlinedApi")
    fun toggleFullScreen() {
        val visible = (mFullScreenAnchorView!!.systemUiVisibility
                and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0
        if (visible) {
            hideSystemUI(mFullScreenAnchorView)
            // actionBar.hide(); // propagated through OnSystemUiVisibilityChangeListener()
        } else {
            showSystemUI(mFullScreenAnchorView)
            // actionBar.show(); // propagated through OnSystemUiVisibilityChangeListener()
        }
    }

    override fun onAccountSet(stateWasRecovered: Boolean) {
        super.onAccountSet(stateWasRecovered)
        if (account != null) {
            var file = file
            /// Validate handled file  (first image to preview)
            checkNotNull(file) { "Instanced with a NULL OCFile" }
            require(file.isImage) { "Non-image file passed as argument" }

            // Update file according to DB file, if it is possible
            if (file.id!! > FileDataStorageManager.ROOT_PARENT_ID) {
                file = storageManager.getFileById(file.id!!)
            }
            if (file != null) {
                /// Refresh the activity according to the Account and OCFile set
                setFile(file) // reset after getting it fresh from storageManager
                updateActionBarTitle(getFile().fileName)
                initViewPager()
            } else {
                // handled file not in the current Account
                finish()
            }
        }
    }

    override fun onBrowsedDownTo(folder: OCFile) {
        // TODO Auto-generated method stub
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUI(anchorView: View?) {
        anchorView!!.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hides NAVIGATION BAR; Android >= 4.0
                    or View.SYSTEM_UI_FLAG_FULLSCREEN // hides STATUS BAR;     Android >= 4.1
                    or View.SYSTEM_UI_FLAG_IMMERSIVE // stays interactive;    Android >= 4.4
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE // draw full window;     Android >= 4.1
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN // draw full window;     Android >= 4.1
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    }

    @SuppressLint("InlinedApi")
    private fun showSystemUI(anchorView: View?) {
        anchorView!!.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE // draw full window;     Android >= 4.1
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN // draw full window;     Android >= 4.1
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    }

    override fun navigateToOption(fileListOption: FileListOption) {
        backToDisplayActivity()
        super.navigateToOption(fileListOption)
    }

    private fun showActionBar(show: Boolean) {
        val actionBar = supportActionBar ?: return
        if (show) {
            actionBar.show()
        } else {
            actionBar.hide()
        }
    }

    private fun updateActionBarTitle(title: String) {
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.title = title
        }
    }

    companion object {
        private const val INITIAL_HIDE_DELAY = 0 // immediate hide
    }
}