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
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.databinding.GridItemBinding
import com.owncloud.android.databinding.ItemFileListBinding
import com.owncloud.android.databinding.ListFooterBinding
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.files.model.OCFooterFile
import com.owncloud.android.presentation.diffutils.FileListDiffCallback
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimetypeIconUtil
import com.owncloud.android.utils.PreferenceUtils

class FileListAdapter(
    private val context: Context,
    private val layoutManager: GridLayoutManager,
    private val listener: FileListAdapterListener,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var files = mutableListOf<Any>()
    private lateinit var viewHolder: RecyclerView.ViewHolder
    private lateinit var parent: ViewGroup

    private val account = AccountUtils.getCurrentOwnCloudAccount(context)

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
        this.parent = parent
        viewHolder = when (viewType) {
            ViewType.LIST_ITEM.ordinal -> {
                val binding = ItemFileListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                binding.root.tag = ViewType.LIST_ITEM
                binding.root.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
                ListViewHolder(binding)
            }
            ViewType.GRID_IMAGE.ordinal -> {
                val binding = GridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                binding.root.tag = ViewType.GRID_IMAGE
                GridImageViewHolder(binding)
            }
            ViewType.GRID_ITEM.ordinal -> {
                val binding = GridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                binding.root.tag = ViewType.GRID_ITEM
                GridViewHolder(binding)
            }
            else -> {
                val binding = ListFooterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                binding.root.tag = ViewType.FOOTER
                FooterViewHolder(binding)
            }
        }
        return viewHolder
    }

    override fun getItemCount(): Int = files.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getItemViewType(position: Int): Int {

        return if (position == files.size.minus(1)) {
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewType = getItemViewType(position)
        if (position != files.size - 1) {
            val file = files[position] as OCFile

            val localStateView = holder.itemView.findViewById<ImageView>(R.id.localFileIndicator)
            val fileIcon = holder.itemView.findViewById<ImageView>(R.id.thumbnail).apply {
                tag = file.id
            }

            val name = file.fileName

            holder.itemView.findViewById<LinearLayout>(R.id.ListItemLayout)?.apply {
                contentDescription = "LinearLayout-$name"

                // Allow or disallow touches with other visible windows
                filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
            }

            when (viewType) {
                ViewType.LIST_ITEM.ordinal -> {
                    val view = holder as ListViewHolder
                    view.binding.let {
                        it.fileListConstraintLayout.apply {
                            filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(context)
                        }

                        it.Filename.text = file.fileName
                        it.fileListSize.text = DisplayUtils.bytesToHumanReadable(file.length, context)
                        it.fileListLastMod.text = DisplayUtils.getRelativeTimestamp(context, file.modificationTimestamp)

                        //TODO Do when manage available offline
                        /*if(onlyAvailableOffline || sharedByLinkFiles ){
                            it.fileListPath.apply {
                                visibility = View.VISIBLE
                                text = file.remotePath
                            }
                        }*/
                    }
                }
                ViewType.GRID_ITEM.ordinal -> {
                    //Filename
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
                                visibility = View.VISIBLE
                                bringToFront()
                            }
                        } else if (file.sharedWithSharee == true || file.isSharedWithMe) {
                            it.sharedIcon.apply {
                                setImageResource(R.drawable.shared_via_users)
                                visibility = View.VISIBLE
                                bringToFront()
                            }
                        } else {
                            it.sharedIcon.visibility = View.GONE
                        }
                    }
                }

            }

            // For all Views
            //setIconPinAccordingToFilesLocalState(localStateView, file)

            holder.itemView.setOnClickListener { listener.clickItem(file) }

            val checkBoxV = holder.itemView.findViewById<ImageView>(R.id.custom_checkbox).apply {
                visibility = View.GONE
            }
            holder.itemView.setBackgroundColor(Color.WHITE)

            //TODO Modify when implements select mode
            /*val parentList = parent as AbsListView
            if (parentList.choiceMode != AbsListView.CHOICE_MODE_NONE && parentList.checkedItemCount > 0) {
                if (parentList.isItemChecked(position)) {
                    holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.selected_item_background))
                    checkBoxV.setImageResource(R.drawable.ic_checkbox_marked)
                } else {
                    holder.itemView.setBackgroundColor(Color.WHITE)
                    checkBoxV.setImageResource(R.drawable.ic_checkbox_blank_outline)
                }
                checkBoxV.visibility = View.VISIBLE
            }*/

            if (file.isFolder) {
                //Folder
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
                            /*val task = ThumbnailsCacheManager.ThumbnailGenerationTask(fileIcon, storageManager, account)
                            val asyncDrawable = ThumbnailsCacheManager.AsyncThumbnailDrawable(context.resources, thumbnail, task)

                            // If drawable is not visible, do not update it.
                            if (asyncDrawable.minimumHeight > 0 && asyncDrawable.minimumWidth > 0) {
                                fileIcon.setImageDrawable(asyncDrawable)
                            }
                            task.execute(file)*/
                        }
                    }

                    if (file.mimeType == "image/png") {
                        fileIcon.setBackgroundColor(ContextCompat.getColor(context, R.color.background_color))
                    }
                }
            }

        } else {
            if (viewType == ViewType.FOOTER.ordinal) {
                val view = holder as FooterViewHolder
                val file = files[position] as OCFooterFile
                view.binding.footerText.text = file.text
            }
        }
    }

    //TODO Uncomment when manage state sync
    /*private fun setIconPinAccordingToFilesLocalState(localStateView: ImageView, file: OCFile) {
        //local state
        localStateView.bringToFront()
        val workManager = WorkManager.getInstance(context)
        val uploaderBinder = transferServiceGetter.getFileUploaderBinder()
        val opsBinder = transferServiceGetter.getOperationsServiceBinder()

        localStateView.visibility = View.INVISIBLE

        if (opsBinder != null && opsBinder.isSynchronizing(account, file)) {
            //syncing
            localStateView.apply {
                setImageResource(R.drawable.sync_pin)
                visibility = View.VISIBLE
            }
        } else if (workManager.isDownloadPending(account, file)) {
            // downloading
            localStateView.apply {
                setImageResource(R.drawable.sync_pin)
                visibility = View.VISIBLE
            }
        } else if (uploaderBinder != null && uploaderBinder.isUploading(account, file)) {
            // uploading
            localStateView.apply {
                setImageResource(R.drawable.sync_pin)
                visibility = View.VISIBLE
            }
        }else if(file.etagInConflict != null){
            //conflict
            localStateView.apply {
                setImageResource(R.drawable.error_pin)
                visibility = View.VISIBLE
            }
        }else{
            if(file.isAvailableLocally){
                localStateView.apply {
                    setImageResource(R.drawable.downloaded_pin)
                    visibility = View.VISIBLE
                }
            }
            FIXME: 13/10/2020 : New_arch: Av.Offline
            if (file.isAvailableOffline()) {
                localStateView.setVisibility(View.VISIBLE);
                localStateView.setImageResource(R.drawable.offline_available_pin);
           }
        }
    }*/

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

    inner class GridViewHolder(val binding: GridItemBinding) : RecyclerView.ViewHolder(binding.root)
    inner class GridImageViewHolder(val binding: GridItemBinding) : RecyclerView.ViewHolder(binding.root)
    inner class ListViewHolder(val binding: ItemFileListBinding) : RecyclerView.ViewHolder(binding.root)
    inner class FooterViewHolder(val binding: ListFooterBinding) : RecyclerView.ViewHolder(binding.root)

    enum class ViewType {
        LIST_ITEM, GRID_IMAGE, GRID_ITEM, FOOTER
    }
}
