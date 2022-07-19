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
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.databinding.UploadListItemBinding
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.utils.DisplayUtils
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

            uploadFileSize.text = DisplayUtils.bytesToHumanReadable(transfer.fileSize, holder.itemView.context) + ", "

            transfer.transferEndTimestamp?.let {
                val dateString = DisplayUtils.getRelativeDateTimeString(
                    holder.itemView.context,
                    it,
                    DateUtils.SECOND_IN_MILLIS,
                    DateUtils.WEEK_IN_MILLIS,
                    0
                )
                uploadDate.text = dateString
            }

            uploadAccount.text = transfer.accountName

            uploadStatus.text = transfer.status.name
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

