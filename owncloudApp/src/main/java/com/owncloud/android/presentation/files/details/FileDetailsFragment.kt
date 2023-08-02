/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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

package com.owncloud.android.presentation.files.details

import android.accounts.Account
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.isVisible
import androidx.work.WorkInfo
import com.google.android.material.snackbar.Snackbar
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.FileDetailsFragmentBinding
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.domain.exceptions.InstanceNotConfiguredException
import com.owncloud.android.domain.exceptions.TooEarlyException
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFileWithSyncInfo
import com.owncloud.android.domain.utils.Event
import com.owncloud.android.extensions.addOpenInWebMenuOptions
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.extensions.filterMenuOptions
import com.owncloud.android.extensions.isDownload
import com.owncloud.android.extensions.openOCFile
import com.owncloud.android.extensions.sendDownloadedFilesByShareSheet
import com.owncloud.android.extensions.showErrorInSnackbar
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.presentation.common.UIResult
import com.owncloud.android.presentation.conflicts.ConflictsResolveActivity
import com.owncloud.android.presentation.files.details.FileDetailsViewModel.ActionsInDetailsView.NONE
import com.owncloud.android.presentation.files.details.FileDetailsViewModel.ActionsInDetailsView.SYNC
import com.owncloud.android.presentation.files.details.FileDetailsViewModel.ActionsInDetailsView.SYNC_AND_OPEN
import com.owncloud.android.presentation.files.details.FileDetailsViewModel.ActionsInDetailsView.SYNC_AND_OPEN_WITH
import com.owncloud.android.presentation.files.details.FileDetailsViewModel.ActionsInDetailsView.SYNC_AND_SEND
import com.owncloud.android.presentation.files.operations.FileOperation.SetFilesAsAvailableOffline
import com.owncloud.android.presentation.files.operations.FileOperation.SynchronizeFileOperation
import com.owncloud.android.presentation.files.operations.FileOperation.UnsetFilesAsAvailableOffline
import com.owncloud.android.presentation.files.operations.FileOperationsViewModel
import com.owncloud.android.presentation.files.removefile.RemoveFilesDialogFragment
import com.owncloud.android.presentation.files.removefile.RemoveFilesDialogFragment.Companion.FRAGMENT_TAG_CONFIRMATION
import com.owncloud.android.presentation.files.renamefile.RenameFileDialogFragment
import com.owncloud.android.presentation.files.renamefile.RenameFileDialogFragment.Companion.FRAGMENT_TAG_RENAME_FILE
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.preview.PreviewAudioFragment
import com.owncloud.android.ui.preview.PreviewImageFragment
import com.owncloud.android.ui.preview.PreviewTextFragment
import com.owncloud.android.ui.preview.PreviewVideoFragment
import com.owncloud.android.usecases.synchronization.SynchronizeFileUseCase
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimetypeIconUtil
import com.owncloud.android.utils.PreferenceUtils
import com.owncloud.android.workers.DownloadFileWorker
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class FileDetailsFragment : FileFragment() {

    private val fileDetailsViewModel by viewModel<FileDetailsViewModel> {
        parametersOf(
            requireArguments().getParcelable(ARG_ACCOUNT),
            requireArguments().getParcelable(ARG_FILE),
            requireArguments().getBoolean(ARG_SYNC_FILE_AT_OPEN),
        )
    }
    private val fileOperationsViewModel by viewModel<FileOperationsViewModel>()

    private var _binding: FileDetailsFragmentBinding? = null
    private val binding get() = _binding!!

    private var openInWebProviders: Map<String, Int> = hashMapOf()

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

        collectLatestLifecycleFlow(fileDetailsViewModel.currentFile) { ocFileWithSyncInfo: OCFileWithSyncInfo? ->
            if (ocFileWithSyncInfo != null) {
                file = ocFileWithSyncInfo.file
                updateDetails(ocFileWithSyncInfo)
            } else {
                requireActivity().onBackPressed()
            }
        }

        collectLatestLifecycleFlow(fileDetailsViewModel.appRegistryMimeType) { appRegistryMimeType ->
            if (appRegistryMimeType != null) {
                // Show or hide open in web options. Hidden by default.
                requireActivity().invalidateOptionsMenu()
            }
        }

        fileDetailsViewModel.openInWebUriLiveData.observe(viewLifecycleOwner, Event.EventObserver { uiResult: UIResult<String?> ->
            if (uiResult is UIResult.Success) {
                val builder = CustomTabsIntent.Builder().build()
                builder.launchUrl(
                    requireActivity(),
                    Uri.parse(uiResult.data)
                )
            } else if (uiResult is UIResult.Error) {
                // Mimetypes not supported via open in web, send 500
                if (uiResult.error is InstanceNotConfiguredException) {
                    val message =
                        getString(R.string.open_in_web_error_generic) + " " + getString(R.string.error_reason) + " " + getString(R.string.open_in_web_error_not_supported)
                    this.showMessageInSnackbar(message, Snackbar.LENGTH_LONG)
                } else if (uiResult.error is TooEarlyException) {
                    this.showMessageInSnackbar(getString(R.string.open_in_web_error_too_early), Snackbar.LENGTH_LONG)
                } else {
                    this.showErrorInSnackbar(
                        R.string.open_in_web_error_generic,
                        uiResult.error
                    )
                }
            }
        })

        fileOperationsViewModel.syncFileLiveData.observe(viewLifecycleOwner, Event.EventObserver { uiResult ->
            when (uiResult) {
                is UIResult.Error -> {
                    showErrorInSnackbar(R.string.sync_fail_ticker, uiResult.error)
                    fileDetailsViewModel.updateActionInDetailsView(NONE)
                    requireActivity().invalidateOptionsMenu()
                }

                is UIResult.Loading -> {}
                is UIResult.Success -> when (uiResult.data) {
                    SynchronizeFileUseCase.SyncType.AlreadySynchronized -> showMessageInSnackbar(getString(R.string.sync_file_nothing_to_do_msg))
                    is SynchronizeFileUseCase.SyncType.ConflictDetected -> {
                        val showConflictActivityIntent = Intent(requireActivity(), ConflictsResolveActivity::class.java)
                        showConflictActivityIntent.putExtra(ConflictsResolveActivity.EXTRA_FILE, file)
                        startActivity(showConflictActivityIntent)
                    }

                    is SynchronizeFileUseCase.SyncType.DownloadEnqueued -> {
                        fileDetailsViewModel.startListeningToWorkInfo(uiResult.data.workerId)
                    }

                    SynchronizeFileUseCase.SyncType.FileNotFound -> showMessageInSnackbar("FILE NOT FOUND")
                    is SynchronizeFileUseCase.SyncType.UploadEnqueued -> fileDetailsViewModel.startListeningToWorkInfo(uiResult.data.workerId)
                    null -> showMessageInSnackbar("NULL")
                }
            }
        })

        collectLatestLifecycleFlow(fileDetailsViewModel.actionsInDetailsView) { actions ->
            val safeFile = fileDetailsViewModel.getCurrentFile()
            if (actions.requiresSync() && safeFile != null)
                fileOperationsViewModel.performOperation(
                    SynchronizeFileOperation(
                        fileToSync = safeFile.file,
                        accountName = fileDetailsViewModel.getAccount().name
                    )
                )
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
        val safeFile = fileDetailsViewModel.getCurrentFile() ?: return
        fileDetailsViewModel.filterMenuOptions(safeFile.file)

        collectLatestLifecycleFlow(fileDetailsViewModel.menuOptions) { menuOptions ->
            val hasWritePermission = safeFile.file.hasWritePermission
            menu.filterMenuOptions(menuOptions, hasWritePermission)
        }

        menu.findItem(R.id.action_search)?.apply {
            isVisible = false
            isEnabled = false
        }

        val appRegistryProviders = fileDetailsViewModel.appRegistryMimeType.value?.appProviders
        openInWebProviders = addOpenInWebMenuOptions(menu, openInWebProviders, appRegistryProviders)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val safeFile = fileDetailsViewModel.getCurrentFile() ?: return false

        // Let's match the ones that are dynamic first.
        openInWebProviders.forEach { (openInWebProviderName, menuItemId) ->
            if (menuItemId == item.itemId) {
                fileDetailsViewModel.openInWeb(safeFile.file.remoteId!!, openInWebProviderName)
                return true
            }
        }

        return when (item.itemId) {
            R.id.action_share_file -> {
                mContainerActivity.fileOperationsHelper.showShareFile(safeFile.file)
                true
            }

            R.id.action_open_file_with -> {
                if (!safeFile.file.isAvailableLocally) {  // Download the file
                    Timber.d("%s : File must be downloaded before opening it", safeFile.file.remotePath)
                    fileDetailsViewModel.updateActionInDetailsView(SYNC_AND_OPEN_WITH)
                } else { // Already downloaded -> Open it
                    requireActivity().openOCFile(safeFile.file)
                }
                true
            }

            R.id.action_remove_file -> {
                val dialog = RemoveFilesDialogFragment.newInstance(safeFile.file)
                dialog.show(parentFragmentManager, FRAGMENT_TAG_CONFIRMATION)
                true
            }

            R.id.action_rename_file -> {
                val dialog = RenameFileDialogFragment.newInstance(safeFile.file)
                dialog.show(parentFragmentManager, FRAGMENT_TAG_RENAME_FILE)
                true
            }

            R.id.action_cancel_sync -> {
                fileDetailsViewModel.cancelCurrentTransfer()
                true
            }

            R.id.action_download_file, R.id.action_sync_file -> {
                fileDetailsViewModel.updateActionInDetailsView(SYNC)
                true
            }

            R.id.action_send_file -> {
                if (!safeFile.file.isAvailableLocally) {  // Download the file
                    Timber.d("%s : File must be downloaded before sending it", safeFile.file.remotePath)
                    fileDetailsViewModel.updateActionInDetailsView(SYNC_AND_SEND)
                } else { // Already downloaded -> Send it
                    requireActivity().sendDownloadedFilesByShareSheet(listOf(safeFile.file))
                }
                true
            }

            R.id.action_set_available_offline -> {
                fileOperationsViewModel.performOperation(SetFilesAsAvailableOffline(listOf(safeFile.file)))
                fileOperationsViewModel.performOperation(SynchronizeFileOperation(safeFile.file, safeFile.file.owner))
                true
            }

            R.id.action_unset_available_offline -> {
                fileOperationsViewModel.performOperation(UnsetFilesAsAvailableOffline(listOf(safeFile.file)))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateDetails(ocFileWithSyncInfo: OCFileWithSyncInfo) {
        binding.fdname.text = ocFileWithSyncInfo.file.fileName
        binding.fdSize.text = DisplayUtils.bytesToHumanReadable(ocFileWithSyncInfo.file.length, requireContext())
        binding.fdPath.text = ocFileWithSyncInfo.file.getParentRemotePath()
        setLastSync(ocFileWithSyncInfo.file)
        setModified(ocFileWithSyncInfo.file)
        setCreated(ocFileWithSyncInfo.file)
        setIconPinAccordingToFilesLocalState(binding.badgeDetailFile, ocFileWithSyncInfo)
        setMimeType(ocFileWithSyncInfo.file)
        setSpaceName(ocFileWithSyncInfo)
        requireActivity().invalidateOptionsMenu()
    }

    private fun setLastSync(ocFile: OCFile) {
        if (ocFile.lastSyncDateForData!! > ZERO_MILLISECOND_TIME) {
            binding.fdLastSync.visibility = View.VISIBLE
            binding.fdLastSyncLabel.visibility = View.VISIBLE
            binding.fdLastSync.text = DisplayUtils.unixTimeToHumanReadable(ocFile.lastSyncDateForData!!)
        }
    }

    private fun setModified(ocFile: OCFile) {
        if (ocFile.modificationTimestamp!! > ZERO_MILLISECOND_TIME) {
            binding.fdModified.visibility = View.VISIBLE
            binding.fdModifiedLabel.visibility = View.VISIBLE
            binding.fdModified.text = DisplayUtils.unixTimeToHumanReadable(ocFile.modificationTimestamp!!)
        }
    }

    private fun setCreated(ocFile: OCFile) {
        if (ocFile.creationTimestamp!! > ZERO_MILLISECOND_TIME) {
            binding.fdCreated.visibility = View.VISIBLE
            binding.fdCreatedLabel.visibility = View.VISIBLE
            binding.fdCreated.text = DisplayUtils.unixTimeToHumanReadable(ocFile.creationTimestamp!!)
        }
    }

    private fun setSpaceName(ocFileWithSyncInfo: OCFileWithSyncInfo) {
        val space = ocFileWithSyncInfo.space
        if (space != null) {
            binding.fdSpace.visibility = View.VISIBLE
            binding.fdSpaceLabel.visibility = View.VISIBLE
            binding.fdIconSpace.visibility = View.VISIBLE
            if (space.isPersonal) {
                binding.fdSpace.text = getString(R.string.bottom_nav_personal)
            } else {
                binding.fdSpace.text = space.name
            }
        }
    }

    private fun setIconPinAccordingToFilesLocalState(thumbnailImageView: ImageView, ocFileWithSyncInfo: OCFileWithSyncInfo) {
        // local state
        thumbnailImageView.bringToFront()
        thumbnailImageView.isVisible = false

        val file =  ocFileWithSyncInfo.file
        if (ocFileWithSyncInfo.isSynchronizing) {
            thumbnailImageView.setImageResource(R.drawable.sync_pin)
            thumbnailImageView.visibility = View.VISIBLE
        } else if (file.etagInConflict != null) {
            // conflict
            thumbnailImageView.setImageResource(R.drawable.error_pin)
            thumbnailImageView.visibility = View.VISIBLE
        } else if (file.isAvailableOffline) {
            thumbnailImageView.setImageResource(R.drawable.offline_available_pin)
            thumbnailImageView.visibility = View.VISIBLE
        } else if (file.isAvailableLocally) {
            thumbnailImageView.setImageResource(R.drawable.downloaded_pin)
            thumbnailImageView.visibility = View.VISIBLE
        }
    }

    private fun setMimeType(ocFile: OCFile) {
        binding.fdType.text = DisplayUtils.convertMIMEtoPrettyPrint(ocFile.mimeType)

        binding.fdImageDetailFile.let { imageView ->
            imageView.apply {
                tag = ocFile.id
                setOnClickListener {
                    if (!ocFile.isAvailableLocally) {  // Download the file
                        Timber.d("%s : File must be downloaded before opening it", ocFile.remotePath)
                        fileDetailsViewModel.updateActionInDetailsView(SYNC_AND_OPEN)
                    } else { // Already downloaded -> Open it
                        navigateToPreviewOrOpenFile(ocFile)
                    }
                }
            }
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
        val safeFile = fileDetailsViewModel.getCurrentFile() ?: return

        showProgressView(isTransferGoingOn = true)
        binding.fdProgressText.text = if (workInfo.isDownload()) {
            getString(R.string.downloader_download_enqueued_ticker, safeFile.file.fileName)
        } else { // Transfer is upload (?)
            getString(R.string.uploader_upload_enqueued_ticker, safeFile.file.fileName)
        }
        binding.fdProgressBar.apply {
            progress = 0
            isIndeterminate = false
        }
    }

    private fun updateLayoutForRunningTransfer(workInfo: WorkInfo) {
        fileDetailsViewModel.getCurrentFile() ?: return

        showProgressView(isTransferGoingOn = true)
        binding.fdProgressText.text = if (workInfo.isDownload()) {
            getString(R.string.downloader_download_in_progress_ticker)
        } else { // Transfer is upload (?)
            getString(R.string.uploader_upload_in_progress_ticker)
        }
        binding.fdProgressBar.apply {
            isIndeterminate = false
            progress = workInfo.progress.getInt(DownloadFileWorker.WORKER_KEY_PROGRESS, -1)
        }
        binding.fdCancelBtn.setOnClickListener { fileDetailsViewModel.cancelCurrentTransfer() }
    }

    private fun updateLayoutForSucceededTransfer(workInfo: WorkInfo) {
        val safeFile = fileDetailsViewModel.getCurrentFile() ?: return

        showProgressView(isTransferGoingOn = false)

        if (workInfo.isDownload()) {
            when (fileDetailsViewModel.actionsInDetailsView.value) {
                NONE -> {}
                SYNC -> {
                    fileDetailsViewModel.updateActionInDetailsView(NONE)
                }

                SYNC_AND_OPEN -> {
                    navigateToPreviewOrOpenFile(file)
                    fileDetailsViewModel.updateActionInDetailsView(NONE)
                }

                SYNC_AND_OPEN_WITH -> {
                    requireActivity().openOCFile(safeFile.file)
                    fileDetailsViewModel.updateActionInDetailsView(NONE)
                }

                SYNC_AND_SEND -> {
                    requireActivity().sendDownloadedFilesByShareSheet(listOf(safeFile.file))
                    fileDetailsViewModel.updateActionInDetailsView(NONE)
                }
            }

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
        fileDetailsViewModel.updateActionInDetailsView(NONE)
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

    // TODO: Move navigation to a common place.
    private fun navigateToPreviewOrOpenFile(fileWaitingToPreview: OCFile) {
        val fileDisplayActivity = requireActivity() as FileDisplayActivity
        when {
            PreviewImageFragment.canBePreviewed(fileWaitingToPreview) -> {
                fileDisplayActivity.startImagePreview(fileWaitingToPreview)
            }

            PreviewAudioFragment.canBePreviewed(fileWaitingToPreview) -> {
                fileDisplayActivity.startAudioPreview(fileWaitingToPreview, 0)
            }

            PreviewVideoFragment.canBePreviewed(fileWaitingToPreview) -> {
                fileDisplayActivity.startVideoPreview(fileWaitingToPreview, 0)
            }

            PreviewTextFragment.canBePreviewed(fileWaitingToPreview) -> {
                fileDisplayActivity.startTextPreview(fileWaitingToPreview)
            }

            else -> fileDisplayActivity.openOCFile(fileWaitingToPreview)
        }
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
        private const val ARG_SYNC_FILE_AT_OPEN = "SYNC_FILE_AT_OPEN"
        private const val ZERO_MILLISECOND_TIME = 0

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
