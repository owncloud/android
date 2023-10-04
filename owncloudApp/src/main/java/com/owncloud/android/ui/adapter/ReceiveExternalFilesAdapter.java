/**
 * ownCloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Christian Schabesberger
 * @author Shashvat Kedia
 * @author David González Verdugo
 * @author John Kalimeris
 * Copyright (C) 2021 ownCloud GmbH.
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

import android.accounts.Account;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datamodel.ThumbnailsCacheManager.AsyncThumbnailDrawable;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.extensions.VectorExtKt;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimetypeIconUtil;
import com.owncloud.android.utils.PreferenceUtils;
import com.owncloud.android.utils.SortFilesUtils;

import java.util.Vector;

public class ReceiveExternalFilesAdapter extends BaseAdapter implements ListAdapter {

    private Vector<OCFile> mImmutableFilesList = new Vector<>();
    private Vector<OCFile> mFiles = new Vector<>();
    private Context mContext;
    private Account mAccount;
    private FileDataStorageManager mStorageManager;
    private LayoutInflater mInflater;
    private OnSearchQueryUpdateListener mOnSearchQueryUpdateListener;

    private Boolean mShowHiddenFiles;

    public ReceiveExternalFilesAdapter(Context context,
                                       FileDataStorageManager storageManager,
                                       Account account,
                                       boolean showHiddenFiles) {
        mStorageManager = storageManager;
        mContext = context;
        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAccount = account;
        mShowHiddenFiles = showHiddenFiles;
        if (mContext instanceof OnSearchQueryUpdateListener) {
            mOnSearchQueryUpdateListener = (OnSearchQueryUpdateListener) mContext;
        }
    }

    @Override
    public int getCount() {
        return (mFiles == null) ? 0 : mFiles.size();
    }

    @Override
    public Object getItem(int position) {
        if (mFiles == null || position < 0 || position >= mFiles.size()) {
            return null;
        } else {
            return mFiles.get(position);
        }
    }

    public void setNewItemVector(Vector<OCFile> newItemVector) {
        mFiles.clear();
        for (OCFile file : newItemVector) {
            if (!mShowHiddenFiles) {
                if (!file.getFileName().startsWith(".")) {
                    mFiles.add(file);
                }
            } else {
                mFiles.add(file);
            }
        }
        mImmutableFilesList.clear();
        mImmutableFilesList.addAll(mFiles);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        if (mFiles == null || position < 0 || position >= mFiles.size()) {
            return -1;
        } else {
            return mFiles.get(position).getId();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        if (convertView == null) {
            vi = mInflater.inflate(R.layout.uploader_list_item_layout, parent, false);

            // Allow or disallow touches with other visible windows
            vi.setFilterTouchesWhenObscured(
                    PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(mContext)
            );
        }

        OCFile file = mFiles.get(position);

        TextView filename = vi.findViewById(R.id.filename);
        filename.setText(file.getFileName());

        ImageView fileIcon = vi.findViewById(R.id.thumbnail);
        fileIcon.setTag(file.getId());

        TextView lastModV = vi.findViewById(R.id.last_mod);
        lastModV.setText(DisplayUtils.getRelativeTimestamp(mContext, file.getModificationTimestamp()));

        TextView fileSizeV = vi.findViewById(R.id.file_size);
        TextView fileSizeSeparatorV = vi.findViewById(R.id.file_separator);

        fileSizeV.setVisibility(View.VISIBLE);
        fileSizeSeparatorV.setVisibility(View.VISIBLE);
        fileSizeV.setText(DisplayUtils.bytesToHumanReadable(file.getLength(), mContext));

        // get Thumbnail if file is image
        if (file.isImage() && file.getRemoteId() != null) {
            // Thumbnail in Cache?
            Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    String.valueOf(file.getRemoteId())
            );
            if (thumbnail != null && !file.getNeedsToUpdateThumbnail()) {
                fileIcon.setImageBitmap(thumbnail);
            } else {
                // generate new Thumbnail
                if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, fileIcon)) {
                    final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                            new ThumbnailsCacheManager.ThumbnailGenerationTask(fileIcon, mAccount);
                    if (thumbnail == null) {
                        thumbnail = ThumbnailsCacheManager.mDefaultImg;
                    }
                    final AsyncThumbnailDrawable asyncDrawable = new AsyncThumbnailDrawable(
                            mContext.getResources(),
                            thumbnail,
                            task
                    );
                    fileIcon.setImageDrawable(asyncDrawable);
                    task.execute(file);
                }
            }
        } else {
            fileIcon.setImageResource(
                    MimetypeIconUtil.getFileTypeIconId(file.getMimeType(), file.getFileName())
            );
        }
        return vi;
    }

    public void setSortOrder(Integer order, boolean isAscending) {
        PreferenceManager.setSortOrder(order, mContext, FileStorageUtils.FILE_DISPLAY_SORT);
        PreferenceManager.setSortAscending(isAscending, mContext, FileStorageUtils.FILE_DISPLAY_SORT);
        FileStorageUtils.mSortOrderFileDisp = order;
        FileStorageUtils.mSortAscendingFileDisp = isAscending;
        if (mFiles != null && mFiles.size() > 0) {
            new SortFilesUtils().sortFiles(
                    (Vector<OCFile>) mFiles,
                    FileStorageUtils.mSortOrderFileDisp,
                    FileStorageUtils.mSortAscendingFileDisp
            );
        }
        notifyDataSetChanged();
    }

    public void filterBySearch(String query) {
        clearFilterBySearch();
        VectorExtKt.filterByQuery(mFiles, query);

        if (mFiles.isEmpty() && !query.isEmpty()) {
            mOnSearchQueryUpdateListener.updateEmptyListMessage(
                    mContext.getString(R.string.local_file_list_search_with_no_matches));
        } else {
            mOnSearchQueryUpdateListener.updateEmptyListMessage(mContext.getString(R.string.file_list_empty_title_all_files));
        }

        notifyDataSetChanged();
    }

    public void clearFilterBySearch() {
        mFiles = (Vector<OCFile>) mImmutableFilesList.clone();
        notifyDataSetChanged();
    }

    public Vector<OCFile> getFiles() {
        return mFiles;
    }

    public interface OnSearchQueryUpdateListener {
        void updateEmptyListMessage(String updateTxt);
    }
}
