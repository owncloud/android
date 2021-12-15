/**
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
 * Copyright (C) 2021 ownCloud GmbH.
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
 *
 */

package com.owncloud.android.presentation.adapters.filelist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.databinding.ItemFileListBinding
import com.owncloud.android.databinding.ListFooterBinding
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFooterFile
import com.owncloud.android.extensions.setPicture
import com.owncloud.android.presentation.diffutils.FileListDiffCallback
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.MimetypeIconUtil

class FileListAdapter(
    private val context: Context,
    private val isShowingJustFolders: Boolean,
    private val listener: FileListAdapterListener,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var files = mutableListOf<Any>()
    private var filesToSort = listOf<OCFile>()
    private lateinit var viewHolder: RecyclerView.ViewHolder

    private val TYPE_ITEMS = 0
    private val TYPE_FOOTER = 1

    fun updateFileList(filesToAdd: List<OCFile>, sortTypeSelected: Int? = null) {
        val diffUtilCallback = FileListDiffCallback(oldList = files, newList = filesToAdd)
        val diffResult = DiffUtil.calculateDiff(diffUtilCallback)
        filesToSort = filesToAdd
        files.clear()
        files.addAll(filesToAdd)

        if (filesToAdd.isNotEmpty()) {
            files.add(OCFooterFile(manageListOfFilesAndGenerateText(filesToAdd)))
        }


        diffResult.dispatchUpdatesTo(this)
    }

    fun setSortOrder(order: Int, ascending: Boolean) {
        PreferenceManager.setSortOrder(order, context, FileStorageUtils.FILE_DISPLAY_SORT)
        PreferenceManager.setSortAscending(ascending, context, FileStorageUtils.FILE_DISPLAY_SORT)

        FileStorageUtils.mSortOrderFileDisp = order
        FileStorageUtils.mSortAscendingFileDisp = ascending

        val sortedFiles = FileStorageUtils.sortFolder(
            filesToSort, FileStorageUtils.mSortOrderFileDisp,
            FileStorageUtils.mSortAscendingFileDisp
        )

        updateFileList(sortedFiles)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            TYPE_ITEMS -> {
                val binding = ItemFileListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                viewHolder = ViewHolder(binding)
            }
            TYPE_FOOTER -> {
                val binding = ListFooterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                viewHolder = FooterViewHolder(binding)
            }
        }
        return viewHolder
    }

    override fun getItemCount(): Int = files.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getItemViewType(position: Int): Int {
        return if (position == files.size.minus(1)) {
            TYPE_FOOTER
        } else {
            TYPE_ITEMS
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            TYPE_ITEMS -> {
                with(holder as ViewHolder) {
                    with(files[position] as OCFile) {
                        binding.Filename.text = this.fileName
                        binding.fileListSize.text = DisplayUtils.bytesToHumanReadable(this.length, context)
                        binding.fileListLastMod.text = DisplayUtils.getRelativeTimestamp(context, this.modificationTimestamp)
                        binding.localFileIndicator.isVisible = false //TODO Modify in the future, when we start the synchronization task.
                        binding.customCheckbox.isVisible = false //TODO Modify in the future, when we start the multi selection task.
                        binding.thumbnail.let {
                            it.tag = this.id
                            getThumbnailPicture(imageView = it, file = this)
                        }
                        //TODO Check this with FileListListAdapter.java and its viewType (LIST or GRID)
                        getSharedIcon(imageView = binding.sharedIcon, file = this)
                        binding.root.setOnClickListener {
                            listener.clickItem(this)
                        }
                    }
                }
            }
            TYPE_FOOTER -> {
                if (!isShowingJustFolders) {
                    with(holder as FooterViewHolder) {
                        with(files[position] as OCFooterFile) {
                            binding.footerText.text = this.text
                        }
                    }
                }
            }
        }
    }

    private fun getThumbnailPicture(imageView: ImageView, file: OCFile) {
        if (file.isFolder) {
            imageView.setPicture(
                MimetypeIconUtil.getFolderTypeIconId(file.isSharedWithMe || file.sharedWithSharee ?: false, file.sharedByLink)
            )
        } else {
            // Set file icon depending on its mimetype.
            imageView.setPicture(MimetypeIconUtil.getFileTypeIconId(file.mimeType, file.fileName))
            if (file.mimeType.contentEquals("image/png")) {
                imageView.setBackgroundColor(ContextCompat.getColor(context, R.color.background_color))
            }
        }
    }

    private fun getSharedIcon(imageView: ImageView, file: OCFile) {
        if (file.sharedByLink) {
            imageView.setImageResource(R.drawable.ic_shared_by_link)
            imageView.visibility = View.VISIBLE
            imageView.bringToFront()
        } else if (file.sharedWithSharee == true || file.isSharedWithMe) {
            imageView.setImageResource(R.drawable.shared_via_users)
            imageView.visibility = View.VISIBLE
            imageView.bringToFront()
        } else {
            imageView.visibility = View.GONE
        }
    }

    private fun manageListOfFilesAndGenerateText(list: List<OCFile>): String {
        var filesCount = 0
        var foldersCount = 0
        val count: Int = list.size
        var file: OCFile
        for (i in 0 until count) {
            file = list[i]
            if (file.isFolder) {
                foldersCount++
            } else {
                if (!file.isHidden) {
                    filesCount++
                }
            }
        }

        return generateFooterText(filesCount, foldersCount)
    }

    private fun generateFooterText(filesCount: Int, foldersCount: Int): String {
        when {
            filesCount <= 0 -> {
                return when {
                    foldersCount <= 0 -> {
                        ""
                    }
                    foldersCount == 1 -> {
                        context.getString(R.string.file_list__footer__folder)
                    }
                    else -> { // foldersCount > 1
                        context.getString(R.string.file_list__footer__folders, foldersCount)
                    }
                }
            }
            filesCount == 1 -> {
                return when {
                    foldersCount <= 0 -> {
                        context.getString(R.string.file_list__footer__file)
                    }
                    foldersCount == 1 -> {
                        context.getString(R.string.file_list__footer__file_and_folder)
                    }
                    else -> { // foldersCount > 1
                        context.getString(R.string.file_list__footer__file_and_folders, foldersCount)
                    }
                }
            }
            else -> {    // filesCount > 1
                return when {
                    foldersCount <= 0 -> {
                        context.getString(R.string.file_list__footer__files, filesCount)
                    }
                    foldersCount == 1 -> {
                        context.getString(R.string.file_list__footer__files_and_folder, filesCount)
                    }
                    else -> { // foldersCount > 1
                        context.getString(
                            R.string.file_list__footer__files_and_folders, filesCount, foldersCount
                        )
                    }
                }
            }
        }
    }

    interface FileListAdapterListener {
        fun clickItem(ocFile: OCFile)
    }

    inner class ViewHolder(val binding: ItemFileListBinding) : RecyclerView.ViewHolder(binding.root)
    inner class FooterViewHolder(val binding: ListFooterBinding) : RecyclerView.ViewHolder(binding.root)
}
