/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2026 ownCloud GmbH.
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

package com.owncloud.android.presentation.spaces.members

import androidx.recyclerview.widget.DiffUtil
import com.owncloud.android.domain.spaces.model.SpaceMember

class SpaceMembersDiffUtil(
    private val oldList: List<SpaceMember>,
    private val newList: List<SpaceMember>,
    private val oldNumberOfManagers: Int,
    private val newNumberOfManagers: Int
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        return ((oldItem.id == newItem.id) && (oldItem.displayName == newItem.displayName) && (oldItem.roles == newItem.roles)
                && (oldItem.expirationDateTime == newItem.expirationDateTime) && (oldNumberOfManagers == newNumberOfManagers))
    }
}
