/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
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

package com.owncloud.android.presentation.adapters.transfers

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.authentication.AccountUtils
import com.owncloud.android.databinding.UploadListGroupBinding
import com.owncloud.android.databinding.UploadListItemBinding
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager.AsyncThumbnailDrawable
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ThumbnailGenerationTask
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.domain.transfers.model.TransferStatus
import com.owncloud.android.extensions.statusToStringRes
import com.owncloud.android.extensions.toStringRes
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimetypeIconUtil
import com.owncloud.android.utils.PreferenceUtils
import timber.log.Timber
import java.io.File

class TransfersAdapter(
    val cancel: (Long) -> Unit,
    val delete: (Long) -> Unit,
    val retry: (OCTransfer) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val transfersList = mutableListOf<TransferRecyclerItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TransferRecyclerItemViewType.ITEM_VIEW_TRANSFER.ordinal) {
            val view = inflater.inflate(R.layout.upload_list_item, parent, false)
            view.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(parent.context)
            TransferItemViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.upload_list_group, parent, false)
            view.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(parent.context)
            HeaderItemViewHolder(view)
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TransferItemViewHolder -> {
                val transferItem = getItem(position) as TransferRecyclerItem.TransferItem
                holder.binding.apply {
                    val remoteFile = File(transferItem.transfer.remotePath)

                    var fileName = remoteFile.name
                    if (fileName.isEmpty()) {
                        fileName = File.separator
                    }
                    uploadName.text = fileName

                    uploadRemotePath.text = holder.itemView.context.getString(R.string.app_name) + remoteFile.parent

                    uploadFileSize.text = DisplayUtils.bytesToHumanReadable(transferItem.transfer.fileSize, holder.itemView.context)

                    uploadDate.isVisible = transferItem.transfer.transferEndTimestamp != null && transferItem.transfer.status != TransferStatus.TRANSFER_FAILED
                    transferItem.transfer.transferEndTimestamp?.let {
                        val dateString = DisplayUtils.getRelativeDateTimeString(
                            holder.itemView.context,
                            it,
                            DateUtils.SECOND_IN_MILLIS,
                            DateUtils.WEEK_IN_MILLIS,
                            0
                        )
                        uploadDate.text = ", $dateString"
                    }

                    try {
                        val account = AccountUtils.getOwnCloudAccountByName(holder.itemView.context, transferItem.transfer.accountName)
                        val oca = OwnCloudAccount(account, holder.itemView.context)
                        val accountName = oca.displayName + " @ " +
                                DisplayUtils.convertIdn(account.name.substring(account.name.lastIndexOf("@") + 1), false)
                        uploadAccount.text  = accountName
                    } catch (e: Exception) {
                        Timber.w("Couldn't get display name for account, using old style")
                        uploadAccount.text = transferItem.transfer.accountName
                    }

                    uploadStatus.isVisible = transferItem.transfer.status != TransferStatus.TRANSFER_SUCCEEDED
                    uploadStatus.text = " — " + holder.itemView.context.getString(transferItem.transfer.statusToStringRes())

                    val fakeFileToCheatThumbnailsCacheManagerInterface = OCFile(
                        owner = transferItem.transfer.accountName,
                        length = transferItem.transfer.fileSize,
                        modificationTimestamp = 0,
                        remotePath = transferItem.transfer.remotePath,
                        mimeType = MimetypeIconUtil.getBestMimeTypeByFilename(transferItem.transfer.localPath),
                        storagePath = transferItem.transfer.localPath,
                    )
                    val allowedToCreateNewThumbnail = ThumbnailsCacheManager.cancelPotentialThumbnailWork(
                        fakeFileToCheatThumbnailsCacheManagerInterface,
                        thumbnail
                    )
                    val parentActivity = holder.itemView.context as FileActivity
                    if (fakeFileToCheatThumbnailsCacheManagerInterface.isImage &&
                        fakeFileToCheatThumbnailsCacheManagerInterface.remoteId != null &&
                        transferItem.transfer.status == TransferStatus.TRANSFER_SUCCEEDED
                    ) {
                        // Thumbnail in cache?
                        var thumbnailImage = ThumbnailsCacheManager
                            .getBitmapFromDiskCache(fakeFileToCheatThumbnailsCacheManagerInterface.remoteId.toString())
                        if (thumbnailImage != null && !fakeFileToCheatThumbnailsCacheManagerInterface.needsToUpdateThumbnail) {
                            thumbnail.setImageBitmap(thumbnailImage)
                        } else {
                            // Generate new thumbnail
                            if (allowedToCreateNewThumbnail) {
                                val task = ThumbnailGenerationTask(thumbnail, parentActivity.account)
                                if (thumbnailImage == null) {
                                    thumbnailImage = ThumbnailsCacheManager.mDefaultImg
                                }
                                val asyncDrawable = AsyncThumbnailDrawable(
                                    parentActivity.resources,
                                    thumbnailImage,
                                    task
                                )
                                thumbnail.setImageDrawable(asyncDrawable)
                                task.execute(fakeFileToCheatThumbnailsCacheManagerInterface)
                                Timber.v("Executing task to generate a new thumbnail")
                            }
                        }
                        if (MimetypeIconUtil.getBestMimeTypeByFilename(transferItem.transfer.localPath) == "image/png") {
                            thumbnail.setBackgroundColor(holder.itemView.context.getColor(R.color.background_color))
                        }
                    } else if (fakeFileToCheatThumbnailsCacheManagerInterface.isImage) {
                        val file = File(transferItem.transfer.localPath)
                        // Thumbnail in cache?
                        var thumbnailImage = ThumbnailsCacheManager.getBitmapFromDiskCache(file.hashCode().toString())
                        if (thumbnailImage != null) {
                            thumbnail.setImageBitmap(thumbnailImage)
                        } else {
                            // Generate new thumbnail
                            if (allowedToCreateNewThumbnail) {
                                val task = ThumbnailGenerationTask(thumbnail)
                                thumbnailImage = ThumbnailsCacheManager.mDefaultImg
                                val asyncDrawable = AsyncThumbnailDrawable(
                                    parentActivity.resources,
                                    thumbnailImage,
                                    task
                                )
                                thumbnail.setImageDrawable(asyncDrawable)
                                task.execute(file)
                                Timber.v("Executing task to generate a new thumbnail")
                            }
                        }
                        if (MimetypeIconUtil.getBestMimeTypeByFilename(transferItem.transfer.localPath).equals("image/png", ignoreCase = true)) {
                            thumbnail.setBackgroundColor(holder.itemView.context.getColor(R.color.background_color))
                        }
                    } else {
                        thumbnail.setImageResource(
                            MimetypeIconUtil.getFileTypeIconId(MimetypeIconUtil.getBestMimeTypeByFilename(transferItem.transfer.localPath), fileName)
                        )
                    }

                    uploadRightButton.isVisible = transferItem.transfer.status != TransferStatus.TRANSFER_SUCCEEDED
                    holder.itemView.setOnClickListener(null)
                    when (transferItem.transfer.status) {
                        TransferStatus.TRANSFER_IN_PROGRESS, TransferStatus.TRANSFER_QUEUED -> {
                            uploadRightButton.apply {
                                setImageResource(R.drawable.ic_action_cancel_grey)
                                setOnClickListener {
                                    cancel(transferItem.transfer.id!!)
                                }
                            }
                        }
                        TransferStatus.TRANSFER_FAILED -> {
                            uploadRightButton.apply {
                                setImageResource(R.drawable.ic_action_delete_grey)
                                setOnClickListener {
                                    delete(transferItem.transfer.id!!)
                                }
                            }
                            holder.itemView.setOnClickListener {
                                retry(transferItem.transfer)
                            }
                            holder.binding.ListItemLayout.isClickable = true
                            holder.binding.ListItemLayout.isFocusable = true
                        }
                        else -> {
                            uploadRightButton.isVisible = false
                        }
                    }
                    // TODO: progress bar
                }
            }
            is HeaderItemViewHolder -> {
                val headerItem = getItem(position) as TransferRecyclerItem.HeaderItem
                holder.binding.apply {
                    uploadListGroupName.text = holder.itemView.context.getString(headerItem.status.toStringRes())

                    val stringResFileCount = if (headerItem.numberTransfers == 1) R.string.uploads_view_group_file_count_single else R.string.uploads_view_group_file_count
                    val fileCountText: String = String.format(holder.itemView.context.getString(stringResFileCount), headerItem.numberTransfers)
                    textViewFileCount.text = fileCountText

                    when (headerItem.status) {
                        TransferStatus.TRANSFER_FAILED -> {
                            uploadListGroupButtonClear.apply {
                                isVisible = true
                                setOnClickListener {  }
                            }
                            uploadListGroupButtonRetry.apply {
                                isVisible = true
                                setOnClickListener {  }
                            }
                        }
                        TransferStatus.TRANSFER_SUCCEEDED -> {
                            uploadListGroupButtonClear.apply {
                                isVisible = true
                                setOnClickListener {  }
                            }
                            uploadListGroupButtonRetry.isVisible = false
                        }
                        else -> {
                            uploadListGroupButtonRetry.isVisible = false
                            uploadListGroupButtonClear.isVisible = false
                        }
                    }
                }
            }
        }

    }

    fun setData(transfers: List<OCTransfer>) {
        //val diffCallback = LoggingDiffUtil(logsList, logs)
        //val diffResult = DiffUtil.calculateDiff(diffCallback)
        val comparator: Comparator<OCTransfer> = object : Comparator<OCTransfer> {
            override fun compare(transfer1: OCTransfer, transfer2: OCTransfer): Int {
                if (transfer1.status == TransferStatus.TRANSFER_IN_PROGRESS) {
                    if (transfer2.status != TransferStatus.TRANSFER_IN_PROGRESS) {
                        return -1
                    }
                    // Previously there was a check here to check if the uploads are in progress
                } else if (transfer2.status == TransferStatus.TRANSFER_IN_PROGRESS) {
                    return 1
                }
                return if (transfer1.transferEndTimestamp == null || transfer2.transferEndTimestamp == null) {
                    compareUploadId(transfer1, transfer2)
                } else {
                    compareUpdateTime(transfer1, transfer2)
                }
            }

            private fun compareUploadId(transfer1: OCTransfer, transfer2: OCTransfer): Int {
                return (transfer1.id!!).compareTo(transfer2.id!!)
            }

            private fun compareUpdateTime(transfer1: OCTransfer, transfer2: OCTransfer): Int {
                return (transfer2.transferEndTimestamp!!).compareTo(transfer1.transferEndTimestamp!!)
            }
        }

        val transfersAndHeaders = mutableListOf<TransferRecyclerItem>()
        val transfersGroupedByStatus = transfers.groupBy { it.status }
        transfersGroupedByStatus.forEach { transferMap ->
            val headerItem = TransferRecyclerItem.HeaderItem(transferMap.key, transferMap.value.size)
            val transferItems = transferMap.value.sortedWith(comparator).map { transfer ->
                TransferRecyclerItem.TransferItem(transfer)
            }
            transfersAndHeaders.add(headerItem)
            transfersAndHeaders.addAll(transferItems)
        }
        transfersList.clear()
        transfersList.addAll(transfersAndHeaders)
        notifyDataSetChanged()
        //diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = transfersList.size

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TransferRecyclerItem.TransferItem -> TransferRecyclerItemViewType.ITEM_VIEW_TRANSFER.ordinal
            is TransferRecyclerItem.HeaderItem -> TransferRecyclerItemViewType.ITEM_VIEW_HEADER.ordinal
        }
    }

    fun getItem(position: Int) = transfersList[position]

    sealed class TransferRecyclerItem {
        data class TransferItem(val transfer: OCTransfer): TransferRecyclerItem()
        data class HeaderItem(
            val status: TransferStatus,
            val numberTransfers: Int,
        ): TransferRecyclerItem()
    }

    class TransferItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = UploadListItemBinding.bind(itemView)
    }

    class HeaderItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = UploadListGroupBinding.bind(itemView)
    }

    enum class TransferRecyclerItemViewType {
        ITEM_VIEW_TRANSFER, ITEM_VIEW_HEADER
    }
}
