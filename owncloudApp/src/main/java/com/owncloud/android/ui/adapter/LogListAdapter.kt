/*
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2018 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.owncloud.android.R

import java.util.ArrayList

import androidx.recyclerview.widget.RecyclerView

/**
 * Built a logs container which will be displayed as a list
 */
class LogListAdapter(private val completeLogs: ArrayList<String>, filter: String) : RecyclerView.Adapter<LogListAdapter.LogViewHolder>() {
    private var filterLogs: List<String> = ArrayList()

    init {
        setFilter(filter)
    }

    fun setFilter(filter: String) {
        filterLogs = completeLogs.filter { it.contains(filter) }
        notifyDataSetChanged()
    }

    /**
     * Define the view for each log in the list
     */
    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val logContent: TextView = view.findViewById(R.id.logContent)

    }

    /**
     * Create the view for each log in the list
     *
     * @param viewGroup
     * @param i
     * @return
     */
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): LogViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.log_item, viewGroup, false)
        return LogViewHolder(view)
    }

    /**
     * Fill in each log in the list
     *
     * @param holder
     * @param position
     */
    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.logContent.text = filterLogs[position]
    }

    override fun getItemCount(): Int {
        return filterLogs.size
    }

}
