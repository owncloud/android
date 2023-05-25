/**
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
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
 *
 */

package com.owncloud.android.presentation.files.filelist

import android.util.SparseBooleanArray
import androidx.recyclerview.widget.RecyclerView

abstract class SelectableAdapter<VH : RecyclerView.ViewHolder?> :
    RecyclerView.Adapter<VH>() {
    private val selectedItems: SparseBooleanArray = SparseBooleanArray()

    /**
     * Indicates if the item at position position is selected
     * @param position Position of the item to check
     * @return true if the item is selected, false otherwise
     */
    fun isSelected(position: Int): Boolean {
        return getSelectedItems().contains(position)
    }

    /**
     * Toggle the selection status of the item at a given position
     * @param position Position of the item to toggle the selection status for
     */
    fun toggleSelection(position: Int) {
        if (selectedItems[position, false]) {
            selectedItems.delete(position)
        } else {
            selectedItems.put(position, true)
        }
        notifyItemChanged(position)
    }

    /**
     * Clear the selection status for all items
     */
    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    /**
     * Count the selected items
     * @return Selected items count
     */
    val selectedItemCount: Int
        get() = selectedItems.size()

    /**
     * Indicates the list of selected items
     * @return List of selected items ids
     */
    fun getSelectedItems(): List<Int> {
        val items: MutableList<Int> = ArrayList(selectedItems.size())
        for (i in 0 until selectedItems.size()) {
            items.add(selectedItems.keyAt(i))
        }
        return items
    }

    /**
     * Toggle selected items in bulk. Basically to do a select inverse.
     * Doing it individually will cost a lot of time since we do a notifyDataSetChanged for each item.
     */
    fun toggleSelectionInBulk(totalItems: Int) {
        for (i in 0 until totalItems) {
            if (selectedItems[i, false]) {
                selectedItems.delete(i)
            } else {
                selectedItems.put(i, true)
            }
        }
        notifyDataSetChanged()
    }

    fun selectAll(totalItems: Int) {
        for (i in 0 until totalItems) {
            selectedItems.put(i, true)
        }
        notifyDataSetChanged()
    }
}
