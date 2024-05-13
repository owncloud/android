/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Abel García de Prada
 * @author Shashvat Kedia
 * @author Juan Carlos Garrote Gascón
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.preview

import android.accounts.Account
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.chrisbanes.photoview.PhotoView
import com.owncloud.android.R
import com.owncloud.android.databinding.PreviewImageFragmentBinding
import com.owncloud.android.domain.files.model.MIME_SVG
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.extensions.collectLatestLifecycleFlow
import com.owncloud.android.extensions.filterMenuOptions
import com.owncloud.android.extensions.sendDownloadedFilesByShareSheet
import com.owncloud.android.presentation.files.operations.FileOperation
import com.owncloud.android.presentation.files.operations.FileOperationsViewModel
import com.owncloud.android.presentation.files.removefile.RemoveFilesDialogFragment
import com.owncloud.android.presentation.files.removefile.RemoveFilesDialogFragment.Companion.TAG_REMOVE_FILES_DIALOG_FRAGMENT
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.PreferenceUtils
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.File

/**
 * This fragment shows a preview of a downloaded image.
 *
 * Trying to get an instance with a NULL [OCFile] will produce an
 * [IllegalStateException].
 *
 * If the [OCFile] passed is not downloaded, an [IllegalStateException] is generated on
 * instantiation too.
 * MUST BE KEPT: the system uses it when tries to reinstantiate a fragment automatically
 * (for instance, when the device is turned a aside).
 *
 * DO NOT CALL IT: an [OCFile] and [Account] must be provided for a successful
 * construction
 */
class PreviewImageFragment : FileFragment() {

    private val bitmap: Bitmap? = null
    private var account: Account? = null
    private var ignoreFirstSavedState = false

    private var _binding: PreviewImageFragmentBinding? = null
    private val binding get() = _binding!!

    private val previewImageViewModel by viewModel<PreviewImageViewModel>()
    private val fileOperationsViewModel: FileOperationsViewModel by inject()

    /**
     * {@inheritDoc}
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            file = it.getParcelable(ARG_FILE)
            // TODO better in super, but needs to check ALL the class extending FileFragment;
            // not right now
            ignoreFirstSavedState = it.getBoolean(ARG_IGNORE_FIRST)
        }
        setHasOptionsMenu(true)
    }

    /**
     * {@inheritDoc}
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        // Inflate the layout for this fragment
        _binding = PreviewImageFragmentBinding.inflate(inflater, container, false)
        return binding.root.apply {
            // Allow or disallow touches with other visible windows
            filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.top.setBackgroundColor(getBackgroundColor(file))

        binding.photoView.isVisible = false
        binding.photoView.setOnClickListener {
            (requireActivity() as PreviewImageActivity).toggleFullScreen()
        }

        savedInstanceState?.let {
            if (!ignoreFirstSavedState) {
                val file: OCFile? = it.getParcelable(ARG_FILE)
                file?.let { ocFile ->
                    setFile(ocFile)
                }
            } else {
                ignoreFirstSavedState = false
            }
        }

        account = requireArguments().getParcelable(PreviewAudioFragment.EXTRA_ACCOUNT)
        checkNotNull(account) { "Instanced with a NULL ownCloud Account" }
        checkNotNull(file) { "Instanced with a NULL OCFile" }
        check(file.isAvailableLocally) { "There is no local file to preview" }

        binding.message.isVisible = false
        binding.progressWheel.isVisible = true
    }

    fun getImageView(): PhotoView = binding.photoView

    /**
     * {@inheritDoc}
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(ARG_FILE, file)
    }

    override fun onStart() {
        super.onStart()
        file?.let {
            loadAndShowImage()
        }
        isOpen = true
        currentFilePreviewing = file
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
        val safeFile = file
        // Update the file
        file = mContainerActivity.storageManager.getFileById(file.id ?: -1)
        val accountName = mContainerActivity.storageManager.account.name
        previewImageViewModel.filterMenuOptions(safeFile, accountName)

        collectLatestLifecycleFlow(previewImageViewModel.menuOptions) { menuOptions ->
            val hasWritePermission = safeFile.hasWritePermission
            menu.filterMenuOptions(menuOptions, hasWritePermission)
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
                openFile()
                true
            }

            R.id.action_remove_file -> {
                val dialog = RemoveFilesDialogFragment.newInstance(file)
                dialog.show(requireFragmentManager(), TAG_REMOVE_FILES_DIALOG_FRAGMENT)
                true
            }

            R.id.action_see_details -> {
                seeDetails()
                true
            }

            R.id.action_send_file -> {
                requireActivity().sendDownloadedFilesByShareSheet(listOf(file))
                true
            }

            R.id.action_sync_file -> {
                mContainerActivity.fileOperationsHelper.syncFile(file)
                true
            }

            R.id.action_set_available_offline -> {
                fileOperationsViewModel.performOperation(FileOperation.SetFilesAsAvailableOffline(listOf(file)))
                true
            }

            R.id.action_unset_available_offline -> {
                fileOperationsViewModel.performOperation(FileOperation.UnsetFilesAsAvailableOffline(listOf(file)))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun seeDetails() {
        mContainerActivity.showDetails(file)
    }

    override fun onDestroy() {
        bitmap?.recycle()
        // putting this in onStop() is just the same; the fragment is always destroyed by
        // {@link FragmentStatePagerAdapter} when the fragment in swiped further than the
        // valid offscreen distance, and onStop() is never called before than that
        super.onDestroy()
        isOpen = false
        currentFilePreviewing = null
    }

    /**
     * Opens the previewed image with an external application.
     */
    private fun openFile() {
        mContainerActivity.fileOperationsHelper.openFile(file)
        finish()
    }

