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

import android.accounts.Account
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.databinding.GridItemBinding
import com.owncloud.android.databinding.ItemFileListBinding
import com.owncloud.android.databinding.ListFooterBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFooterFile
import com.owncloud.android.presentation.diffutils.FileListDiffCallback
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimetypeIconUtil
import com.owncloud.android.utils.PreferenceUtils

class FileListAdapter(
    private val context: Context,
    private val isShowingJustFolders: Boolean,
    private val layoutManager: StaggeredGridLayoutManager,
    private val listener: FileListAdapterListener,
) : SelectableAdapter<RecyclerView.ViewHolder>() {

    var files = mutableListOf<Any>()
    private var account: Account? = AccountUtils.getCurrentOwnCloudAccount(context)
    private lateinit var storageManager: FileDataStorageManager

    init {
        if (account != null) {
            storageManager = FileDataStorageManager(context, account!!, context.contentResolver)
        }
    }

    fun updateFileList(filesToAdd: List<OCFile>) {
        val diffUtilCallback = FileListDiffCallback(oldList = files, newList = filesToAdd)
        val diffResult = DiffUtil.calculateDiff(diffUtilCallback)
        files.clear()
        files.addAll(filesToAdd)

        if (filesToAdd.isNotEmpty()) {
            files.add(OCFooterFile(manageListOfFilesAndGenerateText(filesToAdd)))
        }

        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.LIST_ITEM.ordinal -> {
                val binding = ItemFileListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                binding.root.apply {
                    tag = ViewType.LIST_ITEM
                    filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
                }
                ListViewHolder(binding)
            }
            ViewType.GRID_IMAGE.ordinal -> {
                val binding = GridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                binding.root.apply {
                    tag = ViewType.GRID_IMAGE
                    filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
                }
                GridImageViewHolder(binding)
            }
            ViewType.GRID_ITEM.ordinal -> {
                val binding = GridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                binding.root.apply {
                    tag = ViewType.GRID_ITEM
                    filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
                }
                GridViewHolder(binding)
            }
            else -> {
                val binding = ListFooterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                binding.root.apply {
                    tag = ViewType.FOOTER
                    filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
                }
                FooterViewHolder(binding)
            }
        }
    }

    override fun getItemCount(): Int = files.size

    override fun getItemId(position: Int): Long = position.toLong()

    private fun isFooter(position: Int) = position == files.size.minus(1)

    override fun getItemViewType(position: Int): Int {

        return if (isFooter(position)) {
            ViewType.FOOTER.ordinal
        } else {
            when {
                layoutManager.spanCount == 1 -> {
                    ViewType.LIST_ITEM.ordinal
                }
                (files[position] as OCFile).isImage -> {
                    ViewType.GRID_IMAGE.ordinal
                }
                else -> {
                    ViewType.GRID_ITEM.ordinal
                }
            }
        }
    }

    fun getCheckedItems(): List<OCFile> {
        val checkedItems = mutableListOf<OCFile>()
        val checkedPositions = getSelectedItems()

        for (i in checkedPositions) {
            if (files[i] is OCFile){
                checkedItems.add(files[i] as OCFile)
            }
        }

        return checkedItems
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val viewType = getItemViewType(position)


        if (viewType != ViewType.FOOTER.ordinal) { // Is Item

            val file = files[position] as OCFile
            val name = file.fileName
            val fileIcon = holder.itemView.findViewById<ImageView>(R.id.thumbnail).apply {
                tag = file.id
            }

            holder.itemView.findViewById<LinearLayout>(R.id.ListItemLayout)?.apply {
                contentDescription = "LinearLayout-$name"

                // Allow or disallow touches with other visible windows
                filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
            }

            when (viewType) {
                ViewType.LIST_ITEM.ordinal -> {
                    val view = holder as ListViewHolder
                    view.binding.let {
                        it.fileListConstraintLayout.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
                        it.Filename.text = file.fileName
                        it.fileListSize.text = DisplayUtils.bytesToHumanReadable(file.length, context)
                        it.fileListLastMod.text = DisplayUtils.getRelativeTimestamp(context, file.modificationTimestamp)

                    }
                }
                ViewType.GRID_ITEM.ordinal -> {
                    // Filename
                    val view = holder as GridViewHolder
                    view.binding.Filename.text = file.fileName
                }

                ViewType.GRID_IMAGE.ordinal -> {
                    //SharedIcon
                    val view = holder as GridImageViewHolder
                    view.binding.let {
                        if (file.sharedByLink) {
                            it.sharedIcon.apply {
                                setImageResource(R.drawable.ic_shared_by_link)
                                isVisible = true
                                bringToFront()
                            }
                        } else if (file.sharedWithSharee == true || file.isSharedWithMe) {
                            it.sharedIcon.apply {
                                setImageResource(R.drawable.shared_via_users)
                                isVisible = true
                                bringToFront()
                            }
                        } else {
                            it.sharedIcon.isVisible = false
                        }
                    }
                }

            }

            // TODO Delete it when manage state sync.
            holder.itemView.findViewById<ImageView>(R.id.localFileIndicator).isVisible = false

            holder.itemView.findViewById<ImageView>(R.id.sharedIcon).isVisible = false


            holder.itemView.setOnClickListener {
                listener.clickItem(
                    ocFile = file,
                    position = position
                )
            }

            holder.itemView.setOnLongClickListener {
                listener.longClickItem(
                    ocFile = file,
                    position = position
                )
            }

            val checkBoxV = holder.itemView.findViewById<ImageView>(R.id.custom_checkbox).apply {
                isVisible = false
            }
            holder.itemView.setBackgroundColor(Color.WHITE)

            if (isSelected(position)) {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.selected_item_background))
                checkBoxV.setImageResource(R.drawable.ic_checkbox_marked)
            } else {
                holder.itemView.setBackgroundColor(Color.WHITE)
                checkBoxV.setImageResource(R.drawable.ic_checkbox_blank_outline)
            }
            checkBoxV.isVisible = getCheckedItems().isNotEmpty()

            if (file.isFolder) {
                // Folder
                fileIcon.setImageResource(
                    MimetypeIconUtil.getFolderTypeIconId(
                        file.isSharedWithMe || file.sharedWithSharee == true,
                        file.sharedByLink
                    )
                )
            } else {
                // Set file icon depending on its mimetype. Ask for thumbnail later.
                fileIcon.setImageResource(MimetypeIconUtil.getFileTypeIconId(file.mimeType, file.fileName))
                if (file.remoteId != null) {
                    val thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(file.remoteId)
                    if (thumbnail != null) {
                        fileIcon.setImageBitmap(thumbnail)
                    }
                    if (file.needsToUpdateThumbnail) {
                        // generate new Thumbnail
                        if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, fileIcon)) {
                            val task = ThumbnailsCacheManager.ThumbnailGenerationTask(fileIcon, storageManager, account)
                            val asyncDrawable = ThumbnailsCacheManager.AsyncThumbnailDrawable(context.resources, thumbnail, task)

                            // If drawable is not visible, do not update it.
                            if (asyncDrawable.minimumHeight > 0 && asyncDrawable.minimumWidth > 0) {
                                fileIcon.setImageDrawable(asyncDrawable)
                            }
                            task.execute(file)
                        }
                    }

                    if (file.mimeType == "image/png") {
                        fileIcon.setBackgroundColor(ContextCompat.getColor(context, R.color.background_color))
                    }
                }
            }

        } else { // Is Footer
            if (!isShowingJustFolders) {
                val view = holder as FooterViewHolder
                val file = files[position] as OCFooterFile
                (view.itemView.layoutParams as StaggeredGridLayoutManager.LayoutParams).apply {
                    isFullSpan = true
                }
                view.binding.footerText.text = file.text
            }
        }
    }

    private fun manageListOfFilesAndGenerateText(list: List<OCFile>): String {
        val filesCount = 0
        val foldersCount = 0
        for (file in list) {
            if (file.isFolder) {
                foldersCount.plus(1)
            } else {
                if (!file.isHidden) {
                    filesCount.plus(1)
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
        fun clickItem(ocFile: OCFile, position: Int)
        fun longClickItem(ocFile: OCFile, position: Int): Boolean = true
    }

    inner class GridViewHolder(val binding: GridItemBinding) : RecyclerView.ViewHolder(binding.root)
    inner class GridImageViewHolder(val binding: GridItemBinding) : RecyclerView.ViewHolder(binding.root)
    inner class ListViewHolder(val binding: ItemFileListBinding) : RecyclerView.ViewHolder(binding.root)
    inner class FooterViewHolder(val binding: ListFooterBinding) : RecyclerView.ViewHolder(binding.root)

    enum class ViewType {
        LIST_ITEM, GRID_IMAGE, GRID_ITEM, FOOTER
    }
}
