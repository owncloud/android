/*
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author David González Verdugo
 * @author Abel García de Prada
 * Copyright (C) 2011  Bartek Przybylski
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
package com.owncloud.android.ui.fragment

import android.accounts.Account
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import com.owncloud.android.MainApp.Companion.appContext
import com.owncloud.android.R
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager.AsyncThumbnailDrawable
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ThumbnailGenerationTask
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.files.FileMenuFilter
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder
import com.owncloud.android.presentation.manager.TransferManager
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.controller.TransferProgressController
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment
import com.owncloud.android.ui.dialog.RenameFileDialogFragment
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimetypeIconUtil
import com.owncloud.android.utils.PreferenceUtils
import timber.log.Timber

/**
 * This Fragment is used to display the details about a file.
 */
class FileDetailFragment : FileFragment(), View.OnClickListener {
    private var mLayout: Int
    private var mView: View? = null
    private var mAccount: Account? = null
    private var mProgressController: TransferProgressController? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        mProgressController = TransferProgressController(activity as ComponentsGetter?)
        val progressBar = mView!!.findViewById<ProgressBar>(R.id.fdProgressBar)
        mProgressController!!.setProgressBar(progressBar)

        // Allow or disallow touches with other visible windows
        if (mLayout == R.layout.file_details_fragment) {
            val fileDetailsLayout = requireActivity().findViewById<RelativeLayout>(R.id.fileDetailsLayout)
            fileDetailsLayout.filterTouchesWhenObscured =
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
        } else {
            val fileDetailsEmptyLayout = requireActivity().findViewById<LinearLayout>(R.id.fileDetailsEmptyLayout)
            fileDetailsEmptyLayout.filterTouchesWhenObscured =
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        file = requireArguments().getParcelable(ARG_FILE)
        mAccount = requireArguments().getParcelable(ARG_ACCOUNT)
        if (savedInstanceState != null) {
            file = savedInstanceState.getParcelable(FileActivity.EXTRA_FILE)
            mAccount = savedInstanceState.getParcelable(FileActivity.EXTRA_ACCOUNT)
        }
        if (file != null && mAccount != null) {
            mLayout = R.layout.file_details_fragment
        }
        mView = inflater.inflate(mLayout, null)
        if (mLayout == R.layout.file_details_fragment) {
            requireView().findViewById<View>(R.id.fdCancelBtn).setOnClickListener(this)
        }
        updateFileDetails(false, false)
        return mView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(FileActivity.EXTRA_FILE, file)
        outState.putParcelable(FileActivity.EXTRA_ACCOUNT, mAccount)
    }

    override fun onStart() {
        super.onStart()
        mProgressController!!.startListeningProgressFor(file, mAccount)
    }

    override fun onStop() {
        mProgressController!!.stopListeningProgressFor(file, mAccount)
        super.onStop()
    }

    override fun onTransferServiceConnected() {
        if (mProgressController != null) {
            mProgressController!!.startListeningProgressFor(file, mAccount)
        }
        updateFileDetails(false, false) // TODO - really?
    }

    override fun onFileMetadataChanged(updatedFile: OCFile) {
        if (updatedFile != null) {
            file = updatedFile
        }
        updateFileDetails(false, false)
    }

    override fun onFileMetadataChanged() {
        updateFileDetails(false, true)
    }

    override fun onFileContentChanged() {
        setFiletype(file) // to update thumbnail
    }

    override fun updateViewForSyncInProgress() {
        updateFileDetails(true, false)
    }

    override fun updateViewForSyncOff() {
        updateFileDetails(false, false)
    }

    override fun getView(): View? {
        return if (super.getView() == null) mView else super.getView()
    }

    /**
     * {@inheritDoc}
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.file_actions_menu, menu)
    }

    /**
     * {@inheritDoc}
     */
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (mContainerActivity.storageManager != null) {
            val mf = FileMenuFilter(
                file,
                mContainerActivity.storageManager.account,
                mContainerActivity,
                activity
            )
            mf.filter(menu, false, false, false, false)
        }

