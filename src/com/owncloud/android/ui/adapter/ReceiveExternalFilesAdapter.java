/**
 *   ownCloud Android client application
 *
 *   @author Tobias Kaminsky
 *   @author Christian Schabesberger
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
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datamodel.ThumbnailsCacheManager.AsyncThumbnailDrawable;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimetypeIconUtil;

import java.util.List;

public class ReceiveExternalFilesAdapter extends BaseAdapter implements ListAdapter {

    private List<OCFile> mFiles;
    private Context mContext;
    private Account mAccount;
    private FileDataStorageManager mStorageManager;
    private LayoutInflater mInflater;

    public ReceiveExternalFilesAdapter(Context context,
                                       List<OCFile> files,
                                       FileDataStorageManager storageManager,
                                       Account account
    ) {
        mFiles = files;
        mAccount = account;
        mStorageManager = storageManager;
        mContext = context;
        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

    @Override
    public long getItemId(int position) {
        if (mFiles == null || position < 0 || position >= mFiles.size()) {
            return -1;
        } else {
            return mFiles.get(position).getFileId();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        if (convertView == null) {
            vi = mInflater.inflate(R.layout.uploader_list_item_layout, parent, false);
        }

        OCFile file = mFiles.get(position);

        TextView filename = vi.findViewById(R.id.filename);
        filename.setText(file.getFileName());

        ImageView fileIcon = vi.findViewById(R.id.thumbnail);
        fileIcon.setTag(file.getFileId());

        TextView lastModV = vi.findViewById(R.id.last_mod);
        lastModV.setText(DisplayUtils.getRelativeTimestamp(mContext, file.getModificationTimestamp()));

        TextView fileSizeV = vi.findViewById(R.id.file_size);
        TextView fileSizeSeparatorV = vi.findViewById(R.id.file_separator);

        fileSizeV.setVisibility(View.VISIBLE);
        fileSizeSeparatorV.setVisibility(View.VISIBLE);
        fileSizeV.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength(), mContext));

        // get Thumbnail if file is image
        if (file.isImage() && file.getRemoteId() != null){
             // Thumbnail in Cache?
            Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    String.valueOf(file.getRemoteId())
            );
            if (thumbnail != null && !file.needsUpdateThumbnail()){
                fileIcon.setImageBitmap(thumbnail);
            } else {
                // generate new Thumbnail
                if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, fileIcon)) {
                    final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                            new ThumbnailsCacheManager.ThumbnailGenerationTask(fileIcon, mStorageManager,
                                    mAccount);
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
                    MimetypeIconUtil.getFileTypeIconId(file.getMimetype(), file.getFileName())
            );
        }
        return vi;
    }


}
