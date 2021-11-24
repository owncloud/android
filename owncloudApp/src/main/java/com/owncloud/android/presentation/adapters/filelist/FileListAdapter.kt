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
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.extensions.setPicture
import com.owncloud.android.presentation.diffutils.FileListDiffCallback
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimetypeIconUtil

class FileListAdapter(
    private val context: Context
) : RecyclerView.Adapter<FileListAdapter.ViewHolder>() {

    var onItemClick: ((OCFile) -> Unit)? = null
    private val files = mutableListOf<OCFile>()

    fun updateFileList(filesToAdd: List<OCFile>) {
        val diffUtilCallback = FileListDiffCallback(oldList = files, newList = filesToAdd)
        val diffResult = DiffUtil.calculateDiff(diffUtilCallback)
        files.clear()
        files.addAll(filesToAdd)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFileListBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = files.size

    override fun getItemId(position: Int): Long = position.toLong()

    fun getItem(position: Int): Any? {
        return if (files.size <= position) {
            null
        } else files[position]
    }

    override fun getItemViewType(position: Int): Int = position

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(files[position]) {
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

    inner class ViewHolder(val binding: ItemFileListBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                onItemClick?.invoke(files[adapterPosition])
            }
        }
    }
}
