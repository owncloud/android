/*
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
 * Copyright (C) 2021 ownCloud GmbH.
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

package com.owncloud.android.presentation.adapters.logging

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.databinding.LogListItemBinding
import com.owncloud.android.extensions.getSize
import com.owncloud.android.utils.LoggingDiffUtil
import java.io.File

class RecyclerViewLogsAdapter : RecyclerView.Adapter<RecyclerViewLogsAdapter.ViewHolder>() {

    private val logsList = ArrayList<File>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.log_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = logsList[position]
        holder.binding.apply {
            textViewTitleActivityLogsList.text = log.name
            textViewSubtitleActivityLogsList.text = log.getSize()
        }
    }

    fun setData(logs: List<File>) {
        val diffCallback = LoggingDiffUtil(logsList, logs)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        logsList.clear()
        logsList.addAll(logs)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = logsList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = LogListItemBinding.bind(itemView)
    }
}
