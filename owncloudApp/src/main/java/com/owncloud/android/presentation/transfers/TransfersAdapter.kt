/**
 * ownCloud Android client application
 *
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

package com.owncloud.android.presentation.transfers

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.owncloud.android.R
import com.owncloud.android.databinding.UploadListGroupBinding
import com.owncloud.android.databinding.UploadListItemBinding
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.domain.transfers.model.TransferStatus
import com.owncloud.android.extensions.statusToStringRes
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.presentation.authentication.AccountUtils
import com.owncloud.android.presentation.transfers.TransfersAdapter.TransferRecyclerItem.HeaderItem
import com.owncloud.android.presentation.transfers.TransfersAdapter.TransferRecyclerItem.TransferItem
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimetypeIconUtil
import com.owncloud.android.utils.PreferenceUtils
import com.owncloud.android.workers.DownloadFileWorker
import timber.log.Timber
import java.io.File

class TransfersAdapter(
    val cancel: (OCTransfer) -> Unit,
    val retry: (OCTransfer) -> Unit,
    val clearFailed: () -> Unit,
    val retryFailed: () -> Unit,
    val clearSuccessful: () -> Unit,
    private val personalName: String,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val transferItemsList = mutableListOf<TransferRecyclerItem>()

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
                val transferItem = getItem(position) as TransferItem
                holder.binding.apply {
                    val remoteFile = File(transferItem.transfer.remotePath)

                    var fileName = remoteFile.name
                    if (fileName.isEmpty()) {
                        fileName = File.separator
                    }
                    uploadName.text = fileName

                    transferItem.space?.let {
                        uploadSpaceName.isVisible = true
                        spaceIcon.isVisible = true
                        if (it.isPersonal) {
                            spaceIcon.setImageResource(R.drawable.ic_folder)
                            uploadSpaceName.text = personalName
                        } // TODO: Add else if for shares space
                        else {
                            uploadSpaceName.text = it.name
                        }
                    }

                    uploadRemotePath.text = remoteFile.parent

                    uploadFileSize.text = DisplayUtils.bytesToHumanReadable(transferItem.transfer.fileSize, holder.itemView.context)

                    uploadDate.isVisible =
                        transferItem.transfer.transferEndTimestamp != null && transferItem.transfer.status != TransferStatus.TRANSFER_FAILED
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
                        uploadAccount.text = accountName
                    } catch (e: Exception) {
                        Timber.w("Couldn't get display name for account, using old style")
                        uploadAccount.text = transferItem.transfer.accountName
                    }

                    uploadStatus.isVisible = transferItem.transfer.status != TransferStatus.TRANSFER_SUCCEEDED
                    uploadStatus.text = " — " + holder.itemView.context.getString(transferItem.transfer.statusToStringRes())

                    Glide.with(holder.itemView)
                        .load(transferItem.transfer.localPath)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(
                            MimetypeIconUtil.getFileTypeIconId(
                                MimetypeIconUtil.getBestMimeTypeByFilename(transferItem.transfer.localPath),
                                fileName
                            )
                        )
                        .into(thumbnail)

                    uploadRightButton.isVisible = transferItem.transfer.status != TransferStatus.TRANSFER_SUCCEEDED

                    uploadProgressBar.isVisible = transferItem.transfer.status == TransferStatus.TRANSFER_IN_PROGRESS

                    holder.itemView.setOnClickListener(null)

                    when (transferItem.transfer.status) {
                        TransferStatus.TRANSFER_IN_PROGRESS, TransferStatus.TRANSFER_QUEUED -> {
                            uploadRightButton.apply {
                                setImageResource(R.drawable.ic_action_cancel_grey)
                                setOnClickListener {
                                    cancel(transferItem.transfer)
                                }
                            }
                        }
                        TransferStatus.TRANSFER_FAILED -> {
                            uploadRightButton.apply {
                                setImageResource(R.drawable.ic_action_delete_grey)
                                setOnClickListener {
                                    cancel(transferItem.transfer)
                                }
                            }
                            holder.itemView.setOnClickListener {
                                retry(transferItem.transfer)
                            }
                            holder.binding.ListItemLayout.isClickable = true
                            holder.binding.ListItemLayout.isFocusable = true
                        }
                        TransferStatus.TRANSFER_SUCCEEDED -> {
                            // Nothing to do
                        }
                    }
                }
            }
            is HeaderItemViewHolder -> {
                val headerItem = getItem(position) as HeaderItem
                holder.binding.apply {
                    uploadListGroupName.text = holder.itemView.context.getString(headerTitleStringRes(headerItem.status))

                    val stringResFileCount =
                        if (headerItem.numberTransfers == 1) R.string.uploads_view_group_file_count_single else R.string.uploads_view_group_file_count
                    val fileCountText: String = String.format(holder.itemView.context.getString(stringResFileCount), headerItem.numberTransfers)
                    textViewFileCount.text = fileCountText

                    uploadListGroupButtonClear.isVisible = headerItem.status == TransferStatus.TRANSFER_FAILED ||
                            headerItem.status == TransferStatus.TRANSFER_SUCCEEDED
                    uploadListGroupButtonRetry.isVisible = headerItem.status == TransferStatus.TRANSFER_FAILED

                    when (headerItem.status) {
                        TransferStatus.TRANSFER_FAILED -> {
                            uploadListGroupButtonClear.setOnClickListener {
                                clearFailed()
                            }
                            uploadListGroupButtonRetry.setOnClickListener {
                                retryFailed()
                            }
                        }
                        TransferStatus.TRANSFER_SUCCEEDED -> {
                            uploadListGroupButtonClear.setOnClickListener {
                                clearSuccessful()
                            }
                        }
                        TransferStatus.TRANSFER_QUEUED, TransferStatus.TRANSFER_IN_PROGRESS -> {
                            // Nothing to do
                        }
                    }
                }
            }
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            (holder as TransferItemViewHolder).binding.apply {
                if (payloads[0] is Int) {
                    uploadProgressBar.progress = payloads[0] as Int
                }
            }
        }

    }

    private fun headerTitleStringRes(status: TransferStatus): Int {
        return when (status) {
            TransferStatus.TRANSFER_IN_PROGRESS -> R.string.uploads_view_group_current_uploads
            TransferStatus.TRANSFER_FAILED -> R.string.uploads_view_group_failed_uploads
            TransferStatus.TRANSFER_SUCCEEDED -> R.string.uploads_view_group_finished_uploads
            TransferStatus.TRANSFER_QUEUED -> R.string.uploads_view_group_queued_uploads
        }
    }

    fun setData(transfers: List<OCTransfer>, spaces: List<OCSpace>) {
        val transfersGroupedByStatus = transfers.groupBy { it.status }
        val newTransferItemsList = mutableListOf<TransferRecyclerItem>()
        transfersGroupedByStatus.forEach { transferMap ->
            val headerItem = HeaderItem(transferMap.key, transferMap.value.size)
            newTransferItemsList.add(headerItem)
            val transferItems = transferMap.value.sortedByDescending { it.transferEndTimestamp ?: it.id }.map { transfer ->
                val space = spaces.firstOrNull { it.id == transfer.spaceId && it.accountName == transfer.accountName }
                TransferItem(transfer, space)
            }
            newTransferItemsList.addAll(transferItems)
        }
        val diffCallback = TransfersDiffUtil(transferItemsList, newTransferItemsList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        transferItemsList.clear()
        transferItemsList.addAll(newTransferItemsList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun updateTransferProgress(workInfo: WorkInfo) {
        var updated = false
        var index = 0
        while (!updated && index < transferItemsList.size) {
            val item = transferItemsList[index]
            if (item is TransferItem && workInfo.tags.contains(item.transfer.id.toString())) {
                notifyItemChanged(index, workInfo.progress.getInt(DownloadFileWorker.WORKER_KEY_PROGRESS, -1))
                updated = true
            }
            index += 1
        }
    }

    override fun getItemCount(): Int = transferItemsList.size

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TransferItem -> TransferRecyclerItemViewType.ITEM_VIEW_TRANSFER.ordinal
            is HeaderItem -> TransferRecyclerItemViewType.ITEM_VIEW_HEADER.ordinal
        }
    }

    fun getItem(position: Int) = transferItemsList[position]

    sealed interface TransferRecyclerItem {
        data class TransferItem(
            val transfer: OCTransfer,
            val space: OCSpace?,
        ) : TransferRecyclerItem
        data class HeaderItem(
            val status: TransferStatus,
            val numberTransfers: Int,
        ) : TransferRecyclerItem
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