        // additional restriction for this fragment 
        var item = menu.findItem(R.id.action_see_details)
        if (item != null) {
            item.isVisible = false
            item.isEnabled = false
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_move)
        if (item != null) {
            item.isVisible = false
            item.isEnabled = false
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_copy)
        if (item != null) {
            item.isVisible = false
            item.isEnabled = false
        }
        item = menu.findItem(R.id.action_search)
        if (item != null) {
            item.isVisible = false
            item.isEnabled = false
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share_file -> {
                mContainerActivity.fileOperationsHelper.showShareFile(file)
                true
            }
            R.id.action_open_file_with -> {
                mContainerActivity.fileOperationsHelper.openFile(file)
                true
            }
            R.id.action_remove_file -> {
                val dialog = RemoveFilesDialogFragment.newInstance(file)
                dialog.show(requireFragmentManager(), FTAG_CONFIRMATION)
                true
            }
            R.id.action_rename_file -> {
                val dialog = RenameFileDialogFragment.newInstance(file)
                dialog.show(requireFragmentManager(), FTAG_RENAME_FILE)
                true
            }
            R.id.action_cancel_sync -> {
                (mContainerActivity as FileDisplayActivity).cancelTransference(file)
                true
            }
            R.id.action_download_file, R.id.action_sync_file -> {
                mContainerActivity.fileOperationsHelper.syncFile(file)
                true
            }
            R.id.action_send_file -> {

                // Obtain the file
                if (!file.isAvailableLocally) {  // Download the file
                    Timber.d("%s : File must be downloaded", file.remotePath)
                    (mContainerActivity as FileDisplayActivity).startDownloadForSending(file)
                } else {
                    mContainerActivity.fileOperationsHelper.sendDownloadedFile(file)
                }
                true
            }
            R.id.action_set_available_offline -> {
                mContainerActivity.fileOperationsHelper.toggleAvailableOffline(file, true)
                true
            }
            R.id.action_unset_available_offline -> {
                mContainerActivity.fileOperationsHelper.toggleAvailableOffline(file, false)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.fdCancelBtn -> {
                (mContainerActivity as FileDisplayActivity).cancelTransference(file)
            }
            else -> Timber.e("Incorrect view clicked!")
        }
    }

    /**
     * Check if the fragment was created with an empty layout. An empty fragment can't show file details, must be
     * replaced.
     *
     * @return True when the fragment was created with the empty layout.
     */
    val isEmpty: Boolean
        get() = mLayout == R.layout.file_details_empty || file == null || mAccount == null

    /**
     * Updates the view with all relevant details about that file.
     *
     * @param forcedTransferring Flag signaling if the file should be considered as downloading or uploading,
     * although [FileDownloaderBinder.isDownloading]  and
     * [FileUploaderBinder.isUploading] return false.
     * @param refresh            If 'true', try to refresh the whole file from the database
     */
    private fun updateFileDetails(forcedTransferring: Boolean, refresh: Boolean) {
        if (readyToShow()) {
            val storageManager = mContainerActivity.storageManager
            if (refresh && storageManager != null) {
                file = storageManager.getFileByPath(file.remotePath)
            }
            val file = file

            // set file details
            setFilename(file.fileName)
            setFiletype(file)
            setFilesize(file.length)
            setTimeModified(file.modificationTimestamp)

            // configure UI for depending upon local state of the file
            val transferManager = TransferManager(appContext)
            val uploaderBinder = mContainerActivity.fileUploaderBinder
            val safeAccount = mAccount
            if (forcedTransferring ||
                safeAccount != null && transferManager.isDownloadPending(safeAccount, file) ||
                uploaderBinder != null && uploaderBinder.isUploading(mAccount, file)
            ) {
                setButtonsForTransferring()
            } else if (file.isAvailableLocally) {
                setButtonsForDown()
            } else {
                // TODO load default preview image; when the local file is removed, the preview
                // remains there
                setButtonsForRemote()
            }
        }
        requireView().invalidate()
    }

    /**
     * Checks if the fragment is ready to show details of a OCFile
     *
     * @return 'True' when the fragment is ready to show details of a file
     */
    private fun readyToShow(): Boolean {
        return file != null && mAccount != null && mLayout == R.layout.file_details_fragment
    }

    /**
     * Updates the filename in view
     *
     * @param filename to set
     */
    private fun setFilename(filename: String) {
        val tv = requireView().findViewById<TextView>(R.id.fdFilename)
        tv?.text = filename
    }

