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

package com.owncloud.android.presentation.spaces

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.databinding.SpacesListItemBinding
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.utils.PreferenceUtils

class SpacesListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val spacesList = mutableListOf<OCSpace>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = SpacesListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.root.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(parent.context)
        return SpacesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val spacesViewHolder = holder as SpacesViewHolder
        spacesViewHolder.binding.apply {
            val space = spacesList[position]

            spacesListItemName.text = space.name
            spacesListItemSubtitle.text = space.description

            space.getSpaceImageWebDavUrl()?.let {

            }
        }
    }

    fun setData(spaces: List<OCSpace>) {
        spacesList.clear()
        spacesList.addAll(spaces)
    }

    override fun getItemCount(): Int = spacesList.size

    fun getItem(position: Int) = spacesList[position]

    class SpacesViewHolder(val binding: SpacesListItemBinding) : RecyclerView.ViewHolder(binding.root)
}
