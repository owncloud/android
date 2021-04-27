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
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.owncloud.android.MainApp.Companion.appContext
import com.owncloud.android.R
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager.AsyncThumbnailDrawable
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ThumbnailGenerationTask
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.extensions.observeWorkerTillItFinishes
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.files.FileMenuFilter
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder
import com.owncloud.android.presentation.viewmodels.files.FileDetailsViewModel
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.controller.TransferProgressController
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment
import com.owncloud.android.ui.dialog.RenameFileDialogFragment
import com.owncloud.android.ui.preview.PreviewAudioFragment
import com.owncloud.android.ui.preview.PreviewImageFragment
import com.owncloud.android.ui.preview.PreviewTextFragment
import com.owncloud.android.ui.preview.PreviewVideoFragment
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimetypeIconUtil
import com.owncloud.android.utils.PreferenceUtils
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * This Fragment is used to display the details about a file.
 */
class FileDetailFragment : FileFragment(), View.OnClickListener {
    private var account: Account? = null
    private var progressController: TransferProgressController? = null

    private val fileDetailsViewModel: FileDetailsViewModel by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        file = requireArguments().getParcelable(ARG_FILE)
        account = requireArguments().getParcelable(ARG_ACCOUNT)

        if (savedInstanceState != null) {
            file = savedInstanceState.getParcelable(FileActivity.EXTRA_FILE)
            account = savedInstanceState.getParcelable(FileActivity.EXTRA_ACCOUNT)
        }