    /**
     * Updates the MIME type in view
     *
     * @param file : An [OCFile]
     */
    private fun setFiletype(file: OCFile) {
        val mimetype = file.mimeType
        val tv = requireView().findViewById<TextView>(R.id.fdType)
        if (tv != null) {
            // mimetype      MIME type to set
            val printableMimetype = DisplayUtils.convertMIMEtoPrettyPrint(mimetype)
            tv.text = printableMimetype
        }
        val iv = requireView().findViewById<ImageView>(R.id.fdIcon)
        if (iv != null) {
            var thumbnail: Bitmap?
            iv.tag = file.id
            if (file.isImage) {
                val tagId = file.remoteId.toString()
                thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(tagId)
                if (thumbnail != null && !file.needsToUpdateThumbnail) {
                    iv.setImageBitmap(thumbnail)
                } else {
                    // generate new Thumbnail
                    if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, iv)) {
                        val task = ThumbnailGenerationTask(
                            iv, mContainerActivity.storageManager, mAccount
                        )
                        if (thumbnail == null) {
                            thumbnail = ThumbnailsCacheManager.mDefaultImg
                        }
                        val asyncDrawable = AsyncThumbnailDrawable(
                            appContext.resources,
                            thumbnail,
                            task
                        )
                        iv.setImageDrawable(asyncDrawable)
                        task.execute(file)
                    }
                }
            } else {
                // Name of the file, to deduce the icon to use in case the MIME type is not precise enough
                val filename = file.fileName
                iv.setImageResource(MimetypeIconUtil.getFileTypeIconId(mimetype, filename))
            }
        }
    }

    /**
     * Updates the file size in view
     *
     * @param filesize in bytes to set
     */
    private fun setFilesize(filesize: Long) {
        val tv = requireView().findViewById<TextView>(R.id.fdSize)
        tv?.text = DisplayUtils.bytesToHumanReadable(filesize, activity)
    }

    /**
     * Updates the time that the file was last modified
     *
     * @param milliseconds Unix time to set
     */
    private fun setTimeModified(milliseconds: Long) {
        val tv = requireView().findViewById<TextView>(R.id.fdModified)
        tv?.text = DisplayUtils.unixTimeToHumanReadable(milliseconds)
    }

    /**
     * Enables or disables buttons for a file being downloaded
     */
    private fun setButtonsForTransferring() {
        if (!isEmpty) {
            // show the progress bar for the transfer
            requireView().findViewById<View>(R.id.fdProgressBlock).visibility = View.VISIBLE
            val progressText = requireView().findViewById<TextView>(R.id.fdProgressText)
            progressText.visibility = View.VISIBLE
            val transferManager = TransferManager(appContext)
            val uploaderBinder = mContainerActivity.fileUploaderBinder
            val safeAccount = mAccount
            if (safeAccount != null && transferManager.isDownloadPending(safeAccount, file)) {
                progressText.setText(R.string.downloader_download_in_progress_ticker)
            } else if (uploaderBinder != null && uploaderBinder.isUploading(mAccount, file)) {
                progressText.setText(R.string.uploader_upload_in_progress_ticker)
            }
        }
    }

    /**
     * Enables or disables buttons for a file locally available
     */
    private fun setButtonsForDown() {
        if (!isEmpty) {
            // hides the progress bar
            requireView().findViewById<View>(R.id.fdProgressBlock).visibility = View.GONE
            val progressText = requireView().findViewById<TextView>(R.id.fdProgressText)
            progressText.visibility = View.GONE
        }
    }

    /**
     * Enables or disables buttons for a file not locally available
     */
    private fun setButtonsForRemote() {
        if (!isEmpty) {
            // hides the progress bar
            requireView().findViewById<View>(R.id.fdProgressBlock).visibility = View.GONE
            val progressText = requireView().findViewById<TextView>(R.id.fdProgressText)
            progressText.visibility = View.GONE
        }
    }

    companion object {
        const val FTAG_CONFIRMATION = "REMOVE_CONFIRMATION_FRAGMENT"
        const val FTAG_RENAME_FILE = "RENAME_FILE_FRAGMENT"
        private const val ARG_FILE = "FILE"
        private const val ARG_ACCOUNT = "ACCOUNT"

        /**
         * Public factory method to create new FileDetailFragment instances.
         *
         *
         * When 'fileToDetail' or 'ocAccount' are null, creates a dummy layout (to use when a file wasn't tapped before).
         *
         * @param fileToDetail An [OCFile] to show in the fragment
         * @param account      An ownCloud account; needed to start downloads
         * @return New fragment with arguments set
         */
        fun newInstance(fileToDetail: OCFile?, account: Account?): FileDetailFragment {
            val frag = FileDetailFragment()
            val args = Bundle()
            args.putParcelable(ARG_FILE, fileToDetail)
            args.putParcelable(ARG_ACCOUNT, account)
            frag.arguments = args
            return frag
        }
    }

    /**
     * Creates an empty details fragment.
     *
     *
     * It's necessary to keep a public constructor without parameters; the system uses it when tries
     * to reinstantiate a fragment automatically.
     */
    init {
        mLayout = R.layout.file_details_empty
    }
}