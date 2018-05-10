/**
 *   ownCloud Android client application
 *
 *   @author David González Verdugo
 *   Copyright (C) 2018 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui.adapter;

import android.support.v7.widget.RecyclerView;
import com.owncloud.android.logs.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.owncloud.android.R;

import org.w3c.dom.Text;

import java.util.ArrayList;

/**
 * Built a logs container which will be displayed as a list
 */
public class LogListAdapter extends RecyclerView.Adapter<LogListAdapter.LogViewHolder>{

    private ArrayList<Log> mLogs;

    public LogListAdapter(ArrayList<Log> mLogs) {
        this.mLogs = mLogs;
    }

    /**
     * Define the view for each log in the list
     */
    public static class LogViewHolder extends RecyclerView.ViewHolder {

        public final TextView mLogTimeStamp;
        public final TextView mLogContent;

        public LogViewHolder(View view) {

            super(view);

            mLogTimeStamp = view.findViewById(R.id.logTimestamp);
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
        final View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.log_item, viewGroup, false);
        final LogViewHolder holder = new LogViewHolder(v);
        return holder;
    }

    /**
     * Fill in each log in the list
     *
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(LogViewHolder holder, int position) {
        holder.mLogTimeStamp.setText(String.valueOf(mLogs.get(position).getLogTimestamp()));
        holder.mLogContent.setText(String.valueOf(mLogs.get(position).getLogContent()));
    }

    @Override
    public int getItemCount() {
        return mLogs.size();
    }

    /**
     * Get all logs from the list
     *
     * @return all the logs
     */
    public ArrayList<Log> getAllLogs() {
        return mLogs;
    }

    /**
     * Delete all the logs from the list
     */
    public void clearLogs() {
        mLogs.clear();
        notifyDataSetChanged();
    }
}
