/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Abel García de Prada
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package com.owncloud.android.presentation.files.removefile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.owncloud.android.R
import com.owncloud.android.databinding.RemoveFilesDialogBinding
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.presentation.files.operations.FileOperation
import com.owncloud.android.presentation.files.operations.FileOperationsViewModel
import com.owncloud.android.utils.MimetypeIconUtil
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

/**
 * Dialog requiring confirmation before removing a collection of given OCFiles.
 *
 * Triggers the removal according to the user response.
 */
class RemoveFilesDialogFragment : DialogFragment() {

    private val fileOperationViewModel: FileOperationsViewModel by sharedViewModel()
    private var _binding: RemoveFilesDialogBinding? = null
    private val binding get() = _binding!!

    private lateinit var targetFiles: List<OCFile>
    private var isAvailableLocallyAndNotAvailableOffline: Boolean = false
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = RemoveFilesDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var containsFolder = false
        var messageStringId: Int
        val messageArguments: String

        requireArguments().apply {
            targetFiles = getParcelableArrayList(ARG_TARGET_FILES) ?: emptyList()
            isAvailableLocallyAndNotAvailableOffline = getBoolean(ARG_IS_AVAILABLE_LOCALLY_AND_NOT_AVAILABLE_OFFLINE)
        }

        binding.apply {

            for (file in targetFiles) {
                if (file.isFolder) {
                    containsFolder = true
                }
            }

            handleThumbnail(targetFiles, dialogRemoveThumbnail)

            messageStringId = if (targetFiles.size == 1) {
                // choose message for a single file
                val file = targetFiles.first()
                messageArguments = file.fileName
                if (file.isFolder) {
                    R.string.confirmation_remove_folder_alert
                } else {
                    R.string.confirmation_remove_file_alert
                }
            } else {
                // choose message for more than one file
                messageArguments = targetFiles.size.toString()
                if (containsFolder) {
                    R.string.confirmation_remove_folders_alert
                } else {
                    R.string.confirmation_remove_files_alert
                }
            }
            dialogRemoveInformation.text = requireContext().getString(messageStringId, messageArguments)

            if (isAvailableLocallyAndNotAvailableOffline) {
                dialogRemoveLocalOnly.text = requireContext().getString(R.string.confirmation_remove_local)
            } else {
                dialogRemoveLocalOnly.visibility = View.GONE
            }

            dialogRemoveLocalOnly.setOnClickListener {
                fileOperationViewModel.performOperation(FileOperation.RemoveOperation(targetFiles.toList(), removeOnlyLocalCopy = true))
                dismiss()
            }

            dialogRemoveYes.setOnClickListener {
                fileOperationViewModel.performOperation(FileOperation.RemoveOperation(targetFiles.toList(), removeOnlyLocalCopy = false))
                dismiss()
            }

            dialogRemoveNo.setOnClickListener { dismiss() }
        }
    }

    private fun handleThumbnail(files: List<OCFile>, thumbnailImageView: ImageView) {
        if (files.size == 1) {
            val file = files[0]
            // Show the thumbnail when the file has one
            val thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(file.remoteId)
            if (thumbnail != null) {
                thumbnailImageView.setImageBitmap(thumbnail)
            } else {
                thumbnailImageView.setImageResource(
                    MimetypeIconUtil.getFileTypeIconId(file.mimeType, file.fileName)
                )
            }
        } else {
            thumbnailImageView.visibility = View.GONE
        }
    }

    companion object {
        const val TAG_REMOVE_FILES_DIALOG_FRAGMENT = "TAG_REMOVE_FILES_DIALOG_FRAGMENT"
        private const val ARG_TARGET_FILES = "ARG_TARGET_FILES"
        private const val ARG_IS_AVAILABLE_LOCALLY_AND_NOT_AVAILABLE_OFFLINE = "ARG_IS_AVAILABLE_LOCALLY_AND_NOT_AVAILABLE_OFFLINE"

        fun newInstance(files: ArrayList<OCFile>, isAvailableLocallyAndNotAvailableOffline: Boolean): RemoveFilesDialogFragment {

            val args = Bundle().apply {
                putParcelableArrayList(ARG_TARGET_FILES, files)
                putBoolean(ARG_IS_AVAILABLE_LOCALLY_AND_NOT_AVAILABLE_OFFLINE, isAvailableLocallyAndNotAvailableOffline)
            }
            return RemoveFilesDialogFragment().apply { arguments = args }
        }

        /**
         * Convenience factory method to create new RemoveFilesDialogFragment instances for a single file
         *
         * @param file           File to remove.
         * @return Dialog ready to show.
         */
        @JvmStatic
        @JvmName("newInstanceForSingleFile")
        fun newInstance(file: OCFile): RemoveFilesDialogFragment {
            return newInstance(files = arrayListOf(file), isAvailableLocallyAndNotAvailableOffline = file.isAvailableLocally)
        }
    }
}
