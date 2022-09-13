/**
 * ownCloud Android client application
 *
 * Copyright (C) 2022 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.presentation.ui.files.details

import android.accounts.Account
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.work.WorkInfo
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.FileDetailsFragmentBinding
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.extensions.isDownload
import com.owncloud.android.extensions.openFile
import com.owncloud.android.extensions.openOCFile
import com.owncloud.android.extensions.sendDownloadedFilesByShareSheet
import com.owncloud.android.extensions.showErrorInSnackbar
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.files.FileMenuFilter
import com.owncloud.android.presentation.UIResult
import com.owncloud.android.presentation.ui.files.operations.FileOperation.SetFilesAsAvailableOffline
import com.owncloud.android.presentation.ui.files.operations.FileOperation.SynchronizeFileOperation
import com.owncloud.android.presentation.ui.files.operations.FileOperation.UnsetFilesAsAvailableOffline
import com.owncloud.android.presentation.ui.files.operations.FileOperationsViewModel
import com.owncloud.android.presentation.ui.files.removefile.RemoveFilesDialogFragment
import com.owncloud.android.presentation.ui.files.removefile.RemoveFilesDialogFragment.Companion.FRAGMENT_TAG_CONFIRMATION
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.dialog.RenameFileDialogFragment
import com.owncloud.android.ui.dialog.RenameFileDialogFragment.Companion.FRAGMENT_TAG_RENAME_FILE
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.usecases.synchronization.SynchronizeFileUseCase
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimetypeIconUtil
import com.owncloud.android.utils.PreferenceUtils
import com.owncloud.android.workers.DownloadFileWorker
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class FileDetailsFragment : FileFragment() {

    private val fileDetailsViewModel by viewModel<FileDetailsViewModel>() {
        parametersOf(
            requireArguments().getParcelable(ARG_ACCOUNT),
            requireArguments().getParcelable(ARG_FILE),
            requireArguments().getBoolean(ARG_SYNC_FILE_AT_OPEN),
        )
    }
    private val fileOperationsViewModel by viewModel<FileOperationsViewModel>()

    private var _binding: FileDetailsFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)

        _binding = FileDetailsFragmentBinding.inflate(inflater, container, false)
        return binding.root.apply {
            filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collectLatestLifecycleFlow(fileDetailsViewModel.currentFile) { ocFile: OCFile ->
            file = ocFile
            updateDetails(ocFile)
        }

        fileOperationsViewModel.syncFileLiveData.observe(viewLifecycleOwner, Event.EventObserver { uiResult ->
            when (uiResult) {
                is UIResult.Error -> showErrorInSnackbar(R.string.sync_fail_ticker, uiResult.error)
                is UIResult.Loading -> {}
                is UIResult.Success -> when (uiResult.data) {
                    SynchronizeFileUseCase.SyncType.AlreadySynchronized -> showMessageInSnackbar(getString(R.string.sync_file_nothing_to_do_msg))
                    is SynchronizeFileUseCase.SyncType.ConflictDetected -> showMessageInSnackbar("CONFLICT")
                    is SynchronizeFileUseCase.SyncType.DownloadEnqueued -> { fileDetailsViewModel.startListeningToWorkInfo(uiResult.data.workerId) }
                    SynchronizeFileUseCase.SyncType.FileNotFound -> showMessageInSnackbar("FILE NOT FOUND")
                    is SynchronizeFileUseCase.SyncType.UploadEnqueued -> fileDetailsViewModel.startListeningToWorkInfo(uiResult.data.workerId)
                    null -> showMessageInSnackbar("NULL")
                }
            }
        })

        collectLatestLifecycleFlow(fileDetailsViewModel.shouldSyncFile) { shouldSyncFile ->
            if (shouldSyncFile) {
                fileOperationsViewModel.performOperation(
                    SynchronizeFileOperation(
                        fileToSync = fileDetailsViewModel.getCurrentFile(),
                        accountName = fileDetailsViewModel.getAccount().name
                    )
                )
                fileDetailsViewModel.shouldSyncFile(false)
            }
        }
        startListeningToOngoingTransfers()
        fileDetailsViewModel.checkOnGoingTransfersWhenOpening()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.file_actions_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val fileMenuFilter = FileMenuFilter(
            fileDetailsViewModel.getCurrentFile(),
            fileDetailsViewModel.getAccount(),
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

        menu.findItem(R.id.action_see_details)?.apply {
            isVisible = false
            isEnabled = false
        }

        menu.findItem(R.id.action_move)?.apply {
            isVisible = false
            isEnabled = false
        }

        menu.findItem(R.id.action_copy)?.apply {
            isVisible = false
            isEnabled = false
        }

        menu.findItem(R.id.action_search)?.apply {
            isVisible = false
            isEnabled = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share_file -> {
                mContainerActivity.fileOperationsHelper.showShareFile(fileDetailsViewModel.getCurrentFile())
                true
            }
            R.id.action_open_file_with -> {
                val currentFile = fileDetailsViewModel.getCurrentFile()
                if (!currentFile.isAvailableLocally) {  // Download the file
                    Timber.d("%s : File must be downloaded before opening it", currentFile.remotePath)
                    (mContainerActivity as FileDisplayActivity).startDownloadForOpening(currentFile)
                } else { // Already downloaded -> Open it
                    requireActivity().openOCFile(currentFile)
                }
                true
            }
            R.id.action_remove_file -> {
                val dialog = RemoveFilesDialogFragment.newInstance(fileDetailsViewModel.getCurrentFile())
                dialog.show(parentFragmentManager, FRAGMENT_TAG_CONFIRMATION)
                true
            }
            R.id.action_rename_file -> {
                val dialog = RenameFileDialogFragment.newInstance(fileDetailsViewModel.getCurrentFile())
                dialog.show(parentFragmentManager, FRAGMENT_TAG_RENAME_FILE)
                true
            }
            R.id.action_cancel_sync -> {
                fileDetailsViewModel.cancelCurrentTransfer()
                true
            }
            R.id.action_download_file, R.id.action_sync_file -> {
                fileDetailsViewModel.shouldSyncFile(true)
                true
            }
            R.id.action_send_file -> {
                val currentFile = fileDetailsViewModel.getCurrentFile()
                if (!currentFile.isAvailableLocally) {  // Download the file
                    Timber.d("%s : File must be downloaded before sending it", currentFile.remotePath)
                    (mContainerActivity as FileDisplayActivity).startDownloadForSending(currentFile)
                } else { // Already downloaded -> Send it
                    requireActivity().sendDownloadedFilesByShareSheet(listOf(currentFile))
                }
                true
            }
            R.id.action_set_available_offline -> {
                fileOperationsViewModel.performOperation(SetFilesAsAvailableOffline(listOf(fileDetailsViewModel.getCurrentFile())))
                true
            }
            R.id.action_unset_available_offline -> {
                fileOperationsViewModel.performOperation(UnsetFilesAsAvailableOffline(listOf(fileDetailsViewModel.getCurrentFile())))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateDetails(ocFile: OCFile) {
        binding.fdFilename.text = ocFile.fileName
        binding.fdSize.text = DisplayUtils.bytesToHumanReadable(ocFile.length, requireContext())
        binding.fdModified.text = DisplayUtils.unixTimeToHumanReadable(ocFile.modificationTimestamp)
        setMimeType(ocFile)
        requireActivity().invalidateOptionsMenu()
    }

    private fun setMimeType(ocFile: OCFile) {
        binding.fdType.text = DisplayUtils.convertMIMEtoPrettyPrint(ocFile.mimeType)

        binding.fdIcon.let { imageView ->
            imageView.tag = ocFile.id
            if (ocFile.isImage) {
                val tagId = ocFile.remoteId.toString()
                var thumbnail: Bitmap? = ThumbnailsCacheManager.getBitmapFromDiskCache(tagId)
                if (thumbnail != null && !ocFile.needsToUpdateThumbnail) {
                    imageView.setImageBitmap(thumbnail)
                } else {
                    // generate new Thumbnail
                    if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(ocFile, imageView)) {
                        val task = ThumbnailsCacheManager.ThumbnailGenerationTask(imageView, fileDetailsViewModel.getAccount())
                        if (thumbnail == null) {
                            thumbnail = ThumbnailsCacheManager.mDefaultImg
                        }
                        val asyncDrawable = ThumbnailsCacheManager.AsyncThumbnailDrawable(MainApp.appContext.resources, thumbnail, task)
                        imageView.setImageDrawable(asyncDrawable)
                        task.execute(ocFile)
                    }
                }
            } else {
                // Name of the file, to deduce the icon to use in case the MIME type is not precise enough
                imageView.setImageResource(MimetypeIconUtil.getFileTypeIconId(ocFile.mimeType, ocFile.fileName))
            }
        }
    }

    private fun startListeningToOngoingTransfers() {
        fileDetailsViewModel.ongoingTransfer.observe(viewLifecycleOwner, Event.EventObserver { workInfo ->
            workInfo ?: return@EventObserver

            when (workInfo.state) {
                WorkInfo.State.ENQUEUED -> updateLayoutForEnqueuedTransfer(workInfo)
                WorkInfo.State.RUNNING -> updateLayoutForRunningTransfer(workInfo)
                WorkInfo.State.SUCCEEDED -> updateLayoutForSucceededTransfer(workInfo)
                WorkInfo.State.FAILED -> updateLayoutForFailedTransfer(workInfo)
                WorkInfo.State.BLOCKED -> {}
                WorkInfo.State.CANCELLED -> updateLayoutForCancelledTransfer(workInfo)
            }
        })
    }

    private fun updateLayoutForEnqueuedTransfer(workInfo: WorkInfo) {
        showProgressView(isTransferGoingOn = true)
        binding.fdProgressText.text = if (workInfo.isDownload()) {
            getString(R.string.downloader_download_enqueued_ticker, fileDetailsViewModel.currentFile.value.fileName)
        } else { // Transfer is upload (?)
            getString(R.string.uploader_upload_enqueued_ticker, fileDetailsViewModel.currentFile.value.fileName)
        }
        binding.fdProgressBar.apply {
            progress = 0
            isIndeterminate = false
        }
    }

    private fun updateLayoutForRunningTransfer(workInfo: WorkInfo) {
        showProgressView(isTransferGoingOn = true)
        binding.fdProgressText.text = if (workInfo.isDownload()) {
            getString(R.string.downloader_download_in_progress_ticker, fileDetailsViewModel.currentFile.value.fileName)
        } else { // Transfer is upload (?)
            getString(R.string.uploader_upload_in_progress_ticker, fileDetailsViewModel.currentFile.value.fileName)
        }
        binding.fdProgressBar.apply {
            isIndeterminate = false
            progress = workInfo.progress.getInt(DownloadFileWorker.WORKER_KEY_PROGRESS, -1)
        }
        binding.fdCancelBtn.setOnClickListener { fileDetailsViewModel.cancelCurrentTransfer() }
    }

    private fun updateLayoutForSucceededTransfer(workInfo: WorkInfo) {
        showProgressView(isTransferGoingOn = false)

        if (workInfo.isDownload()) {
            val fileDisplayActivity = activity as FileDisplayActivity
            fileDetailsViewModel.navigateToPreviewOrOpenFile(fileDisplayActivity, file)
        } else { // Transfer is upload (?)
            // Nothing to do at the moment
        }
    }

    private fun updateLayoutForFailedTransfer(workInfo: WorkInfo) {
        showProgressView(isTransferGoingOn = false)

        val message = if (workInfo.isDownload()) {
            getString(R.string.downloader_download_failed_ticker)
        } else { // Transfer is upload (?)
            getString(R.string.uploader_upload_failed_ticker)
        }
        showMessageInSnackbar(message)
    }

    private fun updateLayoutForCancelledTransfer(workInfo: WorkInfo) {
        showProgressView(isTransferGoingOn = false)

        val message = if (workInfo.isDownload()) {
            getString(R.string.downloader_download_canceled_ticker)
        } else { // Transfer is upload (?)
            getString(R.string.uploader_upload_canceled_ticker)
        }
        showMessageInSnackbar(message)
    }

    /**
     * Show or hide progress for transfers.
     */
    private fun showProgressView(isTransferGoingOn: Boolean) {
        binding.fdProgressBar.isVisible = isTransferGoingOn
        binding.fdProgressText.isVisible = isTransferGoingOn
        binding.fdCancelBtn.isVisible = isTransferGoingOn

        // Invalidate to reset the menu items -> Show/Hide Download/Sync/Cancel
        requireActivity().invalidateOptionsMenu()
    }

    override fun updateViewForSyncInProgress() {
        TODO("Not yet implemented")
    }

    override fun updateViewForSyncOff() {
        TODO("Not yet implemented")
    }

    override fun onFileMetadataChanged(updatedFile: OCFile?) {
        // Nothing to do here. We are observing the oCFile from database, so it should be refreshed automatically
    }

    override fun onFileMetadataChanged() {
        TODO("Not yet implemented")
    }

    override fun onFileContentChanged() {
        TODO("Not yet implemented")
    }

    companion object {
        private const val ARG_FILE = "FILE"
        private const val ARG_ACCOUNT = "ACCOUNT"
        private const val ARG_SYNC_FILE_AT_OPEN = "ARG_SYNC_FILE_AT_OPEN"

        /**
         * Public factory method to create new FileDetailsFragment instances.
         *
         *
         * @param fileToDetail An [OCFile] to show in the fragment
         * @param account      An ownCloud account; needed to start downloads
         * @return New fragment with arguments set
         */
        fun newInstance(fileToDetail: OCFile, account: Account, syncFileAtOpen: Boolean = true): FileDetailsFragment =
            FileDetailsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_FILE, fileToDetail)
                    putParcelable(ARG_ACCOUNT, account)
                    putBoolean(ARG_SYNC_FILE_AT_OPEN, syncFileAtOpen)
                }
            }
    }
}