        return inflater.inflate(R.layout.file_details_fragment, container, false).apply {
            filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressController = TransferProgressController(activity as ComponentsGetter?).also {
            it.setProgressBar(view.findViewById(R.id.fdProgressBar))
        }

        updateFileDetails(forcedTransferring = false, refresh = false)

        view.findViewById<View>(R.id.fdCancelBtn).setOnClickListener {
            (mContainerActivity as FileDisplayActivity).cancelTransference(file)
            fileDetailsViewModel.cancelCurrentDownload(file)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(FileActivity.EXTRA_FILE, file)
        outState.putParcelable(FileActivity.EXTRA_ACCOUNT, account)
    }

    override fun onStart() {
        super.onStart()
        progressController?.startListeningProgressFor(file, account)
        startObservingProgressForDownload()
    }

    override fun onStop() {
        progressController?.stopListeningProgressFor(file, account)
        super.onStop()
    }

    private fun startObservingProgressForDownload() {
        val safeAccount = account ?: return

        fileDetailsViewModel.startListeningToDownloadsFromAccountAndFile(file = file, account = safeAccount)
        fileDetailsViewModel.pendingDownloads.observe(viewLifecycleOwner) { }
        fileDetailsViewModel.ongoingDownload.observeWorkerTillItFinishes(
            owner = viewLifecycleOwner,
            onWorkEnqueued = { updateLayoutForEnqueuedDownload() },
            onWorkRunning = { progress -> updateLayoutForRunningDownload(progress) },
            onWorkSucceeded = { updateLayoutForSucceededDownload() },
            onWorkFailed = { showMessageInSnackbar(getString(R.string.downloader_download_failed_ticker)) },
            onWorkCancelled = {
                setButtonsForRemote()
                showMessageInSnackbar(getString(R.string.downloader_download_canceled_ticker))
            },
            removeObserverAfterNull = false
        )
    }

    private fun updateLayoutForEnqueuedDownload() {
        requireView().findViewById<View>(R.id.fdProgressBlock).isVisible = true

        requireView().findViewById<TextView>(R.id.fdProgressText).apply {
            isVisible = true
            text = String.format(getString(R.string.downloader_download_enqueued_ticker), file.fileName)
        }
    }

    private fun updateLayoutForRunningDownload(progress: Int) {
        requireView().findViewById<View>(R.id.fdProgressBlock).isVisible = true

        setButtonsForTransferring()

        requireView().findViewById<ProgressBar>(R.id.fdProgressBar).apply {
            isIndeterminate = false
            setProgress(progress)
            invalidate()
        }
    }

    private fun updateLayoutForSucceededDownload() {
        updateFileDetails(forcedTransferring = false, refresh = false)

        val fileDisplayActivity = activity as FileDisplayActivity
        fileDetailsViewModel.navigateToPreviewOrOpenFile(fileDisplayActivity, file)
    }

    override fun onTransferServiceConnected() {
        progressController?.startListeningProgressFor(file, account)
        updateFileDetails(forcedTransferring = false, refresh = false) // TODO - really?
    }

    override fun onFileMetadataChanged(updatedFile: OCFile?) {
        if (updatedFile != null) {
            file = updatedFile
        }
        updateFileDetails(forcedTransferring = false, refresh = false)
    }

    override fun onFileMetadataChanged() {
        updateFileDetails(forcedTransferring = false, refresh = true)
    }

    override fun onFileContentChanged() {
        setFileType(file) // to update thumbnail
    }

    override fun updateViewForSyncInProgress() {
        updateFileDetails(forcedTransferring = true, refresh = false)
    }

    override fun updateViewForSyncOff() {
        updateFileDetails(forcedTransferring = false, refresh = false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.file_actions_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (mContainerActivity.storageManager != null) {
            val fileMenuFilter = FileMenuFilter(
                file,
                mContainerActivity.storageManager.account,
                mContainerActivity,
                activity
            )
            fileMenuFilter.filter(
                menu,
                false,
                false,
                false,
                false
            )
        }

        // additional restriction for this fragment 
        menu.findItem(R.id.action_see_details)?.apply {
            isVisible = false
            isEnabled = false
        }

        // additional restriction for this fragment
        menu.findItem(R.id.action_move)?.apply {
            isVisible = false
            isEnabled = false
        }

        // additional restriction for this fragment
        menu.findItem(R.id.action_copy)?.apply {
            isVisible = false
            isEnabled = false
        }
        menu.findItem(R.id.action_search)?.apply {
            isVisible = false
            isEnabled = false
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
                dialog.show(parentFragmentManager, FTAG_CONFIRMATION)
                true
            }
            R.id.action_rename_file -> {
                val dialog = RenameFileDialogFragment.newInstance(file)
                dialog.show(parentFragmentManager, FTAG_RENAME_FILE)
                true
            }
            R.id.action_cancel_sync -> {
                (mContainerActivity as FileDisplayActivity).cancelTransference(file)
                true
            }
            R.id.action_download_file, R.id.action_sync_file -> {
                mContainerActivity.fileOperationsHelper.syncFile(file)
                startObservingProgressForDownload()
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
            R.id.fdIcon -> {
                displayFile(file);
            }
            else ->
                Timber.e("Incorrect view clicked!");
        }
    }

    private fun displayFile(ocFile: OCFile) {
        if (PreviewImageFragment.canBePreviewed(file)) {
            // preview image - it handles the sync, if needed
            (activity as FileDisplayActivity).startImagePreview(file);
        } else if (PreviewTextFragment.canBePreviewed(file)) {
            (activity as FileDisplayActivity).startTextPreview(file);
            mContainerActivity.fileOperationsHelper.syncFile(file);

        } else if (PreviewAudioFragment.canBePreviewed(file)) {
            // media preview
            (activity as FileDisplayActivity).startAudioPreview(file, 0);
            mContainerActivity.getFileOperationsHelper().syncFile(file);

        } else if (PreviewVideoFragment.canBePreviewed(file) && file.fileIsDownloading != true) {
            // Available offline exception, don't initialize streaming
            if (!file.isAvailableLocally) {//&& file.isAvailableOffline()) {
                // sync file content, then open with external apps
                (activity as FileDisplayActivity).startSyncThenOpen(file);
            } else {
                // media preview
                (activity as FileDisplayActivity).startVideoPreview(file, 0);
            }

            // If the file is already downloaded sync it, just to update it if there is a
            // new available file version
            if (file.isAvailableLocally) {
                mContainerActivity.fileOperationsHelper.syncFile(file);
            }
        } else {
            // sync file content, then open with external apps
            (activity as FileDisplayActivity).startSyncThenOpen(file);
        }
    }

    /**
     * Updates the view with all relevant details about that file.
     *
     * @param forcedTransferring Flag signaling if the file should be considered as downloading or uploading,
     * although [FileDetailsViewModel.isDownloadPending] and [FileUploaderBinder.isUploading] return false.
     * @param refresh            If 'true', try to refresh the whole file from the database
     */
    private fun updateFileDetails(forcedTransferring: Boolean, refresh: Boolean) {
        val storageManager = mContainerActivity.storageManager
        if (refresh && storageManager != null) {
            file = storageManager.getFileByPath(file.remotePath)
        }
        val file = file

        // Update layout with file details
        with(file) {
            setFilename(fileName)
            setFileType(this)
            setFileSize(length)
            setTimeModified(modificationTimestamp)
        }

        // configure UI for depending upon local state of the file
        val uploaderBinder = mContainerActivity.fileUploaderBinder
        val safeAccount = account
        val transferring = forcedTransferring ||
                safeAccount != null && fileDetailsViewModel.isDownloadPending(safeAccount, file) ||
                uploaderBinder != null && uploaderBinder.isUploading(account, file)

        when {
            transferring -> {
                setButtonsForTransferring()
            }
            file.isAvailableLocally -> {
                setButtonsForLocallyAvailable()
            }
            else -> {
                setButtonsForRemote()
            }
        }
        requireView().invalidate()
    }

    /**
     * Updates the filename in view
     *
     * @param filename to set
     */
    private fun setFilename(filename: String) {
        requireView().findViewById<TextView>(R.id.fdFilename)?.text = filename
    }

    /**
     * Updates the MIME type in view
     *
     * @param file : An [OCFile]
     */
    private fun setFileType(file: OCFile) {
        val mimetype = file.mimeType

        requireView().findViewById<TextView>(R.id.fdType)?.apply {
            text = DisplayUtils.convertMIMEtoPrettyPrint(mimetype)
        }

        val imageView = requireView().findViewById<ImageView>(R.id.fdIcon)
        if (imageView != null) {
            var thumbnail: Bitmap?
            imageView.tag = file.id
            if (file.isImage) {
                val tagId = file.remoteId.toString()
                thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(tagId)
                if (thumbnail != null && !file.needsToUpdateThumbnail) {
                    imageView.setImageBitmap(thumbnail)
                } else {
                    // generate new Thumbnail

                    if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, imageView)) {
                        val task = ThumbnailGenerationTask(imageView, account)
                        if (thumbnail == null) {
                            thumbnail = ThumbnailsCacheManager.mDefaultImg
                        }
                        val asyncDrawable = AsyncThumbnailDrawable(appContext.resources, thumbnail, task)
                        imageView.setImageDrawable(asyncDrawable)
                        task.execute(file)
                    }
                }
            } else {
                // Name of the file, to deduce the icon to use in case the MIME type is not precise enough
                val filename = file.fileName
                imageView.setImageResource(MimetypeIconUtil.getFileTypeIconId(mimetype, filename))
            }
        }
    }

