/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 * @author Manuel Plazas Palacio
 * @author Aitor Balleteros Pavón
 *
 * Copyright (C) 2024 ownCloud GmbH.
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
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import com.owncloud.android.R
import com.owncloud.android.databinding.SpacesListItemBinding
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.extensions.setAccessibilityRole
import com.owncloud.android.presentation.thumbnails.ThumbnailsRequester
import com.owncloud.android.utils.PreferenceUtils

class SpacesListAdapter(
    private val listener: SpacesListAdapterListener,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

            spacesListItemCard.setOnClickListener {
                listener.onItemClick(space)
            }
            spacesListItemCard.setAccessibilityRole(className = Button::class.java)

            if (space.isPersonal) {
                spacesListItemName.text = holder.itemView.context.getString(R.string.bottom_nav_personal)
                spacesListItemImage.apply {
                    dispose()
                    setImageResource(R.drawable.ic_folder)
                }
            } else {
                spacesListItemName.text = space.name
                spacesListItemSubtitle.text = space.description

                val spaceSpecialImage = space.getSpaceSpecialImage()

                if (spaceSpecialImage != null) {
                    spacesListItemImage.load(
                        ThumbnailsRequester.getPreviewUriForSpaceSpecial(spaceSpecialImage),
                        ThumbnailsRequester.getCoilImageLoader()
                    ) {
                        placeholder(R.drawable.ic_spaces_placeholder)
                        error(R.drawable.ic_spaces_placeholder)
                    }
                } else {
                    spacesListItemImage.apply {
                        dispose()
                        setImageResource(R.drawable.ic_spaces_placeholder)
                        setBackgroundColor(ContextCompat.getColor(spacesViewHolder.itemView.context, R.color.spaces_card_background_color))
                    }
                }
            }
        }
    }

    fun setData(spaces: List<OCSpace>) {
        val diffCallback = SpacesListDiffUtil(spacesList, spaces)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        spacesList.clear()
        spacesList.addAll(spaces)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = spacesList.size

    fun getItem(position: Int) = spacesList[position]

    interface SpacesListAdapterListener {
        fun onItemClick(ocSpace: OCSpace)
    }

    class SpacesViewHolder(val binding: SpacesListItemBinding) : RecyclerView.ViewHolder(binding.root)
}