    override fun onFileMetadataChanged(updatedFile: OCFile) {
        file = updatedFile
        requireActivity().invalidateOptionsMenu()
    }

    override fun onFileMetadataChanged() {
        file = mContainerActivity.storageManager.getFileByPath(file.remotePath)
        requireActivity().invalidateOptionsMenu()
    }

    override fun onFileContentChanged() = loadAndShowImage()

    override fun updateViewForSyncInProgress() {
        // Nothing to do here, sync is not shown in previews
    }

    override fun updateViewForSyncOff() {
        // Nothing to do here, sync is not shown in previews
    }

    private fun loadAndShowImage() {
        val localStoragePath = file?.storagePath
        if (localStoragePath == null) {
            Timber.w("Storage path for ${file.fileName} is null, nothing to show here")
            return
        }
        Glide.with(requireContext())
            .load(File(localStoragePath))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any, target: Target<Drawable?>, isFirstResource: Boolean
                ): Boolean {
                    try {
                        binding.errorGroup.isVisible = true
                    } catch (npe: NullPointerException) {
                        Timber.e(npe)
                    }
                    Timber.e(e, "Error loading image")
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?, model: Any, target: Target<Drawable?>,
                    dataSource: DataSource, isFirstResource: Boolean
                ): Boolean {
                    Timber.d("Loading image %s", file.fileName)

                    view?.findViewById<ProgressBar>(R.id.progressWheel)?.isVisible = false
                    return false
                }
            })
            .into(binding.photoView)

        binding.photoView.isVisible = true
    }

    private fun isSVGFile(file: OCFile): Boolean = file.mimeType == MIME_SVG

    private fun getBackgroundColor(file: OCFile): Int {
        return if (isSVGFile(file)) Color.WHITE else Color.BLACK
    }

    /**
     * Finishes the preview
     */
    private fun finish() {
        activity?.finish()
    }

    companion object {
        private const val ARG_FILE = "FILE"
        private const val ARG_ACCOUNT = "ACCOUNT"
        private const val ARG_IGNORE_FIRST = "IGNORE_FIRST"
        var isOpen: Boolean = false
        var currentFilePreviewing: OCFile? = null

        /**
         * Public factory method to create a new fragment that previews an image.
         *
         * Android strongly recommends keep the empty constructor of fragments as the only public constructor, and
         * use [.setArguments] to set the needed arguments.
         *
         * This method hides to client objects the need of doing the construction in two steps.
         *
         * @param file                  An [OCFile] to preview as an image in the fragment
         * @param myAccount             ownCloud account containing file
         * @param ignoreFirstSavedState Flag to work around an unexpected behaviour of [androidx.fragment.app.FragmentStatePagerAdapter]
         * @return Fragment ready to be used.
         */
        @JvmStatic
        fun newInstance(file: OCFile?, myAccount: Account?, ignoreFirstSavedState: Boolean): PreviewImageFragment {
            val args = Bundle().apply {
                putParcelable(ARG_FILE, file)
                putParcelable(ARG_ACCOUNT, myAccount)
                putBoolean(ARG_IGNORE_FIRST, ignoreFirstSavedState)
            }
            return PreviewImageFragment().apply { arguments = args }
        }

        /**
         * Helper method to test if an [OCFile] can be passed to a [PreviewImageFragment] to be previewed.
         *
         * @param file File to test if can be previewed.
         * @return 'True' if the file can be handled by the fragment.
         */
        @JvmStatic
        fun canBePreviewed(file: OCFile?): Boolean {
            return file != null && file.isImage
        }
    }
}