    /**
     * Updates the file size in view
     *
     * @param fileSize in bytes to set
     */
    private fun setFileSize(fileSize: Long) {
        requireView().findViewById<TextView>(R.id.fdSize)?.text = DisplayUtils.bytesToHumanReadable(fileSize, activity)
    }

    /**
     * Updates the time that the file was last modified
     *
     * @param milliseconds Unix time to set
     */
    private fun setTimeModified(milliseconds: Long) {
        requireView().findViewById<TextView>(R.id.fdModified)?.text = DisplayUtils.unixTimeToHumanReadable(milliseconds)
    }

    /**
     * Enables or disables buttons for a file being downloaded
     */
    private fun setButtonsForTransferring() {
        // show the progress bar for the transfer
        requireView().findViewById<View>(R.id.fdProgressBlock).isVisible = true
        val progressText = requireView().findViewById<TextView>(R.id.fdProgressText)
        progressText.isVisible = true
        val uploaderBinder = mContainerActivity.fileUploaderBinder
        val safeAccount = account
        if (safeAccount != null && uploaderBinder != null && uploaderBinder.isUploading(safeAccount, file)) {
            progressText.setText(R.string.uploader_upload_in_progress_ticker)
        }
    }

    /**
     * Enables or disables buttons for a file locally available
     */
    private fun setButtonsForLocallyAvailable() {
        // hides the progress bar
        requireView().findViewById<View>(R.id.fdProgressBlock)?.isVisible = false
        requireView().findViewById<TextView>(R.id.fdProgressText)?.isVisible = false
    }

    /**
     * Enables or disables buttons for a file not locally available
     */
    private fun setButtonsForRemote() {
        // hides the progress bar
        requireView().findViewById<View>(R.id.fdProgressBlock)?.isVisible = false
        requireView().findViewById<TextView>(R.id.fdProgressText)?.isVisible = false
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
        fun newInstance(fileToDetail: OCFile, account: Account): FileDetailFragment {
            val args = Bundle().apply {
                putParcelable(ARG_FILE, fileToDetail)
                putParcelable(ARG_ACCOUNT, account)
            }

            return FileDetailFragment().apply {
                arguments = args
            }
        }
    }
}
