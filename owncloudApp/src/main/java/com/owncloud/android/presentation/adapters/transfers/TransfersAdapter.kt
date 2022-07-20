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
import com.owncloud.android.databinding.UploadListItemBinding
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager.AsyncThumbnailDrawable
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ThumbnailGenerationTask
import com.owncloud.android.domain.files.model.OCFile
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.domain.transfers.model.TransferStatus
import com.owncloud.android.extensions.statusToStringRes
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimetypeIconUtil
import timber.log.Timber
import java.io.File

class TransfersAdapter : RecyclerView.Adapter<TransfersAdapter.TransferItemViewHolder>() {

    private val transfersList = mutableListOf<OCTransfer>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransferItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.upload_list_item, parent, false)
        return TransferItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransferItemViewHolder, position: Int) {
        val transfer = transfersList[position]
        holder.binding.apply {
            val remoteFile = File(transfer.remotePath)

            var fileName = remoteFile.name
            if (fileName.isEmpty()) {
                fileName = File.separator
            }
            uploadName.text = fileName

            uploadRemotePath.text = holder.itemView.context.getString(R.string.app_name) + remoteFile.parent

            uploadFileSize.text = DisplayUtils.bytesToHumanReadable(transfer.fileSize, holder.itemView.context)

            uploadDate.isVisible = transfer.transferEndTimestamp != null && transfer.status != TransferStatus.TRANSFER_FAILED
            transfer.transferEndTimestamp?.let {
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
                val account = AccountUtils.getOwnCloudAccountByName(holder.itemView.context, transfer.accountName)
                val oca = OwnCloudAccount(account, holder.itemView.context)
                val accountName = oca.displayName + " @ " +
                        DisplayUtils.convertIdn(account.name.substring(account.name.lastIndexOf("@") + 1), false)
                uploadAccount.text  = accountName
            } catch (e: Exception) {
                Timber.w("Couldn't get display name for account, using old style")
                uploadAccount.text = transfer.accountName
            }

            uploadStatus.isVisible = transfer.status != TransferStatus.TRANSFER_SUCCEEDED
            uploadStatus.text = " — " + holder.itemView.context.getString(transfer.statusToStringRes())

            val fakeFileToCheatThumbnailsCacheManagerInterface = OCFile(
                owner = transfer.accountName,
                length = transfer.fileSize,
                modificationTimestamp = 0,
                remotePath = transfer.remotePath,
                mimeType = MimetypeIconUtil.getBestMimeTypeByFilename(transfer.localPath),
                storagePath = transfer.localPath,
            )
            val allowedToCreateNewThumbnail = ThumbnailsCacheManager.cancelPotentialThumbnailWork(
                fakeFileToCheatThumbnailsCacheManagerInterface,
                thumbnail
            )
            val parentActivity = holder.itemView.context as FileActivity
            if (fakeFileToCheatThumbnailsCacheManagerInterface.isImage
                && fakeFileToCheatThumbnailsCacheManagerInterface.remoteId != null
                && transfer.status == TransferStatus.TRANSFER_SUCCEEDED
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
                if (MimetypeIconUtil.getBestMimeTypeByFilename(transfer.localPath) == "image/png") {
                    thumbnail.setBackgroundColor(holder.itemView.context.getColor(R.color.background_color))
                }
            } else if (fakeFileToCheatThumbnailsCacheManagerInterface.isImage) {
                val file = File(transfer.localPath)
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
                if (MimetypeIconUtil.getBestMimeTypeByFilename(transfer.localPath).equals("image/png", ignoreCase = true)) {
                    thumbnail.setBackgroundColor(holder.itemView.context.getColor(R.color.background_color))
                }
            } else {
                thumbnail.setImageResource(
                    MimetypeIconUtil.getFileTypeIconId(MimetypeIconUtil.getBestMimeTypeByFilename(transfer.localPath), fileName)
                )
            }
            // TODO: progress bar, upload right button
        }
    }

    fun setData(transfers: List<OCTransfer>) {
        //val diffCallback = LoggingDiffUtil(logsList, logs)
        //val diffResult = DiffUtil.calculateDiff(diffCallback)
        transfersList.clear()
        transfersList.addAll(transfers)
        notifyDataSetChanged()
        //diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = transfersList.size

    class TransferItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = UploadListItemBinding.bind(itemView)
    }

    interface Listener {
        fun delete(transfer: OCTransfer)
        fun retry(transfer: OCTransfer)
        fun cancel(transfer: OCTransfer)
    }
}

