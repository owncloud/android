/*
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * Copyright (C) 2019 ownCloud GmbH.
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

package com.owncloud.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.owncloud.android.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Built a logs container which will be displayed as a list
 */
public class LogListAdapter extends RecyclerView.Adapter<LogListAdapter.LogViewHolder> {

    private ArrayList<String> mLogs;

    public LogListAdapter(ArrayList<String> mLogs) {
        this.mLogs = mLogs;
    }

    /**
     * Define the view for each log in the list
     */
    static class LogViewHolder extends RecyclerView.ViewHolder {

        final TextView mLogContent;

        LogViewHolder(View view) {
            super(view);
            mLogContent = view.findViewById(R.id.logContent);
        }
    }

    /**
     * Create the view for each log in the list
     *
     * @param viewGroup
     * @param i
     * @return
     */
    @Override
    public LogViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        final View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.log_item, viewGroup, false);
        return new LogViewHolder(v);
    }

    /**
     * Fill in each log in the list
     *
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(LogViewHolder holder, int position) {
        holder.mLogContent.setText(String.valueOf(mLogs.get(position)));
    }

    @Override
    public int getItemCount() {
        return mLogs.size();
    }

    /**
     * Delete all the logs from the list
     */
    public void clearLogs() {
        mLogs.clear();
        notifyDataSetChanged();
    }
}
