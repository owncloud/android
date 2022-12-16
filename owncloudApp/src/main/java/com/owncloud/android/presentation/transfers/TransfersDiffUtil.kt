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

package com.owncloud.android.presentation.transfers

import androidx.recyclerview.widget.DiffUtil

class TransfersDiffUtil(
    private val oldList: List<TransfersAdapter.TransferRecyclerItem>,
    private val newList: List<TransfersAdapter.TransferRecyclerItem>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        if (oldItem is TransfersAdapter.TransferRecyclerItem.TransferItem && newItem is TransfersAdapter.TransferRecyclerItem.TransferItem) {
            return oldItem.transfer.id == newItem.transfer.id
        }

        if (oldItem is TransfersAdapter.TransferRecyclerItem.HeaderItem && newItem is TransfersAdapter.TransferRecyclerItem.HeaderItem) {
            return oldItem.status == newItem.status
        }

        return false
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]

}
