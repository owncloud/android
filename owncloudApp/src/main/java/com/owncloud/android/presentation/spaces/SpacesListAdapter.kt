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
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.databinding.SpacesListItemBinding
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.domain.spaces.model.OCSpace
import com.owncloud.android.presentation.authentication.AccountUtils
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

            spacesListItemName.text = space.name
            spacesListItemSubtitle.text = space.description

            val spaceSpecialImage = space.getSpaceSpecialImage()
            spacesListItemImage.tag = spaceSpecialImage?.id

            if (spaceSpecialImage != null) {
                val thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(spaceSpecialImage.id)
                if (thumbnail != null) {
                    spacesListItemImage.run {
                        setImageBitmap(thumbnail)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                }
                if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(spaceSpecialImage, spacesListItemImage)) {
                    val account = AccountUtils.getOwnCloudAccountByName(spacesViewHolder.itemView.context, space.accountName)
                    val task = ThumbnailsCacheManager.ThumbnailGenerationTask(spacesListItemImage, account)
                    val asyncDrawable = ThumbnailsCacheManager.AsyncThumbnailDrawable(spacesViewHolder.itemView.resources, thumbnail, task)

                    // If drawable is not visible, do not update it.
                    if (asyncDrawable.minimumHeight > 0 && asyncDrawable.minimumWidth > 0) {
                        spacesListItemImage.run {
                            spacesListItemImage.setImageDrawable(asyncDrawable)
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                    }
                    task.execute(spaceSpecialImage)
                }
                if (spaceSpecialImage.file.mimeType == "image/png") {
                    spacesListItemImage.setBackgroundColor(ContextCompat.getColor(spacesViewHolder.itemView.context, R.color.background_color))
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
